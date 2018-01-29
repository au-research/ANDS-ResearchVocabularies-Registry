/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.workflow.provider.importer;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.apache.commons.lang3.BooleanUtils;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.manager.RepositoryInfo;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.repository.manager.RepositoryProvider;
import org.openrdf.repository.sail.config.SailRepositoryConfig;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;
import org.openrdf.sail.config.SailImplConfig;
import org.openrdf.sail.inferencer.fc.config.ForwardChainingRDFSInferencerConfig;
import org.openrdf.sail.nativerdf.config.NativeStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.dao.AccessPointDAO;
import au.org.ands.vocabs.registry.db.entity.AccessPoint;
import au.org.ands.vocabs.registry.db.internal.VersionJson;
import au.org.ands.vocabs.registry.db.internal.VocabularyJson;
import au.org.ands.vocabs.registry.enums.AccessPointType;
import au.org.ands.vocabs.registry.enums.ApSource;
import au.org.ands.vocabs.registry.enums.SubtaskOperationType;
import au.org.ands.vocabs.registry.enums.TaskStatus;
import au.org.ands.vocabs.registry.utils.PropertyConstants;
import au.org.ands.vocabs.registry.utils.RegistryNetUtils;
import au.org.ands.vocabs.registry.utils.RegistryProperties;
import au.org.ands.vocabs.registry.workflow.provider.DefaultPriorities;
import au.org.ands.vocabs.registry.workflow.provider.WorkflowProvider;
import au.org.ands.vocabs.registry.workflow.tasks.AccessPointUtils;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;
import au.org.ands.vocabs.registry.workflow.tasks.TaskRunner;
import au.org.ands.vocabs.registry.workflow.tasks.TaskUtils;

/** Sesame importer provider. */
public class SesameImporterProvider implements WorkflowProvider {

    /** Logger for this class. */
    private final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Subtask property to specify if an existing repository should
    * be cleared before triples are imported. Defaults to true;
    * to disable, set the value to {@code "false"}. */
   private static final String CLEAR = "clear";

   /** Prefix for keys used for results that say that a file could
    * not be parsed. */
   public static final String PARSE_PREFIX = "parse-";

    /** URL to access the Sesame server. */
    private String sesameServer = RegistryProperties.getProperty(
            PropertyConstants.SESAME_IMPORTER_SERVERURL);

    /** URL that is a prefix to all SPARQL endpoints. */
    private String sparqlPrefix = RegistryProperties.getProperty(
            PropertyConstants.SESAME_IMPORTER_SPARQLPREFIX);

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

    /** Get information about the Sesame server.
     * TO DO: hook this up again as a REST service
     * @return A list of the repositories in the Sesame server.
     */
    public final Collection<RepositoryInfo> getInfo() {
        RepositoryManager manager = null;
        try {
            manager = RepositoryProvider.getRepositoryManager(sesameServer);
            Collection<RepositoryInfo> infos =
                    manager.getAllRepositoryInfos(true);
            return infos;
        } catch (RepositoryConfigException | RepositoryException e) {
            logger.error("Exception in Sesame getInfo()", e);
        }
        return null;
    }

    /** Create/update the Sesame repository and access points for the version.
     * @param taskInfo The top-level TaskInfo for the subtask.
     * @param subtask The subtask to be performed.
     */
    public final void doImport(final TaskInfo taskInfo,
            final Subtask subtask) {
        boolean success;
        // Create repository
        success = createRepository(taskInfo, subtask);
        if (!success) {
            return;
        }
        // Upload the RDF
        success = uploadRDF(taskInfo, subtask);
        if (!success) {
            return;
        }
        String repositoryId = TaskUtils.getSesameRepositoryId(taskInfo);
//        subtask.addResult("repository_id", repositoryId);
        // Use the nice JAX-RS libraries to construct the path to
        // the SPARQL endpoint.
        Client client = RegistryNetUtils.getClient();
        WebTarget target = client.target(sparqlPrefix);
        WebTarget sparqlTarget = target
                .path(repositoryId);
//        subtask.addResult("sparql_endpoint",
//                sparqlTarget.getUri().toString());
        // Add apiSparql endpoint
        AccessPointUtils.createApiSparqlAccessPoint(taskInfo,
                sparqlTarget.getUri().toString());
        // Add sesameDownload endpoint
        AccessPointUtils.createSesameDownloadAccessPoint(taskInfo,
                repositoryId, sesameServer);
        subtask.setStatus(TaskStatus.SUCCESS);
    }

