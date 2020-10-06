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

import org.dbunit.Assertion;
import org.dbunit.DatabaseUnitException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.SortedTable;
import org.dbunit.dataset.filter.DefaultColumnFilter;
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
import au.org.ands.vocabs.registry.db.internal.VaResourceDocs;
import au.org.ands.vocabs.registry.enums.TaskStatus;
import au.org.ands.vocabs.registry.enums.VersionArtefactType;
import au.org.ands.vocabs.registry.workflow.provider.transform.ConceptTreeTransformProvider;
import au.org.ands.vocabs.registry.workflow.provider.transform.JsonTreeTransformProvider;
import au.org.ands.vocabs.registry.workflow.provider.transform.conceptTree.Resource;
import au.org.ands.vocabs.registry.workflow.provider.transform.conceptTree.StatementHandler;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;
import au.org.ands.vocabs.registry.workflow.tasks.TaskRunner;
import au.org.ands.vocabs.toolkit.test.arquillian.ArquillianBaseTest;
import au.org.ands.vocabs.toolkit.test.arquillian.ArquillianTestUtils;
import au.org.ands.vocabs.toolkit.test.utils.DbUnitConstants;

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
            workflowTask = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
                "JsonTreeTransformProvider failed on task 1");

            va = VersionArtefactDAO.
                    getCurrentVersionArtefactListForVersionByType(1,
                            VersionArtefactType.CONCEPT_TREE, em).get(0);
            vaConceptTree = JSONSerialization.deserializeStringAsJson(
                    va.getData(), VaConceptTree.class);

            String conceptsTreeFilename = vaConceptTree.getPath();
            ArquillianTestUtils.compareJsonFiles(conceptsTreeFilename,
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
            workflowTask = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
                    "JsonTreeTransformProvider failed on task 2");
            va = VersionArtefactDAO.
                    getCurrentVersionArtefactListForVersionByType(2,
                            VersionArtefactType.CONCEPT_TREE, em).get(0);
            vaConceptTree = JSONSerialization.deserializeStringAsJson(
                    va.getData(), VaConceptTree.class);
            conceptsTreeFilename = vaConceptTree.getPath();
            // Note the use of the same correct output as the previous test.
            ArquillianTestUtils.compareJsonFiles(conceptsTreeFilename,
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
            workflowTask = taskInfo.getTask();

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
            workflowTask = taskInfo.getTask();

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
            workflowTask = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
                    "JsonTreeTransformProvider failed on task 5");
            va = VersionArtefactDAO.
                    getCurrentVersionArtefactListForVersionByType(5,
                            VersionArtefactType.CONCEPT_TREE, em).get(0);
            vaConceptTree = JSONSerialization.deserializeStringAsJson(
                    va.getData(), VaConceptTree.class);
            conceptsTreeFilename = vaConceptTree.getPath();
            ArquillianTestUtils.compareJsonFiles(conceptsTreeFilename,
                    testsPath
                    + CLASS_NAME_PREFIX
                    + "testJsonTreeTransformProvider1/"
                    + "test-data5-concepts_tree.json");
            txn.commit();
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

    // Tests of class
    // au.org.ands.vocabs.registry.workflow.provider.transform.
    //   ConceptTreeTransformProvider.

    // Task numbers >= 3 generate magic number warnings.
    /** Server-side test of {@code ConceptTreeTransformProvider}.
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
    public final void testConceptTreeTransformProvider1() throws
        DatabaseUnitException, HibernateException, IOException, SQLException {
        String testsPath = ArquillianTestUtils.getClassesPath()
                + "/test/tests/";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testConceptTreeTransformProvider1");

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
            logger.info("testConceptTreeTransformProvider1: task list length = "
                    + taskList.size());
            Assert.assertEquals(taskList.size(), 5, "Not five tasks");

            Task task = TaskDAO.getTaskById(1);
            version = VersionDAO.getCurrentVersionByVersionId(em, 1);
            taskInfo = new TaskInfo(task, vocabulary, version);
            taskInfo.setEm(em);
            taskInfo.setModifiedBy("SYSTEM");
            taskInfo.setNowTime(nowTime1);
            taskInfo.process();
            workflowTask = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
                "ConceptTreeTransformProvider failed on task 1");

            va = VersionArtefactDAO.
                    getCurrentVersionArtefactListForVersionByType(1,
                            VersionArtefactType.CONCEPT_TREE, em).get(0);
            vaConceptTree = JSONSerialization.deserializeStringAsJson(
                    va.getData(), VaConceptTree.class);

            String conceptsTreeFilename = vaConceptTree.getPath();
            ArquillianTestUtils.compareJsonFiles(conceptsTreeFilename,
                    testsPath
                    + CLASS_NAME_PREFIX
                    + "testConceptTreeTransformProvider1/"
                    + "test-data1-concepts_tree.json");

            task = TaskDAO.getTaskById(2);
            version = VersionDAO.getCurrentVersionByVersionId(em, 2);
            taskInfo = new TaskInfo(task, vocabulary, version);
            taskInfo.setEm(em);
            taskInfo.setModifiedBy("SYSTEM");
            taskInfo.setNowTime(nowTime1);
            taskInfo.process();
            workflowTask = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
                    "ConceptTreeTransformProvider failed on task 2");
            va = VersionArtefactDAO.
                    getCurrentVersionArtefactListForVersionByType(2,
                            VersionArtefactType.CONCEPT_TREE, em).get(0);
            vaConceptTree = JSONSerialization.deserializeStringAsJson(
                    va.getData(), VaConceptTree.class);
            conceptsTreeFilename = vaConceptTree.getPath();
            // Note the use of the same correct output as the previous test.
            ArquillianTestUtils.compareJsonFiles(conceptsTreeFilename,
                    testsPath
                    + CLASS_NAME_PREFIX
                    + "testConceptTreeTransformProvider1/"
                    + "test-data1-concepts_tree.json");

            // Polyhierarchy detection: we get an artefact in this case.
            task = TaskDAO.getTaskById(3);
            version = VersionDAO.getCurrentVersionByVersionId(em, 3);
            taskInfo = new TaskInfo(task, vocabulary, version);
            taskInfo.setEm(em);
            taskInfo.setModifiedBy("SYSTEM");
            taskInfo.setNowTime(nowTime1);
            taskInfo.process();
            workflowTask = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
                    "ConceptTreeTransformProvider failed on task 3");
            va = VersionArtefactDAO.
                    getCurrentVersionArtefactListForVersionByType(3,
                            VersionArtefactType.CONCEPT_TREE, em).get(0);
            vaConceptTree = JSONSerialization.deserializeStringAsJson(
                    va.getData(), VaConceptTree.class);
            conceptsTreeFilename = vaConceptTree.getPath();
            ArquillianTestUtils.compareJsonFiles(conceptsTreeFilename,
                    testsPath
                    + CLASS_NAME_PREFIX
                    + "testConceptTreeTransformProvider1/"
                    + "test-data3-concepts_tree.json");

            // Cycle detection: we _don't_ get an artefact in this case.
            task = TaskDAO.getTaskById(4);
            version = VersionDAO.getCurrentVersionByVersionId(em, 4);
            taskInfo = new TaskInfo(task, vocabulary, version);
            taskInfo.setEm(em);
            taskInfo.setModifiedBy("SYSTEM");
            taskInfo.setNowTime(nowTime1);
            taskInfo.process();
            workflowTask = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.PARTIAL,
                    "ConceptTreeTransformProvider failed on task 4");
            Assert.assertEquals(VersionArtefactDAO.
                    getCurrentVersionArtefactListForVersionByType(4,
                            VersionArtefactType.CONCEPT_TREE, em).size(), 0,
                    "ConceptTreeTransformProvider task 4 created a "
                            + "version artefact");
            Assert.assertEquals(workflowTask.getSubtasks().get(0).
                    getResults().get(ConceptTreeTransformProvider.
                            CONCEPTS_TREE_NOT_PROVIDED),
                    "No concepts tree provided, because there is a cycle.",
                    "ConceptTreeTransformProvider task 4 returned "
                    + "wrong value for "
                    + ConceptTreeTransformProvider.CONCEPTS_TREE_NOT_PROVIDED);

            // Multilingual vocabularies, giving preference to labels
            // in the primary language.
            task = TaskDAO.getTaskById(5);
            version = VersionDAO.getCurrentVersionByVersionId(em, 5);
            taskInfo = new TaskInfo(task, vocabulary, version);
            taskInfo.setEm(em);
            taskInfo.setModifiedBy("SYSTEM");
            taskInfo.setNowTime(nowTime1);
            taskInfo.process();
            workflowTask = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
                    "ConceptTreeTransformProvider failed on task 5");
            va = VersionArtefactDAO.
                    getCurrentVersionArtefactListForVersionByType(5,
                            VersionArtefactType.CONCEPT_TREE, em).get(0);
            vaConceptTree = JSONSerialization.deserializeStringAsJson(
                    va.getData(), VaConceptTree.class);
            conceptsTreeFilename = vaConceptTree.getPath();
            ArquillianTestUtils.compareJsonFiles(conceptsTreeFilename,
                    testsPath
                    + CLASS_NAME_PREFIX
                    + "testConceptTreeTransformProvider1/"
                    + "test-data5-concepts_tree.json");
            txn.commit();
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

    // Task numbers >= 3 generate magic number warnings.
    /** Server-side test of {@code ConceptTreeTransformProvider}.
     * The test data of this test exercises the support for notation
     * formats and sorting.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     */
    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public final void testConceptTreeTransformProvider2() throws
        DatabaseUnitException, HibernateException, IOException, SQLException {
        String testsPath = ArquillianTestUtils.getClassesPath()
                + "/test/tests/";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testConceptTreeTransformProvider2");

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
            logger.info("testConceptTreeTransformProvider2: task list length = "
                    + taskList.size());
            Assert.assertEquals(taskList.size(), 3, "Not three tasks");

            Task task = TaskDAO.getTaskById(1);
            version = VersionDAO.getCurrentVersionByVersionId(em, 1);
            taskInfo = new TaskInfo(task, vocabulary, version);
            taskInfo.setEm(em);
            taskInfo.setModifiedBy("SYSTEM");
            taskInfo.setNowTime(nowTime1);
            taskInfo.process();
            workflowTask = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
                "ConceptTreeTransformProvider failed on task 1");

            va = VersionArtefactDAO.
                    getCurrentVersionArtefactListForVersionByType(1,
                            VersionArtefactType.CONCEPT_TREE, em).get(0);
            vaConceptTree = JSONSerialization.deserializeStringAsJson(
                    va.getData(), VaConceptTree.class);

            String conceptsTreeFilename = vaConceptTree.getPath();
            ArquillianTestUtils.compareJsonFiles(conceptsTreeFilename,
                    testsPath
                    + CLASS_NAME_PREFIX
                    + "testConceptTreeTransformProvider2/"
                    + "test-data1-concepts_tree.json");

            // Wrong notation format specified, so there's a parse error.
            task = TaskDAO.getTaskById(2);
            version = VersionDAO.getCurrentVersionByVersionId(em, 2);
            taskInfo = new TaskInfo(task, vocabulary, version);
            taskInfo.setEm(em);
            taskInfo.setModifiedBy("SYSTEM");
            taskInfo.setNowTime(nowTime1);
            taskInfo.process();
            workflowTask = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.PARTIAL,
                    "ConceptTreeTransformProvider failed on task 2");
            va = VersionArtefactDAO.
                    getCurrentVersionArtefactListForVersionByType(2,
                            VersionArtefactType.CONCEPT_TREE, em).get(0);
            vaConceptTree = JSONSerialization.deserializeStringAsJson(
                    va.getData(), VaConceptTree.class);
            conceptsTreeFilename = vaConceptTree.getPath();
            // Note the use of the same correct output as the previous test.
            ArquillianTestUtils.compareJsonFiles(conceptsTreeFilename,
                    testsPath
                    + CLASS_NAME_PREFIX
                    + "testConceptTreeTransformProvider2/"
                    + "test-data2-concepts_tree.json");

            // Polyhierarchy detection: we get an artefact in this case.
            task = TaskDAO.getTaskById(3);
            version = VersionDAO.getCurrentVersionByVersionId(em, 3);
            taskInfo = new TaskInfo(task, vocabulary, version);
            taskInfo.setEm(em);
            taskInfo.setModifiedBy("SYSTEM");
            taskInfo.setNowTime(nowTime1);
            taskInfo.process();
            workflowTask = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
                    "ConceptTreeTransformProvider failed on task 3");
            va = VersionArtefactDAO.
                    getCurrentVersionArtefactListForVersionByType(3,
                            VersionArtefactType.CONCEPT_TREE, em).get(0);
            vaConceptTree = JSONSerialization.deserializeStringAsJson(
                    va.getData(), VaConceptTree.class);
            conceptsTreeFilename = vaConceptTree.getPath();
            ArquillianTestUtils.compareJsonFiles(conceptsTreeFilename,
                    testsPath
                    + CLASS_NAME_PREFIX
                    + "testConceptTreeTransformProvider2/"
                    + "test-data3-concepts_tree.json");

            txn.commit();
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

    /** Helper method to run through test data that has errors in
     * the SKOS.
     * @param em The EntityManager to use.
     * @param vocabulary The vocabulary under test.
     * @param taskId The task ID of the task to run.
     * @param expectedRdfErrorsString The text of the expected RDF errors.
     */
    private void testRdfErrors(final EntityManager em,
            final Vocabulary vocabulary,
            final int taskId, final String expectedRdfErrorsString) {
        Task task = TaskDAO.getTaskById(taskId);
        Version version = VersionDAO.getCurrentVersionByVersionId(em, taskId);
        TaskInfo taskInfo = new TaskInfo(task, vocabulary, version);
        taskInfo.setEm(em);
        taskInfo.setModifiedBy("SYSTEM");
        taskInfo.setNowTime(nowTime1);
        taskInfo.process();
        au.org.ands.vocabs.registry.workflow.tasks.Task workflowTask =
                taskInfo.getTask();

        Assert.assertEquals(workflowTask.getStatus(), TaskStatus.PARTIAL,
                "ConceptTreeTransformProvider failed on task " + taskId);

        String actualSubtaskResultAlertHtml =
                workflowTask.getSubtasks().get(0).getResults().
                get(TaskRunner.ALERT_HTML);

        String expectedSubtaskResultAlertHtml =
                ConceptTreeTransformProvider.ALERT_HTML_PRELUDE
                + version.getSlug()
                + ConceptTreeTransformProvider.ALERT_HTML_INTERLUDE
                + expectedRdfErrorsString
                + ConceptTreeTransformProvider.ALERT_HTML_POSTLUDE;

        Assert.assertEquals(actualSubtaskResultAlertHtml,
                expectedSubtaskResultAlertHtml,
                "Wrong value for alert-html");
    }

    /** Server-side test of {@code ConceptTreeTransformProvider}.
     * The test data of this test exercises the error checking of
     * RDF data.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     */
    @Test
    @SuppressWarnings({"checkstyle:MagicNumber", "checkstyle:MethodLength"})
    public final void testConceptTreeTransformProvider3() throws
        DatabaseUnitException, HibernateException, IOException, SQLException {
//        String testsPath = ArquillianTestUtils.getClassesPath()
//                + "/test/tests/";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testConceptTreeTransformProvider3");

        EntityManager em = null;
        EntityTransaction txn = null;
        try {
            em = DBContext.getEntityManager();
            txn = em.getTransaction();
            txn.begin();

            Vocabulary vocabulary = VocabularyDAO.
                    getCurrentVocabularyByVocabularyId(em, 1);

            List<Task> taskList = TaskDAO.getAllTask();
            logger.info("testConceptTreeTransformProvider3: task list length = "
                    + taskList.size());
            Assert.assertEquals(taskList.size(), 9, "Not nine tasks");

            // Many errors, but most unreported because this time we
            // have includeCollectionSchemes and includeCollections
            // both false.
            String phase1Errors =
                    StatementHandler.RDF_ERROR_TYPE_LITERAL
                    + "Literal type" + ConceptTreeTransformProvider.BR
                    + StatementHandler.RDF_ERROR_INVALID_TYPE_CHANGE
                    + "http://test/two-types-Concept-ConceptScheme; "
                    + "from type concept to concept scheme"
                    + ConceptTreeTransformProvider.BR
                    + StatementHandler.RDF_ERROR_INVALID_TYPE_CHANGE
                    + "http://test/two-types-OrderedCollection-ConceptScheme; "
                    + "from type ordered collection to concept scheme"
                    + ConceptTreeTransformProvider.BR
                    + StatementHandler.RDF_ERROR_INVALID_TYPE_CHANGE
                    + "http://test/two-types-OrderedCollection-Concept; "
                    + "from type ordered collection to concept"
                    + ConceptTreeTransformProvider.BR
                    + StatementHandler.RDF_ERROR_INVALID_TYPE_CHANGE
                    + "http://test/two-types-"
                        + "UnorderedCollection-ConceptScheme; "
                    + "from type unordered collection to concept scheme"
                    + ConceptTreeTransformProvider.BR
                    + StatementHandler.RDF_ERROR_INVALID_TYPE_CHANGE
                    + "http://test/two-types-UnorderedCollection-Concept; "
                    + "from type unordered collection to concept"
                    + ConceptTreeTransformProvider.BR
                    + StatementHandler.RDF_ERROR_INVALID_TYPE_CHANGE
                    + "http://test/broader1; "
                    + "from type concept scheme to concept"
                    + ConceptTreeTransformProvider.BR
                    + StatementHandler.RDF_ERROR_INVALID_TYPE_CHANGE
                    + "http://test/narrower2; "
                    + "from type concept scheme to concept"
                    + ConceptTreeTransformProvider.BR;
            testRdfErrors(em, vocabulary, 1, phase1Errors);

            // Same data, but this time we set includeCollectionSchemes and
            // includeCollections to true, so many more errors are reported.
            testRdfErrors(em, vocabulary, 2,
                    phase1Errors
                    + StatementHandler.RDF_ERROR_MEMBER_LITERAL
                    + "Literal member" + ConceptTreeTransformProvider.BR
                    + StatementHandler.RDF_ERROR_MEMBERLIST_LITERAL
                    + "Literal memberList" + ConceptTreeTransformProvider.BR
                    + StatementHandler.RDF_ERROR_MULTIPLE_MEMBERLIST
                    + "http://test/two-memberLists"
                    + ConceptTreeTransformProvider.BR
                    + StatementHandler.RDF_ERROR_LIST_FIRST_NIL
                    + "http://test/memberList1"
                    + ConceptTreeTransformProvider.BR
                    + StatementHandler.RDF_ERROR_LIST_MULTIPLE_FIRST
                    + "http://test/memberList2"
                    + ConceptTreeTransformProvider.BR
                    + StatementHandler.RDF_ERROR_LIST_REST_LITERAL
                    + "Literal rest"
                    + ConceptTreeTransformProvider.BR
                    + StatementHandler.RDF_ERROR_LIST_MULTIPLE_REST
                    + "http://test/memberList4"
                    + ConceptTreeTransformProvider.BR);

            // Errors only detected during depth-first search.
            testRdfErrors(em, vocabulary, 3,
                    StatementHandler.RDF_ERROR_CS_MEMBER_NOT_CONCEPT
                    + "http://test/Coll2" + ConceptTreeTransformProvider.BR
                    + StatementHandler.RDF_ERROR_COLL_MEMBER_NOT_VALID
                    + "http://test/CS3" + ConceptTreeTransformProvider.BR
                    + StatementHandler.RDF_ERROR_COLL_MEMBER_NOT_VALID
                    + "http://test/CS4" + ConceptTreeTransformProvider.BR
                    + StatementHandler.RDF_ERROR_TOP_CONCEPT_BROADER
                    + "http://test/TC" + ConceptTreeTransformProvider.BR);

            // Now the "showstoppers". Each one is a separate case.
            testRdfErrors(em, vocabulary, 4,
                    StatementHandler.RDF_ERROR_MEMBERLIST_ELEMENT_LITERAL
                    + "Literal memberList member"
                    + ConceptTreeTransformProvider.BR);
            testRdfErrors(em, vocabulary, 5,
                    StatementHandler.RDF_ERROR_MEMBERLIST_CYCLE
                    + "http://test/memberList-cycle"
                    + ConceptTreeTransformProvider.BR);
            testRdfErrors(em, vocabulary, 6,
                    StatementHandler.RDF_ERROR_MEMBER_NOT_IN_MEMBERLIST
                    + "http://test/memberList-and-other-member"
                    + ConceptTreeTransformProvider.BR);
            testRdfErrors(em, vocabulary, 7,
                    StatementHandler.RDF_ERROR_LIST_MEMBERLIST_ELEMENT_NOT_VALID
                    + "http://test/unknownType"
                    + ConceptTreeTransformProvider.BR);
            testRdfErrors(em, vocabulary, 8,
                    StatementHandler.RDF_ERROR_MEMBER_UNKNOWN_TYPE
                    + "http://test/member-unknown-type"
                    + StatementHandler.RDF_ERROR_MEMBER_UNKNOWN_TYPE_RESOURCE
                    + "http://test/member-unknown-type-resource"
                    + ConceptTreeTransformProvider.BR);

            // An error generated by the RDF parser that doesn't bubble up
            // through StatementHandler. The text of the error message
            // comes from org.openrdf.model.impl.URIImpl.setURIString(),
            // which is then munged by org.openrdf.rio.RDFParseException to
            // include the line number information.
            testRdfErrors(em, vocabulary, 9,
                    "Not a valid (absolute) URI: f [line 1]"
                    + ConceptTreeTransformProvider.BR);

            txn.commit();
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

    /** Server-side test of {@code ConceptTreeTransformProvider}.
     * The test data of this test exercises the behaviour of the
     * {@code mayResolveResources} browse flag.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     */
    @Test
    public final void testConceptTreeTransformProvider4() throws
        DatabaseUnitException, HibernateException, IOException, SQLException {
        String testsPath = ArquillianTestUtils.getClassesPath()
                + "/test/tests/";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testConceptTreeTransformProvider4");

        EntityManager em = null;
        EntityTransaction txn = null;
        try {
            em = DBContext.getEntityManager();
            txn = em.getTransaction();
            txn.begin();

            List<Task> taskList = TaskDAO.getAllTask();
            logger.info("testConceptTreeTransformProvider4: task list length = "
                    + taskList.size());
            Assert.assertEquals(taskList.size(), 2, "Not two tasks");

            Vocabulary vocabulary = VocabularyDAO.
                    getCurrentVocabularyByVocabularyId(em, 1);

            testRdfErrors(em, vocabulary, 1,
                    Resource.UNABLE_TO_NORMALIZE
                    + "http://G&auml;nsef&uuml;&szlig;chen-"
                    + "&Gamma;&epsilon;&iota;ά-&sigma;&alpha;&sigmaf;-"
                    + "नमस्ते-你好.com/x"
                    + ConceptTreeTransformProvider.BR
                    + StatementHandler.UNABLE_TO_CONTINUE
                    + ConceptTreeTransformProvider.BR);

            Version version;

            TaskInfo taskInfo;
            au.org.ands.vocabs.registry.workflow.tasks.Task workflowTask;
            VersionArtefact va;
            VaConceptTree vaConceptTree;

            Task task = TaskDAO.getTaskById(2);
            version = VersionDAO.getCurrentVersionByVersionId(em, 2);
            taskInfo = new TaskInfo(task, vocabulary, version);
            taskInfo.setEm(em);
            taskInfo.setModifiedBy("SYSTEM");
            taskInfo.setNowTime(nowTime1);
            taskInfo.process();

            txn.commit();
            // If a dump is required, uncomment the next lines.
//   ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//           "testConceptTreeTransformProvider4-out.xml");

            workflowTask = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
                "ConceptTreeTransformProvider failed on task 2");

            va = VersionArtefactDAO.
                    getCurrentVersionArtefactListForVersionByType(2,
                            VersionArtefactType.CONCEPT_TREE, em).get(0);
            vaConceptTree = JSONSerialization.deserializeStringAsJson(
                    va.getData(), VaConceptTree.class);

            String conceptsTreeFilename = vaConceptTree.getPath();
            ArquillianTestUtils.compareJsonFiles(conceptsTreeFilename,
                    testsPath
                    + CLASS_NAME_PREFIX
                    + "testConceptTreeTransformProvider4/"
                    + "test-data2-concepts_tree.json");

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

    /** Server-side test of {@code ConceptTreeTransformProvider}.
     * The test data of this test exercises cycle detection of collections.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     */
    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public final void testConceptTreeTransformProvider5() throws
        DatabaseUnitException, HibernateException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testConceptTreeTransformProvider5");

        EntityManager em = null;
        EntityTransaction txn = null;
        try {
            em = DBContext.getEntityManager();
            txn = em.getTransaction();
            txn.begin();

            List<Task> taskList = TaskDAO.getAllTask();
            logger.info("testConceptTreeTransformProvider5: task list length = "
                    + taskList.size());
            Assert.assertEquals(taskList.size(), 8, "Not eight tasks");

            Vocabulary vocabulary = VocabularyDAO.
                    getCurrentVocabularyByVocabularyId(em, 1);

            testRdfErrors(em, vocabulary, 1,
                    /*
                    StatementHandler.RDF_ERROR_CYCLE_COLLECTION_UNVISITED
                    + "http://test/Coll2" + ConceptTreeTransformProvider.BR
                    +
                    */
                    StatementHandler.RDF_ERROR_CYCLE_COLLECTION_BACK_EDGE
                    + "http://test/Coll1 to http://test/Coll2"
                    + ConceptTreeTransformProvider.BR);

            testRdfErrors(em, vocabulary, 2,
                    /*
                    StatementHandler.RDF_ERROR_CYCLE_COLLECTION_UNVISITED
                    + "http://test/Coll2" + ConceptTreeTransformProvider.BR
                    +
                    */
                    StatementHandler.RDF_ERROR_CYCLE_COLLECTION_BACK_EDGE
                    + "http://test/Coll1 to http://test/Coll2"
                    + ConceptTreeTransformProvider.BR);

            testRdfErrors(em, vocabulary, 3,
                    StatementHandler.RDF_ERROR_CYCLE_COLLECTION_BACK_EDGE
                    + "http://test/Coll3 to http://test/Coll2"
                    + ConceptTreeTransformProvider.BR);

            testRdfErrors(em, vocabulary, 4,
                    StatementHandler.RDF_ERROR_CYCLE_COLLECTION_BACK_EDGE
                    + "http://test/Coll3 to http://test/Coll2"
                    + ConceptTreeTransformProvider.BR);

            testRdfErrors(em, vocabulary, 5,
                    /*
                    StatementHandler.RDF_ERROR_CYCLE_COLLECTION_UNVISITED
                    + "http://test/Coll2" + ConceptTreeTransformProvider.BR
                    +
                    */
                    StatementHandler.RDF_ERROR_CYCLE_COLLECTION_BACK_EDGE
                    + "http://test/Coll1 to http://test/Coll2"
                    + ConceptTreeTransformProvider.BR);

            testRdfErrors(em, vocabulary, 6,
                    StatementHandler.RDF_ERROR_CYCLE_COLLECTION_BACK_EDGE
                    + "http://test/Coll3 to http://test/Coll2"
                    + ConceptTreeTransformProvider.BR);

            testRdfErrors(em, vocabulary, 7,
                    StatementHandler.RDF_ERROR_CYCLE_COLLECTION_BACK_EDGE
                    + "http://test/Coll3 to http://test/Coll2"
                    + ConceptTreeTransformProvider.BR);

            // This test exercises the places in the code where we
            // take care, in the case that we broke a cycle, that we
            // don't subsequently try to compare a node that has
            // an orderedCollectionSortOrder value with a node that doesn't.
            String v8Prefix =
                    "https://editor.vocabs.ands.org.au/ANDSRWtestforbrowse1/";
            testRdfErrors(em, vocabulary, 8,
                    /*
                    StatementHandler.RDF_ERROR_CYCLE_COLLECTION_UNVISITED
                    + v8Prefix + "OrderedCollection3"
                    + ConceptTreeTransformProvider.BR
                    +
                    */
                    StatementHandler.RDF_ERROR_CYCLE_COLLECTION_BACK_EDGE
                    + v8Prefix + "OrderedCollection1 to "
                    + v8Prefix + "OrderedCollection3"
                    + ConceptTreeTransformProvider.BR
                    /*
                    + StatementHandler.RDF_ERROR_CYCLE_CONCEPT_UNVISITED
                    + v8Prefix + "C1.1.1" + ConceptTreeTransformProvider.BR
                    + StatementHandler.RDF_ERROR_CYCLE_CONCEPT_UNVISITED
                    + v8Prefix + "C2.1.1" + ConceptTreeTransformProvider.BR
                    + StatementHandler.RDF_ERROR_CYCLE_CONCEPT_UNVISITED
                    + v8Prefix + "C1.2.1.1" + ConceptTreeTransformProvider.BR
                    + StatementHandler.RDF_ERROR_CYCLE_CONCEPT_UNVISITED
                    + v8Prefix + "C1.1.3" + ConceptTreeTransformProvider.BR
                    + StatementHandler.RDF_ERROR_CYCLE_CONCEPT_UNVISITED
                    + v8Prefix + "TC1.1" + ConceptTreeTransformProvider.BR
                    */
                    );

            txn.commit();
            // If a dump is required, uncomment the next lines.
//   ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//           "testConceptTreeTransformProvider5-out.xml");

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

    /** Do one test of concept tree processing, expecting success,
     * and match it against the expected result.
     * @param em The EntityManager to use.
     * @param vocabulary The vocabulary under test.
     * @param versionId The task ID of the task to run.
     * @param taskId The task ID of the task to run.
     * @param testsPath The path to the test data.
     * @param testName The name of the test, used as a directory name.
     * @throws IOException If a problem reading JSON from the correct
     *      and test output files.
     */
    private void testConceptTreeProcessing(final EntityManager em,
            final Vocabulary vocabulary, final int versionId,
            final int taskId, final String testsPath,
            final String testName) throws IOException {
        Version version = VersionDAO.getCurrentVersionByVersionId(em,
                versionId);
        Task task = TaskDAO.getTaskById(taskId);
        TaskInfo taskInfo = new TaskInfo(task, vocabulary, version);
        taskInfo.setEm(em);
        taskInfo.setModifiedBy("SYSTEM");
        taskInfo.setNowTime(nowTime1);
        taskInfo.process();
        au.org.ands.vocabs.registry.workflow.tasks.Task workflowTask =
                taskInfo.getTask();

        Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
                "ConceptTreeTransformProvider failed on task " + taskId);
        VersionArtefact va = VersionArtefactDAO.
                getCurrentVersionArtefactListForVersionByType(versionId,
                        VersionArtefactType.CONCEPT_TREE, em).get(0);
        VaConceptTree vaConceptTree =
                JSONSerialization.deserializeStringAsJson(
                va.getData(), VaConceptTree.class);
        String conceptsTreeFilename = vaConceptTree.getPath();
        ArquillianTestUtils.compareJsonFiles(conceptsTreeFilename,
                testsPath
                + CLASS_NAME_PREFIX
                + testName + "/test-data" + taskId + "-concepts_tree.json");
    }

    /** Server-side test of {@code ConceptTreeTransformProvider}.
     * The test data of this test exercises the processing of
     * concept schemes and collections.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     */
    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public final void testConceptTreeTransformProvider6() throws
        DatabaseUnitException, HibernateException, IOException, SQLException {
        String testsPath = ArquillianTestUtils.getClassesPath()
                + "/test/tests/";
        String testName = "testConceptTreeTransformProvider6";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + testName);

        EntityManager em = null;
        EntityTransaction txn = null;
        try {
            em = DBContext.getEntityManager();
            txn = em.getTransaction();
            txn.begin();

            List<Task> taskList = TaskDAO.getAllTask();
            logger.info(testName + ": task list length = " + taskList.size());
            Assert.assertEquals(taskList.size(), 8, "Not eight tasks");

            Vocabulary vocabulary = VocabularyDAO.
                    getCurrentVocabularyByVocabularyId(em, 1);

            testConceptTreeProcessing(em, vocabulary, 1, 1,
                    testsPath, testName);
            testConceptTreeProcessing(em, vocabulary, 2, 2,
                    testsPath, testName);
            testConceptTreeProcessing(em, vocabulary, 3, 3,
                    testsPath, testName);
            testConceptTreeProcessing(em, vocabulary, 4, 4,
                    testsPath, testName);
            testConceptTreeProcessing(em, vocabulary, 5, 5,
                    testsPath, testName);
            testConceptTreeProcessing(em, vocabulary, 6, 6,
                    testsPath, testName);
            testConceptTreeProcessing(em, vocabulary, 7, 7,
                    testsPath, testName);
            testConceptTreeProcessing(em, vocabulary, 8, 8,
                    testsPath, testName);

            txn.commit();
            // If a dump is required, uncomment the next lines.
//   ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//           "testConceptTreeTransformProvider6-out.xml");

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



    // Tests of class
    // au.org.ands.vocabs.registry.workflow.
    //     provider.transform.ResourceMapTransformProvider.

    /** Server-side test 1 of {@code ResourceMapTransformProvider}.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     */
    @Test
    public final void testResourceMapTransformProvider1() throws
        DatabaseUnitException, HibernateException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testResourceMapTransformProvider1");

        EntityManager em = null;
        EntityTransaction txn = null;
        try {
            em = DBContext.getEntityManager();
            txn = em.getTransaction();
            txn.begin();

            Vocabulary vocabulary = VocabularyDAO.
                    getCurrentVocabularyByVocabularyId(em, 1);
            Version version = VersionDAO.getCurrentVersionByVersionId(em, 1);

            TaskInfo taskInfo;
            au.org.ands.vocabs.registry.workflow.tasks.Task workflowTask;

            List<Task> taskList = TaskDAO.getAllTask();
            logger.info("testResourceMapTransformProvider1: task list length = "
                + taskList.size());

            Task task = TaskDAO.getTaskById(1);
            taskInfo = new TaskInfo(task, vocabulary, version);

            taskInfo.setEm(em);
            taskInfo.setModifiedBy("SYSTEM");
            taskInfo.setNowTime(nowTime1);
            taskInfo.process();
            workflowTask = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
                "ResourceMapTransformProvider failed on task 1");
            txn.commit();
            // If a dump is required, uncomment the next line.
//          ArquillianTestUtils.exportFullDbUnitData(REGISTRY, "trmtp1.xml");

            // Get current contents of resource_map table.
            ITable actualTable = ArquillianTestUtils.
                    getDatabaseTableCurrentContents(REGISTRY,
                            DbUnitConstants.RESOURCEMAPENTRY_TABLE_NAME);
            // And take out the id and access_point_id columns before
            // doing a comparison.
            ITable filteredActualTable =
                    DefaultColumnFilter.excludedColumnsTable(
                        actualTable, new String[]{"ID", "ACCESS_POINT_ID"});

            IDataSet expectedDataSet = ArquillianTestUtils.
                    getDatabaseTableExpectedContents(REGISTRY,
                            "test/tests/"
                                    + CLASS_NAME_PREFIX
                                    + "testResourceMapTransformProvider1/"
                                    + "test-data1-results.xml");
            ITable expectedTable = expectedDataSet.getTable(
                    DbUnitConstants.RESOURCEMAPENTRY_TABLE_NAME);
            ITable filteredExpectedTable =
                    DefaultColumnFilter.excludedColumnsTable(
                        expectedTable, new String[]{"ID", "ACCESS_POINT_ID"});
            Assertion.assertEquals(new SortedTable(filteredExpectedTable),
                    new SortedTable(filteredActualTable,
                            filteredExpectedTable.getTableMetaData()));

            // Now do an UNTRANSFORM.
            txn.begin();
            task = TaskDAO.getTaskById(2);
            taskInfo = new TaskInfo(task, vocabulary, version);
            taskInfo.setEm(em);
            taskInfo.setModifiedBy("SYSTEM");
            taskInfo.setNowTime(nowTime1);
            taskInfo.process();
            workflowTask = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
                    "ResourceMapTransformProvider failed on task 2");
            // Get current contents of resource_map table again.
            txn.commit();
            actualTable = ArquillianTestUtils.
                    getDatabaseTableCurrentContents(REGISTRY,
                            DbUnitConstants.RESOURCEMAPENTRY_TABLE_NAME);
            Assert.assertEquals(actualTable.getRowCount(), 0,
                    "Empty resource_map table after UNTRANSFORM");
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

    /** Server-side test 2 of {@code ResourceMapTransformProvider}.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     */
    @Test
    public final void testResourceMapTransformProvider2() throws
        DatabaseUnitException, HibernateException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testResourceMapTransformProvider2");

        EntityManager em = null;
        EntityTransaction txn = null;
        try {
            em = DBContext.getEntityManager();
            txn = em.getTransaction();
            txn.begin();

            Vocabulary vocabulary = VocabularyDAO.
                    getCurrentVocabularyByVocabularyId(em, 1);
            Version version = VersionDAO.getCurrentVersionByVersionId(em, 1);

            TaskInfo taskInfo;
            au.org.ands.vocabs.registry.workflow.tasks.Task workflowTask;

            List<Task> taskList = TaskDAO.getAllTask();
            logger.info("testResourceMapTransformProvider2: task list length = "
                    + taskList.size());

            Task task = TaskDAO.getTaskById(1);
            taskInfo = new TaskInfo(task, vocabulary, version);

            taskInfo.setEm(em);
            taskInfo.setModifiedBy("SYSTEM");
            taskInfo.setNowTime(nowTime1);
            taskInfo.process();
            workflowTask = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
                    "ResourceMapTransformProvider failed on task 1");
            txn.commit();
            // If a dump is required, uncomment the next line.
//          ArquillianTestUtils.exportFullDbUnitData(REGISTRY, "trmtp2.xml");

            // Get current contents of resource_map table.
            ITable actualTable = ArquillianTestUtils.
                    getDatabaseTableCurrentContents(REGISTRY,
                            DbUnitConstants.RESOURCEMAPENTRY_TABLE_NAME);
            // And take out the id and access_point_id columns before
            // doing a comparison.
            ITable filteredActualTable =
                    DefaultColumnFilter.excludedColumnsTable(
                        actualTable, new String[]{"ID", "ACCESS_POINT_ID"});

            IDataSet expectedDataSet = ArquillianTestUtils.
                    getDatabaseTableExpectedContents(REGISTRY,
                            "test/tests/"
                                    + CLASS_NAME_PREFIX
                                    + "testResourceMapTransformProvider2/"
                                    + "test-data1-results.xml");
            ITable expectedTable = expectedDataSet.getTable(
                    DbUnitConstants.RESOURCEMAPENTRY_TABLE_NAME);
            ITable filteredExpectedTable =
                    DefaultColumnFilter.excludedColumnsTable(
                        expectedTable, new String[]{"ID", "ACCESS_POINT_ID"});
            Assertion.assertEquals(new SortedTable(filteredExpectedTable),
                    new SortedTable(filteredActualTable,
                            filteredExpectedTable.getTableMetaData()));
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

    /** Server-side test 3 of {@code ResourceMapTransformProvider}.
     * A test of resources of all SKOS types, and of deprecation,
     * where there are two vocabularies with two different owners, and
     * there is some overlap in ownership of hosts. There is also a
     * test of multiple definitions of a concept within the same
     * vocabulary.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     */
    @Test
    public final void testResourceMapTransformProvider3() throws
        DatabaseUnitException, HibernateException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testResourceMapTransformProvider3");

        EntityManager em = null;
        EntityTransaction txn = null;
        try {
            em = DBContext.getEntityManager();
            txn = em.getTransaction();
            txn.begin();

            Vocabulary vocabulary = VocabularyDAO.
                    getCurrentVocabularyByVocabularyId(em, 1);
            Version version = VersionDAO.getCurrentVersionByVersionId(em, 1);

            TaskInfo taskInfo;
            au.org.ands.vocabs.registry.workflow.tasks.Task workflowTask;

            List<Task> taskList = TaskDAO.getAllTask();

            logger.info("testResourceMapTransformProvider3: task list length = "
                    + taskList.size());

            Task task = TaskDAO.getTaskById(1);
            taskInfo = new TaskInfo(task, vocabulary, version);

            taskInfo.setEm(em);
            taskInfo.setModifiedBy("SYSTEM");
            taskInfo.setNowTime(nowTime1);
            taskInfo.process();
            workflowTask = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
                    "ResourceMapTransformProvider failed on task 1");

            task = TaskDAO.getTaskById(2);
            vocabulary = VocabularyDAO.
                    getCurrentVocabularyByVocabularyId(em, 2);
            version = VersionDAO.getCurrentVersionByVersionId(em, 2);
            taskInfo = new TaskInfo(task, vocabulary, version);
            taskInfo.setEm(em);
            taskInfo.setModifiedBy("SYSTEM");
            taskInfo.setNowTime(nowTime1);
            taskInfo.process();
            workflowTask = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
                    "ResourceMapTransformProvider failed on task 2");
            txn.commit();
            // If a dump is required, uncomment the next line.
//          ArquillianTestUtils.exportFullDbUnitData(REGISTRY, "trmtp3.xml");

            // Get current contents of resource_map table.
            ITable actualTable = ArquillianTestUtils.
                    getDatabaseTableCurrentContents(REGISTRY,
                            DbUnitConstants.RESOURCEMAPENTRY_TABLE_NAME);
            // And take out the id column before
            // doing a comparison. Cf. the other tests, in which we also
            // take out the access_point_id column. This time, we have
            // two access points in play, so we rely on the database
            // cleaning having reset the counters, so that generated ids can be
            // relied on to start from 1.
            ITable filteredActualTable =
                    DefaultColumnFilter.excludedColumnsTable(
                            actualTable, new String[]{"ID"});

            IDataSet expectedDataSet = ArquillianTestUtils.
                    getDatabaseTableExpectedContents(REGISTRY,
                            "test/tests/"
                                    + CLASS_NAME_PREFIX
                                    + "testResourceMapTransformProvider3/"
                                    + "test-data1-results.xml");
            ITable expectedTable = expectedDataSet.getTable(
                    DbUnitConstants.RESOURCEMAPENTRY_TABLE_NAME);
            ITable filteredExpectedTable =
                    DefaultColumnFilter.excludedColumnsTable(
                            expectedTable, new String[]{"ID"});
            Assertion.assertEquals(new SortedTable(filteredExpectedTable),
                    new SortedTable(filteredActualTable,
                            filteredExpectedTable.getTableMetaData()));
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

    // Tests of class
    // au.org.ands.vocabs.registry.workflow.
    //     provider.transform.ResourceDocsTransformProvider.

    // Task numbers >= 3 generate magic number warnings.
    /** Server-side test of {@code ResourceDocsTransformProvider}.
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
    public final void testResourceDocsTransformProvider1() throws
        DatabaseUnitException, HibernateException, IOException, SQLException {
        String testName = CLASS_NAME_PREFIX
                + "testResourceDocsTransformProvider1";
        String testsPath = ArquillianTestUtils.getClassesPath()
                + "/test/tests/";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, testName);

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
            VaResourceDocs vaResourceDocs;

            List<Task> taskList = TaskDAO.getAllTask();
            logger.info("testResourceDocsTransformProvider1: "
                    + "task list length = " + taskList.size());
            Assert.assertEquals(taskList.size(), 5, "Not five tasks");

            Task task = TaskDAO.getTaskById(1);
            version = VersionDAO.getCurrentVersionByVersionId(em, 1);
            taskInfo = new TaskInfo(task, vocabulary, version);
            taskInfo.setEm(em);
            taskInfo.setModifiedBy("SYSTEM");
            taskInfo.setNowTime(nowTime1);
            taskInfo.process();
            workflowTask = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
                "ResourceDocsTransformProvider failed on task 1");

            va = VersionArtefactDAO.
                    getCurrentVersionArtefactListForVersionByType(1,
                            VersionArtefactType.RESOURCE_DOCS, em).get(0);
            vaResourceDocs = JSONSerialization.deserializeStringAsJson(
                    va.getData(), VaResourceDocs.class);

            String conceptsTreeFilename = vaResourceDocs.getPath();
            ArquillianTestUtils.compareJsonFiles(conceptsTreeFilename,
                    testsPath + testName + "/test-data1-resource_docs.json");

            // Uncomment as needed for further tests.

//            task = TaskDAO.getTaskById(2);
//            version = VersionDAO.getCurrentVersionByVersionId(em, 2);
//            taskInfo = new TaskInfo(task, vocabulary, version);
//            taskInfo.setEm(em);
//            taskInfo.setModifiedBy("SYSTEM");
//            taskInfo.setNowTime(nowTime1);
//            taskInfo.process();
//            workflowTask = taskInfo.getTask();
//
//            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
//                    "ResourceDocsTransformProvider failed on task 2");
//            va = VersionArtefactDAO.
//                    getCurrentVersionArtefactListForVersionByType(2,
//                            VersionArtefactType.RESOURCE_DOCS, em).get(0);
//            vaResourceDocs = JSONSerialization.deserializeStringAsJson(
//                    va.getData(), VaResourceDocs.class);
//            conceptsTreeFilename = vaResourceDocs.getPath();
//            // Note the use of the same correct output as the previous test.
//            ArquillianTestUtils.compareJson(conceptsTreeFilename,
//                    testsPath + testName + "/test-data1-resource_docs.json");
//
//            // Polyhierarchy detection: we get an artefact in this case.
//            task = TaskDAO.getTaskById(3);
//            version = VersionDAO.getCurrentVersionByVersionId(em, 3);
//            taskInfo = new TaskInfo(task, vocabulary, version);
//            taskInfo.setEm(em);
//            taskInfo.setModifiedBy("SYSTEM");
//            taskInfo.setNowTime(nowTime1);
//            taskInfo.process();
//            workflowTask = taskInfo.getTask();
//
//            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
//                    "ResourceDocsTransformProvider failed on task 3");
//            va = VersionArtefactDAO.
//                    getCurrentVersionArtefactListForVersionByType(3,
//                            VersionArtefactType.RESOURCE_DOCS, em).get(0);
//            vaResourceDocs = JSONSerialization.deserializeStringAsJson(
//                    va.getData(), VaResourceDocs.class);
//            conceptsTreeFilename = vaResourceDocs.getPath();
//            ArquillianTestUtils.compareJson(conceptsTreeFilename,
//                    testsPath + testName + "/test-data3-resource_docs.json");
//
//            // Cycle detection: we _don't_ get an artefact in this case.
//            task = TaskDAO.getTaskById(4);
//            version = VersionDAO.getCurrentVersionByVersionId(em, 4);
//            taskInfo = new TaskInfo(task, vocabulary, version);
//            taskInfo.setEm(em);
//            taskInfo.setModifiedBy("SYSTEM");
//            taskInfo.setNowTime(nowTime1);
//            taskInfo.process();
//            workflowTask = taskInfo.getTask();
//
//            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.PARTIAL,
//                    "ResourceDocsTransformProvider failed on task 4");
//            Assert.assertEquals(VersionArtefactDAO.
//                    getCurrentVersionArtefactListForVersionByType(4,
//                            VersionArtefactType.RESOURCE_DOCS, em).size(), 0,
//                    "ResourceDocsTransformProvider task 4 created a "
//                            + "version artefact");
//            Assert.assertEquals(workflowTask.getSubtasks().get(0).
//                    getResults().get(ResourceDocsTransformProvider.
//                            CONCEPTS_TREE_NOT_PROVIDED),
//                    "No concepts tree provided, because there is a cycle.",
//                    "ResourceDocsTransformProvider task 4 returned "
//                    + "wrong value for "
//                + ResourceDocsTransformProvider.CONCEPTS_TREE_NOT_PROVIDED);
//
//            // Multilingual vocabularies, giving preference to labels
//            // in the primary language.
//            task = TaskDAO.getTaskById(5);
//            version = VersionDAO.getCurrentVersionByVersionId(em, 5);
//            taskInfo = new TaskInfo(task, vocabulary, version);
//            taskInfo.setEm(em);
//            taskInfo.setModifiedBy("SYSTEM");
//            taskInfo.setNowTime(nowTime1);
//            taskInfo.process();
//            workflowTask = taskInfo.getTask();
//
//            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
//                    "ResourceDocTransformProvider failed on task 5");
//            va = VersionArtefactDAO.
//                    getCurrentVersionArtefactListForVersionByType(5,
//                            VersionArtefactType.RESOURCE_DOCS, em).get(0);
//            vaResourceDocs = JSONSerialization.deserializeStringAsJson(
//                    va.getData(), VaResourceDocs.class);
//            conceptsTreeFilename = vaResourceDocs.getPath();
//            ArquillianTestUtils.compareJson(conceptsTreeFilename,
//                    testsPath + testName + "/test-data5-resource_docs.json");
            txn.commit();
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
