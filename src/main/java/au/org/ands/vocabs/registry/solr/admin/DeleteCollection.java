/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.solr.admin;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;

/** Delete the Solr Collection for the registry.
 * Run as:
 * java au.org.ands.vocabs.registry.solr.admin.DeleteCollection
 *  http://localhost:8983/solr
 *  vocabs
 */
public final class DeleteCollection {

    /** Private constructor for a utility class. */
    private DeleteCollection() {
    }

    /** Create the collection.
     *
     * @param args The command-line parameters.
     */
    public static void main(final String[] args) {
        String apiURL;
        String collectionName;
        if (args.length != 2) {
            System.out.println("Must provide two command-line parameters");
            return;
        }
        apiURL = args[0];
        collectionName = args[1];

        try (SolrClient client = new HttpSolrClient.Builder(apiURL).build()) {
            CollectionAdminResponse response1;

            CollectionAdminRequest.Delete deleteCollectionRequest =
                    CollectionAdminRequest.deleteCollection(collectionName);

            response1 = deleteCollectionRequest.process(client);
            System.out.println("status:" + response1.getStatus());
            System.out.println("isSuccess:" + response1.isSuccess());
        } catch (SolrServerException sse) {
            System.out.println("Got a SolrServerException:");
            sse.printStackTrace();
        } catch (IOException ioe) {
            System.out.println("Got an IOException:");
            ioe.printStackTrace();
        }
    }

}
