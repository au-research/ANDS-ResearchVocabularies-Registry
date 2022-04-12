/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.workflow.provider.transform;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.vocabulary.SKOS;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.repository.manager.RepositoryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.dao.AccessPointDAO;
import au.org.ands.vocabs.registry.db.dao.ResourceMapEntryDAO;
import au.org.ands.vocabs.registry.db.dao.ResourceOwnerHostDAO;
import au.org.ands.vocabs.registry.db.entity.AccessPoint;
import au.org.ands.vocabs.registry.db.entity.ResourceMapEntry;
import au.org.ands.vocabs.registry.db.entity.ResourceOwnerHost;
import au.org.ands.vocabs.registry.enums.AccessPointType;
import au.org.ands.vocabs.registry.enums.SubtaskOperationType;
import au.org.ands.vocabs.registry.enums.TaskStatus;
import au.org.ands.vocabs.registry.utils.PropertyConstants;
import au.org.ands.vocabs.registry.utils.RegistryProperties;
import au.org.ands.vocabs.registry.workflow.provider.DefaultPriorities;
import au.org.ands.vocabs.registry.workflow.provider.WorkflowProvider;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;
import au.org.ands.vocabs.registry.workflow.tasks.TaskRunner;
import au.org.ands.vocabs.registry.workflow.tasks.TaskUtils;

/**
 * Transform provider for adding/removing resource map entries.
 *
 * Prerequisite for this transform is that the version must have exactly
 * one access point of type SISSVoc.
 */
public class ResourceMapTransformProvider implements WorkflowProvider {

    /** Logger for this class. */
    private final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** URL to access the Sesame server. */
    private static String sesameServer = RegistryProperties.getProperty(
            PropertyConstants.SESAME_IMPORTER_SERVERURL);

    /** Force loading of HttpClientUtils, so that shutdown works
     * properly. Revisit this when using a later version of Tomcat,
     * as the problem may be caused by a defect in Tomcat.
     * For now (Tomcat 7.0.61), without this, you get an error
     * on Tomcat shutdown:
     * <pre>
     * Exception in thread "RepositoryProvider-shutdownHook"
     *  java.lang.NoClassDefFoundError:
     *   org/apache/http/client/utils/HttpClientUtils
     *    at org.openrdf.http.client.SesameClientImpl.shutDown(
     *    SesameClientImpl.java:102)
     * at org.openrdf.repository.manager.RemoteRepositoryManager.shutDown(
     *    RemoteRepositoryManager.java:156)
     * at org.openrdf.repository.manager.
     *   RepositoryProvider$SynchronizedManager.shutDown(
     *     RepositoryProvider.java:68)
     * at org.openrdf.repository.manager.RepositoryProvider$1.run(
     *   RepositoryProvider.java:81)
     * Caused by: java.lang.ClassNotFoundException:
     *    org.apache.http.client.utils.HttpClientUtils
     *  at org.apache.catalina.loader.WebappClassLoader.loadClass(
     *     WebappClassLoader.java:1720)
     *  at org.apache.catalina.loader.WebappClassLoader.loadClass(
     *     WebappClassLoader.java:1571)
     *  ... 4 more
     *  </pre>
     *
     */
    @SuppressWarnings("unused")
    private static final Class<org.apache.http.client.utils.HttpClientUtils>
        HTTPCLIENTUTILS_CLASS =
            org.apache.http.client.utils.HttpClientUtils.class;

    /** The default setting for {@code fail-on-error}. */
    private static final boolean FAIL_ON_ERROR_DEFAULT = false;

    /** Array of resource types of interest. */
    private static URI[] resourceTypes = {
            SKOS.CONCEPT,
            SKOS.CONCEPT_SCHEME,
            SKOS.COLLECTION
    };

    /* After template replacement, the finished query sent to the repository
       looks something like this:
        SELECT ?iri {
          ?iri a ?type
          FILTER (?type IN (
            <http://www.w3.org/2004/02/skos/core#ConceptScheme>,
            <http://www.w3.org/2004/02/skos/core#Collection>,
            <http://www.w3.org/2004/02/skos/core#Concept>))
          FILTER (REGEX(STR(?iri),"^https?://(abcd\\.org|efgh\\.org)/","i"))
        }
     */

    /** Name of the binding for iri
     *  used within {@link #EXTRACT_IRIS_QUERY_TEMPLATE}.
     */
    private static final String BINDING_NAME_IRI = "iri";

