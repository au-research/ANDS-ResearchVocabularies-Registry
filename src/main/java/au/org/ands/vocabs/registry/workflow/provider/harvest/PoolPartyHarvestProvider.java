/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.workflow.provider.harvest;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.editor.admin.model.PoolPartyProject;
import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.dao.PoolPartyServerDAO;
import au.org.ands.vocabs.registry.db.dao.VersionArtefactDAO;
import au.org.ands.vocabs.registry.db.entity.PoolPartyServer;
import au.org.ands.vocabs.registry.db.entity.VersionArtefact;
import au.org.ands.vocabs.registry.enums.SubtaskOperationType;
import au.org.ands.vocabs.registry.enums.TaskStatus;
import au.org.ands.vocabs.registry.enums.VersionArtefactType;
import au.org.ands.vocabs.registry.utils.PoolPartyUtils;
import au.org.ands.vocabs.registry.utils.PropertyConstants;
import au.org.ands.vocabs.registry.utils.RDFUtils;
import au.org.ands.vocabs.registry.utils.RegistryFileUtils;
import au.org.ands.vocabs.registry.utils.RegistryNetUtils;
import au.org.ands.vocabs.registry.utils.RegistryProperties;
import au.org.ands.vocabs.registry.workflow.provider.DefaultPriorities;
import au.org.ands.vocabs.registry.workflow.provider.WorkflowProvider;
import au.org.ands.vocabs.registry.workflow.provider.transform.GetMetadataTransformProvider;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;
import au.org.ands.vocabs.registry.workflow.tasks.TaskRunner;
import au.org.ands.vocabs.registry.workflow.tasks.TaskUtils;
import au.org.ands.vocabs.registry.workflow.tasks.VersionArtefactUtils;

/** Harvest provider for PoolParty. */
public class PoolPartyHarvestProvider implements WorkflowProvider {

    /** The name of the subtask property in which to provide the
     * PoolParty server Id. */
    public static final String SERVER_ID = "serverId";

    /** The name of the subtask property in which to provide the
     * PoolParty project Id. */
    public static final String PROJECT_ID = "projectId";

    /** The logger for this class. */
    private final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Get status information about the PoolParty server.
     * @param ppServerId The PoolParty server id.
     * @return Status information: a list of the projects on the server.
     */
    public final String getInfo(final Integer ppServerId) {
        PoolPartyServer poolPartyServer =
                PoolPartyServerDAO.getPoolPartyServerById(ppServerId);
        if (poolPartyServer == null) {
            return "PoolParty server " + ppServerId + " undefined.";
        }
        String remoteUrl = poolPartyServer.getApiUrl();
        String username = poolPartyServer.getUsername();
        String password = poolPartyServer.getPassword();

        logger.debug("Getting metadata from " + remoteUrl);

        Client client = RegistryNetUtils.getClient();
        WebTarget target = client.target(remoteUrl).
                path(PoolPartyUtils.API_PROJECTS);
        HttpAuthenticationFeature feature =
                HttpAuthenticationFeature.basic(username, password);
        target.register(feature);

        Invocation.Builder invocationBuilder =
                target.request(MediaType.APPLICATION_JSON);

        Response response = invocationBuilder.get();

        // This is how you do it if you want to parse the JSON.
        //            InputStream is = response.readEntity(InputStream.class);
        //            JsonReader jsonReader = Json.createReader(is);
        //            JsonStructure jsonStructure = jsonReader.readArray();
        //            return jsonStructure.toString();

        // Oops, for tidyness, should close the Response object here.
        // But the Jersey implementation closes the underlying
        // resources as part of readEntity().
        return response.readEntity(String.class);
    }

    /** Template for a SPARQL CONSTRUCT Query to get the full names of users
     * mentioned in a PoolParty project. Each such piece of information
     * is returned as two triples, of the form
     * {@code ?agent a dcterms:Agent; foaf:name ?fullname}.
     * Why use FOAF? (a) To follow the examples given at
     * <a href=
     * "http://wiki.dublincore.org/index.php/User_Guide/Publishing_Metadata">
     * http://wiki.dublincore.org/index.php/User_Guide/Publishing_Metadata</a>.
     * (b) In fact, the ADMS graph also uses {@code foaf:name}
     * for this purpose.
     */
    private static final String GET_USER_FULLNAMES_TEMPLATE =
        "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
      + "PREFIX dcterms: <http://purl.org/dc/terms/>\n"
      + "CONSTRUCT { ?agent a dcterms:Agent; foaf:name ?fullname }\n"
      + "FROM #THESAURUS#\n"
      + "FROM #METADATA/void#\n"
      + "FROM NAMED #THESAURUS/users#\n"
      + "WHERE {\n"
      + "  {\n"
      + "    ?s ?p ?agent\n"
      + "    FILTER (STRSTARTS(STR(?p), \"http://purl.org/dc/terms/\"))\n"
      + "  }\n"
      + "  GRAPH #THESAURUS/users# {\n"
      + "    ?agent <http://schema.semantic-web.at/users/username> ?fullname\n"
      + "  }\n"
      + "}\n";

