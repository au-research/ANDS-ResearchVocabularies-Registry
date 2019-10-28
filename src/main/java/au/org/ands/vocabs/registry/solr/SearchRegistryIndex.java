/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.solr;

import static au.org.ands.vocabs.registry.solr.FieldConstants.ACCESS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.ACRONYM;
import static au.org.ands.vocabs.registry.solr.FieldConstants.CONCEPT_PHRASE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.CONCEPT_SEARCH;
import static au.org.ands.vocabs.registry.solr.FieldConstants.DESCRIPTION;
import static au.org.ands.vocabs.registry.solr.FieldConstants.DESCRIPTION_PHRASE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.FORMAT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.ID;
import static au.org.ands.vocabs.registry.solr.FieldConstants.LANGUAGE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.LAST_UPDATED;
import static au.org.ands.vocabs.registry.solr.FieldConstants.LICENCE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.NOTE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.NOTE_PHRASE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.OWNER;
import static au.org.ands.vocabs.registry.solr.FieldConstants.PUBLISHER;
import static au.org.ands.vocabs.registry.solr.FieldConstants.PUBLISHER_PHRASE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.PUBLISHER_SEARCH;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SISSVOC_ENDPOINT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SLUG;
import static au.org.ands.vocabs.registry.solr.FieldConstants.STATUS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SUBJECT_LABELS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SUBJECT_PHRASE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SUBJECT_SEARCH;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TITLE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TITLE_PHRASE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TITLE_SEARCH;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TITLE_SORT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.WIDGETABLE;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.json.TermsFacetMap;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.util.JsonTextWriter;
import org.apache.solr.common.util.Utils;
import org.apache.solr.handler.component.HighlightComponent.HighlightMethod;
import org.apache.solr.search.QueryParsing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ifactory.press.db.solr.search.SafariQueryParser;

import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.enums.SearchSortOrder;
import au.org.ands.vocabs.registry.log.Analytics;

/** Methods for Solr searching of the registry collection. */
public final class SearchRegistryIndex {

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

    /** Set containing the names of field names used as facets.
     * Populated by a static block */
    private static Set<String> facets = new HashSet<>();

    static {
        facets.add(ACCESS);
        facets.add(FORMAT);
        facets.add(LANGUAGE);
        facets.add(LICENCE);
        facets.add(PUBLISHER);
        facets.add(SUBJECT_LABELS);
        // We don't currently offer widgetable as a facet in the Portal.
        // Leave it in for now, for possible future expansion.
        facets.add(WIDGETABLE);
    }

    /** Private constructor for a utility class. */
    private SearchRegistryIndex() {
    }

    /** Optimized access to the shared SolrClient for the registry collection.
     */
    private static final SolrClient SOLR_CLIENT_REGISTRY =
            SolrUtils.getSolrClientRegistry();

    /** Logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** The name of the query parser to use. */
    private static final String QUERY_PARSER = "safari";

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

    /** Value to use for "hl.maxAnalyzedChars". */
    private static final String HIGHLIGHT_MAX_CHARS = "10000000";

    /** Value to use for "hl.simple.pre". */
    private static final String HIGHLIGHT_PRE = "HL_START";

    /** Value to use for "hl.simple.post". */
    private static final String HIGHLIGHT_POST = "HL_END";

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

        // We specify two sets of facets in two different ways in
        // the Solr query:
        // 1. Using "traditional" Solr facets. The facet-based filters
        // specified in filtersJson get turned into these. The
        // facet counts for each facet/value reflect the number
        // of search results that match that facet/value.
        // 2. Using JSON facets. These are specified in order to
        // get facet values that are _not_ included in the
        // search results, but which would give extra search results
        // _if_ the client added them to filtersJson. The facet counts
        // for these facets/values are the number of _additional_
        // search results for these facets/values.

        // Prepare JSON facet information.
        // First, create a set to keep track of which facets are
        // been mentioned in filtersJson.
        Set<String> facetsActive = new HashSet<>();
        // Second, create and populate a JSON facet for _all_
        // of our facets. This will later be "trimmed": we will send to
        // Solr a request for a JSON facet for each facet mentioned
        // in filtersJson.
        Map<String, TermsFacetMap> jsonFacets = new HashMap<>();
        prepareJsonFacets(jsonFacets);

        // Always add these facet fields. These are the "traditional"
        // Solr facets mentioned above, that give facet counts that are
        // the number of search results that match each facet value.
        solrQuery.addFacetField(facets.toArray(new String[0]));
        solrQuery.setFacetSort(FacetParams.FACET_SORT_INDEX);
        solrQuery.setFacetMinCount(1);
        solrQuery.setFacetLimit(-1);

