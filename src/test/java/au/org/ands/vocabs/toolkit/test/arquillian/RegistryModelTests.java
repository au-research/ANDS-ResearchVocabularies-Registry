/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.REGISTRY;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.time.LocalDateTime;

import javax.persistence.EntityManager;
import javax.xml.bind.JAXBException;

import org.dbunit.DatabaseUnitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.api.validation.ValidationMode;
import au.org.ands.vocabs.registry.db.context.DBContext;
import au.org.ands.vocabs.registry.model.ModelMethods;
import au.org.ands.vocabs.registry.model.VocabularyModel;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;

/** Tests of the registry model. */
@Test
public class RegistryModelTests extends ArquillianBaseTest {

    /** Logger. */
    private static Logger logger;

    static {
        logger = LoggerFactory.getLogger(
                MethodHandles.lookup().lookupClass());
    }

    /** A convenient value to use for endDate properties when
     * deleting. */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static LocalDateTime nowTime1 =
            LocalDateTime.of(2017, 10, 1, 10, 10);

    /** A convenient value to use for endDate properties when
     * deleting. */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static LocalDateTime nowTime2 =
            LocalDateTime.of(2017, 10, 1, 10, 20);

    /** Test of attempting to request a vocabulary model without
     * specifying a vocabulary ID. It should throw an exception.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp =
            "Attempt to construct vocabulary model with no Id")
    public final void testRequestModelNullVocabularyId() throws
    DatabaseUnitException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        EntityManager em = DBContext.getEntityManager();
        try {
            ModelMethods.createVocabularyModel(em, null);
        } catch (Exception e) {
            if (em != null) {
                em.getTransaction().rollback();
                throw e;
            }
        } finally {
            em.close();
        }
    }

    /** Test of attempting to request the vocabulary model of
     * a never-existent vocabulary. It should throw an exception.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp =
            "Attempt to construct vocabulary model with invalid Id")
    public final void testRequestModelOfNeverExistentVocabulary() throws
    DatabaseUnitException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        EntityManager em = DBContext.getEntityManager();
        try {
            ModelMethods.createVocabularyModel(em, 1);
        } finally {
            em.close();
        }
    }

    /** Test of deleting the current version of a vocabulary that has only
     * a current instance, with Vocabulary and VocabularyRelatedEntity
     * model elements.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testDeleteOnlyCurrent1() throws
    DatabaseUnitException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY,
                "testDeleteOnlyCurrent1");
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            em.getTransaction().begin();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            ModelMethods.deleteOnlyCurrentVocabulary(vm, "TEST", nowTime1);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em != null) {
                em.getTransaction().rollback();
                throw e;
            }
        } finally {
            if (em != null) {
                em.close();
            }
        }
        ArquillianTestUtils.compareDatabaseCurrentAndExpectedContents(
                REGISTRY,
                "test/tests/au.org.ands.vocabs.toolkit."
                + "test.arquillian.AllArquillianTests."
                + "testDeleteOnlyCurrent1/"
                + "test-registry-results.xml");
    }

    /** Test of deleting the current version of a vocabulary that has only
     * a draft instance, with Vocabulary and VocabularyRelatedEntity
     * model elements.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testDeleteOnlyDraft1() throws
    DatabaseUnitException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY,
                "testDeleteOnlyDraft1");
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            em.getTransaction().begin();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            ModelMethods.deleteOnlyDraftVocabulary(vm, "TEST", nowTime1);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
        ArquillianTestUtils.compareDatabaseCurrentAndExpectedContents(
                REGISTRY,
                "test/tests/au.org.ands.vocabs.toolkit."
                + "test.arquillian.AllArquillianTests."
                + "testDeleteOnlyDraft1/"
                + "test-registry-results.xml");
    }

    /** Test of starting with a vocabulary that has only
     * a current instance, and making that current instance into
     * a draft only. Only Vocabulary and VocabularyRelatedEntity
     * model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testMakeCurrentVocabularyDraft1() throws
    DatabaseUnitException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY,
                "testMakeCurrentVocabularyDraft1");
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            em.getTransaction().begin();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            ModelMethods.makeCurrentVocabularyDraft(vm, "TEST", nowTime1);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em != null) {
                em.getTransaction().rollback();
                throw e;
            }
        } finally {
            if (em != null) {
                em.close();
            }
        }

        ArquillianTestUtils.compareDatabaseCurrentAndExpectedContents(
                REGISTRY,
                "test/tests/au.org.ands.vocabs.toolkit."
                + "test.arquillian.AllArquillianTests."
                + "testMakeCurrentVocabularyDraft1/"
                + "test-registry-results.xml");
    }


    /** Test of deleting the current version of a vocabulary that also has
     * a draft instance, with Vocabulary and VocabularyRelatedEntity
     * model elements.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testDeleteCurrentLeavingDraft1() throws
    DatabaseUnitException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY,
                "testDeleteCurrentLeavingDraft1");
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            em.getTransaction().begin();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            ModelMethods.deleteOnlyCurrentVocabulary(vm, "TEST", nowTime1);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em != null) {
                em.getTransaction().rollback();
                throw e;
            }
        } finally {
            if (em != null) {
                em.close();
            }
        }

        ArquillianTestUtils.compareDatabaseCurrentAndExpectedContents(
                REGISTRY,
                "test/tests/au.org.ands.vocabs.toolkit."
                + "test.arquillian.AllArquillianTests."
                + "testDeleteCurrentLeavingDraft1/"
                + "test-registry-results.xml");
    }

    /** Test of applying changes to the current version of a vocabulary
     * that does not have a draft instance.
     * Only Vocabulary and VocabularyRelatedEntity
     * model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testApplyChangesCurrent1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY,
                "testApplyChangesCurrent1");
        Vocabulary vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/au.org.ands.vocabs.toolkit."
                        + "test.arquillian.AllArquillianTests."
                        + "testApplyChangesCurrent1/"
                        + "test-vocabulary.xml", ValidationMode.UPDATE);
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            em.getTransaction().begin();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            ModelMethods.applyChanges(vm, "TEST", nowTime1, vocabulary);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em != null) {
                em.getTransaction().rollback();
                throw e;
            }
        } finally {
            if (em != null) {
                em.close();
            }
        }

        ArquillianTestUtils.compareDatabaseCurrentAndExpectedContents(
                REGISTRY,
                "test/tests/au.org.ands.vocabs.toolkit."
                + "test.arquillian.AllArquillianTests."
                + "testApplyChangesCurrent1/"
                + "test-registry-results.xml");
    }

    /** Test of starting with a vocabulary that has only a draft instance,
     * and applying a change that makes it published.
     * Only Vocabulary and VocabularyRelatedEntity
     * model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testApplyChangesCurrent2() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY,
                "testApplyChangesCurrent2");
        Vocabulary vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/au.org.ands.vocabs.toolkit."
                        + "test.arquillian.AllArquillianTests."
                        + "testApplyChangesCurrent2/"
                        + "test-vocabulary.xml", ValidationMode.UPDATE);
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            em.getTransaction().begin();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            ModelMethods.applyChanges(vm, "TEST", nowTime1, vocabulary);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em != null) {
                em.getTransaction().rollback();
                throw e;
            }
        } finally {
            if (em != null) {
                em.close();
            }
        }

        ArquillianTestUtils.compareDatabaseCurrentAndExpectedContents(
                REGISTRY,
                "test/tests/au.org.ands.vocabs.toolkit."
                + "test.arquillian.AllArquillianTests."
                + "testApplyChangesCurrent2/"
                + "test-registry-results.xml");
    }

    /** Test of adding a draft to a vocabulary
     * that has an existing published instance, and then publishing
     * the same.
     * Only Vocabulary and VocabularyRelatedEntity
     * model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testApplyChangesDraft1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY,
                "testApplyChangesDraft1");
        Vocabulary vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/au.org.ands.vocabs.toolkit."
                        + "test.arquillian.AllArquillianTests."
                        + "testApplyChangesDraft1/"
                        + "test-vocabulary-1.xml", ValidationMode.UPDATE);
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            em.getTransaction().begin();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            ModelMethods.applyChanges(vm, "TEST", nowTime1, vocabulary);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em != null) {
                em.getTransaction().rollback();
                throw e;
            }
        } finally {
            if (em != null) {
                em.close();
            }
        }

        ArquillianTestUtils.compareDatabaseCurrentAndExpectedContents(
                REGISTRY,
                "test/tests/au.org.ands.vocabs.toolkit."
                + "test.arquillian.AllArquillianTests."
                + "testApplyChangesDraft1/"
                + "test-registry-results-1.xml");

        vocabulary = RegistryTestUtils.getValidatedVocabularyFromFile(
                "test/tests/au.org.ands.vocabs.toolkit."
                        + "test.arquillian.AllArquillianTests."
                        + "testApplyChangesDraft1/"
                        + "test-vocabulary-2.xml", ValidationMode.UPDATE);
        em = null;
        try {
            em = DBContext.getEntityManager();
            em.getTransaction().begin();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            ModelMethods.applyChanges(vm, "TEST", nowTime2, vocabulary);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em != null) {
                em.getTransaction().rollback();
                throw e;
            }
        } finally {
            if (em != null) {
                em.close();
            }
        }

        ArquillianTestUtils.compareDatabaseCurrentAndExpectedContents(
                REGISTRY,
                "test/tests/au.org.ands.vocabs.toolkit."
                + "test.arquillian.AllArquillianTests."
                + "testApplyChangesDraft1/"
                + "test-registry-results-2.xml");
    }

    /** Test of updating a draft of a vocabulary
     * that already has both existing published and draft instances.
     * Only Vocabulary and VocabularyRelatedEntity
     * model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testApplyChangesDraft2() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY,
                "testApplyChangesDraft2");
        Vocabulary vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/au.org.ands.vocabs.toolkit."
                        + "test.arquillian.AllArquillianTests."
                        + "testApplyChangesDraft2/"
                        + "test-vocabulary.xml", ValidationMode.UPDATE);
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            em.getTransaction().begin();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            ModelMethods.applyChanges(vm, "TEST", nowTime1, vocabulary);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em != null) {
                em.getTransaction().rollback();
                throw e;
            }
        } finally {
            if (em != null) {
                em.close();
            }
        }

        ArquillianTestUtils.compareDatabaseCurrentAndExpectedContents(
                REGISTRY,
                "test/tests/au.org.ands.vocabs.toolkit."
                + "test.arquillian.AllArquillianTests."
                + "testApplyChangesDraft2/"
                + "test-registry-results.xml");
    }


//  ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//  "testDeleteOnlyPublished-out.xml");

    // Sample assertions below; remove when done.
//        Assert.assertTrue(TemporalUtils.isDraft(vocabulary), "Not draft");
//        Assert.assertTrue(TemporalUtils.isDraftAdditionOrModification(
//                vocabulary), "Not draft addition or modification");
//        Assert.assertFalse(TemporalUtils.isHistorical(vocabulary),
//                "Is historical");
//        Assert.assertFalse(TemporalUtils.isCurrent(vocabulary), "Is current");
//        Assert.assertFalse(TemporalUtils.isDraftDeletion(
//                vocabulary), "Is draft deletion");
//        Assert.assertEquals(TemporalUtils.getTemporalDescription(vocabulary),
//                TemporalMeaning.DRAFT_ADDITION_OR_MODIFICATION,
//                "Meaning not DRAFT_ADDITION_OR_MODIFICATION");

}
