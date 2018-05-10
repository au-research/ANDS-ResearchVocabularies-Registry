/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.log;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.dao.RegistryEventDAO;
import au.org.ands.vocabs.registry.db.entity.AccessPoint;
import au.org.ands.vocabs.registry.db.entity.RegistryEvent;
import au.org.ands.vocabs.registry.db.entity.RelatedEntity;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.enums.RegistryEventElementType;
import au.org.ands.vocabs.registry.enums.RegistryEventEventType;

/** Utility methods for working with Registry Events. */
public final class RegistryEventUtils {

    /** Private constructor for a utility class. */
    private RegistryEventUtils() {
    }

    /** Create and persist a Registry Event.
     * (This method has too many parameters.)
     * @param em The EntityManager to use to persist the new Registry Event
     *      entity.
     * @param elementType The element type of the event.
     * @param elementId The element ID of the event.
     * @param eventDate The timestamp to use for the event.
     * @param eventType The type of event.
     * @param eventUser The user to record as being responsible for the
     *      creation of the event.
     * @param oldElement For events which are an update or a deletion
     *      of an element, the old instance of the element. Provide null
     *      for other event instances.
     * @param newElement For events which are an update or an insertion
     *      of an element, the new instance of the element. Provide null
     *      for other event instances.
     * @param parameters Additional details of the event. If provided,
     *      this must be a sequence of pairs of (String) keys and
     *      (Object) values to be inserted into the event details field.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public static void createRegistryEvent(
            final EntityManager em,
            final RegistryEventElementType elementType,
            final Integer elementId,
            final LocalDateTime eventDate,
            final RegistryEventEventType eventType,
            final String eventUser,
            final Object oldElement,
            final Object newElement,
            final Object... parameters
            ) {
        RegistryEvent re = new RegistryEvent();
        re.setElementType(elementType);
        re.setElementId(elementId);
        re.setEventDate(eventDate);
        re.setEventType(eventType);
        re.setEventUser(eventUser);
        // To be done: put something sensible in the details.
        Map<String, Object> eventDetails = new HashMap<>();
        switch (eventType) {
        case CREATED:
            addDetailsForCreatedEvent(elementType, newElement, eventDetails);
            break;
        case UPDATED:
            addDetailsForUpdatedEvent(elementType, oldElement, newElement,
                    eventDetails);
            break;
        case DELETED:
            addDetailsForDeletedEvent(elementType, oldElement, eventDetails);
            break;
        default:
        }
        if (parameters != null) {
            int parameterIndex = 0;
            while (parameterIndex < parameters.length) {
                eventDetails.put((String) parameters[parameterIndex],
                        parameters[parameterIndex + 1]);
                parameterIndex += 2;
            }
        }
        re.setEventDetails(JSONSerialization.serializeObjectAsJsonString(
                eventDetails));
        RegistryEventDAO.saveRegistryEvent(em, re);
    }

    /** Add additional details pertaining to a creation event.
     * @param elementType The element type of the event.
     * @param newElement The new instance of the element. Must not be null.
     * @param eventDetails The event details map to be added to.
     */
    private static void addDetailsForCreatedEvent(
            final RegistryEventElementType elementType,
            final Object newElement,
            final Map<String, Object> eventDetails) {
        switch (elementType) {
        case VOCABULARIES:
            Vocabulary vocabulary = (Vocabulary) newElement;
            eventDetails.put(RegistryEventDetailKeys.STATUS,
                    vocabulary.getStatus());
            eventDetails.put(RegistryEventDetailKeys.ID, vocabulary.getId());
            break;
        case VERSIONS:
            Version version = (Version) newElement;
            eventDetails.put(RegistryEventDetailKeys.STATUS,
                    version.getStatus());
            eventDetails.put(RegistryEventDetailKeys.DRAFT,
                    TemporalUtils.isDraft(version));
            eventDetails.put(RegistryEventDetailKeys.ID, version.getId());
            break;
        case ACCESS_POINTS:
            AccessPoint ap = (AccessPoint) newElement;
            eventDetails.put(RegistryEventDetailKeys.DRAFT,
                    TemporalUtils.isDraft(ap));
            eventDetails.put(RegistryEventDetailKeys.ID, ap.getId());
            break;
        case RELATED_ENTITIES:
            RelatedEntity re = (RelatedEntity) newElement;
            eventDetails.put(RegistryEventDetailKeys.ID, re.getId());
            break;
        default:
            break;
        }
    }