    /** Name of the binding for type
     *  used within {@link #EXTRACT_IRIS_QUERY_TEMPLATE}.
     */
    private static final String BINDING_NAME_TYPE = "type";

    /** Name of the binding for owned
     *  used within {@link #EXTRACT_IRIS_QUERY_TEMPLATE}.
     */
    private static final String BINDING_NAME_OWNED = "owned";

    /** Name of the binding for deprecated
     *  used within {@link #EXTRACT_IRIS_QUERY_TEMPLATE}.
     */
    private static final String BINDING_NAME_DEPRECATED = "deprecated";

    /** Template for a SPARQL Query to extract exactly those IRIs
     * to be added to the concept map. The template elements
     * #RESOURCETYPES# and #HOSTNAMES# are replaced by
     * {@link #transform(TaskInfo, Subtask)}.
     * When running this query, turn off inferred statements, otherwise
     * deprecated resources that don't have a defined type will go missing.
     * (If inferring is on, they get the inferred type rdfs:Resource.)
     */
    private static final String EXTRACT_IRIS_QUERY_TEMPLATE =
            "SELECT ?iri ?type ?owned ?deprecated {"
            + "  {"
            + "    ?iri a ?type"
            + "    FILTER (?type IN (#RESOURCETYPES#))"
            + "    OPTIONAL {"
            + "      ?iri <http://www.w3.org/2002/07/owl#deprecated> true"
            + "      BIND (true AS ?found_deprecated)"
            + "    }"
            + "    BIND (BOUND(?found_deprecated) AS ?deprecated)"
            + "  } UNION {"
            + "    ?iri <http://www.w3.org/2002/07/owl#deprecated> true"
            + "    FILTER NOT EXISTS { ?iri a ?another_type }"
            + "    BIND (<http://www.w3.org/2002/07/owl#deprecated> AS ?type)"
            + "    BIND (true AS ?deprecated)"
            + "  }"
            + "  BIND (REGEX(STR(?iri),\"^https?://(#HOSTNAMES#)/\",\"i\")"
            + "    AS ?owned)"
            + "}";

    /** Determine the access point ID associated with the subtask.
     * If there is not exactly one such access point, null is returned.
     * @param taskInfo The TaskInfo for this subtask.
     * @return The access point ID associated with this subtask,
     *      or null, if there is not exactly one such access point.
     */
    private Integer getAccessPointId(final TaskInfo taskInfo) {
        List<AccessPoint> aps =
                AccessPointDAO.getCurrentAccessPointListForVersionByType(
                        taskInfo.getVersion().getVersionId(),
                        AccessPointType.SISSVOC,
                        taskInfo.getEm());
        if (aps.size() != 1) {
            return null;
        }
        return aps.get(0).getAccessPointId();
    }

