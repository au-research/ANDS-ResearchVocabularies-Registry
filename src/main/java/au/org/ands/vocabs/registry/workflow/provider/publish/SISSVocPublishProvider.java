/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.workflow.provider.publish;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.dao.AccessPointDAO;
import au.org.ands.vocabs.registry.db.entity.AccessPoint;
import au.org.ands.vocabs.registry.db.internal.ApSissvoc;
import au.org.ands.vocabs.registry.db.internal.VocabularyJson;
import au.org.ands.vocabs.registry.enums.AccessPointType;
import au.org.ands.vocabs.registry.enums.ApSource;
import au.org.ands.vocabs.registry.enums.SubtaskOperationType;
import au.org.ands.vocabs.registry.enums.TaskStatus;
import au.org.ands.vocabs.registry.utils.PropertyConstants;
import au.org.ands.vocabs.registry.utils.RegistryFileUtils;
import au.org.ands.vocabs.registry.utils.RegistryNetUtils;
import au.org.ands.vocabs.registry.utils.RegistryProperties;
import au.org.ands.vocabs.registry.workflow.provider.DefaultPriorities;
import au.org.ands.vocabs.registry.workflow.provider.WorkflowProvider;
import au.org.ands.vocabs.registry.workflow.tasks.AccessPointUtils;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;
import au.org.ands.vocabs.registry.workflow.tasks.TaskRunner;
import au.org.ands.vocabs.registry.workflow.tasks.TaskUtils;

/** SISSVoc publish provider. */
public class SISSVocPublishProvider implements WorkflowProvider {

    /** Logger for this class. */
    private final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Key of the subtask object that contains additional
     * replacements to use in the spec file template.
     */
    private static final String SPEC_SETTINGS_KEY = "spec_settings";

    /** The location of the spec file template. */
    private String sissvocSpecTemplatePath = RegistryProperties.getProperty(
            PropertyConstants.SISSVOC_SPECTEMPLATE);

    /** The directory in which to write generated spec files. */
    private String sissvocSpecOutputPath = RegistryProperties.getProperty(
            PropertyConstants.SISSVOC_SPECSPATH);

    /** URL that is a prefix to all SISSVoc endpoints. */
    private String sissvocEndpointsPrefix = RegistryProperties.getProperty(
            PropertyConstants.SISSVOC_ENDPOINTSPREFIX);

    /** Values to be substituted in the spec file template. */
    private final HashMap<String, String> specProperties =
            new HashMap<>();

    /** The full path of the generated spec file. */
    private String path;

    /** Create/update the SISSVoc spec file and access point for the version.
     * @param taskInfo The top-level TaskInfo for the subtask.
     * @param subtask The subtask to be performed.
     */
    public final void publish(final TaskInfo taskInfo, final Subtask subtask) {
        addBasicSpecProperties(taskInfo);
        addAdditionalSpecProperties(subtask);
        if (!writeSpecFile(taskInfo, subtask)) {
            return;
        }

        // Use the nice JAX-RS libraries to construct the path to
        // the SPARQL endpoint.
        Client client = RegistryNetUtils.getClient();
        WebTarget target = client.target(sissvocEndpointsPrefix);
        WebTarget sparqlTarget = target
                .path(TaskUtils.getSISSVocRepositoryPath(taskInfo));
//        subtask.addResult("sissvoc_endpoints",
//                sparqlTarget.getUri().toString());
        // Add SISSVoc endpoint.
        AccessPointUtils.createSissvocAccessPoint(taskInfo, path,
                sparqlTarget.getUri().toString());
        subtask.setStatus(TaskStatus.SUCCESS);
    }

    /** Remove the SISSVoc spec file and access point for the version.
     * @param taskInfo The top-level TaskInfo for the subtask.
     * @param subtask The subtask to be performed.
     */
    public final void unpublish(final TaskInfo taskInfo,
            final Subtask subtask) {
        // Remove the SISSVoc access point.
        boolean removed = false;
        List<AccessPoint> aps = AccessPointDAO.
                getCurrentAccessPointListForVersionByType(
                        taskInfo.getVersion().getVersionId(),
                        AccessPointType.SISSVOC,
                        taskInfo.getEm());
        for (AccessPoint ap : aps) {
            if (ap.getSource() == ApSource.SYSTEM) {
                TemporalUtils.makeHistorical(ap, taskInfo.getNowTime());
                ap.setModifiedBy(taskInfo.getModifiedBy());
                AccessPointDAO.updateAccessPoint(taskInfo.getEm(), ap);
                ApSissvoc apSissvoc =
                        JSONSerialization.deserializeStringAsJson(ap.getData(),
                                ApSissvoc.class);
                String existingPath = apSissvoc.getPath();
                if (StringUtils.isNotBlank(existingPath)) {
                    truncateSpecFileIfExists(subtask, existingPath);
                    removed = true;
                }
            }
        }

        // Use the following version when the elda library
        // supports it.
        //        removeSpecFile(taskInfo, subtask, results);
        // For now, use the truncation method.
        // Have we not removed the file? Fall back to computing the
        // filename of the spec file.
        if (!removed) {
            truncateSpecFileIfExists(taskInfo, subtask);
        }
        // truncateSpecFileIfExists() may report an error; don't
        // lose that.
        if (subtask.getStatus() != TaskStatus.ERROR) {
            subtask.setStatus(TaskStatus.SUCCESS);
        }
    }


