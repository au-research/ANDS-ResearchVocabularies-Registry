/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.notification.email;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import au.org.ands.vocabs.registry.db.dao.SubscriberDAO;
import au.org.ands.vocabs.registry.db.dao.SubscriptionDAO;
import au.org.ands.vocabs.registry.db.entity.Subscriber;
import au.org.ands.vocabs.registry.db.entity.Subscription;
import au.org.ands.vocabs.registry.enums.NotificationMode;
import au.org.ands.vocabs.registry.notification.CollectEvents;
import au.org.ands.vocabs.registry.notification.VocabularyDifferences;
import au.org.ands.vocabs.registry.subscription.Owners;

/** Collect the current subscriptions for which email notifications
 * are to be sent. To use this class, invoke the constructor
 * {@link #CollectSubscriptions()}, then {@link
 * #computeVocabularySubscriptionsForSubscribers(CollectEvents)},
 * then use {@link #getSubscriberSubscriptionsModels()} to get
 * the models to feed into email content generation.
 */
public class CollectSubscriptions {

    /** The subscribers with current email subscriptions.
     * Keys are subscriber Ids. */
    private Map<Integer, Subscriber> subscribersMap = new HashMap<>();

    /** Map of subscribers to the set of their current subscriptions.
     * Keys are subscriber Ids. */
    private Map<Integer, Set<Subscription>> subscriberSubscriptions =
            new HashMap<>();

    /** Map of subscribers to the unified model of their subscriptions.
     * Keys are subscriber Ids.
     * This class is reponsible for populating the fields of
     * each subscriberSubscriptionsModel.
     */
    private Map<Integer, SubscriberSubscriptionsModel>
        subscriberSubscriptionsModels = new HashMap<>();

    /** Constructor.
     * Collect all current subscriptions for email notifications. */
    public CollectSubscriptions() {
        List<Subscription> subscriptionList =
                SubscriptionDAO.getCurrentSubscriptionsForNotificationMode(
                        NotificationMode.EMAIL);
        for (Subscription subscription : subscriptionList) {
            Integer subscriberId = subscription.getSubscriberId();
            Set<Subscription> subscriptionsForSubscriber =
                    subscriberSubscriptions.get(subscriberId);
            if (subscriptionsForSubscriber == null) {
                subscriptionsForSubscriber = new HashSet<>();
                subscriberSubscriptions.put(subscriberId,
                        subscriptionsForSubscriber);
                // And now also get the Subscriber. Used later to
                // put their token into their SubscriberSubscriptionsModel.
                Subscriber subscriber = SubscriberDAO.
                        getCurrentSubscriberBySubscriberId(subscriberId);
                subscribersMap.put(subscriberId, subscriber);
            }
            subscriptionsForSubscriber.add(subscription);
        }
    }

    /** Compute the model needed for email notifications for all
     * current subscribers.
     * @param collectedEvents The collected and processed registry events
     *      for the notification period.
     */
    public void computeVocabularySubscriptionsForSubscribers(
            final CollectEvents collectedEvents) {
        Map<Integer, String> ownerFullNames =
                collectedEvents.getOwnerFullNames();
        Map<Integer, Set<Integer>> ownerVocabularies =
                collectedEvents.getOwnerVocabularies();
        Map<Integer, VocabularyDifferences> vocabularyIdMap =
                collectedEvents.getVocabularyIdMap();
        for (Integer subscriberId : subscriberSubscriptions.keySet()) {
            computeVocabularySubscriptionsForSubscriber(ownerFullNames,
                    ownerVocabularies, vocabularyIdMap, subscriberId);
        }
    }

