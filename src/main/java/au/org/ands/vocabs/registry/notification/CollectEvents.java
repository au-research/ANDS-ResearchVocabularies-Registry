/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.notification;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.context.DBContext;
import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.dao.RegistryEventDAO;
import au.org.ands.vocabs.registry.db.entity.RegistryEvent;
import au.org.ands.vocabs.registry.enums.RegistryEventElementType;
import au.org.ands.vocabs.registry.enums.RegistryEventEventType;
import au.org.ands.vocabs.registry.model.fixedtime.TModelMethods;
import au.org.ands.vocabs.registry.model.fixedtime.TVocabularyModel;
import au.org.ands.vocabs.registry.subscription.Owners;
import au.org.ands.vocabs.roles.Role;
import au.org.ands.vocabs.roles.db.utils.RolesUtils;

/** Collect the Registry events to be put into notifications. */
public class CollectEvents {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** The start date of Registry events to be considered for reporting. */
    private LocalDateTime fromDate;

    /** The end date of Registry events to be considered for reporting. */
    private LocalDateTime toDate;

    /** Extract the Registry events for a specified time period.
     * The period is specified as a closed-open interval.
     * @param aFromDate The start date of Registry events to get from
     *      the database.
     * @param aToDate The end date of Registry events to get from
     *      the database.
     */
    public CollectEvents(final LocalDateTime aFromDate,
            final LocalDateTime aToDate) {
        fromDate = aFromDate;
        toDate = aToDate;
        EntityManager em = DBContext.getEntityManager();
        List<RegistryEvent> reList = RegistryEventDAO.
                getRegistryEventsFromToDate(em, fromDate, toDate);
        collectEvents(em, reList);
        em.close();
    }

    /** Extract the Registry events for a specified time period.
     * The period is specified as a start date; all events from that
     * date up to the present moment are extracted.
     * @param aFromDate The start date of Registry events to get from
     *      the database.
     */
    public CollectEvents(final LocalDateTime aFromDate) {
        this(aFromDate, TemporalUtils.nowUTC());
    }

    /** Set of vocabulary Ids that we have seen while working through
     * the Registry Events. Used to help avoid duplicating work in
     * the case that there is nothing to report for a vocabulary
     * because it existed neither at the beginning nor the end
     * of the reporting period, yet where there are multiple
     * registry events for the vocabulary <i>during</i> the period.*/
    private Set<Integer> vocabularyIdsSeen = new HashSet<>();

    /** Map of owner Ids to sets of vocabulary Ids. */
    private Map<Integer, Set<Integer>> ownerVocabularies = new HashMap<>();

    /** Map of owner Ids to their short names. */
    private Map<Integer, String> ownerNames = new HashMap<>();

    /** Map of owner Ids to their full names. */
    private Map<Integer, String> ownerFullNames = new HashMap<>();

    /** Map of vocabulary Ids to the representation of the Registry
     * events recorded for that vocabulary. */
    private Map<Integer, VocabularyDifferences> vocabularyIdMap =
            new HashMap<>();

    /** Get the map of owner Ids to sets of vocabulary Ids that are
     * referenced in registry events of the notification period.
     * @return The map of owner Ids to set of vocabulary Ids.
     */
    public Map<Integer, Set<Integer>> getOwnerVocabularies() {
        return ownerVocabularies;
    }

    /** Get the map of vocabulary Ids to the representation of the
     * changes made to the vocabulary during the notification period.
     * @return The map of vocabulary Ids to the corresponding
     *      VocabularyDifferences instances.
     */
    public Map<Integer, VocabularyDifferences> getVocabularyIdMap() {
        return vocabularyIdMap;
    }

    /** Get the map of owner Ids to their full names.
     * @return The map of owener Ids to full names.
     */
    public Map<Integer, String> getOwnerFullNames() {
        return ownerFullNames;
    }

    /** Collect and organize the Registry Events fetched from the database.
     * @param em The EntityManager to be used to fetch the details of
     *      Registry events from the database.
     * @param reList The list of Registry events to be processed.
     */
    private void collectEvents(final EntityManager em,
            final List<RegistryEvent> reList) {
        // Just work through the elements of the list, one by one,
        // sorting them into their various categories.
        for (RegistryEvent re : reList) {
            switch (re.getElementType()) {
            case VOCABULARIES:
                collectVocabularyEvent(em, re);
                break;
            // (For now) we don't process events for versions and access points
            // separately from their containing vocabularies. (We rely on
            // the fact that a version or access point event will always be
            // accompanied for an event for the vocabulary.)
            case VERSIONS:
            case ACCESS_POINTS:
                break;
            case RELATED_ENTITIES:
                // For now, we don't do any notifications for Registry events
                // for related entities.
                //    processRelatedEntityEvent(em, re);
                break;
            default:
                logger.error("Found Registry event with unknown "
                        + "element type: id = " + re.getId());
                break;
            }
        }
    }

