/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

import au.org.ands.vocabs.registry.utils.PropertyConstants;
import au.org.ands.vocabs.registry.utils.RegistryProperties;

/** Support for connecting to Solr and sending API requests. */
public final class SolrUtils {

    /** Private constructor for a utility class. */
    private SolrUtils() {
    }

    /** Access to the Solr API. */
    private static SolrClient solrClient =
            new HttpSolrClient.Builder(RegistryProperties.getProperty(
                    PropertyConstants.REGISTRY_SOLR_COLLECTION_URL)).build();

    /** Get the shared SolrClient.
     * @return The shared SolrClient.
     */
    public static SolrClient getSolrClient() {
        return solrClient;
    }

}
