/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.subscription;

import java.time.LocalDateTime;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.RandomStringUtils;

import au.org.ands.vocabs.registry.api.auth.AuthConstants;
import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.dao.SubscriberDAO;
import au.org.ands.vocabs.registry.db.dao.SubscriberIdDAO;
import au.org.ands.vocabs.registry.db.entity.Subscriber;
import au.org.ands.vocabs.registry.db.entity.SubscriberId;

/** Utility methods for working with Subscribers. */
public final class SubscriberUtils {

    /** Private constructor for a utility class. */
    private SubscriberUtils() {
    }

    /** Create a new Subscriber entity.
     * @param em The EntityManager to be used.
     * @param nowTime The time to use as the value of "now" when
     *      applying changes to rows of the database.
     * @return The new Subscriber entity.
     */
    public static Subscriber createSubscriber(final EntityManager em,
            final LocalDateTime nowTime) {
        // Need to make the SubscriberId ourselves, because we use
        // the Subscriber Id within the token.
        SubscriberId subscriberId = new SubscriberId();
        SubscriberIdDAO.saveSubscriberId(em, subscriberId);
        Subscriber subscriber = new Subscriber();
        subscriber.setSubscriberId(subscriberId.getId());
        TemporalUtils.makeCurrentlyValid(subscriber, nowTime);
        subscriber.setModifiedBy(AuthConstants.SYSTEM_USER);
        subscriber.setToken(createToken(subscriberId.getId()));
        SubscriberDAO.saveSubscriber(em, subscriber);
        return subscriber;
    }

    /** Create a subscriber authentication token, using the subscriberId.
     * @param subscriberId The subscriber's subscriber Id.
     * @return The generated authentication token.
     */
    public static String createToken(final Integer subscriberId) {
        return subscriberId.toString() + "," + randomString();
    }

    /** Length of random string to generate for use in tokens. */
    public static final int TOKEN_LENGTH = 20;

    /** Generate a random string. The result is a string of
     * alphanumeric characters of length {@link #TOKEN_LENGTH}.
     * @return A random string of alphanumeric characters.
     */
    private static String randomString() {
        return RandomStringUtils.randomAlphanumeric(TOKEN_LENGTH);
    }

}
