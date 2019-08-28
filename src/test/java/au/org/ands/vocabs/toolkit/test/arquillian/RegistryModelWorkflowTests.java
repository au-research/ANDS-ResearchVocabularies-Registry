/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.REGISTRY;
import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.ROLES;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.xml.bind.JAXBException;

import org.dbunit.DatabaseUnitException;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.repository.manager.RepositoryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.api.validation.ValidationMode;
import au.org.ands.vocabs.registry.db.context.DBContext;
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.dao.AccessPointDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyIdDAO;
import au.org.ands.vocabs.registry.db.entity.AccessPoint;
import au.org.ands.vocabs.registry.db.entity.VocabularyId;
import au.org.ands.vocabs.registry.db.internal.ApSesameDownload;
import au.org.ands.vocabs.registry.enums.AccessPointType;
import au.org.ands.vocabs.registry.model.ModelMethods;
import au.org.ands.vocabs.registry.model.VocabularyModel;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;

/** Tests of the registry model that also involve workflow processing.
 * The names of some of the tests contain "codes" that explain how
 * much of the model and workflow system is being tested.
 * <table>
 *  <caption>Codes and their meanings</caption>
 *  <thead>
 *   <tr><th>Code</th><th>Meaning</th></tr>
 *  </thead>
 *  <tbody>
 *   <tr><td>Vo</td><td>VocabularyModel</td></tr>
 *   <tr><td>Ve</td><td>VersionModel</td></tr>
 *   <tr><td>VRE</td><td>VocabularyRelatedEntitiesModel</td></tr>
 *   <tr><td>VRV</td><td>VocabularyRelatedVocabulariesModel</td></tr>
 *   <tr><td>AP</td><td>AccessPointModel</td></tr>
 *   <tr><td>VA</td><td>VersionArtefactModel</td></tr>
 *   <tr><td>W</td><td>Workflow processing</td></tr>
 *  </tbody>
 * </table>
 *
 * For example: the method {@link #testApplyChangesCurrentVoVREVeAPVAW1()}
 * is a test
 * of the
 * {@link ModelMethods#applyChanges(VocabularyModel, String, LocalDateTime,
 *  Vocabulary)} method that exercises {@link VocabularyModel},
 * {@link au.org.ands.vocabs.registry.model.VocabularyRelatedEntitiesModel},
 * {@link au.org.ands.vocabs.registry.model.VersionsModel},
 * {@link au.org.ands.vocabs.registry.model.AccessPointsModel},
 * {@link au.org.ands.vocabs.registry.model.VersionArtefactsModel},
 * and workflow processing,
 * and it is the first test of that combination.
 */
@Test
public class RegistryModelWorkflowTests extends ArquillianBaseTest {

    // Leave logger here, though it is unused. We might want to use
    // it later.
    /** Logger. */
    @SuppressWarnings("unused")
    private static Logger logger;

    static {
        logger = LoggerFactory.getLogger(
                MethodHandles.lookup().lookupClass());
    }

    /** Name of this class, used in paths to test data files. */
    private static final String CLASS_NAME_PREFIX =
            "RegistryModelWorkflowTests.";

    /** A convenient value to use for endDate properties. */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static LocalDateTime nowTime1 =
            LocalDateTime.of(2017, 10, 1, 10, 10);

    /** A convenient value to use for endDate properties. */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static LocalDateTime nowTime2 =
            LocalDateTime.of(2017, 10, 1, 10, 20);

    /** A convenient value to use for endDate properties. */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static LocalDateTime nowTime3 =
            LocalDateTime.of(2017, 10, 1, 10, 30);

