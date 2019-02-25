/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.TOOLKIT;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.dbunit.DatabaseUnitException;
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
import au.org.ands.vocabs.toolkit.test.factory.LocalDateTimeFactory;
import au.org.ands.vocabs.toolkit.test.utils.NetClientUtils;

/** All Arquillian tests of the Toolkit.
 * As this class grows, it might be split up further.
 */
@Test
public class ToolkitTests extends ArquillianBaseTest {

    // Leave logger here, though it is unused. We might want to use
    // it later.
    /** Logger. */
    @SuppressWarnings("unused")
    private static Logger logger;

    static {
        logger = LoggerFactory.getLogger(
                MethodHandles.lookup().lookupClass());
    }

    // Leave here, though it is unused. We might want to use
    // it later.
    /** Name of this class, used in paths to test data files. */
    @SuppressWarnings("unused")
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
        Response response = null;
        String body;
        try {
            response = NetClientUtils.doGet(baseURL,
                    "getInfo/systemHealthCheck",
                    MediaType.APPLICATION_JSON_TYPE);

            Assert.assertEquals(response.getStatusInfo().getFamily(),
                    Family.SUCCESSFUL,
                    "systemHealthCheck response status");
            body = response.readEntity(String.class);
        } finally {
            if (response != null) {
                response.close();
            }
        }

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
