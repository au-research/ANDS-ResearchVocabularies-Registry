/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.subscription;

import java.util.HashMap;
import java.util.Map;

import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;

import au.org.ands.vocabs.registry.db.dao.OwnerDAO;
import au.org.ands.vocabs.registry.db.entity.Owner;

/** CacheLoader helper to populate the cache of Owner entities.
 * It is <i>not</i> responsible for generating new Owner entities, if they
 * are not yet present in the database.
 */
public class OwnersCacheLoader implements CacheLoader<String, Integer> {

    /** {@inheritDoc}
     * Load an Owner entity from the database for the cache.
     * If it is not present in the database, it is <i>not</i> created.
     * @see javax.cache.integration.CacheLoader#load(java.lang.Object)
     */
    @Override
    public Integer load(final String ownerName) throws CacheLoaderException {
        Owner owner = OwnerDAO.getOwnerByOwner(ownerName);
        if (owner != null) {
            return owner.getOwnerId();
        } else {
            return null;
        }
    }

    /** {@inheritDoc}
     * @see javax.cache.integration.CacheLoader#loadAll(java.lang.Iterable)
     */
    @Override
    public Map<String, Integer> loadAll(final Iterable<? extends String> keys)
            throws CacheLoaderException {
        Map<String, Integer> map = new HashMap<>();
        for (String key : keys) {
            map.put(key, load(key));
        }
        return map;
    }

}
