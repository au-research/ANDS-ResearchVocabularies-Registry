/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.TOOLKIT;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.dbunit.Assertion;
import org.dbunit.DatabaseUnitException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.SortedTable;
import org.dbunit.dataset.filter.DefaultColumnFilter;
import org.hibernate.HibernateException;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.meanbean.test.BeanTester;
import org.meanbean.util.RandomValueGenerator;
import org.meanbean.util.SimpleRandomValueGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.workflow.provider.backup.BackupProviderUtils;
import au.org.ands.vocabs.registry.workflow.provider.harvest.HarvestProviderUtils;
import au.org.ands.vocabs.registry.workflow.provider.importer.ImporterProviderUtils;
import au.org.ands.vocabs.registry.workflow.provider.publish.PublishProviderUtils;
import au.org.ands.vocabs.registry.workflow.provider.transform.TransformProviderUtils;
import au.org.ands.vocabs.toolkit.db.ResourceOwnerHostUtils;
import au.org.ands.vocabs.toolkit.db.TaskUtils;
import au.org.ands.vocabs.toolkit.db.model.AccessPoint;
import au.org.ands.vocabs.toolkit.db.model.ResourceMapEntry;
import au.org.ands.vocabs.toolkit.db.model.ResourceOwnerHost;
import au.org.ands.vocabs.toolkit.db.model.Task;
import au.org.ands.vocabs.toolkit.db.model.Version;
import au.org.ands.vocabs.toolkit.db.model.Vocabulary;
import au.org.ands.vocabs.toolkit.tasks.TaskInfo;
import au.org.ands.vocabs.toolkit.tasks.TaskRunner;
import au.org.ands.vocabs.toolkit.test.factory.LocalDateTimeFactory;
import au.org.ands.vocabs.toolkit.test.utils.DbUnitConstants;
import au.org.ands.vocabs.toolkit.test.utils.NetClientUtils;
import au.org.ands.vocabs.toolkit.utils.ToolkitFileUtils;

/** All Arquillian tests of the Toolkit.
 * As this class grows, it might be split up further.
 */
@Test
public class ToolkitTests extends ArquillianBaseTest {

    /** Logger. */
    private static Logger logger;

    static {
        logger = LoggerFactory.getLogger(
                MethodHandles.lookup().lookupClass());
    }

    /** Name of this class, used in paths to test data files. */
    private static final String CLASS_NAME_PREFIX = "ToolkitTests.";

    // Server-side tests go here. Client-side tests later on.

    // Tests of database model entity bean classes.

    /** Test of the various database model entity bean classes
     * using Mean Bean. */
    @Test
    public final void testDbModelEntityBeans() {
        new BeanTester().testBean(AccessPoint.class);
        new BeanTester().testBean(ResourceMapEntry.class);
        // Special treatment of ResourceOwnerHost, as it has
        // fields of type LocalDateTime.
        BeanTester rohBeanTester = new BeanTester();
        RandomValueGenerator randomValueGenerator =
                new SimpleRandomValueGenerator();
        LocalDateTimeFactory localDateTimeFactory =
                new LocalDateTimeFactory(randomValueGenerator);
        rohBeanTester.getFactoryCollection().addFactory(
                LocalDateTime.class, localDateTimeFactory);
        rohBeanTester.testBean(ResourceOwnerHost.class);
        new BeanTester().testBean(Task.class);
        new BeanTester().testBean(Version.class);
        new BeanTester().testBean(Vocabulary.class);
    }

    /** Test of {@link Version#getReleaseDate()}. */
    @Test
    public final void testVersionGetReleaseDate() {
        Version v = new Version();
        // Null returned if data is null.
        v.setData(null);
        Assert.assertNull(v.getReleaseDate(), "Null data");
        // Null returned if data is an empty string.
        v.setData("");
        Assert.assertNull(v.getReleaseDate(), "Data is empty string");
        // Null returned if data is an object, but there is no release date
        // defined.
        v.setData("{ }");
        Assert.assertNull(v.getReleaseDate(), "Data has no release date");
        // And, at last, test with a release date defined.
        v.setData("{ \"" + Version.RELEASE_DATE_KEY + "\": \"testValue\"}");
        Assert.assertEquals(v.getReleaseDate(), "testValue",
                "Data has release date");
    }


