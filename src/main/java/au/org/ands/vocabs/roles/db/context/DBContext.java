/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.roles.db.context;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import au.org.ands.vocabs.roles.utils.RolesProperties;

/** Provide access to the roles database. */
public final class DBContext {

    /** The persistence unit name as specified in persistence.xml. */
    public static final String UNIT_NAME = "ANDS-Vocabs-Roles";

    /** Access to persistence context. */
    private static EntityManagerFactory entityManagerFactory =
            Persistence.createEntityManagerFactory(UNIT_NAME,
                    RolesProperties.getProperties());

    /** Private constructor for a utility class. */
    private DBContext() {
    }

    /** Return an entity manager.
     * @return an entity manager
     */
    public static EntityManager getEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    /** Prepare for shutdown. Call this only in webapp context shutdown!
     */
    public static void doShutdown() {
        entityManagerFactory.close();
        entityManagerFactory = null;
    }


}
