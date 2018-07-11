/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.converter;

import java.util.List;

import javax.persistence.EntityManager;

import org.mapstruct.Context;

import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.dao.OwnerDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.db.internal.VocabularyJson;
import au.org.ands.vocabs.registry.subscription.Owners;
import au.org.ands.vocabs.roles.Role;
import au.org.ands.vocabs.roles.db.utils.RolesUtils;

/** MapStruct mapper from Subscription database to schema. */
public abstract class SubscriptionDbSchemaMapperDecorator
    implements SubscriptionDbSchemaMapper {

    /** The delegate mapper. */
    private final SubscriptionDbSchemaMapper delegate;

    /** Constructor that accepts a delegate.
     * @param aDelegate The delegate mapper.
     */
    public SubscriptionDbSchemaMapperDecorator(
            final SubscriptionDbSchemaMapper aDelegate) {
        delegate = aDelegate;
    }

    /** Decorator method that extends the default mapping behaviour
     * with extraction of the related data.
     * (Don't make this method final; it is extended by the implementation
     * class.)
     */
    @Override
    @SuppressWarnings("checkstyle:DesignForExtension")
    public au.org.ands.vocabs.registry.schema.vocabulary201701.Subscription
    sourceToTarget(final au.org.ands.vocabs.registry.db.entity.Subscription
            source, @Context final EntityManager em) {
        if (source == null) {
            return null;
        }
        au.org.ands.vocabs.registry.schema.vocabulary201701.Subscription
            target = delegate.sourceToTarget(source, em);
        switch (source.getNotificationElementType()) {
        case VOCABULARY:
            // elementId is the vocabulary Id.
            Integer vocabularyId = source.getNotificationElementId();
            target.setElementId(vocabularyId.toString());
            Vocabulary vocabulary = VocabularyDAO.
                    getLastNotDraftVocabularyByVocabularyId(vocabularyId, em);
            if (vocabulary == null) {
                // Could have existed only as a draft so far.
                // We don't (yet) handle that case.
                // Set deleted as a hint (e.g., to the portal) not to
                // offer a link to the view page.
                target.setTitle("Unknown vocabulary");
                target.setDeleted(true);
            } else {
                VocabularyJson vocabularyJson =
                        JSONSerialization.deserializeStringAsJson(
                                vocabulary.getData(), VocabularyJson.class);
                target.setTitle(vocabularyJson.getTitle());
                if (!TemporalUtils.isCurrent(vocabulary)) {
                    target.setDeleted(true);
                }
            }
            break;
        case OWNER:
            // elementId is the owner _name_.
            Integer ownerId = source.getNotificationElementId();
            // Special handling of "all owners" Id.
            if (ownerId.equals(Owners.ALL_OWNERS_OWNER_ID)) {
                target.setElementId(Owners.ALL_OWNERS);
                // But don't set a title in this case.
            } else {
                String ownerName = OwnerDAO.getOwnerById(em,
                        ownerId).getOwner();
                target.setElementId(ownerName);
                List<Role> ownerRoleList =
                        RolesUtils.getOrgRolesByRoleId(ownerName);
                if (ownerRoleList.size() == 0) {
                    target.setTitle("Unkknown owner name");
                } else {
                    target.setTitle(ownerRoleList.get(0).getFullName());
                }
            }
            break;
        case SYSTEM:
            // We don't set elementId or title for this element type.
            break;
        default:
            break;
        }
        return target;
    }

}