    /** Template for a SPARQL CONSTRUCT Query to get the deprecated
     * concepts of a PoolParty project.
     */
    private static final String GET_DEPRECATED_CONCEPTS_TEMPLATE =
        "CONSTRUCT { ?s ?p ?o }\n"
      + "FROM #THESAURUS/deprecated#\n"
      + "WHERE {\n"
      + "  ?s ?p ?o"
      + "  FILTER (?p != rdf:type)\n"
      + "}\n";

    /** Do a harvest. Update the message parameter with the result
     * of the harvest.
     * @param ppServerId The PoolParty server id.
     * @param ppProjectId The PoolParty project id.
     * @param outputDir The directory in which to store output files.
     * @param getMetadata Whether or not to get ADMS and VoID metadata.
     * @param taskInfo The TaskInfo object for this task.
     * @param subtask The specification of this harvest subtask.
     * @return True, iff the harvest succeeded.
     */
    @SuppressWarnings("checkstyle:MethodLength")
    public final boolean getHarvestFiles(
            final Integer ppServerId,
            final String ppProjectId,
            final String outputDir,
            final boolean getMetadata,
            final TaskInfo taskInfo,
            final Subtask subtask) {
        PoolPartyServer poolPartyServer =
                PoolPartyServerDAO.getPoolPartyServerById(ppServerId);
        if (poolPartyServer == null) {
            logger.error("Attempt to harvest from PoolParty server "
                    + "with Id " + ppServerId
                    + ", but no such server defined.");
            subtask.setStatus(TaskStatus.ERROR);
            subtask.addResult(TaskRunner.ERROR,
                    "Attempt to harvest from PoolParty server "
                            + "with Id " + ppServerId
                            + ", but no such server defined.");
            return false;
        }
        String remoteUrl = poolPartyServer.getApiUrl();
        String username = poolPartyServer.getUsername();
        String password = poolPartyServer.getPassword();

        String format = RegistryProperties.getProperty(
                PropertyConstants.POOLPARTYHARVESTER_DEFAULTFORMAT);

// Possible future work: support specifying particular modules.
//        List<String> exportModules =
//                info.getQueryParameters().get("exportModules");
        List<String> exportModules = new ArrayList<>();
        exportModules.add(RegistryProperties.getProperty(
                PropertyConstants.POOLPARTYHARVESTER_DEFAULTEXPORTMODULE));
        if (getMetadata) {
            exportModules.add("adms");
            exportModules.add("void");
        }

        if (getMetadata) {
            // When fetching metadata, don't leave anything left over from
            // a previous fetch. The significance of/need for this was
            // found when changing the Toolkit property
            // PoolPartyHarvester.defaultFormat: the old files corresponding
            // to the previous setting were left.
            FileUtils.deleteQuietly(new File(outputDir));
        }

        logger.debug("Getting project from " + remoteUrl);

        Client client = RegistryNetUtils.getClient();

        // Tip: in case you ever need to debug the traffic to/from
        // PoolParty, uncomment the following. You'll need to add an import
        // for the class org.glassfish.jersey.filter.LoggingFilter.
        // (NB: works with Jersey 2.22.1. For later releases, use
        // LoggingFeature instead.)
//        client.register(new LoggingFilter(
//                java.util.logging.Logger.getGlobal(), true));

        WebTarget target = client.target(remoteUrl).
                path(PoolPartyUtils.API_PROJECTS);
        HttpAuthenticationFeature feature =
                HttpAuthenticationFeature.basic(username, password);
        WebTarget plainTarget = target.register(feature)
                .path(ppProjectId)
                .path("export");

        // Convenience access to outputDir as a Path.
        Path outputDirPath = Paths.get(outputDir);

        for (String exportModule : exportModules) {
            logger.debug("Harvesting from " + plainTarget.toString());

            Invocation.Builder invocationBuilder =
                    plainTarget.request(MediaType.APPLICATION_XML);

            // Since PoolParty API version 7.1, the parameters
            // are sent in the body of a POST, in JSON format.
            JsonObjectBuilder job = Json.createObjectBuilder();
            job.add("format", format);
            job.add("modules", Json.createArrayBuilder().add(exportModule));
            // API documentation now says that the prettyPrint parameter is
            // required, but that seems to be incorrect. Provide a
            // value anyway. We used to get the default value of false,
            // and that seemed to work OK for us, so continue to specify
            // false.
            job.add("prettyPrint", false);
            // It's necessary to use job.build().toString(), not just
            // job.build(), because otherwise you get extra metadata
            // in the generated String, e.g.,
            // {"format":{"valueType":"STRING","chars":"Turtle",
            //            "string":"Turtle"}, ... etc.
            Response response = invocationBuilder.post(
                    Entity.json(job.build().toString()));

            if (response.getStatus()
                    >= Response.Status.BAD_REQUEST.getStatusCode()) {
                logger.error("getHarvestFiles got an error from PoolParty; "
                        + "response code = " + response.getStatus());

                subtask.setStatus(TaskStatus.ERROR);
                subtask.addResult(TaskRunner.ERROR,
                        "PoolPartyHarvestProvider.getHarvestFiles() "
                        + "got an error from PoolParty; "
                        + "response code = " + response.getStatus());
                return false;
            }

            String responseData = response.readEntity(String.class);

            String filePath = RegistryFileUtils.saveRDFToFile(outputDir,
                    exportModule, format, responseData);
            if (taskInfo != null) {
                VersionArtefactUtils.createPoolpartyHarvestVersionArtefact(
                        taskInfo, filePath);
            }

            // Clean up Response object.
            response.close();
        }

        // In order to get content contained in the users named graph,
        // we have to do a SPARQL query. But to do that, we need
        // to use an API call, for which the URL contains the
        // project's "uriSupplement". But to get that, we have
        // to use the API call to get the top-level metadata of
        // _all_ projects! And then we do a search in the result
        // to find this particular project.
        PoolPartyProject[] poolPartyProjects =
                PoolPartyUtils.getPoolPartyProjects();
        // We get back an unsorted array, so a linear search is called for.
        int projectsLength = poolPartyProjects.length;
        int projectIndex = 0;
        boolean found = false;
        while (projectIndex < projectsLength) {
            if (ppProjectId.equals(poolPartyProjects[projectIndex].getId())) {
                // Found it.
                found = true;
                break;
            }
            projectIndex++;
        }
        if (!found) {
            logger.error("getHarvestFiles was unable to get project metadata, "
                    + "even after an export! This should not happen!");
            return false;
        }
        // If not only fetching metadata, get deprecated concepts.
        if (!getMetadata) {
            if (!fetchDataUsingQuery(outputDirPath,
                    poolPartyServer,
                    poolPartyProjects[projectIndex],
                    GET_DEPRECATED_CONCEPTS_TEMPLATE,
                    "deprecated" + RDFUtils.FORMAT_TO_FILEEXT_MAP.get(
                            format.toLowerCase(Locale.ROOT)),
                    RDFUtils.getRDFFormatForName(format).
                        getDefaultMIMEType(),
                    taskInfo, subtask)) {
                return false;
            }
        }
        // Get data from users graph.
        if (!fetchDataUsingQuery(outputDirPath,
                poolPartyServer,
                poolPartyProjects[projectIndex],
                GET_USER_FULLNAMES_TEMPLATE,
                GetMetadataTransformProvider.USERS_GRAPH_FILE,
                GetMetadataTransformProvider.USERS_GRAPH_FORMAT.
                    getDefaultMIMEType(),
                taskInfo, subtask)) {
            return false;
        }

        return true;
    }

