/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.solr;

import static au.org.ands.vocabs.registry.solr.FieldConstants.ACCESS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.FORMAT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.LANGUAGE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.LICENCE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.PUBLISHER;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SUBJECT_LABELS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.WIDGETABLE;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Methods for Solr searching. */
public final class SearchIndex {

    /** Private constructor for a utility class. */
    private SearchIndex() {
    }

    /** Optimized access to the shared SolrClient. */
    private static final SolrClient SOLR_CLIENT = SolrUtils.getSolrClient();

    /** Logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Perform a Solr search.
     *
     * This method is not finished. Not yet usable!
     *
     * @param queryParams The query parameters.
     * @return The response from Solr.
     * @throws IOException If the Solr query generated an IOException.
     * @throws SolrServerException If the Solr query generated a
     *      SolrServerException.
     */
    public static QueryResponse query(
            final MultivaluedMap<String, String> queryParams)
            throws IOException, SolrServerException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.addFacetField(
                SUBJECT_LABELS,
                PUBLISHER,
                LANGUAGE,
                ACCESS,
                FORMAT,
                LICENCE,
                WIDGETABLE);
        solrQuery.setFacetSort("index asc");
        solrQuery.setFacetMinCount(1);

        queryParams.forEach((key, values) -> {
            switch (key) {
            case "q":
                break;
            default:
                break;
            }
        });

        try {
            return SOLR_CLIENT.query(solrQuery);
        } catch (IOException | SolrServerException e) {
            LOGGER.error("Exception while performing Solr query", e);
            throw e;
        }
    }

}
