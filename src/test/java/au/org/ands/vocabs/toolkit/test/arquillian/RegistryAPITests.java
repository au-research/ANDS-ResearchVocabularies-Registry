/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.REGISTRY;
import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.ROLES;

import java.lang.invoke.MethodHandles;
import java.net.URL;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

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
        Response response = NetClientUtils.doGet(baseURL,
                ApiPaths.API_RESOURCE + "/" + ApiPaths.VOCABULARIES,
                MediaType.APPLICATION_XML_TYPE);

        Assert.assertEquals(response.getStatusInfo().getFamily(),
                Family.SUCCESSFUL,
                "getVocabularies response status");
        VocabularyList vocabularyList =
                response.readEntity(VocabularyList.class);
        response.close();

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
        Response response = NetClientUtils.doGet(baseURL,
                ApiPaths.API_RESOURCE + "/" + ApiPaths.VOCABULARIES,
                MediaType.APPLICATION_XML_TYPE);

        Assert.assertEquals(response.getStatusInfo().getFamily(),
                Family.SUCCESSFUL,
                "getVocabularies response status");
        VocabularyList vocabularyList =
                response.readEntity(VocabularyList.class);
        response.close();

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
        Response response = NetClientUtils.doDeleteWithAdditionalComponents(
                baseURL,
                ApiPaths.API_RESOURCE + "/" + ApiPaths.VOCABULARIES + "/1",
                MediaType.APPLICATION_XML_TYPE, "test1", "test",
                webTarget -> webTarget.queryParam("deleteCurrent", "true"));

        Assert.assertEquals(response.getStatusInfo().getFamily(),
                Family.SUCCESSFUL,
                "deleteVocabularies response status");
        response.close();

        // Now confirm that there are no current vocabularies.
        response = NetClientUtils.doGet(baseURL,
                ApiPaths.API_RESOURCE + "/" + ApiPaths.VOCABULARIES,
                MediaType.APPLICATION_XML_TYPE);

        Assert.assertEquals(response.getStatusInfo().getFamily(),
                Family.SUCCESSFUL,
                "getVocabularies response status");
        VocabularyList vocabularyList =
                response.readEntity(VocabularyList.class);
        response.close();

        Assert.assertEquals(vocabularyList.getVocabulary().size(), 0,
            "vocabularyList size");
    }

}
