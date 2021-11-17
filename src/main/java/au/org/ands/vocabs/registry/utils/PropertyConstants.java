/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.utils;

/** Definition of constants that refer to names of properties used
 * by the Registry. Use these values as the parameter of
 * {@link RegistryProperties#getProperty(String)} and related methods.
 */
public final class PropertyConstants {

    /** Private constructor for a utility class. */
    private PropertyConstants() {
    }

    /* Top-level properties. */

    /** Registry version. */
    public static final String REGISTRY_VERSION =
            "Registry.version";

    /** Registry version timestamp. */
    public static final String REGISTRY_VERSIONTIMESTAMP =
            "Registry.versionTimestamp";

    /** Registry build date. */
    public static final String REGISTRY_BUILDDATE =
            "Registry.buildDate";

    /** Registry storage path. */
    public static final String REGISTRY_STORAGEPATH =
            "Registry.storagePath";

    /** Registry vocabs path. */
    public static final String REGISTRY_VOCABSPATH =
            "Registry.vocabsPath";

    /** Registry temp path. */
    public static final String REGISTRY_TEMPPATH =
            "Registry.tempPath";

    /** Registry uploads path. */
    public static final String REGISTRY_UPLOADSPATH =
            "Registry.uploadsPath";

    /** Registry download prefix. */
    public static final String REGISTRY_DOWNLOADPREFIX =
            "Registry.downloadPrefix";

    /** Registry language subtag registry. */
    public static final String REGISTRY_LSR =
            "Registry.lsr";

//  /** Registry . */
//  public static final String REGISTRY_ =
//          "Registry.";

    /* Configure the Swagger view of the API. */

    /** Swagger scheme. */
    public static final String REGISTRY_SWAGGER_DEFAULTSCHEME =
            "Registry.swagger.defaultScheme";
    /** Swagger host. */
    public static final String REGISTRY_SWAGGER_HOST =
            "Registry.swagger.host";
    /** Swagger basePath. */
    public static final String REGISTRY_SWAGGER_BASEPATH =
            "Registry.swagger.basePath";

    /* Logging of API operations. */

    /** Location of a GeoIP2 database for looking up IP addresses. */
    public static final String REGISTRY_LOGGING_GEOIPDB =
            "Registry.logging.GeoIPDb";

    /* Timeouts for network connections to external services.
     * For now, that means to PoolParty. */

    /** Timeout to use for connecting to an external service,
     * in milliseconds. */
    public static final String REGISTRY_NETWORK_TIMEOUT_CONNECT =
            "Registry.network.timeout.connect";

    /** Timeout to use for reads from an external service,
     * in milliseconds. */
    public static final String REGISTRY_NETWORK_TIMEOUT_READ =
            "Registry.network.timeout.read";

    /* Solr. */

    /** Solr base URL. Used for CoreAdmin requests, e.g.,
     * creating/deleting a collection. */
    public static final String REGISTRY_SOLR_BASE_URL =
            "Registry.Solr.baseURL";

    /** Solr ZooKeeper zkHost. Used for ZooKeeper requests, e.g.,
     * uploading configuration files. */
    public static final String REGISTRY_SOLR_ZKHOST =
            "Registry.Solr.zkHost";

    /** Solr collection. The name of the Solr collection used for
     * indexing registry entities. */
    public static final String REGISTRY_SOLR_COLLECTION =
            "Registry.Solr.collection";

    /** Solr registry collection URL. Full path to the Solr collection
     * for registry entities, to be used when creating a SolrClient. */
    public static final String REGISTRY_SOLR_COLLECTION_URL =
            "Registry.Solr.collectionURL";

    /** Solr collection. The name of the Solr collection used for
     * indexing vocabulary resources. */
    public static final String REGISTRY_SOLR_RESOURCES_COLLECTION =
            "Registry.Solr.resources.collection";

    /** Solr registry collection URL. Full path to the Solr collection
     * for vocabulary resources, to be used when creating a SolrClient. */
    public static final String REGISTRY_SOLR_RESOURCES_COLLECTION_URL =
            "Registry.Solr.resources.collectionURL";

    /* Public-facing Sesame. */

    /** Sesame importer SPARQL prefix. */
    public static final String SESAME_IMPORTER_SPARQLPREFIX =
            "SesameImporter.sparqlPrefix";

    /* Public-facing SISSVoc. */

