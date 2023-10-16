/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.REGISTRY;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.dbunit.DatabaseUnitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.api.validation.CheckRelatedEntity;
import au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntity;
import au.org.ands.vocabs.toolkit.test.utils.RegistrySchemaValidationHelper;

/** Tests of the validation of related entity data provided to API methods. */
@Test
public class RegistryCheckRelatedEntityTests extends ArquillianBaseTest {

    // Leave logger here, though it is unused. We might want to use
    // it later.
    /** Logger for this class. */
    @SuppressWarnings("unused")
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Name of this class, used in paths to test data files. */
    private static final String CLASS_NAME_PREFIX =
            "RegistryCheckRelatedEntityTests.";

    /** Test of the CheckRelatedEntity validator, when adding a new
     * related entity.
     * The test data contains multiple validation errors:
     * missing title and owner, invalid email, invalid URL and identifiers, etc.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If there is an error configuring or
     *      reading XML data.
     */
    @Test
    public final void testCheckRelatedEntity1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        String testName = "testCheckRelatedEntity1";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        RelatedEntity newRelatedEntity;

        InputStream is = ArquillianTestUtils.getResourceAsInputStream(
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-validation-1.xml");
        JAXBContext jaxbContext = JAXBContext.newInstance(RelatedEntity.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        newRelatedEntity = (RelatedEntity) jaxbUnmarshaller.unmarshal(is);

        Set<ConstraintViolation<RegistrySchemaValidationHelper>>
        errors = RegistrySchemaValidationHelper.getNewRelatedEntityValidation(
                newRelatedEntity);

        List<ValidationSummary> actualErrors = new ArrayList<>();
        for (ConstraintViolation<RegistrySchemaValidationHelper> oneError
                : errors) {
//            logger.info("One error: message template: "
//                + oneError.getMessageTemplate()
//                + "; propertyPath: " + oneError.getPropertyPath());
            actualErrors.add(new ValidationSummary(
                    oneError.getMessageTemplate(),
                    oneError.getPropertyPath().toString()));
        }

        List<ValidationSummary> expectedErrors = new ArrayList<>();
        String pathPrefix = "testNewRelatedEntity.newRelatedEntity";
        // Generic "Invalid related entity" error.
        expectedErrors.add(new ValidationSummary(
                "{" + CheckRelatedEntity.INTERFACE_NAME
                + "}",
                pathPrefix));
        // And now the specific errors ...
        // First, id.
        expectedErrors.add(new ValidationSummary(
                "{" + CheckRelatedEntity.INTERFACE_NAME
                + ".create.id}",
                pathPrefix + ".id"));

        // type owner title email url
        expectedErrors.add(new ValidationSummary(
                "{" + CheckRelatedEntity.INTERFACE_NAME
                + ".type}",
                pathPrefix + ".type"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckRelatedEntity.INTERFACE_NAME
                + ".owner}",
                pathPrefix + ".owner"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckRelatedEntity.INTERFACE_NAME
                + ".title}",
                pathPrefix + ".title"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckRelatedEntity.INTERFACE_NAME
                + ".email}",
                pathPrefix + ".email"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckRelatedEntity.INTERFACE_NAME
                + ".url}",
                pathPrefix + ".url[0]"));
        // identifiers
        // identifier[0] is missing a type
        expectedErrors.add(new ValidationSummary(
                "{" + CheckRelatedEntity.INTERFACE_NAME
                + ".relatedEntityIdentifier.identifierType}",
                pathPrefix + ".relatedEntityIdentifier[0].identifierType"));
        // identifier[1] specifies an id
        expectedErrors.add(new ValidationSummary(
                "{" + CheckRelatedEntity.INTERFACE_NAME
                + ".relatedEntityIdentifier.id}",
                pathPrefix + ".relatedEntityIdentifier[1].id"));
        // identifier[1] through identifier[14] have invalid values
        // CHECKSTYLE:OFF: MagicNumber
        for (int i = 1; i <= 14; i++) {
            // CHECKSTYLE:ON: MagicNumber
            expectedErrors.add(new ValidationSummary(
                    "{" + CheckRelatedEntity.INTERFACE_NAME
                    + ".relatedEntityIdentifier.identifierValue}",
                    pathPrefix + ".relatedEntityIdentifier["
                            + i + "].identifierValue"));
        }
        /* Copy/paste/fill in as needed:
        expectedErrors.add(new ValidationSummary(
                "{" + CheckRelatedEntity.INTERFACE_NAME
                + "}",
                pathPrefix + ""));
         */
        Assert.assertEqualsNoOrder(actualErrors.toArray(),
                expectedErrors.toArray(),
                "Set of validation errors does not match");
    }

    /** Test of the CheckRelatedEntity validator, when updating a
     * related entity.
     * The test data contains multiple validation errors:
     * missing id, title and owner, invalid email,
     * invalid URL and identifiers, etc.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If there is an error configuring or
     *      reading XML data.
     */
    @Test
    public final void testCheckRelatedEntity2() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        String testName = "testCheckRelatedEntity2";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + testName);

