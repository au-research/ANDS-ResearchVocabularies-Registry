/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

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

import au.org.ands.vocabs.registry.api.context.ErrorHtml;
import au.org.ands.vocabs.registry.api.context.Index;
import au.org.ands.vocabs.registry.api.context.NotFoundExceptionMapper;
import au.org.ands.vocabs.registry.api.user.ErrorResult;
import au.org.ands.vocabs.toolkit.test.utils.NetClientUtils;

/** Client-side tests of "context" responses, such as for the "front page",
 * and for error responses. */
public class RegistryContextTests extends ArquillianBaseTest {

    // Leave logger here, though it is unused. We might want to use
    // it later.
    /** Logger. */
    @SuppressWarnings("unused")
    private static Logger logger;

    static {
        logger = LoggerFactory.getLogger(
                MethodHandles.lookup().lookupClass());
    }

    /** The base URL of the deployed webapp under test.
     * Injected by Arquillian.
     * For future ref: if instead of being specified as a private
     * field, this was to be
     * injected as a parameter into a method annotated as {@code @Test},
     * TestNG has to be made happy by saying
     * {@code @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)}.
     */
    @ArquillianResource private URL baseURL;

    /** Client-side tests of the "front page". */
    @Test
    @RunAsClient
    public final void testFrontPage() {
        Response response;

        response = NetClientUtils.doGet(baseURL, "/",
                MediaType.APPLICATION_XML_TYPE);
        Assert.assertEquals(response.getStatus(),
                Response.Status.NOT_ACCEPTABLE.getStatusCode(),
                "Front page XML response status");
        response = NetClientUtils.doGet(baseURL, "/",
                MediaType.APPLICATION_JSON_TYPE);
        Assert.assertEquals(response.getStatus(),
                Response.Status.NOT_ACCEPTABLE.getStatusCode(),
                "Front page JSON response status");

        response = NetClientUtils.doGet(baseURL, "/",
                MediaType.TEXT_PLAIN_TYPE);
        Assert.assertEquals(response.getStatusInfo().getFamily(),
                Family.SUCCESSFUL,
                "Front page plain text response status");
        Assert.assertEquals(response.getMediaType(),
                MediaType.TEXT_PLAIN_TYPE,
                "Front page plain text response content type");
        Assert.assertEquals(response.readEntity(String.class),
                Index.INDEX_PLAIN_TEXT_CONTENT,
                "Front page plain text response content");

        response = NetClientUtils.doGet(baseURL, "/index.txt",
                MediaType.TEXT_PLAIN_TYPE);
        Assert.assertEquals(response.getStatusInfo().getFamily(),
                Family.SUCCESSFUL,
                "Front page (index.txt) plain text response status");
        Assert.assertEquals(response.getMediaType(),
                MediaType.TEXT_PLAIN_TYPE,
                "Front page (index.txt) plain text response content type");
        Assert.assertEquals(response.readEntity(String.class),
                Index.INDEX_PLAIN_TEXT_CONTENT,
                "Front page (index.txt) plain text response content");

        response = NetClientUtils.doGet(baseURL, "/",
                MediaType.TEXT_HTML_TYPE);
        Assert.assertEquals(response.getStatusInfo().getFamily(),
                Family.SUCCESSFUL,
                "Front page HTML response status");
        Assert.assertEquals(response.getMediaType(),
                MediaType.TEXT_HTML_TYPE,
                "Front page HTML response content type");
        Assert.assertEquals(response.readEntity(String.class),
                Index.INDEX_HTML_CONTENT,
                "Front page HTML response content");

        response = NetClientUtils.doGet(baseURL, "/index.html",
                MediaType.TEXT_HTML_TYPE);
        Assert.assertEquals(response.getStatusInfo().getFamily(),
                Family.SUCCESSFUL,
                "Front page (index.html) HTML response status");
        Assert.assertEquals(response.getMediaType(),
                MediaType.TEXT_HTML_TYPE,
                "Front page (index.html) HTML response content type");
        Assert.assertEquals(response.readEntity(String.class),
                Index.INDEX_HTML_CONTENT,
                "Front page (index.html) HTML response content");

        response = NetClientUtils.doGet(baseURL, "/index.htm",
                MediaType.TEXT_HTML_TYPE);
        Assert.assertEquals(response.getStatusInfo().getFamily(),
                Family.SUCCESSFUL,
                "Front page (index.htm) HTML response status");
        Assert.assertEquals(response.getMediaType(),
                MediaType.TEXT_HTML_TYPE,
                "Front page (index.htm) HTML response content type");
        Assert.assertEquals(response.readEntity(String.class),
                Index.INDEX_HTML_CONTENT,
                "Front page (index.htm) HTML response content");
    }

