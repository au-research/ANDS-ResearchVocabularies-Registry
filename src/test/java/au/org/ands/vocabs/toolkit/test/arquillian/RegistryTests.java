/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.REGISTRY;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.dbunit.DatabaseUnitException;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;

import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.dao.ResourceOwnerHostDAO;
import au.org.ands.vocabs.registry.db.dao.VersionDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.db.entity.ResourceOwnerHost;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.enums.VersionStatus;
import au.org.ands.vocabs.registry.solr.EntityIndexer;
import au.org.ands.vocabs.registry.solr.SearchRegistryIndex;
import au.org.ands.vocabs.registry.solr.SearchResourcesIndex;
import au.org.ands.vocabs.toolkit.db.TaskUtils;

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

    /** Name of this class, used in paths to test data files. */
    private static final String CLASS_NAME_PREFIX = "RegistryTests.";

    // Server-side tests go here. Client-side tests later on.

    /** Test of {@link VocabularyDAO#getAllVocabulary()} and
     * {@link VersionDAO#getAllVersion()}.
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
    public final void testVocabularyVersionDAOGetAll()
            throws DatabaseUnitException, SQLException, IOException {

        ArquillianTestUtils.clearDatabase(REGISTRY);

        List<au.org.ands.vocabs.registry.db.entity.Vocabulary>
        vocabularyList = VocabularyDAO.getAllVocabulary();
        Assert.assertNotNull(vocabularyList);
        Assert.assertEquals(vocabularyList.size(), 0, "Empty vocabulary list");

        List<au.org.ands.vocabs.registry.db.entity.Version>
        versionList = VersionDAO.getAllVersion();
        Assert.assertNotNull(versionList);
        Assert.assertEquals(versionList.size(), 0, "Empty version list");
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
        version.setStartDate(TemporalUtils.nowUTC());
        version.setEndDate(TemporalUtils.nowUTC());
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

    /** Test of {@link
     * VersionDAO#getCurrentVersionListForVocabularyByReleaseDateSlug(Integer)}.
     * This test confirms the correct sorting of versions.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.*/
    @Test
    public final void testVersionDAOVersionsByReleaseDateSlug()
            throws DatabaseUnitException, SQLException, IOException {
        String testName = "testVersionDAOVersionsByReleaseDateSlug";
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + testName);

        List<Version> dbVersions = VersionDAO.
            getCurrentVersionListForVocabularyByReleaseDateSlug(1);
        Version[] versionsArray = dbVersions.toArray(new Version[0]);
        Assert.assertTrue(ArrayUtils.isSorted(versionsArray,
                                              new VersionComparator()),
                          "Versions list is not sorted correctly");
    }

    static class VersionComparator implements Comparator<Version> {
        @Override
        public int compare(final Version v1, final Version v2) {
            // Note "compare(v2, v1)", because we expect sorting
            // in reverse order of both of the sort fields.
            return Comparator
                    .comparing(Version::getReleaseDate)
                    .thenComparing(Version::getSlug)
                    .compare(v2, v1);
        }
    }

    /* Solr. */

    /** Test of Solr indexing of one vocabulary.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws SolrServerException If an error during Solr indexing.
     */
    @Test
    public final void testSolrIndexing1()
            throws HibernateException, DatabaseUnitException,
            IOException, SQLException, SolrServerException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY,
                CLASS_NAME_PREFIX + "testSolrIndexing1");
        EntityIndexer.indexAllVocabularies();
        // Explicit commit is required so that we can do a search
        // immediately.
        EntityIndexer.commit();
        List<Object> filtersAndResultsExtracted = new ArrayList<>();
        String searchResults = SearchRegistryIndex.query("{}",
                filtersAndResultsExtracted, false);
        logger.info("Result: " + searchResults);
        JsonNode resultsJson = TaskUtils.jsonStringToTree(searchResults);
        JsonNode response = resultsJson.get("response");
        Assert.assertEquals(response.get("numFound").asInt(), 1, "numFound");
        JsonNode doc = response.get("docs").get(0);
        Assert.assertEquals(doc.get("id").asText(), "1", "id");
        Assert.assertEquals(doc.get("slug").asText(), "rifcs", "slug");
        // And so on, for the rest of the fields.

        // Facets.
        JsonNode facetCounts = resultsJson.get("facet_counts");
        JsonNode facetFields = facetCounts.get("facet_fields");
        // Languages. This is also a test of language tag resolution.
        JsonNode facetLanguage = facetFields.get("language");
        Assert.assertEquals(facetLanguage.size(), 2, "language facet size");
        Assert.assertEquals(facetLanguage.get(0).asText(), "English",
                "Language facet value");
        Assert.assertEquals(facetLanguage.get(1).asInt(), 1,
                "Language facet count");
    }

    /** Test of Solr indexing with different types of "most suitable" version.
     * There are two vocabularies: one with a current version (and superseded
     * versions), and one with only superseded versions. We are particularly
     * interested to confirm that the latter is also indexed with regard
     * to its "most suitable" version.
     * See {@link
     * EntityIndexer#addFieldsForMostSuitableVersion(EntityManager,Integer,SolrInputDocument)}
     * for details.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws SolrServerException If an error during Solr indexing.
     */
    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public final void testSolrIndexing2()
            throws HibernateException, DatabaseUnitException,
            IOException, SQLException, SolrServerException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY,
                CLASS_NAME_PREFIX + "testSolrIndexing2");
        EntityIndexer.indexAllVocabularies();
        // Explicit commit is required so that we can do a search
        // immediately.
        EntityIndexer.commit();
        List<Object> filtersAndResultsExtracted = new ArrayList<>();
        String searchResults = SearchRegistryIndex.query("{}",
                filtersAndResultsExtracted, false);
        // Default sorting is alpha A-Z; we rely on that.
        logger.info("Result: " + searchResults);
        JsonNode resultsJson = TaskUtils.jsonStringToTree(searchResults);
        JsonNode response = resultsJson.get("response");
        Assert.assertEquals(response.get("numFound").asInt(), 2, "numFound");

        // There are two vocabularies, and we expect the same sort of results
        // for each.
        for (int i = 0; i < 2; i++) {
            int vocabularyId = i + 1;
            String vocabularyIdString = Integer.toString(vocabularyId);
            JsonNode doc = response.get("docs").get(i);
            Assert.assertEquals(doc.get("id").asText(),
                    vocabularyIdString, "id");
            Assert.assertEquals(doc.get("slug").asText(),
                    "rifcs" + vocabularyIdString, "slug");
            // And so on, for the rest of the fields.
        }
        // Facets.
        JsonNode facetCounts = resultsJson.get("facet_counts");
        JsonNode facetFields = facetCounts.get("facet_fields");
        JsonNode facetAccess = facetFields.get("access");
        Assert.assertEquals(facetAccess.size(), 6, "access facet size");
        // Each of the three counts should be 2: both vocabularies have a
        // "most suitable" version with three access points.
        Assert.assertEquals(facetAccess.get(1).asInt(), 2,
                "access facet count");
        Assert.assertEquals(facetAccess.get(3).asInt(), 2,
                "access facet count");
        Assert.assertEquals(facetAccess.get(5).asInt(), 2,
                "access facet count");

        JsonNode facetFormat = facetFields.get("format");
        Assert.assertEquals(facetFormat.size(), 2, "format facet size");
        // The count should be 2: both vocabularies have access points.
        Assert.assertEquals(facetFormat.get(1).asInt(), 2,
                "format facet count");

        JsonNode facetWidgetable = facetFields.get("widgetable");
        Assert.assertEquals(facetWidgetable.size(), 2, "widgetable facet size");
        // The count should be 2: both vocabularies are widgetable.
        Assert.assertEquals(facetWidgetable.get(1).asInt(), 2,
                "widgetable facet count");
    }

    /** Tests of interpreting the "pp" parameter to the resource search,
     * in particular, when the maximum number of rows is requested.
     * (Jira RVA-5)
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws SolrServerException If an error during Solr indexing.
     */
    @Test
    public final void testSolrResourceSearchRows()
            throws HibernateException, DatabaseUnitException,
            IOException, SQLException, SolrServerException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY,
                CLASS_NAME_PREFIX + "testSolrResourceSearchRows");
        EntityIndexer.indexAllVocabularies();
        // Explicit commit is required so that we can do a search
        // immediately.
        EntityIndexer.commit();

        // pp: -1 -> MAX_ROWS
        List<Object> filtersAndResultsExtracted = new ArrayList<>();
        String searchResults = SearchResourcesIndex.query(
                "{\"pp\":-1}",
                filtersAndResultsExtracted, false);
        logger.info("Result: " + searchResults);
        JsonNode resultsJson = TaskUtils.jsonStringToTree(searchResults);
        JsonNode responseHeader = resultsJson.get("responseHeader");
        JsonNode params = responseHeader.get("params");
        Assert.assertEquals(params.get("rows").asInt(),
                SearchResourcesIndex.MAX_ROWS, "rows requested: -1");

        // pp: 0 -> 0
        filtersAndResultsExtracted = new ArrayList<>();
        searchResults = SearchResourcesIndex.query(
                "{\"pp\":0}",
                filtersAndResultsExtracted, false);
        logger.info("Result: " + searchResults);
        resultsJson = TaskUtils.jsonStringToTree(searchResults);
        responseHeader = resultsJson.get("responseHeader");
        params = responseHeader.get("params");
        Assert.assertEquals(params.get("rows").asInt(),
                0, "rows requested: 0");

        // pp: MAX_ROWS + 1 -> MAX_ROWS
        filtersAndResultsExtracted = new ArrayList<>();
        searchResults = SearchResourcesIndex.query(
                "{\"pp\":"
                + SearchResourcesIndex.MAX_ROWS + 1
                + "}",
                filtersAndResultsExtracted, false);
        logger.info("Result: " + searchResults);
        resultsJson = TaskUtils.jsonStringToTree(searchResults);
        responseHeader = resultsJson.get("responseHeader");
        params = responseHeader.get("params");
        Assert.assertEquals(params.get("rows").asInt(),
                SearchResourcesIndex.MAX_ROWS, "rows requested: MAX_ROWS + 1");
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
