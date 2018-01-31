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
import au.org.ands.vocabs.registry.db.converter.VocabularyDbRelatedVocabularySchemaMapper;
import au.org.ands.vocabs.registry.db.converter.VocabularyRelatedVocabularyDbSchemaMapper;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyRelatedVocabularyDAO;
import au.org.ands.vocabs.registry.db.entity.VocabularyRelatedVocabulary;
import au.org.ands.vocabs.registry.enums.RelatedVocabularyRelation;
import au.org.ands.vocabs.registry.enums.VocabularyStatus;
import au.org.ands.vocabs.registry.model.sequence.VocabularyRelatedVocabularyElement;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary.RelatedVocabularyRef;

/** Domain model of a vocabulary's vocabulary-related-vocabularies.
 */
public class VocabularyRelatedVocabulariesModel extends ModelBase {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Map of the current vocabulary-related-vocabularies and their relations.
     * The keys are Vocabulary Ids. */
    private MultivaluedMap<Integer, VocabularyRelatedVocabulary>
        currentRVsAndRelations =
            new MultivaluedHashMap<>();

    /** Map of the draft vocabulary-related-vocabularies and their relations.
     * The keys are Vocabulary Ids. */
    private MultivaluedMap<Integer, VocabularyRelatedVocabulary>
        draftRVsAndRelations =
            new MultivaluedHashMap<>();

    /** Construct vocabulary-related-vocabularies model for a vocabulary.
     * @param anEm The EntityManager to be used to fetch and update
     *      database data.
     * @param aVocabularyId The Id of the vocabulary for which the model
     *      is to be constructed.
     * @throws IllegalArgumentException If aVocabularyId is null.
     */
    public VocabularyRelatedVocabulariesModel(final EntityManager anEm,
            final Integer aVocabularyId) {
        if (aVocabularyId == null) {
            logger.error("Attempt to construct vocabulary-related-vocabularies "
                    + "model with no Id");
            throw new IllegalArgumentException(
                    "Attempt to construct vocabulary-related-vocabularies "
                    + "model with no Id");
        }
        setEm(anEm);
        setVocabularyId(aVocabularyId);
        populateModel();
    }

    /** {@inheritDoc} */
    @Override
    protected void populateModel() {
        // Current
        currentRVsAndRelations =
                VocabularyRelatedVocabularyDAO.
                getCurrentVocabularyRelatedVocabulariesForVocabulary(em(),
                        vocabularyId());

        // Draft
        draftRVsAndRelations =
                VocabularyRelatedVocabularyDAO.
                getDraftVocabularyRelatedVocabulariesForVocabulary(em(),
                        vocabularyId());
    }