    /** Create the repository within Sesame.
     * @param taskInfo The TaskInfo object describing the entire task.
     * @param subtask The subtask to be performed.
     * @return True, iff the repository creation succeeded.
     */
    public final boolean createRepository(final TaskInfo taskInfo,
            final Subtask subtask) {
        RepositoryManager manager = null;
        try {
            manager = RepositoryProvider.getRepositoryManager(sesameServer);

            VocabularyJson vocabularyJson =
                    JSONSerialization.deserializeStringAsJson(
                            taskInfo.getVocabulary().getData(),
                            VocabularyJson.class);
            VersionJson versionJson =
                    JSONSerialization.deserializeStringAsJson(
                            taskInfo.getVersion().getData(),
                            VersionJson.class);
            String repositoryID = TaskUtils.getSesameRepositoryId(
                    taskInfo);
            String versionID = versionJson.getTitle();
            String repositoryTitle = vocabularyJson.getTitle()
                    + " (Version: " + versionID + ")";

            Repository repository = manager.getRepository(repositoryID);
            if (repository != null) {
                // Already exists.
                // Possible future work: see if the vocabulary title
                // has changed in the database, and if so, update
                // the title in the Sesame repository.
                logger.debug("Sesame createRepository: already exists; "
                        + "reusing");
                return true;
            }

            // create a configuration for the SAIL stack
            SailImplConfig backendConfig;
            // NB: we had code here to examine the version title, and if
            // it was "current", to create an in-memory store.
            // That never happened in production, so now just always
            // create a native store.
            // Create a native store.
            boolean forceSync = true;
            NativeStoreConfig nativeConfig = new NativeStoreConfig();
            nativeConfig.setForceSync(forceSync);
            backendConfig = nativeConfig;

            // Stack an inferencer config on top of our backend-config.
            backendConfig =
                    new ForwardChainingRDFSInferencerConfig(backendConfig);

            // Create a configuration for the repository implementation.
            RepositoryImplConfig repositoryTypeSpec =
                  new SailRepositoryConfig(backendConfig);

            RepositoryConfig repConfig =
                  new RepositoryConfig(repositoryID, repositoryTitle,
                          repositoryTypeSpec);
            manager.addRepositoryConfig(repConfig);

            return true;
        } catch (RepositoryConfigException | RepositoryException e) {
            subtask.setStatus(TaskStatus.ERROR);
            subtask.addResult(TaskRunner.ERROR,
                    "Exception in Sesame createRepository()");
            logger.error("Exception in Sesame createRepository()", e);
        }
        return false;
    }

    /** Upload the RDF data into the Sesame repository.
     * @param taskInfo The TaskInfo object describing the entire task.
     * @param subtask The details of the subtask.
     * @return True, iff the upload succeeded.
     */
    public final boolean uploadRDF(final TaskInfo taskInfo,
            final Subtask subtask) {
        RepositoryManager manager = null;
        try {
            manager = RepositoryProvider.getRepositoryManager(sesameServer);

            String repositoryID = TaskUtils.getSesameRepositoryId(
                    taskInfo);

            Repository repository = manager.getRepository(repositoryID);
            if (repository == null) {
                // Repository is missing. This is bad.
                logger.error("Sesame uploadRDF, repository missing");
                return false;
            }

            RepositoryConnection con = null;
            try {
                con = repository.getConnection();
                // Default to removing all existing triples.
                String clearProperty = subtask.getSubtaskProperty(CLEAR);
                if (clearProperty == null
                        || BooleanUtils.toBoolean(clearProperty)) {
                    con.clear();
                }
                List<Path> pathsToProcess =
                        TaskUtils.getPathsToProcessForVersion(taskInfo);
                for (Path entry: pathsToProcess) {
                    try {
                        File file = new File(entry.toString());
                        logger.debug("Full path:"
                                + entry.toAbsolutePath().toString());
                        con.add(file, "",
                                Rio.getParserFormatForFileName(
                                        entry.toString()));
                    } catch (IOException ex) {
                        // I/O error encountered during the iteration,
                        // the cause is an IOException
                        subtask.setStatus(TaskStatus.ERROR);
                        subtask.addResult(TaskRunner.ERROR,
                                "Exception in Sesame uploadRDF");
                        logger.error("Exception in Sesame uploadRDF:", ex);
                        return false;
                    } catch (RDFParseException e) {
                        // Hmm, don't register an error, but keep going.
                        //    subtask.setStatus(TaskStatus.ERROR);
                        // But do log the parse error for this file.
                        subtask.addResult(PARSE_PREFIX + entry.getFileName(),
                                "Exception in Sesame uploadRDF");
                        logger.error("Sesame uploadRDF, error parsing RDF: ",
                                e);
                        return false;
                    }
                }
            } finally {
                if (con != null) {
                    con.close();
                }
            }
        } catch (RepositoryConfigException | RepositoryException e) {
            subtask.setStatus(TaskStatus.ERROR);
            subtask.addResult(TaskRunner.ERROR,
                    "Exception in Sesame uploadRDF");
            logger.error("Exception in Sesame uploadRDF()", e);
            return false;
        }
        return true;
    }

