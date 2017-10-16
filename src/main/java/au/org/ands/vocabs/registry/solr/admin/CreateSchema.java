/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.solr.admin;

import static au.org.ands.vocabs.registry.solr.FieldConstants.ACCESS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.ACRONYM;
import static au.org.ands.vocabs.registry.solr.FieldConstants.ALPHA_ONLY_SORT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.BOOLEAN;
import static au.org.ands.vocabs.registry.solr.FieldConstants.CONCEPT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.CONCEPT_SEARCH;
import static au.org.ands.vocabs.registry.solr.FieldConstants.DESCRIPTION;
import static au.org.ands.vocabs.registry.solr.FieldConstants.FORMAT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.FULLTEXT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.LANGUAGE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.LICENCE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.OWNER;
import static au.org.ands.vocabs.registry.solr.FieldConstants.POOLPARTY_ID;
import static au.org.ands.vocabs.registry.solr.FieldConstants.PUBLISHER;
import static au.org.ands.vocabs.registry.solr.FieldConstants.PUBLISHER_SEARCH;
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
import static au.org.ands.vocabs.registry.solr.FieldConstants.TITLE_SEARCH;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TITLE_SORT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TOP_CONCEPT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.WIDGETABLE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.GenericSolrRequest;
import org.apache.solr.client.solrj.request.schema.AnalyzerDefinition;
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.schema.SchemaResponse.UpdateResponse;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;

import au.org.ands.vocabs.registry.solr.FieldConstants;

/** Create the Solr Schema for the registry.
 * Run as:
 * java au.org.ands.vocabs.registry.solr.admin.CreateSchema
 *  http://localhost:8983/solr/vocabs-registry
 */
public final class CreateSchema {

    /** Private constructor for a utility class. */
    private CreateSchema() {
    }

    /** Submit a request to the Solr API to create a field type
     * "alphaOnlySort" as per the definition given in the sample
     * schemas in the Solr distribution.
     * @param solrClient The SolrClient used to access Solr.
     * @throws IOException If there is an error communicating with
     *      the Solr server.
     * @throws SolrServerException If the Solr server returns an error.
     */
    private static void addAlphaOnlySortFieldType(final SolrClient solrClient)
            throws SolrServerException, IOException {
        System.out.print("Adding field type " + ALPHA_ONLY_SORT + " ... ");

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
        System.out.print("status = " + updateResponse.getStatus());
        System.out.print(updateResponse.getResponse().toString());
        System.out.println(" ... done");
    }

    /** Submit a request to the Solr API to create a field.
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
    private static void addField(final SolrClient solrClient,
            final String fieldName,
            final String type,
            final boolean stored,
            final boolean indexed,
            final boolean multivalued) throws SolrServerException, IOException {
        System.out.print("Adding field: " + fieldName + " ... ");
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
        System.out.print("status = " + updateResponse.getStatus());
        System.out.print(updateResponse.getResponse().toString());
        System.out.println(" ... done");
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
    private static void addCopyField(final SolrClient solrClient,
            final String source,
            final List<String> targets)
                    throws SolrServerException, IOException {
        System.out.print("Adding copy field with source: " + source
                + " ... ");
        SchemaRequest.AddCopyField addCopyFieldRequest =
                new SchemaRequest.AddCopyField(source, targets);
        UpdateResponse updateResponse =
                addCopyFieldRequest.process(solrClient);
        System.out.print("status = " + updateResponse.getStatus());
        System.out.print(updateResponse.getResponse().toString());
        System.out.println(" ... done");
      }

    /** Delete a dynamic field of a collection.
     * @param solrClient The SolrClient used to access Solr.
     * @param field The name of the dynamic field to delete.
     * @throws IOException If there is an error communicating with
     *      the Solr server.
     * @throws SolrServerException If the Solr server returns an error.
     */
    private static void deleteDynamicField(final SolrClient solrClient,
            final String field)
                    throws SolrServerException, IOException {
        System.out.print("Deleting dynamic field: " + field + " ... ");
        SchemaRequest.DeleteDynamicField addCopyFieldRequest =
                new SchemaRequest.DeleteDynamicField(field);
        addCopyFieldRequest.process(solrClient);
        System.out.println(" ... done");
      }

