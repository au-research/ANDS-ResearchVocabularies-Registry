/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.context;

/** Web URL paths to the API. */
public final class ApiPaths {

    /** Private constructor for a utility class. */
    private ApiPaths() {
    }

    /** The top level of the resource API. */
    public static final String API_RESOURCE = "/api/resource";

    /** The top level of the roles API. */
    public static final String API_ROLES = "/api/roles";

    /** Service methods of the API. */
    public static final String API_SERVICES = "/api/services";

    /** The top level of the user API. */
    public static final String API_USER = "/api/user";

    /** Utility methods of the API. */
    public static final String API_UTILITIES = "/api/utilities";

    /* Resource API methods. */

    /* Model elements. */

    /** Subpath for vocabulary resources. */
    public static final String VOCABULARIES = "vocabularies";

    /** Subpath for reverse-related vocabulary resources. */
    public static final String REVERSE_RELATED_VOCABULARIES =
            "reverseRelatedVocabularies";

    /** Subpath for owned vocabulary resources. */
    public static final String OWNED_VOCABULARIES = "ownedVocabularies";

    /** Subpath for vocabulary resources, by slug. */
    public static final String VOCABULARIES_BY_SLUG = "vocabulariesBySlug";

    /** Subpath for version resources. */
    public static final String VERSIONS = "versions";

    /** Subpath for access point resources. */
    public static final String ACCESS_POINTS = "accessPoints";

    /** Subpath for related entity resources. */
    public static final String RELATED_ENTITIES = "relatedEntities";

    /** Subpath for version artefact resources. */
    public static final String VERSION_ARTEFACTS = "versionArtefacts";

    /** Subpath for version artefact resources that are concept trees. */
    public static final String VERSION_ARTEFACTS_CONCEPT_TREE = "conceptTree";

    /** Subpath for file uploads. */
    public static final String UPLOADS = "uploads";

    /** Subpath for download resources. */
    public static final String DOWNLOADS = "downloads";

//  /** Subpath for  resources. */
//  public static final String  = "";

    /* Path parameter names. */

    /** Path parameter name for a vocabulary resource. */
    public static final String VOCABULARY_ID = "{vocabularyId}";

    /** Path parameter name for a version resource. */
    public static final String VERSION_ID = "{versionId}";

    /** Path parameter name for a related entity resource. */
    public static final String RELATED_ENTITY_ID = "{relatedEntityId}";

    /** Path parameter name for a slug. */
    public static final String SLUG = "{slug}";

//    /** Path parameter name for a  resource. */
//    public static final String _ID = "{Id}";

    /* Service API methods. */

    /** Subpath for PoolParty services. */
    public static final String POOLPARTY = "PoolParty";

    /** Subpath for resolution services. */
    public static final String RESOLVE = "resolve";

    /** Subpath for search services. */
    public static final String SEARCH = "search";

    /** Subpath for subscription services. */
    public static final String SUBSCRIPTIONS = "subscriptions";

    /* Subpaths for subscription services. */

    /** Subpath for email subscription services. */
    public static final String EMAIL = "email";

    /** Subpath for email subscription services for OWNER. */
    public static final String OWNER = "owner";

    /** Path parameter name for an owner name. */
    public static final String OWNER_NAME = "{owner}";

    /** Subpath for email subscription services for SYSTEM. */
    public static final String SYSTEM = "system";

    /** Subpath for email subscription services, for all subscription types. */
    public static final String ALL = "all";

    /* Subpaths for search services. */

    /** Subpath for resource search. */
    public static final String RESOURCES = "resources";

    /* Utilities API methods. */

    /** Subpath for language lookups. */
    public static final String LANGUAGES = "languages";
}