    // Tests of the various au.org.ands.vocabs.provider...ProviderUtils classes.

    /** Server-side test of the various {@code ...ProviderUtils} classes
     * that they do not leak an exception on an invalid provider name. */
    @Test
    public final void testProviderUtilsGetProviderNoException() {
        final String provider = "NoSuchProvider";
        Assert.assertNull(BackupProviderUtils.getProvider(provider));
        Assert.assertNull(HarvestProviderUtils.getProvider(provider));
        Assert.assertNull(ImporterProviderUtils.getProvider(provider));
        Assert.assertNull(PublishProviderUtils.getProvider(provider));
        Assert.assertNull(TransformProviderUtils.getProvider(provider));
    }

    // Tests of class au.org.ands.vocabs.toolkit.db.TasksUtils.

    /** Server-side test of {@code TasksUtils.getAllTasks()}
     * when there are no tasks. */
    @Test
    public final void testGetAllTasks() {
        List<Task> taskList = TaskUtils.getAllTasks();
        Assert.assertNotNull(taskList,
                "getAllTasks() with no tasks");
        Assert.assertEquals(taskList.size(), 0,
                "getAllTasks() with no tasks");
    }

    // Tests of class
    // au.org.ands.vocabs.toolkit.
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
    @Test(enabled = false)
    public final void testResourceMapTransformProvider1() throws
        DatabaseUnitException, HibernateException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(TOOLKIT);
        ArquillianTestUtils.loadDbUnitTestFile(TOOLKIT, CLASS_NAME_PREFIX
                + "testResourceMapTransformProvider1");
        List<Task> taskList = TaskUtils.getAllTasks();
        logger.info("testResourceMapTransformProvider1: task list length = "
                + taskList.size());
        TaskInfo taskInfo = ToolkitFileUtils.getTaskInfo(1);
        Assert.assertNotNull(taskInfo, "Test data not loaded, task 1");
        TaskRunner runner = new TaskRunner(taskInfo);
        runner.runTask();
        HashMap<String, String> results = runner.getResults();
        Assert.assertNotNull(results);
        Assert.assertEquals(results.get("status"), "success",
                "ResourceMapTransformProvider failed on task 1");
        // If a dump is required, uncomment the next line.
        // ArquillianTestUtils.exportFullDbUnitData("trmtp1.xml");

        // Get current contents of resource_map table.
        ITable actualTable = ArquillianTestUtils.
                getDatabaseTableCurrentContents(TOOLKIT,
                        DbUnitConstants.RESOURCEMAPENTRY_TABLE_NAME);
        // And take out the id and access_point_id columns before
        // doing a comparison.
        ITable filteredActualTable = DefaultColumnFilter.excludedColumnsTable(
                actualTable, new String[]{"ID", "ACCESS_POINT_ID"});

        IDataSet expectedDataSet = ArquillianTestUtils.
                getDatabaseTableExpectedContents(TOOLKIT,
                        "test/tests/"
                        + CLASS_NAME_PREFIX
                        + "testResourceMapTransformProvider1/"
                        + "test-data1-results.xml");
        ITable expectedTable = expectedDataSet.getTable(
                DbUnitConstants.RESOURCEMAPENTRY_TABLE_NAME);
        ITable filteredExpectedTable = DefaultColumnFilter.excludedColumnsTable(
                expectedTable, new String[]{"ID", "ACCESS_POINT_ID"});
        Assertion.assertEquals(new SortedTable(filteredExpectedTable),
                new SortedTable(filteredActualTable,
                        filteredExpectedTable.getTableMetaData()));

