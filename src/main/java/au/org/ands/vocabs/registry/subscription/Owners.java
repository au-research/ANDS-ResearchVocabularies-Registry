/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.subscription;

import java.lang.invoke.MethodHandles;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.dao.OwnerDAO;
import au.org.ands.vocabs.registry.db.entity.Owner;
import au.org.ands.vocabs.toolkit.utils.ApplicationCacheProvider;

/** Cache of Owners for use in subscriptions. */
public final class Owners {

    /** Special owner name that means "all owners". It is cached
     * with the owner Id {@link #ALL_OWNERS_OWNER_ID}.
     */
    public static final String ALL_OWNERS = "*";

    /** Special owner Id that means "all owners". It is cached
     * against the owner name {@link #ALL_OWNERS}.
     */
    public static final Integer ALL_OWNERS_OWNER_ID = 0;

    /** Logger for this class. */
    private static Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Private constructor for a utility class. */
    private Owners() {
    }

    /** Name of the cache that stores Owners .*/
    private static final String OWNERS_CACHE = "ownersCache";

    /** The cache of Owners. */
    private static Cache<String, Integer> cache;

    static {
        init();
    }

    /** Initialize the cache. This method is invoked by a static block. */
    private static void init() {
        logger.info("In Owners.init()");
        CacheManager manager = ApplicationCacheProvider.getCacheManager();

        // Define a cache configuration.
        MutableConfiguration<String, Integer> cacheConfiguration =
                new MutableConfiguration<String, Integer>().
                setStoreByValue(false).
                setTypes(String.class, Integer.class).
                setCacheLoaderFactory(FactoryBuilder.factoryOf(
                        OwnersCacheLoader.class)).
                setReadThrough(true);

        // Create the cache.
        cache = manager.createCache(OWNERS_CACHE, cacheConfiguration);

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

        // Add the "magic" key "*" -> 0.
        cache.put(ALL_OWNERS, ALL_OWNERS_OWNER_ID);
    }

    /** Close the cache, if it is open. */
    public static void close() {
        logger.info("In Owners.close()");
        if (cache != null) {
            cache.close();
            cache = null;
        }
    }

    /** Get an Owner ID, given the owner name. This method does <i>not</i>
     * create a new Owner entity, if there is no existing Owner with
     * this name.
     * @param ownerName The owner name to be looked up.
     * @return The Owner ID of the owner, or null, if there is no such Owner.
     */
    public static Integer getOwnerId(final String ownerName) {
        return cache.get(ownerName);
    }

    /** Require that there be an Owner, with the given owner name.
     * If there is not currently such an Owner, create it. Subsequent calls
     * to {@link #getOwnerId(String)} will return the Owner Id of the Owner.
     * @param ownerName The owner name, for which an Owner is to be required
     *      to exist.
     */
    public static synchronized void requireOwner(final String ownerName) {
        if (cache.get(ownerName) == null) {
            Owner owner = new Owner();
            owner.setOwner(ownerName);
            OwnerDAO.saveOwner(owner);
        }
    }

    /** Clear the cache. Don't call this method in normal use!
     * It's provided for use by the test suite, so that the cache
     * can be cleared between tests. See the
     * <code>ArquillianTestUtils.clearDatabase(DatabaseSelector)</code>
     * method.
     */
    public static synchronized void clear() {
        cache.clear();
    }

}
