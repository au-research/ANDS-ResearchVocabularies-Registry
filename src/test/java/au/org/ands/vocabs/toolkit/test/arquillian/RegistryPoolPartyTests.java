/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.REGISTRY;
import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.ROLES;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.dbunit.DatabaseUnitException;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.api.context.ApiPaths;
import au.org.ands.vocabs.registry.workflow.provider.backup.PoolPartyBackupProvider;
import au.org.ands.vocabs.registry.workflow.tasks.TaskRunner;
import au.org.ands.vocabs.toolkit.test.utils.NetClientUtils;

/** Tests of the Registry API methods that get data from PoolParty,
 * <i>apart from</i> tests of harvesting during the publication workflow.
 */
public class RegistryPoolPartyTests extends ArquillianBaseTest {

    // Leave logger here, though it is unused. We might want to use
    // it later.
    /** Logger. */
    @SuppressWarnings("unused")
    private static Logger logger;

    static {
        logger = LoggerFactory.getLogger(
                MethodHandles.lookup().lookupClass());
    }

    /** Name of this class, used in paths to test data files. */
    private static final String CLASS_NAME_PREFIX =
            "RegistryPoolPartyTests.";

    /** The base URL of the deployed webapp under test.
     * Injected by Arquillian.
     * For future ref: if instead of being specified as a private
     * field, this was to be
     * injected as a parameter into a method annotated as {@code @Test},
     * TestNG has to be made happy by saying
     * {@code @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)}.
     */
    @ArquillianResource private URL baseURL;

//    /** A convenient value to use for endDate properties. */
//    @SuppressWarnings("checkstyle:MagicNumber")
//    private static LocalDateTime nowTime1 =
//            LocalDateTime.of(2017, 10, 1, 10, 10);

    /** Tests of getting the list of PoolParty projects.
     * First, no authentication is sent, and the API method must reject
     * the request. Then, authentication is included, and the API method
     * must return a list of projects.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    @RunAsClient
    public final void testPoolPartyGetMetadata1() throws
    DatabaseUnitException, IOException, SQLException {
        String testName = CLASS_NAME_PREFIX
                + "testPoolPartyGetMetadata1";
        // Getting data from PoolParty requires authorization, so load roles.
        ArquillianTestUtils.clientClearDatabase(ROLES, baseURL);
        ArquillianTestUtils.clientLoadDbUnitTestFile(ROLES, baseURL,
                testName);
        ArquillianTestUtils.clientClearDatabase(REGISTRY, baseURL);
        ArquillianTestUtils.clientLoadDbUnitTestFile(REGISTRY, baseURL,
                testName);
        Response response = null;
        String responseString;

        // First, without any authorization.
        try {
            response = NetClientUtils.doGet(baseURL,
                    ApiPaths.API_SERVICES + "/" + ApiPaths.POOLPARTY
                    + "/1",
                    MediaType.APPLICATION_JSON_TYPE);
            responseString = response.readEntity(String.class);
            Assert.assertEquals(response.getStatus(),
                    Response.Status.UNAUTHORIZED.getStatusCode(),
                    "Response code getting PoolParty data when no auth");
        } finally {
            if (response != null) {
                response.close();
            }
        }
        response = null;

        // Now, with authorization.
        try {
            response = NetClientUtils.doGetBasicAuthWithAdditionalComponents(
                    baseURL,
                    ApiPaths.API_SERVICES + "/" + ApiPaths.POOLPARTY
                    + "/1",
                    MediaType.APPLICATION_JSON_TYPE,
                    "test1", "test",
                    null);
            responseString = response.readEntity(String.class);
            logger.info("response: " + responseString);
            Assert.assertEquals(response.getStatusInfo().getFamily(),
                    Family.SUCCESSFUL,
                    "getPoolPartyProjects response status");
            // This is basically a "round-trip" test. The Registry API
            // method is a "pass-through": it returns exactly what
            // PoolParty returned. So there should be an exact match here.
            ArquillianTestUtils.compareJsonStringWithFile(responseString,
                    ArquillianTestUtils.
                    clientResolveWireMockStubbedContentFilename(
                            "body-PoolParty-api-projects.json"));
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    /** Test of doing a backup of a PoolParty project, where reading from the
     * PoolParty server times out.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testPoolPartyBackupTimeout1() throws
    DatabaseUnitException, IOException, SQLException {
        String testName = CLASS_NAME_PREFIX
                + "testPoolPartyBackupTimeout1";
        ArquillianTestUtils.clearDatabase(ROLES);
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, testName);

        PoolPartyBackupProvider ppbp = new PoolPartyBackupProvider();
        // Provide a usable temporary path. (But if the test works correctly,
        // the directory won't be used.)
        Path outputPath = ArquillianTestUtils.getTempPathForTest(testName);
        HashMap<String, String> backupResponse = ppbp.getBackupFiles(
                "readTimeout", outputPath.toString());

        // The message "Read timed out" is a string literal that is
        // hard-coded in the JDK 8 source files
        //   jdk/src/solaris/native/java/net/SocketInputStream.c
        //   jdk/src/windows/native/java/net/SocketInputStream.c
        // in function Java_java_net_SocketInputStream_socketRead0().
        // (In later releases of the JDK, the files may have been moved
        // to different directories.)
        Assert.assertEquals(backupResponse.get(TaskRunner.ERROR),
                PoolPartyBackupProvider.ERROR_READING_FROM_POOLPARTY
                + "Read timed out");
    }

}
