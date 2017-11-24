/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.model;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.collections4.sequence.CommandVisitor;
import org.apache.commons.collections4.sequence.SequencesComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.dao.VocabularyRelatedEntityDAO;
import au.org.ands.vocabs.registry.db.entity.VocabularyRelatedEntity;
import au.org.ands.vocabs.registry.enums.RelatedEntityRelation;
import au.org.ands.vocabs.registry.enums.VocabularyStatus;
import au.org.ands.vocabs.registry.model.sequence.VocabularyRelatedEntityElement;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;

/** Domain model of a vocabulary's vocabulary-related-entities.
 */
public class VocabularyRelatedEntitiesModel extends ModelBase {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Map of the current vocabulary-related-entities and their relations.
     * The keys are Related Entity Ids. */
    private MultivaluedMap<Integer, VocabularyRelatedEntity>
        currentREsAndRelations =
            new MultivaluedHashMap<>();

    /** Map of the draft vocabulary-related-entities and their relations.
     * The keys are Related Entity Ids. */
    private MultivaluedMap<Integer, VocabularyRelatedEntity>
        draftREsAndRelations =
            new MultivaluedHashMap<>();

    /** Construct vocabulary-related-entities model for a vocabulary.
     * @param anEm The EntityManager to be used to fetch and update
     *      database data.
     * @param aVocabularyId The Id of the vocabulary for which the model
     *      is to be constructed.
     * @throws IllegalArgumentException If aVocabularyId is null.
     */
    public VocabularyRelatedEntitiesModel(final EntityManager anEm,
            final Integer aVocabularyId) {
        if (aVocabularyId == null) {
            logger.error("Attempt to construct vocabulary-related-entities "
                    + "model with no Id");
            throw new IllegalArgumentException(
                    "Attempt to construct vocabulary-related-entities model "
                    + "with no Id");
        }
        setEm(anEm);
        setVocabularyId(aVocabularyId);
        populateModel();
    }

    /** {@inheritDoc} */
    @Override
    protected void populateModel() {
        // Current
        currentREsAndRelations =
                VocabularyRelatedEntityDAO.
                getCurrentVocabularyRelatedEntitiesForVocabulary(em(),
                        vocabularyId());

        // Draft
        draftREsAndRelations =
                VocabularyRelatedEntityDAO.
                getDraftVocabularyRelatedEntitiesForVocabulary(em(),
                        vocabularyId());
    }

    /** {@inheritDoc} */
    @Override
    public List<String> describeModel() {
        List<String> description = new ArrayList<>();
        if (vocabularyId() == null) {
            description.add("VRE | No vocabulary Id");
            return description;
        }
        description.add("VRE | Vocabulary; Id: " + vocabularyId());
        if (currentREsAndRelations != null) {
            for (Integer reId : currentREsAndRelations.keySet()) {
                for (VocabularyRelatedEntity vre
                        : currentREsAndRelations.get(reId)) {
                    description.add("VRE | Current vocabulary has RE; "
                            + "RE Id, relation: " + reId + ","
                            + vre.getRelation());
                }
            }
        }
        if (draftREsAndRelations != null) {
            for (Integer reId : draftREsAndRelations.keySet()) {
                for (VocabularyRelatedEntity vre
                        : draftREsAndRelations.get(reId)) {
                    description.add("VRE | Draft vocabulary has RE; "
                            + "RE Id, relation, meaning: " + reId + ","
                            + vre.getRelation() + ","
                            + TemporalUtils.getTemporalDescription(
                                    vre).getValue());
                }
            }
        }
        return description;
    }