    /** Fetch data from a PoolParty project using a SPARQL query,
     * and save the results to a file.
     * Sorry that this method has too many parameters.
     * @param outputDirPath The Path to the output directory to use.
     * @param poolPartyServer The PoolParty server; must be non-null.
     * @param poolPartyProject The PoolParty project to run the query
     *      against.
     * @param queryTemplate The template of the SPARQL query to run.
     * @param outputFile The base name of the output file to use.
     * @param outputFileMimeType The MIME type to send as the
     *      requested response type.
     * @param taskInfo The TaskInfo object for this task.
     * @param subtask The specification of this harvest subtask.
     * @return True, iff the fetch succeeded.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    private boolean fetchDataUsingQuery(final Path outputDirPath,
            final PoolPartyServer poolPartyServer,
            final PoolPartyProject poolPartyProject,
            final String queryTemplate,
            final String outputFile,
            final String outputFileMimeType,
            final TaskInfo taskInfo,
            final Subtask subtask) {
        String queryResults = PoolPartyUtils.runQuery(
                poolPartyServer,
                poolPartyProject,
                queryTemplate,
                outputFileMimeType);
        File queryResultsFile = new File(outputDirPath.
                resolve(outputFile).
                toString());
        try {
            logger.info("fetchDataUsingQuery: "
                    + queryResultsFile.getAbsolutePath());
            FileUtils.writeStringToFile(queryResultsFile, queryResults,
                    StandardCharsets.UTF_8);
            if (taskInfo != null) {
                VersionArtefactUtils.createPoolpartyHarvestVersionArtefact(
                        taskInfo, queryResultsFile.getAbsolutePath());
            }
            return true;
        } catch (IOException e) {
            subtask.setStatus(TaskStatus.ERROR);
            subtask.addResult(TaskRunner.ERROR,
                    "PoolPartyHarvester: can't write result of fetching "
                    + " data with SPARQL.");
            subtask.addResult(TaskRunner.STACKTRACE,
                    ExceptionUtils.getStackTrace(e));
            logger.error("PoolPartyHarvester fetchDataUsingQuery: "
                    + "can't write results to file.",
                    e);
            return false;
        }
    }

    /** Do a harvest from PoolParty for this version.
     * @param taskInfo The TaskInfo object describing the entire task.
     * @param subtask The details of the subtask.
     */
    public final void harvest(final TaskInfo taskInfo, final Subtask subtask) {
        if (subtask.getSubtaskProperty(SERVER_ID) == null
                || subtask.getSubtaskProperty(PROJECT_ID) == null) {
            logger.error("Request for PoolParty harvest, but project "
                    + "specification missing or incomplete.");
            subtask.setStatus(TaskStatus.ERROR);
            subtask.addResult(TaskRunner.ERROR, "No project specified");
            return;
        }

        // Clean out any current version artefacts first.
        makeVersionArtefactsHistorical(taskInfo);
        // And harvest again.
        boolean harvestSuccess = getHarvestFiles(
                Integer.parseInt(subtask.getSubtaskProperty(SERVER_ID)),
                subtask.getSubtaskProperty(PROJECT_ID),
                TaskUtils.getTaskHarvestOutputPath(taskInfo, true),
                false, taskInfo, subtask);
        if (harvestSuccess) {
            subtask.setStatus(TaskStatus.SUCCESS);
        }
    }

