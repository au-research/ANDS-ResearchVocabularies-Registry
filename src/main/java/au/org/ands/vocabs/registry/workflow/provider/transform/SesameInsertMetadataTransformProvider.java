/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.workflow.provider.transform;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;

import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.internal.VersionJson;
import au.org.ands.vocabs.registry.enums.SubtaskOperationType;
import au.org.ands.vocabs.registry.enums.TaskStatus;
import au.org.ands.vocabs.registry.workflow.provider.DefaultPriorities;
import au.org.ands.vocabs.registry.workflow.provider.WorkflowProvider;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;
import au.org.ands.vocabs.registry.workflow.tasks.TaskRunner;

/**
 * Transform provider for inserting (version) metadata into a Sesame repository.
 * In case we need to do a transform provider that operates on "raw" RDF files,
 * see
 * https://groups.google.com/d/msg/sesame-users/fJctKX_vNEs/a1gm7rqD3L0J for how
 * to do it.
 */
public class SesameInsertMetadataTransformProvider
    implements WorkflowProvider {

    /** Logger for this class. */
    private final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Update to insert dcterms:issued metadata. Removes any existing
     * triples of this format. */
    private static final String INSERT_DCTERMS_ISSUED_METADATA_UPDATE =
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n"
            + "PREFIX dcterms: <http://purl.org/dc/terms/>\n"
            + "DELETE {\n"
            + "  ?scheme dcterms:issued ?oldIssuedDate\n"
            + "} WHERE {\n"
            + "  ?scheme a skos:ConceptScheme .\n"
            + "  ?scheme dcterms:issued ?oldIssuedDate\n"
            + "} ;\n"
            + "INSERT {\n"
            + "  ?scheme dcterms:issued ?issuedDate\n"
            + "} WHERE {\n"
            + "  ?scheme a skos:ConceptScheme\n"
            + "}";

    /** Update to insert owl:versionInfo metadata. Removes any existing
     * triples of this format. */
    private static final String INSERT_OWL_VERSIONINFO_METADATA_UPDATE =
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n"
            + "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
            + "DELETE {\n"
            + "  ?scheme owl:versionInfo ?oldVersionTitle\n"
            + "} WHERE {\n"
            + "  ?scheme a skos:ConceptScheme .\n"
            + "  ?scheme owl:versionInfo ?oldVersionTitle\n"
            + "} ;\n"
            + "INSERT {\n"
            + "  ?scheme owl:versionInfo ?versionTitle .\n"
            + "} WHERE {\n"
            + "  ?scheme a skos:ConceptScheme\n"
            + "}";

    /* * Update to insert adms:status metadata. Removes any existing
     * triples of this format. */
    /* Uncomment when needed.
    private static final String INSERT_ADMS_STATUS_METADATA_UPDATE =
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n"
            + "PREFIX adms: <http://www.w3.org/ns/adms#>\n"
            + "DELETE {\n"
            + "  ?scheme adms:status ?oldVersionStatus \n"
            + "} WHERE {\n"
            + "  ?scheme a skos:ConceptScheme .\n"
            + "  ?scheme adms:status ?oldVersionStatus \n"
            + "} ;\n"
            + "INSERT {\n"
            + "  ?scheme adms:status ?versionStatus \n"
            + "} WHERE {\n"
            + "  ?scheme a skos:ConceptScheme\n"
            + "}";
            */

    /** Update to remove (version) metadata. */
    private static final String DELETE_METADATA_UPDATE =
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n"
            + "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
            + "PREFIX dcterms: <http://purl.org/dc/terms/>\n"
            + "PREFIX adms: <http://www.w3.org/ns/adms#>\n"
            + "DELETE {\n"
            + "  ?scheme dcterms:issued ?oldIssuedDate\n"
            + "} WHERE {\n"
            + "  ?scheme a skos:ConceptScheme .\n"
            + "  ?scheme dcterms:issued ?oldIssuedDate\n"
            + "} ;\n"
            + "DELETE {\n"
            + "  ?scheme owl:versionInfo ?oldVersionTitle\n"
            + "} WHERE {\n"
            + "  ?scheme a skos:ConceptScheme .\n"
            + "  ?scheme owl:versionInfo ?oldVersionTitle\n"
            + "} ;\n"
            + "DELETE {\n"
            + "  ?scheme adms:status ?oldVersionStatus \n"
            + "} WHERE {\n"
            + "  ?scheme a skos:ConceptScheme .\n"
            + "  ?scheme adms:status ?oldVersionStatus \n"
            + "}";

    /** Map of our own version status indicators to the PURLs used
     * by ADMS 1.0. */
    private static HashMap<String, String> admsStatusMap =
            new HashMap<>();

    static {
        admsStatusMap.put("current",
                "http://purl.org/adms/status/Completed");
        admsStatusMap.put("superseded",
                "http://purl.org/adms/status/Withdrawn");
        admsStatusMap.put("deprecated",
                "http://purl.org/adms/status/Deprecated");
    }

    /** Apply metadata insertion to the Sesame repository for the version.
     * @param taskInfo The top-level TaskInfo for the subtask.
     * @param subtask The subtask to be performed.
     */
    public final void transform(final TaskInfo taskInfo,
            final Subtask subtask) {
        boolean result = true;
        // Get the metadata values to be inserted.
        Version version = taskInfo.getVersion();

        // Use the release date as it is. As it may be
        // YYYY, YYYY-MM, or YYYY-MM-DD, can't use a date
        // formatter.
        String issuedDate = version.getReleaseDate();

        VersionJson versionJson = JSONSerialization.deserializeStringAsJson(
                version.getData(), VersionJson.class);
        String versionTitle = versionJson.getTitle();

        // Construct bindings for SPARQL Update.
        ValueFactory factory = ValueFactoryImpl.getInstance();
        HashMap<String, Value> bindings = new HashMap<>();

        if (issuedDate != null) {
            bindings.put("issuedDate", factory.createLiteral(issuedDate));
            result = SesameTransformUtils.runUpdate(taskInfo, subtask,
                    INSERT_DCTERMS_ISSUED_METADATA_UPDATE, bindings);
            if (!result) {
                // Failure applying the Update. Stop here.
                subtask.setStatus(TaskStatus.ERROR);
                subtask.addResult(TaskRunner.ERROR,
                        "Error applying dcterms:issued SPARQL update");
                return;
            }
        }

        if (versionTitle != null) {
            // Reset bindings and apply the version title Update.
            bindings.clear();
            bindings.put("versionTitle", factory.createLiteral(versionTitle));
            result = SesameTransformUtils.runUpdate(taskInfo, subtask,
                    INSERT_OWL_VERSIONINFO_METADATA_UPDATE, bindings);
            if (!result) {
                // Failure applying the Update. Stop here.
                subtask.setStatus(TaskStatus.ERROR);
                subtask.setStatus(TaskStatus.ERROR);
                subtask.addResult(TaskRunner.ERROR,
                        "Error applying owl:versionInfo SPARQL update");
                return;
            }
        }

        /* Future work: Add ADMS status. The problem is, that the
         * publication workflow is not so great, so metadata injection
         * doesn't happen when the status changes. So once set, always
         * set with the same value. If/when publication workflow is
         * improved, uncomment this.
         */
        /*
        String versionStatus = admsStatusMap.get(version.getStatus());
        if (versionStatus != null) {
            // Reset bindings and apply the ADMS status Update.
            bindings.clear();
            bindings.put("versionStatus", factory.createURI(versionStatus));
            return SesameTransformUtils.runUpdate(taskInfo, subtask, results,
                    INSERT_ADMS_STATUS_METADATA_UPDATE, bindings);
        }
        */
        subtask.setStatus(TaskStatus.SUCCESS);
    }

    /** Undo the metadata insertion from the Sesame repository for the version.
     * @param taskInfo The top-level TaskInfo for the subtask.
     * @param subtask The subtask to be performed.
     */
    public final void untransform(final TaskInfo taskInfo,
            final Subtask subtask) {
        boolean success = SesameTransformUtils.runUpdate(taskInfo, subtask,
                DELETE_METADATA_UPDATE, new HashMap<String, Value>());
        if (success) {
            subtask.setStatus(TaskStatus.SUCCESS);
        } else {
            subtask.setStatus(TaskStatus.ERROR);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Integer defaultPriority(final SubtaskOperationType operationType) {
        switch (operationType) {
        case INSERT:
        case PERFORM:
            return DefaultPriorities.
                    DEFAULT_TRANSFORM_AFTER_IMPORTER_INSERT_PRIORITY;
        case DELETE:
            return DefaultPriorities.
                    DEFAULT_TRANSFORM_AFTER_IMPORTER_DELETE_PRIORITY;
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
