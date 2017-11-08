/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.REGISTRY;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.dbunit.DatabaseUnitException;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.dao.ResourceOwnerHostDAO;
import au.org.ands.vocabs.registry.db.dao.VersionDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.db.entity.ResourceOwnerHost;
import au.org.ands.vocabs.registry.enums.VersionStatus;
import au.org.ands.vocabs.registry.solr.SolrUtils;

/** All Arquillian tests of the Registry.
 * As this class grows, it might be split up further.
 */
@Test
public class RegistryTests extends ArquillianBaseTest {

    /** Logger. */
    private static Logger logger;

    static {
        logger = LoggerFactory.getLogger(
                MethodHandles.lookup().lookupClass());
    }

    // Server-side tests go here. Client-side tests later on.

    /** Test of {@link VocabularyDAO#getAllVocabulary()}.
     * This is just a sanity test to make sure that the registry
     * code is included correctly. */
    @Test
    public final void testVocabularyDAOGetAllVocabulary() {
        List<au.org.ands.vocabs.registry.db.entity.Vocabulary>
            vocabularyList = VocabularyDAO.getAllVocabulary();
        Assert.assertNotNull(vocabularyList);
        Assert.assertEquals(vocabularyList.size(), 0, "Empty list");
    }

    /** Test of {@link VersionDAO#getAllVersion()}.
     * This is just a sanity test to make sure that the registry
     * code is included correctly.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.*/
    @Test
    public final void testVersionDAOGetAllVersion()
            throws DatabaseUnitException, SQLException, IOException {

        ArquillianTestUtils.clearDatabase(REGISTRY);

        List<au.org.ands.vocabs.registry.db.entity.Version>
                versionList = VersionDAO.getAllVersion();
        Assert.assertNotNull(versionList);
        Assert.assertEquals(versionList.size(), 0, "should be empty");
    }

    /** Test adding a Registry Version and fetching it again.
     * Note: as it stands, this test currently passes, but it should fail,
     * for the reasons explained in comments that begin "Fix this".
     * Some work should be done to enhance the VersionListener class
     * so that each one of these cases fails in turn, and then this
     * test code should be adjusted to make the test pass again.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     */
    @Test
    public final void testVersionDAOAddVersion()
            throws DatabaseUnitException, SQLException, IOException {

        ArquillianTestUtils.clearDatabase(REGISTRY);

        au.org.ands.vocabs.registry.db.entity.Version
                version = new au.org.ands.vocabs.registry.db.entity.Version();
        version.setVersionId(1);
        // Fix this in VersionListener: should not be possible to persist this,
        // as it is not valid JSON.
        version.setData("some data");
        version.setModifiedBy("SYSTEM");
        version.setStartDate(LocalDateTime.now());
        version.setEndDate(LocalDateTime.now());
        // Fix this in VersionListener: should not be possible to persist this,
        // as it is not in the right format.
        version.setReleaseDate("release_date");
        version.setSlug("version-slug");
        version.setStatus(VersionStatus.CURRENT);
        // Fix this in VersionListener: should not be possible to persist this,
        // as there is no such vocabulary.
        version.setVocabularyId(1);
        VersionDAO.saveVersion(version);

        // has 1
        List<au.org.ands.vocabs.registry.db.entity.Version>
                versionList = VersionDAO.getAllVersion();
        Assert.assertNotNull(versionList);
        Assert.assertEquals(versionList.size(), 1, "saved 1");
        logger.info("versionID: " + versionList.get(0).getId());

        // get
        au.org.ands.vocabs.registry.db.entity.Version
                versionEntity = VersionDAO.getVersionById(1);
        Assert.assertNotNull(versionEntity);
        Assert.assertEquals(versionEntity.getSlug(), versionEntity.getSlug());
    }

    /* Solr. */

    /** Test of {@link VocabularyDAO#getAllVocabulary()}.
     * This is just a sanity test to make sure that the registry
     * code is included correctly. */
    @Test
    public final void testSolrOK() {
        SolrClient solrClient = SolrUtils.getSolrClient();
        Assert.assertNotNull(solrClient);
    }

    /** Test of date/time conversion at the time of the
     * daylight savings changeover, using {@link ResourceOwnerHost}
     * as an example entity class.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public final void testResourceOwnerHostDateTimeConversion()
            throws HibernateException, DatabaseUnitException,
            IOException, SQLException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ResourceOwnerHost roh = new ResourceOwnerHost();
        // Doesn't matter what these are; we're testing start and end dates.
        roh.setHost("http://www");
        roh.setOwner("me");
        // The date/time 2016-10-02T02:01:00 is fine in UTC,
        // but it does not exist in the Australia/Sydney time zone!
        // If the implementation treats the value as a time in
        // the Australia/Sydney time zone, it will be persisted
        // incorrectly.
        LocalDateTime startDate = LocalDateTime.of(2016, 10, 2, 2, 1, 0);
        TemporalUtils.makeCurrentlyValid(roh, startDate);
        ResourceOwnerHostDAO.saveResourceOwnerHost(roh);
        List<ResourceOwnerHost> rohList = ResourceOwnerHostDAO.
                getCurrentResourceOwnerHostsForOwner("me");
        Assert.assertEquals(rohList.size(), 1, "Not exactly one ROH");
        roh = rohList.get(0);
        Assert.assertEquals(roh.getStartDate(), startDate,
                "Start date different");
    }


    // Client-side tests go here. Server-side tests are above this line.



}
