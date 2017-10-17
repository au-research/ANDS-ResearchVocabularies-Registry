/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.solr;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.solr.admin.CreateSchema;
import au.org.ands.vocabs.registry.utils.PropertyConstants;
import au.org.ands.vocabs.registry.utils.RegistryProperties;
import au.org.ands.vocabs.toolkit.utils.ApplicationContextListener;

/** Support for connecting to Solr and sending API requests. */
public final class SolrUtils {

    /** Private constructor for a utility class. */
    private SolrUtils() {
    }

    /** Logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Access to the Solr API. */
    private static SolrClient solrClient;

    static {
        String solrCollectionUrl = RegistryProperties.getProperty(
                PropertyConstants.REGISTRY_SOLR_COLLECTION_URL);
        if (solrCollectionUrl.startsWith("http")) {
            solrClient = new HttpSolrClient.Builder(solrCollectionUrl).build();
        } else {
            // Start an embedded Solr for automated testing.
            String coreName = "vocabs-registry";
            LOGGER.info("Starting an embedded Solr.");
            Path classesPath = Paths.get(
                    ApplicationContextListener.getServletContext().
                    getRealPath("/WEB-INF/classes"));
            Path home = classesPath.resolve("solr");
            Path configFile = home.resolve("solr.xml");
            CoreContainer container = CoreContainer.createAndLoad(
                    home, configFile);
            solrClient = new EmbeddedSolrServer(container, coreName);
            CreateSchema createSchema = new CreateSchema();
            try {
                createSchema.installSchema(solrClient);
            } catch (SolrServerException | IOException e) {
                LOGGER.error("Unable to install schema", e);
            }
        }
    }

    /** Get the shared SolrClient.
     * @return The shared SolrClient.
     */
    public static SolrClient getSolrClient() {
        return solrClient;
    }

}
