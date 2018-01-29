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

//  /** Registry . */
//  public static final String REGISTRY_ =
//          "Registry.";

    /* Configure the swagger view of the API. */

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

    /* Solr. */

    /** Solr base URL. Used for CoreAdmin requests, e.g.,
     * creating/deleting a collection. */
    public static final String REGISTRY_SOLR_BASE_URL =
            "Registry.Solr.baseURL";

    /** Solr collection. The name of the Solr collection used for
     * indexing registry entities. */
    public static final String REGISTRY_SOLR_COLLECTION =
            "Registry.Solr.collection";

    /** Solr collection URL. Full path to the Solr collection,
     * to be used when creating a SolrClient. */
    public static final String REGISTRY_SOLR_COLLECTION_URL =
            "Registry.Solr.collectionURL";

    /* Public-facing Sesame. */

    /** Sesame importer SPARQL prefix. */
    public static final String SESAMEIMPORTER_SPARQLPREFIX =
            "SesameImporter.sparqlPrefix";

    /* Public-facing SISSVoc. */

    /** SISSVoc endpoints prefix. */
    public static final String SISSVOC_ENDPOINTSPREFIX =
            "SISSVoc.endpointsPrefix";

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

}
