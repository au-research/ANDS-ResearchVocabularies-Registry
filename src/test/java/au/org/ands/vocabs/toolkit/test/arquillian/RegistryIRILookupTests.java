/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.REGISTRY;

import java.net.URI;
import java.net.URL;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.api.user.ResolveIRI;
import au.org.ands.vocabs.toolkit.test.utils.NetClientUtils;

/** Tests of the global IRI resolution. */
public class RegistryIRILookupTests extends ArquillianBaseTest {

    /** Path to the IRI lookup service. */
    private static final String RESOLVE_LOOKUP_IRI =
            "api/services/resolve/lookupIRI";

    /** Name of this class, used in paths to test data files. */
    private static final String CLASS_NAME_PREFIX = "RegistryIRILookupTests.";

    /** The base URL of the deployed webapp under test.
     * Injected by Arquillian.
     * For future ref: if instead of being specified as a private
     * field, this was to be
     * injected as a parameter into a method annotated as {@code @Test},
     * TestNG has to be made happy by saying
     * {@code @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)}.
     */
    @ArquillianResource private URL baseURL;

    // Tests of REST web services defined in
    //   au.org.ands.vocabs.toolkit.rest.ResolveIRI.

    /** Client-side test 1 of the global IRI resolver function,
     * {@link au.org.ands.vocabs.registry.api.user.ResolveIRI#lookupIRI}.
     * Lookup data is based on the correct result
     * of {@link
     * au.org.ands.vocabs.registry.test.TransformProviderTests#testResourceMapTransformProvider1()}.
     */
    @Test
    @RunAsClient
    public final void testResolveIRILookupIRI1() {
        ArquillianTestUtils.clientClearDatabase(REGISTRY, baseURL);
        ArquillianTestUtils.clientLoadDbUnitTestFile(REGISTRY, baseURL,
                CLASS_NAME_PREFIX + "testResolveIRILookupIRI1");

        String service = RESOLVE_LOOKUP_IRI;

        // Test: resource IRI not specified.
        Response response = NetClientUtils.doGet(baseURL,
                service);
        Assert.assertEquals(response.getStatusInfo().getFamily(),
                Family.CLIENT_ERROR,
                "lookupIRI response status");
        String body = response.readEntity(String.class);
        response.close();
        Assert.assertTrue(body.startsWith(ResolveIRI.NO_IRI_SPECIFIED),
                "Error message when no IRI specified");

        // Test: unsupported mode specified.
        response = NetClientUtils.doGetWithAdditionalComponents(
                baseURL, service,
                webTarget -> webTarget.queryParam("iri", "test").
                    queryParam("mode", "bogusMode"));
        Assert.assertEquals(response.getStatusInfo().getFamily(),
                Family.CLIENT_ERROR,
                "lookupIRI response status");
        body = response.readEntity(String.class);
        response.close();
        Assert.assertTrue(body.startsWith(ResolveIRI.UNSUPPORTED_MODE),
                "Error message when unsupported mode specified");

        // Test: bogus IRI specified.
        response = NetClientUtils.doGetWithAdditionalComponents(
                baseURL, service,
                webTarget -> webTarget.queryParam("iri", "test"));
        Assert.assertEquals(response.getStatusInfo().getFamily(),
                Family.CLIENT_ERROR,
                "lookupIRI response status");
        body = response.readEntity(String.class);
        response.close();
        Assert.assertTrue(body.startsWith(ResolveIRI.NO_DEFINITION),
                "Error message when bogus IRI specified");

        String redirectPrefix =
                "http://testing-host.org.au/repository/api/lda/"
                + "ands/testresourcemaptransformprovider1/v1/resource";

        // Tests: valid IRI specified.
        String[] resourcesThatShouldResolve = {
                "http://vocab.owner.org/def/vocab1/1",
                "http://vocab.owner.org/def/vocab1/2",
                "http://vocab.owner.org/def/vocab1/3",
                "http://vocab.owner.org/def/vocab1/5",
                "http://vocab.owner.org/def/vocab1/6",
                "http://vocab.owner.org/def/vocab1/7",
                "http://vocab.owner.org/def/vocab1/9"
        };

        testValidIRISpecified(service, redirectPrefix,
                resourcesThatShouldResolve);

        String[] resourcesThatShouldNotResolve = {
                "http://some.other.org/def/vocab1/1",
                "http://some.other.org/def/vocab1/2",
                "http://some.other.org/def/vocab1/3",
                "http://some.other.org/def/vocab1/5",
                "http://some.other.org/def/vocab1/6",
                "http://some.other.org/def/vocab1/7",
                "http://some.other.org/def/vocab1/9"
        };

        testInvalidIRISpecified(service, resourcesThatShouldNotResolve,
                ResolveIRI.NO_DEFINITION,
                "Error message when non-owned IRI specified");
    }

