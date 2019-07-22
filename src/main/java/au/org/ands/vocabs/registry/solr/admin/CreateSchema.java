/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.solr.admin;

import static au.org.ands.vocabs.registry.solr.FieldConstants.ACCESS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.ACRONYM;
import static au.org.ands.vocabs.registry.solr.FieldConstants.ALPHA_ONLY_SORT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.BOOLEAN;
import static au.org.ands.vocabs.registry.solr.FieldConstants.CONCEPT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.CONCEPT_SEARCH;
import static au.org.ands.vocabs.registry.solr.FieldConstants.DATE_POINT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.DESCRIPTION;
import static au.org.ands.vocabs.registry.solr.FieldConstants.DESCRIPTION_PHRASE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.FORMAT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.FULLTEXT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.LANGUAGE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.LAST_UPDATED;
import static au.org.ands.vocabs.registry.solr.FieldConstants.LICENCE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.LOWER_EXACT_WORDS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.OWNER;
import static au.org.ands.vocabs.registry.solr.FieldConstants.POOLPARTY_ID;
import static au.org.ands.vocabs.registry.solr.FieldConstants.PUBLISHER;
import static au.org.ands.vocabs.registry.solr.FieldConstants.PUBLISHER_SEARCH;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SCHEMA_VERSION;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SISSVOC_ENDPOINT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SLUG;
import static au.org.ands.vocabs.registry.solr.FieldConstants.STATUS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.STRING;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SUBJECT_IRIS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SUBJECT_LABELS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SUBJECT_NOTATIONS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SUBJECT_SEARCH;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SUBJECT_SOURCES;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TEXT_EN_SPLITTING;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TITLE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TITLE_PHRASE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TITLE_SEARCH;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TITLE_SORT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TOP_CONCEPT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.WIDGETABLE;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.GenericSolrRequest;
import org.apache.solr.client.solrj.request.RequestWriter.StringPayloadContentWriter;
import org.apache.solr.client.solrj.request.schema.AnalyzerDefinition;
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.client.solrj.response.SimpleSolrResponse;
import org.apache.solr.client.solrj.response.SolrResponseBase;
import org.apache.solr.client.solrj.response.schema.SchemaResponse.UpdateResponse;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.solr.FieldConstants;

/** Create the Solr Schema for the registry.
 * Run as:
 * java au.org.ands.vocabs.registry.solr.admin.CreateSchema
 *  http://localhost:8983/solr/vocabs-registry
 */
public final class CreateSchema {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Value to use for the positionIncrementGap attribute of
     * field type definitions. Instances of the field type that
     * are defined as being multivalued will get this setting.
     * As noted in the Solr documentation, this
     * is used to "prevent spurious phrase matches".
     */
    private static final int POSITION_INCREMENT_GAP = 100;

    /** Check that a SolrJ action completed successfully.
     * Throws a RuntimeException if there was an error.
     * @param response The response from SolrJ to be checked.
     */
    private void checkResponse(final SolrResponseBase response) {
        NamedList<Object> responseList = response.getResponse();
        Object errors = responseList.get("errors");
        if (errors != null) {
            logger.info("Got errors: " + errors.toString());
            throw new RuntimeException("Error from SolrJ");
        }

    }

    /** Get the installed schema version.
     * This is represented as the default value of the special
     * field SCHEMA_VERSION.
     * @param solrClient The SolrClient used to access Solr.
     * @return The installed schema version. 0, if the schema
     *      version field has not yet been installed. -1, if there
     *      was an error getting the value.
     */
    private int getInstalledSchemaVersion(final SolrClient solrClient) {
        if (!(solrClient instanceof HttpSolrClient)) {
            // Embedded Solr doesn't support getting properties the way
            // we do. So just return 0;
            return 0;
        }
        try {
            String schemaVersion = getUserProperty(solrClient, SCHEMA_VERSION);
            if (schemaVersion == null) {
                // No such property. OK, return 0.
                return 0;
            }
            return Integer.parseInt((schemaVersion));
        } catch (SolrServerException e) {
            logger.error("Exception:", e);
            return -1;
        } catch (IOException e) {
            logger.error("Exception:", e);
            return -1;
        }
    }

