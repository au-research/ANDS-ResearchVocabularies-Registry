/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.solr;

import static au.org.ands.vocabs.registry.solr.FieldConstants.ALL_SUFFIX;
import static au.org.ands.vocabs.registry.solr.FieldConstants.DCTERMS_DESCRIPTION;
import static au.org.ands.vocabs.registry.solr.FieldConstants.DCTERMS_TITLE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.ID;
import static au.org.ands.vocabs.registry.solr.FieldConstants.IRI;
import static au.org.ands.vocabs.registry.solr.FieldConstants.LANGUAGE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.LAST_UPDATED;
import static au.org.ands.vocabs.registry.solr.FieldConstants.OWNER;
import static au.org.ands.vocabs.registry.solr.FieldConstants.PHRASE_SUFFIX;
import static au.org.ands.vocabs.registry.solr.FieldConstants.PUBLISHER;
import static au.org.ands.vocabs.registry.solr.FieldConstants.RDFS_LABEL;
import static au.org.ands.vocabs.registry.solr.FieldConstants.RDF_TYPE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SEARCH_SUFFIX;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SISSVOC_ENDPOINT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SKOS_ALTLABEL;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SKOS_DEFINITION;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SKOS_HIDDENLABEL;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SKOS_NOTATION;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SKOS_PREFLABEL;
import static au.org.ands.vocabs.registry.solr.FieldConstants.STATUS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SUBJECT_LABELS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TITLE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TITLE_SORT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TOP_CONCEPT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TOP_CONCEPT_PHRASE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.VERSION_ID;
import static au.org.ands.vocabs.registry.solr.FieldConstants.VERSION_RELEASE_DATE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.VERSION_TITLE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.VOCABULARY_ID;
import static au.org.ands.vocabs.registry.solr.FieldConstants.VOCABULARY_ID_IRI;
import static au.org.ands.vocabs.registry.solr.FieldConstants.VOCABULARY_TITLE;

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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
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
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.ExpandParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.util.JsonTextWriter;
import org.apache.solr.common.util.Utils;
import org.apache.solr.handler.component.HighlightComponent.HighlightMethod;
import org.apache.solr.search.CollapsingQParserPlugin;
import org.apache.solr.search.QueryParsing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ifactory.press.db.solr.search.SafariQueryParser;

import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.enums.SearchResourcesCollapse;
import au.org.ands.vocabs.registry.enums.SearchSortOrder;
import au.org.ands.vocabs.registry.log.Analytics;

/** Methods for Solr searching of the resources collection. */
public final class SearchResourcesIndex {

    /** Map of incoming filter keys to the field names used for logging. */
    private static Map<String, String> filterArrays = new HashMap<>();

    static {
        filterArrays.put(LANGUAGE, Analytics.SEARCH_LANGUAGE_FIELD);
        // Possible future work: support filtering by last_updated.
        // filterArrays.put(LAST_UPDATED, Analytics.SEARCH_LAST_UPDATED_FIELD);
        filterArrays.put(PUBLISHER, Analytics.SEARCH_PUBLISHER_FIELD);
        filterArrays.put(RDF_TYPE, Analytics.SEARCH_RDF_TYPE_FIELD);
        filterArrays.put(STATUS, Analytics.SEARCH_VERSION_STATUS_FIELD);
        filterArrays.put(SUBJECT_LABELS, Analytics.SEARCH_SUBJECT_LABELS_FIELD);
    }

    /** Set containing the names of field names used as facets.
     * Populated by a static block */
    private static Set<String> facets = new HashSet<>();

    static {
        // Possible future work: support faceting by last_updated.
        // facets.add(LAST_UPDATED);
        facets.add(PUBLISHER);
        facets.add(RDF_TYPE);
        facets.add(STATUS);
        facets.add(SUBJECT_LABELS);
    }

    /** Private constructor for a utility class. */
    private SearchResourcesIndex() {
    }

    /** Optimized access to the shared SolrClient for the resources collection.
     */
    private static final SolrClient SOLR_CLIENT_RESOURCES =
            SolrUtils.getSolrClientResources();

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

