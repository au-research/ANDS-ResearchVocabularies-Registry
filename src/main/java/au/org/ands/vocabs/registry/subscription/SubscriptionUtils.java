/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.subscription;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.EntityManager;

import au.org.ands.vocabs.registry.db.context.TemporalConstants;
import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.dao.SubscriptionDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.db.entity.Subscription;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.enums.NotificationElementType;
import au.org.ands.vocabs.registry.enums.NotificationMode;

/** Methods that manage subscriptions: addition, deletion, etc. */
public final class SubscriptionUtils {

    /** Private constructor for a utility class. */
    private SubscriptionUtils() {
    }

    /** Create an email subscription for a subscriber email, for a vocabulary.
     * There must be a current instance of the vocabulary with the specified
     * vocabulary Id; otherwise, an IllegalArgumentException is thrown.
     * @param email The subscriber email address.
     * @param vocabularyId The vocabulary Id.
     * @param em The EntityManager to use.
     * @param nowTime The time to use as the value of "now" when
     *      applying changes to rows of the database.
     * @param modifiedBy The value to use for "modifiedBy" when adding
     *      or updating rows of the database.
     */
    public static void createEmailSubscriptionVocabulary(final String email,
            final Integer vocabularyId, final EntityManager em,
            final LocalDateTime nowTime, final String modifiedBy) {
        Vocabulary vocabulary = VocabularyDAO.
                getCurrentVocabularyByVocabularyId(em, vocabularyId);
        if (vocabulary == null) {
            throw new IllegalArgumentException(
                    "No current vocabulary instance with that vocabulary Id");
        }

        Integer subscriberId = SubscriberEmailAddressUtils.
                requireSubscriberIdByEmailAddress(email, em, nowTime);

        // Get any existing such subscription; we won't add another
        // database row if the subscriber is already subscribed to
        // this vocabulary.
        List<Subscription> subscriptionList = SubscriptionDAO.
                getCurrentSubscriptionsForSubscriberAndNotification(
                        subscriberId, NotificationMode.EMAIL,
                        NotificationElementType.VOCABULARY, vocabularyId,
                        em);

        if (subscriptionList.isEmpty()) {
            Subscription subscription = new Subscription();
            TemporalUtils.makeCurrentlyValid(subscription, nowTime);
            subscription.setSubscriberId(subscriberId);
            subscription.setModifiedBy(modifiedBy);
            subscription.setNotificationMode(NotificationMode.EMAIL);
            subscription.setNotificationElementType(
                    NotificationElementType.VOCABULARY);
            subscription.setNotificationElementId(vocabularyId);
            subscription.setLastNotification(TemporalConstants.NEVER_NOTIFIED);
            // For now, data is an empty array.
            subscription.setData("{}");
            SubscriptionDAO.saveSubscriptionWithId(em, subscription);
        }
    }

    /** Create an email subscription for a subscriber email, for an owner.
     * @param email The subscriber email address.
     * @param owner The owner.
     * @param em The EntityManager to use.
     * @param nowTime The time to use as the value of "now" when
     *      applying changes to rows of the database.
     * @param modifiedBy The value to use for "modifiedBy" when adding
     *      or updating rows of the database.
     */
    public static void createEmailSubscriptionOwner(final String email,
            final String owner, final EntityManager em,
            final LocalDateTime nowTime, final String modifiedBy) {
        Integer ownerId = Owners.getOwnerId(owner);
        if (ownerId == null) {
            throw new IllegalArgumentException("No such owner");
        }

        Integer subscriberId = SubscriberEmailAddressUtils.
                requireSubscriberIdByEmailAddress(email, em, nowTime);

        // Get any existing such subscription; we won't add another
        // database row if the subscriber is already subscribed to
        // this vocabulary.
        List<Subscription> subscriptionList = SubscriptionDAO.
                getCurrentSubscriptionsForSubscriberAndNotification(
                        subscriberId, NotificationMode.EMAIL,
                        NotificationElementType.OWNER,
                        ownerId, em);

        if (subscriptionList.isEmpty()) {
            Subscription subscription = new Subscription();
            TemporalUtils.makeCurrentlyValid(subscription, nowTime);
            subscription.setSubscriberId(subscriberId);
            subscription.setModifiedBy(modifiedBy);
            subscription.setNotificationMode(NotificationMode.EMAIL);
            subscription.setNotificationElementType(
                    NotificationElementType.OWNER);
            subscription.setNotificationElementId(ownerId);
            subscription.setLastNotification(TemporalConstants.NEVER_NOTIFIED);
            // For now, data is an empty array.
            subscription.setData("{}");
            SubscriptionDAO.saveSubscriptionWithId(em, subscription);
        }
    }

