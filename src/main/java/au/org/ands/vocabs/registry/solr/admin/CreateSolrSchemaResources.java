/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.solr.admin;

import static au.org.ands.vocabs.registry.solr.FieldConstants.ALPHA_ONLY_SORT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.DATE_POINT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.DCTERMS_DESCRIPTION;
import static au.org.ands.vocabs.registry.solr.FieldConstants.DCTERMS_TITLE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.IRI;
import static au.org.ands.vocabs.registry.solr.FieldConstants.LAST_UPDATED;
import static au.org.ands.vocabs.registry.solr.FieldConstants.LOWER_EXACT_WORDS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.OWNER;
import static au.org.ands.vocabs.registry.solr.FieldConstants.PINT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.PUBLISHER;
import static au.org.ands.vocabs.registry.solr.FieldConstants.RDFS_LABEL;
import static au.org.ands.vocabs.registry.solr.FieldConstants.RDF_TYPE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SISSVOC_ENDPOINT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SKOS_ALTLABEL;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SKOS_DEFINITION;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SKOS_HIDDENLABEL;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SKOS_NOTATION;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SKOS_PREFLABEL;
import static au.org.ands.vocabs.registry.solr.FieldConstants.STATUS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.STRING;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SUBJECT_LABELS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TEXT_EN_SPLITTING;
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
import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.cloud.SolrZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.solr.FieldConstants;

/** Create the Registry's Solr Schema for the vocabulary resources.
 * Run as:
 * java au.org.ands.vocabs.registry.solr.admin.CreateSolrSchemaResources
 *  http://localhost:8983/solr/vocabs-resources
 */
public final class CreateSolrSchemaResources extends SolrSchemaBase {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** {@inheritDoc} */
    @Override
    protected Logger getLogger() {
        return logger;
    }

    /** The latest version of the schema definition. */
    private static final int SCHEMA_DEFINITION = 1;

    /** Install resource cschema version 1.
     * @param client The SolrClient into which to install the schema.
     * @throws SolrServerException If a SolrServerException was generated
     *      by the SolrJ API.
     * @throws IOException If an IOException was generated by the SolrJ API.
     */
    private void installSchema1(final SolrClient client)
            throws SolrServerException, IOException {
        try {
            // We don't use SCHEMA_DEFINITION here,
            // but only in installSchema().
            // CHECKSTYLE:OFF: MagicNumber
            setInstalledSchemaVersion(client, 1);
            // CHECKSTYLE:ON: MagicNumber
            addAlphaOnlySortFieldType(client);
            addLowerFieldType(client);

            // Data about the Registry's storage of the version.
            // For example, the timestamp of the last update.
            addField(client, LAST_UPDATED, DATE_POINT, true, true, false);

            // Basic fields that are stored, and not multiValued.
            // Why STRING, not PINT for the vocabulary Id field?
            // Sigh, for "consistency" with the use of a STRING
            // for the "id" field of the Registry collection,
            // and in case we want to do a "join" of
            // this vocabulary_id field with those values.
            addField(client, IRI, STRING, true, true, false);
            addField(client, VOCABULARY_ID, STRING, true, true, false);
            addField(client, VOCABULARY_ID_IRI, STRING, true, true, false);
            addField(client, VOCABULARY_TITLE, STRING, true, true, false);
            // But we do use PINT for the version Id, because we don't
            // need to match on it, and it helps us to do sorting.
            addField(client, VERSION_ID, PINT, true, true, false);
            addField(client, VERSION_RELEASE_DATE, STRING, true, true, false);
            addField(client, VERSION_TITLE, STRING, true, true, false);
            addField(client, OWNER, STRING, true, true, false);
            addField(client, TITLE, STRING, true, true, false);
            addField(client, TOP_CONCEPT, TEXT_EN_SPLITTING,
                    true, true, false);
            addField(client, SKOS_NOTATION, STRING, true, true, true);

            addMultilingualFields(client, RDFS_LABEL, true, true, true);
            addMultilingualFields(client, DCTERMS_TITLE, true, true, true);
            addMultilingualFields(client, DCTERMS_DESCRIPTION, true, true,
                    true);
            // SKOS prefLabel has an integrity constraint limiting
            // to one per language. Allow multiple instances anyway ...
            addMultilingualFields(client, SKOS_PREFLABEL, true, true, true);
            addMultilingualFields(client, SKOS_ALTLABEL, true, true, true);
            addMultilingualFields(client, SKOS_HIDDENLABEL, true, true, true);
            addMultilingualFields(client, SKOS_DEFINITION, true, true, true);

            addField(client, STATUS, STRING, true, true, false);
            addField(client, SISSVOC_ENDPOINT, STRING,
                    true, true, false);

            // Basic fields that are stored, and multiValued.
            addField(client, RDF_TYPE, STRING, true, true, true);
            addField(client, PUBLISHER, STRING, true, true, true);
            addField(client, SUBJECT_LABELS, STRING, true, true, true);

            // Fields with custom type alphaOnlySort, used only for
            // sorting results.
            addField(client, TITLE_SORT, ALPHA_ONLY_SORT, true, true, false);

            // Fields that are used for searching, that are "analysed".
            // They are stored, so that we get highlighting for them.

            // Phrase fields have stored = true, indexed = true.
            // Have a *_phrase field for every field mentioned in
            // the edismax qf parameter. These fields are then used
            // in the custom query plugin's pqf parameter.
            addField(client, TOP_CONCEPT_PHRASE, LOWER_EXACT_WORDS,
                    true, true, false);

            addCopyField(client, FieldConstants.TITLE,
                    Arrays.asList(TITLE_SORT));
            addCopyField(client, TOP_CONCEPT,
                    Arrays.asList(TOP_CONCEPT_PHRASE));

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
                installSchema1(client);
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
        CreateSolrSchemaResources createSolrSchemaResources =
                new CreateSolrSchemaResources();
        if (args.length != 4) {
            createSolrSchemaResources.getLogger().error(
                    "Must provide four command-line parameters");
            return;
        }

        String baseURL = args[0];
        String collectionName = args[1];
        String apiURL = args[2];
        String zkHost = args[3];

        try (SolrClient client = new HttpSolrClient.Builder(apiURL).build()) {
            createSolrSchemaResources.installSchema(
                    baseURL, client, collectionName, zkHost);
        } catch (SolrServerException sse) {
            createSolrSchemaResources.getLogger().error(
                    "Got a SolrServerException:", sse);
        } catch (IOException ioe) {
            createSolrSchemaResources.getLogger().error(
                    "Got an IOException:", ioe);
        }
    }

}
