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
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.xml.bind.JAXBException;

import org.apache.solr.client.solrj.SolrServerException;
import org.dbunit.DatabaseUnitException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
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
import au.org.ands.vocabs.registry.db.internal.ApSissvoc;
import au.org.ands.vocabs.registry.enums.AccessPointType;
import au.org.ands.vocabs.registry.model.ModelMethods;
import au.org.ands.vocabs.registry.model.VocabularyModel;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;

/** Tests of the registry model that also involve workflow processing.
 * The tests in this class particularly exercise the sequence in which
 * tasks and subtasks are executed.
 */
@Test
public class RegistryWorkflowTaskTests extends ArquillianBaseTest {

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
            "RegistryWorkflowTaskTests.";

    /** A convenient value to use for endDate properties. */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static LocalDateTime nowTime1 =
            LocalDateTime.of(2017, 10, 1, 10, 10);

    /** A convenient value to use for endDate properties. */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static LocalDateTime nowTime2 =
            LocalDateTime.of(2017, 10, 1, 10, 20);

    /** Test of deleting and adding a version in the same step, where
     * the version title, and therefore the version slug, is the same.
     * It's necessary to ensure that the task for the deleted version
     * is done <i>before</i> the task for the new version. Otherwise,
     * the net result can be the loss of the external content underlying some
     * of the access points. E.g., we could have the Sesame and SISSVoc
     * access points for the new version in the database,
     * but the Sesame repository and SISSVoc spec file having been deleted.
     * CC-2917.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     * @throws OpenRDFException If there is a problem connecting with Sesame.
     * @throws SolrServerException If an error during Solr searching.
     *  */
    @Test(enabled = false)
    @SuppressWarnings("checkstyle:MethodLength")
    public final void testDeleteAndAddSameVersion1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException,
    OpenRDFException, SolrServerException {
        String testName = CLASS_NAME_PREFIX
                + "testDeleteAndAddSameVersion1";
        ArquillianTestUtils.clearDatabase(ROLES);
        ArquillianTestUtils.loadDbUnitTestFile(ROLES, testName);
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, testName);
        ArquillianTestUtils.copyUploadsFilesForTest(testName);
        ArquillianTestUtils.removeAllSesameRepositories();

        // CHECKSTYLE:OFF: MagicNumber
        // Yes, this is a magic number. It's taken from the real-world
        // production service.
        int firstVersionId = 990;
        // CHECKSTYLE:ON: MagicNumber

        ArquillianTestUtils.setSequenceValue(REGISTRY,
                "VERSION_IDS", "ID", firstVersionId);

        Vocabulary vocabulary = RegistryTestUtils.
                getValidatedVocabularyFromFile(
                "test/tests/" + testName + "/test-vocabulary1.xml",
                ValidationMode.CREATE);
        EntityManager em = null;
        List<AccessPoint> sesameAPs = null;
        List<AccessPoint> sissvocAPs = null;

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

        // Now delete and add the same version.
        // The content of test-vocabulary2.xml differs from
        // test-vocabulary1.xml _only_ in that the vocabulary element
        // specifies the vocabulary ID. So we get the _same_ vocabulary,
        // but a _different_, new version, but with the same slug.

        // CHECKSTYLE:OFF: MagicNumber
        // Yes, this is a magic number. It's taken from the real-world
        // production service.
        int secondVersionId = 1028;
        // CHECKSTYLE:ON: MagicNumber
        ArquillianTestUtils.setSequenceValue(REGISTRY,
                "VERSION_IDS", "ID", secondVersionId);
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
            sesameAPs = AccessPointDAO.
                    getCurrentAccessPointListForVersionByType(secondVersionId,
                    AccessPointType.SESAME_DOWNLOAD, em);
            sissvocAPs = AccessPointDAO.
                    getCurrentAccessPointListForVersionByType(secondVersionId,
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

        // And NOW it's time to confirm that everything is in place.

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
        Assert.assertEquals(sissvocAPs.size(), 1,
                "Not exactly one sissvoc access point");
        ApSissvoc apSissvoc = JSONSerialization.
                deserializeStringAsJson(sissvocAPs.get(0).getData(),
                        ApSissvoc.class);
        String apSissvocPath = apSissvoc.getPath();
        Path apSissvocPathPath = Paths.get(apSissvocPath);
        Assert.assertTrue(Files.exists(apSissvocPathPath),
                "SISSVoc spec file missing");
        MatcherAssert.assertThat("SISSVoc spec file size",
                Files.size(apSissvocPathPath),
                Matchers.greaterThan(0L));
    }

