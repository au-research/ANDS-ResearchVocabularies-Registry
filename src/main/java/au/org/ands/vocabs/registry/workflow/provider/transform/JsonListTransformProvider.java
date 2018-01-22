/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.workflow.provider.transform;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

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

import com.fasterxml.jackson.databind.JsonNode;

import au.org.ands.vocabs.registry.enums.SubtaskOperationType;
import au.org.ands.vocabs.registry.workflow.provider.DefaultPriorities;
import au.org.ands.vocabs.registry.workflow.provider.WorkflowProvider;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;
import au.org.ands.vocabs.toolkit.db.TaskUtils;
import au.org.ands.vocabs.toolkit.tasks.TaskInfo;
import au.org.ands.vocabs.toolkit.tasks.TaskStatus;
import au.org.ands.vocabs.toolkit.utils.ToolkitFileUtils;

/** Transform provider for generating a list-like representation of the
 * concepts as JSON. This assumes a vocabulary encoded using SKOS. */
public class JsonListTransformProvider extends TransformProvider
    implements WorkflowProvider {

    /** Logger for this class. */
    private final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Key used for storing a SKOS prefLabel. */
    public static final String PREF_LABEL = "prefLabel";

    @Override
    public final String getInfo() {
        // Return null for now.
        return null;
    }

    @Override
    public final boolean transform(final TaskInfo taskInfo,
            final JsonNode subtask,
            final HashMap<String, String> results) {
        Path dir = Paths.get(ToolkitFileUtils.getTaskHarvestOutputPath(
                taskInfo));
        ConceptHandler conceptHandler = new ConceptHandler();
        try (DirectoryStream<Path> stream =
                Files.newDirectoryStream(dir)) {
            for (Path entry: stream) {
                RDFFormat format = Rio.getParserFormatForFileName(
                        entry.toString());
                RDFParser rdfParser = Rio.createParser(format);
                rdfParser.setRDFHandler(conceptHandler);
                FileInputStream is = new FileInputStream(entry.toString());
                rdfParser.parse(is, entry.toString());
                logger.debug("Reading RDF:" + entry.toString());

            }
        } catch (DirectoryIteratorException
                | IOException
                | RDFParseException
                | RDFHandlerException
                | UnsupportedRDFormatException ex) {
            results.put(TaskStatus.EXCEPTION,
                    "Exception in JsonListTransform while Parsing RDF");
            logger.error("Exception in JsonListTransform while Parsing RDF:",
                    ex);
            return false;
        }

        String resultFileName = ToolkitFileUtils.getTaskOutputPath(taskInfo,
                "concepts_list.json");
        try {
            File out = new File(resultFileName);
            results.put("concepts_list", resultFileName);
            HashMap<String, HashMap<String, Object>> conceptMap =
                    conceptHandler.getConceptMap();
            FileUtils.writeStringToFile(out,
                    TaskUtils.mapToJSONString(conceptMap),
                    StandardCharsets.UTF_8);
        } catch (IOException ex) {
            results.put(TaskStatus.EXCEPTION,
                    "Exception in JsonListTransform while Parsing RDF");
            logger.error("Exception in JsonListTransform generating result:",
                    ex);
            return false;
        }
        return true;
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

    @Override
    public final boolean untransform(final TaskInfo taskInfo,
            final JsonNode subtask,
            final HashMap<String, String> results) {
        // TO DO: remove it!
        return false;
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
    public void doSubtask(
            final au.org.ands.vocabs.registry.workflow.tasks.TaskInfo taskInfo,
            final Subtask subtask) {
        // TO DO

    }

}