    /** Set the installed schema version.
     * @param solrClient The SolrClient used to access Solr.
     * @param schemaVersion The schema version value to be set.
     * @throws IOException If there is an error communicating with
     *      the Solr server.
     * @throws SolrServerException If the Solr server returns an error.
     */
    private void setInstalledSchemaVersion(final SolrClient solrClient,
            final int schemaVersion)
                    throws SolrServerException, IOException {
        if (!(solrClient instanceof HttpSolrClient)) {
            // Embedded Solr doesn't support getting properties the way
            // we do. So just return.
            return;
        }
        setUserProperty(solrClient, SCHEMA_VERSION,
                Integer.toString(schemaVersion));
    }


    /** Submit a request to the Solr API to create a field type
     * "alphaOnlySort" as per the definition given in the sample
     * schemas in the Solr distribution.
     * @param solrClient The SolrClient used to access Solr.
     * @throws IOException If there is an error communicating with
     *      the Solr server.
     * @throws SolrServerException If the Solr server returns an error.
     */
    private void addAlphaOnlySortFieldType(final SolrClient solrClient)
            throws SolrServerException, IOException {
        logger.info("Adding field type " + ALPHA_ONLY_SORT + " ... ");

        /*
        <!-- This is an example of using the KeywordTokenizer along
             With various TokenFilterFactories to produce a sortable field
             that does not include some properties of the source text
             -->
        <fieldType name="alphaOnlySort" class="solr.TextField"
                    sortMissingLast="true" omitNorms="true">
           <analyzer>
             <!-- KeywordTokenizer does no actual tokenizing, so the entire
                  input string is preserved as a single token
                  -->
             <tokenizer class="solr.KeywordTokenizerFactory"/>
             <!-- The LowerCase TokenFilter does what you expect, which can be
                  when you want your sorting to be case insensitive
                  -->
             <filter class="solr.LowerCaseFilterFactory"/>
             <!-- The TrimFilter removes any leading or trailing whitespace -->
             <filter class="solr.TrimFilterFactory"/>
             <!-- The PatternReplaceFilter gives you the flexibility to use
                  Java Regular expression to replace any sequence of characters
                  matching a pattern with an arbitrary replacement string,
                  which may include back references to portions of the original
                  string matched by the pattern.

                  See the Java Regular Expression documentation for more
                  information on pattern and replacement string syntax.

                  http://java.sun.com/j2se/1.6.0/docs/api/java/util/regex/
                         package-summary.html
                  -->
             <filter class="solr.PatternReplaceFilterFactory"
                     pattern="([^a-z])" replacement="" replace="all"/>
           </analyzer>
        </fieldType>
        */

        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        Map<String, Object> fieldTypeAttributes = new LinkedHashMap<>();
        // Top-level attributes of the field type:
        //   name="alphaOnlySort" class="solr.TextField"
        //   sortMissingLast="true" omitNorms="true"
        fieldTypeAttributes.put("name", ALPHA_ONLY_SORT);
        fieldTypeAttributes.put("class", "solr.TextField");
        fieldTypeAttributes.put("sortMissingLast", true);
        fieldTypeAttributes.put("omitNorms", true);
        fieldTypeDefinition.setAttributes(fieldTypeAttributes);
        AnalyzerDefinition analyzerDefinition = new AnalyzerDefinition();
        // Tokenizer: <tokenizer class="solr.KeywordTokenizerFactory"/>
        Map<String, Object> tokenizerAttributes = new LinkedHashMap<>();
        tokenizerAttributes.put("class", "solr.KeywordTokenizerFactory");
        analyzerDefinition.setTokenizer(tokenizerAttributes);
        // Filters:
        List<Map<String, Object>> filters = new ArrayList<>();
        // <!-- The LowerCase TokenFilter does what you expect, which can be
        // when you want your sorting to be case insensitive -->
        Map<String, Object> filter1 = new LinkedHashMap<>();
        filter1.put("class", "solr.LowerCaseFilterFactory");
        filters.add(filter1);
        // <!-- The TrimFilter removes any leading or trailing whitespace -->
        Map<String, Object> filter2 = new LinkedHashMap<>();
        filter2.put("class", "solr.TrimFilterFactory");
        filters.add(filter2);
        // <!-- The PatternReplaceFilter gives you the flexibility to use
        // Java Regular expression to replace any sequence of Characters ...
        Map<String, Object> filter3 = new LinkedHashMap<>();
        filter3.put("class", "solr.PatternReplaceFilterFactory");
        filter3.put("pattern", "([^a-z])");
        filter3.put("replacement", "");
        filter3.put("replace", "all");
        filters.add(filter3);
        analyzerDefinition.setFilters(filters);
        // Same analyzer for indexing and querying.
        fieldTypeDefinition.setIndexAnalyzer(analyzerDefinition);
        fieldTypeDefinition.setQueryAnalyzer(analyzerDefinition);
        SchemaRequest.AddFieldType addFieldTypeRequest =
                new SchemaRequest.AddFieldType(fieldTypeDefinition);
        UpdateResponse updateResponse =
                addFieldTypeRequest.process(solrClient);
        checkResponse(updateResponse);
        logger.info(" ... done");
    }

