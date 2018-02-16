/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.REGISTRY;
import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.ROLES;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.time.LocalDateTime;

import javax.persistence.EntityManager;
import javax.xml.bind.JAXBException;

import org.dbunit.DatabaseUnitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.api.validation.ValidationMode;
import au.org.ands.vocabs.registry.db.context.DBContext;
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

    /** Logger. */
    private static Logger logger;

    static {
        logger = LoggerFactory.getLogger(
                MethodHandles.lookup().lookupClass());
    }

    /** Name of this class, used in paths to test data files. */
    private static final String CLASS_NAME_PREFIX =
            "RegistryModelWorkflowTests.";

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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testDeleteOnlyDraftVoVREVeAPVAW1");
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

        ArquillianTestUtils.compareDatabaseCurrentAndExpectedContents(
                REGISTRY,
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testDeleteOnlyDraftVoVREVeAPVAW1/"
                + "test-registry-results.xml");
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

        ArquillianTestUtils.
        compareDatabaseCurrentAndExpectedContentsIgnoreTaskTimestamps(
                REGISTRY,
                "test/tests/" + testName + "/test-registry-results.xml");
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 4);
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

        ArquillianTestUtils.
        compareDatabaseCurrentAndExpectedContentsIgnoreTaskTimestamps(
                REGISTRY,
                "test/tests/" + testName + "/test-registry-results-2.xml");
        ArquillianTestUtils.assertTempForTestHasFiles(testName, 3);
    }

//  /** Test of updating a draft of a vocabulary
//   * that already has both existing published and draft instances.
//   * Vocabulary, VocabularyRelatedEntity, and Version
//   * model elements are used.
//   * @throws DatabaseUnitException If a problem with DbUnit.
//   * @throws IOException If a problem getting test data for DbUnit,
//   *          or reading JSON from the correct and test output files.
//   * @throws SQLException If DbUnit has a problem performing
//   *           performing JDBC operations.
//   * @throws JAXBException If a problem loading vocabulary data.
//   *  */
//  @Test
//  public final void testApplyChangesDraftVoVREVeAPVAW2() throws
//  DatabaseUnitException, IOException, SQLException, JAXBException {
//      ArquillianTestUtils.clearDatabase(REGISTRY);
//      ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
//              + "testApplyChangesDraftVoVREVeAPVAW2");
//      Vocabulary vocabulary = RegistryTestUtils.
//              getValidatedVocabularyFromFile(
//              "test/tests/"
//                      + CLASS_NAME_PREFIX
//                      + "testApplyChangesDraftVoVREVeAPVAW2/"
//                      + "test-vocabulary.xml",
//                      ValidationMode.UPDATE);
//      EntityManager em = null;
//      try {
//          em = DBContext.getEntityManager();
//          em.getTransaction().begin();
//          VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
//          ModelMethods.applyChanges(vm, "TEST", nowTime1, vocabulary);
//          em.getTransaction().commit();
//      } catch (Exception e) {
//          if (em != null) {
//              em.getTransaction().rollback();
//              throw e;
//          }
//      } finally {
//          if (em != null) {
//              em.close();
//          }
//      }
//
//    ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//            testName + "-out.xml");
//      ArquillianTestUtils.compareDatabaseCurrentAndExpectedContents(
//              REGISTRY,
//              "test/tests/"
//              + CLASS_NAME_PREFIX
//              + "testApplyChangesDraftVoVREVeAPVAW2/"
//              + "test-registry-results.xml");
//  }
//
//  /** Test of getting the draft of a vocabulary in registry schema format,
//   * where the vocabulary has only a draft instance.
//   * Vocabulary, VocabularyRelatedEntity, and Version
//   * model elements are used.
//   * @throws DatabaseUnitException If a problem with DbUnit.
//   * @throws IOException If a problem getting test data for DbUnit,
//   *          or reading JSON from the correct and test output files.
//   * @throws SQLException If DbUnit has a problem performing
//   *           performing JDBC operations.
//   * @throws JAXBException If a problem loading vocabulary data.
//   *  */
//  @Test
//  public final void testGetDraftVoVREVeAPVAW1() throws
//  DatabaseUnitException, IOException, SQLException, JAXBException {
//      ArquillianTestUtils.clearDatabase(REGISTRY);
//      ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
//              + "testGetDraftVoVREVeAPVAW1");
//      EntityManager em = null;
//      try {
//          em = DBContext.getEntityManager();
//          VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
//          Vocabulary vocabularyAsSchema = ModelMethods.getDraft(vm,
//                  true, false, false);
//          String expectedString = ArquillianTestUtils.getResourceAsString(
//                  "test/tests/"
//                          + CLASS_NAME_PREFIX
//                          + "testGetDraftVoVREVeAPVAW1/"
//                          + "test-vocabulary-output.xml");
//          String actualString =
//                  RegistryTestUtils.serializeVocabularySchemaEntityToXML(
//                          vocabularyAsSchema);
//          MatcherAssert.assertThat(actualString,
//                  Matchers.equalToIgnoringWhiteSpace(expectedString));
//      } finally {
//          if (em != null) {
//              em.close();
//          }
//      }
//  }
//
//  /** Test of getting the draft of a vocabulary in registry schema format,
//   * where the vocabulary has both a published and draft instance.
//   * Vocabulary, VocabularyRelatedEntity, and Version
//   * model elements are used.
//   * @throws DatabaseUnitException If a problem with DbUnit.
//   * @throws IOException If a problem getting test data for DbUnit,
//   *          or reading JSON from the correct and test output files.
//   * @throws SQLException If DbUnit has a problem performing
//   *           performing JDBC operations.
//   * @throws JAXBException If a problem loading vocabulary data.
//   *  */
//  @Test
//  public final void testGetDraftVoVREVeAPVAW2() throws
//  DatabaseUnitException, IOException, SQLException, JAXBException {
//      ArquillianTestUtils.clearDatabase(REGISTRY);
//      ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
//              + "testGetDraftVoVREVeAPVAW2");
//      EntityManager em = null;
//      try {
//          em = DBContext.getEntityManager();
//          VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
//          Vocabulary vocabularyAsSchema = ModelMethods.getDraft(vm,
//                  true, false, false);
//          String expectedString = ArquillianTestUtils.getResourceAsString(
//                  "test/tests/"
//                          + CLASS_NAME_PREFIX
//                          + "testGetDraftVoVREVeAPVAW2/"
//                          + "test-vocabulary-output.xml");
//          String actualString =
//                  RegistryTestUtils.serializeVocabularySchemaEntityToXML(
//                          vocabularyAsSchema);
//          MatcherAssert.assertThat(actualString,
//                  Matchers.equalToIgnoringWhiteSpace(expectedString));
//      } finally {
//          if (em != null) {
//              em.close();
//          }
//      }
//  }

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
