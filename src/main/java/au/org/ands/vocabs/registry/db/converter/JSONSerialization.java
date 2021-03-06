/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.converter;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

import au.org.ands.vocabs.registry.db.internal.VersionJson;
import au.org.ands.vocabs.registry.db.internal.VocabularyJson;

/** Utility class that provides serialization and deserialization
 * of JSON data. */
public final class JSONSerialization {

    /** Private constructor for a utility class. */
    private JSONSerialization() {
    }

    /** Logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Jackson ObjectMapper used for serializing JSON data into Strings.
     * It is initialized by a static block. */
    private static ObjectMapper jsonMapper;

    static {
        jsonMapper = new ObjectMapper();
        // Enable the use of the JAXB annotations in the classes
        // being serialized.
        // Registering this module also has the effect that
        // for a key/value pair, if the value is an empty array,
        // the key/value is omitted. (The NON_NULL serialization
        // inclusion setting doesn't do this.)
        JaxbAnnotationModule module = new JaxbAnnotationModule();
        jsonMapper.registerModule(module);
        // Don't serialize null values.  With this, but without the
        // JaxbAnnotationModule module registration
        // above, empty arrays that are values of a key/value pair
        // _would_ still be serialized.
        jsonMapper.setSerializationInclusion(Include.NON_NULL);
        // The next setting is not obvious, and the results are not obvious.
        // There is an interaction with the JaxbAnnotationModule
        // (which, in turn, uses Jackson's JaxbAnnotationIntrospector).
        // For one of our JAXB-generated classes, its effect seems to be
        // as follows:
        //  First, serialize the properties mentioned in the @XmlType
        //    annotation's propOrder attribute, in that order.
        //  Then, serialize all remaining properties in alpha order.
        // So, for VocabularyJson, the order is currently:
        //   poolpartyProject
        //   subjects
        //   otherLanguages
        //   topConcepts
        //   acronym
        //   creation-date
        //   description
        //   draft-created-date
        //   draft-modified-date
        //   licence
        //   note
        //   primary-language
        //   revision-cycle
        //   title
        jsonMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY,
                true);
        // The following setting applies when serializing a (Hash)Map;
        // keys/values are sorted alphabetically by key.
        // This is currently only important to achieve really consistent
        // results for the test suite. (The behaviour was probably consistent
        // anyway, but the behaviour might change across JVM version
        // and implementation.)
        jsonMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,
                true);
    }

    /** Deserialize a String in JSON format.
     * @param jsonString The String to be deserialized. It should normally be
     *      an instance of one of our custom JSON storage objects,
     *      such as {@link VocabularyJson} or {@link VersionJson}.
     *      However, generic collections such as Map are supported.
     * @param <T> Type parameter of the target JSON storage object class.
     * @param jsonClass The target JSON storage object class.
     * @return The deserialization as a JSON storage object of jsonString.
     */
    public static <T> T deserializeStringAsJson(final String jsonString,
            final Class<T> jsonClass) {
        try {
            return jsonMapper.readValue(jsonString, jsonClass);
        } catch (IOException e) {
            LOGGER.error("Unable to deserialize JSON", e);
            return null;
        }
    }

    /** Deserialize a String in JSON format.
     * @param jsonString The String to be deSerialized. It should normally be
     *      an instance of one of our custom JSON storage objects,
     *      such as {@link VocabularyJson} or {@link VersionJson}.
     *      However, generic collections such as Map are supported.
     * @param <T> Type parameter of the target JSON storage object class.
     * @param typeReference A type reference for the target JSON storage
     *      object class.
     * @return The deserialization as a JSON storage object of jsonString.
     */
    public static <T> T deserializeStringAsJson(final String jsonString,
            final TypeReference<T> typeReference) {
        try {
            return jsonMapper.readValue(jsonString, typeReference);
        } catch (IOException e) {
            LOGGER.error("Unable to deserialize JSON", e);
            return null;
        }
    }

    /** Deserialize a file containing data in JSON format.
     * @param jsonFile A File containing the JSON data to be deSerialized.
     *      The contents should normally be
     *      an instance of one of our custom JSON storage objects,
     *      such as {@link VocabularyJson} or {@link VersionJson}.
     *      For a generic collection type, use the variant method that
     *      accepts a TypeReference parameter.
     * @param <T> Type parameter of the target JSON storage object class.
     * @param jsonClass The target JSON storage object class.
     * @return The deserialization as a JSON storage object of the
     *      contents of jsonFile.
     */
    public static <T> T deserializeStringAsJson(final File jsonFile,
            final Class<T> jsonClass) {
        try {
            return jsonMapper.readValue(jsonFile, jsonClass);
        } catch (IOException e) {
            LOGGER.error("Unable to deserialize JSON", e);
            return null;
        }
    }

    /** Deserialize a file containing data in JSON format.
     * @param jsonFile A File containing the JSON data to be deSerialized.
     *      The contents should normally be
     *      an instance of one of our custom JSON storage objects,
     *      such as {@link VocabularyJson} or {@link VersionJson}.
     *      However, generic collections such as Map are supported.
     * @param <T> Type parameter of the target JSON storage object class.
     * @param typeReference A type reference for the target JSON storage
     *      object class.
     * @return The deserialization as a JSON storage object of the
     *      contents of jsonFile.
     */
    public static <T> T deserializeStringAsJson(final File jsonFile,
            final TypeReference<T> typeReference) {
        try {
            return jsonMapper.readValue(jsonFile, typeReference);
        } catch (IOException e) {
            LOGGER.error("Unable to deserialize JSON", e);
            return null;
        }
    }

    /** Serialize an object into a String in JSON format.
     * @param object The Object to be serialized.
     * @return The serialization as a JSON String of object.
     */
    public static String serializeObjectAsJsonString(final Object object) {
        try {
            return jsonMapper.writeValueAsString(object);
        } catch (IOException e) {
            LOGGER.error("Unable to serialize as JSON", e);
            return null;
        }
    }

}