    /** Submit a request to the Solr API to create a text field type
     * {@link FieldConstants#LOWER_EXACT_WORDS} that uses the
     * ICUTokenizerFactory and ICUFoldingFilterFactory.
     * @param solrClient The SolrClient used to access Solr.
     * @throws IOException If there is an error communicating with
     *      the Solr server.
     * @throws SolrServerException If the Solr server returns an error.
     */
    private void addLowerFieldType(final SolrClient solrClient)
            throws SolrServerException, IOException {
        logger.info("Adding field type " + LOWER_EXACT_WORDS + " ... ");

        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        Map<String, Object> fieldTypeAttributes = new LinkedHashMap<>();
        // Top-level attributes of the field type:
        //   name="lower" class="solr.TextField" positionIncrementGap="100"
        fieldTypeAttributes.put("name", LOWER_EXACT_WORDS);
        fieldTypeAttributes.put("class", "solr.TextField");
        fieldTypeAttributes.put("positionIncrementGap", POSITION_INCREMENT_GAP);
        fieldTypeDefinition.setAttributes(fieldTypeAttributes);
        AnalyzerDefinition analyzerDefinition = new AnalyzerDefinition();
        // Tokenizer: <tokenizer class="solr.ICUTokenizerFactory"/>
        Map<String, Object> tokenizerAttributes = new LinkedHashMap<>();
        tokenizerAttributes.put("class", "solr.ICUTokenizerFactory");
        analyzerDefinition.setTokenizer(tokenizerAttributes);
        // Filters:
        List<Map<String, Object>> filters = new ArrayList<>();
        //  <filter class="solr.ICUFoldingFilterFactory"/>
        // The ICUFoldingFilter is a Unicode-friendly lower-case filter,
        // for when you want your sorting to be case insensitive.
        Map<String, Object> filter1 = new LinkedHashMap<>();
        filter1.put("class", "solr.ICUFoldingFilterFactory");
        filters.add(filter1);
        analyzerDefinition.setFilters(filters);
        // Same analyzer for indexing and querying.
        fieldTypeDefinition.setIndexAnalyzer(analyzerDefinition);
        fieldTypeDefinition.setQueryAnalyzer(analyzerDefinition);
        SchemaRequest.AddFieldType addFieldTypeRequest =
                new SchemaRequest.AddFieldType(fieldTypeDefinition);
        UpdateResponse updateResponse =
                addFieldTypeRequest.process(solrClient);
        checkResponse(updateResponse);
        logger.info(" ... done");
    }

