/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.REGISTRY;
import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.ROLES;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.xml.bind.JAXBException;

import org.apache.solr.client.solrj.SolrServerException;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import au.org.ands.vocabs.registry.api.validation.ValidationMode;
import au.org.ands.vocabs.registry.db.context.DBContext;
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.dao.AccessPointDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyIdDAO;
import au.org.ands.vocabs.registry.db.entity.AccessPoint;
import au.org.ands.vocabs.registry.db.entity.VocabularyId;
import au.org.ands.vocabs.registry.db.internal.ApSesameDownload;
import au.org.ands.vocabs.registry.db.internal.ApSissvoc;
import au.org.ands.vocabs.registry.enums.AccessPointType;
import au.org.ands.vocabs.registry.model.ModelMethods;
import au.org.ands.vocabs.registry.model.VocabularyModel;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;
import au.org.ands.vocabs.registry.solr.EntityIndexer;
import au.org.ands.vocabs.registry.solr.SearchResourcesIndex;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;
import au.org.ands.vocabs.registry.workflow.tasks.TaskUtils;

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
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 5);
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
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 3);
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
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 5);
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
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 3);
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
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 5);
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
     * This test involves the deletion of the current instance of a version.
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
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 6);
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
        // One fewer file: the file "1.txt" was deleted.
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 5);
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

    /** Test of removing the only file access point from the current instance
     * of a vocabulary that does not have a draft instance.
     * Vocabulary, Version, VocabularyRelatedEntity,
     * AccessPoint, and VersionArtefact
     * model elements are used. Workflow processing is applied.
     * This test particularly exercises the need to run the concept
     * and resource docs transforms in this case.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public final void testApplyChangesCurrentVoVREVeAPVAW5() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        String testName = CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVREVeAPVAW5";
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

    /** Test of removing one of two file access points from the current
     * instance of a vocabulary that does not have a draft instance.
     * Vocabulary, Version, VocabularyRelatedEntity,
     * AccessPoint, and VersionArtefact
     * model elements are used. Workflow processing is applied.
     * This test particularly exercises the need to run the concept
     * and resource docs transforms in this case.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public final void testApplyChangesCurrentVoVREVeAPVAW6() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        String testName = CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVREVeAPVAW6";
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
     * This test involves the changing of the primary language of the
     * vocabulary. We expect the ConceptTree transform to be run
     * (only) for the version with status="current".
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public final void testApplyChangesCurrentVoVREVeAPVAW7() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        String testName = CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVREVeAPVAW7";
        ArquillianTestUtils.clearDatabase(ROLES);
        ArquillianTestUtils.loadDbUnitTestFile(ROLES, testName);
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, testName);
        ArquillianTestUtils.copyUploadsFilesForTest(testName);
        ArquillianTestUtils.copyTempFilesForTest(testName);
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 6);
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
        // The same number of files as before.
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 6);
    }

    /** Test of applying changes to the current instance of a vocabulary
     * that does not have a draft instance.
     * Vocabulary, Version, VocabularyRelatedEntity,
     * AccessPoint, and VersionArtefact
     * model elements are used. Workflow processing is applied.
     * This test involves the changing of the top concepts of the
     * vocabulary. We expect the ResourceDocs transform to be run
     * for all versions. The test data adds a top concept, where
     * there was none before, and there are no other concepts.
     * So we get a new resource_docs.json file generated for the
     * current version only, containing the top concept.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public final void testApplyChangesCurrentVoVREVeAPVAW8() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        String testName = CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVREVeAPVAW8";
        ArquillianTestUtils.clearDatabase(ROLES);
        ArquillianTestUtils.loadDbUnitTestFile(ROLES, testName);
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, testName);
        ArquillianTestUtils.copyUploadsFilesForTest(testName);
        ArquillianTestUtils.copyTempFilesForTest(testName);
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 6);
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
        // The same number of files as before.
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 6);
    }

    /** Test of applying <i>no</i>changes to the current instance
     * of a vocabulary that does not have a draft instance.
     * Vocabulary, Version, VocabularyRelatedEntity,
     * AccessPoint, and VersionArtefact
     * model elements are used. Workflow processing is applied.
     * This is a test of making <i>no</i> change. We expect
     * no task to be created/run.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public final void testApplyChangesCurrentVoVREVeAPVAW9() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        String testName = CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVREVeAPVAW9";
        ArquillianTestUtils.clearDatabase(ROLES);
        ArquillianTestUtils.loadDbUnitTestFile(ROLES, testName);
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, testName);
        ArquillianTestUtils.copyUploadsFilesForTest(testName);
        ArquillianTestUtils.copyTempFilesForTest(testName);
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 6);
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
        // The same number of files as before.
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 6);
    }

    /** Test of applying changes to the current instance
     * of a vocabulary that does not have a draft instance.
     * Vocabulary, Version, VocabularyRelatedEntity,
     * AccessPoint, and VersionArtefact
     * model elements are used. Workflow processing is applied.
     * This is a test of changing the browse flags.
     * Three versions (of which one is the current version) have
     * browse flags added.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public final void testApplyChangesCurrentVoVREVeAPVAW10() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        String testName = CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVREVeAPVAW10";
        ArquillianTestUtils.clearDatabase(ROLES);
        ArquillianTestUtils.loadDbUnitTestFile(ROLES, testName);
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, testName);
        ArquillianTestUtils.copyUploadsFilesForTest(testName);
        ArquillianTestUtils.copyTempFilesForTest(testName);
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 6);
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
        // The same number of files as before.
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 6);
    }

    /** Test of applying changes to the current instance
     * of a vocabulary that does not have a draft instance.
     * Vocabulary, Version, VocabularyRelatedEntity,
     * AccessPoint, and VersionArtefact
     * model elements are used. Workflow processing is applied.
     * This is a test of turning on the do-import flag for versions,
     * and then turning it off.
     * Three versions (of which one is the current version) have
     * the do-import flag turned on, where it was previously off.
     * Then, the flag is turned off for all versions.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public final void testApplyChangesCurrentVoVREVeAPVAW11() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        String testName = CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVREVeAPVAW11";
        ArquillianTestUtils.clearDatabase(ROLES);
        ArquillianTestUtils.loadDbUnitTestFile(ROLES, testName);
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, testName);
        ArquillianTestUtils.copyUploadsFilesForTest(testName);
        ArquillianTestUtils.copyTempFilesForTest(testName);
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 6);
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
        ArquillianTestUtils.
        compareDatabaseCurrentAndExpectedContentsIgnoreTaskTimestamps(
                REGISTRY,
                "test/tests/" + testName + "/test-registry-results-1.xml");
        // The same number of files as before.
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 6);

        // Now turn the do-import flag off again.
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
        // The same number of files as before.
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 6);
    }

    /** Test of applying changes to the current instance of a vocabulary
     * that does not have a draft instance.
     * Vocabulary, Version, VocabularyRelatedEntity,
     * AccessPoint, and VersionArtefact
     * model elements are used. Workflow processing is applied.
     * This test involves turning off the do-poolparty-harvest flag
     * for a version, where the do-import flag is, and remains set.
     * The version remains valid, because there is also a file
     * access point. But we require that the Sesame import task be
     * run, to have the effect of removing the vocabulary data that
     * was previously harvested.
     * This test currently fails. See CC-2704.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public final void testApplyChangesCurrentVoVREVeAPVAW12() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        String testName = CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVREVeAPVAW12";
        ArquillianTestUtils.clearDatabase(ROLES);
        ArquillianTestUtils.loadDbUnitTestFile(ROLES, testName);
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, testName);
        ArquillianTestUtils.copyUploadsFilesForTest(testName);
        ArquillianTestUtils.copyTempFilesForTest(testName);
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 9);
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
        // The same number of files as before.
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 9);
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
        // This test is OK, but doesn't prove very much:
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 3);
        // ... we also need to check that the files for the version artefacts
        // exist in the file system.
        TaskInfo taskInfo = TaskUtils.getTaskInfo(2);
        taskInfo.setNowTime(nowTime2);
        String taskPath = TaskUtils.getTaskOutputPath(taskInfo, false, null);
        try (Stream<Path> stream = Files.list(Paths.get(taskPath))) {
            // Use Files::isRegularFile to filter to just files, of which
            // there should be 3. (There is also the harvest_data directory.)
            Assert.assertEquals(stream.
                    filter(Files::isRegularFile).count(), 3,
                    "Expected to have 3 regular files for version artefacts");
        }
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

    /** Test of changing a version slug, when there are access points
     * in play that in some way use the version slug: both Sesame
     * and SISSVoc. We don't specify the force-workflow flag; we shouldn't
     * have to.
     * Nevertheless, we require that the access points that use the old
     * slug are deleted/made historical. But we also require that
     * the concept-related subtasks aren't re-rerun in this case;
     * they don't need to be.
     * Make sure that after deletion of the version, the Sesame repo is gone.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     * @throws OpenRDFException If there is a problem connecting with Sesame.
     * @throws SolrServerException If an error during Solr searching.
     *  */
    @Test
    @SuppressWarnings("checkstyle:MethodLength")
    public final void testChangeVersionSlug1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException,
    OpenRDFException, SolrServerException {
        String testName = CLASS_NAME_PREFIX
                + "testChangeVersionSlug1";
        ArquillianTestUtils.clearDatabase(ROLES);
        ArquillianTestUtils.loadDbUnitTestFile(ROLES, testName);
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, testName);
        ArquillianTestUtils.copyUploadsFilesForTest(testName);
        ArquillianTestUtils.removeAllSesameRepositories();

        Vocabulary vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/" + testName + "/test-vocabulary1.xml",
                ValidationMode.CREATE);
        EntityManager em = null;
        List<AccessPoint> sesameAPs = null;
        List<AccessPoint> sissvocAPsOldSlug = null;
        List<AccessPoint> sissvocAPsNewSlug = null;

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
            sesameAPs = AccessPointDAO.
                    getCurrentAccessPointListForVersionByType(1,
                    AccessPointType.SESAME_DOWNLOAD, em);
            sissvocAPsOldSlug = AccessPointDAO.
                    getCurrentAccessPointListForVersionByType(1,
                    AccessPointType.SISSVOC, em);
        } catch (Exception e) {
            if (em != null) {
                em.getTransaction().rollback();
                throw e;
            }
        }
