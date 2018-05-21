/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.utils;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Context class that provides access to the system-wide JCache
 * Caching Provider and Cache Manager.
 */
public final class ApplicationCacheProvider {

    /** Logger for this class. */
    private static Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Path to the file ehcache.xml. This file must exist. */
    public static final String EHCACHE_XML_PATH = "/ehcache.xml";

    /** Private constructor for a utility class. */
    private ApplicationCacheProvider() {
    }

    /** The system-wide Caching Provider. */
    private static CachingProvider cachingProvider = null;

    /** The system-wide Cache Manager. */
    private static CacheManager cacheManager = null;

    /** Get the system-wide Caching Provider.
     * @return The system-wide Caching Provider.
     */
    public static synchronized CachingProvider getCachingProvider() {
        if (cachingProvider == null) {
            cachingProvider = Caching.getCachingProvider();
            logger.info("default URI: " + cachingProvider.getDefaultURI());
        }
        return cachingProvider;
    }

    /** Get the system-wide Cache Manager.
     * @return The system-wide Cache Manager.
     */
    public static synchronized CacheManager getCacheManager() {
        if (cacheManager == null) {
            URI ehcacheXmlURL = null;
            try {
                ehcacheXmlURL = ApplicationCacheProvider.class.getResource(
                        EHCACHE_XML_PATH).toURI();
            } catch (NullPointerException | URISyntaxException e) {
                logger.error("Unable to load ehcache.xml (" + e + ")");
            }
            if (ehcacheXmlURL == null) {
                logger.info("Getting CacheManager without ebcache.xml.");
                cacheManager = getCachingProvider().getCacheManager();
            } else {
                logger.info("Getting CacheManager with ebcache.xml.");
                cacheManager = getCachingProvider().getCacheManager(
                        ehcacheXmlURL,
                        ApplicationCacheProvider.class.getClassLoader());
            }
        }
        return cacheManager;
    }

    /** Shut down the system-wide Cache Manager and Caching Provider. */
    public static synchronized void shutdown() {
        if (cacheManager != null) {
            logger.info("Closing system-wide CacheManager");
            cacheManager.close();
            cacheManager = null;
        }
        if (cachingProvider != null) {
            logger.info("Closing system-wide CachingProvider");
            cachingProvider.close();
            cachingProvider = null;
        }
    }
}