    /** Remove the Sesame repository and access points for the version.
     * @param taskInfo The top-level TaskInfo for the subtask.
     * @param subtask The subtask to be performed.
     */
    public final void unimport(final TaskInfo taskInfo,
            final Subtask subtask) {
        // Remove the sesameDownload access point.
        List<AccessPoint> aps = AccessPointDAO.
                getCurrentAccessPointListForVersionByType(
                        taskInfo.getVersion().getVersionId(),
                        AccessPointType.SESAME_DOWNLOAD,
                        taskInfo.getEm());
        for (AccessPoint ap : aps) {
            if (ap.getSource() == ApSource.SYSTEM) {
                TemporalUtils.makeHistorical(ap, taskInfo.getNowTime());
                AccessPointDAO.updateAccessPoint(taskInfo.getEm(), ap);
            }
        }
        // Remove the apiSparql access point.
        aps = AccessPointDAO.
                getCurrentAccessPointListForVersionByType(
                        taskInfo.getVersion().getVersionId(),
                        AccessPointType.API_SPARQL,
                        taskInfo.getEm());
        for (AccessPoint ap : aps) {
            if (ap.getSource() == ApSource.SYSTEM) {
                TemporalUtils.makeHistorical(ap, taskInfo.getNowTime());
                AccessPointDAO.updateAccessPoint(taskInfo.getEm(), ap);
            }
        }
        // Remove the repository from the Sesame server.
        RepositoryManager manager = null;
        try {
            manager = RepositoryProvider.getRepositoryManager(sesameServer);
            String repositoryID = TaskUtils.getSesameRepositoryId(
                    taskInfo);
            Repository repository = manager.getRepository(repositoryID);
            if (repository == null) {
                // No such repository; nothing to do.
                 logger.debug("Sesame unimport: nothing to do.");
                return;
            }
            manager.removeRepository(repositoryID);
            // Seems to be necessary to invoke refresh() to make
            // the manager "forget" about the repository.
            // Without it, if you immediately reimport,
            // createRepository's call to getRepository() wrongly reports
            // that the repository already exists, and the subsequent
            // importing of data fails.
            manager.refresh();
            // If we're still here, success, so return true.
            return;
        } catch (RepositoryConfigException | RepositoryException e) {
            subtask.setStatus(TaskStatus.ERROR);
            subtask.addResult(TaskRunner.ERROR,
                    "Exception in Sesame unimport");
            logger.error("Exception in Sesame unimport", e);
        }
        return;
    }

    /** {@inheritDoc} */
    @Override
    public Integer defaultPriority(final SubtaskOperationType operationType) {
        switch (operationType) {
        case INSERT:
        case PERFORM:
            return DefaultPriorities.DEFAULT_IMPORTER_INSERT_PRIORITY;
        case DELETE:
            return DefaultPriorities.DEFAULT_IMPORTER_DELETE_PRIORITY;
        default:
            // Unknown operation type!
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void doSubtask(
            final au.org.ands.vocabs.registry.workflow.tasks.TaskInfo taskInfo,
            final Subtask subtask) {
        switch (subtask.getOperation()) {
        case INSERT:
        case PERFORM:
            doImport(taskInfo, subtask);
            break;
        case DELETE:
            unimport(taskInfo, subtask);
            break;
        default:
            logger.error("Unknown operation!");
           break;
        }

    }

}
