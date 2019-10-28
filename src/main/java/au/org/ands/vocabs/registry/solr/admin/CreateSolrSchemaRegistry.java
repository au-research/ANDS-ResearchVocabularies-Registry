/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.solr.admin;

import static au.org.ands.vocabs.registry.solr.FieldConstants.ACCESS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.ACRONYM;
import static au.org.ands.vocabs.registry.solr.FieldConstants.ALPHA_ONLY_SORT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.BOOLEAN;
import static au.org.ands.vocabs.registry.solr.FieldConstants.CONCEPT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.CONCEPT_PHRASE;
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
import static au.org.ands.vocabs.registry.solr.FieldConstants.NOTE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.NOTE_PHRASE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.OWNER;
import static au.org.ands.vocabs.registry.solr.FieldConstants.POOLPARTY_ID;
import static au.org.ands.vocabs.registry.solr.FieldConstants.PUBLISHER;
import static au.org.ands.vocabs.registry.solr.FieldConstants.PUBLISHER_PHRASE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.PUBLISHER_SEARCH;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SISSVOC_ENDPOINT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SLUG;
import static au.org.ands.vocabs.registry.solr.FieldConstants.STATUS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.STRING;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SUBJECT_IRIS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SUBJECT_LABELS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SUBJECT_NOTATIONS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SUBJECT_PHRASE;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.cloud.SolrZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.solr.FieldConstants;

/** Create the Registry's Solr Schema for the vocabulary metadata.
 * Run as:
 * java au.org.ands.vocabs.registry.solr.admin.CreateSolrSchemaRegistry
 *  http://localhost:8983/solr/vocabs-registry
 */
public final class CreateSolrSchemaRegistry extends SolrSchemaBase {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** {@inheritDoc} */
    @Override
    protected Logger getLogger() {
        return logger;
    }

    /** The latest version of the schema definition. */
    private static final int SCHEMA_DEFINITION = 3;

    /** Install schema version 3.
     * @param client The SolrClient into which to install the schema.
     * @throws SolrServerException If a SolrServerException was generated
     *      by the SolrJ API.
     * @throws IOException If an IOException was generated by the SolrJ API.
     */
    private void installSchema3(final SolrClient client)
            throws SolrServerException, IOException {
        try {
            // We don't use SCHEMA_DEFINITION here,
            // but only in installSchema().
            // CHECKSTYLE:OFF: MagicNumber
            setInstalledSchemaVersion(client, 3);
            // CHECKSTYLE:ON: MagicNumber
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
            addField(client, NOTE, TEXT_EN_SPLITTING,
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
            // Have a *_phrase field for every field mentioned in
            // the edismax qf parameter. These fields are then used
            // in the custom query plugin's pqf parameter.
            addField(client, TITLE_PHRASE, LOWER_EXACT_WORDS,
                    true, true, false);
            addField(client, SUBJECT_PHRASE, LOWER_EXACT_WORDS,
                    true, true, true);
            addField(client, DESCRIPTION_PHRASE, LOWER_EXACT_WORDS,
                    true, true, false);
            addField(client, NOTE_PHRASE, LOWER_EXACT_WORDS,
                    true, true, false);
            addField(client, CONCEPT_PHRASE, LOWER_EXACT_WORDS,
                    true, true, true, extraAttributesForConcepts);
            addField(client, PUBLISHER_PHRASE, LOWER_EXACT_WORDS,
                    true, true, true);

            addCopyField(client, "*", Arrays.asList(FULLTEXT));
            addCopyField(client, FieldConstants.TITLE,
                    Arrays.asList(TITLE_SEARCH, TITLE_SORT, TITLE_PHRASE));
            addCopyField(client, CONCEPT,
                    Arrays.asList(CONCEPT_SEARCH, CONCEPT_PHRASE));
            addCopyField(client, TOP_CONCEPT,
                    Arrays.asList(CONCEPT_SEARCH, CONCEPT_PHRASE));
            addCopyField(client, SUBJECT_LABELS,
                    Arrays.asList(SUBJECT_SEARCH, SUBJECT_PHRASE));
            addCopyField(client, SUBJECT_NOTATIONS,
                    Arrays.asList(SUBJECT_SEARCH, SUBJECT_PHRASE));
            addCopyField(client, PUBLISHER,
                    Arrays.asList(PUBLISHER_SEARCH, PUBLISHER_PHRASE));

            addCopyField(client, DESCRIPTION,
                    Arrays.asList(DESCRIPTION_PHRASE));
            addCopyField(client, NOTE,
                    Arrays.asList(NOTE_PHRASE));

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
            if (installedSchemaVersion < SCHEMA_DEFINITION) {
                if (!(client instanceof EmbeddedSolrServer)) {
                    // The value of "update.autoCreateFields" is used only in
                    // solrconfig.xml. Because it _is_ used in solrconfig.xml,
                    // we need to set its value here.
                    setUserProperty(client, "update.autoCreateFields", "false");
                    uploadSolrconfig(collectionName, zkHost);
                    reloadCollection(baseURL, collectionName);
                }
                installSchema3(client);
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
        CreateSolrSchemaRegistry createSolrSchemaRegistry =
                new CreateSolrSchemaRegistry();
        if (args.length != 4) {
            createSolrSchemaRegistry.getLogger().error(
                    "Must provide four command-line parameters");
            return;
        }

        String baseURL = args[0];
        String collectionName = args[1];
        String apiURL = args[2];
        String zkHost = args[3];

        try (SolrClient client = new HttpSolrClient.Builder(apiURL).build()) {
            createSolrSchemaRegistry.installSchema(
                    baseURL, client, collectionName, zkHost);
        } catch (SolrServerException sse) {
            createSolrSchemaRegistry.getLogger().error(
                    "Got a SolrServerException:", sse);
        } catch (IOException ioe) {
            createSolrSchemaRegistry.getLogger().error(
                    "Got an IOException:", ioe);
        }
    }

}
