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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testDeleteOnlyCurrentVoVRE1");
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
        ArquillianTestUtils.compareDatabaseCurrentAndExpectedContents(
                REGISTRY,
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testDeleteOnlyCurrentVoVRE1/"
                + "test-registry-results.xml");
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testDeleteOnlyCurrentVoVREVRV1");
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
        ArquillianTestUtils.compareDatabaseCurrentAndExpectedContents(
                REGISTRY,
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testDeleteOnlyCurrentVoVREVRV1/"
                + "test-registry-results.xml");
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testDeleteOnlyCurrentVoVREVe1");
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
        ArquillianTestUtils.compareDatabaseCurrentAndExpectedContents(
                REGISTRY,
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testDeleteOnlyCurrentVoVREVe1/"
                + "test-registry-results.xml");
    }

    /** Test of deleting the current instance of a vocabulary that has only
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testDeleteOnlyDraftVoVRE1");
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
                + "testDeleteOnlyDraftVoVRE1/"
                + "test-registry-results.xml");
    }

    /** Test of deleting the current instance of a vocabulary that has only
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testDeleteOnlyDraftVoVREVRV1");
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
                + "testDeleteOnlyDraftVoVREVRV1/"
                + "test-registry-results.xml");
    }

    /** Test of deleting the current instance of a vocabulary that has only
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testDeleteOnlyDraftVoVREVe1");
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
                + "testDeleteOnlyDraftVoVREVe1/"
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
    public final void testMakeCurrentVocabularyDraftVoVRE1() throws
    DatabaseUnitException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testMakeCurrentVocabularyDraftVoVRE1");
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testMakeCurrentVocabularyDraftVoVRE1/"
                + "test-registry-results.xml");
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testMakeCurrentVocabularyDraftVoVREVRV1");
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testMakeCurrentVocabularyDraftVoVREVRV1/"
                + "test-registry-results.xml");
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testMakeCurrentVocabularyDraftVoVREVe1");
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testMakeCurrentVocabularyDraftVoVREVe1/"
                + "test-registry-results.xml");
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testDeleteDraftLeavingCurrentVoVRE1");
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testDeleteDraftLeavingCurrentVoVRE1/"
                + "test-registry-results.xml");
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testDeleteDraftLeavingCurrentVoVREVRV1");
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testDeleteDraftLeavingCurrentVoVREVRV1/"
                + "test-registry-results.xml");
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testDeleteDraftLeavingCurrentVoVREVe1");
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testDeleteDraftLeavingCurrentVoVREVe1/"
                + "test-registry-results.xml");
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testDeleteCurrentLeavingDraftVoVRE1");
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testDeleteCurrentLeavingDraftVoVRE1/"
                + "test-registry-results.xml");
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testDeleteCurrentLeavingDraftVoVREVRV1");
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testDeleteCurrentLeavingDraftVoVREVRV1/"
                + "test-registry-results.xml");
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testDeleteCurrentLeavingDraftVoVREVe1");
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testDeleteCurrentLeavingDraftVoVREVe1/"
                + "test-registry-results.xml");
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVRE1");
        Vocabulary vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/"
                        + CLASS_NAME_PREFIX
                        + "testApplyChangesCurrentVoVRE1/"
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVRE1/"
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
    public final void testApplyChangesCurrentVoVRE2() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVRE2");
        Vocabulary vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/"
                        + CLASS_NAME_PREFIX
                        + "testApplyChangesCurrentVoVRE2/"
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVRE2/"
                + "test-registry-results.xml");
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVREVRV1");
        Vocabulary vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/"
                        + CLASS_NAME_PREFIX
                        + "testApplyChangesCurrentVoVREVRV1/"
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVREVRV1/"
                + "test-registry-results.xml");
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVREVRV2");
        Vocabulary vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/"
                        + CLASS_NAME_PREFIX
                        + "testApplyChangesCurrentVoVREVRV2/"
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVREVRV2/"
                + "test-registry-results.xml");
    }

    /** Test of applying changes to the current instance of a vocabulary
     * that does not have a draft instance.
     * Vocabulary, Version, and VocabularyRelatedEntity
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVREVe1");
        Vocabulary vocabulary = RegistryTestUtils.
                getUnvalidatedVocabularyFromFile(
                "test/tests/"
                        + CLASS_NAME_PREFIX
                        + "testApplyChangesCurrentVoVREVe1/"
                        + "test-vocabulary.xml");
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVREVe1/"
                + "test-registry-results.xml");
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVREVe2");
        Vocabulary vocabulary = RegistryTestUtils.
                getUnvalidatedVocabularyFromFile(
                "test/tests/"
                        + CLASS_NAME_PREFIX
                        + "testApplyChangesCurrentVoVREVe2/"
                        + "test-vocabulary.xml");
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testApplyChangesCurrentVoVREVe2/"
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
    public final void testApplyChangesDraftVoVRE1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testApplyChangesDraftVoVRE1");
        Vocabulary vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/"
                        + CLASS_NAME_PREFIX
                        + "testApplyChangesDraftVoVRE1/"
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testApplyChangesDraftVoVRE1/"
                + "test-registry-results-1.xml");

        vocabulary = RegistryTestUtils.getValidatedVocabularyFromFile(
                "test/tests/"
                        + CLASS_NAME_PREFIX
                        + "testApplyChangesDraftVoVRE1/"
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testApplyChangesDraftVoVRE1/"
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
    public final void testApplyChangesDraftVoVRE2() throws
    DatabaseUnitException, IOException, SQLException, JAXBException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testApplyChangesDraftVoVRE2");
        Vocabulary vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/"
                        + CLASS_NAME_PREFIX
                        + "testApplyChangesDraftVoVRE2/"
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testApplyChangesDraftVoVRE2/"
                + "test-registry-results.xml");
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testApplyChangesDraftVoVREVRV1");
        Vocabulary vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/"
                        + CLASS_NAME_PREFIX
                        + "testApplyChangesDraftVoVREVRV1/"
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testApplyChangesDraftVoVREVRV1/"
                + "test-registry-results-1.xml");

        vocabulary = RegistryTestUtils.getValidatedVocabularyFromFile(
                "test/tests/"
                        + CLASS_NAME_PREFIX
                        + "testApplyChangesDraftVoVREVRV1/"
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testApplyChangesDraftVoVREVRV1/"
                + "test-registry-results-2.xml");
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testApplyChangesDraftVoVREVRV2");
        Vocabulary vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/"
                        + CLASS_NAME_PREFIX
                        + "testApplyChangesDraftVoVREVRV2/"
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testApplyChangesDraftVoVREVRV2/"
                + "test-registry-results.xml");
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testApplyChangesDraftVoVREVe1");
        Vocabulary vocabulary = RegistryTestUtils.
                getUnvalidatedVocabularyFromFile(
                "test/tests/"
                        + CLASS_NAME_PREFIX
                        + "testApplyChangesDraftVoVREVe1/"
                        + "test-vocabulary-1.xml");
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testApplyChangesDraftVoVREVe1/"
                + "test-registry-results-1.xml");

        vocabulary = RegistryTestUtils.getUnvalidatedVocabularyFromFile(
                "test/tests/"
                        + CLASS_NAME_PREFIX
                        + "testApplyChangesDraftVoVREVe1/"
                        + "test-vocabulary-2.xml");
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testApplyChangesDraftVoVREVe1/"
                + "test-registry-results-2.xml");
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testApplyChangesDraftVoVREVe2");
        Vocabulary vocabulary = RegistryTestUtils.
                getUnvalidatedVocabularyFromFile(
                "test/tests/"
                        + CLASS_NAME_PREFIX
                        + "testApplyChangesDraftVoVREVe2/"
                        + "test-vocabulary.xml");
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
                "test/tests/"
                + CLASS_NAME_PREFIX
                + "testApplyChangesDraftVoVREVe2/"
                + "test-registry-results.xml");
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testGetDraftVoVRE1");
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            // First time, get without REs.
            Vocabulary vocabularyAsSchema = ModelMethods.getDraft(vm,
                    false, false, false);
            String expectedString = ArquillianTestUtils.getResourceAsString(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + "testGetDraftVoVRE1/"
                            + "test-vocabulary-output-1.xml");
            String actualString =
                    RegistryTestUtils.serializeVocabularySchemaEntityToXML(
                            vocabularyAsSchema);
            MatcherAssert.assertThat(actualString,
                    Matchers.equalToIgnoringWhiteSpace(expectedString));
            // Second time, get with REs.
            vocabularyAsSchema = ModelMethods.getDraft(vm, false, false, true);
            expectedString = ArquillianTestUtils.getResourceAsString(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + "testGetDraftVoVRE1/"
                            + "test-vocabulary-output-2.xml");
            actualString =
                    RegistryTestUtils.serializeVocabularySchemaEntityToXML(
                            vocabularyAsSchema);
            MatcherAssert.assertThat(actualString,
                    Matchers.equalToIgnoringWhiteSpace(expectedString));
        } finally {
            if (em != null) {
                em.close();
            }
        }
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testGetDraftVoVRE2");
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            // First time, get without REs.
            Vocabulary vocabularyAsSchema = ModelMethods.getDraft(vm,
                    false, false, false);
            String expectedString = ArquillianTestUtils.getResourceAsString(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + "testGetDraftVoVRE2/"
                            + "test-vocabulary-output-1.xml");
            String actualString =
                    RegistryTestUtils.serializeVocabularySchemaEntityToXML(
                            vocabularyAsSchema);
            MatcherAssert.assertThat(actualString,
                    Matchers.equalToIgnoringWhiteSpace(expectedString));
            // Second time, get with REs.
            vocabularyAsSchema = ModelMethods.getDraft(vm, false, false, true);
            expectedString = ArquillianTestUtils.getResourceAsString(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + "testGetDraftVoVRE2/"
                            + "test-vocabulary-output-2.xml");
            actualString =
                    RegistryTestUtils.serializeVocabularySchemaEntityToXML(
                            vocabularyAsSchema);
            MatcherAssert.assertThat(actualString,
                    Matchers.equalToIgnoringWhiteSpace(expectedString));
        } finally {
            if (em != null) {
                em.close();
            }
        }
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testGetDraftVoVREVRV1");
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            // First time, get without REs.
            Vocabulary vocabularyAsSchema = ModelMethods.getDraft(vm,
                    false, false, false);
            String expectedString = ArquillianTestUtils.getResourceAsString(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + "testGetDraftVoVREVRV1/"
                            + "test-vocabulary-output-1.xml");
            String actualString =
                    RegistryTestUtils.serializeVocabularySchemaEntityToXML(
                            vocabularyAsSchema);
            MatcherAssert.assertThat(actualString,
                    Matchers.equalToIgnoringWhiteSpace(expectedString));
            // Second time, get with REs.
            vocabularyAsSchema = ModelMethods.getDraft(vm, false, false, true);
            expectedString = ArquillianTestUtils.getResourceAsString(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + "testGetDraftVoVREVRV1/"
                            + "test-vocabulary-output-2.xml");
            actualString =
                    RegistryTestUtils.serializeVocabularySchemaEntityToXML(
                            vocabularyAsSchema);
            MatcherAssert.assertThat(actualString,
                    Matchers.equalToIgnoringWhiteSpace(expectedString));
        } finally {
            if (em != null) {
                em.close();
            }
        }
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testGetDraftVoVREVRV2");
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            // First time, get without REs.
            Vocabulary vocabularyAsSchema = ModelMethods.getDraft(vm,
                    false, false, false);
            String expectedString = ArquillianTestUtils.getResourceAsString(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + "testGetDraftVoVREVRV2/"
                            + "test-vocabulary-output-1.xml");
            String actualString =
                    RegistryTestUtils.serializeVocabularySchemaEntityToXML(
                            vocabularyAsSchema);
            MatcherAssert.assertThat(actualString,
                    Matchers.equalToIgnoringWhiteSpace(expectedString));
            // Second time, get with REs.
            vocabularyAsSchema = ModelMethods.getDraft(vm, false, false, true);
            expectedString = ArquillianTestUtils.getResourceAsString(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + "testGetDraftVoVREVRV2/"
                            + "test-vocabulary-output-2.xml");
            actualString =
                    RegistryTestUtils.serializeVocabularySchemaEntityToXML(
                            vocabularyAsSchema);
            MatcherAssert.assertThat(actualString,
                    Matchers.equalToIgnoringWhiteSpace(expectedString));
        } finally {
            if (em != null) {
                em.close();
            }
        }
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testGetDraftVoVREVe1");
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            Vocabulary vocabularyAsSchema = ModelMethods.getDraft(vm,
                    true, false, false);
            String expectedString = ArquillianTestUtils.getResourceAsString(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + "testGetDraftVoVREVe1/"
                            + "test-vocabulary-output.xml");
            String actualString =
                    RegistryTestUtils.serializeVocabularySchemaEntityToXML(
                            vocabularyAsSchema);
            MatcherAssert.assertThat(actualString,
                    Matchers.equalToIgnoringWhiteSpace(expectedString));
        } finally {
            if (em != null) {
                em.close();
            }
        }
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
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + "testGetDraftVoVREVe2");
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            Vocabulary vocabularyAsSchema = ModelMethods.getDraft(vm,
                    true, false, false);
            String expectedString = ArquillianTestUtils.getResourceAsString(
                    "test/tests/"
                            + CLASS_NAME_PREFIX
                            + "testGetDraftVoVREVe2/"
                            + "test-vocabulary-output.xml");
            String actualString =
                    RegistryTestUtils.serializeVocabularySchemaEntityToXML(
                            vocabularyAsSchema);
            MatcherAssert.assertThat(actualString,
                    Matchers.equalToIgnoringWhiteSpace(expectedString));
        } finally {
            if (em != null) {
                em.close();
            }
        }
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
