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

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.validation.ConstraintViolation;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.dbunit.DatabaseUnitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.api.validation.CheckVocabulary;
import au.org.ands.vocabs.registry.api.validation.SubjectSources;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary.Subject;
import au.org.ands.vocabs.toolkit.test.utils.RegistrySchemaValidationHelper;

/** Tests of the validation of vocabulary data provided to API methods. */
@Test
public class RegistryCheckVocabularyTests extends ArquillianBaseTest {

    // Leave logger here, though it is unused. We might want to use
    // it later.
    /** Logger for this class. */
    @SuppressWarnings("unused")
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Name of this class, used in paths to test data files. */
    private static final String CLASS_NAME_PREFIX =
            "RegistryCheckVocabularyTests.";

    /** Test of the CheckVocabulary validator.
     * The test data contains multiple validation errors:
     * there is no publisher, there is a reference to an unknown related
     * entity, the version creation is not in the correct syntax,
     * the PoolParty server id is wrong, etc.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If there is an error configuring or
     *      reading XML data.
     */
    @Test
    public final void testCheckVocabulary1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        String testName = "testCheckVocabulary1";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        Vocabulary newVocabulary;

        InputStream is = ArquillianTestUtils.getResourceAsInputStream(
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-validation-1.xml");
        JAXBContext jaxbContext = JAXBContext.newInstance(Vocabulary.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        newVocabulary = (Vocabulary) jaxbUnmarshaller.unmarshal(is);

        Set<ConstraintViolation<RegistrySchemaValidationHelper>>
        errors = RegistrySchemaValidationHelper.getNewVocabularyValidation(
                newVocabulary);

        List<ValidationSummary> actualErrors = new ArrayList<>();
        for (ConstraintViolation<RegistrySchemaValidationHelper> oneError
                : errors) {
//            logger.info("One error: message template: "
//                + oneError.getMessageTemplate()
//                + "; propertypath: " + oneError.getPropertyPath());
            actualErrors.add(new ValidationSummary(
                    oneError.getMessageTemplate(),
                    oneError.getPropertyPath().toString()));
        }

        List<ValidationSummary> expectedErrors = new ArrayList<>();
        String pathPrefix = "testNewVocabulary.newVocabulary.";
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".relatedEntityRef.noPublisher}",
                pathPrefix + "relatedEntityRef"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".relatedEntityRef.unknown}",
                pathPrefix + "relatedEntityRef[0]"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".version.creationDate}; "
                + "value must be either 4, 7, or 10 characters long",
                pathPrefix + "version[0].releaseDate"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".poolpartyProject.serverId}",
                pathPrefix + "poolpartyProject.serverId"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".subject.source}",
                pathPrefix + "subject[0].source"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".version.id}",
                pathPrefix + "version[0].id"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".subject.noAnzsrcFor}",
                pathPrefix + "subject"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".version.noAccessPoint}",
                pathPrefix + "version[0]"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".version.doImportButNothingToImport}",
                pathPrefix + "version[0].doImport"));
//        expectedErrors.add(new ValidationSummary(
//                "{" + CheckVocabulary.INTERFACE_NAME
//                + "}",
//                pathPrefix + ""));

        Assert.assertEqualsNoOrder(actualErrors.toArray(),
                expectedErrors.toArray(),
                "Set of validation errors does not match");
    }

    /** Test of the CheckVocabulary validator.
     * This is a test to confirm that a vocabulary being updated
     * may not have a related vocabulary that is itself.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If there is an error configuring or
     *      reading XML data.
     */
    @Test
    public final void testCheckVocabulary2() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        String testName = "testCheckVocabulary2";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + testName);

        Vocabulary updatedVocabulary;

        InputStream is = ArquillianTestUtils.getResourceAsInputStream(
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-validation.xml");
        JAXBContext jaxbContext = JAXBContext.newInstance(Vocabulary.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        updatedVocabulary = (Vocabulary) jaxbUnmarshaller.unmarshal(is);

        Set<ConstraintViolation<RegistrySchemaValidationHelper>>
        errors = RegistrySchemaValidationHelper.getUpdatedVocabularyValidation(
                updatedVocabulary);

        List<ValidationSummary> actualErrors = new ArrayList<>();
        for (ConstraintViolation<RegistrySchemaValidationHelper> oneError
                : errors) {
//            logger.info("One error: message template: "
//                + oneError.getMessageTemplate()
//                + "; propertypath: " + oneError.getPropertyPath());
            actualErrors.add(new ValidationSummary(
                    oneError.getMessageTemplate(),
                    oneError.getPropertyPath().toString()));
        }

        List<ValidationSummary> expectedErrors = new ArrayList<>();
        String pathPrefix = "testUpdatedVocabulary.updatedVocabulary.";
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".relatedVocabularyRef.selfReference}",
                pathPrefix + "relatedVocabularyRef[0]"));