    /** Test of updating two existing versions by swapping their
     * version titles and slugs.
     * It's necessary to ensure that the deletion and insertion subtasks
     * for the two versions are serialized so that no deletion of the
     * external content for an access point of one version is performed
     * <i>after</i> the corresponding insert for the other version.
     * Otherwise,
     * the net result can be the loss of the external content underlying some
     * of the access points. E.g., we could have the Sesame and SISSVoc
     * access points for one of the versions in the database,
     * but the Sesame repository and SISSVoc spec file having been deleted.
     * CC-2917.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws JAXBException If a problem loading vocabulary data.
     * @throws OpenRDFException If there is a problem connecting with Sesame.
     * @throws SolrServerException If an error during Solr searching.
     *  */
    @Test(enabled = false)
    @SuppressWarnings("checkstyle:MethodLength")
    public final void testSwapVersionSlugs1() throws
    DatabaseUnitException, IOException, SQLException, JAXBException,
    OpenRDFException, SolrServerException {
        String testName = CLASS_NAME_PREFIX
                + "testSwapVersionSlugs1";
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
        List<AccessPoint> sesameAPs1 = null;
        List<AccessPoint> sissvocAPs1 = null;
        List<AccessPoint> sesameAPs2 = null;
        List<AccessPoint> sissvocAPs2 = null;

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

        // Now swap the titles and slugs of the two versions.

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
            sesameAPs1 = AccessPointDAO.
                    getCurrentAccessPointListForVersionByType(1,
                    AccessPointType.SESAME_DOWNLOAD, em);
            sissvocAPs1 = AccessPointDAO.
                    getCurrentAccessPointListForVersionByType(1,
                    AccessPointType.SISSVOC, em);
            sesameAPs2 = AccessPointDAO.
                    getCurrentAccessPointListForVersionByType(2,
                    AccessPointType.SESAME_DOWNLOAD, em);
            sissvocAPs2 = AccessPointDAO.
                    getCurrentAccessPointListForVersionByType(2,
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

        // Now confirm that everything is in place.

        // Confirm that the Sesame repos exist.
        Assert.assertEquals(sesameAPs1.size(), 1,
                "Not exactly one sesameDownload access point for version 1");
        Assert.assertEquals(sesameAPs2.size(), 1,
                "Not exactly one sesameDownload access point for version 2");
        ApSesameDownload apSesameDownload1 = JSONSerialization.
                deserializeStringAsJson(sesameAPs1.get(0).getData(),
                        ApSesameDownload.class);
        ApSesameDownload apSesameDownload2 = JSONSerialization.
                deserializeStringAsJson(sesameAPs2.get(0).getData(),
                        ApSesameDownload.class);
        String apServerBase1 = apSesameDownload1.getServerBase();
        String apRepositoryID1 = apSesameDownload1.getRepository();
        String apServerBase2 = apSesameDownload2.getServerBase();
        String apRepositoryID2 = apSesameDownload2.getRepository();
        RepositoryManager manager = null;
        try {
            manager = RepositoryProvider.getRepositoryManager(apServerBase1);
            Repository repository1 = manager.getRepository(apRepositoryID1);
            Assert.assertNotNull(repository1,
                    "Repository missing for version 1");
            manager = RepositoryProvider.getRepositoryManager(apServerBase2);
            Repository repository2 = manager.getRepository(apRepositoryID2);
            Assert.assertNotNull(repository2,
                    "Repository missing for version 2");
        } catch (RepositoryConfigException | RepositoryException e) {
            throw e;
        }

        // Confirm that the SISSVoc spec files exist.
        Assert.assertEquals(sissvocAPs1.size(), 1,
                "Not exactly one sissvoc access point for version 1");
        Assert.assertEquals(sissvocAPs2.size(), 1,
                "Not exactly one sissvoc access point for version 2");
        ApSissvoc apSissvoc1 = JSONSerialization.
                deserializeStringAsJson(sissvocAPs1.get(0).getData(),
                        ApSissvoc.class);
        String apSissvocPath1 = apSissvoc1.getPath();
        Path apSissvocPathPath1 = Paths.get(apSissvocPath1);
        Assert.assertTrue(Files.exists(apSissvocPathPath1),
                "SISSVoc spec file missing for version 1");
        MatcherAssert.assertThat("SISSVoc spec file size for version 1",
                Files.size(apSissvocPathPath1),
                Matchers.greaterThan(0L));
        ApSissvoc apSissvoc2 = JSONSerialization.
                deserializeStringAsJson(sissvocAPs2.get(0).getData(),
                        ApSissvoc.class);
        String apSissvocPath2 = apSissvoc2.getPath();
        Path apSissvocPathPath2 = Paths.get(apSissvocPath2);
        Assert.assertTrue(Files.exists(apSissvocPathPath2),
                "SISSVoc spec file missing for version 2");
        MatcherAssert.assertThat("SISSVoc spec file size for version 2",
                Files.size(apSissvocPathPath2),
                Matchers.greaterThan(0L));
    }

    // Code to do a database dump; copy/paste and use as required
    // during development of a test.
//  ArquillianTestUtils.exportFullDbUnitData(REGISTRY,
//  testName + "-out.xml");

}