    /** Compute the model needed for email notifications for one
     * subscriber.
     * @param ownerFullNames The map of owner Ids to their full names.
     * @param ownerVocabularies The map of owner Ids to the set of vocabulary
     *      Ids that are mentioned in registry events for the
     *      notification period.
     * @param vocabularyIdMap The map of vocabulary differences for all
     *      vocabularies mentioned in registry events for the notification
     *      period.
     * @param subscriberId The subscriber Id.
     */
    private void computeVocabularySubscriptionsForSubscriber(
            final Map<Integer, String> ownerFullNames,
            final Map<Integer, Set<Integer>> ownerVocabularies,
            final Map<Integer, VocabularyDifferences> vocabularyIdMap,
            final Integer subscriberId) {
        Set<Subscription> subscriptionsForSubscriber =
                subscriberSubscriptions.get(subscriberId);

        // At the top level, a subscriber's email report contains two sections:
        //   1. Reports on individual vocabularies.
        //   2. Reports on individual owners, each report of which contains
        //      reports on vocabularies owned by that owner.
        // So:
        // We keep track of a set of owner Ids for the owners that will be
        // included in the report:
        Set<Integer> allOwnerIdsToReport = new HashSet<>();
        // We compute a set of all vocabulary Ids that the subscriber
        // will be notified about, _based on_ the fact that they are
        // owned by an owner that the subscriber will be notified about.
        Set<Integer> allVocabulariesFromOwners = new HashSet<>();
        // And we keep track of the vocabulary Ids for which the
        // subscriber has a specific subscription:
        Set<Integer> allIndividualVocabularySubscriptions = new HashSet<>();
        // Once we get through this loop, we will remove from
        // allIndividualVocabularySubscriptions all values that are
        // also in allVocabulariesFromOwners, so that the subscriber
        // does not receive two copies of the notification for a vocabulary.

        for (Subscription subscription: subscriptionsForSubscriber) {
            switch (subscription.getNotificationElementType()) {
            case OWNER:
                Integer ownerId = subscription.getNotificationElementId();
                if (ownerId.equals(Owners.ALL_OWNERS_OWNER_ID)) {
                    // Add vocabularies from all owners.
                    for (Entry<Integer, Set<Integer>> ownerVocabulariesMapEntry
                            : ownerVocabularies.entrySet()) {
                        allOwnerIdsToReport.add(
                                ownerVocabulariesMapEntry.getKey());
                        allVocabulariesFromOwners.addAll(
                                ownerVocabulariesMapEntry.getValue());
                    }
                    // And we might as well stop now, because nothing we
                    // would find subsequently would add a vocabulary
                    // to report.
                    break;
                } else {
                    Set<Integer> vocabulariesForOwner =
                            ownerVocabularies.get(ownerId);
                    if (vocabulariesForOwner != null) {
                        allOwnerIdsToReport.add(ownerId);
                        allVocabulariesFromOwners.addAll(
                                ownerVocabularies.get(ownerId));
                    }
                }
                break;
            case VOCABULARY:
                Integer vocabularyId = subscription.getNotificationElementId();
                if (vocabularyIdMap.containsKey(vocabularyId)) {
                    // Only notify, if there is something to notify.
                    allIndividualVocabularySubscriptions.add(vocabularyId);
                }
                break;
            case SYSTEM:
                // Not yet supported.
                break;
            default:
                // "Can't happen."
                break;
            }
        }
        // Compute set difference: this is how we implement the case
        // where a subscriber is subscribed to both a vocabulary by its
        // Id, and also the vocabulary's owner. The vocabulary will be
        // reported against its _owner_, not as an individual vocabulary.
        allIndividualVocabularySubscriptions.removeAll(
                allVocabulariesFromOwners);

        SubscriberSubscriptionsModel subscriberSubscriptionsModel =
                new SubscriberSubscriptionsModel();
        subscriberSubscriptionsModel.setToken(subscribersMap.get(
                subscriberId).getToken());
        subscriberSubscriptionsModel.setOwnerFullNames(ownerFullNames);
        subscriberSubscriptionsModel.setOwnerVocabularies(ownerVocabularies);
        subscriberSubscriptionsModel.setVocabularyIdMap(vocabularyIdMap);
        subscriberSubscriptionsModel.setAllOwnerIdsToReport(
                allOwnerIdsToReport);
        subscriberSubscriptionsModel.setAllIndividualVocabularySubscriptions(
                allIndividualVocabularySubscriptions);
        subscriberSubscriptionsModels.put(subscriberId,
                subscriberSubscriptionsModel);
    }

    /** Get the map of Subscriber instances. Keys are subscriber ids.
     * @return The map of Subscriber instances.
     */
    public Map<Integer, Subscriber> getSubscribersMap() {
        return subscribersMap;
    }

    /** Get the map of SubscriberSubscriptionsModel instances.
     * Keys are subscriber Ids.
     * @return The map of SubscriberSubscriptionsModel instances.
     */
    public Map<Integer, SubscriberSubscriptionsModel>
    getSubscriberSubscriptionsModels() {
        return subscriberSubscriptionsModels;
    }

}
