/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.solr.EntityIndexer;
import au.org.ands.vocabs.registry.test.LanguageSubtagRegistryTests;
import au.org.ands.vocabs.toolkit.test.arquillian.ArquillianBaseTest;

/** Unit tests of the indexing of language names.
 * For more tests of language resolution, see
 * {@link LanguageSubtagRegistryTests}.
 */
@Test
public class TestLanguageConversion extends ArquillianBaseTest {

    /** Run tests of the language conversion. */
    @Test
    public void testLanguageConversion() {
        // Check resolution of some valid ISO 639 tags.
        Assert.assertEquals(EntityIndexer.resolveLanguage("de"), "German");
        Assert.assertEquals(EntityIndexer.resolveLanguage("en"), "English");
        Assert.assertEquals(EntityIndexer.resolveLanguage("es"), "Spanish");
        Assert.assertEquals(EntityIndexer.resolveLanguage("fr"), "French");
        Assert.assertEquals(EntityIndexer.resolveLanguage("it"), "Italian");
        Assert.assertEquals(EntityIndexer.resolveLanguage("ja"), "Japanese");
        // We currently rely on the use of the en_NZ locale to get the
        // correct value for this case.
        Assert.assertEquals(EntityIndexer.resolveLanguage("mi"), "Māori");
        Assert.assertEquals(EntityIndexer.resolveLanguage("ru"), "Russian");
        Assert.assertEquals(EntityIndexer.resolveLanguage("zh"), "Chinese");

        // Check resolution of language variants.
        // For now, indexing is done on the brief description,
        // not the full description.
//        Assert.assertEquals(EntityIndexer.resolveLanguage("en-CA"),
//                "English (Canada)");
//        Assert.assertEquals(EntityIndexer.resolveLanguage("en-MT"),
//                "English (Malta)");
//        Assert.assertEquals(EntityIndexer.resolveLanguage("es-UY"),
//                "Spanish (Uruguay)");
        Assert.assertEquals(EntityIndexer.resolveLanguage("en-CA"),
                "English");
        Assert.assertEquals(EntityIndexer.resolveLanguage("en-MT"),
                "English");
        Assert.assertEquals(EntityIndexer.resolveLanguage("es-UY"),
                "Spanish");

        // Check resolution of values that aren't valid language tags.
        // These used to be accepted as-is, but are no longer.
//        Assert.assertEquals(EntityIndexer.resolveLanguage("English"),
//                "English");
//        Assert.assertEquals(EntityIndexer.resolveLanguage("Turkish"),
//                "Turkish");
        Assert.assertEquals(EntityIndexer.resolveLanguage("English"),
                "");
        Assert.assertEquals(EntityIndexer.resolveLanguage("Turkish"),
                "");

        //        loc = new ULocale("");
//        Assert.assertEquals(EntityIndexer.resolveLanguage(),
//                "");
    }
}
