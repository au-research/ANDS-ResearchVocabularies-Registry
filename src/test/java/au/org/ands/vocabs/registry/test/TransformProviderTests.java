/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.test;

import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.REGISTRY;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.dbunit.DatabaseUnitException;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.db.context.DBContext;
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.dao.TaskDAO;
import au.org.ands.vocabs.registry.db.dao.VersionArtefactDAO;
import au.org.ands.vocabs.registry.db.dao.VersionDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.db.entity.Task;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.VersionArtefact;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.db.internal.VaConceptTree;
import au.org.ands.vocabs.registry.enums.TaskStatus;
import au.org.ands.vocabs.registry.enums.VersionArtefactType;
import au.org.ands.vocabs.registry.workflow.provider.transform.JsonTreeTransformProvider;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;
import au.org.ands.vocabs.toolkit.test.arquillian.ArquillianBaseTest;
import au.org.ands.vocabs.toolkit.test.arquillian.ArquillianTestUtils;

/** Tests of the Registry transform workflow providers.
 * As this class grows, it might be split up further.
 */
@Test
public class TransformProviderTests extends ArquillianBaseTest {

    /** Logger. */
    private static Logger logger;

    static {
        logger = LoggerFactory.getLogger(
                MethodHandles.lookup().lookupClass());
    }

    /** Name of this class, used in paths to test data files. */
    private static final String CLASS_NAME_PREFIX = "TransformProviderTests.";

    // Server-side tests go here. Client-side tests later on.

    /** A convenient value to use for endDate properties when
     * deleting. */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static LocalDateTime nowTime1 =
            LocalDateTime.of(2017, 10, 1, 10, 10);

    // Tests of class
    // au.org.ands.vocabs.registry.workflow.provider.transform.
    //   JsonTreeTransformProvider.

    // Task numbers 3 and 4 generate magic number warnings.
    /** Server-side test of {@code JsonTreeTransformProvider}.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     */
    @Test
    @SuppressWarnings({"checkstyle:MethodLength", "checkstyle:MagicNumber"})
    public final void testJsonTreeTransformProvider1() throws
        DatabaseUnitException, HibernateException, IOException, SQLException {

        // TO DO: turn this into a registry test!
        // (If you were to run it now, it would fail.)

        String testsPath = ArquillianTestUtils.getClassesPath()
                + "/test/tests/";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testJsonTreeTransformProvider1");

        EntityManager em = null;
        EntityTransaction txn = null;
        try {
            em = DBContext.getEntityManager();
            txn = em.getTransaction();
            txn.begin();

            Vocabulary vocabulary = VocabularyDAO.
                    getCurrentVocabularyByVocabularyId(em, 1);
            Version version;

            TaskInfo taskInfo;
            au.org.ands.vocabs.registry.workflow.tasks.Task workflowTask;
            VersionArtefact va;
            VaConceptTree vaConceptTree;

            List<Task> taskList = TaskDAO.getAllTask();
            logger.info("testJsonTreeTransformProvider1: task list length = "
                    + taskList.size());
            Assert.assertEquals(taskList.size(), 5, "Not five tasks");

            Task task = TaskDAO.getTaskById(1);
            version = VersionDAO.getCurrentVersionByVersionId(em, 1);
            taskInfo = new TaskInfo(task, vocabulary, version);
            taskInfo.setEm(em);
            taskInfo.setModifiedBy("SYSTEM");
            taskInfo.setNowTime(nowTime1);
            taskInfo.process();
            workflowTask  = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
                "JsonTreeTransformProvider failed on task 1");

            va = VersionArtefactDAO.
                    getCurrentVersionArtefactListForVersionByType(1,
                            VersionArtefactType.CONCEPT_TREE, em).get(0);
            vaConceptTree = JSONSerialization.deserializeStringAsJson(
                    va.getData(), VaConceptTree.class);

            String conceptsTreeFilename = vaConceptTree.getPath();
            ArquillianTestUtils.compareJson(conceptsTreeFilename,
                    testsPath
                    + CLASS_NAME_PREFIX
                    + "testJsonTreeTransformProvider1/"
                    + "test-data1-concepts_tree.json");

            task = TaskDAO.getTaskById(2);
            version = VersionDAO.getCurrentVersionByVersionId(em, 2);
            taskInfo = new TaskInfo(task, vocabulary, version);
            taskInfo.setEm(em);
            taskInfo.setModifiedBy("SYSTEM");
            taskInfo.setNowTime(nowTime1);
            taskInfo.process();
            workflowTask  = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
                    "JsonTreeTransformProvider failed on task 2");
            va = VersionArtefactDAO.
                    getCurrentVersionArtefactListForVersionByType(2,
                            VersionArtefactType.CONCEPT_TREE, em).get(0);
            vaConceptTree = JSONSerialization.deserializeStringAsJson(
                    va.getData(), VaConceptTree.class);
            conceptsTreeFilename = vaConceptTree.getPath();
            // Note the use of the same correct output as the previous test.
            ArquillianTestUtils.compareJson(conceptsTreeFilename,
                    testsPath
                    + CLASS_NAME_PREFIX
                    + "testJsonTreeTransformProvider1/"
                    + "test-data1-concepts_tree.json");