    /** SISSVoc endpoints prefix. */
    public static final String SISSVOC_ENDPOINTSPREFIX =
            "SISSVoc.endpointsPrefix";

    /* Harvesters. */

    /** PoolParty harvester default format. */
    public static final String POOLPARTYHARVESTER_DEFAULTFORMAT =
            "PoolPartyHarvester.defaultFormat";

    /** PoolParty harvester default export module. */
    public static final String POOLPARTYHARVESTER_DEFAULTEXPORTMODULE =
            "PoolPartyHarvester.defaultExportModule";

    /** Metadata transform provider metadata rewrite map path. */
    public static final String REGISTRY_METADATAREWRITEMAPPATH =
            "Registry.metadataRewriteMapPath";

    /* Importers. */

    /* Sesame importer. */

    /** Sesame importer server URL. */
    public static final String SESAME_IMPORTER_SERVERURL =
            "SesameImporter.serverUrl";

    /* Publishers. */

    /* SISSVoc publisher. */

    /** SISSVoc spec template. */
    public static final String SISSVOC_SPECTEMPLATE =
            "SISSVoc.specTemplate";

    /** SISSVoc specs path. */
    public static final String SISSVOC_SPECSPATH =
            "SISSVoc.specsPath";

    /** SISSVoc template variable DEPLOYPATH. */
    public static final String SISSVOC_VARIABLE_DEPLOYPATH =
            "SISSVoc.variable.DEPLOYPATH";

    /** SISSVoc template variable SERVICE_TITLE. */
    public static final String SISSVOC_VARIABLE_SERVICE_TITLE =
            "SISSVoc.variable.SERVICE_TITLE";

    /** SISSVoc template variable SERVICE_AUTHOR. */
    public static final String SISSVOC_VARIABLE_SERVICE_AUTHOR =
            "SISSVoc.variable.SERVICE_AUTHOR";

    /** SISSVoc template variable SERVICE_AUTHOR_EMAIL. */
    public static final String SISSVOC_VARIABLE_SERVICE_AUTHOR_EMAIL =
            "SISSVoc.variable.SERVICE_AUTHOR_EMAIL";

    /** SISSVoc template variable SERVICE_HOMEPAGE. */
    public static final String SISSVOC_VARIABLE_SERVICE_HOMEPAGE =
            "SISSVoc.variable.SERVICE_HOMEPAGE";

    /** SISSVoc template variable SPARQL_ENDPOINT_PREFIX. */
    public static final String SISSVOC_VARIABLE_SPARQL_ENDPOINT_PREFIX =
            "SISSVoc.variable.SPARQL_ENDPOINT_PREFIX";

    /** SISSVoc template variable HTML_STYLESHEET. */
    public static final String SISSVOC_VARIABLE_HTML_STYLESHEET =
            "SISSVoc.variable.HTML_STYLESHEET";

    /* Notifications */

    /** Prefix to use for URLs of portal pages referred to in notifications. */
    public static final String NOTIFICATIONS_PORTAL_PREFIX =
            "Notifications.portalPrefix";

    /* Notifications: email */

    /** Hostname of SMTP server used to send generated emails. */
    public static final String NOTIFICATIONS_EMAIL_SMTPHOST =
            "Notifications.email.smtpHost";

    /** Port number of SMTP server used to send generated emails. */
    public static final String NOTIFICATIONS_EMAIL_SMTPPORT =
            "Notifications.email.smtpPort";

    /** What to put in the "From" line of generated emails. */
    public static final String NOTIFICATIONS_EMAIL_SENDER_EMAILADDRESS =
            "Notifications.email.senderEmailAddress";

    /** What to put in the "From" line of generated emails. */
    public static final String NOTIFICATIONS_EMAIL_SENDER_FULLNAME =
            "Notifications.email.senderFullName";

    /** What to put in the "Reply-To" line of generated emails. */
    public static final String NOTIFICATIONS_EMAIL_REPLYTO =
            "Notifications.email.replyTo";

    /** What to put in the "Subject" line of generated emails. */
    public static final String NOTIFICATIONS_EMAIL_SUBJECT =
            "Notifications.email.subject";

    /** Path to file containing HTML template. */
    public static final String NOTIFICATIONS_EMAIL_TEMPLATE_HTML =
            "Notifications.email.template.HTML";

    /** Path to file containing plain text template. */
    public static final String NOTIFICATIONS_EMAIL_TEMPLATE_PLAINTEXT =
            "Notifications.email.template.plaintext";

}