    /** Value to use in the language filter to specify that
     * multilingual fields without a language tag should be searched
     * and reported on.
     */
    public static final String NO_LANGUAGE = "NONE";

    /** When collapse/expand has been applied, each main result
     * will include a field/value with this field name alias,
     * whose value is a copy of the value used to do the collapsing, and which
     * can be used as an index into the expanded section. The point is
     * to avoid extra client-side conditionals and other code otherwise
     * needed to determine the key to use for each result.
     */
    private static final String COLLAPSE_ID = "collapse_id";

    /** Setting to collapse results that have the same IRI, giving
     * preference to the result that is the result with
     * the most recent last_updated value of a current version
     * as each group's representative. */
    private static final String COLLAPSE_IRI =
            "{!" + CollapsingQParserPlugin.NAME
            + " field=" + IRI
            + " nullPolicy=" + CollapsingQParserPlugin.NULL_EXPAND
            + " " + CommonParams.SORT
            + "='"
            + STATUS + " asc"
            + ","
            + LAST_UPDATED + " desc"
            + "'"
            +  "}";

    /** Setting to collapse results that have the same IRI <i>and</i>
     * the same vocabulary Id, giving
     * preference to the result that is the result with
     * the most recent last_updated value of a current version
     * as each group's representative. */
    private static final String COLLAPSE_VOCABULARY_ID_IRI =
            "{!" + CollapsingQParserPlugin.NAME
            + " field=" + VOCABULARY_ID_IRI
            + " nullPolicy=" + CollapsingQParserPlugin.NULL_EXPAND
            + " " + CommonParams.SORT
            + "='"
            + STATUS + " asc"
            + ","
            + LAST_UPDATED + " desc"
            + "'"
            +  "}";

    /** Setting to sort expanded results, where there was collapsing
     * by IRI. We "group" by vocabulary, then give the
     * "most recently-released" (as far as we can tell) first.
     * Note that given the field types, the answer is, in general,
     * not "sorted" by vocabulary Id:
     * the vocabulary_id field has string type,
     * and their values are sorted _as strings_, rather than
     * as numeric values. This means that, for example, "100" &lt; "99".
     * In this case, we don't really mind about the vocabulary_ids,
     * as we're just grouping identical vocabulary_ids together.
     * We have made version_id of type pint, and therefore its values
     * _are_ sorted "correctly" as numeric values.
     */
    private static final String EXPAND_SORT_IRI =
            VOCABULARY_ID + " asc,"
            + VERSION_RELEASE_DATE + " desc,"
            + VERSION_ID + " desc";

    /** Setting to sort expanded results, where there was collapsing
     * by IRI <i>and</i> vocabulary Id. We "group" by the
     * "most recently-released" (as far as we can tell) version first.
     * Note that given the field types, the answer is, in general,
     * not "sorted" by vocabulary Id:
     * the vocabulary_id field has string type,
     * and their values are sorted _as strings_, rather than
     * as numeric values. This means that, for example, "100" &lt; "99".
     * In this case, we don't really mind about the vocabulary_ids,
     * as we're just grouping identical vocabulary_ids together.
     * We have made version_id of type pint, and therefore its values
     * _are_ sorted "correctly" as numeric values.
     */
    private static final String EXPAND_SORT_VOCABULARY_ID_IRI =
            VERSION_RELEASE_DATE + " desc,"
            + VERSION_ID + " desc";

    /** Limit on the number of additional results for each IRI
     * included in the "expanded" section of results.
     * The value has been chosen somewhat arbitrarily; revisit
     * if/when necessary.
     */
    private static final String EXPAND_ROWS = "100";

    /** Basic list of fields to be returned from queries. Supplement this
     * list with language-specific fields. */
    private static final ArrayList<String> BASIC_FIELDS;

