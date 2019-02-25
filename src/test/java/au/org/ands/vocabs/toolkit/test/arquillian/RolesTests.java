/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.REGISTRY;
import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.ROLES;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.sql.SQLException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.http.HttpStatus;
import org.dbunit.DatabaseUnitException;
import org.hibernate.HibernateException;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.api.auth.AuthConstants;
import au.org.ands.vocabs.registry.api.auth.AuthUtils;
import au.org.ands.vocabs.registry.api.auth.AuthorizationFetcher;
import au.org.ands.vocabs.roles.UserInfo;
import au.org.ands.vocabs.roles.db.entity.AuthenticationServiceId;
import au.org.ands.vocabs.roles.db.utils.RolesUtils;
import au.org.ands.vocabs.toolkit.test.utils.NetClientUtils;

/** Tests of the Roles database.
 */
@Test
public class RolesTests extends ArquillianBaseTest {

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
    private static final String CLASS_NAME_PREFIX = "RolesTests.";

    // Server-side tests go here. Client-side tests later on.

    /** Test of roles.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.*/
    @Test
    public final void testRoles1()
            throws DatabaseUnitException, SQLException, IOException {

        ArquillianTestUtils.clearDatabase(ROLES);
        ArquillianTestUtils.loadDbUnitTestFile(ROLES, CLASS_NAME_PREFIX
                + "testRoles1");

        AuthorizationFetcher authorizationFetcher = new AuthorizationFetcher();

        Assert.assertTrue(RolesUtils.isValidRole("test1",
                AuthenticationServiceId.AUTHENTICATION_BUILT_IN),
                "No role test1");
        Assert.assertFalse(RolesUtils.isValidBuiltinRole("test1", "test1"),
                "Wrong password accepted for role test1");
        Assert.assertTrue(RolesUtils.isValidBuiltinRole("test1", "test"),
                "Wrong password for role test1");

        CommonProfile profile = new CommonProfile();
        profile.setId("test1");
        authorizationFetcher.generate(null, profile);

        Assert.assertEquals(profile.getDisplayName(), "Test 1",
                "Wrong display name for test1");

        UserInfo userInfo = (UserInfo) profile.getAttribute(
                AuthConstants.USER_INFO);
        Assert.assertEquals(userInfo.getIsSuperUser(), Boolean.FALSE,
                "test1 is a superuser");

        Assert.assertTrue(profile.getRoles().contains("ANDS"),
                "test1 does not have the ANDS organizational role");
        Assert.assertFalse(profile.getRoles().contains("Org2"),
                "test1 has the Org2 organizational role");
        Assert.assertTrue(AuthUtils.ownerIsAuthorizedByOrganisation(profile,
                "ANDS"), "test1 is not authorized to publish with owner ANDS");
        Assert.assertFalse(AuthUtils.ownerIsAuthorizedByOrganisation(profile,
                "Org2"), "test1 is authorized to publish with owner Org2");

        Assert.assertTrue(RolesUtils.isValidRole("testsuper1",
                AuthenticationServiceId.AUTHENTICATION_BUILT_IN),
                "No role testsuper1");
        Assert.assertFalse(RolesUtils.isValidBuiltinRole("testsuper1", "test1"),
                "Wrong password accepted for role testsuper1");
        Assert.assertTrue(RolesUtils.isValidBuiltinRole("testsuper1", "test"),
                "Wrong password for role testsuper1");

        profile = new CommonProfile();
        profile.setId("testsuper1");
        authorizationFetcher.generate(null, profile);

        Assert.assertEquals(profile.getDisplayName(), "Test Super 1",
                "Wrong display name for testsuper1");

        userInfo = (UserInfo) profile.getAttribute(AuthConstants.USER_INFO);
        Assert.assertEquals(userInfo.getIsSuperUser(), Boolean.TRUE,
                "Test Super 1 should be a superuser");

        Assert.assertTrue(profile.getRoles().contains("ANDS"),
                "testsuper1 does not have the ANDS organizational role");
        Assert.assertTrue(profile.getRoles().contains("Org2"),
                "testsuper1 does not have the Org2 organizational role");
        Assert.assertTrue(AuthUtils.ownerIsAuthorizedByOrganisation(profile,
                "ANDS"),
                "testsuper1 is not authorized to publish with owner ANDS");
        Assert.assertTrue(AuthUtils.ownerIsAuthorizedByOrganisation(profile,
                "Org2"),
                "testsuper1 is not authorized to publish with owner Org2");

    }