    /** Submit a request to the Solr API to create a field.
     * This version of the method accepts {@code stored}, {@code indexed},
     * and {@code multivalued} parameters.
     * @param solrClient The SolrClient used to access Solr.
     * @param fieldName The name of the new field.
     * @param type The type of the new field.
     * @param stored Whether the values of the field are to be stored.
     * @param indexed Whether the values of the field are to be indexed.
     * @param multivalued Whether the field is multivalued.
     * @throws IOException If there is an error communicating with
     *      the Solr server.
     * @throws SolrServerException If the Solr server returns an error.
     */
    private void addField(final SolrClient solrClient,
            final String fieldName,
            final String type,
            final boolean stored,
            final boolean indexed,
            final boolean multivalued) throws SolrServerException, IOException {
        logger.info("Adding field: " + fieldName + " ... ");
        Map<String, Object> fieldAttributes = new LinkedHashMap<>();
        fieldAttributes.put("name", fieldName);
        fieldAttributes.put("type", type);
        fieldAttributes.put("stored", stored);
        fieldAttributes.put("indexed", indexed);
        if (multivalued) {
            fieldAttributes.put("multiValued", true);
        }

        SchemaRequest.AddField addFieldRequest =
                new SchemaRequest.AddField(fieldAttributes);
        UpdateResponse updateResponse =
                addFieldRequest.process(solrClient);
        checkResponse(updateResponse);
        logger.info(" ... done");
      }

    /** Submit a request to the Solr API to create a field.
     * This version of the method accepts {@code stored}, {@code indexed},
     * and {@code multivalued} parameters, as well as a parameter
     * {@code extraAttributes} containing additional field attributes.
     * @param solrClient The SolrClient used to access Solr.
     * @param fieldName The name of the new field.
     * @param type The type of the new field.
     * @param stored Whether the values of the field are to be stored.
     * @param indexed Whether the values of the field are to be indexed.
     * @param multivalued Whether the field is multivalued.
     * @param extraAttributes Additional field attributes.
     * @throws IOException If there is an error communicating with
     *      the Solr server.
     * @throws SolrServerException If the Solr server returns an error.
     */
    private void addField(final SolrClient solrClient,
            final String fieldName,
            final String type,
            final boolean stored,
            final boolean indexed,
            final boolean multivalued,
            final Map<String, Object> extraAttributes)
                    throws SolrServerException, IOException {
        logger.info("Adding field: " + fieldName + " ... ");
        Map<String, Object> fieldAttributes = new LinkedHashMap<>();
        fieldAttributes.put("name", fieldName);
        fieldAttributes.put("type", type);
        fieldAttributes.put("stored", stored);
        fieldAttributes.put("indexed", indexed);
        if (multivalued) {
            fieldAttributes.put("multiValued", true);
        }
        fieldAttributes.putAll(extraAttributes);

        SchemaRequest.AddField addFieldRequest =
                new SchemaRequest.AddField(fieldAttributes);
        UpdateResponse updateResponse =
                addFieldRequest.process(solrClient);
        checkResponse(updateResponse);
        logger.info(" ... done");
      }

    /** Add a copy field to a collection.
     * @param solrClient The SolrClient used to access Solr.
     * @param source The name of the source field from which values are
     *      to be copied.
     * @param targets The list of target fields into which the source field
     *      is to be copied.
     * @throws IOException If there is an error communicating with
     *      the Solr server.
     * @throws SolrServerException If the Solr server returns an error.
     */
    private void addCopyField(final SolrClient solrClient,
            final String source,
            final List<String> targets)
                    throws SolrServerException, IOException {
        logger.info("Adding copy field with source: " + source
                + " ... ");
        SchemaRequest.AddCopyField addCopyFieldRequest =
                new SchemaRequest.AddCopyField(source, targets);
        UpdateResponse updateResponse =
                addCopyFieldRequest.process(solrClient);
        checkResponse(updateResponse);
        logger.info(" ... done");
      }