    /** Add the essential properties required by the spec file template.
     * @param taskInfo The TaskInfo object for this task.
     */
    private void addBasicSpecProperties(final TaskInfo taskInfo) {
        VocabularyJson vocabularyJson =
                JSONSerialization.deserializeStringAsJson(
                        taskInfo.getVocabulary().getData(),
                        VocabularyJson.class);
        // Top-level of deployment path
        specProperties.put("DEPLOYPATH",
                RegistryProperties.getProperty(
                        PropertyConstants.SISSVOC_VARIABLE_DEPLOYPATH,
                        "/repository/api/lda"));
        // The name of the ANDS Vocabulary service
        specProperties.put("SERVICE_TITLE",
                RegistryProperties.getProperty(
                        PropertyConstants.SISSVOC_VARIABLE_SERVICE_TITLE,
                        "ANDS Vocabularies LDA service"));
        // The name of the ANDS Vocabulary service owner
        specProperties.put("SERVICE_AUTHOR",
                RegistryProperties.getProperty(
                        PropertyConstants.SISSVOC_VARIABLE_SERVICE_AUTHOR,
                        "ANDS Services"));
        // Contact email address for the ANDS Vocabulary service owner
        specProperties.put("SERVICE_AUTHOR_EMAIL",
                RegistryProperties.getProperty(
                        PropertyConstants.SISSVOC_VARIABLE_SERVICE_AUTHOR_EMAIL,
                        "services@ands.org.au"));
        // Homepage of the ANDS Vocabulary service
        // ANDS home page for now; in future, could be
        // vocabs.ands.org.au itself.
        specProperties.put("SERVICE_HOMEPAGE",
                RegistryProperties.getProperty(
                        PropertyConstants.SISSVOC_VARIABLE_SERVICE_HOMEPAGE,
                        "http://www.ands.org.au/"));
        // Vocabulary title
        specProperties.put("SERVICE_LABEL",
                StringEscapeUtils.escapeJava(
                        vocabularyJson.getTitle()));
        String repositoryId = TaskUtils.getSesameRepositoryId(taskInfo);
        // SPARQL endpoint to use for doing queries
        specProperties.put("SPARQL_ENDPOINT",
                RegistryProperties.getProperty(
                    PropertyConstants.SISSVOC_VARIABLE_SPARQL_ENDPOINT_PREFIX,
                        "http://localhost:8080/repository/"
                                + "openrdf-sesame/repositories/")
                                + repositoryId);
        specProperties.put("SVC_ID", repositoryId);
        // Additional path to all the endpoints for this repository.
        // The template assumes the variable begins with a slash.
        specProperties.put("SVC_PREFIX",
                "/" + TaskUtils.getSISSVocRepositoryPath(taskInfo));
        // Path to the XSL stylesheet that generates the HTML pages.
        // Path is relative to the SISSVoc webapp.
        specProperties.put("HTML_STYLESHEET",
                RegistryProperties.getProperty(
                        PropertyConstants.SISSVOC_VARIABLE_HTML_STYLESHEET,
                        "resources/default/transform/ands-ashtml-sissvoc.xsl"));
        // Empty string for now
        specProperties.put("NAMESPACES", "");
        // Title of the vocab displayed at the top of HTML pages
        specProperties.put("ANDS_VOCABNAME",
                StringEscapeUtils.escapeJava(
                        vocabularyJson.getTitle()));
        // Add more properties here, if/when needed.
//        specProperties.put("", "");
        // The above properties are all more-or-less required.
        // Below are properties that are optional, and
        // may be overridden by the subtask settings.
        specProperties.put("ANDS_VOCABMORE", "");
        specProperties.put("ANDS_VOCABAPIDOCO", "");
    }

    /** Add the additional properties as provided in the subtask
     * specification. Values are escaped using StringEscapeUtils.escapeJava()
     * to prevent nasty injection.
     * @param subtask The specification of this publish subtask
     */
    private void addAdditionalSpecProperties(final Subtask subtask) {
//      Map<String, String> subtaskProperties = subtask.getSubtaskProperties();
        if (subtask.getSubtaskProperty(SPEC_SETTINGS_KEY) == null) {
            // No additional properties specified.
            return;
        }
        // Possible future work: support this by, e.g.,
        // having SPEC_SETTINGS_KEY's value be a comma-separated
        // list of _other_ keys in the properties.
        /*
        for (Iterator<Entry<String, JsonNode>> nodeIterator =
                subtaskProperties.get(SPEC_SETTINGS_KEY).fields();
                nodeIterator.hasNext();) {
            Entry<String, JsonNode> specProperty = nodeIterator.next();
            logger.debug("addAdditionalSpecProperties replacing with"
                    + " value: " + specProperty.getValue().textValue());
            specProperties.put(
                    specProperty.getKey(),
                    StringEscapeUtils.escapeJava(
                            specProperty.getValue().textValue()));
        }
        */
    }