//        expectedErrors.add(new ValidationSummary(
//                "{" + CheckVocabulary.INTERFACE_NAME
//                + "}",
//                pathPrefix + ""));

        Assert.assertEqualsNoOrder(actualErrors.toArray(),
                expectedErrors.toArray(),
                "Set of validation errors does not match");
    }

    /** Tests of the CheckVocabulary validator.
     * The test data contains multiple validation errors related to
     * languages.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If there is an error configuring or
     *      reading XML data.
     */
    @Test
    public final void testCheckVocabulary3() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        String testName = "testCheckVocabulary3";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + testName);
        Vocabulary newVocabulary;

        InputStream is = ArquillianTestUtils.getResourceAsInputStream(
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-validation-1.xml");
        JAXBContext jaxbContext = JAXBContext.newInstance(Vocabulary.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        newVocabulary = (Vocabulary) jaxbUnmarshaller.unmarshal(is);

        Set<ConstraintViolation<RegistrySchemaValidationHelper>>
        errors = RegistrySchemaValidationHelper.getNewVocabularyValidation(
                newVocabulary);

        List<ValidationSummary> actualErrors = new ArrayList<>();
        for (ConstraintViolation<RegistrySchemaValidationHelper> oneError
                : errors) {
//            logger.info("One error: message template: "
//                + oneError.getMessageTemplate()
//                + "; propertypath: " + oneError.getPropertyPath());
            actualErrors.add(new ValidationSummary(
                    oneError.getMessageTemplate(),
                    oneError.getPropertyPath().toString()));
        }

        List<ValidationSummary> expectedErrors = new ArrayList<>();
        String pathPrefix = "testNewVocabulary.newVocabulary.";
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".primaryLanguage}",
                pathPrefix + "primaryLanguage"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".otherLanguage}",
                pathPrefix + "otherLanguage[1]"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".otherLanguage}",
                pathPrefix + "otherLanguage[3]"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".otherLanguage.duplicate}",
                pathPrefix + "otherLanguage"));
//        expectedErrors.add(new ValidationSummary(
//                "{" + CheckVocabulary.INTERFACE_NAME
//                + "}",
//                pathPrefix + ""));

        Assert.assertEqualsNoOrder(actualErrors.toArray(),
                expectedErrors.toArray(),
                "Set of validation errors does not match");

        is = ArquillianTestUtils.getResourceAsInputStream(
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-validation-2.xml");
        newVocabulary = (Vocabulary) jaxbUnmarshaller.unmarshal(is);

        errors = RegistrySchemaValidationHelper.getNewVocabularyValidation(
                newVocabulary);

        actualErrors = new ArrayList<>();
        for (ConstraintViolation<RegistrySchemaValidationHelper> oneError
                : errors) {
//            logger.info("One error: message template: "
//                + oneError.getMessageTemplate()
//                + "; propertypath: " + oneError.getPropertyPath());
            actualErrors.add(new ValidationSummary(
                    oneError.getMessageTemplate(),
                    oneError.getPropertyPath().toString()));
        }

        expectedErrors = new ArrayList<>();
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".otherLanguage}",
                pathPrefix + "otherLanguage[2]"));
