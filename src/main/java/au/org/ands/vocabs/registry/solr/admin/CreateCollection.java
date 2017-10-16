/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.solr.admin;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;

/** Create the Solr Collection for the registry.
 * You must already have created the schema config using, e.g.,
 * bin/solr zk upconfig -n vocabs_schema_config -z localhost:9983
 *   -d server/solr/configsets/data_driven_schema_configs
 * Run as:
 * java au.org.ands.vocabs.registry.solr.admin.CreateCollection
 *  http://localhost:8983/solr
 *  vocabs_schema_config
 *  vocabs-registry
 */
public final class CreateCollection {

    /** Private constructor for a utility class. */
    private CreateCollection() {
    }

    /** Create the Solr collection.
     *
     * @param args The command-line parameters.
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    public static void main(final String[] args) {
        String apiURL;
        String configName;
        String collectionName;
        if (args.length != 3) {
            System.out.println("Must provide three command-line parameters: "
                    + "Solr-URL schema-name collection-name");
            return;
        }
        apiURL = args[0];
        configName = args[1];
        collectionName = args[2];

        try (SolrClient client = new HttpSolrClient.Builder(apiURL).build()) {
            CollectionAdminResponse caResponse;

            CollectionAdminRequest.Create createCollectionRequest =
                    CollectionAdminRequest.createCollection(collectionName,
                            configName, 1, 1);

            caResponse = createCollectionRequest.process(client);
            System.out.println("status:" + caResponse.getStatus());
            System.out.println("isSuccess:" + caResponse.isSuccess());
        } catch (SolrServerException sse) {
            System.out.println("Got a SolrServerException:");
            sse.printStackTrace();
        } catch (IOException ioe) {
            System.out.println("Got an IOException:");
            ioe.printStackTrace();
        }
    }

}
