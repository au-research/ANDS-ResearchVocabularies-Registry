/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.ibm.icu.util.ULocale;

/** Unit tests of the conversion between ISO 639 and human-readable
 * forms of languages. */
public class TestLanguageConversion {

    /** Run tests of the language conversion. */
    @Test
    public void testSlugGenerator() {
        ULocale displayLocale = new ULocale("en_NZ");
        ULocale loc;
        loc = new ULocale("de");
        Assert.assertEquals(loc.getDisplayName(displayLocale),
                "German");
        loc = new ULocale("en");
        Assert.assertEquals(loc.getDisplayName(displayLocale),
                "English");
        loc = new ULocale("es");
        Assert.assertEquals(loc.getDisplayName(displayLocale),
                "Spanish");
        loc = new ULocale("fr");
        Assert.assertEquals(loc.getDisplayName(displayLocale),
                "French");
        loc = new ULocale("it");
        Assert.assertEquals(loc.getDisplayName(displayLocale),
                "Italian");
        loc = new ULocale("ja");
        Assert.assertEquals(loc.getDisplayName(displayLocale),
                "Japanese");
        loc = new ULocale("mi");
        Assert.assertEquals(loc.getDisplayName(displayLocale),
                "Māori");
        loc = new ULocale("ru");
        Assert.assertEquals(loc.getDisplayName(displayLocale),
                "Russian");
        loc = new ULocale("zh");
        Assert.assertEquals(loc.getDisplayName(displayLocale),
                "Chinese");
//        Assert.assertEquals(SlugGenerator.generateSlug(""),
//                "");
    }
}