    /** Add the resource map entries for the version.
     * @param taskInfo The top-level TaskInfo for the subtask.
     * @param subtask The subtask to be performed.
     */
    @SuppressWarnings("checkstyle:MethodLength")
    public final void transform(final TaskInfo taskInfo,
            final Subtask subtask) {
        Integer accessPointId = getAccessPointId(taskInfo);
        if (accessPointId == null) {
            logNotExactlyOneSissvocAccessPoint(taskInfo, subtask);
            // Return failure (i.e., false) in this case,
            // if fail_on_error is set to true.
            if (TaskUtils.isSubtaskFailOnError(subtask,
                    FAIL_ON_ERROR_DEFAULT)) {
                subtask.setStatus(TaskStatus.ERROR);
            } else {
                subtask.setStatus(TaskStatus.SUCCESS);
            }
            return;
        }

        String owner = taskInfo.getVocabulary().getOwner();
        List<ResourceOwnerHost> resourceOwnerHosts =
                ResourceOwnerHostDAO.getCurrentResourceOwnerHostsForOwner(
                        owner, taskInfo.getEm());

        // Possible future change: support indexing of "everything",
        // i.e., even when the owner has no associated hosts.
        if (resourceOwnerHosts.size() == 0) {
            // This owner has no hosts associated with it. So there
            // is nothing more to be done.
            logNoHosts(taskInfo, subtask);
            subtask.setStatus(TaskStatus.SUCCESS);
            return;
        }

        // Join the resource owner hostnames together to get a String
        // "host\\.name\\.one|host\\.name\\.two".
        String resourceOwnerRegex =
                resourceOwnerHosts.stream()
                .map(roh -> roh.getHost().replaceAll("\\.", "\\\\\\\\."))
                .collect(Collectors.joining("|"));
        // Join the resource types together to get a String
        // "<http://...>, <http://...>, <http://...>"
        // for insertion into the SPARQL query.
        String resourceTypesString =
                Arrays.asList(resourceTypes).stream()
                .map(i -> "<" + i.toString() + ">")
                .collect(Collectors.joining(", "));
        // Now do search/replace of placeholders in the query string.
        String queryString =
                EXTRACT_IRIS_QUERY_TEMPLATE.replace(
                        "#RESOURCETYPES#", resourceTypesString)
                .replace("#HOSTNAMES#", resourceOwnerRegex);

        RepositoryManager manager = null;
        Repository repository;
        // First, open the repository.
        try {
            manager = RepositoryProvider.getRepositoryManager(sesameServer);
            String repositoryID = TaskUtils.getSesameRepositoryId(
                    taskInfo);
            repository = manager.getRepository(repositoryID);
            if (repository == null) {
                subtask.addResult(TaskRunner.ERROR,
                        "ResourceMapTransformProvider.transform(): "
                        + "no such repository: " + repositoryID);
                // Return failure (i.e., false) if fail_on_error is set to true.
                if (TaskUtils.isSubtaskFailOnError(subtask,
                        FAIL_ON_ERROR_DEFAULT)) {
                    subtask.setStatus(TaskStatus.ERROR);
                } else {
                    subtask.setStatus(TaskStatus.SUCCESS);
                }
                return;
            }
        } catch (RepositoryConfigException | RepositoryException e) {
            // Log separately so as to get stacktrace.
            logger.error("Exception in ResourceMapTransformProvider."
                    + "transform() opening repository", e);
            subtask.addResult(TaskRunner.ERROR,
                    "Exception in ResourceMapTransformProvider.transform() "
                            + "opening repository");
            subtask.addResult(TaskRunner.STACKTRACE,
                    ExceptionUtils.getStackTrace(e));
            // An exception: always return false in this case.
            subtask.setStatus(TaskStatus.ERROR);
            return;
        }

        // Clear out any existing entries before proceeding.
        ResourceMapEntryDAO.deleteResourceMapEntriesForAccessPoint(
                accessPointId, taskInfo.getEm());
        // Now, open a connection and process the resources.
        try {
            RepositoryConnection conn = null;
            TupleQueryResult queryResult = null;
            try {
                conn = repository.getConnection();
                try {
                    TupleQuery query =
                            conn.prepareTupleQuery(QueryLanguage.SPARQL,
                            queryString);
                    // Don't include inferred results. Doing so causes
                    // deprecated concepts without a defined type to
                    // go missing from query results.
                    query.setIncludeInferred(false);
                    queryResult = query.evaluate();
                    // CC-2014 Do database processing in bulk, using our own
                    // transaction. This makes a very big difference
                    // to performance, over doing a separate transaction
                    // to insert each new entry.
                    while (queryResult.hasNext()) {
                        BindingSet aBinding = queryResult.next();
                        Value iri = aBinding.getBinding(BINDING_NAME_IRI)
                                .getValue();
                        LiteralImpl owned = (LiteralImpl)
                                aBinding.getBinding(BINDING_NAME_OWNED)
                                        .getValue();
                        Value resourceType = aBinding.
                                getBinding(BINDING_NAME_TYPE).getValue();
                        LiteralImpl deprecated = (LiteralImpl)
                                aBinding.getBinding(BINDING_NAME_DEPRECATED)
                                        .getValue();
                        ResourceMapEntry rme = new ResourceMapEntry();
                        rme.setIri(iri.stringValue());
                        rme.setAccessPointId(accessPointId);
                        rme.setOwned(owned.booleanValue());
                        rme.setResourceType(resourceType.stringValue());
                        rme.setDeprecated(deprecated.booleanValue());
                        ResourceMapEntryDAO.saveResourceMapEntry(
                                taskInfo.getEm(), rme);
                    }
                    queryResult.close();
                } catch (MalformedQueryException | QueryEvaluationException e) {
                    logger.error("Bad query constructed in "
                            + "ResourceMapTransformProvider.transform(): "
                            + queryString, e);
                    subtask.addResult(TaskRunner.STACKTRACE,
                            ExceptionUtils.getStackTrace(e));
                    subtask.addResult(TaskRunner.ERROR,
                            "Bad query constructed in "
                            + "ResourceMapTransformProvider.transform(): "
                            + queryString);
                    // An exception: always return error status in this case.
                    subtask.setStatus(TaskStatus.ERROR);
                    return;
                }
            } finally {
                if (queryResult != null) {
                    queryResult.close();
                }
                if (conn != null) {
                    conn.close();
                }
            }
        } catch (RepositoryException | QueryEvaluationException e) {
            logger.error("Exception in ResourceMapTransformProvider."
                    + "transform() with connection handling", e);
            subtask.addResult(TaskRunner.STACKTRACE,
                    ExceptionUtils.getStackTrace(e));
            subtask.addResult(TaskRunner.ERROR,
                    "Exception in ResourceMapTransformProvider."
                    + "transform() with connection handling");
            // An exception: always return error status in this case.
            subtask.setStatus(TaskStatus.ERROR);
            return;
        }

        // Subtask completed successfully.
        subtask.setStatus(TaskStatus.SUCCESS);
    }

