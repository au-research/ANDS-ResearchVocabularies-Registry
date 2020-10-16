/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.test;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.utils.language.LanguageSubtagRegistry;
import au.org.ands.vocabs.registry.utils.language.ParsedLanguage;
import au.org.ands.vocabs.toolkit.test.arquillian.ArquillianBaseTest;

/** Unit tests of the language subtag registry. */
@Test
public class LanguageSubtagRegistryTests extends ArquillianBaseTest {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Run tests of one valid tag.
     * @param tag The tag to be tested.
     * @param canonicalTag The expected canonical form of the tag.
     * @param briefDescription The expected brief description.
     * @param description The expected (full) description.
     */
    private void testOneValidTag(final String tag,
            final String canonicalTag,
            final String briefDescription, final String description) {
        LanguageSubtagRegistry lsr = LanguageSubtagRegistry.getLSR();
        ParsedLanguage parsedLanguage;

        parsedLanguage = lsr.parseTag(tag);
        if (parsedLanguage.getErrors() != null) {
            logger.info(parsedLanguage.getErrors().toString());
        }
        Assert.assertTrue(parsedLanguage.isValid(),
                tag + " parsed as invalid");
        Assert.assertEquals(parsedLanguage.getCanonicalForm(),
                canonicalTag, tag + " not canonicalized correctly");
        Assert.assertEquals(parsedLanguage.getBriefDescription(),
                briefDescription, tag + " not briefly described correctly");
        Assert.assertEquals(parsedLanguage.getDescription(),
                description, tag + " not described correctly");
    }

    /** Run tests of one valid tag. Use this
     * variant of the method, where the canonical form of the tag
     * is identical with the tag being tested.
     * @param tag The tag to be tested, where the expected canonical
     *      form is identical.
     * @param briefDescription The expected brief description.
     * @param description The expected (full) description.
     */
    private void testOneValidTag(final String tag,
            final String briefDescription, final String description) {
        testOneValidTag(tag, tag, briefDescription, description);
    }

    /** Run tests of one valid tag. Use this
     * variant of the method, where the expected canonical form of the tag
     * is identical with the tag being tested, and
     * the brief and full descriptions are expected to be the same.
     * @param tag The tag to be tested.
     * @param description The expected brief and full description.
     */
    private void testOneValidTag(final String tag, final String description) {
        testOneValidTag(tag, tag, description, description);
    }

    /** Run tests of one invalid tag.
     * @param tag The tag to be tested.
     * @param problem A description of the expected error(s).
     * @param expectedErrors The text(s) of the expected error(s).
     */
    private void testOneInvalidTag(final String tag,
            final String problem, final String... expectedErrors) {
        LanguageSubtagRegistry lsr = LanguageSubtagRegistry.getLSR();
        ParsedLanguage parsedLanguage;

        parsedLanguage = lsr.parseTag(tag);
        Assert.assertFalse(parsedLanguage.isValid(),
                StringUtils.capitalize(problem) + ", but parsed as valid");
        Assert.assertEquals(parsedLanguage.getErrors(),
                Arrays.asList(expectedErrors),
                "Did not get exactly error message for " + problem);
    }

    /** Run tests of the language subtag registry. These are tests
     * of the "legacy" values. */
    @Test
    public void testLSRLegacyValues() {
        testOneValidTag("de", "German");
        testOneValidTag("en", "English");
        testOneValidTag("es", "Spanish");
        testOneValidTag("fr", "French");
        testOneValidTag("it", "Italian");
        testOneValidTag("ja", "Japanese");
        testOneValidTag("mi", "Māori");
        testOneValidTag("ru", "Russian");
        testOneValidTag("zh", "Chinese");

        testOneValidTag("en-CA", "English", "English (Canada)");
        testOneValidTag("en-MT", "English",  "English (Malta)");
        testOneValidTag("es-UY", "Spanish", "Spanish (Uruguay)");

        // Check resolution of values that aren't valid language tags.
        LanguageSubtagRegistry lsr = LanguageSubtagRegistry.getLSR();
        ParsedLanguage parsedLanguage;

        // English
        parsedLanguage = lsr.parseTag("English");
        Assert.assertFalse(parsedLanguage.isValid(),
                "English parsed as valid");

        // Turkish
        parsedLanguage = lsr.parseTag("Turkish");
        Assert.assertFalse(parsedLanguage.isValid(),
                "Turkish parsed as valid");
    }