    /** Create an email subscription for a subscriber email, for the system.
     * @param email The subscriber email address.
     * @param em The EntityManager to use.
     * @param nowTime The time to use as the value of "now" when
     *      applying changes to rows of the database.
     * @param modifiedBy The value to use for "modifiedBy" when adding
     *      or updating rows of the database.
     */
    public static void createEmailSubscriptionSystem(final String email,
            final EntityManager em, final LocalDateTime nowTime,
            final String modifiedBy) {
        Integer subscriberId = SubscriberEmailAddressUtils.
                requireSubscriberIdByEmailAddress(email, em, nowTime);

        // Get any existing such subscription; we won't add another
        // database row if the subscriber is already subscribed to
        // this vocabulary.
        List<Subscription> subscriptionList = SubscriptionDAO.
                getCurrentSubscriptionsForSubscriberAndNotification(
                        subscriberId, NotificationMode.EMAIL,
                        NotificationElementType.SYSTEM, 0,
                        em);

        if (subscriptionList.isEmpty()) {
            Subscription subscription = new Subscription();
            TemporalUtils.makeCurrentlyValid(subscription, nowTime);
            subscription.setSubscriberId(subscriberId);
            subscription.setModifiedBy(modifiedBy);
            subscription.setNotificationMode(NotificationMode.EMAIL);
            subscription.setNotificationElementType(
                    NotificationElementType.SYSTEM);
            subscription.setNotificationElementId(0);
            subscription.setLastNotification(TemporalConstants.NEVER_NOTIFIED);
            // For now, data is an empty array.
            subscription.setData("{}");
            SubscriptionDAO.saveSubscriptionWithId(em, subscription);
        }
    }

    /** Delete all email subscriptions for a subscriber, for a vocabulary.
     * @param subscriberId The subscriber Id.
     * @param vocabularyId The vocabulary Id.
     * @param em The EntityManager to use.
     * @param nowTime The time to use as the value of "now" when
     *      applying changes to rows of the database.
     * @param modifiedBy The value to use for "modifiedBy" when adding
     *      or updating rows of the database.
     */
    public static void deleteEmailSubscriptionVocabulary(
            final Integer subscriberId,
            final Integer vocabularyId, final EntityManager em,
            final LocalDateTime nowTime, final String modifiedBy) {
        List<Subscription> subscriptionList = SubscriptionDAO.
                getCurrentSubscriptionsForSubscriberAndNotification(
                        subscriberId, NotificationMode.EMAIL,
                        NotificationElementType.VOCABULARY, vocabularyId,
                        em);

        for (Subscription subscription : subscriptionList) {
            TemporalUtils.makeHistorical(subscription, nowTime);
            subscription.setModifiedBy(modifiedBy);
            SubscriptionDAO.updateSubscription(em, subscription);
        }
    }

    /** Delete all email subscriptions for a subscriber, for an owner.
     * @param subscriberId The subscriber Id.
     * @param owner The owner.
     * @param em The EntityManager to use.
     * @param nowTime The time to use as the value of "now" when
     *      applying changes to rows of the database.
     * @param modifiedBy The value to use for "modifiedBy" when adding
     *      or updating rows of the database.
     */
    public static void deleteEmailSubscriptionOwner(
            final Integer subscriberId,
            final String owner, final EntityManager em,
            final LocalDateTime nowTime, final String modifiedBy) {
        Integer ownerId = Owners.getOwnerId(owner);
        if (ownerId == null) {
            throw new IllegalArgumentException("No such owner");
        }
        List<Subscription> subscriptionList = SubscriptionDAO.
                getCurrentSubscriptionsForSubscriberAndNotification(
                        subscriberId, NotificationMode.EMAIL,
                        NotificationElementType.OWNER,
                        ownerId, em);

        for (Subscription subscription : subscriptionList) {
            TemporalUtils.makeHistorical(subscription, nowTime);
            subscription.setModifiedBy(modifiedBy);
            SubscriptionDAO.updateSubscription(em, subscription);
        }
    }

    /** Delete all email subscriptions for a subscriber, for the system.
     * @param subscriberId The subscriber Id.
     * @param em The EntityManager to use.
     * @param nowTime The time to use as the value of "now" when
     *      applying changes to rows of the database.
     * @param modifiedBy The value to use for "modifiedBy" when adding
     *      or updating rows of the database.
     */
    public static void deleteEmailSubscriptionSystem(
            final Integer subscriberId,
            final EntityManager em, final LocalDateTime nowTime,
            final String modifiedBy) {
        List<Subscription> subscriptionList = SubscriptionDAO.
                getCurrentSubscriptionsForSubscriberAndNotification(
                        subscriberId, NotificationMode.EMAIL,
                        NotificationElementType.SYSTEM, 0,
                        em);

        for (Subscription subscription : subscriptionList) {
            TemporalUtils.makeHistorical(subscription, nowTime);
            subscription.setModifiedBy(modifiedBy);
            SubscriptionDAO.updateSubscription(em, subscription);
        }
    }

    /** Delete all email subscriptions for a subscriber.
     * @param subscriberId The subscriber Id.
     * @param em The EntityManager to use.
     * @param nowTime The time to use as the value of "now" when
     *      applying changes to rows of the database.
     * @param modifiedBy The value to use for "modifiedBy" when adding
     *      or updating rows of the database.
     */
    public static void deleteEmailSubscriptionAll(final Integer subscriberId,
            final EntityManager em, final LocalDateTime nowTime,
            final String modifiedBy) {
        List<Subscription> subscriptionList =  SubscriptionDAO.
                getCurrentSubscriptionListForSubscriber(em, subscriberId);

        for (Subscription subscription : subscriptionList) {
            TemporalUtils.makeHistorical(subscription, nowTime);
            subscription.setModifiedBy(modifiedBy);
            SubscriptionDAO.updateSubscription(em, subscription);
        }
    }

}