    /** {@inheritDoc} */
    @Override
    public List<String> describeModel() {
        List<String> description = new ArrayList<>();
        if (vocabularyId() == null) {
            description.add("VRV | No vocabulary Id");
            return description;
        }
        description.add("VRV | Vocabulary; Id: " + vocabularyId());
        if (currentRVsAndRelations != null) {
            for (Integer rvId : currentRVsAndRelations.keySet()) {
                for (VocabularyRelatedVocabulary vrv
                        : currentRVsAndRelations.get(rvId)) {
                    description.add("VRV | Current vocabulary has RV; "
                            + "RV Id, relation: " + rvId + ","
                            + vrv.getRelation());
                }
            }
        }
        if (draftRVsAndRelations != null) {
            for (Integer rvId : draftRVsAndRelations.keySet()) {
                for (VocabularyRelatedVocabulary vrv
                        : draftRVsAndRelations.get(rvId)) {
                    description.add("VRV | Draft vocabulary has RV; "
                            + "RV Id, relation, meaning: " + rvId + ","
                            + vrv.getRelation() + ","
                            + TemporalUtils.getTemporalDescription(
                                    vrv).getValue());
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
    protected void insertIntoSchemaFromCurrent(final
            au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
            outputVocabulary,
            final boolean includeVersions,
            final boolean includeAccessPoints,
            final boolean includeRelatedEntitiesAndVocabularies) {
        List<RelatedVocabularyRef> rvrList =
                outputVocabulary.getRelatedVocabularyRef();

        VocabularyRelatedVocabularyDbSchemaMapper vrvdbMapper =
                VocabularyRelatedVocabularyDbSchemaMapper.INSTANCE;
        VocabularyDbRelatedVocabularySchemaMapper rvMapper =
                VocabularyDbRelatedVocabularySchemaMapper.INSTANCE;

        for (Integer reId : currentRVsAndRelations.keySet()) {
            RelatedVocabularyRef rvRef = null;
            for (VocabularyRelatedVocabulary vrv
                    : currentRVsAndRelations.get(reId)) {
                if (rvRef == null) {
                    rvRef = vrvdbMapper.sourceToTarget(vrv);
                }
                rvRef.getRelation().add(vrv.getRelation());
            }
            if (includeRelatedEntitiesAndVocabularies) {
                au.org.ands.vocabs.registry.schema.vocabulary201701.
                RelatedVocabulary targetRelatedVocabulary =
                getRelatedVocabulary(reId, rvMapper);
                rvRef.setRelatedVocabulary(targetRelatedVocabulary);
            }
            rvrList.add(rvRef);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void insertIntoSchemaFromDraft(final
            au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
            outputVocabulary,
            final boolean includeVersions,
            final boolean includeAccessPoints,
            final boolean includeRelatedEntitiesAndVocabularies) {
        List<RelatedVocabularyRef> rvrList =
                outputVocabulary.getRelatedVocabularyRef();

        VocabularyRelatedVocabularyDbSchemaMapper vrvdbMapper =
                VocabularyRelatedVocabularyDbSchemaMapper.INSTANCE;
        VocabularyDbRelatedVocabularySchemaMapper rvMapper =
                VocabularyDbRelatedVocabularySchemaMapper.INSTANCE;

        for (Integer reId : draftRVsAndRelations.keySet()) {
            RelatedVocabularyRef rvRef = null;
            for (VocabularyRelatedVocabulary vrv
                    : draftRVsAndRelations.get(reId)) {
                if (rvRef == null) {
                    rvRef = vrvdbMapper.sourceToTarget(vrv);
                }
                rvRef.getRelation().add(vrv.getRelation());
            }
            if (includeRelatedEntitiesAndVocabularies) {
                au.org.ands.vocabs.registry.schema.vocabulary201701.
                RelatedVocabulary targetRelatedVocabulary =
                getRelatedVocabulary(reId, rvMapper);
                rvRef.setRelatedVocabulary(targetRelatedVocabulary);
            }
            rvrList.add(rvRef);
        }
    }

    /** Fetch a related vocabulary and map to the registry schema.
     * @param rvId The related vocabulary Id.
     * @param rvMapper The related vocabulary mapper to use.
     * @return The RelatedVocabulary, in registry schema format.
     */
    private au.org.ands.vocabs.registry.schema.vocabulary201701.
    RelatedVocabulary getRelatedVocabulary(
            final Integer rvId,
            final VocabularyDbRelatedVocabularySchemaMapper rvMapper) {
        au.org.ands.vocabs.registry.db.entity.Vocabulary dbRV = VocabularyDAO.
                getCurrentVocabularyByVocabularyId(rvId);
        au.org.ands.vocabs.registry.schema.vocabulary201701.
        RelatedVocabulary targetRelatedVocabulary =
            rvMapper.sourceToTarget(dbRV);
        return targetRelatedVocabulary;
    }

    /** {@inheritDoc} */
    @Override
    protected void deleteOnlyCurrent() {
        for (Integer reId : currentRVsAndRelations.keySet()) {
            for (VocabularyRelatedVocabulary vrv
                    : currentRVsAndRelations.get(reId)) {
                TemporalUtils.makeHistorical(vrv, nowTime());
                vrv.setModifiedBy(modifiedBy());
                VocabularyRelatedVocabularyDAO.
                updateVocabularyRelatedVocabulary(em(), vrv);
            }
        }
        currentRVsAndRelations.clear();
    }

    /** {@inheritDoc} */
    @Override
    protected void makeCurrentIntoDraft() {
        if (currentRVsAndRelations.isEmpty()) {
            // Oops, nothing to do!
            return;
        }
        if (!draftRVsAndRelations.isEmpty()) {
            // Error
            throw new IllegalArgumentException(
                    "Existing draft; you must delete it first");
        }
        for (Integer rvId : currentRVsAndRelations.keySet()) {
            for (VocabularyRelatedVocabulary vrv
                    : currentRVsAndRelations.get(rvId)) {
                TemporalUtils.makeHistorical(vrv, nowTime());
                vrv.setModifiedBy(modifiedBy());
                VocabularyRelatedVocabularyDAO.
                updateVocabularyRelatedVocabulary(em(), vrv);
                // Now make a new draft record.
                VocabularyRelatedVocabulary newVrv =
                        new VocabularyRelatedVocabulary();
                newVrv.setVocabularyId(vocabularyId());
                newVrv.setRelatedVocabularyId(rvId);
                newVrv.setRelation(vrv.getRelation());
                newVrv.setModifiedBy(modifiedBy());
                TemporalUtils.makeDraft(newVrv);
                VocabularyRelatedVocabularyDAO.saveVocabularyRelatedVocabulary(
                        em(), newVrv);
                draftRVsAndRelations.add(rvId, newVrv);
            }
        }
        currentRVsAndRelations.clear();
    }

    /** {@inheritDoc} */
    @Override
    protected void deleteOnlyDraft() {
        deleteDraftDatabaseRows();
    }

    /** {@inheritDoc} */
    @Override
    protected void deleteDraftDatabaseRows() {
        for (Integer rvId : draftRVsAndRelations.keySet()) {
            for (VocabularyRelatedVocabulary vrv
                    : draftRVsAndRelations.get(rvId)) {
                VocabularyRelatedVocabularyDAO.
                    deleteVocabularyRelatedVocabulary(em(), vrv);
            }
        }
        draftRVsAndRelations.clear();
    }

    /** Get a draft database row associated with this model
     * with a specified related vocabulary Id and relation,
     * if there is such a draft row.
     * @param reId The related vocabulary.
     * @param rvr The related vocabulary relation.
     * @return The draft VocabularyRelatedVocabulary row, if there is one,
     *      or null if there is no such row.
     */
    private VocabularyRelatedVocabulary getDraftDatabaseRow(
            final Integer reId, final RelatedVocabularyRelation rvr) {
        List<VocabularyRelatedVocabulary> draftsForId =
                draftRVsAndRelations.get(reId);
        if (draftsForId == null) {
            return null;
        }
        for (VocabularyRelatedVocabulary vrv : draftsForId) {
            if (vrv.getRelation() == rvr) {
                return vrv;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    protected void applyChanges(final Vocabulary updatedVocabulary) {
        VocabularyStatus status = updatedVocabulary.getStatus();
        if (status == VocabularyStatus.DRAFT) {
            applyChangesDraft(updatedVocabulary);
        } else {
            applyChangesCurrent(updatedVocabulary);
        }
    }

    /** Make the database's draft view of the
     * VocabularyRelatedVocabularies match updatedVocabulary.
     * @param updatedVocabulary The updated vocabulary.
     */
    private void applyChangesDraft(final Vocabulary updatedVocabulary) {
        // Create sequences.
        // First, any existing draft values.
        List<VocabularyRelatedVocabularyElement> existingDraftSequence =
                new ArrayList<>();
        for (Integer rvId : draftRVsAndRelations.keySet()) {
            for (VocabularyRelatedVocabulary vrv
                    : draftRVsAndRelations.get(rvId)) {
                existingDraftSequence.add(
                        new VocabularyRelatedVocabularyElement(
                        rvId, vrv.getRelation(), vrv));
            }
        }
        Collections.sort(existingDraftSequence);

        // And now, the updated draft values.
        List<VocabularyRelatedVocabularyElement> updatedSequence =
                new ArrayList<>();
        updatedVocabulary.getRelatedVocabularyRef().forEach(
                rvRef -> {
                    Integer rvId = rvRef.getId();
                    for (RelatedVocabularyRelation rel : rvRef.getRelation()) {
                        updatedSequence.add(
                                new VocabularyRelatedVocabularyElement(
                                rvId, rel, null));
                    }
                });
        Collections.sort(updatedSequence);

        // Compute difference.
        final SequencesComparator<VocabularyRelatedVocabularyElement>
        comparator = new SequencesComparator<>(
                        existingDraftSequence, updatedSequence);
        // Apply the changes.
        comparator.getScript().visit(new UpdateDraftVisitor());
    }

    /** Visitor class that applies a sequence of updates to the
     * draft view of the database. */
    private class UpdateDraftVisitor
        implements CommandVisitor<VocabularyRelatedVocabularyElement> {

        /** {@inheritDoc} */
        @Override
        public void visitKeepCommand(
                final VocabularyRelatedVocabularyElement vrve) {
            // No action required.
        }

        /** {@inheritDoc} */
        @Override
        public void visitDeleteCommand(
                final VocabularyRelatedVocabularyElement vrve) {
            Integer draftRvId = vrve.getRvId();
            VocabularyRelatedVocabulary draftVrv = vrve.getVrv();
            // Remove from our own records ...
            draftRVsAndRelations.get(draftRvId).remove(draftVrv);
            // ... and from the database.
            VocabularyRelatedVocabularyDAO.deleteVocabularyRelatedVocabulary(
                    em(), draftVrv);
        }

        /** {@inheritDoc} */
        @Override
        public void visitInsertCommand(
                final VocabularyRelatedVocabularyElement vrve) {
            Integer insertRvId = vrve.getRvId();
            RelatedVocabularyRelation insertRvRelation = vrve.getRvRelation();
            VocabularyRelatedVocabulary draftInsertionVrv =
                    new VocabularyRelatedVocabulary();
            draftInsertionVrv.setVocabularyId(vocabularyId());
            draftInsertionVrv.setRelatedVocabularyId(insertRvId);
            draftInsertionVrv.setRelation(insertRvRelation);
            draftInsertionVrv.setModifiedBy(modifiedBy());
            TemporalUtils.makeDraft(draftInsertionVrv);
            VocabularyRelatedVocabularyDAO.saveVocabularyRelatedVocabulary(em(),
                    draftInsertionVrv);
            draftRVsAndRelations.add(insertRvId, draftInsertionVrv);
        }
    }

    /** Make the database's currently-valid view of the
     * VocabularyRelatedVocabularies match updatedVocabulary.
     * @param updatedVocabulary The updated vocabulary.
     */
    private void applyChangesCurrent(final Vocabulary updatedVocabulary) {
        // Create sequences.
        // First, the current values.
        List<VocabularyRelatedVocabularyElement> currentSequence =
                new ArrayList<>();
        for (Integer reId : currentRVsAndRelations.keySet()) {
            for (VocabularyRelatedVocabulary vrv
                    : currentRVsAndRelations.get(reId)) {
                currentSequence.add(new VocabularyRelatedVocabularyElement(
                        reId, vrv.getRelation(), vrv));
            }
        }
        Collections.sort(currentSequence);

        // And now, the updated values.
        List<VocabularyRelatedVocabularyElement> updatedSequence =
                new ArrayList<>();
        updatedVocabulary.getRelatedVocabularyRef().forEach(
                rvRef -> {
                    Integer reId = rvRef.getId();
                    for (RelatedVocabularyRelation rel : rvRef.getRelation()) {
                        updatedSequence.add(
                                new VocabularyRelatedVocabularyElement(
                                reId, rel, null));
                    }
                });
        Collections.sort(updatedSequence);

        // Compute difference.
        final SequencesComparator<VocabularyRelatedVocabularyElement>
        comparator = new SequencesComparator<>(
                        currentSequence, updatedSequence);
        // Apply the changes.
        comparator.getScript().visit(new UpdateCurrentVisitor());
        // Delete any remaining draft rows.
        deleteDraftDatabaseRows();
    }

    /** Visitor class that applies a sequence of updates to the
     * currently-valid view of the database. */
    private class UpdateCurrentVisitor
        implements CommandVisitor<VocabularyRelatedVocabularyElement> {

        /** {@inheritDoc} */
        @Override
        public void visitKeepCommand(
                final VocabularyRelatedVocabularyElement vrve) {
            // No action required.
        }

        /** {@inheritDoc} */
        @Override
        public void visitDeleteCommand(
                final VocabularyRelatedVocabularyElement vrve) {
            // Make the existing row historical.
            VocabularyRelatedVocabulary vrv = vrve.getVrv();
            TemporalUtils.makeHistorical(vrv, nowTime());
            vrv.setModifiedBy(modifiedBy());
            VocabularyRelatedVocabularyDAO.updateVocabularyRelatedVocabulary(
                    em(), vrv);
            // Remove from our own records ...
            currentRVsAndRelations.get(vrv.getRelatedVocabularyId()).
                remove(vrv);
        }

        /** {@inheritDoc} */
        @Override
        public void visitInsertCommand(
                final VocabularyRelatedVocabularyElement vrve) {
            // Add a new row ...
            // ... but try to reuse a suitable draft row, if there is one.
            Integer newRvId = vrve.getRvId();
            RelatedVocabularyRelation newRvRelation = vrve.getRvRelation();
            VocabularyRelatedVocabulary draftVrv =
                    getDraftDatabaseRow(newRvId, newRvRelation);
            if (draftVrv != null) {
                // Reuse existing draft row.
                draftVrv.setStartDate(nowTime());
                TemporalUtils.makeCurrentlyValid(draftVrv);
                draftVrv.setModifiedBy(modifiedBy());
                VocabularyRelatedVocabularyDAO.
                updateVocabularyRelatedVocabulary(em(), draftVrv);
                // No longer regard as a draft row in our own records.
                // Important to to do this, as we will call
                // deleteDraftDatabaseRows() later.
                draftRVsAndRelations.get(newRvId).remove(draftVrv);
                currentRVsAndRelations.add(newRvId, draftVrv);
            } else {
                // New row required.
                VocabularyRelatedVocabulary newVrv =
                        new VocabularyRelatedVocabulary();
                newVrv.setVocabularyId(vocabularyId());
                newVrv.setRelatedVocabularyId(newRvId);
                newVrv.setRelation(newRvRelation);
                newVrv.setModifiedBy(modifiedBy());
                TemporalUtils.makeCurrentlyValid(newVrv, nowTime());
                VocabularyRelatedVocabularyDAO.saveVocabularyRelatedVocabulary(
                        em(), newVrv);
                currentRVsAndRelations.add(newRvId, newVrv);
            }
        }
    }

}
