/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.utils;

import static au.org.ands.vocabs.registry.workflow.provider.transform.ResourceDocsTransformProvider.MAXIMUM_STRING_LENGTH_BYTES;

import java.nio.charset.StandardCharsets;

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

    /** Test string that's ten characters long, each of which is
     * one byte in UTF-8, and of which the last
     * character, and only the last character, is a space.
     * It's necessary that the number ten be a divisor of
     * {@link ResourceDocsTransformProvider#MAXIMUM_STRING_LENGTH_BYTES}. */
    public static final String TEN_BYTES_ENDING_WITH_A_SPACE =
            "123456789 ";

    /** Test string that's ten characters long, each of which is
     * one byte in UTF-8, and of which the last
     * character, and only the last character, is a space.
     * It's necessary that the number ten be a divisor of
     * {@link ResourceDocsTransformProvider#MAXIMUM_STRING_LENGTH_BYTES}. */
    public static final String TEN_BYTES_WITHOUT_A_SPACE =
            "1234567890";

    /** Test string that's ten bytes long, of which the last
     * bytes, and only the last byte, is a space.
     * It's necessary that the number ten be a divisor of
     * {@link ResourceDocsTransformProvider#MAXIMUM_STRING_LENGTH_BYTES}. */
    public static final String TEN_BYTES_MULTIBYTE_ENDING_WITH_A_SPACE =
            "æ😂尝 ";

    /** Test string that's ten bytes long, of which the last
     * bytes, and only the last byte, is a space.
     * It's necessary that the number ten be a divisor of
     * {@link ResourceDocsTransformProvider#MAXIMUM_STRING_LENGTH_BYTES}. */
    public static final String TEN_BYTES_MULTIBYTE_WITHOUT_A_SPACE =
            "Aæ😂尝";

    // Here's a test of the variant method that takes an extra
    // limit parameter.
//    @Test
//    @SuppressWarnings("checkstyle:MagicNumber")
//    public void testDecode() {
//        ResourceDocsTransformProvider rdtp =
//                new ResourceDocsTransformProvider();
//        String test = "Aæ😂尝试";
//        for (int i = 1; i <= 16; i++) {
//            System.out.println("Size " + i + ": " + rdtp.trim(test, i));
//        }
//    }

    /** Run tests of string trimming with strings in which every
     * character takes up only one byte in UTF-8. */
    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testTrimmingOneByteCharacters() {
        ResourceDocsTransformProvider rdtp =
                new ResourceDocsTransformProvider();

        // Require MAXIMUM_STRING_LENGTH_BYTES to be a multiple of 10,
        // because the correctness of these tests relys on that.
        Assert.assertEquals(MAXIMUM_STRING_LENGTH_BYTES % 10, 0,
                "MAXIMUM_STRING_LENGTH_BYTES must be a multiple of 10");

        // Basic sanity checks: null and empty string.
        Assert.assertEquals(rdtp.trim(null), null);
        String empty = "";
        Assert.assertTrue(rdtp.trim(empty) == empty,
                "Empty string should be left untouched by trimming");

        // More complicated.
        String abcdef = "Abc def";
        Assert.assertTrue(rdtp.trim(abcdef) == abcdef,
                "Simple string should be left untouched by trimming");

        String string1 = StringUtils.repeat(TEN_BYTES_ENDING_WITH_A_SPACE,
                MAXIMUM_STRING_LENGTH_BYTES
                / TEN_BYTES_ENDING_WITH_A_SPACE.length());
        Assert.assertTrue(rdtp.trim(string1) == string1,
                "String of maximum length should be left unchanged");

        String string2 = string1 + "1";
        String string1Stripped = StringUtils.stripEnd(string1, " ");
        Assert.assertEquals(rdtp.trim(string2),
                string1Stripped,
                "String of maximum length plus one should be trimmed "
                + "to last space");

        String string3 = StringUtils.repeat(TEN_BYTES_WITHOUT_A_SPACE,
                MAXIMUM_STRING_LENGTH_BYTES
                / TEN_BYTES_WITHOUT_A_SPACE.length());
        Assert.assertTrue(rdtp.trim(string3) == string3,
                "String of maximum length should be left unchanged");

        String string4 = string3 + "1";
        Assert.assertEquals(rdtp.trim(string4),
                string3,
                "String of maximum length plus one, with no spaces,"
                + " should be trimmed to maximum length");
    }

    /** Run tests of string trimming with strings in which
     * there are characters that take up more than one byte in UTF-8. */
    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testTrimmingMultibyteCharacters() {
        ResourceDocsTransformProvider rdtp =
                new ResourceDocsTransformProvider();

        // Sanity check. Require TEN_BYTES_MULTIBYTE_ENDING_WITH_A_SPACE to
        // be, in fact, 10 bytes long.
        int tbmewasLength = TEN_BYTES_MULTIBYTE_ENDING_WITH_A_SPACE.
                getBytes(StandardCharsets.UTF_8).length;
        Assert.assertEquals(tbmewasLength, 10,
                "TEN_BYTES_MULTIBYTE_ENDING_WITH_A_SPACE's length isn't "
                + "exactly 10");

        // Sanity check. Require TEN_BYTES_MULTIBYTE_WITHOUT_A_SPACE to
        // be, in fact, 10 bytes long.
        int tbmwasLength = TEN_BYTES_MULTIBYTE_WITHOUT_A_SPACE.
                getBytes(StandardCharsets.UTF_8).length;
        Assert.assertEquals(tbmwasLength, 10,
                "TEN_BYTES_MULTIBYTE_WITHOUT_A_SPACE's length isn't "
                + "exactly 10");

        String string1 = StringUtils.repeat(
                TEN_BYTES_MULTIBYTE_ENDING_WITH_A_SPACE,
                MAXIMUM_STRING_LENGTH_BYTES / tbmewasLength);
        Assert.assertTrue(rdtp.trim(string1) == string1,
                "String of maximum length should be left unchanged");

        String string2 = string1 + "1";
        String string1Stripped = StringUtils.stripEnd(string1, " ");
        Assert.assertEquals(rdtp.trim(string2),
                string1Stripped,
                "String of maximum length plus one should be trimmed "
                + "to last space");

        String string3 = StringUtils.repeat(TEN_BYTES_WITHOUT_A_SPACE,
                MAXIMUM_STRING_LENGTH_BYTES / tbmwasLength);
        Assert.assertTrue(rdtp.trim(string3) == string3,
                "String of maximum length should be left unchanged");

        String string4 = string3 + "1";
        Assert.assertEquals(rdtp.trim(string4),
                string3,
                "String of maximum length plus one, with no spaces,"
                + " should be trimmed to maximum length");

        String string5 = string3 + "æ😂尝";
        Assert.assertEquals(rdtp.trim(string5),
                string3,
                "String of maximum length plus a few, with no spaces,"
                + " should be trimmed to maximum length");
    }

}
