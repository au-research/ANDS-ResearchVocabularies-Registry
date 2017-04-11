/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests of the slug generator. */
public class TestSlugGenerator {

    /** Run tests of the slug generator. */
    @Test
    public void testSlugGenerator() {
        Assert.assertEquals(SlugGenerator.generateSlug("Gänsefüßchen"),
                "gansefusschen");
        Assert.assertEquals(SlugGenerator.generateSlug("Γειά σας नमस्ते 你好"),
                "geia-sas-namaste-ni-hao");
        Assert.assertEquals(SlugGenerator.generateSlug("Dzień dobry!"),
                "dzien-dobry");
        Assert.assertEquals(SlugGenerator.generateSlug("Number 9"),
                "number-9");
        Assert.assertEquals(SlugGenerator.generateSlug("Version 1.1"),
                "version-1-1");
//        Assert.assertEquals(SlugGenerator.generateSlug(""),
//                "");
    }
}
