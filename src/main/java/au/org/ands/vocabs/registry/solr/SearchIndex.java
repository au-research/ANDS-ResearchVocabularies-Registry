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
import static au.org.ands.vocabs.registry.solr.FieldConstants.LAST_UPDATED;
import static au.org.ands.vocabs.registry.solr.FieldConstants.LICENCE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.OWNER;
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
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.util.JsonTextWriter;
import org.apache.solr.handler.component.HighlightComponent.HighlightMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.enums.SearchSortOrder;
import au.org.ands.vocabs.registry.log.Analytics;

/** Methods for Solr searching. */
public final class SearchIndex {

    /** Map of incoming filter keys to the field names used for logging. */
    private static Map<String, String> filterArrays = new HashMap<>();

    static {
        filterArrays.put(ACCESS, Analytics.SEARCH_ACCESS_FIELD);
        filterArrays.put(FORMAT, Analytics.SEARCH_FORMAT_FIELD);
        filterArrays.put(LANGUAGE, Analytics.SEARCH_LANGUAGE_FIELD);
        filterArrays.put(LICENCE, Analytics.SEARCH_LICENCE_FIELD);
        filterArrays.put(PUBLISHER, Analytics.SEARCH_PUBLISHER_FIELD);
        filterArrays.put(SUBJECT_LABELS, Analytics.SEARCH_SUBJECT_LABELS_FIELD);
    }

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
     *  href="https://wiki.apache.org/solr/CommonQueryParameters">https://wiki.apache.org/solr/CommonQueryParameters</a>
     * (archived at <a
     *  href="https://web.archive.org/web/20190405125211/https://wiki.apache.org/solr/CommonQueryParameters">https://web.archive.org/web/20190405125211/https://wiki.apache.org/solr/CommonQueryParameters</a>).
     */
    private static final int RIDICULOUSLY_LARGE_VALUE = 10000000;

    /* Things to pay attention to, when performing maintenance on
     * this method:
     *
     * + search filters that are numeric values ("p", "pp"):
     *   cope with them being provided either as numeric values or as strings
     * + analytics logging
     * + test cases!
     */

    /** Perform a Solr search.
     * @param filtersJson The query parameters, specified as a String
     *      in JSON format.
     * @param filtersAndResultsExtracted A list into which the extracted
     *      query parameters and some of the fields of the results are stored,
     *      for later use in analytics logging.
     *      For each type of logging field, two elements are appended
     *      to the list: the first is the log field name; the second
     *      is the value of the field.
     * @param logResults true, if details of results are to be included in
     *      {@code filtersAndResultsExtracted}.
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
            final String filtersJson,
            final List<Object> filtersAndResultsExtracted,
            final boolean logResults)
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
        // Keep track of any search sort order specified.
        SearchSortOrder searchSortOrder = null;

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
            // Use the "unified" highlight method, as it's significantly
            // faster than the "original" method.
            solrQuery.setParam(HighlightParams.METHOD,
                    HighlightMethod.UNIFIED.getMethodName());
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
                filtersAndResultsExtracted.add(Analytics.SEARCH_PP_FIELD);
                filtersAndResultsExtracted.add(rows);
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
            // OWNER field is required (so far, only) by this method,
            // to include in analytics logging.
            solrQuery.setFields(
                    ID, LAST_UPDATED, SLUG, STATUS, TITLE, ACRONYM,
                    PUBLISHER, DESCRIPTION, WIDGETABLE,
                    SISSVOC_ENDPOINT, OWNER);
            solrQuery.set(DisMaxParams.QF,
                    TITLE_SEARCH + "^1 "
                            + SUBJECT_SEARCH + "^0.5 "
                            + DESCRIPTION + "^0.01 "
                            + FULLTEXT + "^0.001 "
                            + CONCEPT_SEARCH + "^0.02 "
                            + PUBLISHER_SEARCH + "^0.5");

            for (Entry<String, Object> filterEntry : filters.entrySet()) {
                Object value = filterEntry.getValue();
                String filterKey = filterEntry.getKey();
                switch (filterKey) {
                case "q":
                    String stringValue = (String) value;
                    if (StringUtils.isNotBlank(stringValue)) {
                        solrQuery.setQuery(stringValue);
                        queryIsSet = true;
                        filtersAndResultsExtracted.add(
                                Analytics.SEARCH_Q_FIELD);
                        filtersAndResultsExtracted.add(stringValue);
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
                    filtersAndResultsExtracted.add(Analytics.SEARCH_P_FIELD);
                    filtersAndResultsExtracted.add(page);
                    break;
                case "pp":
                    // We've already seen this, above.
                    break;
                case "sort":
                    Object ssoValueAsObject = filterEntry.getValue();
                    if (ssoValueAsObject instanceof String) {
                        try {
                            searchSortOrder = SearchSortOrder.fromValue(
                                    (String) ssoValueAsObject);
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException("sort "
                                    + "parameter must be one of the "
                                    + "supported values");
                        }
                    }
                    // searchSortOrder remains null if no value specified,
                    // or the value specified was not a string.
                    break;
                case WIDGETABLE:
                    String widgetableValue;
                    widgetableValue = "+\""
                            + StringEscapeUtils.escapeEcmaScript(
                                    value.toString()) + "\"";
                    solrQuery.addFilterQuery(WIDGETABLE
                            + ":(" + widgetableValue + ")");
                    filtersAndResultsExtracted.add(
                            Analytics.SEARCH_WIDGETABLE_FIELD);
                    if (value instanceof Boolean) {
                        filtersAndResultsExtracted.add(value);
                    } else {
                        filtersAndResultsExtracted.add(BooleanUtils.toBoolean(
                                value.toString()));
                    }
                    break;
                case ACCESS:
                case FORMAT:
                case LANGUAGE:
                case LICENCE:
                case PUBLISHER:
                case SUBJECT_LABELS:
                    // In the following, support values that are _either_
                    // just a string, or an _array_ of strings.
                    String filterStringValue;
                    String logFieldName = filterArrays.get(filterKey);
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
                        filtersAndResultsExtracted.add(logFieldName);
                        filtersAndResultsExtracted.add(value);
                    } else {
                        filterStringValue = "+\""
                                + StringEscapeUtils.escapeEcmaScript(
                                        value.toString()) + "\"";
                        filtersAndResultsExtracted.add(logFieldName);
                        filtersAndResultsExtracted.add(
                                new String[] {value.toString()});
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
            if (searchSortOrder == null) {
                searchSortOrder = SearchSortOrder.A_TO_Z;
            }
            switch (searchSortOrder) {
            case A_TO_Z:
                solrQuery.setSort(TITLE_SORT, ORDER.asc);
                break;
            case Z_TO_A:
                solrQuery.setSort(TITLE_SORT, ORDER.desc);
                break;
            case RELEVANCE:
                throw new IllegalArgumentException("relevance sort "
                        + "only allowed when there is a query term");
            case LAST_UPDATED_ASC:
                solrQuery.setSort(LAST_UPDATED, ORDER.asc);
                break;
            case LAST_UPDATED_DESC:
                solrQuery.setSort(LAST_UPDATED, ORDER.desc);
                break;
            default:
                LOGGER.error("Unknown search sort order: " + searchSortOrder);
            }
        } else {
            if (searchSortOrder == null) {
                searchSortOrder = SearchSortOrder.RELEVANCE;
            }
            switch (searchSortOrder) {
            case A_TO_Z:
                solrQuery.setSort(TITLE_SORT, ORDER.asc);
                break;
            case Z_TO_A:
                solrQuery.setSort(TITLE_SORT, ORDER.desc);
                break;
            case RELEVANCE:
                // Nothing to do.
                break;
            case LAST_UPDATED_ASC:
                solrQuery.setSort(LAST_UPDATED, ORDER.asc);
                break;
            case LAST_UPDATED_DESC:
                solrQuery.setSort(LAST_UPDATED, ORDER.desc);
                break;
            default:
                LOGGER.error("Unknown search sort order: " + searchSortOrder);
            }
        }

        try {
            QueryResponse responseQuery = SOLR_CLIENT.query(solrQuery);
            if (logResults) {
                SolrDocumentList solrDocumentList =
                        responseQuery.getResults();
                List<Integer> resultIds = new ArrayList<>();
                List<String> resultOwners = new ArrayList<>();
                for (SolrDocument sd : solrDocumentList) {
                    resultIds.add(Integer.valueOf(
                            (String) sd.getFieldValue(ID)));
                    resultOwners.add((String) sd.getFieldValue(OWNER));
                }
                LOGGER.info("resultIds: " + resultIds);
                LOGGER.info("resultOwners: " + resultOwners);
                filtersAndResultsExtracted.add(
                        Analytics.SEARCH_RESULT_ID_FIELD);
                filtersAndResultsExtracted.add(resultIds);
                filtersAndResultsExtracted.add(
                        Analytics.SEARCH_RESULT_OWNER_FIELD);
                filtersAndResultsExtracted.add(resultOwners);
                filtersAndResultsExtracted.add(
                        Analytics.SEARCH_RESULT_NUM_FOUND_FIELD);
                filtersAndResultsExtracted.add(solrDocumentList.getNumFound());
            }

            StringWriter stringWriter = new StringWriter();
            // Specify JSON_NL_FLAT to get the standard stupid format
            // with interleaved keys/values: [ "English", 19, "German", 1 ]
            ServerSolrJSONWriter solrJSONWriter =
                    new ServerSolrJSONWriter(stringWriter,
                    JsonTextWriter.JSON_NL_FLAT);
            solrJSONWriter.writeObj(responseQuery);
            solrJSONWriter.close();
            return stringWriter.toString();
        } catch (IOException | SolrServerException e) {
            LOGGER.error("Exception while performing Solr query", e);
            throw e;
        }
    }

}
