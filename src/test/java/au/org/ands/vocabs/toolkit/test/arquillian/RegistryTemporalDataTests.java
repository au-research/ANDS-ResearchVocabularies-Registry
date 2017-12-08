/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import java.time.LocalDateTime;

import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.db.context.TemporalMeaning;
import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;

/** Tests of the modelling of temporal data contained within registry
 * database entities.
 */
@Test
public class RegistryTemporalDataTests extends ArquillianBaseTest {

    /** Test of the utility methods provided for registry database entities
     * that have temporal columns. */
    @Test
    public final void testTemporalProperties() {
        // Vocabulary here stands for all of the database entities that
        // have temporal columns.
        Vocabulary vocabulary = new Vocabulary();
        LocalDateTime now = TemporalUtils.nowUTC();

        // First, a currently-valid entity.
        vocabulary.setStartDate(now);
        TemporalUtils.makeCurrentlyValid(vocabulary);
        Assert.assertTrue(TemporalUtils.isCurrent(vocabulary), "Not current");
        Assert.assertFalse(TemporalUtils.isHistorical(vocabulary),
                "Is historical");
        Assert.assertFalse(TemporalUtils.isDraft(vocabulary), "Is draft");
        Assert.assertEquals(TemporalUtils.getTemporalDescription(vocabulary),
                TemporalMeaning.CURRENT, "Meaning not CURRENT");

        // Historical.
        LocalDateTime slightlyLaterNow = TemporalUtils.nowUTC();
        TemporalUtils.makeHistorical(vocabulary, slightlyLaterNow);
        Assert.assertTrue(TemporalUtils.isHistorical(vocabulary),
                "Not historical");
        Assert.assertFalse(TemporalUtils.isCurrent(vocabulary), "Is current");
        Assert.assertFalse(TemporalUtils.isDraft(vocabulary), "Is draft");
        Assert.assertEquals(TemporalUtils.getTemporalDescription(vocabulary),
                TemporalMeaning.HISTORICAL, "Meaning not HISTORICAL");

        // Draft.
        TemporalUtils.makeDraft(vocabulary);
        Assert.assertTrue(TemporalUtils.isDraft(vocabulary), "Not draft");
        Assert.assertFalse(TemporalUtils.isHistorical(vocabulary),
                "Is historical");
        Assert.assertFalse(TemporalUtils.isCurrent(vocabulary), "Is current");
        Assert.assertEquals(TemporalUtils.getTemporalDescription(vocabulary),
                TemporalMeaning.DRAFT,
                "Meaning not DRAFT");
    }

}
