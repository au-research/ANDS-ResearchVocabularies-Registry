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

import au.org.ands.vocabs.registry.solr.admin.CreateSolrSchemaRegistry;
import au.org.ands.vocabs.registry.solr.admin.CreateSolrSchemaResources;
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

    /** Access to the SolrJ API for the registry collection. */
    private static SolrClient solrClientRegistry;

    /** Access to the SolrJ API for the vocabulary resources collection. */
    private static SolrClient solrClientResources;

    /** Name of the registry collection used during tests. */
    public static final String TEST_COLLECTION_REGISTRY = "vocabs-registry";

    /** Name of the resources collection used during tests. */
    public static final String TEST_COLLECTION_RESOURCES = "vocabs-resources";

    static {
        CoreContainer container = null;

        // Registry collection.
        String solrRegistryCollectionUrl = RegistryProperties.getProperty(
                PropertyConstants.REGISTRY_SOLR_COLLECTION_URL);
        if (solrRegistryCollectionUrl.startsWith("http")) {
            solrClientRegistry = new HttpSolrClient.Builder(
                    solrRegistryCollectionUrl).build();
        } else {
            // Start an embedded Solr for automated testing.
            LOGGER.info("Starting an embedded Solr for the "
                    + "registry collection.");
            container = prepareEmbeddedContainer();
            solrClientRegistry = new EmbeddedSolrServer(container,
                    TEST_COLLECTION_REGISTRY);
            CreateSolrSchemaRegistry createSolrSchemaRegistry =
                    new CreateSolrSchemaRegistry();
            try {
                createSolrSchemaRegistry.installSchema(
                        null, solrClientRegistry, null, null);
            } catch (SolrServerException | IOException e) {
                LOGGER.error("Unable to install schema", e);
            }
        }

        // Resources collection.
        String solrResourcesCollectionUrl = RegistryProperties.getProperty(
                PropertyConstants.REGISTRY_SOLR_RESOURCES_COLLECTION_URL);
        if (solrResourcesCollectionUrl.startsWith("http")) {
            solrClientResources = new HttpSolrClient.Builder(
                    solrResourcesCollectionUrl).build();
        } else {
            // Start an embedded Solr for automated testing.
            LOGGER.info("Starting an embedded Solr for the "
                    + "resources collection.");
            if (container == null) {
                container = prepareEmbeddedContainer();
            }
            solrClientResources = new EmbeddedSolrServer(container,
                    TEST_COLLECTION_RESOURCES);
            CreateSolrSchemaResources createSolrSchemaResources =
                    new CreateSolrSchemaResources();
            try {
                createSolrSchemaResources.installSchema(
                        null, solrClientResources, null, null);
            } catch (SolrServerException | IOException e) {
                LOGGER.error("Unable to install schema", e);
            }
        }
    }

    /** Prepare a Solr Container containing test setup, for use
     * in creating EmbeddedSolrServer instances.
     * @return A Solr Container.
     */
    private static CoreContainer prepareEmbeddedContainer() {
        CoreContainer container;
        Path classesPath = Paths.get(
                ApplicationContextListener.getServletContext().
                getRealPath("/WEB-INF/classes"));
        Path home = classesPath.resolve("solr");
        Path configFile = home.resolve("solr.xml");
        // Define solr.install.dir, as used by solrconfig.xml.
        System.setProperty("solr.install.dir",
                home.toAbsolutePath().toString());
        container = CoreContainer.createAndLoad(home, configFile);
        return container;
    }

    /** Get the shared SolrClient for the registry collection.
     * @return The shared SolrClient for the registry collection.
     */
    public static SolrClient getSolrClientRegistry() {
        return solrClientRegistry;
    }

    /** Get the shared SolrClient for the resources collection.
     * @return The shared SolrClient for the resources collection.
     */
    public static SolrClient getSolrClientResources() {
        return solrClientResources;
    }

    /** Shut down the shared SolrClients, if any are active. */
    public static void shutdownSolrClients() {
        if (solrClientRegistry != null) {
            try {
                solrClientRegistry.close();
            } catch (IOException e) {
                LOGGER.error("Exception while closing SolrClient", e);
            }
            solrClientRegistry = null;
        }
        if (solrClientResources != null) {
            try {
                solrClientResources.close();
            } catch (IOException e) {
                LOGGER.error("Exception while closing SolrClient", e);
            }
            solrClientResources = null;
        }
    }

}