    /** Client-side test 2 of the global IRI resolver function,
     * {@link au.org.ands.vocabs.registry.api.user.ResolveIRI#lookupIRI}.
     * Lookup data is based on the correct result
     * of {@link
     * au.org.ands.vocabs.registry.test.TransformProviderTests#testResourceMapTransformProvider2()}.
     */
    @Test
    @RunAsClient
    public final void testResolveIRILookupIRI2() {
        ArquillianTestUtils.clientClearDatabase(REGISTRY, baseURL);
        ArquillianTestUtils.clientLoadDbUnitTestFile(REGISTRY, baseURL,
                CLASS_NAME_PREFIX + "testResolveIRILookupIRI2");

        String service = RESOLVE_LOOKUP_IRI;

        String redirectPrefix =
                "http://testing-host.org.au/repository/api/lda/"
                + "ands/testresourcemaptransformprovider2/v1/resource";

        // Tests: valid IRI specified.
        String[] resourcesThatShouldResolve = {
                "http://vocab.owner.org/def/vocab1/1",
                "http://vocab.owner.org/def/vocab1/2",
                "http://vocab.owner.org/def/vocab1/3",
                "http://vocab.owner.org/def/vocab1/5",
                "http://vocab.owner.org/def/vocab1/6",
                "http://vocab.owner.org/def/vocab1/7",
                "http://vocab.owner.org/def/vocab1/9",
                "https://second.owned.com/def/vocab2/1",
                "https://second.owned.com/def/vocab2/2",
                "https://second.owned.com/def/vocab2/3",
                "https://second.owned.com/def/vocab2/5",
                "https://second.owned.com/def/vocab2/6",
                "https://second.owned.com/def/vocab2/7",
                "https://second.owned.com/def/vocab2/9"
        };

        testValidIRISpecified(service, redirectPrefix,
                resourcesThatShouldResolve);

        // Tests: invalid IRI specified: defined, but not owned
        String[] resourcesThatShouldNotResolve = {
                "http://some.other.org/def/vocab1/1",
                "http://some.other.org/def/vocab1/2",
                "http://some.other.org/def/vocab1/3",
                "http://some.other.org/def/vocab1/5",
                "http://some.other.org/def/vocab1/6",
                "http://some.other.org/def/vocab1/7",
                "http://some.other.org/def/vocab1/9"
        };

        // Tests: non-owned IRI specified.
        testInvalidIRISpecified(service, resourcesThatShouldNotResolve,
                ResolveIRI.NO_DEFINITION,
                "Error message when non-owned IRI specified");
    }

    /** Client-side test 3 of the global IRI resolver function,
     * {@link au.org.ands.vocabs.registry.api.user.ResolveIRI#lookupIRI}.
     * Lookup data is based on the correct result
     * of {@link
     * au.org.ands.vocabs.registry.test.TransformProviderTests#testResourceMapTransformProvider3()}.
     */
    @Test
    @RunAsClient
    public final void testResolveIRILookupIRI3() {
        ArquillianTestUtils.clientClearDatabase(REGISTRY, baseURL);
        ArquillianTestUtils.clientLoadDbUnitTestFile(REGISTRY, baseURL,
                CLASS_NAME_PREFIX + "testResolveIRILookupIRI3");

        String service = RESOLVE_LOOKUP_IRI;

        String redirectPrefix =
                "http://testing-host.org.au/repository/api/lda/"
                + "ands/testresourcemaptransformprovider3-1/v1/resource";

        // Tests: valid IRI specified.
        String[] resourcesThatShouldResolve = {
                "http://vocab.owner.org/def/vocab1/1",
                "http://vocab.owner.org/def/vocab1/2",
                "http://vocab.owner.org/def/vocab1/3",
                "http://vocab.owner.org/def/vocab1/5",
                "http://vocab.owner.org/def/vocab1/6",
                "http://vocab.owner.org/def/vocab1/7",
                "http://vocab.owner.org/def/vocab1/9",
                // vocab1/10 is multiply defined, but it should still
                // resolve, because the multiple definitions are
                // within the _same_ access point.
                "http://vocab.owner.org/def/vocab1/10",
                "https://second.owned.com/def/vocab2/1",
                "https://second.owned.com/def/vocab2/2",
                "https://second.owned.com/def/vocab2/3",
                "https://second.owned.com/def/vocab2/5",
                "https://second.owned.com/def/vocab2/6",
                "https://second.owned.com/def/vocab2/7",
                "https://second.owned.com/def/vocab2/9"
        };

        testValidIRISpecified(service, redirectPrefix,
                resourcesThatShouldResolve);

        // Tests: invalid IRI specified: no owned definition.
        String[] resourcesThatShouldNotResolveNoOwnedDefinition = {
                "http://some.other.org/def/vocab1/1",
                "http://some.other.org/def/vocab1/2",
                "http://some.other.org/def/vocab1/3",
                "http://some.other.org/def/vocab1/5",
                "http://some.other.org/def/vocab1/6",
                "http://some.other.org/def/vocab1/7",
                "http://some.other.org/def/vocab1/9"
        };

        testInvalidIRISpecified(service,
                resourcesThatShouldNotResolveNoOwnedDefinition,
                ResolveIRI.NO_DEFINITION,
                "Error message when non-owned IRI specified");

        // Tests: invalid IRI specified: definitions in multiple access points.
        String[] resourcesThatShouldNotResolveMultipleDefinitions = {
                "http://third.another.edu/def/vocab3/1",
                "http://third.another.edu/def/vocab3/2",
                "http://third.another.edu/def/vocab3/3",
                "http://third.another.edu/def/vocab3/5",
                "http://third.another.edu/def/vocab3/6",
                "http://third.another.edu/def/vocab3/7",
                "http://third.another.edu/def/vocab3/9"
        };

        testInvalidIRISpecified(service,
                resourcesThatShouldNotResolveMultipleDefinitions,
                ResolveIRI.MULTIPLE_DEFINITIONS,
                "Error message when multiply-defined IRI specified");

        // Now make one of the versions superseded, and observe how this
        // affects resolution.
        ArquillianTestUtils.clientLoadDbUnitTestFileAsUpdate(REGISTRY, baseURL,
                CLASS_NAME_PREFIX + "testResolveIRILookupIRI3",
                "input-dbunit2.xml");

        // Tests: valid IRI specified.
        testValidIRISpecified(service, redirectPrefix,
                resourcesThatShouldResolve);

        testInvalidIRISpecified(service,
                resourcesThatShouldNotResolveNoOwnedDefinition,
                ResolveIRI.NO_DEFINITION,
                "Error message when non-owned IRI specified");

        // No longer multiple definitions.
        testValidIRISpecified(service, redirectPrefix,
                resourcesThatShouldNotResolveMultipleDefinitions);

    }

