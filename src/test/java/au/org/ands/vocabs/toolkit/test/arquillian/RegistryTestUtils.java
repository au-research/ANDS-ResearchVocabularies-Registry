/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import java.io.InputStream;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import au.org.ands.vocabs.registry.api.validation.ValidationMode;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;
import au.org.ands.vocabs.toolkit.test.utils.RegistrySchemaValidationHelper;

/** Utility methods to support tests of the registry. */
public final class RegistryTestUtils {

    /** Private constructor for a utility class. */
    private RegistryTestUtils() {
    }

    /** Logger. */
    private static Logger logger;

    static {
        logger = LoggerFactory.getLogger(
                MethodHandles.lookup().lookupClass());
    }

    /** Get vocabulary data from a file, in registry schema format.
     * Don't validate it.
     * @param filename The filename of the file to be loaded.
     * @return The parsed vocabulary data.
     * @throws JAXBException If a problem loading vocabulary data.
     */
    public static Vocabulary getUnvalidatedVocabularyFromFile(
            final String filename) throws JAXBException {
        InputStream is = ArquillianTestUtils.getResourceAsInputStream(
                filename);
        JAXBContext jaxbContext = JAXBContext.newInstance(Vocabulary.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        Vocabulary vocabulary = (Vocabulary) jaxbUnmarshaller.unmarshal(is);

        // Return as is, without validation.
        return vocabulary;
    }

    /** Get vocabulary data from a file, in registry schema format.
     * Validate it according to the specified mode.
     * @param filename The filename of the file to be loaded.
     * @param validationMode The validation mode to be used.
     * @return The parsed vocabulary data.
     * @throws JAXBException If a problem loading vocabulary data.
     */
    public static Vocabulary getValidatedVocabularyFromFile(
            final String filename,
            final ValidationMode validationMode) throws JAXBException {
        InputStream is = ArquillianTestUtils.getResourceAsInputStream(
                filename);
        JAXBContext jaxbContext = JAXBContext.newInstance(Vocabulary.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        Vocabulary vocabulary = (Vocabulary) jaxbUnmarshaller.unmarshal(is);

        Set<ConstraintViolation<RegistrySchemaValidationHelper>>
        errors;
        switch (validationMode) {
        case CREATE:
            errors = RegistrySchemaValidationHelper.getNewVocabularyValidation(
                    vocabulary);
            break;
        case UPDATE:
            errors = RegistrySchemaValidationHelper.
            getValidUpdatedVocabularyValidation(vocabulary);
            break;
        default:
            logger.error("Can't happen: unknown validation mode.");
            throw new IllegalArgumentException("Unknown validation mode");
        }

        // If we do get validation errors, helpful to see them.
        for (Object oneError : errors) {
            logger.error("Validation error: {}", oneError.toString());
        }
        // But do require that there be no errors.
        Assert.assertEquals(errors.size(), 0);
        return vocabulary;
    }

    /** Serialize a registry schema format Vocabulary instance into
     * an XML String.
     * @param vocabulary The Vocabulary instance to be serialized.
     * @return The Vocabulary instance serialized as XML.
     * @throws JAXBException If a problem loading vocabulary data.
     */
    public static String serializeVocabularySchemaEntityToXML(
            final Vocabulary vocabulary) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Vocabulary.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        // Generate DOCTYPE, to match what we put in our test data.
        jaxbMarshaller.setProperty("com.sun.xml.internal.bind.xmlHeaders",
                " <!DOCTYPE vocabulary>\n");
        // Make it pretty, so we can use newlines in our test data.
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        StringWriter stringWriter = new StringWriter();
        jaxbMarshaller.marshal(vocabulary, stringWriter);
        return stringWriter.toString();
    }

}
