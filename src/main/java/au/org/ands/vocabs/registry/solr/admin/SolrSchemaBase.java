/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.solr.admin;

import static au.org.ands.vocabs.registry.solr.FieldConstants.ALL_SUFFIX;
import static au.org.ands.vocabs.registry.solr.FieldConstants.ALPHA_ONLY_SORT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.LOWER_EXACT_WORDS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.PHRASE_SUFFIX;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SCHEMA_VERSION;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SEARCH_SUFFIX;
import static au.org.ands.vocabs.registry.solr.FieldConstants.STRING;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TEXT_EN_SPLITTING;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.GenericSolrRequest;
import org.apache.solr.client.solrj.request.RequestWriter.StringPayloadContentWriter;
import org.apache.solr.client.solrj.request.schema.AnalyzerDefinition;
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.client.solrj.response.SimpleSolrResponse;
import org.apache.solr.client.solrj.response.schema.SchemaResponse.UpdateResponse;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;

import au.org.ands.vocabs.registry.solr.FieldConstants;

/** Parent of classes that create Solr schemas. */
public abstract class SolrSchemaBase {

    /** Get the logger for this class.
     * @return The logger for this class. */
    abstract Logger getLogger();

    /** Check that a SolrJ action completed successfully.
     * Throws a RuntimeException if there was an error.
     * @param response The response from SolrJ to be checked.
     */
    protected void checkResponse(final SolrResponse response) {
        NamedList<Object> responseList = response.getResponse();
        Object errors = responseList.get("errors");
        if (errors != null) {
            getLogger().info("Got errors: " + errors.toString());
            throw new RuntimeException("Error from SolrJ");
        }
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
    protected void setUserProperty(final SolrClient solrClient,
            final String name,
            final String value) throws SolrServerException, IOException {
        getLogger().info("Setting config property: " + name + " ... ");
        JsonObjectBuilder job1 = Json.createObjectBuilder();
        job1.add(name, value);
        JsonObjectBuilder job2 = Json.createObjectBuilder();
        job2.add("set-user-property", job1);

        String payload = job2.build().toString();

        GenericSolrRequest request = new GenericSolrRequest(
                SolrRequest.METHOD.POST, "/config", null);
        request.setContentWriter(new StringPayloadContentWriter(payload,
                CommonParams.JSON_MIME));
        SimpleSolrResponse response = request.process(solrClient);
        checkResponse(response);
        getLogger().info(" ... done");
    }

    /** Get the installed schema version.
     * This is represented as the default value of the special
     * field SCHEMA_VERSION.
     * @param solrClient The SolrClient used to access Solr.
     * @return The installed schema version. 0, if the schema
     *      version field has not yet been installed. -1, if there
     *      was an error getting the value.
     */
    protected int getInstalledSchemaVersion(final SolrClient solrClient) {
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
            return Integer.parseInt(schemaVersion);
        } catch (SolrServerException e) {
            getLogger().error("Exception:", e);
            return -1;
        } catch (IOException e) {
            getLogger().error("Exception:", e);
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
    protected void setInstalledSchemaVersion(final SolrClient solrClient,
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
    protected void addAlphaOnlySortFieldType(final SolrClient solrClient)
            throws SolrServerException, IOException {
        getLogger().info("Adding field type " + ALPHA_ONLY_SORT + " ... ");

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
        getLogger().info(" ... done");
    }

    /** Value to use for the positionIncrementGap attribute of
     * field type definitions. Instances of the field type that
     * are defined as being multivalued will get this setting.
     * As noted in the Solr documentation, this
     * is used to "prevent spurious phrase matches".
     */
    protected static final int POSITION_INCREMENT_GAP = 100;

    /** Submit a request to the Solr API to create a text field type
     * {@link FieldConstants#LOWER_EXACT_WORDS} that uses the
     * ICUTokenizerFactory and ICUFoldingFilterFactory.
     * @param solrClient The SolrClient used to access Solr.
     * @throws IOException If there is an error communicating with
     *      the Solr server.
     * @throws SolrServerException If the Solr server returns an error.
     */
    protected void addLowerFieldType(final SolrClient solrClient)
            throws SolrServerException, IOException {
        getLogger().info("Adding field type " + LOWER_EXACT_WORDS + " ... ");

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
        getLogger().info(" ... done");
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
    protected void addField(final SolrClient solrClient,
            final String fieldName,
            final String type,
            final boolean stored,
            final boolean indexed,
            final boolean multivalued) throws SolrServerException, IOException {
        getLogger().info("Adding field: " + fieldName + " ... ");
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
        getLogger().info(" ... done");
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
    protected void addField(final SolrClient solrClient,
            final String fieldName,
            final String type,
            final boolean stored,
            final boolean indexed,
            final boolean multivalued,
            final Map<String, Object> extraAttributes)
                    throws SolrServerException, IOException {
        getLogger().info("Adding field: " + fieldName + " ... ");
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
        getLogger().info(" ... done");
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
    protected void addCopyField(final SolrClient solrClient,
            final String source,
            final List<String> targets)
                    throws SolrServerException, IOException {
        getLogger().info("Adding copy field with source: " + source
                + " ... ");
        SchemaRequest.AddCopyField addCopyFieldRequest =
                new SchemaRequest.AddCopyField(source, targets);
        UpdateResponse updateResponse =
                addCopyFieldRequest.process(solrClient);
        checkResponse(updateResponse);
        getLogger().info(" ... done");
      }

    /** Submit a request to the Solr API to create a dynamic field.
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
    protected void addDynamicField(final SolrClient solrClient,
            final String fieldName,
            final String type,
            final boolean stored,
            final boolean indexed,
            final boolean multivalued)
                    throws SolrServerException, IOException {
        getLogger().info("Adding dynamic field: " + fieldName + " ... ");
        Map<String, Object> fieldAttributes = new LinkedHashMap<>();
        fieldAttributes.put("name", fieldName);
        fieldAttributes.put("type", type);
        fieldAttributes.put("stored", stored);
        fieldAttributes.put("indexed", indexed);
        if (multivalued) {
            fieldAttributes.put("multiValued", true);
        }
        SchemaRequest.AddDynamicField addDynamicFieldRequest =
                new SchemaRequest.AddDynamicField(fieldAttributes);
        UpdateResponse updateResponse =
                addDynamicFieldRequest.process(solrClient);
        checkResponse(updateResponse);
        getLogger().info(" ... done");
      }

    /** Delete a dynamic field of a collection.
     * @param solrClient The SolrClient used to access Solr.
     * @param field The name of the dynamic field to delete.
     * @throws IOException If there is an error communicating with
     *      the Solr server.
     * @throws SolrServerException If the Solr server returns an error.
     */
    protected void deleteDynamicField(final SolrClient solrClient,
            final String field)
                    throws SolrServerException, IOException {
        getLogger().info("Deleting dynamic field: " + field + " ... ");
        SchemaRequest.DeleteDynamicField deleteDynamicFieldRequest =
                new SchemaRequest.DeleteDynamicField(field);
        UpdateResponse updateResponse =
                deleteDynamicFieldRequest.process(solrClient);
        checkResponse(updateResponse);
        getLogger().info(" ... done");
      }

    /** Add fields for data that may be multilingual.
     * @param solrClient The SolrClient used to access Solr.
     * @param fieldName The name of the new field.
     * @param stored Whether the values of the field are to be stored.
     * @param indexed Whether the values of the field are to be indexed.
     * @throws IOException If there is an error communicating with
     *      the Solr server.
     * @throws SolrServerException If the Solr server returns an error.
     */
    protected void addMultilingualFields(final SolrClient solrClient,
            final String fieldName,
            final boolean stored,
            final boolean indexed)
                    throws SolrServerException, IOException {
        // e.g., skos_prefLabel (prefLabels without a language tag)
        addField(solrClient, fieldName, STRING, stored, indexed, false);
        // e.g., skos_prefLabel-en (prefLabels with a language tag)
        addDynamicField(solrClient, fieldName + "-*", STRING,
                stored, indexed, false);
        // e.g., skos_prefLabel_search (prefLabels without a language tag)
        addField(solrClient, fieldName + SEARCH_SUFFIX, TEXT_EN_SPLITTING,
                stored, indexed, false);
        // e.g., skos_prefLabel_search-* (prefLabels with a language tag)
        addDynamicField(solrClient, fieldName + SEARCH_SUFFIX + "-*",
                TEXT_EN_SPLITTING, stored, indexed, false);
        // e.g., skos_prefLabel_phrase (prefLabels without a language tag)
        addField(solrClient, fieldName + PHRASE_SUFFIX, LOWER_EXACT_WORDS,
                stored, indexed, false);
        // e.g., skos_prefLabel_phrase-en (prefLabels with a language tag)
        addDynamicField(solrClient, fieldName + PHRASE_SUFFIX + "-*",
                LOWER_EXACT_WORDS, stored, indexed, false);
        // Multivalued fields for all languages
        // e.g., skos_prefLabel_all
        //       (all prefLabels, with or without a language tag)
        addField(solrClient, fieldName + ALL_SUFFIX, STRING,
                stored, indexed, true);
        // e.g., skos_prefLabel_search_all
        //       (all prefLabels, with or without a language tag)
        addField(solrClient, fieldName + SEARCH_SUFFIX + ALL_SUFFIX,
                TEXT_EN_SPLITTING, stored, indexed, true);
        // e.g., skos_prefLabel_phrase_all
        //       (all prefLabels, with or without a language tag)
        addField(solrClient, fieldName + PHRASE_SUFFIX + ALL_SUFFIX,
                LOWER_EXACT_WORDS, stored, indexed, true);

        // Copy fields
        // e.g., skos_prefLabel -> skos_prefLabel_all,
        //           skos_prefLabel_search, skos_prefLabel_search_all,
        //           skos_prefLabel_phrase, skos_prefLabel_phrase_all
        addCopyField(solrClient, fieldName,
                Arrays.asList(fieldName + ALL_SUFFIX,
                        fieldName + SEARCH_SUFFIX,
                        fieldName + SEARCH_SUFFIX + ALL_SUFFIX,
                        fieldName + PHRASE_SUFFIX,
                        fieldName + PHRASE_SUFFIX + ALL_SUFFIX));
        // e.g., skos_prefLabel-en -> skos_prefLabel_all,
        //           skos_prefLabel_search-en, skos_prefLabel_search_all,
        //           skos_prefLabel_phrase-en, skos_prefLabel_phrase_all
        addCopyField(solrClient, fieldName + "-*",
                Arrays.asList(fieldName + ALL_SUFFIX,
                        fieldName + SEARCH_SUFFIX + "-*",
                        fieldName + SEARCH_SUFFIX + ALL_SUFFIX,
                        fieldName + PHRASE_SUFFIX + "-*",
                        fieldName + PHRASE_SUFFIX + ALL_SUFFIX));
    }

    /** Set a property of a collection.
     * @param solrClient The SolrClient used to access Solr.
     * @param name The name of the property to be set.
     * @param value The value of the property to be set.
     * @throws IOException If there is an error communicating with
     *      the Solr server.
     * @throws SolrServerException If the Solr server returns an error.
     */
    protected void setProperty(final SolrClient solrClient,
            final String name,
            final String value) throws SolrServerException, IOException {
        getLogger().info("Setting config property: " + name + " ... ");
        JsonObjectBuilder job1 = Json.createObjectBuilder();
        job1.add(name, value);
        JsonObjectBuilder job2 = Json.createObjectBuilder();
        job2.add("set-property", job1);

        String payload = job2.build().toString();

        GenericSolrRequest request = new GenericSolrRequest(
                SolrRequest.METHOD.POST, "/config", null);
        request.setContentWriter(new StringPayloadContentWriter(payload,
                CommonParams.JSON_MIME));
        SimpleSolrResponse response = request.process(solrClient);
        checkResponse(response);
        getLogger().info(" ... done");
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

    /** Reload the Solr collection.
     * @param baseURL The base URL of Solr.
     * @param collectionName The name of the Solr collection.
     * @throws SolrServerException If a SolrServerException was generated
     *      by the SolrJ API.
     * @throws IOException If an IOException was generated by the SolrJ API.
     */
    protected void reloadCollection(final String baseURL,
            final String collectionName)
            throws SolrServerException, IOException {
        try (SolrClient client = new HttpSolrClient.Builder(baseURL).build()) {
            CollectionAdminResponse caResponse;
            CollectionAdminRequest.Reload reloadCollectionRequest =
                    CollectionAdminRequest.reloadCollection(
                            collectionName);

            caResponse = reloadCollectionRequest.process(client);
            checkResponse(caResponse);
            getLogger().info("reload status:" + caResponse.getStatus());
            getLogger().info("reload isSuccess:" + caResponse.isSuccess());
        } catch (SolrServerException sse) {
            getLogger().error("Got a SolrServerException:", sse);
            throw sse;
        } catch (IOException ioe) {
            getLogger().error("Got an IOException:", ioe);
            throw ioe;
        }
    }

}
