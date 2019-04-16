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
import au.org.ands.vocabs.registry.enums.TaskStatus;
import au.org.ands.vocabs.registry.enums.VersionArtefactType;
import au.org.ands.vocabs.registry.workflow.provider.transform.ConceptTreeTransformProvider;
import au.org.ands.vocabs.registry.workflow.provider.transform.JsonTreeTransformProvider;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;
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
            ArquillianTestUtils.compareJson(conceptsTreeFilename,
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

    // Task numbers 3 and 4 generate magic number warnings.
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
            ArquillianTestUtils.compareJson(conceptsTreeFilename,
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
            ArquillianTestUtils.compareJson(conceptsTreeFilename,
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
            ArquillianTestUtils.compareJson(conceptsTreeFilename,
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
            ArquillianTestUtils.compareJson(conceptsTreeFilename,
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


    // Client-side tests go here. Server-side tests are above this line.

}