    static {
        BASIC_FIELDS = new ArrayList<>();
        BASIC_FIELDS.add(ID);
        BASIC_FIELDS.add(IRI);
        BASIC_FIELDS.add(LAST_UPDATED);
        BASIC_FIELDS.add(VOCABULARY_ID);
        BASIC_FIELDS.add(VOCABULARY_ID_IRI);
        BASIC_FIELDS.add(VOCABULARY_TITLE);
        BASIC_FIELDS.add(TITLE);
        BASIC_FIELDS.add(TOP_CONCEPT);
        BASIC_FIELDS.add(TOP_CONCEPT_PHRASE);
        BASIC_FIELDS.add(OWNER);
        BASIC_FIELDS.add(PUBLISHER);
        BASIC_FIELDS.add(VERSION_ID);
        BASIC_FIELDS.add(VERSION_RELEASE_DATE);
        BASIC_FIELDS.add(VERSION_TITLE);
        BASIC_FIELDS.add(STATUS);
        BASIC_FIELDS.add(RDF_TYPE);
        BASIC_FIELDS.add(SISSVOC_ENDPOINT);
        BASIC_FIELDS.add(SKOS_NOTATION);
    }

    /** String containing the regular expression for validating
     * language tags. For now, that means either (a) the special
     * value of {@code ALL_SUFFIX}, or (b) a value that contains
     * only Latin alphabetic characters. */
    private static final String LANGUAGE_REGEX =
            "^("
            + ALL_SUFFIX
            + "|"
            + "\\p{Alpha}+"
            + ")$";

    /** The compiled pattern containing the regular expression for
     *  validating language tags. */
    private static final Pattern LANGUAGE_PATTERN =
            Pattern.compile(LANGUAGE_REGEX);

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

        // Possible future work: support year ranges for last_updated facet.
//        solrQuery.set("facet.range", LAST_UPDATED);
//        solrQuery.set("f."
//                + LAST_UPDATED
//                + ".facet.range.start", "2010-01-01T00:00:00Z");
//        solrQuery.set("f."
//                + LAST_UPDATED
//                + ".facet.range.end", "NOW+1YEAR");
//        solrQuery.set("f."
//                + LAST_UPDATED
//                + ".facet.range.gap", "+1YEAR");

        // Keep track if we have seen a query term (i.e., the "q" parameter).
        boolean queryIsSet = false;
        // Keep track of the rows we will ask for.
        // Set 10 rows as default. This can be overridden by passing
        // in a "pp" filter.
        int rows = DEFAULT_ROWS;
        // Keep track of any search sort order specified.
        SearchSortOrder searchSortOrder = null;

        // We will do collapsing/expanding unless the client says not to.
        SearchResourcesCollapse collapseExpand =
                SearchResourcesCollapse.VOCABULARY_ID_IRI;

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

            // Extract the languages from the filters.
            // An empty list means no languages have been specified,
            // which will mean that we will use the "_all" fields.
            ArrayList<String> languages = new ArrayList<>();

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
                case "collapse_expand":
                    // Apply collapse/expand settings.
                    // The default is "vocabularyIdIri"; we do something
                    // different _only if_ the user provides a value
                    // that's different and valid.
                    Object collapseExpandValueAsObject = filterEntry.getValue();
                    if (collapseExpandValueAsObject instanceof String) {
                        try {
                            collapseExpand = SearchResourcesCollapse.
                                    fromValue((String)
                                            collapseExpandValueAsObject);
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException(
                                    "collapse_expand parameter invalid");
                        }
                    } else {
                        throw new IllegalArgumentException("collapse_expand "
                                + "parameter must be a string");
                    }
                    break;
                case LANGUAGE:
                    // Can filter on language, but it's not a _facet_.
                    // Language setting affects which _fields_ are
                    // searched and highlighted.
                    // In the following, support values that are _either_
                    // just a string, or an _array_ of strings.
                    if (value instanceof ArrayList) {
                        if (((ArrayList<?>) value).isEmpty()) {
                            // The portal does send empty lists. In this case,
                            // don't add anything to the query.
                            break;
                        }
                        @SuppressWarnings("unchecked")
                        ArrayList<String> stringValues =
                            (ArrayList<String>) value;
                        for (String v : stringValues) {
                            stringValue = StringUtils.trimToNull(v);
                            validateLanguage(stringValue);
                            if (stringValue != null) {
                                languages.add(mapLanguage(stringValue));
                            }
                        }
                    } else {
                        stringValue = StringUtils.trimToNull(value.toString());
                        if (stringValue != null) {
                            validateLanguage(stringValue);
                            languages.add(mapLanguage(stringValue));
                        }
                    }
                    break;
                // Possible future work: support faceting by last_updated.
//                case LAST_UPDATED:
                case PUBLISHER:
                case RDF_TYPE:
                case STATUS:
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

