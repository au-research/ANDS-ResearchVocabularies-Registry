/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.REGISTRY;
import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.ROLES;

import java.lang.invoke.MethodHandles;
import java.net.URL;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.hamcrest.MatcherAssert;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.xmlunit.matchers.CompareMatcher;

import au.org.ands.vocabs.registry.api.context.ApiPaths;
import au.org.ands.vocabs.registry.enums.VocabularyStatus;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;
import au.org.ands.vocabs.registry.schema.vocabulary201701.VocabularyList;
import au.org.ands.vocabs.toolkit.test.utils.NetClientUtils;

/** Client-side tests of the Registry API. */
public class RegistryAPITests extends ArquillianBaseTest {

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
    private static final String CLASS_NAME_PREFIX = "RegistryAPITests.";

    /** The base URL of the deployed webapp under test.
     * Injected by Arquillian.
     * For future ref: if instead of being specified as a private
     * field, this was to be
     * injected as a parameter into a method annotated as {@code @Test},
     * TestNG has to be made happy by saying
     * {@code @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)}.
     */
    @ArquillianResource private URL baseURL;

    /** Client-side test of getVocabularies, when there aren't any
     * vocabularies.
     */
    @Test
    @RunAsClient
    public final void testGetVocabularies1() {
        ArquillianTestUtils.clientClearDatabase(REGISTRY, baseURL);
        Response response = null;
        VocabularyList vocabularyList;
        try {
            response = NetClientUtils.doGet(baseURL,
                    ApiPaths.API_RESOURCE + "/" + ApiPaths.VOCABULARIES,
                    MediaType.APPLICATION_XML_TYPE);

            Assert.assertEquals(response.getStatusInfo().getStatusCode(),
                    Status.OK.getStatusCode(),
                    "getVocabularies response status");
            vocabularyList = response.readEntity(VocabularyList.class);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        Assert.assertEquals(vocabularyList.getVocabulary().size(), 0,
                "vocabularyList return value");
    }

    /** Client-side test of getVocabularies, when there is one
     * vocabulary.
     */
    // '3' is a magic number!
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    @RunAsClient
    public final void testGetVocabularies2() {
        ArquillianTestUtils.clientClearDatabase(REGISTRY, baseURL);
        ArquillianTestUtils.clientLoadDbUnitTestFile(REGISTRY, baseURL,
                CLASS_NAME_PREFIX + "testGetVocabularies2");
        Response response = null;
        VocabularyList vocabularyList;
        try {
            response = NetClientUtils.doGet(baseURL,
                    ApiPaths.API_RESOURCE + "/" + ApiPaths.VOCABULARIES,
                    MediaType.APPLICATION_XML_TYPE);

            Assert.assertEquals(response.getStatusInfo().getStatusCode(),
                    Status.OK.getStatusCode(),
                    "getVocabularies response status");
            vocabularyList = response.readEntity(VocabularyList.class);
        } finally {
            if (response != null) {
                response.close();
            }
        }

        Assert.assertEquals(vocabularyList.getVocabulary().size(), 1,
            "vocabularyList size");
        Vocabulary vocabulary = vocabularyList.getVocabulary().get(0);
        Assert.assertEquals(vocabulary.getId().intValue(), 1, "ID");
        Assert.assertEquals(vocabulary.getAcronym(), "RIF-CS", "acronym");
        Assert.assertEquals(vocabulary.getCreationDate(), "2008-12-31",
                "creation date");
        Assert.assertEquals(vocabulary.getDescription(),
                "The RIF-CS schema is a data interchange format that supports "
                + "the electronic exchange of collection and service "
                + "descriptions. It organises information about collections "
                + "and services into a format used by the Australian National "
                + "Data Service (ANDS) Collections Registry. More information "
                + "on the RIF-CS Schema can be found at "
                + "http://ands.org.au/guides/cpguide/cpgrifcs.html. "
                + "With assistance from the RIF-CS Advisory Board and user "
                + "community, ANDS has developed a suggested vocabulary for "
                + "use with the RIF-CS Schema. The vocabulary is comprised "
                + "of suggested terms for specific elements of the schema. "
                + "Use of the ANDS vocabulary with the RIF-CS Schema is not "
                + "mandatory but is however recommended. Use of consistent "
                + "terminology improves data discoverability and the "
                + "precision of search operations. The ANDS vocabularies are "
                + "expected to be informed and developed further by the "
                + "RIF-CS user community. Please contact services@ands.org.au "
                + "if you have candidate vocabulary terms to be evaluated for "
                + "possible inclusion in the ANDS vocabularies.",
                "description");
        Assert.assertEquals(vocabulary.getLicence(), "CC-BY", "licence");
        Assert.assertEquals(vocabulary.getNote(), null, "note");
        Assert.assertEquals(vocabulary.getOwner(), "ANDS-Curated", "owner");
        Assert.assertEquals(vocabulary.getPrimaryLanguage(), "en",
                "primary language");
        Assert.assertEquals(vocabulary.getRevisionCycle(),
                "Updated as required", "revision cycle");
        Assert.assertEquals(vocabulary.getSlug(), "rifcs", "slug");
        Assert.assertEquals(vocabulary.getTitle(),
                "Registry Interchange Format - Collections and Services "
                + "(Vocabularies)", "title");
        Assert.assertEquals(vocabulary.getOtherLanguage().size(), 0,
                "other language");
        Assert.assertEquals(vocabulary.getPoolpartyProject(), null,
                "PoolParty project");
        Assert.assertEquals(vocabulary.getRelatedEntityRef().size(), 3,
                "related entity ref");
        Assert.assertEquals(vocabulary.getRelatedVocabularyRef().size(), 0,
                "related vocabulary ref");
        Assert.assertEquals(vocabulary.getStatus(), VocabularyStatus.PUBLISHED,
                "status");
        Assert.assertEquals(vocabulary.getSubject().size(), 1, "subject");
        Assert.assertEquals(vocabulary.getTopConcept().size(), 0,
                "top concept ");
        Assert.assertEquals(vocabulary.getVersion().size(), 0, "version");
//        Assert.assertEquals(vocabulary.get, "", "");

    }

    /** Client-side test of deleteVocabulary, deleting a current instance.
     */
    @Test
    @RunAsClient
    public final void testDeleteVocabulary1() {
        // Delete requires authorization, so load roles.
        ArquillianTestUtils.clientClearDatabase(ROLES, baseURL);
        ArquillianTestUtils.clientLoadDbUnitTestFile(ROLES, baseURL,
                CLASS_NAME_PREFIX + "testDeleteVocabulary1");
        ArquillianTestUtils.clientClearDatabase(REGISTRY, baseURL);
        ArquillianTestUtils.clientLoadDbUnitTestFile(REGISTRY, baseURL,
                CLASS_NAME_PREFIX + "testDeleteVocabulary1");
        Response response = null;
        try {
            response = NetClientUtils.doDeleteWithAdditionalComponents(
                    baseURL,
                    ApiPaths.API_RESOURCE + "/" + ApiPaths.VOCABULARIES + "/1",
                    MediaType.APPLICATION_XML_TYPE, "test1", "test",
                    webTarget -> webTarget.queryParam("deleteCurrent", "true"));

            Assert.assertEquals(response.getStatusInfo().getStatusCode(),
                    Status.NO_CONTENT.getStatusCode(),
                    "deleteVocabularies response status");
        } finally {
            if (response != null) {
                response.close();
                response = null;
            }
        }

        // Now confirm that there are no current vocabularies.
        VocabularyList vocabularyList;
        try {
            response = NetClientUtils.doGet(baseURL,
                    ApiPaths.API_RESOURCE + "/" + ApiPaths.VOCABULARIES,
                    MediaType.APPLICATION_XML_TYPE);

            Assert.assertEquals(response.getStatusInfo().getStatusCode(),
                    Status.OK.getStatusCode(),
                    "getVocabularies response status");
            vocabularyList = response.readEntity(VocabularyList.class);
        } finally {
            if (response != null) {
                response.close();
            }
        }

        Assert.assertEquals(vocabularyList.getVocabulary().size(), 0,
            "vocabularyList size");
    }

    /** Client-side test of updateVocabulary, changing the owner in
     * a way that is permitted.
     */
    @Test
    @RunAsClient
    public final void testUpdateVocabularyChangeOwner1() {
        String testName = "testUpdateVocabularyChangeOwner1";
        // Update requires authorization, so load roles.
        ArquillianTestUtils.clientClearDatabase(ROLES, baseURL);
        ArquillianTestUtils.clientLoadDbUnitTestFile(ROLES, baseURL,
                CLASS_NAME_PREFIX + testName);
        ArquillianTestUtils.clientClearDatabase(REGISTRY, baseURL);
        ArquillianTestUtils.clientLoadDbUnitTestFile(REGISTRY, baseURL,
                CLASS_NAME_PREFIX + testName);

        // First, make sure we are starting with the correct content.
        String expectedString;
        expectedString = ArquillianTestUtils.getTestFileAsString(
                "test/tests/"
                        + CLASS_NAME_PREFIX
                        + testName
                        + "/test-vocabulary-1.xml");

        // Get the original vocabulary.
        Response response = null;
        String actualString;
        try {
            response = NetClientUtils.doGetWithAdditionalComponents(
                    baseURL,
                    ApiPaths.API_RESOURCE + "/" + ApiPaths.VOCABULARIES + "/1",
                    MediaType.APPLICATION_XML_TYPE,
                    webTarget -> webTarget.queryParam(
                            "includeRelatedEntitiesAndVocabularies", "true"));

            actualString = response.readEntity(String.class);
        } finally {
            if (response != null) {
                response.close();
                response = null;
            }
        }

        // Use XMLUnit, as our test data has an additional DOCTYPE.
        MatcherAssert.assertThat("Before update",
                actualString,
                CompareMatcher.isIdenticalTo(expectedString).
                ignoreWhitespace());

        // The content to be sent as the body of the PUT request.
        String body = ArquillianTestUtils.getTestFileAsString(
                "test/tests/"
                        + CLASS_NAME_PREFIX
                        + testName
                        + "/test-vocabulary-2.xml");

        try {
            response =
                    NetClientUtils.doPutBasicAuthWithAdditionalComponentsXml(
                            baseURL,
                            ApiPaths.API_RESOURCE + "/"
                                    + ApiPaths.VOCABULARIES + "/1",
                                    MediaType.APPLICATION_XML_TYPE, "test1",
                                    "test",
                                    webTarget -> webTarget,
                                    body);

            expectedString = ArquillianTestUtils.getTestFileAsString(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + testName
                            + "/test-vocabulary-3.xml");

            Assert.assertEquals(response.getStatusInfo().getStatusCode(),
                    Status.OK.getStatusCode(),
                    "updateVocabularies response status");

            actualString = response.readEntity(String.class);
        } finally {
            if (response != null) {
                response.close();
            }
        }

//        logger.debug("Response: " + actualString);

        // Use XMLUnit, as our test data has an additional DOCTYPE.
        MatcherAssert.assertThat("After update",
                actualString,
                CompareMatcher.isIdenticalTo(expectedString).
                ignoreWhitespace());
    }

    /** Client-side test of updateVocabulary, changing the owner in
     * a way that is not permitted.
     */
    @Test
    @RunAsClient
    public final void testUpdateVocabularyChangeOwner2() {
        String testName = "testUpdateVocabularyChangeOwner2";
        // Update requires authorization, so load roles.
        ArquillianTestUtils.clientClearDatabase(ROLES, baseURL);
        ArquillianTestUtils.clientLoadDbUnitTestFile(ROLES, baseURL,
                CLASS_NAME_PREFIX + testName);
        ArquillianTestUtils.clientClearDatabase(REGISTRY, baseURL);
        ArquillianTestUtils.clientLoadDbUnitTestFile(REGISTRY, baseURL,
                CLASS_NAME_PREFIX + testName);

        // First, make sure we are starting with the correct content.
        String expectedString;
        expectedString = ArquillianTestUtils.getTestFileAsString(
                "test/tests/"
                        + CLASS_NAME_PREFIX
                        + testName
                        + "/test-vocabulary-1.xml").trim();

        // Get the original vocabulary.
        Response response = null;
        String actualString;
        try {
            response = NetClientUtils.doGetWithAdditionalComponents(
                    baseURL,
                    ApiPaths.API_RESOURCE + "/" + ApiPaths.VOCABULARIES + "/1",
                    MediaType.APPLICATION_XML_TYPE,
                    webTarget -> webTarget.queryParam(
                            "includeRelatedEntitiesAndVocabularies", "true"));

            actualString = response.readEntity(String.class);
        } finally {
            if (response != null) {
                response.close();
                response = null;
            }
        }

        // Use XMLUnit, as our test data has an additional DOCTYPE.
        MatcherAssert.assertThat("Before update",
                actualString,
                CompareMatcher.isIdenticalTo(expectedString).
                ignoreWhitespace());

        // The content to be sent as the body of the PUT request.
        String body = ArquillianTestUtils.getTestFileAsString(
                "test/tests/"
                        + CLASS_NAME_PREFIX
                        + testName
                        + "/test-vocabulary-2.xml");

        try {
            response =
                    NetClientUtils.doPutBasicAuthWithAdditionalComponentsXml(
                            baseURL,
                            ApiPaths.API_RESOURCE + "/"
                                    + ApiPaths.VOCABULARIES + "/1",
                                    MediaType.APPLICATION_XML_TYPE, "test1",
                                    "test",
                                    webTarget -> webTarget,
                                    body);

            // test-vocabulary-3.xml contains an error response, not
            // an updated vocabulary.
            expectedString = ArquillianTestUtils.getTestFileAsString(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + testName
                            + "/test-vocabulary-3.xml").trim();

            Assert.assertEquals(response.getStatusInfo().getStatusCode(),
                    Status.BAD_REQUEST.getStatusCode(),
                    "updateVocabularies response status");

            actualString = response.readEntity(String.class);
        } finally {
            if (response != null) {
                response.close();
            }
        }

//        logger.debug("Response: " + actualString);

        // Use XMLUnit, as our test data has an additional DOCTYPE.
        MatcherAssert.assertThat("After update",
                actualString,
                CompareMatcher.isIdenticalTo(expectedString).
                ignoreWhitespace());
    }

}
