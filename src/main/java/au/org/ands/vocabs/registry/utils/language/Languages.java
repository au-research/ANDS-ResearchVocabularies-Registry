/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.utils.language;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.toolkit.utils.ApplicationCacheProvider;

/** Cache of ParsedLanguages for use in language vaidation. */
public final class Languages {

    /** Logger for this class. */
    private static Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Private constructor for a utility class. */
    private Languages() {
    }

    /** Name of the cache that stores ParsedLanguages .*/
    private static final String LANGUAGES_CACHE = "languagesCache";

    /** The cache of ParsedLanguages. */
    private static Cache<String, ParsedLanguage> cache;

    static {
        init();
    }

    /** Initialize the cache. This method is invoked by a static block. */
    private static void init() {
        logger.info("In Languages.init()");
        CacheManager manager = ApplicationCacheProvider.getCacheManager();

        // Define a cache configuration.
        MutableConfiguration<String, ParsedLanguage> cacheConfiguration =
                new MutableConfiguration<String, ParsedLanguage>().
                setStoreByValue(false).
                setTypes(String.class, ParsedLanguage.class).
                setCacheLoaderFactory(FactoryBuilder.factoryOf(
                        LsrCacheLoader.class)).
                setReadThrough(true);

        // Create the cache.
        cache = manager.createCache(LANGUAGES_CACHE, cacheConfiguration);

        // For confirming the heap config size.
        /*
        CacheRuntimeConfiguration<String, Integer> ehcacheConfig =
                (CacheRuntimeConfiguration<String, Integer>)
                cache.getConfiguration(Eh107Configuration.class).
                unwrap(CacheRuntimeConfiguration.class);
        logger.info("heap config size: "
                + ehcacheConfig.getResourcePools().getPoolForResource(
                        ResourceType.Core.HEAP).getSize());
         */

    }

    /** Close the cache, if it is open. */
    public static void close() {
        logger.info("In Languages.close()");
        if (cache != null) {
            cache.close();
            cache = null;
        }
    }

    /** Get a ParsedLanguage for a language tag, if the tag is valid.
     * @param tag The language tag to be looked up.
     * @return The ParsedLanguage of the tag, or null, if the tag is invalid.
     */
    public static ParsedLanguage getParsedLanguage(final String tag) {
        if (tag == null) {
            return null;
        }
        return cache.get(tag.trim().toLowerCase(Locale.ROOT));
    }

}