    /** Add additional details pertaining to an update event.
     * @param elementType The element type of the event.
     * @param oldElement The old instance of the element. Must not be null.
     * @param newElement The new instance of the element. Must not be null.
     * @param eventDetails The event details map to be added to.
     */
    private static void addDetailsForUpdatedEvent(
            final RegistryEventElementType elementType,
            final Object oldElement,
            final Object newElement,
            final Map<String, Object> eventDetails) {
        switch (elementType) {
        case VOCABULARIES:
            Vocabulary oldVocabulary = (Vocabulary) oldElement;
            Vocabulary newVocabulary = (Vocabulary) newElement;
            eventDetails.put(RegistryEventDetailKeys.STATUS,
                    newVocabulary.getStatus());
            eventDetails.put(RegistryEventDetailKeys.ID, newVocabulary.getId());
            if (!TemporalUtils.isDraft(oldVocabulary)) {
                eventDetails.put(RegistryEventDetailKeys.PREVIOUS_ID,
                        oldVocabulary.getId());
            }
            break;
        case VERSIONS:
            Version oldVersion = (Version) oldElement;
            Version newVersion = (Version) newElement;
            eventDetails.put(RegistryEventDetailKeys.STATUS,
                    newVersion.getStatus());
            eventDetails.put(RegistryEventDetailKeys.DRAFT,
                    TemporalUtils.isDraft(newVersion));
            eventDetails.put(RegistryEventDetailKeys.ID, newVersion.getId());
            if (!TemporalUtils.isDraft(oldVersion)) {
                eventDetails.put(RegistryEventDetailKeys.PREVIOUS_ID,
                        oldVersion.getId());
            }
            break;
        case ACCESS_POINTS:
            // Not supported for now.
//            AccessPoint oldAp = (AccessPoint) oldElement;
//            AccessPoint newAp = (AccessPoint) newElement;
//            eventDetails.put(RegistryEventDetailKeys.ID, ap.getId());
            break;
        case RELATED_ENTITIES:
            RelatedEntity oldRe = (RelatedEntity) oldElement;
            RelatedEntity newRe = (RelatedEntity) newElement;
            eventDetails.put(RegistryEventDetailKeys.ID, newRe.getId());
            eventDetails.put(RegistryEventDetailKeys.PREVIOUS_ID,
                    oldRe.getId());
            break;
        default:
            break;
        }
    }

    /** Add additional details pertaining to a deleted event.
     * @param elementType The element type of the event.
     * @param oldElement The old instance of the element. Must not be null.
     * @param eventDetails The event details map to be added to.
     */
    private static void addDetailsForDeletedEvent(
            final RegistryEventElementType elementType,
            final Object oldElement,
            final Map<String, Object> eventDetails) {
        switch (elementType) {
        case VOCABULARIES:
            Vocabulary vocabulary = (Vocabulary) oldElement;
            eventDetails.put(RegistryEventDetailKeys.STATUS,
                    vocabulary.getStatus());
            if (!TemporalUtils.isDraft(vocabulary)) {
                eventDetails.put(RegistryEventDetailKeys.ID,
                        vocabulary.getId());
            }
            break;
        case VERSIONS:
            Version version = (Version) oldElement;
            eventDetails.put(RegistryEventDetailKeys.STATUS,
                    version.getStatus());
            boolean isDraft = TemporalUtils.isDraft(version);
            eventDetails.put(RegistryEventDetailKeys.DRAFT, isDraft);
            if (!isDraft) {
                eventDetails.put(RegistryEventDetailKeys.ID, version.getId());
            }
            break;
        case ACCESS_POINTS:
            AccessPoint ap = (AccessPoint) oldElement;
            isDraft = TemporalUtils.isDraft(ap);
            eventDetails.put(RegistryEventDetailKeys.DRAFT,
                    isDraft);
            if (!isDraft) {
                eventDetails.put(RegistryEventDetailKeys.ID, ap.getId());
            }
            break;
        case RELATED_ENTITIES:
            RelatedEntity re = (RelatedEntity) oldElement;
            eventDetails.put(RegistryEventDetailKeys.ID, re.getId());
            break;
        default:
            break;
        }
    }

}