    /** Delete a dynamic field of a collection.
     * @param solrClient The SolrClient used to access Solr.
     * @param field The name of the dynamic field to delete.
     * @throws IOException If there is an error communicating with
     *      the Solr server.
     * @throws SolrServerException If the Solr server returns an error.
     */
    private void deleteDynamicField(final SolrClient solrClient,
            final String field)
                    throws SolrServerException, IOException {
        logger.info("Deleting dynamic field: " + field + " ... ");
        SchemaRequest.DeleteDynamicField deleteCopyFieldRequest =
                new SchemaRequest.DeleteDynamicField(field);
        UpdateResponse updateResponse =
                deleteCopyFieldRequest.process(solrClient);
        checkResponse(updateResponse);
        logger.info(" ... done");
      }

    /** Set a property of a collection.
     * @param solrClient The SolrClient used to access Solr.
     * @param name The name of the property to be set.
     * @param value The value of the property to be set.
     * @throws IOException If there is an error communicating with
     *      the Solr server.
     * @throws SolrServerException If the Solr server returns an error.
     */
    private void setProperty(final SolrClient solrClient,
            final String name,
            final String value) throws SolrServerException, IOException {
        logger.info("Setting config property: " + name + " ... ");
        JsonObjectBuilder job1 = Json.createObjectBuilder();
        job1.add(name, value);
        JsonObjectBuilder job2 = Json.createObjectBuilder();
        job2.add("set-property", job1);

        String payload = job2.build().toString();

        GenericSolrRequest request = new GenericSolrRequest(
                SolrRequest.METHOD.POST, "/config", null);
        request.setContentWriter(new StringPayloadContentWriter(payload,
                CommonParams.JSON_MIME));
        request.process(solrClient);
        logger.info(" ... done");
    }

    /** Get the value of a user property of a collection.
     * @param solrClient The SolrClient used to access Solr.
     * @param name The name of the property to be set.
     * @return The value of the user property.
     * @throws IOException If there is an error communicating with
     *      the Solr server.
     * @throws SolrServerException If the Solr server returns an error.
     */
    private String getUserProperty(final SolrClient solrClient,
            final String name) throws SolrServerException, IOException {
        GenericSolrRequest request = new GenericSolrRequest(
                SolrRequest.METHOD.GET, "/config/overlay", null);
        SimpleSolrResponse response = request.process(solrClient);
        // Unfortunately, SolrJ doesn't help here, so we need to do a cast.
        @SuppressWarnings("unchecked")
        Map<String, Map<String, String>> map =
                (Map<String, Map<String, String>>)
                response.getResponse().get("overlay");
        if (map == null || !map.containsKey("userProps")) {
            return null;
        }
        return map.get("userProps").get(name);
    }

    /** Set a user property of a collection.
     * @param solrClient The SolrClient used to access Solr.
     * @param name The name of the property to be set.
     * @param value The value of the property to be set.
     * @throws IOException If there is an error communicating with
     *      the Solr server.
     * @throws SolrServerException If the Solr server returns an error.
     */
    private void setUserProperty(final SolrClient solrClient,
            final String name,
            final String value) throws SolrServerException, IOException {
        logger.info("Setting config property: " + name + " ... ");
        JsonObjectBuilder job1 = Json.createObjectBuilder();
        job1.add(name, value);
        JsonObjectBuilder job2 = Json.createObjectBuilder();
        job2.add("set-user-property", job1);

        String payload = job2.build().toString();

        GenericSolrRequest request = new GenericSolrRequest(
                SolrRequest.METHOD.POST, "/config", null);
        request.setContentWriter(new StringPayloadContentWriter(payload,
                CommonParams.JSON_MIME));
        request.process(solrClient);
        logger.info(" ... done");
    }

