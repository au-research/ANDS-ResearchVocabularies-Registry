/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.utils;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.icu.ICUFoldingFilterFactory;
import org.apache.lucene.analysis.icu.ICUTransformFilterFactory;
import org.apache.lucene.analysis.pattern.PatternReplaceFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.solr.analysis.TokenizerChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class for generating slugs.
 * Slugs are generated using a chain of Lucene analyzers.
 * Non-ASCII characters are transliterated into ASCII using
 * the ICU library and folded into lower-case.
 * Non-alpha, non-digit characters are turned into hyphens.
 *  */
public final class SlugGenerator {

    /** Logger for this class. */
    private static Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Private constructor for a utility class. */
    private SlugGenerator() {
    }

    /** The Lucene {@link TokenizerChain} used to generate slugs. */
    private static TokenizerChain tokenizerChain;

    static {
        initialize();
    }

    /** Initialize the Lucene {@link TokenizerChain} used to generate slugs.
     * Invoked by a static block.
     */
    private static void initialize() {
        HashMap<String, String> tokenizerMap = new HashMap<>();
        TokenizerFactory tokenizer = new StandardTokenizerFactory(tokenizerMap);

        HashMap<String, String> icuTransformMap = new HashMap<>();
        icuTransformMap.put("id", "Any-Latin");
        TokenFilterFactory icuTransform =
                new ICUTransformFilterFactory(icuTransformMap);

        HashMap<String, String> icuFoldingMap = new HashMap<>();
        TokenFilterFactory icuFolding =
                new ICUFoldingFilterFactory(icuFoldingMap);

        HashMap<String, String> periodPatternReplaceMap =
                new HashMap<>();
        periodPatternReplaceMap.put("pattern", "\\.");
        periodPatternReplaceMap.put("replacement", "-");
        TokenFilterFactory periodPatternReplace =
                new PatternReplaceFilterFactory(periodPatternReplaceMap);

        HashMap<String, String> nonASCIIPatternReplaceMap =
                new HashMap<>();
        nonASCIIPatternReplaceMap.put("pattern", "[^a-z0-9-]");
        nonASCIIPatternReplaceMap.put("replacement", "");
        TokenFilterFactory nonASCIIPatternReplace =
                new PatternReplaceFilterFactory(nonASCIIPatternReplaceMap);

        tokenizerChain = new TokenizerChain(tokenizer,
                new TokenFilterFactory[] {
                        icuTransform, icuFolding, periodPatternReplace,
                        nonASCIIPatternReplace});
    }

    /** Generate a slug for a String.
     * @param inputString The String for which a slug is to be generated.
     * @return The generated slug.
     */
    public static String generateSlug(final String inputString) {
        if (tokenizerChain == null) {
            logger.error("Unable to generate slug: "
                    + "tokenizerChain not initialized");
            return "Error";
        }
        String result = "";
        try (TokenStream stream =
                tokenizerChain.tokenStream(null, inputString)) {
            stream.reset();
            CharTermAttribute charTermAttribute =
                    stream.addAttribute(CharTermAttribute.class);
            while (stream.incrementToken()) {
                String term = charTermAttribute.toString();
                if (term.isEmpty()) {
                    continue;
                }
                if (!result.isEmpty()) {
                    result += "-";
                }
                result += term;
            }
            System.out.println("Output: " + result);
            stream.end();
            // System.out.println(line);
        } catch (IOException e) {
            logger.error("Exception while generating slug", e);
            return "Error";
        }
        return result;
    }

    /** Close the Lucene {@link TokenizerChain} used to generate slugs. */
    public static void shutdown() {
        if (tokenizerChain != null) {
            tokenizerChain.close();
            tokenizerChain = null;
        }
    }

}