        // Keep track if we have seen a query term (i.e., the "q" parameter).
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
            // Rather than using a wildcard:
            //   solrQuery.addHighlightField("*");
            // add highlight fields for precisely the fields that
            // are searched. See the setting of the DisMaxParams.QF
            // and SafariQueryParser.PQF parameters in setQueryFields() below.
            // All of these fields have stored="true".
            // All of this means you never get a search result that doesn't
            // also have highlighting.
            solrQuery.addHighlightField(TITLE_SEARCH);
            solrQuery.addHighlightField(SUBJECT_SEARCH);
            solrQuery.addHighlightField(DESCRIPTION);
            solrQuery.addHighlightField(NOTE);
            solrQuery.addHighlightField(CONCEPT_SEARCH);
            solrQuery.addHighlightField(PUBLISHER_SEARCH);
            solrQuery.addHighlightField(TITLE_PHRASE);
            solrQuery.addHighlightField(SUBJECT_PHRASE);
            solrQuery.addHighlightField(DESCRIPTION_PHRASE);
            solrQuery.addHighlightField(NOTE_PHRASE);
            solrQuery.addHighlightField(CONCEPT_PHRASE);
            solrQuery.addHighlightField(PUBLISHER_PHRASE);
            // Use the "unified" highlight method, as it's significantly
            // faster than the "original" method.
            solrQuery.setParam(HighlightParams.METHOD,
                    HighlightMethod.UNIFIED.getMethodName());
            // By default, highlighting stops after 51200 characters
            // of content. To get highlighting of all concept data,
            // need to say explicitly to keep looking.
            solrQuery.setParam(HighlightParams.MAX_CHARS, HIGHLIGHT_MAX_CHARS);
            // With the "unified" highlight method (but not with the "original"
            // highlight method!), it seems we need
            // to set hl.requireFieldMatch=true to avoid some cases
            // of missing highlighting: e.g., where the
            // query is abc AND def, but no single field has _both_ terms.
            // See mailing list thread at:
            // http://mail-archives.apache.org/mod_mbox/lucene-solr-user/
            //        201907.mbox/%3cB5D715AC-C028-4081-BA7B-CFDE27CD6B0D@
            //        ardc.edu.au%3e
            solrQuery.setParam(HighlightParams.FIELD_MATCH, true);
            // Put markers around the highlighted content.
            solrQuery.setHighlightSimplePre(HIGHLIGHT_PRE);
            solrQuery.setHighlightSimplePost(HIGHLIGHT_POST);
            solrQuery.setHighlightSnippets(2);
            solrQuery.set(QueryParsing.DEFTYPE, QUERY_PARSER);
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
            // OWNER field is required (so far, only) by this method,
            // to include in analytics logging.
            solrQuery.setFields(
                    ID, LAST_UPDATED, SLUG, STATUS, TITLE, ACRONYM,
                    PUBLISHER, DESCRIPTION, WIDGETABLE,
                    SISSVOC_ENDPOINT, OWNER);
            // Ensure that the highlight fields are set above, corresponding
            // to the fields listed in the QF and PQF parameters.
            setQueryFields(solrQuery);

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
                                map(v -> "\""
                                        + StringEscapeUtils.escapeEcmaScript(v)
                                        + "\"").
                                collect(Collectors.joining(" "));
                        filterStringValue = filterStringValues;
                        filtersAndResultsExtracted.add(logFieldName);
                        filtersAndResultsExtracted.add(value);
                    } else {
                        filterStringValue = "\""
                                + StringEscapeUtils.escapeEcmaScript(
                                        value.toString()) + "\"";
                        filtersAndResultsExtracted.add(logFieldName);
                        filtersAndResultsExtracted.add(
                                new String[] {value.toString()});
                    }
                    addFilterQuery(solrQuery, jsonFacets, facetsActive,
                            filterKey, filterStringValue);
                    break;
                default:
                    // For now, ignore it.
                    break;
                }
            }
        }

        // We can now set rows.
        solrQuery.setRows(rows);
        // Always log the value of rows that we use, whether or not
        // the user provided a value for it.
        filtersAndResultsExtracted.add(Analytics.SEARCH_PP_FIELD);
        filtersAndResultsExtracted.add(rows);
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
        // Always log the value of searchSortOrder that we use,
        // whether or not the user provided a value for it.
        filtersAndResultsExtracted.add(Analytics.SEARCH_SORT_ORDER_FIELD);
        filtersAndResultsExtracted.add(searchSortOrder.value());

        trimSolrFacets(jsonFacets, facetsActive);
        if (!jsonFacets.isEmpty()) {
            solrQuery.add("json.facet", Utils.toJSONString(jsonFacets));
        }

        try {
            LOGGER.debug("solrQuery: " + solrQuery.toString());
            // Queries can get long, so force the use of the POST
            // method. (If the query exceeds 8192 characters,
            // the default GET method fails.)
            // See https://issues.apache.org/jira/browse/SOLR-13014
            // Sigh: this is currently _broken_ for EmbeddedSolrServer,
            // so for the test suite, need to use the default (GET) method.
            // See https://issues.apache.org/jira/browse/SOLR-12858
            QueryResponse responseQuery;
            if (SOLR_CLIENT_REGISTRY instanceof EmbeddedSolrServer) {
                responseQuery = SOLR_CLIENT_REGISTRY.query(solrQuery);
            } else {
                responseQuery = SOLR_CLIENT_REGISTRY.query(solrQuery,
                        SolrRequest.METHOD.POST);
            }
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
                LOGGER.debug("resultIds: " + resultIds);
                LOGGER.debug("resultOwners: " + resultOwners);
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

    /** Set the query fields. This sets the "qf" parameter, and
     * the "pqf" parameter provided by the Safari custom query parser.
     * @param query The Solr query being constructed.
     */
    private static void setQueryFields(
            final SolrQuery query) {
        // NB: ensure that the highlight fields are set above, corresponding
        // to the fields listed in the QF and PQF parameters.
        query.set(DisMaxParams.QF,
                TITLE_SEARCH + "^1 "
                        + SUBJECT_SEARCH + "^0.5 "
                        + DESCRIPTION + "^0.01 "
                        + NOTE + "^0.01 "
                        + CONCEPT_SEARCH + "^0.5 "
                        + PUBLISHER_SEARCH + "^0.5");
        // Default (1) boosting for phrases: phrase matches should
        // be considered (much) more important than non-phrase matches.
        query.set(SafariQueryParser.PQF,
                TITLE_PHRASE + " "
                        + SUBJECT_PHRASE + " "
                        + DESCRIPTION_PHRASE + " "
                        + NOTE_PHRASE + " "
                        + CONCEPT_PHRASE + " "
                        + PUBLISHER_PHRASE);
    }

    /** Populate the JSON facets map with initial values for
     * each facet.
     * @param jsonFacets The map of JSON facets.
     */
    private static void prepareJsonFacets(
            final Map<String, TermsFacetMap> jsonFacets) {
        for (String f : facets) {
            TermsFacetMap facetDefinition = new TermsFacetMap(f);
            jsonFacets.put(f, facetDefinition);
            facetDefinition.setSort("index asc");
            // Sigh, setLimit() requires a non-negative value!
            // We'd like to say:
            //  facetDefinition.setLimit(-1);
            // but that fails. So fall back to this "low-level" way instead:
            facetDefinition.put("limit", -1);
            Map<String, String> domain = new HashMap<>();
            // "filter" will contain negation of the top-level filter values
            // for this facet. No need to define it here; it is assigned
            // in addFilterQuery().
            //   domain.put("filter", null);
            // Use "excludeTags" to exclude the top-level filters
            // for this facet. The tag is the name of the facet.
            domain.put("excludeTags", f);
            facetDefinition.put("domain", domain);
        }
    }

    /** Add a filter to the query for one facet. This adds a
     * Solr "filter query" that is a disjunction of the values specified
     * for the facet. It marks the facet as being "active", and
     * it creates/adds clauses in all of the JSON facets.
     * @param solrQuery The Solr query being constructed.
     * @param jsonFacets The map of JSON facets.
     * @param facetsActive The set of active facets, i.e., the facets
     *      for which the query specified at least one filter.
     * @param facet The name of the facet.
     * @param filterStringValue The filter value(s) selected for the facet.
     */
    private static void addFilterQuery(
            final SolrQuery solrQuery,
            final Map<String, TermsFacetMap> jsonFacets,
            final Set<String> facetsActive,
            final String facet, final String filterStringValue) {
        // Add filter to top-level query, tagging it with the
        // name of the facet, so that it can be excluded in the jsonFacets
        // using the "excludeTags" setting applied by prepareJsonFacets().
        solrQuery.addFilterQuery(
                "{!tag=" + facet + "}"
                        + facet + ":(" + filterStringValue + ")");
        facetsActive.add(facet);
        TermsFacetMap jsonFacet = jsonFacets.get(facet);
        @SuppressWarnings("unchecked")
        Map<String, String> jsonFacetDomain =
        (Map<String, String>) jsonFacet.get("domain");
        // Replace filters for this facet with the negation!
        // (Note that this works in combination with the "excludeTags"
        // setting applied by prepareJsonFacets().)
        String jsonFacetDomainQuery = "-" + facet
                + ":(" + filterStringValue + ")";
        jsonFacetDomain.put("filter", jsonFacetDomainQuery);
    }

    /** Trim the map of JSON facets by removing the key/value pairs
     *  for facets that are not "active", i.e., for which no filter was
     *  specified for the facet.
     * @param jsonFacets The map of JSON facets.
     * @param facetsActive The set of active facets, i.e., the facets
     *      for which the query specified at least one filter.
     */
    private static void trimSolrFacets(
            final Map<String, TermsFacetMap> jsonFacets,
            final Set<String> facetsActive) {
        for (String f : facets) {
            if (!facetsActive.contains(f)) {
                jsonFacets.remove(f);
            }
        }
    }

}
