/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.context;

import io.swagger.annotations.ApiKeyAuthDefinition;
import io.swagger.annotations.BasicAuthDefinition;
import io.swagger.annotations.Contact;
import io.swagger.annotations.ExternalDocs;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SecurityDefinition;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;

/** Empty interface that serves as as an anchor to which the
 * top-level Swagger definition is attached.
 */
@SuppressWarnings("checkstyle:InterfaceIsType")
@SwaggerDefinition(
        info = @Info(
                description = "Public access to the Vocabulary Registry API",
                // The @Info annotation requires that a version be specified,
                // but its value is not used. See
                // https://github.com/swagger-api/swagger-core/issues/1594
                // for the defect. Instead, the value is defined in
                // the SwaggerBootstrapper class.
                version = "see SwaggerBootstrapper for version",
                // The @Info annotation requires that a title be specified,
                // but its value is not used. See
                // https://github.com/swagger-api/swagger-core/issues/1594
                // for the defect. Instead, the value is defined in web.xml
                // as the init-param "swagger.api.title".
                title = "Vocabulary Registry API",
                termsOfService = "https://documentation.ardc.edu.au/",
                contact = @Contact(
                   name = "ARDC Services",
                   email = "services@ardc.edu.au",
                   url = "https://ardc.edu.au/"
                ),
                license = @License(
                   name = "Apache 2.0",
                   url = "https://www.apache.org/licenses/LICENSE-2.0"
                )
        ),
        consumes = { "application/xml", "application/json" },
        produces = { "application/xml", "application/json" },
        schemes = {SwaggerDefinition.Scheme.HTTPS},
        // For convenience of testing using a "bare" Tomcat,
        // comment out the previous line, and uncomment the following two:
//        schemes = {SwaggerDefinition.Scheme.HTTP,
//                SwaggerDefinition.Scheme.HTTPS},
        tags = {
                @Tag(name = SwaggerInterface.TAG_ADMIN,
                        description = "System administration operations"),
                @Tag(name = SwaggerInterface.TAG_RESOURCES,
                        description = "Operations with resources"),
                @Tag(name = SwaggerInterface.TAG_SERVICES,
                        description = "Service methods"),
                @Tag(name = SwaggerInterface.TAG_UTILITIES,
                        description = "Utility methods")
        },
        externalDocs = @ExternalDocs(
                value = "Research Vocabularies Australia",
                url = "https://documentation.ardc.edu.au/"),
        securityDefinition = @SecurityDefinition(
                basicAuthDefinitions = {
                        @BasicAuthDefinition(key = SwaggerInterface.BASIC_AUTH)
                        },
                apiKeyAuthDefinitions = {
                        @ApiKeyAuthDefinition(
                                key = SwaggerInterface.API_KEY_AUTH,
                                name = "ands_authentication",
                                in =
                                ApiKeyAuthDefinition.ApiKeyLocation.HEADER,
                                description = "RDA cookie passed as header")}
                )
)
public interface SwaggerInterface {

    /** The name of the tag used to group admin-level methods.
     * Use this within
     * {@link io.swagger.annotations.Api} annotations. */
    String TAG_ADMIN = "admin";

    /** The name of the tag used to group methods that work with
     * resources. Use this within
     * {@link io.swagger.annotations.Api} annotations. */
    String TAG_RESOURCES = "resources";

    /** The name of the tag used to group service methods.
     * Use this within
     * {@link io.swagger.annotations.Api} annotations. */
    String TAG_SERVICES = "services";

    /** The name of the tag used to group utility methods.
     * Use this within
     * {@link io.swagger.annotations.Api} annotations. */
    String TAG_UTILITIES = "utilities";

    /** The name of the basic authentication security definition.
     * Use this within
     * {@link io.swagger.annotations.Authorization} annotations. */
    String BASIC_AUTH = "basicAuth";

    /** The name of the API key authentication security definition.
     * Use this within
     * {@link io.swagger.annotations.Authorization} annotations. */
    String API_KEY_AUTH = "apiKeyAuth";

}
