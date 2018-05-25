/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.subscription;

import java.time.LocalDateTime;

import javax.persistence.EntityManager;

import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.dao.SubscriberEmailAddressDAO;
import au.org.ands.vocabs.registry.db.entity.Subscriber;
import au.org.ands.vocabs.registry.db.entity.SubscriberEmailAddress;

/** Utility methods for working with Subscriber Email Addresses. */
public final class SubscriberEmailAddressUtils {

    /** Private constructor for a utility class. */
    private SubscriberEmailAddressUtils() {
    }

    /** Given a subscriber email address, require that there be a
     * currently-valid Subscriber that has that email address,
     * creating a new Subscriber entity if necessary.
     * @param email The email address of the subscriber.
     * @param em The EntityManager to use to create a new Subscriber entity,
     *      if there is not currently one.
     * @param nowTime The time to use as the value of "now" when
     *      applying changes to rows of the database.
     * @return The Subscriber Id of the Subscriber entity.
     */
    public static Integer requireSubscriberIdByEmailAddress(
            final String email, final EntityManager em,
            final LocalDateTime nowTime) {
        SubscriberEmailAddress sea = SubscriberEmailAddressDAO.
                getCurrentSubscriberEmailAddressByEmailAddress(email);
        if (sea != null) {
            return sea.getSubscriberId();
        }
        Subscriber subscriber = SubscriberUtils.createSubscriber(em, nowTime);
        sea = new SubscriberEmailAddress();
        sea.setSubscriberId(subscriber.getSubscriberId());
        TemporalUtils.makeCurrentlyValid(sea, nowTime);
        sea.setEmailAddress(email);
        SubscriberEmailAddressDAO.saveSubscriberEmailAddressWithId(em, sea);
        return subscriber.getSubscriberId();
    }
}