        RelatedEntity updatedRelatedEntity;

        InputStream is = ArquillianTestUtils.getResourceAsInputStream(
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-validation-1.xml");
        JAXBContext jaxbContext = JAXBContext.newInstance(RelatedEntity.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        updatedRelatedEntity = (RelatedEntity) jaxbUnmarshaller.unmarshal(is);

        Set<ConstraintViolation<RegistrySchemaValidationHelper>>
        errors = RegistrySchemaValidationHelper.
        getUpdatedRelatedEntityValidation(
                updatedRelatedEntity);

        List<ValidationSummary> actualErrors = new ArrayList<>();
        for (ConstraintViolation<RegistrySchemaValidationHelper> oneError
                : errors) {
//            logger.info("One error: message template: "
//                + oneError.getMessageTemplate()
//                + "; propertyPath: " + oneError.getPropertyPath());
            actualErrors.add(new ValidationSummary(
                    oneError.getMessageTemplate(),
                    oneError.getPropertyPath().toString()));
        }

        List<ValidationSummary> expectedErrors = new ArrayList<>();
        String pathPrefix = "testUpdatedRelatedEntity.updatedRelatedEntity";
        // Generic "Invalid related entity" error.
        expectedErrors.add(new ValidationSummary(
                "{" + CheckRelatedEntity.INTERFACE_NAME
                + "}",
                pathPrefix));
        // And now the specific errors ...
        // First, id.
        expectedErrors.add(new ValidationSummary(
                "{" + CheckRelatedEntity.INTERFACE_NAME
                + ".update.id}",
                pathPrefix + ".id"));

        // type owner title email url
        expectedErrors.add(new ValidationSummary(
                "{" + CheckRelatedEntity.INTERFACE_NAME
                + ".type}",
                pathPrefix + ".type"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckRelatedEntity.INTERFACE_NAME
                + ".owner}",
                pathPrefix + ".owner"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckRelatedEntity.INTERFACE_NAME
                + ".title}",
                pathPrefix + ".title"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckRelatedEntity.INTERFACE_NAME
                + ".email}",
                pathPrefix + ".email"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckRelatedEntity.INTERFACE_NAME
                + ".url}",
                pathPrefix + ".url[0]"));
        // identifiers
        // identifier[0] is missing a type
        expectedErrors.add(new ValidationSummary(
                "{" + CheckRelatedEntity.INTERFACE_NAME
                + ".relatedEntityIdentifier.identifierType}",
                pathPrefix + ".relatedEntityIdentifier[0].identifierType"));
        // identifier[1] specifies an id, which, on update, is allowed
        // identifier[1] through identifier[14] have invalid values
        // CHECKSTYLE:OFF: MagicNumber
        for (int i = 1; i <= 14; i++) {
            // CHECKSTYLE:ON: MagicNumber
            expectedErrors.add(new ValidationSummary(
                    "{" + CheckRelatedEntity.INTERFACE_NAME
                    + ".relatedEntityIdentifier.identifierValue}",
                    pathPrefix + ".relatedEntityIdentifier["
                            + i + "].identifierValue"));
        }
        /* Copy/paste/fill in as needed:
        expectedErrors.add(new ValidationSummary(
                "{" + CheckRelatedEntity.INTERFACE_NAME
                + "}",
                pathPrefix + ""));
         */

        Assert.assertEqualsNoOrder(actualErrors.toArray(),
                expectedErrors.toArray(),
                "Set of validation errors does not match");
    }



    /** Static nested class to aid comparison between actual and expected
     * sets of constraint violations. An instance of this class represents
     * one violation simplified to its essentials. */
    private static class ValidationSummary {

        /** The message template of the violation. */
        private String messageTemplate;

        /** The property path of the violation, as a String. */
        private String propertyPath;

        /** Constructor.
         * @param aMessageTemplate The message template of the violation.
         * @param aPropertyPath The property path of the violation, as a String.
         */
        ValidationSummary(final String aMessageTemplate,
                final String aPropertyPath) {
            messageTemplate = aMessageTemplate;
            propertyPath = aPropertyPath;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "messageTemplate: " + messageTemplate
                    + "; propertyPath:" + propertyPath;
        }

        /** {@inheritDoc}
         * Equality test based on messageTemplate and propertyPath.
         */
        @Override
        public boolean equals(final Object other) {
            if (other == null
                    || !(other instanceof ValidationSummary)) {
                return false;
            }
            ValidationSummary vsOther =
                    (ValidationSummary) other;
            return messageTemplate.equals(vsOther.messageTemplate)
                    && propertyPath.equals(vsOther.propertyPath);
        }

        /** {@inheritDoc}
         * The hash code returned is that of the messageTemplate.
         */
        @Override
        public int hashCode() {
            return messageTemplate.hashCode();
        }

    }

}
