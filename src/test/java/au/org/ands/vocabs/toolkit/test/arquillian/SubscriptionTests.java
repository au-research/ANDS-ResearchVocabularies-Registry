/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;


import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.REGISTRY;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import javax.persistence.EntityManager;

import org.dbunit.DatabaseUnitException;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.db.context.DBContext;
import au.org.ands.vocabs.registry.subscription.Owners;
import au.org.ands.vocabs.registry.subscription.SubscriptionUtils;

/** Tests of the subscription subsystem. */
public class SubscriptionTests extends ArquillianBaseTest {

    /** Logger for this class. */
    private static Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Name of this class, used in paths to test data files. */
    private static final String CLASS_NAME_PREFIX = "SubscriptionTests.";

    /** A convenient value to use for startDate and endDate properties. */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static LocalDateTime nowTime1 =
            LocalDateTime.of(2017, 10, 1, 10, 10);

    /** A convenient value to use for startDate and endDate properties. */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static LocalDateTime nowTime2 =
            LocalDateTime.of(2017, 10, 1, 10, 20);

    /** User to use for modifiedBy values.*/
    private static final String TEST_USER = "TEST";

    // Initialization for tests in this class.

    // Server-side tests go here. Client-side tests later on.

    /** Set up the database, run a test script using subscriptions, then
     * compare the actual database contents to the expected contents.
     * @param testName The name of the test calling this script. The name
     *      is included as part of the directory name used for loading
     *      test data.
     * @param script The code under test.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    private void scriptRunner1(final String testName,
            final Consumer<EntityManager> script) throws
    DatabaseUnitException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + testName);
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            em.getTransaction().begin();
            script.accept(em);
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
        ArquillianTestUtils.
        compareDatabaseCurrentAndExpectedContentsIgnoreSubscriberTokens(
                REGISTRY,
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-registry-results.xml");
    }

    /** Set up the database, run a test script using subscriptions, then
     * compare the actual database contents to the expected contents.
     * Then run a second script, and do another comparison.
     * @param testName The name of the test calling this script. The name
     *      is included as part of the directory name used for loading
     *      test data.
     * @param script1 The first code under test.
     * @param script2 The second code under test.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    private void scriptRunner2(final String testName,
            final Consumer<EntityManager> script1,
            final Consumer<EntityManager> script2) throws
    DatabaseUnitException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY, CLASS_NAME_PREFIX
                + testName);
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            em.getTransaction().begin();
            script1.accept(em);
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
        ArquillianTestUtils.
        compareDatabaseCurrentAndExpectedContentsIgnoreSubscriberTokens(
                REGISTRY,
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-registry-results-1.xml");

        em = null;
        try {
            em = DBContext.getEntityManager();
            em.getTransaction().begin();
            script2.accept(em);
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
//                testName + "-2-out.xml");
        ArquillianTestUtils.
        compareDatabaseCurrentAndExpectedContentsIgnoreSubscriberTokens(
                REGISTRY,
                "test/tests/"
                + CLASS_NAME_PREFIX
                + testName
                + "/test-registry-results-2.xml");
    }

    /** Test of {@link SubscriptionUtils#createEmailSubscriptionVocabulary(
     * String, Integer, EntityManager, LocalDateTime, String)} and {@link
     * SubscriptionUtils#deleteEmailSubscriptionVocabulary(Integer,
     * Integer, EntityManager, LocalDateTime, String)}.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.*/
    @Test
    public final void testSubscribeVocabulary1()
            throws DatabaseUnitException, SQLException, IOException {
        scriptRunner2("testSubscribeVocabulary1",
                em -> SubscriptionUtils.createEmailSubscriptionVocabulary(
                        "dummy@abc.com", 1, em, nowTime1,
                        TEST_USER),
                em -> SubscriptionUtils.deleteEmailSubscriptionVocabulary(
                        1, 1, em, nowTime2, TEST_USER));
    }

    /** Test of {@link SubscriptionUtils#createEmailSubscriptionOwner(
     * String, String, EntityManager, LocalDateTime, String)} and {@link
     * SubscriptionUtils#deleteEmailSubscriptionOwner(Integer, String,
     * EntityManager, LocalDateTime, String)}, where the owner name is
     * a "real" owner.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.*/
    @Test
    public final void testSubscribeOwner1()
            throws DatabaseUnitException, SQLException, IOException {
        scriptRunner2("testSubscribeOwner1",
                em -> SubscriptionUtils.createEmailSubscriptionOwner(
                        "dummy@abc.com", "ANDS-Curated", em, nowTime1,
                        TEST_USER),
                em -> SubscriptionUtils.deleteEmailSubscriptionOwner(
                        1, "ANDS-Curated", em, nowTime2,
                        TEST_USER));
    }

    /** Test of {@link SubscriptionUtils#createEmailSubscriptionOwner(
     * String, String, EntityManager, LocalDateTime, String)} and {@link
     * SubscriptionUtils#deleteEmailSubscriptionOwner(Integer, String,
     * EntityManager, LocalDateTime, String)}, where the owner name is the
     * special value {@link Owners#ALL_OWNERS}.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.*/
    @Test
    public final void testSubscribeOwner2()
            throws DatabaseUnitException, SQLException, IOException {
        scriptRunner2("testSubscribeOwner2",
                em -> SubscriptionUtils.createEmailSubscriptionOwner(
                        "dummy@abc.com", Owners.ALL_OWNERS, em, nowTime1,
                        TEST_USER),
                em -> SubscriptionUtils.deleteEmailSubscriptionOwner(
                        1, Owners.ALL_OWNERS, em, nowTime2,
                        TEST_USER));
    }

    /** Test of {@link SubscriptionUtils#createEmailSubscriptionSystem(
     * String, EntityManager, LocalDateTime, String)} and {@link
     * SubscriptionUtils#deleteEmailSubscriptionSystem(Integer,
     * EntityManager, LocalDateTime, String)}.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.*/
    @Test
    public final void testSubscribeSystem1()
            throws DatabaseUnitException, SQLException, IOException {
        scriptRunner2("testSubscribeSystem1",
                em -> SubscriptionUtils.createEmailSubscriptionSystem(
                        "dummy@abc.com", em, nowTime1,
                        TEST_USER),
                em -> SubscriptionUtils.deleteEmailSubscriptionSystem(
                        1, em, nowTime2, TEST_USER));
    }

    /** Test of {@link SubscriptionUtils#deleteEmailSubscriptionAll(Integer,
     * EntityManager, LocalDateTime, String)}.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.*/
    @Test
    public final void testDeleteAllSusbscriptions1()
            throws DatabaseUnitException, SQLException, IOException {
        scriptRunner1("testDeleteAllSusbscriptions1",
                em -> SubscriptionUtils.deleteEmailSubscriptionAll(
                        1, em, nowTime1, TEST_USER));
    }

}