    /** Log the fact that there is not exactly one access point of
     * type "sissvoc".
     * @param taskInfo The TaskInfo object describing the entire task.
     * @param subtask The subtask to be performed.
     */
    private void logNotExactlyOneSissvocAccessPoint(final TaskInfo taskInfo,
            final Subtask subtask) {
        subtask.addResult(TaskRunner.ERROR,
                "ResourceMapTransformProvider.transform(): "
                + "not exactly one SISSVoc access point for version: "
                + taskInfo.getVersion().getVersionId());
    }

    /** Log the fact that there are no hosts associated with the
     * owner of the vocabulary.
     * @param taskInfo The TaskInfo object describing the entire task.
     * @param subtask The subtask to be performed.
     */
    private void logNoHosts(final TaskInfo taskInfo,
            final Subtask subtask) {
        subtask.addResult(TaskRunner.INFO_PRIVATE,
                "ResourceMapTransformProvider.transform(): "
                + "no hosts associated with this owner: "
                + taskInfo.getVocabulary().getOwner());
    }

    /** Add the resource map entries for the version.
     * @param taskInfo The top-level TaskInfo for the subtask.
     * @param subtask The subtask to be performed.
     */
    public final void untransform(final TaskInfo taskInfo,
            final Subtask subtask) {
        Integer accessPointId = getAccessPointId(taskInfo);
        if (accessPointId == null) {
            subtask.addResult(TaskRunner.ERROR,
                    "ResourceMapTransformProvider.untransform(): "
                    + "not exactly one SISSVoc access point for version: "
                    + taskInfo.getVersion().getVersionId());
            // Return failure (i.e., false) in this case,
            // if fail-on-error is set to true.
            if (TaskUtils.isSubtaskFailOnError(subtask,
                    FAIL_ON_ERROR_DEFAULT)) {
                subtask.setStatus(TaskStatus.ERROR);
            } else {
                subtask.setStatus(TaskStatus.SUCCESS);
            }
        }

        ResourceMapEntryDAO.deleteResourceMapEntriesForAccessPoint(
                accessPointId, taskInfo.getEm());

        // Subtask completed successfully.
        subtask.setStatus(TaskStatus.SUCCESS);
    }

    /** {@inheritDoc} */
    @Override
    public Integer defaultPriority(final SubtaskOperationType operationType) {
        switch (operationType) {
        case INSERT:
        case PERFORM:
            return DefaultPriorities.
                    DEFAULT_TRANSFORM_AFTER_PUBLISH_INSERT_PRIORITY;
        case DELETE:
            return DefaultPriorities.
                    DEFAULT_TRANSFORM_AFTER_PUBLISH_DELETE_PRIORITY;
        default:
            // Unknown operation type!
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void doSubtask(final TaskInfo taskInfo, final Subtask subtask) {
        switch (subtask.getOperation()) {
        case INSERT:
        case PERFORM:
            transform(taskInfo, subtask);
            break;
        case DELETE:
            untransform(taskInfo, subtask);
            break;
        default:
            logger.error("Unknown operation!");
            break;
        }
    }

}