            // At this point, we now know about any specified
            // language filters. Only now can we set query fields
            // and highlight fields.
            if (languages.size() == 0) {
                // No languages specified as filters, so use the
                // special suffix "_all".
                languages.add(ALL_SUFFIX);
            }
            // Now we know the value of languages to put into the analytics.
            filtersAndResultsExtracted.add(LANGUAGE);
            filtersAndResultsExtracted.add(languages);

            // see Portal views/includes/search-view.blade.php for the
            // fields that must be returned for the "main" search function.
            // NB: highlighting can/does also return snippets
            // from other fields not listed in fl (which is good!).
            setQueryFields(solrQuery, languages);
            // Ensure that the highlight fields are set corresponding
            // to the fields listed in the QF and PQF parameters.
            // We could use solrQuery.setFields(ID, IRI, etc.)
            // follows by lots of solrQuery.addField(...),
            // but let's make it more efficient by making our own list.
            @SuppressWarnings("unchecked")
            ArrayList<String> fieldsList = (ArrayList<String>)
                    BASIC_FIELDS.clone();
            for (String language : languages) {
                fieldsList.add(SKOS_PREFLABEL + language);
                fieldsList.add(SKOS_ALTLABEL + language);
                fieldsList.add(SKOS_HIDDENLABEL + language);
                fieldsList.add(SKOS_DEFINITION + language);
                fieldsList.add(RDFS_LABEL + language);
                fieldsList.add(DCTERMS_TITLE + language);
                fieldsList.add(DCTERMS_DESCRIPTION + language);

                fieldsList.add(SKOS_PREFLABEL + SEARCH_SUFFIX + language);
                fieldsList.add(SKOS_ALTLABEL + SEARCH_SUFFIX + language);
                fieldsList.add(SKOS_HIDDENLABEL + SEARCH_SUFFIX + language);
                fieldsList.add(SKOS_DEFINITION + SEARCH_SUFFIX + language);
                fieldsList.add(RDFS_LABEL + SEARCH_SUFFIX + language);
                fieldsList.add(DCTERMS_TITLE + SEARCH_SUFFIX + language);
                fieldsList.add(DCTERMS_DESCRIPTION + SEARCH_SUFFIX + language);

                fieldsList.add(SKOS_PREFLABEL + PHRASE_SUFFIX + language);
                fieldsList.add(SKOS_ALTLABEL + PHRASE_SUFFIX + language);
                fieldsList.add(SKOS_HIDDENLABEL + PHRASE_SUFFIX + language);
                fieldsList.add(SKOS_DEFINITION + PHRASE_SUFFIX + language);
                fieldsList.add(RDFS_LABEL + PHRASE_SUFFIX + language);
                fieldsList.add(DCTERMS_TITLE + PHRASE_SUFFIX + language);
                fieldsList.add(DCTERMS_DESCRIPTION + PHRASE_SUFFIX + language);
            }
            solrQuery.setFields(fieldsList.toArray(new String[0]));

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
//            solrQuery.addHighlightField(IRI);
//            solrQuery.addHighlightField(PUBLISHER);
//            solrQuery.addHighlightField(RDF_TYPE);
//            solrQuery.addHighlightField(SUBJECT_LABELS);
            solrQuery.addHighlightField(TOP_CONCEPT);
            solrQuery.addHighlightField(TOP_CONCEPT_PHRASE);
            addMultilingualHighlightFields(solrQuery, languages);
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
        }
        // That was the end of the body of the condition
        // "if (filtersJson != null)".

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

        // Apply the collapse/expand setting.
        switch (collapseExpand) {
        case NONE:
            break;
        case IRI:
            // Collapse/expand settings
            solrQuery.addFilterQuery(COLLAPSE_IRI);
            solrQuery.addField(COLLAPSE_ID + ":" + IRI);
            solrQuery.set(ExpandParams.EXPAND, true);
            // We now expand the expansion, so as to get _all_ instances
            // of this IRI, not just ones that match the top-level
            // query term and filters. We sort by vocabulary_id
            // in order to _group_ results with the same vocabulary_id
            // together.
            solrQuery.set(ExpandParams.EXPAND_Q, "*:*");
            solrQuery.set(ExpandParams.EXPAND_FQ, "*:*");
            solrQuery.set(ExpandParams.EXPAND_SORT, EXPAND_SORT_IRI);
            solrQuery.set(ExpandParams.EXPAND_ROWS, EXPAND_ROWS);
            break;
        case VOCABULARY_ID_IRI:
            // Collapse/expand settings
            solrQuery.addFilterQuery(COLLAPSE_VOCABULARY_ID_IRI);
            solrQuery.addField(COLLAPSE_ID + ":" + VOCABULARY_ID_IRI);
            solrQuery.set(ExpandParams.EXPAND, true);
            // We now expand the expansion, so as to get _all_ instances
            // of this IRI, not just ones that match the top-level
            // query term and filters.
            solrQuery.set(ExpandParams.EXPAND_Q, "*:*");
            solrQuery.set(ExpandParams.EXPAND_FQ, "*:*");
            solrQuery.set(ExpandParams.EXPAND_SORT,
                    EXPAND_SORT_VOCABULARY_ID_IRI);
            solrQuery.set(ExpandParams.EXPAND_ROWS, EXPAND_ROWS);
            break;
        default:
            // Oops!
            break;
        }
        // Always log the value of collapseExpand that we use,
        // whether or not the user provided a value for it.
        filtersAndResultsExtracted.add(Analytics.SEARCH_COLLAPSE_EXPAND_FIELD);
        filtersAndResultsExtracted.add(collapseExpand.value());

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
            if (SOLR_CLIENT_RESOURCES instanceof EmbeddedSolrServer) {
                responseQuery = SOLR_CLIENT_RESOURCES.query(solrQuery);
            } else {
                responseQuery = SOLR_CLIENT_RESOURCES.query(solrQuery,
                        SolrRequest.METHOD.POST);
            }
            if (logResults) {
                SolrDocumentList solrDocumentList =
                        responseQuery.getResults();
                List<Integer> resultIds = new ArrayList<>();
                List<String> resultOwners = new ArrayList<>();
                for (SolrDocument sd : solrDocumentList) {
                    resultIds.add(Integer.valueOf(
                            (String) sd.getFieldValue(VOCABULARY_ID)));
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

    /** Validate a language tag. Returns without throwing an exception,
     * if the tag is valid, otherwise, throws an
     * {@code IllegalArgumentException}.
     * @param language A language tag to be validated.
     * @throws IllegalArgumentException If the language tag is invalid.
     */
    private static void validateLanguage(final String language)
            throws IllegalArgumentException {
        if (!LANGUAGE_PATTERN.matcher(language).matches()) {
            throw new IllegalArgumentException("Invalid language value");
        }
    }

    /** Map a language specified in the language filter into a
     * suffix to use against multilingual fields.
     * @param language A language tag, or either of the special values of
     *      {@code NO_LANGUAGE} or {@code ALL_SUFFIX}.
     * @return The suffix to use, or an empty string, if {@code language}
     *      equals the value of {@code NO_LANGUAGE}, or {@code ALL_SUFFIX},
     *      if {@code language} equals the value of {@code ALL_SUFFIX}.
     */
    private static String mapLanguage(final String language) {
        if (NO_LANGUAGE.equals(language)) {
            return "";
        }
        if (ALL_SUFFIX.equals(language)) {
            return ALL_SUFFIX;
        }
        return "-" + language;
    }

    /** Add highlight fields for each language of interest.
     * @param solrQuery The SolrQuery to which highlight fields are
     *      to be added.
     * @param languages The list of languages to be used to add
     *      highlight fields.
     */
    private static void addMultilingualHighlightFields(
            final SolrQuery solrQuery,
            final ArrayList<String> languages) {
        for (String language : languages) {
            // Because we set HighlightParams.FIELD_MATCH to true,
            // we have to request highlighting for the _search
            // and _phrase fields.
            solrQuery.addHighlightField(SKOS_PREFLABEL
                    + SEARCH_SUFFIX + language);
            solrQuery.addHighlightField(SKOS_ALTLABEL
                    + SEARCH_SUFFIX + language);
            solrQuery.addHighlightField(SKOS_HIDDENLABEL
                    + SEARCH_SUFFIX + language);
            solrQuery.addHighlightField(SKOS_DEFINITION
                    + SEARCH_SUFFIX + language);
            solrQuery.addHighlightField(RDFS_LABEL
                    + SEARCH_SUFFIX + language);
            solrQuery.addHighlightField(DCTERMS_TITLE
                    + SEARCH_SUFFIX + language);
            solrQuery.addHighlightField(DCTERMS_DESCRIPTION
                    + SEARCH_SUFFIX + language);

            solrQuery.addHighlightField(SKOS_PREFLABEL
                    + PHRASE_SUFFIX + language);
            solrQuery.addHighlightField(SKOS_ALTLABEL
                    + PHRASE_SUFFIX + language);
            solrQuery.addHighlightField(SKOS_HIDDENLABEL
                    + PHRASE_SUFFIX + language);
            solrQuery.addHighlightField(SKOS_DEFINITION
                    + PHRASE_SUFFIX + language);
            solrQuery.addHighlightField(RDFS_LABEL
                    + PHRASE_SUFFIX + language);
            solrQuery.addHighlightField(DCTERMS_TITLE
                    + PHRASE_SUFFIX + language);
            solrQuery.addHighlightField(DCTERMS_DESCRIPTION
                    + PHRASE_SUFFIX + language);
        }
    }

    /** Set the query fields. This sets the "qf" parameter, and
     * the "pqf" parameter provided by the Safari custom query parser.
     * @param query The Solr query being constructed.
     * @param languages The list of languages to be used to select
     *      query fields.
     */
    private static void setQueryFields(
            final SolrQuery query,
            final ArrayList<String> languages) {
        // NB: ensure that the highlight fields are set above, corresponding
        // to the fields listed in the QF and PQF parameters.
        String langQueryFields = TOP_CONCEPT + " ";
        for (String language : languages) {
            langQueryFields = langQueryFields
                    + SKOS_PREFLABEL + SEARCH_SUFFIX + language + " "
                    + SKOS_ALTLABEL + SEARCH_SUFFIX + language + " "
                    + SKOS_HIDDENLABEL + SEARCH_SUFFIX + language + " "
                    + SKOS_DEFINITION + SEARCH_SUFFIX + language + " "
                    + RDFS_LABEL + SEARCH_SUFFIX + language + " "
                    + DCTERMS_TITLE + SEARCH_SUFFIX + language + " "
                    + DCTERMS_DESCRIPTION + SEARCH_SUFFIX + language + " ";
        }

        query.set(DisMaxParams.QF, langQueryFields);
        // Default (1) boosting for phrases: phrase matches should
        // be considered (much) more important than non-phrase matches.
        String langPhraseQueryFields = TOP_CONCEPT_PHRASE + " ";
        for (String language : languages) {
            langPhraseQueryFields = langPhraseQueryFields
                    + SKOS_PREFLABEL + PHRASE_SUFFIX + language + " "
                    + SKOS_ALTLABEL + PHRASE_SUFFIX + language + " "
                    + SKOS_HIDDENLABEL + PHRASE_SUFFIX + language + " "
                    + SKOS_DEFINITION + PHRASE_SUFFIX + language + " "
                    + RDFS_LABEL + PHRASE_SUFFIX + language + " "
                    + DCTERMS_TITLE + PHRASE_SUFFIX + language + " "
                    + DCTERMS_DESCRIPTION + PHRASE_SUFFIX + language + " ";
        }
        query.set(SafariQueryParser.PQF, langPhraseQueryFields);
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
