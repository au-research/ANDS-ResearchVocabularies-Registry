/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.context;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.toolkit.test.arquillian.ArquillianBaseTest;
import au.org.ands.vocabs.toolkit.test.utils.NetClientUtils;

/** Class that uses TestNG/Arquillian as a means to generate swagger.json
 * from the Registry API.
 */
@Test(groups = "swagger")
public class GenerateSwaggerJson extends ArquillianBaseTest {

    /** Logger. */
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

    /** Generate swagger.json.
     * @throws IOException If there is a failure when writing the
     *      file swagger.json.
     */
    @Test
    @RunAsClient
    public final void generateSwaggerJson() throws IOException {
        logger.info("In generateSwaggerJson()");
        Response response = NetClientUtils.doGet(baseURL,
                "swagger.json", MediaType.APPLICATION_JSON_TYPE);

        Assert.assertEquals(response.getStatusInfo().getFamily(),
                Family.SUCCESSFUL,
                "swagger.json response status");
        String body = response.readEntity(String.class);
        response.close();
        File file = new File("swagger.json");
        FileUtils.writeStringToFile(file, body, StandardCharsets.UTF_8);
    }

}