//        ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//        testName + "-out1.xml");
        ArquillianTestUtils.
        compareDatabaseCurrentAndExpectedContentsIgnoreTaskTimestamps(
                REGISTRY,
                "test/tests/" + testName + "/test-registry-results-1.xml");

        // Confirm that the Sesame repo exists.
        Assert.assertEquals(sesameAPs.size(), 1,
                "Not exactly one sesameDownload access point");
        ApSesameDownload apSesameDownload = JSONSerialization.
                deserializeStringAsJson(sesameAPs.get(0).getData(),
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

        // Confirm that the SISSVoc spec file exists.
        Assert.assertEquals(sissvocAPsOldSlug.size(), 1,
                "Not exactly one sissvoc access point");
        ApSissvoc apSissvoc = JSONSerialization.
                deserializeStringAsJson(sissvocAPsOldSlug.get(0).getData(),
                        ApSissvoc.class);
        String apSissvocOldSlugPath = apSissvoc.getPath();
        Path apSissvocOldSlugPathPath = Paths.get(apSissvocOldSlugPath);
        Assert.assertTrue(Files.exists(apSissvocOldSlugPathPath),
                "SISSVoc spec file missing");

        // The following confirms (among other things) that the
        // sissvoc_endpoint in resource search results ends with
        // "...version-1", i.e., the correct version slug.
        EntityIndexer.indexAllVocabularies();
        EntityIndexer.commit();
        List<Object> filtersAndResultsExtracted = new ArrayList<>();
        String searchResults = SearchResourcesIndex.query("{}",
                filtersAndResultsExtracted, false);
//        logger.info("Result from resources index: " + searchResults);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode testJson = mapper.readTree(searchResults);
        JsonNode docsJson = testJson.get("response").get("docs");
        ArquillianTestUtils.compareJsonNodeWithFile(docsJson,
                ArquillianTestUtils.getClassesPath()
                + "/test/tests/" + testName + "/searchResultsDocs1.json");

        // Now change the version slug.
        vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/" + testName + "/test-vocabulary2.xml",
                ValidationMode.UPDATE);
        try {
            EntityTransaction txn = em.getTransaction();
            txn.begin();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            ModelMethods.applyChanges(vm, "TEST", nowTime2, vocabulary);
            txn.commit();
            sissvocAPsNewSlug = AccessPointDAO.
                    getCurrentAccessPointListForVersionByType(1,
                    AccessPointType.SISSVOC, em);
        } catch (Exception e) {
            if (em != null) {
                em.getTransaction().rollback();
                throw e;
            }
        }
