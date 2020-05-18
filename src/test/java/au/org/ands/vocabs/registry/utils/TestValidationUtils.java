/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.utils;

import java.lang.invoke.MethodHandles;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.api.validation.ValidationUtils;

/** Tests of the methods of the {@link ValidationUtils} class. */
public class TestValidationUtils {

    // Leave logger here, though it is unused. We might want to use
    // it later.
    /** Logger for this class. */
    @SuppressWarnings("unused")
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Run tests of the field validation methods. */
    @Test
    public void testFieldValidation() {
        Assert.assertTrue(ValidationUtils.isValidURL(
                "http://invalid.com/with-\"-in"));
    }

    /** Run tests of the HTML validation method. */
    @Test
    public void testHTMLValidation() {
        Assert.assertTrue(ValidationUtils.isValidHTML(
                "<p>Richard's first test on<br />the new devl.</p>"));

        Assert.assertTrue(ValidationUtils.isValidHTML(
                "Richard's first test on the new devl.<br />Test.<br />"
                + "<ul><li>A list.</li><li>Second element.</li></ul>"
                + "<br />End."));

        String testString = "Richard's first test on the new devl.<br />"
                + "<strong>asdf</strong><br /><em>italics</em><br />"
                + "asdf<br />A link "
                + "<a href=\"http://www.google.com/\" "
                + "target=\"_blank\" rel=\"nofollow\">"
                + "http://www.google.com/</a>.";

//        testString = ValidationUtils.cleanHTML(testString);
//        logger.info("After cleaning: " + testString);
        Assert.assertTrue(ValidationUtils.isValidHTML(testString));

        testString = "Richard's first test on the new devl.<br />"
                + "<strong>asdf</strong><br /><em>italics</em><br />"
                + "asdf<br />A link "
                + "<a href=\"http://www.google.com/\" "
                + "target=\"_blank\" rel=\"nofollow noopener noreferrer\">"
                + "http://www.google.com/</a>.";

//        testString = ValidationUtils.cleanHTML(testString);
//        logger.info("After cleaning: " + testString);
        Assert.assertTrue(ValidationUtils.isValidHTML(testString));

        testString = "Richard's first test on the new devl.<br />"
                + "<strong>asdf</strong><br /><em>italics</em><br />"
                + "asdf<br />A link "
                + "<a href=\"http://www.google.com/\" target=\"_blank\" "
                + "rel=\"nofollow noopener noreferrer\">"
                + "http://www.google.com/</a>.";

        Assert.assertTrue(ValidationUtils.isValidHTML(testString));


        testString = "EuroVoc:&nbsp;"
                + "<a href=\"../../../en/EuroVoc/geography\" "
                + "target=\"_blank\" rel=\"nofollow noopener "
                + "noreferrer\">geography</a>";

//        Assert.assertTrue(ValidationUtils.isValidHTML(testString));

        testString = "Link:&nbsp;&eacute;&mdash;Gänsefüßchen &asymp; "
                + "Γειά σας नमस्ते 你好<a href=\"https://vocabs.ands.org.au/"
                + "aodn-geographic-extents-vocabulary\" target=\"_blank\" "
                + "rel=\"nofollow noopener noreferrer\">https://vocabs."
                + "ands.org.au/aodn-geographic-extents-vocabulary<span>"
                + "<span> (link is external)</span></span></a>"
                + "SKOS File:&nbsp; <a href=\"http://vocabs.ands.org.au/"
                + "repository/api/download/910/aodn_aodn-geographic-"
                + "extents-vocabulary_version-2-0.rdf\" target=\"_blank\" "
                + "rel=\"nofollow noopener noreferrer\">http://vocabs."
                + "ands.org.au/repository/api/download/910/aodn_aodn-"
                + "geographic-extents-vocabulary_version-2-0.rdf<span>"
                + "<span> (link is external)</span></span></a>"
                + "English Abstract:";

        // Various experiments:
//        logger.info("Stripped 1: "
//                + Jsoup.parse(testString).text());
//
//        logger.info("Stripped 2: "
//                + Jsoup.clean(testString, Whitelist.none()));
//
//        Document newDocument = Jsoup.parse(testString,
//                "", Parser.htmlParser());
//        newDocument.outputSettings().escapeMode(EscapeMode.base);
//        newDocument.outputSettings().charset(StandardCharsets.UTF_8);
//
//        logger.info("Stripped 3: "
//                + newDocument.text());

        // The result of the first experiment seems to be acceptable,
        // so use it:

        Assert.assertEquals(Jsoup.parse(testString).text(),
                "Link: é—Gänsefüßchen ≈ Γειά σας नमस्ते 你好https://vocabs.ands."
                + "org.au/aodn-geographic-extents-vocabulary (link is "
                + "external)SKOS File: http://vocabs.ands.org.au/"
                + "repository/api/download/910/aodn_aodn-geographic-"
                + "extents-vocabulary_version-2-0.rdf (link is external)"
                + "English Abstract:");
    }

    /** Test validation of ROR values. */
    @Test
    public void testRORValidation() {
        Assert.assertFalse(ValidationUtils.isValidROR(null),
                "Null regarded as valid ROR");
        Assert.assertFalse(ValidationUtils.isValidROR("0"),
                "Wrong length regarded as valid ROR");
        Assert.assertFalse(ValidationUtils.isValidROR("03yrm5c266"),
                "Wrong length regarded as valid ROR");
        Assert.assertFalse(ValidationUtils.isValidROR("03yrm5c2Z"),
                "Wrong character allowed in ROR");
        Assert.assertTrue(ValidationUtils.isValidROR("03yrm5c26"),
                "Valid ROR rejected");
    }

}