//        expectedErrors.add(new ValidationSummary(
//                "{" + CheckVocabulary.INTERFACE_NAME
//                + "}",
//                pathPrefix + ""));

        Assert.assertEqualsNoOrder(actualErrors.toArray(),
                expectedErrors.toArray(),
                "Set of validation errors does not match");
        // Now check canonicalization of the language tags.
        Assert.assertEquals(newVocabulary.getPrimaryLanguage(),
                "en-CA", "Primary language not canonicalized");
        List<String> otherLanguages = newVocabulary.getOtherLanguage();
        Assert.assertEquals(otherLanguages.get(1),
                "fr-FR", "Other language 1 not canonicalized");
    }

    /** Tests of the CheckVocabulary validator.
     * This tests the resolution of subject labels and notations.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If there is an error configuring or
     *      reading XML data.
     */
    @Test
    public final void testCheckVocabulary4() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        String testName = "testCheckVocabulary4";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + testName);
        EntityManager em = ArquillianTestUtils.getEntityManagerForDb(REGISTRY);
        em.getTransaction().begin();
        Query q = em.createQuery(
                "UPDATE SubjectResolverEntry SET notation = '' "
                + "WHERE source = 'gcmd'");
        q.executeUpdate();
        em.getTransaction().commit();
        em.close();

        // This test populates the subject resolver, by adding values
        // to SubjectSources.RESOLVING_SUBJECT_SOURCES.
        // We don't want other tests to use it, so we need to reset both
        // before and after.
        SubjectSources.resetResolvingSubjectSources();

        Vocabulary newVocabulary;

        InputStream is = ArquillianTestUtils.getResourceAsInputStream(
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-validation-1.xml");
        JAXBContext jaxbContext = JAXBContext.newInstance(Vocabulary.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        newVocabulary = (Vocabulary) jaxbUnmarshaller.unmarshal(is);

        Set<ConstraintViolation<RegistrySchemaValidationHelper>>
        errors = RegistrySchemaValidationHelper.getNewVocabularyValidation(
                newVocabulary);

        logger.error(errors.toString());

        Assert.assertTrue(errors.isEmpty(),
                "Non-empty set of validation errors");

        List<Subject> subjects = newVocabulary.getSubject();
        Subject subject = subjects.get(0);
        Assert.assertEquals(subject.getNotation(), "08");
        Assert.assertEquals(subject.getLabel(),
                "INFORMATION AND COMPUTING SCIENCES");

        subject = subjects.get(1);
        Assert.assertEquals(subject.getNotation(), "8101");
        Assert.assertEquals(subject.getLabel(),
                "DEFENCE");

        subject = subjects.get(2);
        Assert.assertEquals(subject.getNotation(), "");
        Assert.assertEquals(subject.getLabel(),
                "ATMOSPHERE");

        // This test populates the subject resolver, by adding values
        // to SubjectSources.RESOLVING_SUBJECT_SOURCES.
        // We don't want other tests to use it, so we need to reset both
        // before and after.
        SubjectSources.resetResolvingSubjectSources();
    }

    /** Test of the CheckVocabulary validator.
     * The test data contains errors relating to SesameDownload access points.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If there is an error configuring or
     *      reading XML data.
     */
    @Test
    public final void testCheckVocabulary5() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        String testName = "testCheckVocabulary5";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + testName);

        Vocabulary newVocabulary;

        InputStream is = ArquillianTestUtils.getResourceAsInputStream(
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-validation-1.xml");
        JAXBContext jaxbContext = JAXBContext.newInstance(Vocabulary.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        newVocabulary = (Vocabulary) jaxbUnmarshaller.unmarshal(is);

        Set<ConstraintViolation<RegistrySchemaValidationHelper>>
        errors = RegistrySchemaValidationHelper.getNewVocabularyValidation(
                newVocabulary);

        List<ValidationSummary> actualErrors = new ArrayList<>();
        for (ConstraintViolation<RegistrySchemaValidationHelper> oneError
                : errors) {
//            logger.info("One error: message template: "
//                + oneError.getMessageTemplate()
//                + "; propertypath: " + oneError.getPropertyPath()
//                + "; message: " + oneError.getMessage());
            actualErrors.add(new ValidationSummary(
                    oneError.getMessageTemplate(),
                    oneError.getPropertyPath().toString()));
        }

        List<ValidationSummary> expectedErrors = new ArrayList<>();
        String pathPrefix = "testNewVocabulary.newVocabulary.";
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".accessPoint.apSesameDownload.create}",
                pathPrefix + "version[0].accessPoint[0].apSesameDownload"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".accessPoint.apSesameDownload.urlPrefix}",
                pathPrefix + "version[0].accessPoint[0]."
                        + "apSesameDownload.urlPrefix"));

//        expectedErrors.add(new ValidationSummary(
//                "{" + CheckVocabulary.INTERFACE_NAME
//                + "}",
//                pathPrefix + ""));

        Assert.assertEqualsNoOrder(actualErrors.toArray(),
                expectedErrors.toArray(),
                "Set of validation errors does not match");

        is = ArquillianTestUtils.getResourceAsInputStream(
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-validation-2.xml");
        Vocabulary updatedVocabulary =
                (Vocabulary) jaxbUnmarshaller.unmarshal(is);

        errors = RegistrySchemaValidationHelper.getUpdatedVocabularyValidation(
                updatedVocabulary);

        actualErrors = new ArrayList<>();
        for (ConstraintViolation<RegistrySchemaValidationHelper> oneError
                : errors) {
//            logger.info("One error: message template: "
//                + oneError.getMessageTemplate()
//                + "; propertypath: " + oneError.getPropertyPath()
//                + "; message: " + oneError.getMessage());
            actualErrors.add(new ValidationSummary(
                    oneError.getMessageTemplate(),
                    oneError.getPropertyPath().toString()));
        }

        expectedErrors = new ArrayList<>();
        pathPrefix = "testUpdatedVocabulary.updatedVocabulary.";
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".accessPoint.apSesameDownload.source}",
                pathPrefix + "version[0].accessPoint[0]."
                        + "apSesameDownload.source"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".accessPoint.apSesameDownload.urlPrefix}",
                pathPrefix + "version[0].accessPoint[0]."
                        + "apSesameDownload.urlPrefix"));

