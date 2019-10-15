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
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.api.validation.ValidationMode;
import au.org.ands.vocabs.registry.db.context.DBContext;
import au.org.ands.vocabs.registry.model.ModelMethods;
import au.org.ands.vocabs.registry.model.VocabularyModel;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;

/** Tests of the registry model.
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
 * For example: the method {@link #testApplyChangesCurrentVoVRE1()} is a test
 * of the
 * {@link ModelMethods#applyChanges(VocabularyModel, String, LocalDateTime,
 *  Vocabulary)} method that exercises {@link VocabularyModel}
 * and
 * {@link au.org.ands.vocabs.registry.model.VocabularyRelatedEntitiesModel},
 * and it is the first test of that combination.
 *
 * There used to be a strict distinction between tests in this class
 * and those in RegistryModelWorkflowTests, namely: the tests in this class
 * did not use the workflow, i.e., they did not use any tasks.
 * However, that distinction no longer applies.
 */
@Test
public class RegistryModelTests extends ArquillianBaseTest {

    /** Logger. */
    private static Logger logger;

    static {
        logger = LoggerFactory.getLogger(
                MethodHandles.lookup().lookupClass());
    }

    /** Name of this class, used in paths to test data files. */
    private static final String CLASS_NAME_PREFIX = "RegistryModelTests.";

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