    /** Process one Registry event of element type
     * {@link RegistryEventElementType#VOCABULARIES}.
     * @param em The EntityManager to be used to fetch the details of
     *      Registry events from the database.
     * @param re The RegistryEvent to be processed.
     */
    private void collectVocabularyEvent(final EntityManager em,
            final RegistryEvent re) {
        Integer vocabularyId = re.getElementId();
        if (vocabularyIdsSeen.contains(vocabularyId)) {
            // Already processed, and the result can't change.
            return;
        }
        // Record that we've now "seen" this vocabulary Id ...
        vocabularyIdsSeen.add(vocabularyId);
        // .. and now we will find out if there should also be
        // a VocabularyDifferences for this vocabulary.
        // There _won't_ be, if the vocabulary didn't exist either
        // at fromDate or endDate.

        if (!vocabularyIdMap.containsKey(vocabularyId)) {
            VocabularyDifferences vdiff = new VocabularyDifferences();
            vocabularyIdMap.put(vocabularyId, vdiff);
            TVocabularyModel vstart = TModelMethods.createTVocabularyModel(em,
                    vocabularyId, fromDate);
            TVocabularyModel vend = TModelMethods.createTVocabularyModel(em,
                    vocabularyId, toDate);
            if (vend.isEmpty()) {
                if (vstart.isEmpty()) {
                    // Vocabulary didn't exist either at fromDate or endDate,
                    // so we don't report anything for this vocabulary.
                    // NB: we _don't_ call recordVocabularyForOwner()
                    // in this case, and we prune vdiff from vocabularyIdMap.
                    vocabularyIdMap.remove(vocabularyId);
                    return;
                }
                // The vocabulary was deleted.
                vdiff.setTitle(vstart.getVocabularyTitle());
                vdiff.setFinalResult(RegistryEventEventType.DELETED);
                recordVocabularyForOwner(vstart.getVocabularyOwner(),
                        vocabularyId);
                // We call diff() to get the vocabulary difference,
                // i.e., "The vocabulary was deleted.".
                TModelMethods.diff(vstart, vend, vdiff);
                return;
            }
            // vend is non-empty. Could still be either CREATED or UPDATED.
            if (vstart.isEmpty()) {
                // The vocabulary was created.
                vdiff.setTitle(vend.getVocabularyTitle());
                vdiff.setFinalResult(RegistryEventEventType.CREATED);
                recordVocabularyForOwner(vend.getVocabularyOwner(),
                        vocabularyId);
                // We call diff() to get the vocabulary difference,
                // i.e., "The vocabulary was created.". (In future,
                // we may also report on versions.)
                TModelMethods.diff(vstart, vend, vdiff);
                return;
            }
            // The vocabulary was updated ... maybe.
            // We compare, but we only keep the result if it is non-empty.
            vdiff.setTitle(vstart.getVocabularyTitle());
            vdiff.setFinalResult(RegistryEventEventType.UPDATED);
            TModelMethods.diff(vstart, vend, vdiff);
            if (vdiff.isEmpty()) {
                // Prune this vocabulary, because we found no differences.
                // This will be the case if the owner edited and
                // republished without changing anything.
                vocabularyIdMap.remove(vocabularyId);
            } else {
                recordVocabularyForOwner(vstart.getVocabularyOwner(),
                        vocabularyId);
            }
        }
    }

    /** Record the fact that a vocabulary is mentioned in a Registry event,
     * against the vocabulary's owner.
     * @param owner The owner of the vocabulary.
     * @param vocabularyId The vocabulary Id of the vocabulary mentioned
     *      in the Registry event.
     */
    private void recordVocabularyForOwner(final String owner,
            final Integer vocabularyId) {
        Integer ownerId = Owners.getOwnerId(owner);
        Set<Integer> vocabularySet = ownerVocabularies.get(ownerId);
        if (vocabularySet == null) {
            vocabularySet = new HashSet<>();
            ownerVocabularies.put(ownerId, vocabularySet);
            ownerNames.put(ownerId, owner);
            // Now fetch the details from the roles database.
            List<Role> roles = RolesUtils.getOrgRolesByRoleId(owner);
            if (roles.size() == 1) {
                // The full name is stored in the "name" column of the table.
                ownerFullNames.put(ownerId, roles.get(0).getFullName());
            } else {
                // Oops! Fall back to the short name.
                logger.warn("Didn't find role in the roles database: "
                        + "role Id = " + owner);
                ownerFullNames.put(ownerId, owner);
            }
        }
        vocabularySet.add(vocabularyId);
    }

}