    /*
    private static void createField(final SolrClient solrClient,
            final String fieldName,
            final Object...otherParameters) throws Exception {
        int otherParametersLength = otherParameters.length;
        if (otherParametersLength % 2 != 0) {
            System.err.println("For fieldName = " + fieldName
                    + ", otherParameters was not a list "
                    + "of pairs of keys/values");
                    return;
        }

        Map<String, Object> fieldAttributes = new LinkedHashMap<>();
        fieldAttributes.put("name", fieldName);

        for (int i = 0; i < otherParametersLength; i = i + 2) {
            if (!(otherParameters[i] instanceof String)) {
                System.err.println("For fieldName = " + fieldName
                        + ", otherParameters has a key "
                        + "that is not a String: " + otherParameters[i]);
                return;
            }
            fieldAttributes.put((String) otherParameters[i],
                    otherParameters[i + 1]);
        }

        SchemaRequest.AddField addFieldRequest =
                new SchemaRequest.AddField(fieldAttributes);
        addFieldRequest.process(solrClient);
      }
    */

    /** Install schema version 2.
     * @param client The SolrClient into which to install the schema.
     * @throws SolrServerException If a SolrServerException was generated
     *      by the SolrJ API.
     * @throws IOException If an IOException was generated by the SolrJ API.
     */
    private void installSchema2(final SolrClient client)
            throws SolrServerException, IOException {
        try {
            setInstalledSchemaVersion(client, 2);
            addAlphaOnlySortFieldType(client);
            addLowerFieldType(client);

            // Additional field attributes for fields that hold
            // large content, i.e., fields that hold concepts,
            // as recommended to improve highlighting performance
            // for the "unified" highlight method:
            // https://lucene.apache.org/solr/guide/8_1/highlighting.html
            //         #schema-options-and-performance-considerations
            Map<String, Object> extraAttributesForConcepts =
                    new HashMap<>();
            extraAttributesForConcepts.put("storeOffsetsWithPositions", true);
            // The advice suggests that we should also do this:
            //   extraAttributesForConcepts.put("termVectors", true);
            // but there seems to be a defect in Solr, so that Solr
            // has an exception during highlighting. See:
            // http://mail-archives.apache.org/mod_mbox/lucene-solr-user/
            //        201907.mbox/%3cDCFD018B-87D1-4B01-8D99-4B8775EC46F2
            //        @ardc.edu.au%3e

            // Data about the Registry's storage of the vocabulary.
            // For example, the timestamp of the last update.
            addField(client, LAST_UPDATED, DATE_POINT, true, true, false);

            // Basic fields that are stored, and not multiValued.
            addField(client, SLUG, STRING, true, true, false);
            addField(client, TITLE, STRING, true, true, false);
            addField(client, DESCRIPTION, TEXT_EN_SPLITTING,
                    true, true, false);
            addField(client, LICENCE, STRING, true, true, false);
            addField(client, POOLPARTY_ID, STRING, true, true, false);
            addField(client, OWNER, STRING, true, true, false);
            addField(client, ACRONYM, STRING, true, true, false);
            addField(client, STATUS, STRING, true, true, false);
            addField(client, SISSVOC_ENDPOINT, STRING,
                    true, true, false);
            addField(client, WIDGETABLE, BOOLEAN, true, true, false);

            // Basic fields that are stored, and multiValued.
            addField(client, TOP_CONCEPT, TEXT_EN_SPLITTING,
                    true, true, true);
            addField(client, LANGUAGE, STRING, true, true, true);
            addField(client, CONCEPT, STRING, true, true, true,
                    extraAttributesForConcepts);
            addField(client, PUBLISHER, STRING, true, true, true);
            addField(client, ACCESS, STRING, true, true, true);
            addField(client, FORMAT, STRING, true, true, true);
            addField(client, SUBJECT_SOURCES, STRING, true, true, true);
            addField(client, SUBJECT_LABELS, STRING, true, true, true);
            addField(client, SUBJECT_NOTATIONS, STRING, true, true, true);
            addField(client, SUBJECT_IRIS, STRING, true, true, true);

            // Fields with custom type alphaOnlySort, used only for
            // sorting results.
            addField(client, TITLE_SORT, ALPHA_ONLY_SORT, true, true, false);

            // Fields that are used for searching, that are "analysed".
            // They are stored, so that we get highlighting for them.
            addField(client, CONCEPT_SEARCH, TEXT_EN_SPLITTING,
                    true, true, true, extraAttributesForConcepts);
            addField(client, TITLE_SEARCH, TEXT_EN_SPLITTING,
                    true, true, false);
            addField(client, SUBJECT_SEARCH, TEXT_EN_SPLITTING,
                    true, true, true);
            addField(client, PUBLISHER_SEARCH, TEXT_EN_SPLITTING,
                    true, true, true);
            // The fulltext field is not stored.
            addField(client, FULLTEXT, TEXT_EN_SPLITTING,
                    false, true, true);

            // Phrase fields have stored = true, indexed = true.
            addField(client, TITLE_PHRASE, LOWER_EXACT_WORDS,
                    true, true, false);
            addField(client, DESCRIPTION_PHRASE, LOWER_EXACT_WORDS,
                    true, true, false);

            addCopyField(client, "*", Arrays.asList(FULLTEXT));
            addCopyField(client, FieldConstants.TITLE,
                    Arrays.asList(TITLE_SEARCH, TITLE_SORT, TITLE_PHRASE));
            addCopyField(client, CONCEPT, Arrays.asList(CONCEPT_SEARCH));
            addCopyField(client, TOP_CONCEPT,
                    Arrays.asList(SUBJECT_SEARCH));
            addCopyField(client, SUBJECT_LABELS,
                    Arrays.asList(SUBJECT_SEARCH));
            addCopyField(client, SUBJECT_NOTATIONS,
                    Arrays.asList(SUBJECT_SEARCH));
            addCopyField(client, PUBLISHER,
                    Arrays.asList(PUBLISHER_SEARCH));

            addCopyField(client, DESCRIPTION,
                    Arrays.asList(DESCRIPTION_PHRASE));

            /** Part of the data-driven schema, but we don't use it. */
            deleteDynamicField(client, "*_point");

            if (client instanceof HttpSolrClient) {
                // Do this for production. It doesn't seem to work well
                // when done with the embedded Solr used for automated
                // testing.
                setProperty(client, "updateHandler.autoSoftCommit.maxTime",
                        "10000");
            }
        } catch (SolrServerException sse) {
            logger.error("Got a SolrServerException:", sse);
            throw sse;
        } catch (IOException ioe) {
            logger.error("Got an IOException:", ioe);
            throw ioe;
        } catch (RuntimeException re) {
            logger.error("Got an error from SolrJ, so not proceeding. "
                    + "Most likely cause: schema already installed.");
            throw re;
        }
    }

