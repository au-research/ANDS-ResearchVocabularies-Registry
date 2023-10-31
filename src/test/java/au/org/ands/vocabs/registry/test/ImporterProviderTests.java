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
import au.org.ands.vocabs.registry.db.dao.TaskDAO;
import au.org.ands.vocabs.registry.db.dao.VersionDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.db.entity.Task;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.enums.TaskStatus;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;
import au.org.ands.vocabs.toolkit.test.arquillian.ArquillianBaseTest;
import au.org.ands.vocabs.toolkit.test.arquillian.ArquillianTestUtils;

/** Tests of the Registry Sesame importer workflow provider.
 * As this class grows, it might be split up further.
 */
@Test
public class ImporterProviderTests extends ArquillianBaseTest {

    /** Logger. */
    private static Logger logger;

    static {
        logger = LoggerFactory.getLogger(
                MethodHandles.lookup().lookupClass());
    }

    /** Name of this class, used in paths to test data files. */
    private static final String CLASS_NAME_PREFIX = "ImporterProviderTests.";

    // Server-side tests go here. Client-side tests later on.

    /** A convenient value to use for endDate properties when
     * deleting. */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static LocalDateTime nowTime1 =
            LocalDateTime.of(2017, 10, 1, 10, 10);

    // Tests of class
    // au.org.ands.vocabs.registry.workflow.provider.importer.
    //   SesameImporterProvider.

    /** Test importing both RDF and non-RDF files. The non-RDF files
     * should not cause the importing process to fail.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     */
    @Test
    public final void testNonRDFImport1() throws
        DatabaseUnitException, HibernateException, IOException, SQLException {
        String testName = CLASS_NAME_PREFIX
                + "testNonRDFImport1";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, testName);
        ArquillianTestUtils.copyTempFilesForTest(testName);

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

            List<Task> taskList = TaskDAO.getAllTask();
            logger.info("testNonRDFImport1: task list length = "
                    + taskList.size());
            Assert.assertEquals(taskList.size(), 1, "Not one task");

            Task task = TaskDAO.getTaskById(1);
            version = VersionDAO.getCurrentVersionByVersionId(em, 1);
            taskInfo = new TaskInfo(task, vocabulary, version);
            taskInfo.setEm(em);
            taskInfo.setModifiedBy("SYSTEM");
            taskInfo.setNowTime(nowTime1);
            taskInfo.process();
            workflowTask = taskInfo.getTask();

            Assert.assertEquals(workflowTask.getStatus(), TaskStatus.SUCCESS,
                "SesameImporterProvider failed on task 1");

            // We're happy if the task succeeds; no further assertion.

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
