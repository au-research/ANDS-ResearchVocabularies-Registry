/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import static au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector.REGISTRY;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.time.LocalDateTime;

import javax.persistence.EntityManager;

import org.dbunit.DatabaseUnitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.db.context.DBContext;
import au.org.ands.vocabs.registry.model.ModelMethods;
import au.org.ands.vocabs.registry.model.VocabularyModel;

/** Tests of the registry model.
 */
@Test
public class RegistryModelTests extends ArquillianBaseTest {

    /** Logger. */
    private static Logger logger;

    static {
        logger = LoggerFactory.getLogger(
                MethodHandles.lookup().lookupClass());
    }

    /** A convenient value to use for endDate properties when
     * deleting. */
    @SuppressWarnings("checkstyle:MagicNumber")
    private static LocalDateTime nowTime =
            LocalDateTime.of(2017, 10, 1, 10, 10);

    /** Test of deleting the current version of a vocabulary that has only
     * a current instance, with Vocabulary and VocabularyRelatedEntity
     * model elements.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If a problem getting test data for DbUnit,
     *          or reading JSON from the correct and test output files.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     *  */
    @Test
    public final void testDeleteOnlyCurrent1() throws
    DatabaseUnitException, IOException, SQLException {
        ArquillianTestUtils.clearDatabase(REGISTRY);
        ArquillianTestUtils.loadDbUnitTestFile(REGISTRY,
                "testDeleteOnlyCurrent1");
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            em.getTransaction().begin();
            VocabularyModel vm = ModelMethods.createVocabularyModel(em, 1);
            ModelMethods.deleteOnlyCurrentVocabulary(vm, "TEST", nowTime);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
        ArquillianTestUtils.compareDatabaseCurrentAndExpectedContents(
                REGISTRY,
                "test/tests/au.org.ands.vocabs.toolkit."
                + "test.arquillian.AllArquillianTests."
                + "testDeleteOnlyCurrent1/"
                + "test-registry-results.xml");
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
