/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.provider;

import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.REGISTRY;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import org.dbunit.DatabaseUnitException;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.toolkit.db.TaskUtils;
import au.org.ands.vocabs.toolkit.db.model.Task;
import au.org.ands.vocabs.toolkit.tasks.TaskInfo;
import au.org.ands.vocabs.toolkit.tasks.TaskRunner;
import au.org.ands.vocabs.toolkit.test.arquillian.ArquillianBaseTest;
import au.org.ands.vocabs.toolkit.test.arquillian.ArquillianTestUtils;
import au.org.ands.vocabs.toolkit.utils.ToolkitFileUtils;

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
    @SuppressWarnings("checkstyle:MagicNumber")
    public final void testJsonTreeTransformProvider1() throws
        DatabaseUnitException, HibernateException, IOException, SQLException {

        // TO DO: turn this into a registry test!
        // (If you were to run it now, it would fail.)

        String testsPath = ArquillianTestUtils.getClassesPath()
                + "/test/tests/";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testJsonTreeTransformProvider1");
        List<Task> taskList = TaskUtils.getAllTasks();
        logger.info("testJsonTreeTransformProvider1: task list length = "
                + taskList.size());
        TaskInfo taskInfo = ToolkitFileUtils.getTaskInfo(1);
        Assert.assertNotNull(taskInfo, "Test data not loaded, task 1");
        TaskRunner runner = new TaskRunner(taskInfo);
        runner.runTask();
        HashMap<String, String> results = runner.getResults();

        Assert.assertNotNull(results);
        Assert.assertEquals(results.get("status"), "success",
                "JsonTreeTransformProvider failed on task 1");
        String conceptsTreeFilename = results.get("concepts_tree");
        ArquillianTestUtils.compareJson(conceptsTreeFilename,
                testsPath
                + CLASS_NAME_PREFIX
                + "testJsonTreeTransformProvider1/"
                + "test-data1-concepts_tree.json");

        taskInfo = ToolkitFileUtils.getTaskInfo(2);
        Assert.assertNotNull(taskInfo, "Test data not loaded, task 2");
        runner = new TaskRunner(taskInfo);
        runner.runTask();
        results = runner.getResults();

        Assert.assertNotNull(results);
        Assert.assertEquals(results.get("status"), "success",
                "JsonTreeTransformProvider failed on task 2");
        conceptsTreeFilename = results.get("concepts_tree");
        // Note the use of the same correct output as the previous test.
        ArquillianTestUtils.compareJson(conceptsTreeFilename,
                testsPath
                + CLASS_NAME_PREFIX
                + "testJsonTreeTransformProvider1/"
                + "test-data1-concepts_tree.json");

        // Polyhierarchy detection
        taskInfo = ToolkitFileUtils.getTaskInfo(3);
        Assert.assertNotNull(taskInfo, "Test data not loaded, task 3");
        runner = new TaskRunner(taskInfo);
        runner.runTask();
        results = runner.getResults();

        Assert.assertNotNull(results);
        Assert.assertEquals(results.get("status"), "success",
                "JsonTreeTransformProvider failed on task 3");
        Assert.assertFalse(results.containsKey("concepts_tree"),
                "JsonTreeTransformProvider task 3 returned a concepts_tree "
                + "value");
        Assert.assertEquals(results.get("concepts_tree_not_provided"),
                "No concepts tree provided, because there is a forward "
                + "or cross edge.",
                "JsonTreeTransformProvider task 3 returned wrong value for "
                + "concepts_tree_not_provided");

        // Cycle detection
        taskInfo = ToolkitFileUtils.getTaskInfo(4);
        Assert.assertNotNull(taskInfo, "Test data not loaded, task 4");
        runner = new TaskRunner(taskInfo);
        runner.runTask();
        results = runner.getResults();

        Assert.assertNotNull(results);
        Assert.assertEquals(results.get("status"), "success",
                "JsonTreeTransformProvider failed on task 4");
        Assert.assertFalse(results.containsKey("concepts_tree"),
                "JsonTreeTransformProvider task 4 returned a concepts_tree "
                + "value");
        Assert.assertEquals(results.get("concepts_tree_not_provided"),
                "No concepts tree provided, because there is a cycle.",
                "JsonTreeTransformProvider task 4 returned wrong value for "
                + "concepts_tree_not_provided");

        // Multilingual vocabularies, based on CC-1866.
        taskInfo = ToolkitFileUtils.getTaskInfo(5);
        Assert.assertNotNull(taskInfo, "Test data not loaded, task 5");
        runner = new TaskRunner(taskInfo);
        runner.runTask();
        results = runner.getResults();

        Assert.assertNotNull(results);
        Assert.assertEquals(results.get("status"), "success",
                "JsonTreeTransformProvider failed on task 5");
        conceptsTreeFilename = results.get("concepts_tree");
        ArquillianTestUtils.compareJson(conceptsTreeFilename,
                testsPath
                + CLASS_NAME_PREFIX
                + "testJsonTreeTransformProvider1/"
                + "test-data5-concepts_tree.json");
    }



    // Client-side tests go here. Server-side tests are above this line.



}