    // Client-side tests go here. Server-side tests are above this line.

    /** The base URL of the deployed webapp under test.
     * Injected by Arquillian.
     * For future ref: if instead of being specified as a private
     * field, this was to be
     * injected as a parameter into a method annotated as {@code @Test},
     * TestNG has to be made happy by saying
     * {@code @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)}.
     */
    @ArquillianResource private URL baseURL;

    /** Client-side test of user data. */
    @Test
    @RunAsClient
    public final void testRolesClient1() {
        ArquillianTestUtils.clientClearDatabase(ROLES, baseURL);
        ArquillianTestUtils.clientLoadDbUnitTestFile(ROLES, baseURL,
                CLASS_NAME_PREFIX + "testRoles1");
        String urlUnderTest = "api/user/userData";

        Response response = null;
        try {
            response = NetClientUtils.doGet(baseURL,
                    urlUnderTest, MediaType.APPLICATION_JSON_TYPE);
            Assert.assertEquals(response.getStatusInfo().getStatusCode(),
                    HttpStatus.SC_UNAUTHORIZED,
                    "Didn't get an error when not authenticated");
        } finally {
            if (response != null) {
                response.close();
                response = null;
            }
        }

        UserInfo userInfo;
        try {
            response = NetClientUtils.doGetBasicAuthWithAdditionalComponents(
                    baseURL,
                    urlUnderTest, MediaType.APPLICATION_XML_TYPE,
                    "test1", "test", webTarget -> webTarget);

            Assert.assertEquals(response.getStatusInfo().getFamily(),
                    Family.SUCCESSFUL,
                    "User data response status");
            userInfo = response.readEntity(UserInfo.class);
        } finally {
            if (response != null) {
                response.close();
            }
        }

        Assert.assertEquals(userInfo.getId(), "test1",
                "User data Id");
        Assert.assertEquals(userInfo.getFullName(), "Test 1",
                "User data full name");
    }

    /** Client-side test of getting vocabularies owned by the user. */
    @Test
    @RunAsClient
    public final void testGetOwnedVocabularies() {
        ArquillianTestUtils.clientClearDatabase(ROLES, baseURL);
        ArquillianTestUtils.clientLoadDbUnitTestFile(ROLES, baseURL,
                CLASS_NAME_PREFIX + "testGetOwnedVocabularies");
        ArquillianTestUtils.clientClearDatabase(REGISTRY, baseURL);
        ArquillianTestUtils.clientLoadDbUnitTestFile(REGISTRY, baseURL,
                CLASS_NAME_PREFIX + "testGetOwnedVocabularies");
        String urlUnderTest = "api/resource/ownedVocabularies";

        Response response = null;
        String result;
        try {
            response = NetClientUtils.
                    doGetBasicAuthWithAdditionalComponents(baseURL,
                            urlUnderTest, MediaType.APPLICATION_XML_TYPE,
                            "test1", "test", webTarget -> webTarget);

            Assert.assertEquals(response.getStatusInfo().getFamily(),
                    Family.SUCCESSFUL,
                    "Response status");

            result = response.readEntity(String.class);
        } finally {
            if (response != null) {
                response.close();
            }
        }

        Assert.assertEquals(result,
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<owned-vocabulary-list xmlns=\"http://vocabs.ands.org.au/registry/"
            + "schema/2017/01/vocabulary\">"
        + "<owned-vocabulary id=\"1\" status=\"published\" has-draft=\"true\" "
            + "title=\"Registry Interchange Format - Collections and Services "
            + "(Vocabularies)\"/>"
        + "<owned-vocabulary id=\"2\" status=\"deprecated\" "
            + "has-draft=\"false\" "
            + "title=\"AGROVOC Multilingual Agricultural Thesaurus\"/>"
        + "<owned-vocabulary id=\"4\" status=\"draft\" has-draft=\"true\" "
            + "title=\"Water Resources Thesaurus\"/>"
        + "</owned-vocabulary-list>",
                "Owned vocabulary list");
    }

}