    /** Set a property of a collection.
     * @param solrClient The SolrClient used to access Solr.
     * @param name The name of the property to be set.
     * @param value The value of the property to be set.
     * @throws IOException If there is an error communicating with
     *      the Solr server.
     * @throws SolrServerException If the Solr server returns an error.
     */
    private static void setProperty(final SolrClient solrClient,
            final String name,
            final String value) throws SolrServerException, IOException {
        System.out.print("Setting config property: " + name + " ... ");
        JsonObjectBuilder job1 = Json.createObjectBuilder();
        job1.add(name, value);
        JsonObjectBuilder job2 = Json.createObjectBuilder();
        job2.add("set-property", job1);

        ContentStreamBase.StringStream stringStream = new
                ContentStreamBase.StringStream(job2.build().toString());
        Collection<ContentStream> contentStreams = Collections.<ContentStream>
            singletonList(stringStream);

        GenericSolrRequest request = new GenericSolrRequest(
                SolrRequest.METHOD.POST, "/config", null);
        request.setContentStreams(contentStreams);
        request.process(solrClient);
        System.out.println(" ... done");
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

    /** Create the schema.
     *
     * @param args The command-line parameters.
     */
    public static void main(final String[] args) {
        String apiURL;
        if (args.length != 1) {
            System.out.println("Must provide one command-line parameter");
            return;
        }
        apiURL = args[0];

        try (SolrClient client = new HttpSolrClient.Builder(apiURL).build()) {
            addAlphaOnlySortFieldType(client);
            addField(client, SLUG, STRING, true, true, false);
            addField(client, TITLE, STRING, true, true, false);
            addField(client, TITLE_SORT, ALPHA_ONLY_SORT, false, true, false);
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
//            createField(client, "", STRING, true, true, false);


            // This one is subsequently undone.
//            createField(client, "subjects", STRING, true, true, true);
            addField(client, TOP_CONCEPT, TEXT_EN_SPLITTING,
                    true, true, true);
            addField(client, LANGUAGE, STRING, true, true, true);
            addField(client, CONCEPT, STRING, true, true, true);
            addField(client, PUBLISHER, STRING, true, true, true);
            addField(client, ACCESS, STRING, true, true, true);
            addField(client, FORMAT, STRING, true, true, true);
//            createField(client, "", STRING, true, true, true);
            addField(client, SUBJECT_SOURCES, STRING, true, true, true);
            addField(client, SUBJECT_LABELS, STRING, true, true, true);
            addField(client, SUBJECT_NOTATIONS, STRING, true, true, true);
            addField(client, SUBJECT_IRIS, STRING, true, true, true);

            addField(client, CONCEPT_SEARCH, TEXT_EN_SPLITTING,
                    false, true, true);
            addField(client, TITLE_SEARCH, TEXT_EN_SPLITTING,
                    false, true, true);
            addField(client, SUBJECT_SEARCH, TEXT_EN_SPLITTING,
                    false, true, true);
            addField(client, PUBLISHER_SEARCH, TEXT_EN_SPLITTING,
                    false, true, true);
            addField(client, FULLTEXT, TEXT_EN_SPLITTING,
                    false, true, true);
//            createField(client, "", TEXT_EN_SPLITTING, false, true, true);

            addCopyField(client, "*", Arrays.asList(FULLTEXT));
            addCopyField(client, FieldConstants.TITLE,
                    Arrays.asList(TITLE_SEARCH, TITLE_SORT));
            // This one is subsequently undone.
//            addCopyField(client, "subjects", Arrays.asList("subject_search"));
            addCopyField(client, CONCEPT, Arrays.asList(CONCEPT_SEARCH));
            addCopyField(client, TOP_CONCEPT,
                    Arrays.asList(SUBJECT_SEARCH));
//            addCopyField(client, "", Arrays.asList(""));
            addCopyField(client, SUBJECT_LABELS,
                    Arrays.asList(SUBJECT_SEARCH));
            addCopyField(client, SUBJECT_NOTATIONS,
                    Arrays.asList(SUBJECT_SEARCH));

            deleteDynamicField(client, "*_point");

            setProperty(client, "updateHandler.autoSoftCommit.maxTime",
                    "10000");
        } catch (SolrServerException sse) {
            System.out.println("Got a SolrServerException:");
            sse.printStackTrace();
        } catch (IOException ioe) {
            System.out.println("Got an IOException:");
            ioe.printStackTrace();
        }
    }

}