    /** Log the textual description of the model of the vocabulary. */
    public void logModelDescription() {
        List<String> description = describeModel();
        for (String line : description) {
            logger.info(line);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void makeCurrentHistorical(final boolean preserveAsDraft) {
        for (Integer reId : currentREsAndRelations.keySet()) {
            for (VocabularyRelatedEntity vre
                    : currentREsAndRelations.get(reId)) {
                TemporalUtils.makeHistorical(vre, nowTime());
                vre.setModifiedBy(modifiedBy());
                VocabularyRelatedEntityDAO.updateVocabularyRelatedEntity(
                        em(), vre);
                if (preserveAsDraft) {
                    RelatedEntityRelation reRelation = vre.getRelation();
                    VocabularyRelatedEntity draftVre =
                            getDraftDatabaseRow(reId, reRelation);
                    if (draftVre == null) {
                        // No existing draft row; add one.
                        VocabularyRelatedEntity newVre =
                                new VocabularyRelatedEntity();
                        newVre.setVocabularyId(vocabularyId());
                        newVre.setRelatedEntityId(reId);
                        newVre.setRelation(reRelation);
                        newVre.setModifiedBy(modifiedBy());
                        TemporalUtils.makeDraftAdditionOrModification(newVre);
                        VocabularyRelatedEntityDAO.saveVocabularyRelatedEntity(
                                em(), newVre);
                    } else {
                        // An existing draft row.
                        // This row must be a draft deletion.
                        // NB: this is the case because for VREs, there is no
                        // notion of "modification/update", but only add
                        // and delete.
                        // If there were, we would need to wrap this line
                        // in the following conditional:
                        // if (TemporalUtils.isDraftDeletion(draftVre)) {
                        // The existing draft specified deletion; now there
                        // is no currently-valid row either, so delete
                        // this deletion!
                        deleteDraftDeletionDatabaseRow(reId, reRelation);
                        // }
                    }
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void deleteOnlyDraft() {
        deleteDraftDatabaseRows();
    }

    /** {@inheritDoc} */
    @Override
    protected void deleteDraftDatabaseRows() {
        for (Integer reId : draftREsAndRelations.keySet()) {
            for (VocabularyRelatedEntity vre
                    : draftREsAndRelations.get(reId)) {
                em().remove(vre);
            }
        }
        draftREsAndRelations.clear();
    }

    /** Delete a draft database row associated with this model
     * that specifies deletion of a row with a specified related entity Id
     * and relation; if there is such a draft database row.
     * @param reId The related entity.
     * @param rer The related entity relation.
     */
    private void deleteDraftDeletionDatabaseRow(final Integer reId,
            final RelatedEntityRelation rer) {
        List<VocabularyRelatedEntity> draftsForId =
                draftREsAndRelations.get(reId);
        if (draftsForId == null) {
            return;
        }
        for (VocabularyRelatedEntity vre : draftsForId) {
            if (vre.getRelation() == rer
                    && TemporalUtils.isDraftDeletion(vre)) {
                em().remove(vre);
                draftREsAndRelations.get(reId).remove(vre);
                return;
            }
        }
    }

    /** Get a draft database row associated with this model
     * with a specified related entity Id and relation,
     * if there is such a draft row.
     * It might specify either addition/modification or deletion.
     * @param reId The related entity.
     * @param rer The related entity relation.
     * @return The draft VocabularyRelatedEntity row, if there is one,
     *      or null if there is no such row.
     */
    private VocabularyRelatedEntity getDraftDatabaseRow(
            final Integer reId, final RelatedEntityRelation rer) {
        List<VocabularyRelatedEntity> draftsForId =
                draftREsAndRelations.get(reId);
        if (draftsForId == null) {
            return null;
        }
        for (VocabularyRelatedEntity vre : draftsForId) {
            if (vre.getRelation() == rer) {
                return vre;
            }
        }
        return null;
    }

    /** Get a draft database row associated with this model
     * that specify addition/modification of a row with a specified
     * related entity Id and relation, if there is such a draft row.
     * @param reId The related entity.
     * @param rer The related entity relation.
     * @return The draft VocabularyRelatedEntity row, if there is one,
     *      or null if there is no such row.
     */
    private VocabularyRelatedEntity getDraftAdditionOrModificationDatabaseRow(
            final Integer reId, final RelatedEntityRelation rer) {
        List<VocabularyRelatedEntity> draftsForId =
                draftREsAndRelations.get(reId);
        if (draftsForId == null) {
            return null;
        }
        for (VocabularyRelatedEntity vre : draftsForId) {
            if (vre.getRelation() == rer
                    && TemporalUtils.isDraftAdditionOrModification(vre)) {
                return vre;
            }
        }
        return null;
    }

    // The following method was done as preliminary work, but it
    // has not been needed so far.
//    /** Delete all of the draft database rows associated with this model
//     * that specify addition/modification of a row with a specified
//     * related entity Id and relation.
//     * @param reId The related entity.
//     * @param rer The related entity relation.
//     */
//    private void deleteDraftAdditionOrModificationDatabaseRow(
//            final Integer reId, final RelatedEntityRelation rer) {
//        for (VocabularyRelatedEntity vre : draftREsAndRelations.get(reId)) {
//            if (vre.getRelation() == rer
//                    && TemporalUtils.isDraftAdditionOrModification(vre)) {
//                em.remove(vre);
//                draftREsAndRelations.get(reId).remove(vre);
//            }
//        }
//        draftREsAndRelations.remove(reId);
//    }

    /** {@inheritDoc} */
    @Override
    public void applyChanges(final Vocabulary updatedVocabulary) {
        VocabularyStatus status = updatedVocabulary.getStatus();
        if (status == VocabularyStatus.DRAFT) {
            applyChangesDraft(updatedVocabulary);
        } else {
            applyChangesCurrent(updatedVocabulary);
        }
    }

    /** Make the database's draft view of the
     * VocabularyRelatedEntities match updatedVocabulary.
     * @param updatedVocabulary The updated vocabulary.
     */
    private void applyChangesDraft(final Vocabulary updatedVocabulary) {
        // Create sequences.
        // First, the current values.
        List<VocabularyRelatedEntityElement> currentSequence =
                new ArrayList<>();
        for (Integer reId : currentREsAndRelations.keySet()) {
            for (VocabularyRelatedEntity vre
                    : currentREsAndRelations.get(reId)) {
                currentSequence.add(new VocabularyRelatedEntityElement(
                        reId, vre.getRelation(), vre));
            }
        }
        Collections.sort(currentSequence);

        // And now, the updated values.
        List<VocabularyRelatedEntityElement> updatedSequence =
                new ArrayList<>();
        updatedVocabulary.getRelatedEntityRef().forEach(
                reRef -> {
                    Integer reId = reRef.getId();
                    for (RelatedEntityRelation rel : reRef.getRelation()) {
                        updatedSequence.add(new VocabularyRelatedEntityElement(
                                reId, rel, null));
                    }
                });
        Collections.sort(updatedSequence);

        // Compute difference.
        final SequencesComparator<VocabularyRelatedEntityElement> comparator =
                new SequencesComparator<>(
                        currentSequence, updatedSequence);
        // Apply the changes.
        comparator.getScript().visit(new UpdateDraftVisitor());
    }

    /** Visitor class that applies a sequence of updates to the
     * draft view of the database. */
    private class UpdateDraftVisitor
        implements CommandVisitor<VocabularyRelatedEntityElement> {

        /** {@inheritDoc} */
        @Override
        public void visitKeepCommand(
                final VocabularyRelatedEntityElement vree) {
            Integer keepReId = vree.getReId();
            RelatedEntityRelation keepReRelation = vree.getReRelation();
            VocabularyRelatedEntity draftVre =
                    getDraftDatabaseRow(keepReId, keepReRelation);
            if (draftVre != null) {
                // In this case, it must be a draft deletion.
                // (There isn't (i.e., shouldn't be) a draft row
                // for addition/modification where the published
                // view already has a row specifying the same values.)
                // We no longer wish to do that deletion.
                deleteDraftDeletionDatabaseRow(keepReId, keepReRelation);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void visitDeleteCommand(
                final VocabularyRelatedEntityElement vree) {
            Integer deleteReId = vree.getReId();
            RelatedEntityRelation deleteReRelation = vree.getReRelation();
            VocabularyRelatedEntity draftVre =
                    getDraftDatabaseRow(deleteReId, deleteReRelation);
            // If there is an existing draft row, it can only be a draft
            // deletion. In that case, no further action is required;
            // leave the existing draft deletion.
            // NB: this is the case because for VREs, there is no
            // notion of "modification/update", but only add and delete.
            // If there were, we would need to have code to apply
            // an update to an existing draft modification.
            // But if there is no existing draft row ...
            if (draftVre == null) {
                // No draft row; add one to do the deletion.
                VocabularyRelatedEntity draftDeletionVre =
                        new VocabularyRelatedEntity();
                draftDeletionVre.setVocabularyId(vocabularyId());
                draftDeletionVre.setRelatedEntityId(deleteReId);
                draftDeletionVre.setRelation(deleteReRelation);
                draftDeletionVre.setModifiedBy(modifiedBy());
                TemporalUtils.makeDraftDeletion(draftDeletionVre);
                VocabularyRelatedEntityDAO.saveVocabularyRelatedEntity(em(),
                        draftDeletionVre);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void visitInsertCommand(
                final VocabularyRelatedEntityElement vree) {
            Integer insertReId = vree.getReId();
            RelatedEntityRelation insertReRelation = vree.getReRelation();
            VocabularyRelatedEntity draftVre =
                    getDraftDatabaseRow(insertReId, insertReRelation);
            // If there is an existing draft row, it can only be a draft
            // addition. In that case, no further action is required;
            // leave the existing draft addition.
            // NB: this is the case because for VREs, there is no
            // notion of "modification/update", but only add and delete.
            // If there were, we would need to have code to apply
            // an update to an existing draft modification.
            // But if there is no existing draft row ...
            if (draftVre == null) {
                // No draft row; add one to do the insertion.
                VocabularyRelatedEntity draftInsertionVre =
                        new VocabularyRelatedEntity();
                draftInsertionVre.setVocabularyId(vocabularyId());
                draftInsertionVre.setRelatedEntityId(insertReId);
                draftInsertionVre.setRelation(insertReRelation);
                draftInsertionVre.setModifiedBy(modifiedBy());
                TemporalUtils.makeDraftAdditionOrModification(
                        draftInsertionVre);
                VocabularyRelatedEntityDAO.saveVocabularyRelatedEntity(em(),
                        draftInsertionVre);
            }
        }
    }

    /** Make the database's currently-valid view of the
     * VocabularyRelatedEntities match updatedVocabulary.
     * @param updatedVocabulary The updated vocabulary.
     */
    private void applyChangesCurrent(final Vocabulary updatedVocabulary) {
        // Create sequences.
        // First, the current values.
        List<VocabularyRelatedEntityElement> currentSequence =
                new ArrayList<>();
        for (Integer reId : currentREsAndRelations.keySet()) {
            for (VocabularyRelatedEntity vre
                    : currentREsAndRelations.get(reId)) {
                currentSequence.add(new VocabularyRelatedEntityElement(
                        reId, vre.getRelation(), vre));
            }
        }
        Collections.sort(currentSequence);

        // And now, the updated values.
        List<VocabularyRelatedEntityElement> updatedSequence =
                new ArrayList<>();
        updatedVocabulary.getRelatedEntityRef().forEach(
                reRef -> {
                    Integer reId = reRef.getId();
                    for (RelatedEntityRelation rel : reRef.getRelation()) {
                        updatedSequence.add(new VocabularyRelatedEntityElement(
                                reId, rel, null));
                    }
                });
        Collections.sort(updatedSequence);

        // Compute difference.
        final SequencesComparator<VocabularyRelatedEntityElement> comparator =
                new SequencesComparator<>(
                        currentSequence, updatedSequence);
        // Apply the changes.
        comparator.getScript().visit(new UpdateCurrentVisitor());
        // Delete any remaining draft rows.
        deleteDraftDatabaseRows();
    }

    /** Visitor class that applies a sequence of updates to the
     * currently-valid view of the database. */
    private class UpdateCurrentVisitor
        implements CommandVisitor<VocabularyRelatedEntityElement> {

        /** {@inheritDoc} */
        @Override
        public void visitKeepCommand(
                final VocabularyRelatedEntityElement vree) {
            // No action required.
            // There _may_ be draft rows that say to delete this VRE.
            // We don't worry about them here; they are cleaned up by a
            // subsequent call to deleteDraftDatabaseRows().
        }

        /** {@inheritDoc} */
        @Override
        public void visitDeleteCommand(
                final VocabularyRelatedEntityElement vree) {
            // Make the existing row historical.
            VocabularyRelatedEntity vre = vree.getVre();
            TemporalUtils.makeHistorical(vre, nowTime());
            vre.setModifiedBy(modifiedBy());
            VocabularyRelatedEntityDAO.updateVocabularyRelatedEntity(
                    em(), vre);
            deleteDraftDeletionDatabaseRow(vre.getRelatedEntityId(),
                    vre.getRelation());
        }

        /** {@inheritDoc} */
        @Override
        public void visitInsertCommand(
                final VocabularyRelatedEntityElement vree) {
            // Add a new row ...
            // ... but try to reuse a suitable draft row, if there is one.
            Integer newReId = vree.getReId();
            RelatedEntityRelation newReRelation = vree.getReRelation();
            VocabularyRelatedEntity draftVre =
                    getDraftAdditionOrModificationDatabaseRow(newReId,
                            newReRelation);
            if (draftVre != null) {
                // Reuse existing draft row.
                draftVre.setStartDate(nowTime());
                TemporalUtils.makeCurrentlyValid(draftVre);
                draftVre.setModifiedBy(modifiedBy());
                VocabularyRelatedEntityDAO.updateVocabularyRelatedEntity(
                        em(), draftVre);
                // No longer regard as a draft row in our own records.
                // Important to to do this, as we will call
                // deleteDraftDatabaseRows() later.
                draftREsAndRelations.get(newReId).remove(draftVre);
            } else {
                // New row required.
                VocabularyRelatedEntity newVre = new VocabularyRelatedEntity();
                newVre.setVocabularyId(vocabularyId());
                newVre.setRelatedEntityId(newReId);
                newVre.setRelation(newReRelation);
                newVre.setModifiedBy(modifiedBy());
                TemporalUtils.makeCurrentlyValid(newVre, nowTime());
                VocabularyRelatedEntityDAO.saveVocabularyRelatedEntity(em(),
                        newVre);
            }
        }
    }

}