    /** Test that an array of IRIs resolve to the respective desired locations.
     * Test with and without a suffix parameter provided.
     * @param service The URL component to the lookup service, to be
     *      appended to baseURL.
     * @param redirectPrefix The beginning of the desired redirect location.
     * @param resourcesThatShouldResolve An array of IRIs that are to
     *      be looked up, to confirm that they resolve to the correct
     *      location.
     */
    private void testValidIRISpecified(final String service,
            final String redirectPrefix,
            final String[] resourcesThatShouldResolve) {
        // Include an ampersand in the test suffix, as it is important
        // to confirm that this comes back without any form of escaping applied.
        String dummyTestSuffix = "&dummyTestSuffix";
        Response response;
        for (String resource : resourcesThatShouldResolve) {
            response = NetClientUtils.doGetWithAdditionalComponentsNoRedirects(
                    baseURL, service,
                    webTarget -> webTarget.queryParam("iri", resource));
            Assert.assertEquals(response.getStatusInfo().getFamily(),
                    Family.REDIRECTION,
                    "lookupIRI response status for iri: " + resource);
            URI redirectLocation = response.getLocation();
            response.close();
            Assert.assertEquals(redirectLocation.toString(),
                    redirectPrefix + "?uri=" + resource,
                    "Redirect URL for " + resource);
            // Now again, with a suffix provided.
            response = NetClientUtils.doGetWithAdditionalComponentsNoRedirects(
                    baseURL, service,
                    webTarget -> webTarget.queryParam("iri", resource)
                        .queryParam("suffix", dummyTestSuffix));
            Assert.assertEquals(response.getStatusInfo().getFamily(),
                    Family.REDIRECTION,
                    "lookupIRI response status for iri: " + resource);
            redirectLocation = response.getLocation();
            response.close();
            Assert.assertEquals(redirectLocation.toString(),
                    redirectPrefix + "?uri=" + resource + dummyTestSuffix,
                    "Redirect URL for " + resource
                    + " with suffix " + dummyTestSuffix);
        }
    }

    /** Test that an array of IRIs do not return a resolved location.
     * @param service The URL component to the lookup service, to be
     *      appended to baseURL.
     * @param resourcesThatShouldNotResolve An array of IRIs that are to
     *      be looked up, to confirm that they do not resolve.
     * @param expectedErrorMessagePrefix The beginning of the expected
     *      error message that comes back from the resolution service.
     * @param errorMessageOnFailure The assertion error to be printed,
     *      if the assertion fails.
     */
    private void testInvalidIRISpecified(final String service,
            final String[] resourcesThatShouldNotResolve,
            final String expectedErrorMessagePrefix,
            final String errorMessageOnFailure) {
        Response response;
        String body;
        for (String resource : resourcesThatShouldNotResolve) {
            response = NetClientUtils.doGetWithAdditionalComponentsNoRedirects(
                    baseURL, service,
                    webTarget -> webTarget.queryParam("iri", resource));
            Assert.assertEquals(response.getStatusInfo().getFamily(),
                    Family.CLIENT_ERROR, "lookupIRI response status, iri: "
                    + resource);
            body = response.readEntity(String.class);
            response.close();
            Assert.assertTrue(body.startsWith(expectedErrorMessagePrefix),
                    errorMessageOnFailure);
        }
    }

}
