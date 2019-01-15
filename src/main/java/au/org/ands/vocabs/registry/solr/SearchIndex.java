/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.solr;

import static au.org.ands.vocabs.registry.solr.FieldConstants.ACCESS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.ACRONYM;
import static au.org.ands.vocabs.registry.solr.FieldConstants.CONCEPT_SEARCH;
import static au.org.ands.vocabs.registry.solr.FieldConstants.DESCRIPTION;
import static au.org.ands.vocabs.registry.solr.FieldConstants.FORMAT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.FULLTEXT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.ID;
import static au.org.ands.vocabs.registry.solr.FieldConstants.LANGUAGE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.LICENCE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.PUBLISHER;
import static au.org.ands.vocabs.registry.solr.FieldConstants.PUBLISHER_SEARCH;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SISSVOC_ENDPOINT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SLUG;
import static au.org.ands.vocabs.registry.solr.FieldConstants.STATUS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SUBJECT_LABELS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SUBJECT_SEARCH;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TITLE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TITLE_SEARCH;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TITLE_SORT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.WIDGETABLE;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import au.org.ands.vocabs.registry.db.converter.JSONSerialization;

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

    /** Default number of rows to be returned. */
    private static final int DEFAULT_ROWS = 10;

    /** Number of rows to use, if the filters include a negative
     * value for rows. The name "ridiculously large value" comes
     * from the Solr documentation for the rows parameter at
     * <a
     *  href="https://wiki.apache.org/solr/CommonQueryParameters">https://wiki.apache.org/solr/CommonQueryParameters</a>.
     */
    private static final int RIDICULOUSLY_LARGE_VALUE = 10000000;

    /** Perform a Solr search.
     * @param filtersJson The query parameters, specified as a String
     *      in JSON format.
     * @return The response from Solr, in the raw JSON form that came back.
     * @throws IOException If the Solr query generated an IOException.
     * @throws SolrServerException If the Solr query generated a
     *      SolrServerException.
     */
    // Sorry for the long method, due to complex legacy behaviour
    // and the need to support embedded Solr for testing.
    // Please refactor this.
    @SuppressWarnings("checkstyle:MethodLength")
    public static String query(
            final String filtersJson)
            throws IOException, SolrServerException {
        SolrQuery solrQuery = new SolrQuery();
        // Specify that we want the raw JSON that Solr produces.
        // See one of the non-accepted answers at:
        // https://stackoverflow.com/questions/28374428/
        //             return-solr-response-in-json-format
        QueryRequest request = new QueryRequest(solrQuery);
        NoOpResponseParser rawJsonResponseParser = new NoOpResponseParser();
        rawJsonResponseParser.setWriterType(CommonParams.JSON);
        request.setResponseParser(rawJsonResponseParser);

        // Always add these facet fields.
        solrQuery.addFacetField(
                SUBJECT_LABELS, PUBLISHER, LANGUAGE, ACCESS,
                FORMAT, LICENCE, WIDGETABLE);
        solrQuery.setFacetSort(FacetParams.FACET_SORT_INDEX);
        solrQuery.setFacetMinCount(1);

        // Keep track if we have seen a query term.
        boolean queryIsSet = false;
        // Keep track of the rows we will ask for.
        // Set 10 rows as default. This can be overridden by passing
        // in a "pp" filter.
        int rows = DEFAULT_ROWS;

        // See if there are filters; if so, apply them.
        if (filtersJson != null) {
            Map<String, Object> filters =
                    JSONSerialization.deserializeStringAsJson(filtersJson,
                            new TypeReference<Map<String, Object>>() { });

            if (filters == null) {
                // NB: If invalid JSON, the previous deserializeStringAsJson()
                // will have logged an exception, but returned null.
                throw new IllegalArgumentException("Filter specification is "
                        + "not valid JSON");
            }

            // Since there are filters, always apply highlighting.
            // addHighlightField() does solrQuery.setHighlight(true) for us.
            solrQuery.addHighlightField("*");
            solrQuery.setHighlightSimplePre("&lt;b&gt;");
            solrQuery.setHighlightSimplePost("&lt;/b&gt;");
            solrQuery.setHighlightSnippets(2);
            solrQuery.set("defType", "edismax");
            // Check for a "pp" setting, now, as we might need it later
            // if we find a "p" filter.
            if (filters.containsKey("pp")) {
                // Support both "pp":2 and "pp":"2".
                Object rowsValueAsObject = filters.get("pp");
                if (rowsValueAsObject instanceof Integer) {
                    rows = (Integer) (rowsValueAsObject);
                } else if (rowsValueAsObject instanceof String) {
                    rows = Integer.parseInt((String) rowsValueAsObject);
                } else {
                    throw new IllegalArgumentException("pp parameter must "
                            + "be specified as either an integer or a "
                            + "string");
                }
                if (rows < 0) {
                    /* If a negative value specified, the caller really
                     * wants "all" rows. Unfortunately, Solr does not
                     * directly support that. See the Solr doc. */
                    rows = RIDICULOUSLY_LARGE_VALUE;
                }
            }

            solrQuery.set(DisMaxParams.ALTQ, "*:*");

            // see Portal (1) views/includes/search-view.blade.php for the
            // fields that must be returned for the "main" search function,
            // (2) assets/templates/widgetDirective.html and
            // assets/js/vocabDisplayDirective.js for the fields needed
            // for the Widget Explorer. The Widget Explorer needs
            // "sissvoc_endpoint" added to the list required by the "main"
            // search. NB: highlighting can/does also return snippets
            // from other fields not listed in fl (which is good!).
            solrQuery.setFields(
                    ID, SLUG, STATUS, TITLE, ACRONYM,
                    PUBLISHER, DESCRIPTION, WIDGETABLE,
                    SISSVOC_ENDPOINT);
            solrQuery.set(DisMaxParams.QF,
                    TITLE_SEARCH + "^1 "
                            + SUBJECT_SEARCH + "^0.5 "
                            + DESCRIPTION + "^0.01 "
                            + FULLTEXT + "^0.001 "
                            + CONCEPT_SEARCH + "^0.02 "
                            + PUBLISHER_SEARCH + "^0.5");

            for (Entry<String, Object> filterEntry : filters.entrySet()) {
                Object value = filterEntry.getValue();
                switch (filterEntry.getKey()) {
                case "q":
                    String stringValue = (String) value;
                    if (StringUtils.isNotBlank(stringValue)) {
                        solrQuery.setQuery(stringValue);
                        queryIsSet = true;
                    }
                    break;
                case "p":
                    // Support both "p":2 and "p":"2".
                    int page;
                    Object pValueAsObject = filterEntry.getValue();
                    if (pValueAsObject instanceof Integer) {
                        page = (Integer) (pValueAsObject);
                    } else if (pValueAsObject instanceof String) {
                        page = Integer.parseInt((String) pValueAsObject);
                    } else {
                        throw new IllegalArgumentException("p parameter must "
                                + "be specified as either an integer or a "
                                + "string");
                    }
                    if (page > 1) {
                        int start = rows * (page - 1);
                        solrQuery.setStart(start);
                    }
                    break;
                case "pp":
                    // We've already seen this, above.
                    break;
                case ACCESS:
                case FORMAT:
                case LANGUAGE:
                case LICENCE:
                case PUBLISHER:
                case SUBJECT_LABELS:
                case WIDGETABLE:
                    // In the following, support values that are _either_
                    // just a string, or an _array_ of strings.
                    String filterStringValue;
                    if (value instanceof ArrayList) {
                        if (((ArrayList<?>) value).isEmpty()) {
                            // The portal does send empty lists. In this case,
                            // don't add anything to the query.
                            break;
                        }
                        @SuppressWarnings("unchecked")
                        String filterStringValues = ((ArrayList<String>) value).
                                stream().
                                map(v -> "+\""
                                        + StringEscapeUtils.escapeEcmaScript(
                                                v.toString()) + "\"").
                                collect(Collectors.joining(" "));
                        filterStringValue = filterStringValues;
                    } else {
                        filterStringValue = "+\""
                                + StringEscapeUtils.escapeEcmaScript(
                                        value.toString()) + "\"";
                    }
                    solrQuery.addFilterQuery(filterEntry.getKey()
                            + ":(" + filterStringValue + ")");
                    break;
                default:
                    // For now, ignore it.
                    break;
                }
            }
        }

        // We can now set rows.
        solrQuery.setRows(rows);
        // If there was no query specified, get all documents,
        // and sort by title_sort.
        if (!queryIsSet) {
            solrQuery.setQuery("*:*");
            solrQuery.setSort(TITLE_SORT, ORDER.asc);
        }

        try {
            NamedList<Object> response = SOLR_CLIENT.request(request);
            Object responseObject = response.get("response");
            if (responseObject instanceof SolrDocumentList) {
                // NB: We reach here _only_ for the embedded Solr.
                // For "normal" use with the HTTP server, we fall through
                // without further modifying responseObject.
                // Special treatment for the embedded Solr used in
                // testing: fake the JSON that comes back from the HTTP server.
                SolrDocumentList solrDocumentList =
                        (SolrDocumentList) responseObject;
                Map<String, Object> map = new HashMap<>();
                map.put("responseHeader", new HashMap<>());
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("docs", response.get("response"));
                responseMap.put("numFound", solrDocumentList.getNumFound());
                responseMap.put("start", solrDocumentList.getStart());
                map.put("response", responseMap);
                // Now overwrite responseObject, so that it approximates
                // the "real thing".
                responseObject = JSONSerialization.serializeObjectAsJsonString(
                        map);
            }
            return (String) (responseObject);
        } catch (IOException | SolrServerException e) {
            LOGGER.error("Exception while performing Solr query", e);
            throw e;
        }
    }

}