//        ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//                testName + "-out2.xml");
        ArquillianTestUtils.
        compareDatabaseCurrentAndExpectedContentsIgnoreTaskTimestamps(
                REGISTRY,
                "test/tests/" + testName + "/test-registry-results-2.xml");

        // Now confirm that the Sesame repo for the _old_ slug no longer
        // exists, and that there _is_ one with the _new_ slug.
        try {
            manager = RepositoryProvider.getRepositoryManager(apServerBase);
            Repository repository = manager.getRepository(apRepositoryID);
            Assert.assertNull(repository,
                    "Repository with old version slug still exists");
            repository = manager.getRepository("ands-curated_rifcs_version-2");
            Assert.assertNotNull(repository,
                    "Repository with new version slug does not now exist");
        } catch (RepositoryConfigException | RepositoryException e) {
            throw e;
        }

        // Confirm that the SISSVoc spec file for the _old_ slug
        // is now an empty file, and that there is spec file
        // for the _new_ slug.
        Assert.assertTrue(Files.exists(apSissvocOldSlugPathPath),
                "SISSVoc spec file for old slug was deleted");
        Assert.assertTrue(Files.size(apSissvocOldSlugPathPath) == 0,
                "SISSVoc spec file for old slug is non-empty");
        Assert.assertEquals(sissvocAPsNewSlug.size(), 1,
                "Not exactly one sissvoc access point");
        apSissvoc = JSONSerialization.
                deserializeStringAsJson(sissvocAPsNewSlug.get(0).getData(),
                        ApSissvoc.class);
        String apSissvocNewSlugPath = apSissvoc.getPath();
        Path apSissvocNewSlugPathPath = Paths.get(apSissvocNewSlugPath);
        Assert.assertTrue(Files.exists(apSissvocNewSlugPathPath),
                "SISSVoc spec file for new slug does not now exist");

        // The sissvoc_endpoint in resource search results should now
        // end with "...version-2", i.e., with the updated version slug.
        // The point: the value changed, although we didn't re-run the
        // ResourceDocs transform. CC-2709.
        EntityIndexer.indexAllVocabularies();
        EntityIndexer.commit();
        filtersAndResultsExtracted = new ArrayList<>();
        searchResults = SearchResourcesIndex.query("{}",
                filtersAndResultsExtracted, false);
//        logger.info("Result from resources index: " + searchResults);
        testJson = mapper.readTree(searchResults);
        docsJson = testJson.get("response").get("docs");
        ArquillianTestUtils.compareJsonNodeWithFile(docsJson,
                ArquillianTestUtils.getClassesPath()
                + "/test/tests/" + testName + "/searchResultsDocs2.json");

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
//      ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//      testName + "-out3.xml");
        ArquillianTestUtils.
        compareDatabaseCurrentAndExpectedContentsIgnoreTaskTimestamps(
                REGISTRY,
                "test/tests/" + testName + "/test-registry-results-3.xml");

        // Confirm that the Sesame repo _no longer_ exists.
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

        // Confirm that the SISSVoc spec file for the _new_ slug
        // is now an empty file.
        Assert.assertTrue(Files.exists(apSissvocNewSlugPathPath),
                "SISSVoc spec file for new slug was deleted");
        Assert.assertTrue(Files.size(apSissvocNewSlugPathPath) == 0,
                "SISSVoc spec file for new slug is non-empty");
    }

    // Code to do a database dump; copy/paste and use as required
    // during development of a test.
//  ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//  testName + "-out.xml");

}