        // Now do an UNTRANSFORM.
        taskInfo = ToolkitFileUtils.getTaskInfo(2);
        Assert.assertNotNull(taskInfo, "Test data not loaded, task 2");
        runner = new TaskRunner(taskInfo);
        runner.runTask();
        results = runner.getResults();
        Assert.assertNotNull(results);
        Assert.assertEquals(results.get("status"), "success",
                "ResourceMapTransformProvider failed on task 2");
        // Get current contents of resource_map table again.
        actualTable = ArquillianTestUtils.
                getDatabaseTableCurrentContents(TOOLKIT,
                        DbUnitConstants.RESOURCEMAPENTRY_TABLE_NAME);
        Assert.assertEquals(actualTable.getRowCount(), 0,
                "Empty resource_map table after UNTRANSFORM");
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
    @Test(enabled = false)
    public final void testResourceMapTransformProvider2() throws
        DatabaseUnitException, HibernateException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(TOOLKIT);
        ArquillianTestUtils.loadDbUnitTestFile(TOOLKIT, CLASS_NAME_PREFIX
                + "testResourceMapTransformProvider2");
        List<Task> taskList = TaskUtils.getAllTasks();
        logger.info("testResourceMapTransformProvider2: task list length = "
                + taskList.size());
        TaskInfo taskInfo = ToolkitFileUtils.getTaskInfo(1);
        Assert.assertNotNull(taskInfo, "Test data not loaded, task 1");
        TaskRunner runner = new TaskRunner(taskInfo);
        runner.runTask();
        HashMap<String, String> results = runner.getResults();
        Assert.assertNotNull(results);
        Assert.assertEquals(results.get("status"), "success",
                "ResourceMapTransformProvider failed on task 1");
        // If a dump is required, uncomment the next line.
        // ArquillianTestUtils.exportFullDbUnitData("trmtp2.xml");

        // Get current contents of resource_map table.
        ITable actualTable = ArquillianTestUtils.
                getDatabaseTableCurrentContents(TOOLKIT,
                        DbUnitConstants.RESOURCEMAPENTRY_TABLE_NAME);
        // And take out the id and access_point_id columns before
        // doing a comparison.
        ITable filteredActualTable = DefaultColumnFilter.excludedColumnsTable(
                actualTable, new String[]{"ID", "ACCESS_POINT_ID"});

        IDataSet expectedDataSet = ArquillianTestUtils.
                getDatabaseTableExpectedContents(TOOLKIT,
                        "test/tests/"
                        + CLASS_NAME_PREFIX
                        + "testResourceMapTransformProvider2/"
                        + "test-data1-results.xml");
        ITable expectedTable = expectedDataSet.getTable(
                DbUnitConstants.RESOURCEMAPENTRY_TABLE_NAME);
        ITable filteredExpectedTable = DefaultColumnFilter.excludedColumnsTable(
                expectedTable, new String[]{"ID", "ACCESS_POINT_ID"});
        Assertion.assertEquals(new SortedTable(filteredExpectedTable),
                new SortedTable(filteredActualTable,
                        filteredExpectedTable.getTableMetaData()));
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
    @Test(enabled = false)
    public final void testResourceMapTransformProvider3() throws
        DatabaseUnitException, HibernateException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(TOOLKIT);
        ArquillianTestUtils.loadDbUnitTestFile(TOOLKIT, CLASS_NAME_PREFIX
                + "testResourceMapTransformProvider3");
        List<Task> taskList = TaskUtils.getAllTasks();
        logger.info("testResourceMapTransformProvider3: task list length = "
                + taskList.size());
        TaskInfo taskInfo = ToolkitFileUtils.getTaskInfo(1);
        Assert.assertNotNull(taskInfo, "Test data not loaded, task 1");
        TaskRunner runner = new TaskRunner(taskInfo);
        runner.runTask();
        HashMap<String, String> results = runner.getResults();
        Assert.assertNotNull(results);
        Assert.assertEquals(results.get("status"), "success",
                "ResourceMapTransformProvider failed on task 1");

        taskInfo = ToolkitFileUtils.getTaskInfo(2);
        Assert.assertNotNull(taskInfo, "Test data not loaded, task 2");
        runner = new TaskRunner(taskInfo);
        runner.runTask();
        results = runner.getResults();
        Assert.assertNotNull(results);
        Assert.assertEquals(results.get("status"), "success",
                "ResourceMapTransformProvider failed on task 2");
        // If a dump is required, uncomment the next line.
        // ArquillianTestUtils.exportFullDbUnitData("trmtp3.xml");

