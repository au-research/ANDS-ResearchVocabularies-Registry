/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.utils.language;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CacheLoader helper to populate the cache of ParsedLanguage entities. */
public class LsrCacheLoader implements CacheLoader<String, ParsedLanguage> {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** {@inheritDoc}
     * Load a ParsedLanguage entity for the tag.
     * @see javax.cache.integration.CacheLoader#load(java.lang.Object)
     */
    @Override
    public ParsedLanguage load(final String tag) throws CacheLoaderException {
        logger.info("Parsing a language tag for the cache: " + tag);
        LanguageSubtagRegistry lsr = LanguageSubtagRegistry.getLSR();
        ParsedLanguage parsedLanguage = lsr.parseTag(tag);
        if (parsedLanguage != null) {
            return parsedLanguage;
        } else {
            // Really shouldn't happen.
            logger.error("parseTag returned null for tag: " + tag);
            return null;
        }
    }

    /** {@inheritDoc}
     * @see javax.cache.integration.CacheLoader#loadAll(java.lang.Iterable)
     */
    @Override
    public Map<String, ParsedLanguage> loadAll(
            final Iterable<? extends String> keys)
            throws CacheLoaderException {
        Map<String, ParsedLanguage> map = new HashMap<>();
        for (String key : keys) {
            map.put(key, load(key));
        }
        return map;
    }

}
