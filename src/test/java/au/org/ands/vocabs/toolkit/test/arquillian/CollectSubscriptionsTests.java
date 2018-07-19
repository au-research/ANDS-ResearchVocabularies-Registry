/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.REGISTRY;
import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.ROLES;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;

import javax.persistence.EntityManager;

import org.dbunit.DatabaseUnitException;
import org.hamcrest.MatcherAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.xmlunit.matchers.CompareMatcher;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import au.org.ands.vocabs.registry.db.context.DBContext;
import au.org.ands.vocabs.registry.notification.CollectEvents;
import au.org.ands.vocabs.registry.notification.email.CollectSubscriptions;
import au.org.ands.vocabs.registry.notification.email.SubscriberSubscriptionsModel;

/** Tests of the CollectSubscriptions class. */
@Test
public class CollectSubscriptionsTests extends ArquillianBaseTest {

    /** Logger for this class. */
    @SuppressWarnings("unused")
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Name of this class, used in paths to test data files. */
    private static final String CLASS_NAME_PREFIX =
            "CollectSubscriptionsTests.";

    /** A convenient value to use for the start or end date of a
     * reporting period. */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static final LocalDateTime NOW_TIME_1 =
            LocalDateTime.of(2017, 10, 1, 10, 0);

//    /** A convenient value to use for the start or end date of a
//     * reporting period. */
//    @SuppressWarnings("checkstyle:MagicNumber")
//    private static final LocalDateTime NOW_TIME_2 =
//            LocalDateTime.of(2017, 10, 1, 10, 10);

    /** A convenient value to use for the start or end date of a
     * reporting period. */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static final LocalDateTime NOW_TIME_3 =
            LocalDateTime.of(2017, 10, 1, 10, 20);

    /** XStream serializer for complex data structures.
     * Initialized by a method with {@link BeforeMethod} annotatation. */
    private XStream xstream;

    /** Initialize the XStream serializer. */
    @BeforeMethod
    private void initXStream() {
        xstream = new XStream(new StaxDriver());
        // Aliases for a more concise result.
        xstream.alias("CollectEvents",
                au.org.ands.vocabs.registry.notification.CollectEvents.class);
        xstream.alias("VocabularyDifferences",
                au.org.ands.vocabs.registry.notification.
                    VocabularyDifferences.class);
        xstream.alias("VersionDifferences",
                au.org.ands.vocabs.registry.notification.
                    VersionDifferences.class);
        xstream.alias("SubjectsWithEquals",
                au.org.ands.vocabs.registry.log.utils.SubjectsWithEquals.class);
        xstream.alias("SubscriberSubscriptionsModel",
                au.org.ands.vocabs.registry.notification.email.
                    SubscriberSubscriptionsModel.class);
//      xstream.alias("",
//              .class);
        // We're not interested in these fields.
        // NB: This means the results can not be deserialized as is!
        xstream.omitField(org.apache.commons.lang3.builder.DiffBuilder.class,
                "style");
        xstream.omitField(au.org.ands.vocabs.registry.notification.
                CollectEvents.class, "logger");
//        xstream.omitField(.class,
//                "");
    }


    /** First test of the {@link CollectSubscriptions} class.
     * @param testName The name of the test calling this script. The name
     *      is included as part of the directory name used for loading
     *      test data.
     * @param startDate The start date to use for the notification period.
     * @param endDate The end date to use for the notification period.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     */
    private void scriptCollectSubscriptions1(final String testName,
            final LocalDateTime startDate, final LocalDateTime endDate) throws
    DatabaseUnitException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + testName);
        ArquillianTestUtils.clearDatabase(ROLES);
        ArquillianTestUtils.loadDbUnitTestFile(ROLES, CLASS_NAME_PREFIX
                + testName);
        EntityManager em = null;
        String collectedEventsXML;
        String subscriberSubscriptionsModelsXML;
        try {
            em = DBContext.getEntityManager();
            CollectEvents collectedEvents =
                    new CollectEvents(startDate, endDate);
            collectedEventsXML = xstream.toXML(collectedEvents);
            CollectSubscriptions collectSubscriptions =
                    new CollectSubscriptions(em);
            collectSubscriptions.computeVocabularySubscriptionsForSubscribers(
                    collectedEvents);
            Map<Integer, SubscriberSubscriptionsModel>
            subscriberSubscriptionsModels =
                collectSubscriptions.getSubscriberSubscriptionsModels();
            subscriberSubscriptionsModelsXML =
                    xstream.toXML(subscriberSubscriptionsModels);
        } finally {
            if (em != null) {
                em.close();
            }
        }

//        logger.info("collectedEventsXML: " + collectedEventsXML);

        String expectedCollectedEvents =
                ArquillianTestUtils.getResourceAsString(
                "test/tests/"
                        + CLASS_NAME_PREFIX
                        + testName
                        + "/test-collectevents.xml");

        MatcherAssert.assertThat(collectedEventsXML,
                CompareMatcher.isIdenticalTo(expectedCollectedEvents).
                ignoreWhitespace());

//        logger.info("subscriberSubscriptionsModelsXML: "
//                + subscriberSubscriptionsModelsXML);

        String expectedSubscriberSubscriptionsModels =
                ArquillianTestUtils.getResourceAsString(
                "test/tests/"
                        + CLASS_NAME_PREFIX
                        + testName
                        + "/test-subscribersubscriptionsmodels.xml");

        MatcherAssert.assertThat(subscriberSubscriptionsModelsXML,
                CompareMatcher.isIdenticalTo(
                        expectedSubscriberSubscriptionsModels).
                        ignoreWhitespace());
    }

    /** A test of the {@link CollectSubscriptions} class that
     * observes changes to the top-level metadata of a vocabulary.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     */
    @Test
    public void testCollectSubscriptions1() throws
    DatabaseUnitException, IOException, SQLException {
        scriptCollectSubscriptions1("testCollectSubscriptions1",
                NOW_TIME_1, NOW_TIME_3);
    }

    /** A test of the {@link CollectSubscriptions} class that
     * observes the creation of a vocabulary.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     */
    @Test
    public void testCollectSubscriptions2() throws
    DatabaseUnitException, IOException, SQLException {
        scriptCollectSubscriptions1("testCollectSubscriptions2",
                NOW_TIME_1, NOW_TIME_3);
    }

    /** A test of the {@link CollectSubscriptions} class that
     * observes changes to the top-level metadata of a vocabulary
     * and the addition of versions.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     */
    @Test
    public void testCollectSubscriptions3() throws
    DatabaseUnitException, IOException, SQLException {
        scriptCollectSubscriptions1("testCollectSubscriptions3",
                NOW_TIME_1, NOW_TIME_3);
    }

    /** A test of the {@link CollectSubscriptions} class that
     * observes changes to the a version and the addition of a version.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     */
    @Test
    public void testCollectSubscriptions4() throws
    DatabaseUnitException, IOException, SQLException {
        scriptCollectSubscriptions1("testCollectSubscriptions4",
                NOW_TIME_1, NOW_TIME_3);
    }


}
