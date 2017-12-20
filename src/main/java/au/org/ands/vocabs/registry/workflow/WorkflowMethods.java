/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.entity.AccessPoint;
import au.org.ands.vocabs.registry.db.entity.VersionArtefact;
import au.org.ands.vocabs.registry.db.internal.ApFile;
import au.org.ands.vocabs.registry.enums.ApSource;
import au.org.ands.vocabs.registry.enums.SubtaskOperationType;
import au.org.ands.vocabs.registry.enums.SubtaskProviderType;
import au.org.ands.vocabs.registry.workflow.provider.importer.SesameImporterProvider;
import au.org.ands.vocabs.registry.workflow.provider.publish.SISSVocPublishProvider;
import au.org.ands.vocabs.registry.workflow.provider.transform.JsonListTransformProvider;
import au.org.ands.vocabs.registry.workflow.provider.transform.JsonTreeTransformProvider;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;

/** The interface provided by the workflow package. API and methods
 * in other parts of the registry should invoke the methods of this class,
 * rather than directly invoking the methods of the workflow classes
 * in this package.
 */
public final class WorkflowMethods {

    /** Logger for this class. */
    private static Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Private constructor for a utility class. */
    private WorkflowMethods() {
    }

    /** Apply workflow deletion to an access point.
     * One of two situations applies: either, the access point can be, and is,
     * deleted immediately, or, a list of workflow subtasks must be performed.
     * NB: in either case, this method does <i>not</i> delete (or mark as
     * historical) the database row for the access point.
     * @param ap The access point to be deleted.
     * @return null, if the deletion has been completed, or a non-empty
     *      list of required workflow subtasks that need to be performed.
     */
    public static List<Subtask> deleteAccessPoint(final AccessPoint ap) {
        List<Subtask> subtaskList = new ArrayList<>();
        Subtask subtask = new Subtask();
        subtaskList.add(subtask);
        switch (ap.getType()) {
        case API_SPARQL:
            if (ap.getSource() == ApSource.USER) {
                // No further action required.
                return null;
            }
            // Handle source=SYSTEM.
            // Note well: we rely on the fact that SesameImporterProvider
            // is responsible for _both_ the API_SPARQL and SESAME_DOWNLOAD
            // access points, and where there is one, there will also be
            // the other. We choose to do something for API_SPARQL and
            // "ignore" the corresponding SESAME_DOWNLOAD.
            subtask.setSubtaskProviderType(SubtaskProviderType.IMPORTER);
            subtask.setProvider(SesameImporterProvider.class);
            subtask.setOperation(SubtaskOperationType.DELETE);
            subtask.determinePriority();
            break;
        case FILE:
            ApFile apFile = JSONSerialization.deserializeStringAsJson(
                    ap.getData(), ApFile.class);
            try {
                FileUtils.deleteDirectory(new File(apFile.getPath()));
            } catch (IOException e) {
                logger.error("Error deleting file: " + apFile.getPath(), e);
            }
            // No subtask required.
            return null;
        case SESAME_DOWNLOAD:
            // Note well: we rely on the fact that SesameImporterProvider
            // is responsible for _both_ the API_SPARQL and SESAME_DOWNLOAD
            // access points, and where there is one, there will also be
            // the other. So we do nothing here.
            // And since there's nothing to be done if source=USER,
            // we do nothing for that case either. Below is the sort of
            // code that would be required if this assumption should change.
            /*
            if (ap.getSource() == ApSource.USER) {
                // No further action required.
                return null;
            }
            subtask.setSubtaskProviderType(SubtaskProviderType.IMPORTER);
            subtask.setProvider(SesameImporterProvider.class);
            subtask.setOperation(SubtaskOperationType.DELETE);
            subtask.determinePriority();
            */
            break;
        case SISSVOC:
            if (ap.getSource() == ApSource.USER) {
                // No further action required.
                return null;
            }
            // Handle source=SYSTEM.
            subtask.setSubtaskProviderType(SubtaskProviderType.PUBLISH);
            subtask.setProvider(SISSVocPublishProvider.class);
            subtask.setOperation(SubtaskOperationType.DELETE);
            subtask.determinePriority();
            break;
        case WEB_PAGE:
            // These are all source=USER, and no further action is required.
            return null;
        default:
            // Oops, a type we don't know about.
            throw new IllegalArgumentException(
                    "Unknown access point type: " + ap.getType());
        }
        return subtaskList;
    }

    /** Apply workflow deletion to a version artefact.
     * One of two situations applies: either, the version artefact can be,
     * and is, deleted immediately, or, a list of workflow subtasks must
     * be performed.
     * NB: in either case, this method does <i>not</i> delete (or mark
     * as historical) the database row for the version artefact.
     * @param va The version artefact to be deleted.
     * @return null, if the deletion has been completed, or a non-empty
     *      list of required workflow subtasks that need to be performed.
     */
    public static List<Subtask> deleteVersionArtefact(
            final VersionArtefact va) {
        List<Subtask> subtaskList = new ArrayList<>();
        Subtask subtask = new Subtask();
        subtaskList.add(subtask);
        switch (va.getType()) {
        case CONCEPT_LIST:
            subtask.setSubtaskProviderType(SubtaskProviderType.TRANSFORM);
            subtask.setProvider(JsonListTransformProvider.class);
            subtask.setOperation(SubtaskOperationType.DELETE);
            subtask.determinePriority();
            break;
        case CONCEPT_TREE:
            subtask.setSubtaskProviderType(SubtaskProviderType.TRANSFORM);
            subtask.setProvider(JsonTreeTransformProvider.class);
            subtask.setOperation(SubtaskOperationType.DELETE);
            subtask.determinePriority();
            break;
        default:
            // Oops, a type we don't know about.
            throw new IllegalArgumentException(
                    "Unknown version artefact type: " + va.getType());
        }
        return subtaskList;
    }

}