    /** Run tests of the language subtag registry. These are tests
     * are of invalid tags; we confirm that the validation produces
     * the expected error messages.
     */
    @Test
    public void testLSRErrorMessages() {
        // null
        testOneInvalidTag(null, "null tag",
                LanguageSubtagRegistry.ERROR_NULL_TAG);

        // Empty string
        testOneInvalidTag("", "empty string",
                LanguageSubtagRegistry.ERROR_EMPTY_TAG);

        // One-character string; this is reported as unknown language subtag.
        testOneInvalidTag("a", "one-character tag",
                LanguageSubtagRegistry.ERROR_UNKNOWN_LANGUAGE_SUBTAG);

        // Internal space
        testOneInvalidTag("en GB", "internal space",
                LanguageSubtagRegistry.ERROR_INTERNAL_SPACE);

        // Begins with a hyphen
        testOneInvalidTag("-en", "begins with hyphen",
                LanguageSubtagRegistry.ERROR_BEGIN_END_HYPHEN);

        // Ends with a hyphen
        testOneInvalidTag("en-", "ends with hyphen",
                LanguageSubtagRegistry.ERROR_BEGIN_END_HYPHEN);

        // Empty subtag
        testOneInvalidTag("en--GB", "missing subtag",
                LanguageSubtagRegistry.ERROR_EMPTY_SUBTAG);

        // Invalid character
        testOneInvalidTag("en@", "invalid character",
                LanguageSubtagRegistry.ERROR_INVALID_CHARACTER);

        // Grandfathered, but no preferred value.
        testOneInvalidTag("cel-gaulish", "grandfathered, no preferred value",
                LanguageSubtagRegistry.ERROR_GRANDFATHERED_DEPRECATED);

        // Begins with private use.
        // This is well-formed, but we consider it invalid.
        testOneInvalidTag("x-abc", "only private use",
                LanguageSubtagRegistry.ERROR_BEGINS_PRIVATE_USE);

        // Private use subtag has a component that's too long.
        testOneInvalidTag("en-x-abcdefghi",
                "private use subtag with too-long component",
                LanguageSubtagRegistry.ERROR_PRIVATE_USE_COMPONENT_LENGTH);

        // More than one private use subtag.
        testOneInvalidTag("en-x-abc-x-def", "more than one private use subtag",
                LanguageSubtagRegistry.ERROR_MULTIPLE_PRIVATE_USE);

        // Test of duplicate extension singleton.
        testOneInvalidTag("en-t-bbb-t-ccc", "duplicate extension singleton",
                LanguageSubtagRegistry.ERROR_DUPLICATE_SINGLETON);

        // Test of invalid extension singleton.
        testOneInvalidTag("en-a-bbb", "invalid extension singleton",
                LanguageSubtagRegistry.ERROR_INVALID_SINGLETON);

        // Test of invalid extension singleton after a valid one.
        testOneInvalidTag("en-t-bbb-a-ccc", "invalid extension singleton",
                LanguageSubtagRegistry.ERROR_INVALID_SINGLETON);

        // Extension has a component that's too long.
        testOneInvalidTag("en-t-abcdefghi", "extension component too long",
                LanguageSubtagRegistry.ERROR_EXTENSION_COMPONENT_LENGTH);

        // Only extensions.
        testOneInvalidTag("u-abcd-t-efgh", "only extensions",
                LanguageSubtagRegistry.ERROR_BEGINS_EXTENSION);

        // Private use language subtag.
        testOneInvalidTag("qab", "private use language subtag",
                LanguageSubtagRegistry.ERROR_PRIVATE_USE_LANGUAGE_UNSUPPORTED);

        // Uhknown language subtag.
        testOneInvalidTag("ex-GB", "unknown language subtag",
                LanguageSubtagRegistry.ERROR_UNKNOWN_LANGUAGE_SUBTAG);

        // Disallowed language
        testOneInvalidTag("mis", "disallowed language subtag",
                LanguageSubtagRegistry.ERROR_DISALLOWED_LANGUAGE_SUBTAG);

        // Subtag too long.
        testOneInvalidTag("en-toooolong", "subtag too long",
                LanguageSubtagRegistry.ERROR_SUBTAG_TOO_LONG);

        // Misplaced extlang
        testOneInvalidTag("zh-Hans-cmn-CN", "misplaced extlang",
                LanguageSubtagRegistry.ERROR_EXTLANG_NOT_ALLOWED);

        // Misplaced script
        testOneInvalidTag("en-GB-Latn", "misplaced script",
                LanguageSubtagRegistry.ERROR_SCRIPT_NOT_ALLOWED);

        // Misplaced two-alpha region
        testOneInvalidTag("en-GB-DE", "misplaced two-alpha region",
                LanguageSubtagRegistry.ERROR_REGION_NOT_ALLOWED);

        // Misplaced three-digit region
        testOneInvalidTag("de-1996-001", "misplaced three-digit region",
                LanguageSubtagRegistry.ERROR_REGION_NOT_ALLOWED);

        // Unknown extlang
        testOneInvalidTag("zh-cmx-Hans-CN", "unknown extlang",
                LanguageSubtagRegistry.ERROR_UNKNOWN_EXTLANG_SUBTAG);

        // Extlang whose prefix doesn't match the language subtag
        testOneInvalidTag("en-abh", "extlang prefix doesn't match",
                LanguageSubtagRegistry.ERROR_EXTLANG_PREFIX_DIFFERS);

        // Unknown script
        testOneInvalidTag("en-Latx", "unknown script",
                LanguageSubtagRegistry.ERROR_UNKNOWN_SCRIPT_SUBTAG);

        // Unknown two-alpha region
        testOneInvalidTag("en-GX", "unknown two-alpha region",
                LanguageSubtagRegistry.ERROR_UNKNOWN_REGION_SUBTAG);

        // Unknown three-digit region
        testOneInvalidTag("en-004", "unknown three-digit region",
                LanguageSubtagRegistry.ERROR_UNKNOWN_REGION_SUBTAG);

        // Unknown four-character variant
        testOneInvalidTag("en-1997", "unknown variant",
                LanguageSubtagRegistry.ERROR_UNKNOWN_VARIANT_SUBTAG);

        // Unknown five-to-eight-character variant
        testOneInvalidTag("en-19970", "unknown variant",
                LanguageSubtagRegistry.ERROR_UNKNOWN_VARIANT_SUBTAG);

        // Duplicate four-character variant
        testOneInvalidTag("en-1996-1996", "duplicate variant",
                LanguageSubtagRegistry.ERROR_DUPLICATE_VARIANT_SUBTAG);

        // Duplicate five-character variant
        testOneInvalidTag("kea-barla-barla", "duplicate variant",
                LanguageSubtagRegistry.ERROR_DUPLICATE_VARIANT_SUBTAG);

        // Future work TODO variant but none of the prefixes match
        // sl-IT-biske
//        testOneInvalidTag("sl-IT-biske", "variant not allowed for prefix",
//                LanguageSubtagRegistry.ERROR_INVALID_VARIANT_FOR_PREFIX);

        // Future work TODO ordering of multiple variants (overlaps with above)
        // sl-IT-1994-rozaj-biske
        // sl-IT-biske-rozaj
    }