//        expectedErrors.add(new ValidationSummary(
//                "{" + CheckVocabulary.INTERFACE_NAME
//                + "}",
//                pathPrefix + ""));

        Assert.assertEqualsNoOrder(actualErrors.toArray(),
                expectedErrors.toArray(),
                "Set of validation errors does not match");

    }

    /** Test of the CheckVocabulary validator.
     * The test data contains errors relating to generated, but empty slugs.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If there is an error configuring or
     *      reading XML data.
     */
    @Test
    public final void testCheckVocabulary6() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        String testName = "testCheckVocabulary6";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + testName);

        Vocabulary newVocabulary;

        InputStream is = ArquillianTestUtils.getResourceAsInputStream(
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-validation-1.xml");
        JAXBContext jaxbContext = JAXBContext.newInstance(Vocabulary.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        newVocabulary = (Vocabulary) jaxbUnmarshaller.unmarshal(is);

        Set<ConstraintViolation<RegistrySchemaValidationHelper>>
        errors = RegistrySchemaValidationHelper.getNewVocabularyValidation(
                newVocabulary);

        List<ValidationSummary> actualErrors = new ArrayList<>();
        for (ConstraintViolation<RegistrySchemaValidationHelper> oneError
                : errors) {
//            logger.info("One error: message template: "
//                + oneError.getMessageTemplate()
//                + "; propertypath: " + oneError.getPropertyPath()
//                + "; message: " + oneError.getMessage());
            actualErrors.add(new ValidationSummary(
                    oneError.getMessageTemplate(),
                    oneError.getPropertyPath().toString()));
        }

        List<ValidationSummary> expectedErrors = new ArrayList<>();
        String pathPrefix = "testNewVocabulary.newVocabulary.";
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".title.generatedSlugEmpty}",
                pathPrefix + "title"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".version.title.generatedSlugEmpty}",
                pathPrefix + "version[0].title"));

//        expectedErrors.add(new ValidationSummary(
//                "{" + CheckVocabulary.INTERFACE_NAME
//                + "}",
//                pathPrefix + ""));

        Assert.assertEqualsNoOrder(actualErrors.toArray(),
                expectedErrors.toArray(),
                "Set of validation errors does not match");

        is = ArquillianTestUtils.getResourceAsInputStream(
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-validation-2.xml");
        Vocabulary updatedVocabulary =
                (Vocabulary) jaxbUnmarshaller.unmarshal(is);

        errors = RegistrySchemaValidationHelper.getUpdatedVocabularyValidation(
                updatedVocabulary);

        actualErrors = new ArrayList<>();
        for (ConstraintViolation<RegistrySchemaValidationHelper> oneError
                : errors) {
//            logger.info("One error: message template: "
//                + oneError.getMessageTemplate()
//                + "; propertypath: " + oneError.getPropertyPath()
//                + "; message: " + oneError.getMessage());
            actualErrors.add(new ValidationSummary(
                    oneError.getMessageTemplate(),
                    oneError.getPropertyPath().toString()));
        }

        expectedErrors = new ArrayList<>();
        pathPrefix = "testUpdatedVocabulary.updatedVocabulary.";
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".title.generatedSlugEmpty}",
                pathPrefix + "title"));
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".version.title.generatedSlugEmpty}",
                pathPrefix + "version[0].title"));
        // At present, we don't allow the vocabulary slug to be changed,
        // so we must also expect this error.
        expectedErrors.add(new ValidationSummary(
                "{" + CheckVocabulary.INTERFACE_NAME
                + ".update.slug}",
                pathPrefix + "slug"));

//        expectedErrors.add(new ValidationSummary(
//                "{" + CheckVocabulary.INTERFACE_NAME
//                + "}",
//                pathPrefix + ""));

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
