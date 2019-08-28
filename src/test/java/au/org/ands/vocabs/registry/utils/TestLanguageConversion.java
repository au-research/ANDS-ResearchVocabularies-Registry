/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.solr.EntityIndexer;

/** Unit tests of the conversion between ISO 639 and human-readable
 * forms of language names. */
public class TestLanguageConversion {

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

        // Check resolution of values that aren't valid language tags.
        Assert.assertEquals(EntityIndexer.resolveLanguage("English"),
                "English");
        Assert.assertEquals(EntityIndexer.resolveLanguage("Turkish"),
                "Turkish");

        //        loc = new ULocale("");
//        Assert.assertEquals(EntityIndexer.resolveLanguage(),
//                "");
    }
}
