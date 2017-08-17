/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.context;

import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import au.org.ands.vocabs.registry.db.entity.RelatedEntity;
import au.org.ands.vocabs.registry.db.entity.Upload;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.utils.PropertyConstants;
import au.org.ands.vocabs.registry.utils.RegistryProperties;
import au.org.ands.vocabs.toolkit.utils.ToolkitNetUtils;

/** Utility methods to work with URL paths to registry entities. */
public final class EntityPaths {

    /** Private constructor for a utility class. */
    private EntityPaths() {
    }

    // For now, we don't need to keep these around after the static
    // block has done its work. If needed later, uncomment, and
    // remove the assignment statements from the beginning of the
    // static block.
//    /** Scheme of the API. */
//    private static String scheme =
//            RegistryProperties.getProperty(
//                    PropertyConstants.REGISTRY_SWAGGER_DEFAULTSCHEME,
//                    "https");
//
//    /** Host of the API. */
//    private static String host =
//            RegistryProperties.getProperty(
//                    PropertyConstants.REGISTRY_SWAGGER_HOST,
//                    SwaggerBootstrapper.REGISTRY_SWAGGER_HOST_DEFAULT);
//
//    /** Base path of the API. */
//    private static String basePath =
//            RegistryProperties.getProperty(
//                    PropertyConstants.REGISTRY_SWAGGER_BASEPATH,
//                    SwaggerBootstrapper.REGISTRY_SWAGGER_BASEPATH_DEFAULT);

    /** URI for resolving resources. Initialized in a static block. */
    private static WebTarget resourceTarget;

    // Initialize resourceTarget.
    static {
        String scheme =
                RegistryProperties.getProperty(
                        PropertyConstants.REGISTRY_SWAGGER_DEFAULTSCHEME,
                        "https");
        String host =
                RegistryProperties.getProperty(
                        PropertyConstants.REGISTRY_SWAGGER_HOST,
                        SwaggerBootstrapper.REGISTRY_SWAGGER_HOST_DEFAULT);
        String basePath =
                RegistryProperties.getProperty(
                        PropertyConstants.REGISTRY_SWAGGER_BASEPATH,
                        SwaggerBootstrapper.REGISTRY_SWAGGER_BASEPATH_DEFAULT);
        Client client = ToolkitNetUtils.getClient();
        resourceTarget = client.target(scheme + "://" + host).
                path(basePath).path(ApiPaths.API_RESOURCE);
    }


    /** Get the URI which an API user can use to retrieve this registry entity.
     * @param entity A registry entity.
     * @return The URI that an API user can use to retrieve the entity.
     */
    public static URI getURIOfEntity(final Vocabulary entity) {
        return resourceTarget.path(ApiPaths.VOCABULARIES).
                path(entity.getVocabularyId().toString()).getUri();
    }

    /** Get the URI which an API user can use to retrieve a Version.
     * @param entity A registry entity.
     * @return The URI that an API user can use to retrieve the entity.
     */
    public static URI getURIOfEntity(final Version entity) {
        return resourceTarget.path(ApiPaths.VERSIONS).
                path(entity.getVersionId().toString()).getUri();
    }

    /** Get the URI which an API user can use to retrieve a RelatedEntity.
     * @param entity A registry entity.
     * @return The URI that an API user can use to retrieve the entity.
     */
    public static URI getURIOfEntity(final RelatedEntity entity) {
        return resourceTarget.path(ApiPaths.RELATED_ENTITIES).
                path(entity.getRelatedEntityId().toString()).getUri();
    }

    /** Get the URI which an API user can use to retrieve an Upload.
     * @param entity A registry entity.
     * @return The URI that an API user can use to retrieve the entity.
     */
    public static URI getURIOfEntity(final Upload entity) {
        return resourceTarget.path(ApiPaths.UPLOADS).
                path(entity.getId().toString()).getUri();
    }

}
