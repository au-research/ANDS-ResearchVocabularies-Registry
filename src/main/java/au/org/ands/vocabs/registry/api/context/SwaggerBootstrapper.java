/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.context;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

import au.org.ands.vocabs.registry.utils.PropertyConstants;
import au.org.ands.vocabs.registry.utils.RegistryProperties;
import io.swagger.converter.ModelConverters;
import io.swagger.jackson.ModelResolver;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.StringProperty;
import io.swagger.util.Json;

/** Class to bootstrap the configuration of the Swagger view of the API.
 */
public class SwaggerBootstrapper extends HttpServlet {

    /** Default setting for the host for the API. */
    public static final String REGISTRY_SWAGGER_HOST_DEFAULT = "localhost:8080";

    /** Default setting for the base path for the API. */
    public static final String REGISTRY_SWAGGER_BASEPATH_DEFAULT =
            "/vocabs-registry";

    /** Generated UID for serialization. */
    private static final long serialVersionUID = -6508920960856827547L;

    /** Initialize the settings of the Swagger view of the API.
     */
    @SuppressWarnings("checkstyle:DesignForExtension")
    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);

        // This makes Swagger honor JAXB annotations. Well,
        // not _all_ of them. It seems it doesn't respect
        // @XmlEnumValue.
        ObjectMapper mapper = Json.mapper();
        mapper.registerModule(new JaxbAnnotationModule());

        // The problem is the constructor of Swagger's AbstractModelConverter
        // class, which "overrides" our introspector with Swagger's own.
        // At least, the "override" comes into play at a point we don't
        // want it to, i.e., when values of enums are extracted.

        // So, create a customized ModelResolver that uses the
        // JaxbAnnotationModule to get values of enumerated types.
        // Need to create an instance of an anonymous class that
        // overrides the _addEnumProps() method!
        // (Could also be done as a separate subclass, of course.)
        final AnnotationIntrospector jaxbIntrospector =
                new JaxbAnnotationIntrospector(mapper.getTypeFactory());
        // Also need Jackson's introspector, as it is used for
        // swagger-core's own enums (e.g., io.swagger.models.auth.In).
        final AnnotationIntrospector jacksonIntrospector =
                new JacksonAnnotationIntrospector();
        mapper.setAnnotationIntrospector(
                AnnotationIntrospector.pair(jacksonIntrospector,
                        jaxbIntrospector));

        ModelResolver modelResolver = new ModelResolver(mapper) {
            @Override
            protected void _addEnumProps(final Class<?> propClass,
                    final Property property) {
                final boolean useIndex = _mapper.isEnabled(
                        SerializationFeature.WRITE_ENUMS_USING_INDEX);
                final boolean useToString = _mapper.isEnabled(
                        SerializationFeature.WRITE_ENUMS_USING_TO_STRING);

                @SuppressWarnings("unchecked")
                Class<Enum<?>> enumClass = (Class<Enum<?>>) propClass;
                // The following was updated to work with Jackson 2.8.
                Enum<?>[] enumConstants = enumClass.getEnumConstants();
                String[] enumValues = new String[enumConstants.length];
                enumValues = jaxbIntrospector.findEnumValues(
                        enumClass, enumConstants, enumValues);
                for (int i = 0; i < enumConstants.length; i++) {
                    String n;
                    if (useIndex) {
                        n = String.valueOf(enumConstants[i].ordinal());
                    } else if (useToString) {
                        n = enumConstants[i].toString();
                    } else {
                        // This is the original code, which gets the "first"
                        // introspector, which is Swagger's, not ours.
//                        n = _intr.findEnumValue(en);
                        // Instead, use the introspector we created above.
                        n = enumValues[i];
                    }
                    if (property instanceof StringProperty) {
                        StringProperty sp = (StringProperty) property;
                        sp._enum(n);
                    }
                }
            }
        };

        // Have Swagger use our custom ModelConverter when generating
        // swagger.json.
        ModelConverters.getInstance().addConverter(modelResolver);

        // Now define the top-level Swagger properties.
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setTitle("Vocabulary Registry API");
        beanConfig.setVersion("0.2.0");
        // Schemes are set in SwaggerInterface.
//        beanConfig.setSchemes(new String[] {"https"});
        beanConfig.setHost(RegistryProperties.getProperty(
                PropertyConstants.REGISTRY_SWAGGER_HOST,
                REGISTRY_SWAGGER_HOST_DEFAULT));
        beanConfig.setBasePath(RegistryProperties.getProperty(
                PropertyConstants.REGISTRY_SWAGGER_BASEPATH,
                REGISTRY_SWAGGER_BASEPATH_DEFAULT));
        // If you add a new package that has classes that have
        // API methods, you need to update the following statement:
        beanConfig.setResourcePackage("au.org.ands.vocabs.registry.api"
                + ",au.org.ands.vocabs.registry.notification.email.admin"
                + ",au.org.ands.vocabs.registry.solr"
                + ",au.org.ands.vocabs.registry.workflow");
        // Without scan=true, the top-level properties set in the previous
        // lines don't make their way into the generated swagger.json!
        beanConfig.setScan(true);
    }
}
