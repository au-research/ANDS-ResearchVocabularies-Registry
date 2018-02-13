/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.workflow.provider.transform;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.openrdf.model.Statement;
import org.openrdf.model.vocabulary.SKOS;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.UnsupportedRDFormatException;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.dao.VersionArtefactDAO;
import au.org.ands.vocabs.registry.db.entity.VersionArtefact;
import au.org.ands.vocabs.registry.enums.SubtaskOperationType;
import au.org.ands.vocabs.registry.enums.TaskStatus;
import au.org.ands.vocabs.registry.enums.VersionArtefactType;
import au.org.ands.vocabs.registry.workflow.provider.DefaultPriorities;
import au.org.ands.vocabs.registry.workflow.provider.WorkflowProvider;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;
import au.org.ands.vocabs.registry.workflow.tasks.TaskRunner;
import au.org.ands.vocabs.registry.workflow.tasks.TaskUtils;
import au.org.ands.vocabs.registry.workflow.tasks.VersionArtefactUtils;

/** Transform provider for generating a list-like representation of the
 * concepts as JSON. This assumes a vocabulary encoded using SKOS. */
public class JsonListTransformProvider implements WorkflowProvider {

    /** Prefix for keys used for results that say that a file could
     * not be parsed. */
    public static final String PARSE_PREFIX = "parse-";

    /** Logger for this class. */
    private final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Key used for storing a SKOS prefLabel. */
    public static final String PREF_LABEL = "prefLabel";

    /** Create/update the JsonList version artefact for the version.
     * @param taskInfo The top-level TaskInfo for the subtask.
     * @param subtask The subtask to be performed.
     */
    public final void transform(final TaskInfo taskInfo,
            final Subtask subtask) {
        ConceptHandler conceptHandler = new ConceptHandler();
        List<Path> pathsToProcess =
                TaskUtils.getPathsToProcessForVersion(taskInfo);
        for (Path entry: pathsToProcess) {
            try {
                RDFFormat format = Rio.getParserFormatForFileName(
                        entry.toString());
                RDFParser rdfParser = Rio.createParser(format);
                rdfParser.setRDFHandler(conceptHandler);
                logger.debug("Reading RDF:" + entry.toString());
                FileInputStream is = new FileInputStream(entry.toString());
                rdfParser.parse(is, entry.toString());
            } catch (DirectoryIteratorException
                    | IOException
                    | RDFParseException
                    | RDFHandlerException
                    | UnsupportedRDFormatException ex) {
                // Hmm, don't register an error, but keep going.
                //    subtask.setStatus(TaskStatus.ERROR);
                // But do log the parse error for this file.
                subtask.addResult(PARSE_PREFIX + entry.getFileName(),
                        "Exception in JsonListTransform while Parsing RDF");
                logger.error("Exception in JsonListTransform "
                        + "while Parsing RDF:", ex);
            }
        }

        String resultFileName = TaskUtils.getTaskOutputPath(taskInfo, true,
                "concepts_list.json");
        try {
            File out = new File(resultFileName);
            HashMap<String, HashMap<String, Object>> conceptMap =
                    conceptHandler.getConceptMap();
            FileUtils.writeStringToFile(out,
                    JSONSerialization.serializeObjectAsJsonString(conceptMap),
                    StandardCharsets.UTF_8);
            VersionArtefactUtils.createConceptListVersionArtefact(taskInfo,
                    resultFileName);
        } catch (IOException ex) {
            subtask.setStatus(TaskStatus.ERROR);
            subtask.addResult(TaskRunner.ERROR,
                    "Exception in JsonListTransform while Parsing RDF");
            logger.error("Exception in JsonListTransform generating result:",
                    ex);
            return;
        }
        subtask.setStatus(TaskStatus.SUCCESS);
    }

    /** RDF Handler to extract prefLabels, notation, and use broader
     * and narrow properties to construct a list-like structure. */
    class ConceptHandler extends RDFHandlerBase {

        /** Map from concept IRI to a map that maps
         * property name to the property value(s). */
        private HashMap<String, HashMap<String, Object>> conceptMap =
                new HashMap<>();

        @Override
        public void handleStatement(final Statement st) {
            if (conceptMap.get(st.getSubject().stringValue()) == null) {
                conceptMap.put(st.getSubject().stringValue(),
                        new HashMap<String, Object>());
            }
            HashMap<String, Object> concept =
                    conceptMap.get(st.getSubject().stringValue());
            if (st.getPredicate().equals(SKOS.PREF_LABEL)) {
                concept.put(JsonListTransformProvider.PREF_LABEL,
                        st.getObject().stringValue());
            }
            if (st.getPredicate().equals(SKOS.NOTATION)) {
                concept.put("notation", st.getObject().stringValue());
            }
            if (st.getPredicate().equals(SKOS.BROADER)) {
                if (concept.get("broader") == null) {
                    concept.put("broader",
                            new ArrayList<String>());
                }
                @SuppressWarnings("unchecked")
                ArrayList<String> broaderList =
                    (ArrayList<String>) concept.get("broader");
                broaderList.add(st.getObject().stringValue());
            }
            if (st.getPredicate().equals(SKOS.NARROWER)) {
                if (concept.get("narrower") == null) {
                    concept.put("narrower",
                            new ArrayList<String>());
                }
                @SuppressWarnings("unchecked")
                ArrayList<String> narrowerList =
                    (ArrayList<String>) concept.get("narrower");
                narrowerList.add(st.getObject().stringValue());
            }
        }

        /** Getter for concepts list.
         * @return The completed concept map. */
        public HashMap<String, HashMap<String, Object>> getConceptMap() {
            return conceptMap;
        }
    }

    /** Remove the JsonList version artefact for the version.
     * @param taskInfo The top-level TaskInfo for the subtask.
     * @param subtask The subtask to be performed.
     */
    public final void untransform(final TaskInfo taskInfo,
            final Subtask subtask) {
        // Remove the JsonList version artefact.
        List<VersionArtefact> vas = VersionArtefactDAO.
                getCurrentVersionArtefactListForVersionByType(
                        taskInfo.getVersion().getVersionId(),
                        VersionArtefactType.CONCEPT_LIST,
                        taskInfo.getEm());
        for (VersionArtefact va : vas) {
            // We _don't_ delete the file. But if we did:
            /*
            VaConceptList vaConceptList =
                    JSONSerialization.deserializeStringAsJson(
                            va.getData(), VaConceptList.class);
            Files.deleteIfExists(Paths.get(vaConceptList.getPath()));
            */
            TemporalUtils.makeHistorical(va, taskInfo.getNowTime());
            va.setModifiedBy(taskInfo.getModifiedBy());
            VersionArtefactDAO.updateVersionArtefact(taskInfo.getEm(), va);
        }
        subtask.setStatus(TaskStatus.SUCCESS);
    }

    /** {@inheritDoc} */
    @Override
    public Integer defaultPriority(final SubtaskOperationType operationType) {
        switch (operationType) {
        case INSERT:
        case PERFORM:
            return DefaultPriorities.
                    DEFAULT_TRANSFORM_BEFORE_IMPORTER_INSERT_PRIORITY;
        case DELETE:
            return DefaultPriorities.
                    DEFAULT_TRANSFORM_BEFORE_IMPORTER_DELETE_PRIORITY;
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