    /** Timeout to use when connecting to ZooKeeper, in milliseconds. */
    private static final int ZK_TIMEOUT = 30000;

    /** The base filename of solrconfig.xml. */
    private static final String SOLRCONFIG_XML = "solrconfig.xml";

    /** Upload our custom {@code solrconfig.xml} to ZooKeeper.
     * @param collectionName The name of the Solr collection.
     * @param zkHost The value of zkHost to use to connect to ZooKeeper.
     * @throws Exception If the ZooKeeper upload could not be completed.
     */
    private void uploadSolrconfig(final String collectionName,
            final String zkHost) throws Exception {
        try (SolrZkClient zkClient = new SolrZkClient(zkHost, ZK_TIMEOUT)) {
          logger.info("Connecting to ZooKeeper at " + zkHost + " ...");

          String fullPath = "conf/" + SOLRCONFIG_XML;

          String src = fullPath;
          String dst = "configs/" + collectionName + "/" + SOLRCONFIG_XML;
          Boolean recurse = false;
          logger.info("Copying from '" + src + "' to '" + dst
                  + "'. ZooKeeper at " + zkHost);

          boolean srcIsZk = false;
          boolean dstIsZk = true;

          String srcName = src;
          String dstName = dst;
          zkClient.zkTransfer(srcName, srcIsZk, dstName, dstIsZk, recurse);
        } catch (Exception e) {
          logger.error("Could not complete the zk operation for reason: "
                  + e.getMessage());
          throw (e);
        }
    }