    /** Client-side tests of the "not found" (404) responses. */
    @Test
    @RunAsClient
    public final void testNotFound() {
        String notFound = "/notFound";
        Response response;
        ErrorResult errorResult;

        response = NetClientUtils.doGet(baseURL, notFound,
                MediaType.APPLICATION_XML_TYPE);
        Assert.assertEquals(response.getStatus(),
                Response.Status.NOT_FOUND.getStatusCode(),
                "Not found page XML response status");
        Assert.assertEquals(response.getMediaType(),
                MediaType.APPLICATION_XML_TYPE,
                "Not found page XML response content type");
        errorResult = response.readEntity(ErrorResult.class);
        Assert.assertEquals(errorResult.getMessage(),
                NotFoundExceptionMapper.NOT_FOUND,
                "Not found page XML response content");

        response = NetClientUtils.doGet(baseURL, notFound,
                MediaType.APPLICATION_JSON_TYPE);
        Assert.assertEquals(response.getStatus(),
                Response.Status.NOT_FOUND.getStatusCode(),
                "Not found page JSON response status");
        Assert.assertEquals(response.getMediaType(),
                MediaType.APPLICATION_JSON_TYPE,
                "Not found page JSON response content type");
        errorResult = response.readEntity(ErrorResult.class);
        Assert.assertEquals(errorResult.getMessage(),
                NotFoundExceptionMapper.NOT_FOUND,
                "Not found page JSON response content");

        response = NetClientUtils.doGet(baseURL, notFound,
                MediaType.TEXT_PLAIN_TYPE);
        Assert.assertEquals(response.getStatus(),
                Response.Status.NOT_FOUND.getStatusCode(),
                "Not found page plain text response status");
        Assert.assertEquals(response.getMediaType(),
                MediaType.TEXT_PLAIN_TYPE,
                "Not found page plain text response content type");
        Assert.assertEquals(response.readEntity(String.class),
                NotFoundExceptionMapper.NOT_FOUND,
                "Not found page plain text response content");

        response = NetClientUtils.doGet(baseURL, notFound,
                MediaType.TEXT_HTML_TYPE);
        Assert.assertEquals(response.getStatus(),
                Response.Status.NOT_FOUND.getStatusCode(),
                "Not found page HTML response status");
        Assert.assertEquals(response.getMediaType(),
                MediaType.TEXT_HTML_TYPE,
                "Not found page HTML response content type");
        Assert.assertEquals(response.readEntity(String.class),
                NotFoundExceptionMapper.NOT_FOUND_HTML,
                "Not found page HTML response content");
    }

    /** Client-side tests of the "internal server error" (500) responses. */
    @Test
    @RunAsClient
    public final void testInternalError() {
        Response response;
        ErrorResult errorResult;

        response = NetClientUtils.doGet(baseURL, "/" + ErrorHtml.SPLAT,
                MediaType.APPLICATION_XML_TYPE);
        Assert.assertEquals(response.getStatus(),
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Internal server error page XML response status");
        Assert.assertEquals(response.getMediaType(),
                MediaType.APPLICATION_XML_TYPE,
                "Internal server error page XML response content type");
        errorResult = response.readEntity(ErrorResult.class);
        Assert.assertEquals(errorResult.getMessage(),
                ErrorHtml.INTERNAL_ERROR,
                "Internal server error page XML response content");

        response = NetClientUtils.doGet(baseURL, "/" + ErrorHtml.SPLAT,
                MediaType.APPLICATION_JSON_TYPE);
        Assert.assertEquals(response.getStatus(),
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Internal server error page JSON response status");
        Assert.assertEquals(response.getMediaType(),
                MediaType.APPLICATION_JSON_TYPE,
                "Internal server error page JSON response content type");
        errorResult = response.readEntity(ErrorResult.class);
        Assert.assertEquals(errorResult.getMessage(),
                ErrorHtml.INTERNAL_ERROR,
                "Internal server error page JSON response content");

        response = NetClientUtils.doGet(baseURL, "/" + ErrorHtml.SPLAT,
                MediaType.TEXT_PLAIN_TYPE);
        Assert.assertEquals(response.getStatus(),
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Internal server error page plain text response status");
        Assert.assertEquals(response.getMediaType(),
                MediaType.TEXT_PLAIN_TYPE,
                "Internal server error page plain text response content type");
        Assert.assertEquals(response.readEntity(String.class),
                ErrorHtml.INTERNAL_ERROR,
                "Internal server error page plain text response content");

        response = NetClientUtils.doGet(baseURL, "/" + ErrorHtml.SPLAT,
                MediaType.TEXT_HTML_TYPE);
        Assert.assertEquals(response.getStatus(),
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Internal server error page HTML response status");
        Assert.assertEquals(response.getMediaType(),
                MediaType.TEXT_HTML_TYPE,
                "Internal server error page HTML response content type");
        Assert.assertEquals(response.readEntity(String.class),
                ErrorHtml.INTERNAL_ERROR_HTML,
                "Internal server error page HTML response content");
    }

}