    /** Run tests of the language subtag registry. These tests
     * go beyond "legacy" tags, and tags with straightforward errors,
     * and explore some of the many possibilities of BCP 47. */
    @Test
    public void testLSR() {
        LanguageSubtagRegistry lsr = LanguageSubtagRegistry.getLSR();
        ParsedLanguage parsedLanguage;

        // Grandfathered, with preferred value.
        parsedLanguage = lsr.parseTag("art-lojban");
        Assert.assertTrue(parsedLanguage.isValid(),
                "Grandfathered, with preferred value, but parsed as invalid");
        Assert.assertEquals(parsedLanguage.getCanonicalForm(), "jbo",
                "art-lojban not parsed as jbo");
        Assert.assertEquals(parsedLanguage.getBriefDescription(),
                "Lojban", "art-lojban not briefly described correctly");
        Assert.assertEquals(parsedLanguage.getDescription(), "Lojban",
                "art-lojban not described as Lojban");

        // Redundant, with preferred value
        parsedLanguage = lsr.parseTag("zh-wuu");
        Assert.assertTrue(parsedLanguage.isValid(),
                "Redundant, with preferred value, but parsed as invalid");
        Assert.assertEquals(parsedLanguage.getCanonicalForm(),
                "wuu", "zh-wuu not parsed as wuu");
        Assert.assertEquals(parsedLanguage.getBriefDescription(),
                "Wu Chinese", "zh-wuu not briefly described correctly");
        Assert.assertEquals(parsedLanguage.getDescription(),
                "Wu Chinese", "zh-wuu not described as Wu Chinese");

        // Redundant, without preferred value
        parsedLanguage = lsr.parseTag("zh-Hant-TW");
        Assert.assertTrue(parsedLanguage.isValid(),
                "Redundant, without preferred value, but parsed as invalid");
        Assert.assertEquals(parsedLanguage.getCanonicalForm(),
                "zh-Hant-TW", "zh-Hant-TW not parsed as zh-Hant-TW");
        Assert.assertEquals(parsedLanguage.getBriefDescription(),
                "Chinese", "zh-Hant-TW not briefly described correctly");
        Assert.assertEquals(parsedLanguage.getDescription(),
                "Chinese (Han (Traditional variant), Taiwan, "
                        + "Province of China)",
                "zh-Hant-TW not described correctly");

        // Test of private use subtag with multiple components.
        parsedLanguage = lsr.parseTag("en-x-abcd-efgh");
        Assert.assertTrue(parsedLanguage.isValid(),
                "Valid tag with private use subtag, but parsed as invalid");
        Assert.assertEquals(parsedLanguage.getPrivateUse(), "x-abcd-efgh",
                "Incorrectly-parsed private use tag");
        Assert.assertEquals(parsedLanguage.getCanonicalForm(), "en-x-abcd-efgh",
                "Incorrectly-parsed private use tag");
        Assert.assertEquals(parsedLanguage.getBriefDescription(),
                "English", "en-x-abcd-efgh not briefly described correctly");
        Assert.assertEquals(parsedLanguage.getDescription(),
                "English (x-abcd-efgh)",
                "en-x-abcd-efgh not described as English");

        // Test of canonicalization of extensions, by sorting them by singleton.
        parsedLanguage = lsr.parseTag("en-u-abcd-t-efgh");
        Assert.assertTrue(parsedLanguage.isValid(),
                "Valid tag with extensions, but parsed as invalid");
        Assert.assertEquals(parsedLanguage.getExtensions(),
                Arrays.asList("t-efgh", "u-abcd"),
                "Extensions not extracted/sorted correctly");
        Assert.assertEquals(parsedLanguage.getCanonicalForm(),
                "en-t-efgh-u-abcd",
                "Canonical form with extensions not sorted correctly");
        Assert.assertEquals(parsedLanguage.getBriefDescription(),
                "English", "en-u-abcd-t-efgh not briefly described correctly");
        Assert.assertEquals(parsedLanguage.getDescription(),
                "English (t-efgh, u-abcd)",
                "en-u-abcd-t-efgh not described correctly");

        // Language subtag "in" has preferred value "id".
        testOneValidTag("in", "id", "Indonesian", "Indonesian");

        // Suppress-Script, am-Ethi
        testOneValidTag("am-Ethi", "am", "Amharic", "Amharic");

        // Canonicalization of region, en-gb
        testOneValidTag("en-gb", "en-GB",
                "English", "English (United Kingdom)");

        // Canonicalization of three-character region, en-003
        testOneValidTag("en-003", "en-003",
                "English", "English (North America)");

        // Canonicalization of subtags, Suppress-Script, EN-LATN-Gb
        testOneValidTag("EN-LATN-Gb", "en-GB", "English",
                "English (United Kingdom)");

        // Canonicalization of subtags, No Suppress-Script, EN-CYRL-Gb
        testOneValidTag("EN-CYRL-Gb", "en-Cyrl-GB", "English",
                "English (Cyrillic, United Kingdom)");

        // Canonicalization of region, my-BU
        testOneValidTag("my-BU", "my-MM",
                "Burmese", "Burmese (Myanmar)");

        // Canonicalization of variants
        testOneValidTag("ja-Latn-heploc", "ja-Latn-alalc97",
                "Japanese",
                "Japanese (Latin, ALA-LC Romanization, 1997 edition)");

        // Examples taken from RFC 5646

        // Section 4.1, point 6, sl-IT-rozaj-biske-1994
        testOneValidTag("sl-IT-rozaj-biske-1994", "sl-IT-rozaj-biske-1994",
                "Slovenian",
                "Slovenian (Italy, Resian, The San Giorgio dialect of Resian, "
                        + "Standardized Resian orthography)");

        // Section 4.2. tlh-Kore-AQ-fonipa
        // (Klingon, Korean script, as used in Antarctica, IPA
        //        phonetic transcription)
        testOneValidTag("tlh-Kore-AQ-fonipa", "Klingon",
                "Klingon (Korean (alias for Hangul + Han), "
                        + "Antarctica, International Phonetic Alphabet)");

        // Section 4.5, point 3, extlang replacement.
        testOneValidTag("zh-hak", "hak", "Hakka Chinese", "Hakka Chinese");

        /// Appendix A. Variants, sl-rozaj-biske
        testOneValidTag("sl-rozaj-biske", "Slovenian",
                "Slovenian (Resian, The San Giorgio dialect of Resian)");

        // End of examples taken from RFC 5646.

        // Some examples taken from PoolParty:

        // Japanese (Japan,JP) [ja-JP-u-ca-japanese-x-lvariant-JP]
        testOneValidTag("ja-JP-u-ca-japanese-x-lvariant-JP",
                "ja-JP-u-ca-japanese-x-lvariant-jp",
                "Japanese", "Japanese (Japan, u-ca-japanese, x-lvariant-jp)");

        // Serbian (Latin,Bosnia and Herzegovina) [sr-Latn-BA]
        testOneValidTag("sr-Latn-BA", "Serbian",
                "Serbian (Latin, Bosnia and Herzegovina)");

        // Thai (Thailand,TH) [th-TH-u-nu-thai-x-lvariant-TH]
        testOneValidTag("th-TH-u-nu-thai-x-lvariant-TH",
                "th-TH-u-nu-thai-x-lvariant-th",
                "Thai", "Thai (Thailand, u-nu-thai, x-lvariant-th)");

        // End of examples taken from PoolParty.

        // Add more more nice values here
//        testOneValidTag("", "", "", "");

        // To log actual errors, use:
//      logger.info(parsedLanguage.getErrors().toString());
    }

    /** Run some tests of the language subtag registry. These tests
     * are of tags that should not be parsed as valid, but currently are.
     * It is future work to add extra validation so that these
     * tags are rejected as invalid.
     * For now, we "manage" them by making sure that they don't
     * cause us any harm. */
    @Test
    public void testTestsThatPassButShouldFail() {
        // Uh-oh, these tests are wrong. See the end of
        // testLSRErrorMessages() for future work that should be done.
        // For now, we "manage" them by making sure that they don't
        // cause us any harm.
        testOneValidTag("sl-IT-biske", "sl-IT-biske",
                "Slovenian",
                "Slovenian (Italy, The San Giorgio dialect of Resian)");
        testOneValidTag("sl-IT-1994-rozaj-biske", "sl-IT-1994-rozaj-biske",
                "Slovenian",
                "Slovenian (Italy, Standardized Resian orthography, Resian, "
                + "The San Giorgio dialect of Resian)");
        testOneValidTag("sl-IT-biske-rozaj", "sl-IT-biske-rozaj",
                "Slovenian",
                "Slovenian (Italy, The San Giorgio dialect of Resian, Resian)");
    }

}