    /** Test of deleting the current instance of a vocabulary that has only
     * a current instance, with Vocabulary, VocabularyRelatedEntity,
     * Version, AccessPoint, and VersionArtefact model elements, and
     * involves workflow processing.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public final void testDeleteOnlyCurrentVoVREVeAPVAW1() throws
    DatabaseUnitException, IOException, SQLException {
        String testName = CLASS_NAME_PREFIX
                + "testDeleteOnlyCurrentVoVREVeAPVAW1";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, testName);
        ArquillianTestUtils.copyTempFilesForTest(testName);
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 4);
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            em.getTransaction().begin();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            Assert.assertTrue(vm.hasCurrent(), "Missing current instance");
            Assert.assertFalse(vm.hasDraft(), "Unexpected draft instance");
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

//        ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//                testName + "-out.xml");
        ArquillianTestUtils.
        compareDatabaseCurrentAndExpectedContentsIgnoreTaskTimestamps(
                REGISTRY,
                "test/tests/" + testName + "/test-registry-results.xml");
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 2);
    }

    /** Test of deleting the current instance of a vocabulary that has only
     * a draft instance, with Vocabulary, VocabularyRelatedEntity,
     * Version, AccessPoint, and VersionArtefact model elements.
     * In fact, it currently does <i>not</i> involve workflow processing,
     * because we don't currently do workflow processing for drafts.
     * There really shouldn't even be VersionArtefact instances for
     * drafts. But this test confirms that the draft rows are nevertheless
     * deleted.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testDeleteOnlyDraftVoVREVeAPVAW1() throws
    DatabaseUnitException, IOException, SQLException {
        String testName = CLASS_NAME_PREFIX
                + "testDeleteOnlyDraftVoVREVeAPVAW1";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, testName);
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            em.getTransaction().begin();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            Assert.assertFalse(vm.hasCurrent(), "Unexpected current instance");
            Assert.assertTrue(vm.hasDraft(), "Missing draft instance");
            ModelMethods.deleteOnlyDraftVocabulary(vm, "TEST", nowTime1);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }

//        ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//                testName + "-out.xml");
        ArquillianTestUtils.compareDatabaseCurrentAndExpectedContents(
                REGISTRY,
                "test/tests/" + testName + "/test-registry-results.xml");
    }

    /** Test of starting with a vocabulary that has only
     * a current instance, and making that current instance into
     * a draft only. Vocabulary, VocabularyRelatedEntity, Version,
     * AccessPoint, and VersionArtefact
     * model elements are used. Workflow processing is applied.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public final void testMakeCurrentVocabularyDraftVoVREVeAPVAW1() throws
    DatabaseUnitException, IOException, SQLException {
        String testName = CLASS_NAME_PREFIX
                + "testMakeCurrentVocabularyDraftVoVREVeAPVAW1";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, testName);
        ArquillianTestUtils.copyTempFilesForTest(testName);
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 4);
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

//        ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//                testName + "-out.xml");
        ArquillianTestUtils.
        compareDatabaseCurrentAndExpectedContentsIgnoreTaskTimestamps(
                REGISTRY,
                "test/tests/" + testName + "/test-registry-results.xml");
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 2);
    }

    /** Test of deleting the draft instance of a vocabulary that also has
     * a current instance, with Vocabulary, VocabularyRelatedEntity,
     * Version, AccessPoint, and VersionArtefact model elements.
     * In fact, it currently does <i>not</i> involve workflow processing,
     * because we don't currently do workflow processing for drafts.
     * There really shouldn't even be VersionArtefact instances for
     * drafts. But this test confirms that the draft rows are nevertheless
     * deleted.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public final void testDeleteDraftLeavingCurrentVoVREVeAPVAW1() throws
    DatabaseUnitException, IOException, SQLException {
        String testName = CLASS_NAME_PREFIX
                + "testDeleteDraftLeavingCurrentVoVREVeAPVAW1";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, testName);
        ArquillianTestUtils.copyTempFilesForTest(testName);
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 4);
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            em.getTransaction().begin();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            ModelMethods.deleteOnlyDraftVocabulary(vm, "TEST", nowTime1);
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

//        ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//                testName + "-out.xml");
        ArquillianTestUtils.compareDatabaseCurrentAndExpectedContents(
                REGISTRY,
                "test/tests/" + testName + "/test-registry-results.xml");
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 4);
    }

    /** Test of deleting the current instance of a vocabulary that also has
     * a draft instance, with Vocabulary, VocabularyRelatedEntity,
     * Version, AccessPoint, and VersionArtefact model elements.
     * Workflow processing is applied.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public final void testDeleteCurrentLeavingDraftVoVREVeAPVAW1() throws
    DatabaseUnitException, IOException, SQLException {
        String testName = CLASS_NAME_PREFIX
                + "testDeleteCurrentLeavingDraftVoVREVeAPVAW1";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testDeleteCurrentLeavingDraftVoVREVeAPVAW1");
        ArquillianTestUtils.copyTempFilesForTest(testName);
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 4);
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

//        ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//                testName + "-out.xml");
        ArquillianTestUtils.
        compareDatabaseCurrentAndExpectedContentsIgnoreTaskTimestamps(
                REGISTRY,
                "test/tests/" + testName + "/test-registry-results.xml");
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 2);
    }

    /** Test of applying changes to the current instance of a vocabulary
     * that does not have a draft instance.
     * Vocabulary, Version, VocabularyRelatedEntity,
     * AccessPoint, and VersionArtefact
     * model elements are used. Workflow processing is applied.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public final void testApplyChangesCurrentVoVREVeAPVAW1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        String testName = CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVREVeAPVAW1";
        ArquillianTestUtils.clearDatabase(ROLES);
        ArquillianTestUtils.loadDbUnitTestFile(ROLES, testName);
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, testName);
        ArquillianTestUtils.copyUploadsFilesForTest(testName);
        ArquillianTestUtils.copyTempFilesForTest(testName);
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 4);
        Vocabulary vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/" + testName + "/test-vocabulary.xml",
                ValidationMode.UPDATE);
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

//        ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//                testName + "-out.xml");
        ArquillianTestUtils.
        compareDatabaseCurrentAndExpectedContentsIgnoreTaskTimestamps(
                REGISTRY,
                "test/tests/" + testName + "/test-registry-results.xml");
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 4);
    }

    /** Test of starting with a vocabulary that has only a draft instance,
     * and applying a change that makes it published.
     * Vocabulary, VocabularyRelatedEntity, Version,
     * AccessPoint, and VersionArtefact
     * model elements are used. Workflow processing is applied.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testApplyChangesCurrentVoVREVeAPVAW2() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        String testName = CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVREVeAPVAW2";
        ArquillianTestUtils.clearDatabase(ROLES);
        ArquillianTestUtils.loadDbUnitTestFile(ROLES, testName);
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVREVeAPVAW2");
        ArquillianTestUtils.copyUploadsFilesForTest(testName);
        Vocabulary vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/"
                        + CLASS_NAME_PREFIX
                        + "testApplyChangesCurrentVoVREVeAPVAW2/"
                        + "test-vocabulary.xml",
                        ValidationMode.UPDATE);
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

//        ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//                testName + "-out.xml");
        ArquillianTestUtils.
        compareDatabaseCurrentAndExpectedContentsIgnoreTaskTimestamps(
                REGISTRY,
                "test/tests/" + testName + "/test-registry-results.xml");
    }

    /** Test of applying changes to the current instance of a vocabulary
     * that does not have a draft instance.
     * Vocabulary, Version, VocabularyRelatedEntity,
     * AccessPoint, and VersionArtefact
     * model elements are used. Workflow processing is applied.
     * The difference between this test and
     * {@link #testApplyChangesCurrentVoVREVeAPVAW3()} is that this
     * test involves the deletion of the current instance of a version.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public final void testApplyChangesCurrentVoVREVeAPVAW3() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        String testName = CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVREVeAPVAW3";
        ArquillianTestUtils.clearDatabase(ROLES);
        ArquillianTestUtils.loadDbUnitTestFile(ROLES, testName);
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, testName);
        ArquillianTestUtils.copyUploadsFilesForTest(testName);
        ArquillianTestUtils.copyTempFilesForTest(testName);
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 5);
        Vocabulary vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/" + testName + "/test-vocabulary.xml",
                ValidationMode.UPDATE);
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

//        ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//                testName + "-out.xml");
        ArquillianTestUtils.
        compareDatabaseCurrentAndExpectedContentsIgnoreTaskTimestamps(
                REGISTRY,
                "test/tests/" + testName + "/test-registry-results.xml");
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 4);
    }

    /** Test of applying changes to the current instance of a vocabulary
     * that does not have a draft instance.
     * Vocabulary, Version, VocabularyRelatedEntity,
     * AccessPoint, and VersionArtefact
     * model elements are used. Workflow processing is applied.
     * This test particularly exercises the ability to add more
     * than one file access point to the same version, at the same time.
     * See CC-2387.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public final void testApplyChangesCurrentVoVREVeAPVAW4() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        String testName = CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVREVeAPVAW4";
        ArquillianTestUtils.clearDatabase(ROLES);
        ArquillianTestUtils.loadDbUnitTestFile(ROLES, testName);
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, testName);
        ArquillianTestUtils.copyUploadsFilesForTest(testName);
        ArquillianTestUtils.copyTempFilesForTest(testName);
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 3);
        Vocabulary vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/" + testName + "/test-vocabulary.xml",
                ValidationMode.UPDATE);
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

//        ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//                testName + "-out.xml");
        ArquillianTestUtils.
        compareDatabaseCurrentAndExpectedContentsIgnoreTaskTimestamps(
                REGISTRY,
                "test/tests/" + testName + "/test-registry-results.xml");
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 3);
    }

    /** Test of adding a draft to a vocabulary
     * that has an existing published instance, and then publishing
     * the same.
     * Vocabulary, VocabularyRelatedEntity, Version,
     * AccessPoint, and VersionArtefact
     * model elements are used. Workflow processing is applied.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public final void testApplyChangesDraftVoVREVeAPVAW1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        String testName = CLASS_NAME_PREFIX
                + "testApplyChangesDraftVoVREVeAPVAW1";
        ArquillianTestUtils.clearDatabase(ROLES);
        ArquillianTestUtils.loadDbUnitTestFile(ROLES, testName);
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, testName);
        ArquillianTestUtils.copyUploadsFilesForTest(testName);
        ArquillianTestUtils.copyTempFilesForTest(testName);
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 3);
        Vocabulary vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/" + testName + "/test-vocabulary-1.xml",
                ValidationMode.UPDATE);
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

//        ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//                testName + "-1-out.xml");
        ArquillianTestUtils.compareDatabaseCurrentAndExpectedContents(
                REGISTRY,
                "test/tests/" + testName + "/test-registry-results-1.xml");
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 3);

        vocabulary = RegistryTestUtils.getValidatedVocabularyFromFile(
                "test/tests/" + testName + "/test-vocabulary-2.xml",
                ValidationMode.UPDATE);
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

//        ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//                testName + "-2-out.xml");
        ArquillianTestUtils.
        compareDatabaseCurrentAndExpectedContentsIgnoreTaskTimestamps(
                REGISTRY,
                "test/tests/" + testName + "/test-registry-results-2.xml");
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 3);
    }

    /** Test of updating a draft of a vocabulary
     * that already has both existing published and draft instances.
     * Vocabulary, VocabularyRelatedEntity, Version,
     * AccessPoint, and VersionArtefact
     * model elements are used. Workflow processing is applied.
     * In fact, that's not right: this test confirms that
     * <i>no</i> VersionArtefact
     * is created, and that the workflow is <i>not</i> run in this case,
     * since no processing is done when updating a draft.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testApplyChangesDraftVoVREVeAPVAW2() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        String testName = CLASS_NAME_PREFIX
                + "testApplyChangesDraftVoVREVeAPVAW2";
        ArquillianTestUtils.clearDatabase(ROLES);
        ArquillianTestUtils.loadDbUnitTestFile(ROLES, testName);
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, testName);
        ArquillianTestUtils.copyUploadsFilesForTest(testName);
        Vocabulary vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/" + testName + "/test-vocabulary.xml",
                ValidationMode.UPDATE);
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

//        ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//                testName + "-out.xml");
        ArquillianTestUtils.compareDatabaseCurrentAndExpectedContents(
                REGISTRY,
                "test/tests/" + testName + "/test-registry-results.xml");
    }

    /** Test of changing a version slug, when there is a Sesame
     * repository in play. In particular, make sure that after deletion
     * of the version, the Sesame repo is gone.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     * @throws OpenRDFException If there is a problem connecting with Sesame.
     *  */
    @Test
    public final void testChangeVersionSlug1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException,
    OpenRDFException {
        String testName = CLASS_NAME_PREFIX
                + "testChangeVersionSlug1";
        ArquillianTestUtils.clearDatabase(ROLES);
        ArquillianTestUtils.loadDbUnitTestFile(ROLES, testName);
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, testName);
        ArquillianTestUtils.copyUploadsFilesForTest(testName);
        Vocabulary vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/" + testName + "/test-vocabulary1.xml",
                ValidationMode.CREATE);
        EntityManager em = null;
        List<AccessPoint> aps = null;

        // Create the vocabulary.
        try {
            em = DBContext.getEntityManager();
            EntityTransaction txn = em.getTransaction();

            txn.begin();
            VocabularyId vocabularyId = new VocabularyId();
            VocabularyIdDAO.saveVocabularyId(em, vocabularyId);

            Integer newVocabularyId = vocabularyId.getId();
            Assert.assertEquals(newVocabularyId.intValue(), 1,
                    "Vocabulary ID not 1");

            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            ModelMethods.applyChanges(vm, "TEST", nowTime1, vocabulary);
            txn.commit();
            aps = AccessPointDAO.getCurrentAccessPointListForVersionByType(1,
                    AccessPointType.SESAME_DOWNLOAD, em);
        } catch (Exception e) {
            if (em != null) {
                em.getTransaction().rollback();
                throw e;
            }
        }
        ArquillianTestUtils.
        compareDatabaseCurrentAndExpectedContentsIgnoreTaskTimestamps(
                REGISTRY,
                "test/tests/" + testName + "/test-registry-results1.xml");
        vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/" + testName + "/test-vocabulary2.xml",
                ValidationMode.UPDATE);

        // Now confirm that the Sesame repo exists.
        Assert.assertEquals(aps.size(), 1, "Not exactly one sesameDownload "
                + "access point");
        ApSesameDownload apSesameDownload = JSONSerialization.
                deserializeStringAsJson(aps.get(0).getData(),
                        ApSesameDownload.class);
        String apServerBase = apSesameDownload.getServerBase();
        String apRepositoryID = apSesameDownload.getRepository();
        RepositoryManager manager = null;
        try {
            manager = RepositoryProvider.getRepositoryManager(apServerBase);
            Repository repository = manager.getRepository(apRepositoryID);
            Assert.assertNotNull(repository, "Repository missing");
        } catch (RepositoryConfigException | RepositoryException e) {
            throw e;
        }

        // Now change the version slug.
        try {
            EntityTransaction txn = em.getTransaction();
            txn.begin();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            ModelMethods.applyChanges(vm, "TEST", nowTime2, vocabulary);
            txn.commit();
        } catch (Exception e) {
            if (em != null) {
                em.getTransaction().rollback();
                throw e;
            }
        }
        ArquillianTestUtils.
        compareDatabaseCurrentAndExpectedContentsIgnoreTaskTimestamps(
                REGISTRY,
                "test/tests/" + testName + "/test-registry-results2.xml");

        // Now confirm that the Sesame repo still exists, and that
        // there isn't one with the _new_ slug.
        try {
            manager = RepositoryProvider.getRepositoryManager(apServerBase);
            Repository repository = manager.getRepository(apRepositoryID);
            Assert.assertNotNull(repository, "Repository no longer exists");
            repository = manager.getRepository("ands-curated_rifcs_version-2");
            Assert.assertNull(repository, "Repository with new version slug "
                    + "now exists");
        } catch (RepositoryConfigException | RepositoryException e) {
            throw e;
        }

        // Now delete the version.
        vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/" + testName + "/test-vocabulary3.xml",
                ValidationMode.UPDATE);
        try {
            EntityTransaction txn = em.getTransaction();
            txn.begin();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            ModelMethods.applyChanges(vm, "TEST", nowTime3, vocabulary);
            txn.commit();
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
        ArquillianTestUtils.
        compareDatabaseCurrentAndExpectedContentsIgnoreTaskTimestamps(
                REGISTRY,
                "test/tests/" + testName + "/test-registry-results3.xml");

        // Now confirm that the Sesame repo _no longer_ exists.
        try {
            manager = RepositoryProvider.getRepositoryManager(apServerBase);
            Repository repository = manager.getRepository(apRepositoryID);
            // CC-2596: There _was_ a defect in SesameImporterProvider
            // that has been fixed in the same commit that adds this
            // very test. Before the fix, the following assertion
            // failed. The unimport recalculated the repository name
            // from the slugs, rather than using the repository name
            // stored in the access point database row.
            Assert.assertNull(repository, "Repository still exists");
        } catch (RepositoryConfigException | RepositoryException e) {
            throw e;
        }
    }


    // Code to do a database dump; copy/paste and use as required
    // during development of a test.
//  ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//  "testDeleteOnlyPublished-out.xml");

}
