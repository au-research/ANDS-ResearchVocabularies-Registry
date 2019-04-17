/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.utils.migration;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.auth.AuthConstants;
import au.org.ands.vocabs.registry.db.context.DBContext;
import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.dao.VersionArtefactDAO;
import au.org.ands.vocabs.registry.db.dao.VersionDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.VersionArtefact;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.db.internal.VaConceptTree;
import au.org.ands.vocabs.registry.enums.SubtaskOperationType;
import au.org.ands.vocabs.registry.enums.SubtaskProviderType;
import au.org.ands.vocabs.registry.enums.VersionArtefactType;
import au.org.ands.vocabs.registry.workflow.WorkflowMethods;
import au.org.ands.vocabs.registry.workflow.provider.transform.JsonTreeTransformProvider;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;
import au.org.ands.vocabs.registry.workflow.tasks.Task;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;

/** Standalone program to create tasks to remove all currently-valid
 * version artefacts generated by the JsonTreeTransformProvider,
 * and to replace them with new ones using the
 * ConceptTreeTransformProvider. Note: the tasks are not run;
 * this must be done afterwards using the API, by a suitable user.
 */
public final class MigrateJsonTreesToConceptTrees {

    /** Logger for this class. */
    private static Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Private constructor for a standalone program. */
    private MigrateJsonTreesToConceptTrees() {
    }

    /** Create tasks to remove all currently-valid version artefacts
     * generated by the JsonTreeTransformProvider and
     * replace them with new ones using the ConceptTreeTransformProvider.
     * @param args Command-line parameters. None are expected.
     */
    public static void main(final String[] args) {
        logger.info("Starting MigrateJsonTreesToConceptTrees");

        EntityManager em = null;
        EntityTransaction txn = null;

        try {
            em = DBContext.getEntityManager();
            txn = em.getTransaction();

            // Timestamp to use for start/end date values.
            LocalDateTime now = TemporalUtils.nowUTC();

            // No DAO method to get all current VAs of a particular type,
            // so get all current VAs, then check each one to see if
            // it's the type we're looking for.
            List<VersionArtefact> vaList =
                    VersionArtefactDAO.getAllCurrentVersionArtefact();
            for (VersionArtefact va : vaList) {
                if (va.getType() != VersionArtefactType.CONCEPT_TREE) {
                    // Not a concept tree version artefact.
                    continue;
                }
                VaConceptTree conceptTree =
                        JSONSerialization.deserializeStringAsJson(va.getData(),
                                VaConceptTree.class);

                if (!conceptTree.getPath().endsWith("tree.json")) {
                    // This wasn't generated by the original transform.
                    // Quite probably, it has already been migrated.
                    logger.info("Skipping VA with id  (surrogate key): "
                            + va.getId() + "; wasn't generated by JsonTree");
                    continue;
                }
                int versionId = va.getVersionId();
                logger.info("Working on VA with id (surrogate key): "
                        + va.getId() + "; version Id: " + versionId);
                Version version = VersionDAO.getCurrentVersionByVersionId(
                        em, versionId);
                if (version == null) {
                    logger.error("Ouch: version with version Id: "
                            + versionId + " missing!");
                    continue;
                }
                int vocabularyId = version.getVocabularyId();
                Vocabulary vocabulary = VocabularyDAO.
                        getCurrentVocabularyByVocabularyId(em, vocabularyId);
                if (vocabulary == null) {
                    logger.error("Ouch: vocabulary with vocabulary Id: "
                            + vocabularyId + " missing!");
                    continue;
                }
                Task task = new Task();
                task.setVocabularyId(vocabularyId);
                task.setVersionId(versionId);
                Subtask deleteSubtask = new Subtask();
                deleteSubtask.setSubtaskProviderType(
                        SubtaskProviderType.TRANSFORM);
                deleteSubtask.setProvider(JsonTreeTransformProvider.class);
                deleteSubtask.setOperation(SubtaskOperationType.DELETE);
                deleteSubtask.determinePriority();
                task.addSubtask(deleteSubtask);
                Subtask createSubtask =
                        WorkflowMethods.newConceptBrowseSubtask();
                task.addSubtask(createSubtask);
                TaskInfo taskInfo = new TaskInfo(task, vocabulary, version);
                taskInfo.setEm(em);
                taskInfo.setModifiedBy(AuthConstants.SYSTEM_USER);
                taskInfo.setNowTime(now);
                logger.info("Persisting task ...");
                txn.begin();
                taskInfo.persist();
                logger.info("... done.");
                logger.info("You must now run the task with task Id: "
                        + taskInfo.getDbTask().getId());
                txn.commit();
            }
        } catch (Throwable t) {
            if (txn != null && txn.isActive()) {
                try {
                    logger.error("Exception during transaction; rolling back",
                            t);
                    txn.rollback();
                } catch (Exception e) {
                    logger.error("Rollback failure!", e);
                }
            } else {
                logger.error("Exception, either during rollback, or "
                        + "outside active transaction", t);
            }
            // Otherwise, don't throw, but fall through so that the user sees
            // an error message.
        } finally {
            if (em != null) {
                em.close();
            }
        }

    }

}