    /** Test script of deleting the current instance of a vocabulary
     * that has only a current instance.
     * @param testName The name of the test calling this script. The name
     *      is included as part of the directory name used for loading
     *      test data.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    private void scriptDeleteOnlyCurrent1(final String testName) throws
    DatabaseUnitException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + testName);
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
        ArquillianTestUtils.compareDatabaseCurrentAndExpectedContents(
                REGISTRY,
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-registry-results.xml");
    }

    /** Test of deleting the current instance of a vocabulary that has only
     * a current instance, with Vocabulary and VocabularyRelatedEntity
     * model elements.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testDeleteOnlyCurrentVoVRE1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptDeleteOnlyCurrent1("testDeleteOnlyCurrentVoVRE1");
    }

    /** Test of deleting the current instance of a vocabulary that has only
     * a current instance, with Vocabulary, VocabularyRelatedEntity,
     * and VocabularyRelatedVocabulary model elements.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testDeleteOnlyCurrentVoVREVRV1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptDeleteOnlyCurrent1("testDeleteOnlyCurrentVoVREVRV1");
    }

    /** Test of deleting the current instance of a vocabulary that has only
     * a current instance, with Vocabulary, VocabularyRelatedEntity,
     * and VocabularyRelatedVocabulary model elements.
     * This method also tests what happens to other vocabularies
     * that are related to the now-deleted current instance, in particular,
     * that those vocabularies no longer have VocabularyRelatedVocabulary
     * instances that refer to the now-deleted vocabulary.
     * See CC-2440.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testDeleteCurrentReverseVRV1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptDeleteOnlyCurrent1("testDeleteCurrentReverseVRV1");
    }

    /** Test of deleting the current instance of a vocabulary that has only
     * a current instance, with Vocabulary, VocabularyRelatedEntity, and
     * Version model elements.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testDeleteOnlyCurrentVoVREVe1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptDeleteOnlyCurrent1("testDeleteOnlyCurrentVoVREVe1");
    }

    /** Test of deleting the current instance of a vocabulary that has only
     * a current instance, with Vocabulary, VocabularyRelatedEntity,
     * Version, and AccessPoint model elements.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testDeleteOnlyCurrentVoVREVeAP1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptDeleteOnlyCurrent1("testDeleteOnlyCurrentVoVREVeAP1");
    }

    /** Test script of deleting the draft instance of a vocabulary
     * that has only a draft instance.
     * @param testName The name of the test calling this script. The name
     *      is included as part of the directory name used for loading
     *      test data.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    private void scriptDeleteOnlyDraft1(final String testName) throws
    DatabaseUnitException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + testName);
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-registry-results.xml");
    }

    /** Test of deleting the draft instance of a vocabulary that has only
     * a draft instance, with Vocabulary and VocabularyRelatedEntity
     * model elements.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testDeleteOnlyDraftVoVRE1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptDeleteOnlyDraft1("testDeleteOnlyDraftVoVRE1");
    }

    /** Test of deleting the draft instance of a vocabulary that has only
     * a draft instance, with Vocabulary, VocabularyRelatedEntity, and
     * VocabularyRelatedVocabulary model elements.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testDeleteOnlyDraftVoVREVRV1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptDeleteOnlyDraft1("testDeleteOnlyDraftVoVREVRV1");
    }

    /** Test of deleting the draft instance of a vocabulary that has only
     * a draft instance, with Vocabulary, VocabularyRelatedEntity, and
     * Version model elements.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testDeleteOnlyDraftVoVREVe1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptDeleteOnlyDraft1("testDeleteOnlyDraftVoVREVe1");
    }

    /** Test of deleting the draft instance of a vocabulary that has only
     * a draft instance, with Vocabulary, VocabularyRelatedEntity, Version,
     * and AccessPoint model elements.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testDeleteOnlyDraftVoVREVeAP1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptDeleteOnlyDraft1("testDeleteOnlyDraftVoVREVeAP1");
    }

    /** Test script of starting with a vocabulary that has only
     * a current instance, and making that current instance into
     * a draft only.
     * @param testName The name of the test calling this script. The name
     *      is included as part of the directory name used for loading
     *      test data.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    private void scriptMakeCurrentVocabularyDraft1(final String testName)
            throws DatabaseUnitException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + testName);
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
        ArquillianTestUtils.compareDatabaseCurrentAndExpectedContents(
                REGISTRY,
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-registry-results.xml");
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
    public final void testMakeCurrentVocabularyDraftVoVRE1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptMakeCurrentVocabularyDraft1(
                "testMakeCurrentVocabularyDraftVoVRE1");
    }

    /** Test of starting with a vocabulary that has only
     * a current instance, and making that current instance into
     * a draft only. Only Vocabulary, VocabularyRelatedEntity, and
     * VocabularyRelatedVocabulary model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testMakeCurrentVocabularyDraftVoVREVRV1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptMakeCurrentVocabularyDraft1(
                "testMakeCurrentVocabularyDraftVoVREVRV1");
    }

    /** Test of starting with a vocabulary that has only
     * a current instance, and making that current instance into
     * a draft only. Only Vocabulary, VocabularyRelatedEntity, and
     * VocabularyRelatedVocabulary model elements are used.
     * This method also tests what happens to other vocabularies
     * that are related to the now-deleted current instance, in particular,
     * that those vocabularies no longer have VocabularyRelatedVocabulary
     * instances that refer to the now-deleted vocabulary.
     * See CC-2440.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testMakeCurrentVocabularyDraftReverseVRV1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptMakeCurrentVocabularyDraft1(
                "testMakeCurrentVocabularyDraftReverseVRV1");
    }

    /** Test of starting with a vocabulary that has only
     * a current instance, and making that current instance into
     * a draft only. Vocabulary, VocabularyRelatedEntity, and Version
     * model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testMakeCurrentVocabularyDraftVoVREVe1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptMakeCurrentVocabularyDraft1(
                "testMakeCurrentVocabularyDraftVoVREVe1");
    }

    /** Test of starting with a vocabulary that has only
     * a current instance, and making that current instance into
     * a draft only. Vocabulary, VocabularyRelatedEntity, Version,
     * and AccessPoint model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testMakeCurrentVocabularyDraftVoVREVeAP1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptMakeCurrentVocabularyDraft1(
                "testMakeCurrentVocabularyDraftVoVREVeAP1");
    }

    /** Test script of deleting the draft instance of a vocabulary
     * that also has a current instance.
     * @param testName The name of the test calling this script. The name
     *      is included as part of the directory name used for loading
     *      test data.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    private void scriptDeleteDraftLeavingCurrent1(final String testName) throws
    DatabaseUnitException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + testName);
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-registry-results.xml");
    }

    /** Test of deleting the draft instance of a vocabulary that also has
     * a current instance, with Vocabulary and VocabularyRelatedEntity
     * model elements.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testDeleteDraftLeavingCurrentVoVRE1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptDeleteDraftLeavingCurrent1(
                "testDeleteDraftLeavingCurrentVoVRE1");
    }

    /** Test of deleting the draft instance of a vocabulary that also has
     * a current instance, with Vocabulary, VocabularyRelatedEntity,
     * and VocabularyRelatedVocabulary model elements.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testDeleteDraftLeavingCurrentVoVREVRV1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptDeleteDraftLeavingCurrent1(
                "testDeleteDraftLeavingCurrentVoVREVRV1");
    }

    /** Test of deleting the draft instance of a vocabulary that also has
     * a current instance, with Vocabulary, VocabularyRelatedEntity, and
     * Version model elements.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testDeleteDraftLeavingCurrentVoVREVe1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptDeleteDraftLeavingCurrent1(
                "testDeleteDraftLeavingCurrentVoVREVe1");
    }

    /** Test of deleting the draft instance of a vocabulary that also has
     * a current instance, with Vocabulary, VocabularyRelatedEntity,
     * Version, and AccessPoint model elements.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testDeleteDraftLeavingCurrentVoVREVeAP1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptDeleteDraftLeavingCurrent1(
                "testDeleteDraftLeavingCurrentVoVREVeAP1");
    }

    /** Test script of deleting the current instance of a vocabulary
     * that also has a draft instance.
     * @param testName The name of the test calling this script. The name
     *      is included as part of the directory name used for loading
     *      test data.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    private void scriptDeleteCurrentLeavingDraft1(final String testName) throws
    DatabaseUnitException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + testName);
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
        ArquillianTestUtils.compareDatabaseCurrentAndExpectedContents(
                REGISTRY,
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-registry-results.xml");
    }

    /** Test of deleting the current instance of a vocabulary that also has
     * a draft instance, with Vocabulary and VocabularyRelatedEntity
     * model elements.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testDeleteCurrentLeavingDraftVoVRE1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptDeleteCurrentLeavingDraft1(
                "testDeleteCurrentLeavingDraftVoVRE1");
    }

    /** Test of deleting the current instance of a vocabulary that also has
     * a draft instance, with Vocabulary, VocabularyRelatedEntity, and
     * VocabularyRelatedVocabulary model elements.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testDeleteCurrentLeavingDraftVoVREVRV1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptDeleteCurrentLeavingDraft1(
                "testDeleteCurrentLeavingDraftVoVREVRV1");
    }

    /** Test of deleting the current instance of a vocabulary that also has
     * a draft instance, with Vocabulary, VocabularyRelatedEntity, and
     * Version model elements.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testDeleteCurrentLeavingDraftVoVREVe1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptDeleteCurrentLeavingDraft1(
                "testDeleteCurrentLeavingDraftVoVREVe1");
    }

    /** Test of deleting the current instance of a vocabulary that also has
     * a draft instance, with Vocabulary, VocabularyRelatedEntity,
     * Version, and AccessPoint model elements.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testDeleteCurrentLeavingDraftVoVREVeAP1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptDeleteCurrentLeavingDraft1(
                "testDeleteCurrentLeavingDraftVoVREVeAP1");
    }

    /** Test script of applying changes to the current instance of a vocabulary
     * that does not have a draft instance.
     * @param testName The name of the test calling this script. The name
     *      is included as part of the directory name used for loading
     *      test data.
     * @param validate Whether or not the test vocabulary data should
     *      be validated before use.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    private void scriptApplyChangesCurrent1(final String testName,
            final boolean validate) throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + testName);
        Vocabulary vocabulary;
        if (validate) {
            vocabulary = RegistryTestUtils.getValidatedVocabularyFromFile(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + testName
                            + "/test-vocabulary.xml", ValidationMode.UPDATE);
        } else {
            vocabulary = RegistryTestUtils.getUnvalidatedVocabularyFromFile(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + testName
                            + "/test-vocabulary.xml");
        }
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            em.getTransaction().begin();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            ModelMethods.applyChanges(vm, "TEST", nowTime1, vocabulary);
            em.getTransaction().commit();
        } catch (Exception e) {
            logger.error("Error: ", e);
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-registry-results.xml");
    }

    /** Test of applying changes to the current instance of a vocabulary
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
    public final void testApplyChangesCurrentVoVRE1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptApplyChangesCurrent1("testApplyChangesCurrentVoVRE1", true);
    }

    /** Test of applying changes to the current instance of a vocabulary
     * that does not have a draft instance.
     * Only Vocabulary, VocabularyRelatedEntity, and
     * VocabularyRelatedVocabulary model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testApplyChangesCurrentVoVREVRV1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptApplyChangesCurrent1("testApplyChangesCurrentVoVREVRV1", true);
    }

    /** Test of applying changes to the current instance of a vocabulary
     * that does not have a draft instance.
     * Vocabulary, VocabularyRelatedEntity, and Version
     * model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testApplyChangesCurrentVoVREVe1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptApplyChangesCurrent1("testApplyChangesCurrentVoVREVe1", false);
    }

    /** Test of applying changes to the current instance of a vocabulary
     * that does not have a draft instance.
     * Vocabulary, VocabularyRelatedEntity, Version, and AccessPoint
     * model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testApplyChangesCurrentVoVREVeAP1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptApplyChangesCurrent1("testApplyChangesCurrentVoVREVeAP1", true);
    }

    /** Test script of starting with a vocabulary that has only a draft
     * instance, and applying a change that makes it published.
     * @param testName The name of the test calling this script. The name
     *      is included as part of the directory name used for loading
     *      test data.
     * @param validate Whether or not the test vocabulary data should
     *      be validated before use.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    private void scriptApplyChangesCurrent2(final String testName,
            final boolean validate) throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + testName);
        Vocabulary vocabulary;
        if (validate) {
            vocabulary = RegistryTestUtils.getValidatedVocabularyFromFile(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + testName
                            + "/test-vocabulary.xml", ValidationMode.UPDATE);
        } else {
            vocabulary = RegistryTestUtils.getUnvalidatedVocabularyFromFile(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + testName
                            + "/test-vocabulary.xml");
        }
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            em.getTransaction().begin();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            ModelMethods.applyChanges(vm, "TEST", nowTime1, vocabulary);
            em.getTransaction().commit();
        } catch (Exception e) {
            logger.error("Error: ", e);
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-registry-results.xml");
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
    public final void testApplyChangesCurrentVoVRE2() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptApplyChangesCurrent2("testApplyChangesCurrentVoVRE2", true);
    }

    /** Test of starting with a vocabulary that has only a draft instance,
     * and applying a change that makes it published.
     * Only Vocabulary, VocabularyRelatedEntity, and
     * VocabularyRelatedVocabulary model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testApplyChangesCurrentVoVREVRV2() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptApplyChangesCurrent2("testApplyChangesCurrentVoVREVRV2", true);
    }

    /** Test of starting with a vocabulary that has only a draft instance,
     * and applying a change that makes it published.
     * Vocabulary, VocabularyRelatedEntity, and Version
     * model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testApplyChangesCurrentVoVREVe2() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptApplyChangesCurrent2("testApplyChangesCurrentVoVREVe2", false);
    }

    /** Test of starting with a vocabulary that has only a draft instance,
     * and applying a change that makes it published.
     * Vocabulary, VocabularyRelatedEntity, Version, and AccessPoint
     * model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testApplyChangesCurrentVoVREVeAP2() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptApplyChangesCurrent2("testApplyChangesCurrentVoVREVeAP2", true);
    }

    /** Test of applying changes to the current instance of a vocabulary
     * that does not have a draft instance.
     * Vocabulary, VocabularyRelatedEntity, Version, and AccessPoint
     * model elements are used.
     * This test particularly exercises the ability to add more
     * than one access point to the same version, at the same time.
     * See CC-2387. This test adds two apiSparql access points.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testApplyChangesCurrentVoVREVeAP3() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptApplyChangesCurrent1("testApplyChangesCurrentVoVREVeAP3", true);
    }

    /** Test of applying changes to the current instance of a vocabulary
     * that does not have a draft instance.
     * Vocabulary, VocabularyRelatedEntity, Version, and AccessPoint
     * model elements are used.
     * This test particularly exercises the ability to add more
     * than one access point to the same version, at the same time.
     * See CC-2387. This test adds two sissvoc access points.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testApplyChangesCurrentVoVREVeAP4() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptApplyChangesCurrent1("testApplyChangesCurrentVoVREVeAP4", true);
    }

    /** Test of applying changes to the current instance of a vocabulary
     * that does not have a draft instance.
     * Vocabulary, VocabularyRelatedEntity, Version, and AccessPoint
     * model elements are used.
     * This test particularly exercises the ability to add more
     * than one access point to the same version, at the same time.
     * See CC-2387. This test adds two webPage access points.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testApplyChangesCurrentVoVREVeAP5() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptApplyChangesCurrent1("testApplyChangesCurrentVoVREVeAP5", true);
    }

    /** Test of applying changes to the current instance of a vocabulary
     * that does not have a draft instance.
     * Vocabulary, VocabularyRelatedEntity, Version, and AccessPoint
     * model elements are used.
     * This test particularly exercises the ability to add more
     * than one access point to the same version, at the same time.
     * See CC-2387. This test adds two access points of <i>different</i>
     * types; in this case, an apiSparql and a webPage.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testApplyChangesCurrentVoVREVeAP6() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptApplyChangesCurrent1("testApplyChangesCurrentVoVREVeAP6", true);
    }

    /** Test script of adding a draft to a vocabulary
     * that has an existing published instance, and then publishing
     * the same.
     * @param testName The name of the test calling this script. The name
     *      is included as part of the directory name used for loading
     *      test data.
     * @param validate Whether or not the test vocabulary data should
     *      be validated before use.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    private void scriptApplyChangesDraft1(final String testName,
            final boolean validate) throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + testName);
        Vocabulary vocabulary;
        if (validate) {
            vocabulary = RegistryTestUtils.getValidatedVocabularyFromFile(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + testName
                            + "/test-vocabulary-1.xml", ValidationMode.UPDATE);
        } else {
            vocabulary = RegistryTestUtils.getUnvalidatedVocabularyFromFile(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + testName
                            + "/test-vocabulary-1.xml");
        }
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            em.getTransaction().begin();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            ModelMethods.applyChanges(vm, "TEST", nowTime1, vocabulary);
            em.getTransaction().commit();
        } catch (Exception e) {
            logger.error("Error: ", e);
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-registry-results-1.xml");

        if (validate) {
            vocabulary = RegistryTestUtils.getValidatedVocabularyFromFile(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + testName
                            + "/test-vocabulary-2.xml", ValidationMode.UPDATE);
        } else {
            vocabulary = RegistryTestUtils.getUnvalidatedVocabularyFromFile(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + testName
                            + "/test-vocabulary-2.xml");
        }
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-registry-results-2.xml");
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
    public final void testApplyChangesDraftVoVRE1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptApplyChangesDraft1("testApplyChangesDraftVoVRE1", true);
    }

    /** Test of adding a draft to a vocabulary
     * that has an existing published instance, and then publishing
     * the same.
     * Only Vocabulary, VocabularyRelatedEntity, and
     * VocabularyRelatedVocabulary model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testApplyChangesDraftVoVREVRV1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptApplyChangesDraft1("testApplyChangesDraftVoVREVRV1", true);
    }

    /** Test of adding a draft to a vocabulary
     * that has an existing published instance, and then publishing
     * the same.
     * Vocabulary, VocabularyRelatedEntity, and Version
     * model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testApplyChangesDraftVoVREVe1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptApplyChangesDraft1("testApplyChangesDraftVoVREVe1", false);
    }

    /** Test of adding a draft to a vocabulary
     * that has an existing published instance, and then publishing
     * the same.
     * Vocabulary, VocabularyRelatedEntity, Version, and AccessPoint
     * model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testApplyChangesDraftVoVREVeAP1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptApplyChangesDraft1("testApplyChangesDraftVoVREVeAP1", true);
    }


    /** Test script of updating a draft of a vocabulary
     * that already has both existing published and draft instances.
     * @param testName The name of the test calling this script. The name
     *      is included as part of the directory name used for loading
     *      test data.
     * @param validate Whether or not the test vocabulary data should
     *      be validated before use.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    private void scriptApplyChangesDraft2(final String testName,
            final boolean validate) throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + testName);
        Vocabulary vocabulary;
        if (validate) {
            vocabulary = RegistryTestUtils.getValidatedVocabularyFromFile(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + testName
                            + "/test-vocabulary.xml", ValidationMode.UPDATE);
        } else {
            vocabulary = RegistryTestUtils.getUnvalidatedVocabularyFromFile(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + testName
                            + "/test-vocabulary.xml");
        }
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            em.getTransaction().begin();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            ModelMethods.applyChanges(vm, "TEST", nowTime1, vocabulary);
            em.getTransaction().commit();
        } catch (Exception e) {
            logger.error("Error: ", e);
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-registry-results.xml");
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
    public final void testApplyChangesDraftVoVRE2() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptApplyChangesDraft2("testApplyChangesDraftVoVRE2", true);
    }

    /** Test of updating a draft of a vocabulary
     * that already has both existing published and draft instances.
     * Only Vocabulary, VocabularyRelatedEntity, and
     * VocabularyRelatedVocabulary model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testApplyChangesDraftVoVREVRV2() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptApplyChangesDraft2("testApplyChangesDraftVoVREVRV2", true);
    }

    /** Test of updating a draft of a vocabulary
     * that already has both existing published and draft instances.
     * Vocabulary, VocabularyRelatedEntity, and Version
     * model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testApplyChangesDraftVoVREVe2() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptApplyChangesDraft2("testApplyChangesDraftVoVREVe2", false);
    }

    /** Test of updating a draft of a vocabulary
     * that already has both existing published and draft instances.
     * Vocabulary, VocabularyRelatedEntity, Version, and AccessPoint
     * model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testApplyChangesDraftVoVREVeAP2() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptApplyChangesDraft2("testApplyChangesDraftVoVREVeAP2", true);
    }

    /** Test script of getting the draft of a vocabulary in registry schema
     * format.
     * @param testName The name of the test calling this script. The name
     *      is included as part of the directory name used for loading
     *      test data.
     * @param withVersions Whether invocations of getDraft should ask
     *      for versions.
     * @param withAPs Whether invocations of getDraft should ask
     *      for access points.
     * @param withREs Whether a second invocation of getDraft should be
     *      done that includes related entities.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    private void scriptGetDraft(final String testName,
            final boolean withVersions,
            final boolean withAPs,
            final boolean withREs) throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + testName);
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            // First time, get without REs.
            Vocabulary vocabularyAsSchema = ModelMethods.getDraft(vm,
                    withVersions, withAPs, false);
            // If withREs, use "-1" in filename of the first output file.
            String outputFilename;
            if (withREs) {
                outputFilename = "/test-vocabulary-output-1.xml";
            } else {
                outputFilename = "/test-vocabulary-output.xml";
            }
            String expectedString = ArquillianTestUtils.getResourceAsString(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + testName
                            + outputFilename);
            String actualString =
                    RegistryTestUtils.serializeVocabularySchemaEntityToXML(
                            vocabularyAsSchema);
            MatcherAssert.assertThat(actualString,
                    Matchers.equalToIgnoringWhiteSpace(expectedString));
            if (withREs) {
                // Second time, get with REs.
                vocabularyAsSchema = ModelMethods.getDraft(vm,
                        withVersions, withAPs, true);
                expectedString = ArquillianTestUtils.getResourceAsString(
                        "test/tests/"
                                + CLASS_NAME_PREFIX
                                + testName
                                + "/test-vocabulary-output-2.xml");
                actualString =
                        RegistryTestUtils.serializeVocabularySchemaEntityToXML(
                                vocabularyAsSchema);
                MatcherAssert.assertThat(actualString,
                        Matchers.equalToIgnoringWhiteSpace(expectedString));
            }
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /** Test of getting the draft of a vocabulary in registry schema format,
     * where the vocabulary has only a draft instance.
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
    public final void testGetDraftVoVRE1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptGetDraft("testGetDraftVoVRE1", false, false, true);
    }

    /** Test of getting the draft of a vocabulary in registry schema format,
     * where the vocabulary has only a draft instance.
     * Only Vocabulary, VocabularyRelatedEntity, and
     * VocabularyRelatedVocabulary model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testGetDraftVoVREVRV1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptGetDraft("testGetDraftVoVREVRV1", false, false, true);
    }

    /** Test of getting the draft of a vocabulary in registry schema format,
     * where the vocabulary has only a draft instance.
     * Vocabulary, VocabularyRelatedEntity, and Version
     * model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testGetDraftVoVREVe1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptGetDraft("testGetDraftVoVREVe1", true, false, false);
    }

    /** Test of getting the draft of a vocabulary in registry schema format,
     * where the vocabulary has only a draft instance.
     * Vocabulary, VocabularyRelatedEntity, Version, and AccessPoint
     * model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testGetDraftVoVREVeAP1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptGetDraft("testGetDraftVoVREVeAP1", true, true, false);
    }

    /** Test of getting the draft of a vocabulary in registry schema format,
     * where the vocabulary has both a published and draft instance.
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
    public final void testGetDraftVoVRE2() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptGetDraft("testGetDraftVoVRE2", false, false, true);
    }

    /** Test of getting the draft of a vocabulary in registry schema format,
     * where the vocabulary has both a published and draft instance.
     * Only Vocabulary, VocabularyRelatedEntity, and
     * VocabularyRelatedVocabulary model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testGetDraftVoVREVRV2() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptGetDraft("testGetDraftVoVREVRV2", false, false, true);
    }

    /** Test of getting the draft of a vocabulary in registry schema format,
     * where the vocabulary has both a published and draft instance.
     * Vocabulary, VocabularyRelatedEntity, and Version
     * model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testGetDraftVoVREVe2() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptGetDraft("testGetDraftVoVREVe2", true, false, false);
    }

    /** Test of getting the draft of a vocabulary in registry schema format,
     * where the vocabulary has both a published and draft instance.
     * Vocabulary, VocabularyRelatedEntity, Version, and AccessPoint
     * model elements are used.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     *  */
    @Test
    public final void testGetDraftVoVREVeAP2() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        scriptGetDraft("testGetDraftVoVREVeAP2", true, true, false);
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
