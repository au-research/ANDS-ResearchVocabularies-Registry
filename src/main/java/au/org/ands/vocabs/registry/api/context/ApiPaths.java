/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.context;

/** Web URL paths to the API. */
public final class ApiPaths {

    /** Private constructor for a utility class. */
    private ApiPaths() {
    }

    /** The top level of the user API. */
    public static final String API_USER = "/api/user";

    /** The top level of the resource API. */
    public static final String API_RESOURCE = "/api/resource";

    /** The top level of the roles API. */
    public static final String API_ROLES = "/api/roles";

    /** Utility methods of the API. */
    public static final String API_UTILITIES = "/api/utilities";

    /* Model elements. */

    /** Subpath for vocabulary resources. */
    public static final String VOCABULARIES = "vocabularies";

    /** Subpath for version resources. */
    public static final String VERSIONS = "versions";

    /** Subpath for access point resources. */
    public static final String ACCESS_POINTS = "accessPoints";

    /** Subpath for related entity resources. */
    public static final String RELATED_ENTITIES = "relatedEntities";

    /** Subpath for file uploads. */
    public static final String UPLOADS = "uploads";

//  /** Subpath for  resources. */
//  public static final String  = "";

    /* Path parameter names. */

    /** Path parameter name for a vocabulary resource. */
    public static final String VOCABULARY_ID = "{vocabularyId}";

    /** Path parameter name for a version resource. */
    public static final String VERSION_ID = "{versionId}";

    /** Path parameter name for a related entity resource. */
    public static final String RELATED_ENTITY_ID = "{relatedEntityId}";

//    /** Path parameter name for a  resource. */
//    public static final String _ID = "{Id}";



}