    /** Reload the Solr collection.
     * @param baseURL The base URL of Solr.
     * @param collectionName The name of the Solr collection.
     * @throws SolrServerException If a SolrServerException was generated
     *      by the SolrJ API.
     * @throws IOException If an IOException was generated by the SolrJ API.
     */
    private void reloadCollection(final String baseURL,
            final String collectionName)
            throws SolrServerException, IOException {
        try (SolrClient client = new HttpSolrClient.Builder(baseURL).build()) {
            CollectionAdminResponse caResponse;
            CollectionAdminRequest.Reload reloadCollectionRequest =
                    CollectionAdminRequest.reloadCollection(
                            collectionName);

            caResponse = reloadCollectionRequest.process(client);
            System.out.println("reload status:" + caResponse.getStatus());
            System.out.println("reload isSuccess:" + caResponse.isSuccess());
        } catch (SolrServerException sse) {
            logger.error("Got a SolrServerException:", sse);
            throw sse;
        } catch (IOException ioe) {
            logger.error("Got an IOException:", ioe);
            throw ioe;
        }
    }

    /** Install the schema.
     * @param baseURL The base URL of Solr.
     * @param client The SolrClient into which to install the schema.
     *  This can be an instance of {@link EmbeddedSolrServer},
     *  in which case, the value of the other three parameters may be
     *  {@code null}.
     * @param collectionName The name of the Solr collection.
     * @param zkHost The value of zkHost to use to connect to ZooKeeper.
     * @throws SolrServerException If a SolrServerException was generated
     *      by the SolrJ API.
     * @throws IOException If an IOException was generated by the SolrJ API.
     */
    public void installSchema(final String baseURL,
            final SolrClient client,
            final String collectionName,
            final String zkHost)
            throws SolrServerException, IOException {
        // Of course, this will become more sophisticated, once there
        // is a revision of the schema.
        try {
            int installedSchemaVersion = getInstalledSchemaVersion(client);
            if (installedSchemaVersion < 0) {
                logger.error("Got an error getting the schema version. "
                        + "Not proceeding.");
                return;
            }
            logger.info("Installed schema version is: "
                    + installedSchemaVersion);
            if (installedSchemaVersion < 1) {
                if (!(client instanceof EmbeddedSolrServer)) {
                    uploadSolrconfig(collectionName, zkHost);
                    reloadCollection(baseURL, collectionName);
                }
                installSchema2(client);
            }
            // Don't want to have to wait. This is most useful when
            // running the embedded Solr instance used for the test suite.
            client.commit();
        } catch (Exception e) {
            // Most likely, already installed.
            logger.error("Runtime exception: ", e);
        }
    }

    /** Create the schema.
     * @param args The command-line parameters:
     * <ol>
     *   <li>The base URL of Solr</li>
     *   <li>the collection name</li>
     *   <li>the URL of the Solr collection</li>
     *   <li>the zkHost.</li>
     * </ol>
     */
    // Magic number errors generated for argument positions 3 and 4.
    @SuppressWarnings("checkstyle:MagicNumber")
    public static void main(final String[] args) {
        CreateSchema createSchema = new CreateSchema();
        if (args.length != 4) {
            createSchema.logger.error("Must provide four command-line "
                    + "parameters");
            return;
        }

        String baseURL = args[0];
        String collectionName = args[1];
        String apiURL = args[2];
        String zkHost = args[3];

        try (SolrClient client = new HttpSolrClient.Builder(apiURL).build()) {
            createSchema.installSchema(
                    baseURL, client, collectionName, zkHost);
        } catch (SolrServerException sse) {
            createSchema.logger.info("Got a SolrServerException:");
            sse.printStackTrace();
        } catch (IOException ioe) {
            createSchema.logger.info("Got an IOException:");
            ioe.printStackTrace();
        }
    }

}