    /** Remove any files harvested from PoolParty for this version.
     * @param taskInfo The TaskInfo object describing the entire task.
     * @param subtask The details of the subtask.
     */
    public final void unharvest(final TaskInfo taskInfo,
            final Subtask subtask) {
        // Remove the harvest version artefacts.
        makeVersionArtefactsHistorical(taskInfo);
        subtask.setStatus(TaskStatus.SUCCESS);
    }

    /** Make any currently-valid version artefacts historial.
     * @param taskInfo The TaskInfo object describing the entire task.
     */
    private void makeVersionArtefactsHistorical(final TaskInfo taskInfo) {
        List<VersionArtefact> vas = VersionArtefactDAO.
                getCurrentVersionArtefactListForVersionByType(
                        taskInfo.getVersion().getVersionId(),
                        VersionArtefactType.HARVEST_POOLPARTY,
                        taskInfo.getEm());
        for (VersionArtefact va : vas) {
            // We _don't_ delete the file. But if we did:
            /*
            VaHarvestPoolparty vaHarvestPoolparty =
                    JSONSerialization.deserializeStringAsJson(
                            va.getData(), VaHarvestPoolparty.class);
            Files.deleteIfExists(Paths.get(vaHarvestPoolparty.getPath()));
            */
            TemporalUtils.makeHistorical(va, taskInfo.getNowTime());
            va.setModifiedBy(taskInfo.getModifiedBy());
            VersionArtefactDAO.updateVersionArtefact(taskInfo.getEm(), va);
        }
    }

    /** Get metadata for a PoolParty project.
     * @param serverId The PoolParty server Id.
     * @param projectId The PoolParty Project Id.
     * @return The result of getting the metadata.
     */
    public final Map<String, String> getMetadata(
            final Integer serverId, final String projectId) {
        Subtask subtask = new Subtask();

        String outputDirectory = TaskUtils.getMetadataOutputPath(projectId);
        getHarvestFiles(serverId, projectId, outputDirectory,
                true, null, subtask);
        return subtask.getResults();
    }

    /** {@inheritDoc} */
    @Override
    public Integer defaultPriority(final SubtaskOperationType operationType) {
        switch (operationType) {
        case INSERT:
            return DefaultPriorities.DEFAULT_HARVEST_INSERT_PRIORITY;
        case DELETE:
            return DefaultPriorities.DEFAULT_HARVEST_DELETE_PRIORITY;
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
            harvest(taskInfo, subtask);
            break;
        case DELETE:
            unharvest(taskInfo, subtask);
            break;
        default:
            logger.error("Unknown operation!");
           break;
        }
    }

}