    /** Write out the spec file for SISSVoc.
     * @param taskInfo The TaskInfo object for this task.
     * @param subtask The specification of this publish subtask.
     * @return True iff success.
     */
    private boolean writeSpecFile(final TaskInfo taskInfo,
            final Subtask subtask) {
        File templateFile = new File(sissvocSpecTemplatePath);
        String specTemplate;
        try {
            specTemplate = FileUtils.readFileToString(templateFile,
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            subtask.setStatus(TaskStatus.ERROR);
            subtask.addResult(TaskRunner.ERROR,
                    "SISSVoc writeSpecFile: can't open template file");
            logger.error("SISSVoc writeSpecFile: can't open template file",
                    e);
            return false;
        }
        StrSubstitutor sub = new StrSubstitutor(specProperties);
        String customSpec = sub.replace(specTemplate);
        RegistryFileUtils.requireDirectory(sissvocSpecOutputPath);
        path = Paths.get(sissvocSpecOutputPath).
                resolve(TaskUtils.getSesameRepositoryId(taskInfo)
                        + ".ttl").toString();
        File specFile = new File(path);
        try {
            FileUtils.writeStringToFile(specFile, customSpec,
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            subtask.setStatus(TaskStatus.ERROR);
            subtask.addResult(TaskRunner.ERROR,
                    "SISSVoc writeSpecFile: can't write spec file");
            logger.error("SISSVoc writeSpecFile: can't write spec file.",
                    e);
            return false;
        }
        return true;
    }

    /** If there is an existing spec file for SISSVoc, overwrite
     * it and truncate it to zero size. This is the workaround
     * for unpublication until the elda library supports detection
     * of deleted files. Use this version of the method when a
     * file path is recorded in the access point.
     * @param subtask The subtask to be performed.
     * @param existingPath The full path to the spec file, as recorded
     *      in the access point.
     */
    private void truncateSpecFileIfExists(final Subtask subtask,
            final String existingPath) {
        try {
            Path specFilePath = Paths.get(existingPath);
            if (Files.exists(specFilePath)) {
                Files.write(specFilePath, new byte[0]);
            }

        } catch (IOException e) {
            // This may mean a file permissions problem, so do log it.
            subtask.setStatus(TaskStatus.ERROR);
            subtask.addResult(TaskRunner.ERROR,
                    "SISSVoc truncateSpecFileIfExists: failed");
            logger.error("truncateSpecFileIfExists failed", e);
        }
    }

    /** If there is an existing spec file for SISSVoc, overwrite
     * it and truncate it to zero size. This is the workaround
     * for unpublication until the elda library supports detection
     * of deleted files. Use this version of the method when no
     * file path is recorded in the access point, and it must
     * be computed from the TaskInfo data.
     * @param taskInfo The TaskInfo object for this task.
     * @param subtask The subtask to be performed.
     */
    private void truncateSpecFileIfExists(final TaskInfo taskInfo,
            final Subtask subtask) {
        try {
            Path specFilePath = Paths.get(sissvocSpecOutputPath).
                    resolve(TaskUtils.getSesameRepositoryId(taskInfo)
                            + ".ttl");
            if (Files.exists(specFilePath)) {
                Files.write(specFilePath, new byte[0]);
            }

        } catch (IOException e) {
            // This may mean a file permissions problem, so do log it.
            subtask.setStatus(TaskStatus.ERROR);
            subtask.addResult(TaskRunner.ERROR,
                    "SISSVoc truncateSpecFileIfExists: failed");
            logger.error("truncateSpecFileIfExists failed", e);
        }
    }

    /** Remove any existing spec file for SISSVoc.
     * @param taskInfo The TaskInfo object for this task.
     * @param subtask The specification of this publish subtask.
     * @return True iff success.
     */
    @SuppressWarnings("unused")
    private boolean removeSpecFile(final TaskInfo taskInfo,
            final Subtask subtask) {
        try {
            Files.deleteIfExists(Paths.get(sissvocSpecOutputPath).
                    resolve(TaskUtils.getSesameRepositoryId(taskInfo)
                            + ".ttl"));
        } catch (IOException e) {
            // This may mean a file permissions problem, so do log it.
            subtask.setStatus(TaskStatus.ERROR);
            subtask.addResult(TaskRunner.ERROR,
                    "SISSVoc removeSpecFile: failed");
            logger.error("removeSpecFile failed", e);
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Integer defaultPriority(final SubtaskOperationType operationType) {
        switch (operationType) {
        case INSERT:
        case PERFORM:
            return DefaultPriorities.DEFAULT_PUBLISH_INSERT_PRIORITY;
        case DELETE:
            return DefaultPriorities.DEFAULT_PUBLISH_DELETE_PRIORITY;
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
            publish(taskInfo, subtask);
            break;
        case DELETE:
            unpublish(taskInfo, subtask);
            break;
        default:
            logger.error("Unknown operation!");
           break;
        }
    }

}