            // Polyhierarchy detection
            task = TaskDAO.getTaskById(3);
            version = VersionDAO.getCurrentVersionByVersionId(em, 3);
            taskInfo = new TaskInfo(task, vocabulary, version);
            taskInfo.setEm(em);
            taskInfo.setModifiedBy("SYSTEM");
            taskInfo.setNowTime(nowTime1);
            taskInfo.process();
            workflowTask  = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.PARTIAL,
                    "JsonTreeTransformProvider failed on task 3");
            Assert.assertEquals(VersionArtefactDAO.
                    getCurrentVersionArtefactListForVersionByType(3,
                            VersionArtefactType.CONCEPT_TREE, em).size(), 0,
                    "JsonTreeTransformProvider task 3 created a "
                            + "version artefact");
            Assert.assertEquals(workflowTask.getSubtasks().get(0).
                    getResults().get(JsonTreeTransformProvider.
                            CONCEPTS_TREE_NOT_PROVIDED),
                    "No concepts tree provided, because there is a forward "
                    + "or cross edge.",
                    "JsonTreeTransformProvider task 3 returned wrong value for "
                    + JsonTreeTransformProvider.CONCEPTS_TREE_NOT_PROVIDED);

            // Cycle detection
            task = TaskDAO.getTaskById(4);
            version = VersionDAO.getCurrentVersionByVersionId(em, 4);
            taskInfo = new TaskInfo(task, vocabulary, version);
            taskInfo.setEm(em);
            taskInfo.setModifiedBy("SYSTEM");
            taskInfo.setNowTime(nowTime1);
            taskInfo.process();
            workflowTask  = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.PARTIAL,
                    "JsonTreeTransformProvider failed on task 4");
            Assert.assertEquals(VersionArtefactDAO.
                    getCurrentVersionArtefactListForVersionByType(4,
                            VersionArtefactType.CONCEPT_TREE, em).size(), 0,
                    "JsonTreeTransformProvider task 4 created a "
                            + "version artefact");
            Assert.assertEquals(workflowTask.getSubtasks().get(0).
                    getResults().get(JsonTreeTransformProvider.
                            CONCEPTS_TREE_NOT_PROVIDED),
                    "No concepts tree provided, because there is a cycle.",
                    "JsonTreeTransformProvider task 4 returned wrong value for "
                    + JsonTreeTransformProvider.CONCEPTS_TREE_NOT_PROVIDED);

            // Multilingual vocabularies, based on CC-1866.
            task = TaskDAO.getTaskById(5);
            version = VersionDAO.getCurrentVersionByVersionId(em, 5);
            taskInfo = new TaskInfo(task, vocabulary, version);
            taskInfo.setEm(em);
            taskInfo.setModifiedBy("SYSTEM");
            taskInfo.setNowTime(nowTime1);
            taskInfo.process();
            workflowTask  = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
                    "JsonTreeTransformProvider failed on task 5");
            va = VersionArtefactDAO.
                    getCurrentVersionArtefactListForVersionByType(5,
                            VersionArtefactType.CONCEPT_TREE, em).get(0);
            vaConceptTree = JSONSerialization.deserializeStringAsJson(
                    va.getData(), VaConceptTree.class);
            conceptsTreeFilename = vaConceptTree.getPath();
            ArquillianTestUtils.compareJson(conceptsTreeFilename,
                    testsPath
                    + CLASS_NAME_PREFIX
                    + "testJsonTreeTransformProvider1/"
                    + "test-data5-concepts_tree.json");
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
                logger.error("Exception other than during transaction: ", t);
            }
            throw t;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    // Client-side tests go here. Server-side tests are above this line.

}