        // Get current contents of resource_map table.
        ITable actualTable = ArquillianTestUtils.
                getDatabaseTableCurrentContents(TOOLKIT,
                        DbUnitConstants.RESOURCEMAPENTRY_TABLE_NAME);
        // And take out the id column before
        // doing a comparison. Cf. the other tests, in which we also
        // take out the access_point_id column. This time, we have
        // two access points in play, so we rely on the database
        // cleaning having reset the counters, so that generated ids can be
        // relied on to start from 1.
        ITable filteredActualTable = DefaultColumnFilter.excludedColumnsTable(
                actualTable, new String[]{"ID"});

        IDataSet expectedDataSet = ArquillianTestUtils.
                getDatabaseTableExpectedContents(TOOLKIT,
                        "test/tests/"
                        + CLASS_NAME_PREFIX
                        + "testResourceMapTransformProvider3/"
                        + "test-data1-results.xml");
        ITable expectedTable = expectedDataSet.getTable(
                DbUnitConstants.RESOURCEMAPENTRY_TABLE_NAME);
        ITable filteredExpectedTable = DefaultColumnFilter.excludedColumnsTable(
                expectedTable, new String[]{"ID"});
        Assertion.assertEquals(new SortedTable(filteredExpectedTable),
                new SortedTable(filteredActualTable,
                        filteredExpectedTable.getTableMetaData()));
    }

    // Client-side tests go here. Server-side tests are above this line.

    // Tests of REST web services defined in
    //   au.org.ands.vocabs.toolkit.rest.GetInfo.

    /** The base URL of the deployed webapp under test.
     * Injected by Arquillian.
     * For future ref: if instead of being specified as a private
     * field, this was to be
     * injected as a parameter into a method annotated as {@code @Test},
     * TestNG has to be made happy by saying
     * {@code @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)}.
     */
    @ArquillianResource private URL baseURL;

    /** Client-side test of the system health check function.
     */
    @Test
    @RunAsClient
    public final void testSystemHealthCheck() {
        ArquillianTestUtils.clientClearDatabase(TOOLKIT, baseURL);
        Response response = NetClientUtils.doGet(baseURL,
                "getInfo/systemHealthCheck", MediaType.APPLICATION_JSON_TYPE);

        Assert.assertEquals(response.getStatusInfo().getFamily(),
                Family.SUCCESSFUL,
                "systemHealthCheck response status");
        String body = response.readEntity(String.class);
        response.close();

        Assert.assertEquals(body, "[]",
            "systemHealthCheck return value");
        // Assert.fail("Test of failing in testSystemHealthCheck");
    }

    /** Test of date/time conversion at the time of the
     * daylight savings changeover, using {@link ResourceOwnerHost}
     * as an example entity class.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public final void testResourceOwnerHostDateTimeConversion()
            throws HibernateException, DatabaseUnitException,
            IOException, SQLException {
        ArquillianTestUtils.clearDatabase(TOOLKIT);
        ResourceOwnerHost roh = new ResourceOwnerHost();
        // Doesn't matter what these are; we're testing start and end dates.
        roh.setHost("http://www");
        roh.setOwner("me");
        // The date/time 2016-10-02T02:01:00 is fine in UTC,
        // but it does not exist in the Australia/Sydney time zone!
        // If the implementation treats the value as a time in
        // the Australia/Sydney time zone, it will be persisted
        // incorrectly.
        LocalDateTime startDate = LocalDateTime.of(2016, 10, 2, 2, 1, 0);
        // The endDate must be after the present time, so that
        // the subsequent call to getResourceOwnerHostMapEntriesForOwner()
        // returns the instance we inserted!
        LocalDateTime endDate =
                LocalDateTime.now(ZoneOffset.UTC).plusHours(1);
        roh.setStartDate(startDate);
        roh.setEndDate(endDate);
        ResourceOwnerHostUtils.saveResourceOwnerHost(roh);
        List<ResourceOwnerHost> rohList = ResourceOwnerHostUtils.
                getResourceOwnerHostMapEntriesForOwner("me");
        roh = rohList.get(0);
        Assert.assertEquals(roh.getStartDate(), startDate,
                "Start date different");
        Assert.assertEquals(roh.getEndDate(), endDate,
                "End date different");
    }

}
