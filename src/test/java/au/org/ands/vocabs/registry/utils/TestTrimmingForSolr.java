/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.utils;

import static au.org.ands.vocabs.registry.workflow.provider.transform.ResourceDocsTransformProvider.MAXIMUM_STRING_LENGTH;
import static au.org.ands.vocabs.registry.workflow.provider.transform.ResourceDocsTransformProvider.trim;

import org.apache.commons.lang3.StringUtils;
import org.eu.ingwar.tools.arquillian.extension.suite.annotations.ArquillianSuiteDeployment;
import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.workflow.provider.transform.ResourceDocsTransformProvider;

/** Tests of trimming string values that are to be stored in
 * Solr fields of type string.
 */
@ArquillianSuiteDeployment
@Test(groups = "standalone")
public class TestTrimmingForSolr {

    /* NB: the combination of the annotations
     * @ArquillianSuiteDeployment and @Test(groups = "standalone")
     * enables these tests to be run, as you'd expect, either standalone
     * or in the Tomcat container. */

    /** Test string that's ten characters long, of which the last
     * character, and only the last character, is a space.
     * It's necessary that the number ten be a divisor of
     * {@link ResourceDocsTransformProvider#MAXIMUM_STRING_LENGTH}. */
    public static final String TEN_CHARACTERS_ENDING_WITH_A_SPACE =
            "123456789 ";

    /** Test string that's ten characters long, of which the last
     * character, and only the last character, is a space.
     * It's necessary that the number ten be a divisor of
     * {@link ResourceDocsTransformProvider#MAXIMUM_STRING_LENGTH}. */
    public static final String TEN_CHARACTERS_WITHOUT_A_SPACE =
            "1234567890";


    /** Run tests of string trimming. */
    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testTrimming() {
        // Require MAXIMUM_STRING_LENGTH to be a multiple of 10,
        // because the correctness of these tests relys on that.
        Assert.assertEquals(MAXIMUM_STRING_LENGTH % 10, 0,
                "MAXIMUM_STRING_LENGTH must be a multiple of 10");

        // Basic sanity checks: null and empty string.
        Assert.assertEquals(trim(null), null);
        String empty = "";
        Assert.assertTrue(trim(empty) == empty,
                "Empty string should be left untouched by trimming");

        // More complicated.
        String abcdef = "Abc def";
        Assert.assertTrue(trim(abcdef) == abcdef,
                "Simple string should be left untouched by trimming");

        String string1 = StringUtils.repeat(TEN_CHARACTERS_ENDING_WITH_A_SPACE,
                MAXIMUM_STRING_LENGTH
                / TEN_CHARACTERS_ENDING_WITH_A_SPACE.length());
        Assert.assertTrue(trim(string1) == string1,
                "String of maximum length should be left unchanged");

        String string2 = string1 + "1";
        String string1Stripped = StringUtils.stripEnd(string1, " ");
        Assert.assertEquals(trim(string2),
                string1Stripped,
                "String of maximum length plus one should be trimmed "
                + "to last space");

        String string3 = StringUtils.repeat(TEN_CHARACTERS_WITHOUT_A_SPACE,
                MAXIMUM_STRING_LENGTH
                / TEN_CHARACTERS_WITHOUT_A_SPACE.length());
        Assert.assertTrue(trim(string3) == string3,
                "String of maximum length should be left unchanged");

        String string4 = string3 + "1";
        Assert.assertEquals(trim(string4),
                string3,
                "String of maximum length plus one, with no spaces,"
                + " should be trimmed to maximum length");

    }
}
