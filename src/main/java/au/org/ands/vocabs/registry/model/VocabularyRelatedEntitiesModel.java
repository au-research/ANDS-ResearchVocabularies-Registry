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
import au.org.ands.vocabs.registry.db.converter.RelatedEntityDbSchemaMapper;
import au.org.ands.vocabs.registry.db.converter.RelatedEntityIdentifierDbSchemaMapper;
import au.org.ands.vocabs.registry.db.converter.VocabularyRelatedEntityDbSchemaMapper;
import au.org.ands.vocabs.registry.db.dao.RelatedEntityDAO;
import au.org.ands.vocabs.registry.db.dao.RelatedEntityIdentifierDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyRelatedEntityDAO;
import au.org.ands.vocabs.registry.db.entity.RelatedEntity;
import au.org.ands.vocabs.registry.db.entity.VocabularyRelatedEntity;
import au.org.ands.vocabs.registry.enums.RelatedEntityRelation;
import au.org.ands.vocabs.registry.enums.VocabularyStatus;
import au.org.ands.vocabs.registry.model.sequence.VocabularyRelatedEntityElement;
import au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntityIdentifier;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary.RelatedEntityRef;

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
    protected void insertIntoSchemaFromDraft(final
            au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
            outputVocabulary,
            final boolean includeVersions,
            final boolean includeAccessPoints,
            final boolean includeRelatedEntitiesAndVocabularies) {

        List<RelatedEntityRef> rerList = outputVocabulary.getRelatedEntityRef();
        VocabularyRelatedEntityDbSchemaMapper vredbMapper =
                VocabularyRelatedEntityDbSchemaMapper.INSTANCE;

        RelatedEntityDbSchemaMapper reMapper =
                RelatedEntityDbSchemaMapper.INSTANCE;
        RelatedEntityIdentifierDbSchemaMapper reiMapper =
                RelatedEntityIdentifierDbSchemaMapper.INSTANCE;

        for (Integer reId : draftREsAndRelations.keySet()) {
            RelatedEntityRef reRef = null;
            for (VocabularyRelatedEntity vre
                    : draftREsAndRelations.get(reId)) {
                if (reRef == null) {
                    reRef = vredbMapper.sourceToTarget(vre);
                }
                reRef.getRelation().add(vre.getRelation());
            }
            if (includeRelatedEntitiesAndVocabularies) {
                au.org.ands.vocabs.registry.schema.vocabulary201701.
                RelatedEntity targetRelatedEntity = getRelatedEntity(
                        reId, reMapper, reiMapper);
                reRef.setRelatedEntity(targetRelatedEntity);
            }
            rerList.add(reRef);
        }
    }

    /** Fetch a RelatedEntity and map to the registry schema.
     * @param reId The related entity Id.
     * @param reMapper The Related Entity mapper to use.
     * @param reiMapper The Related Entity Identifier mapper to use.
     * @return The RelatedEntity, in registry schema format.
     */
    private au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntity
    getRelatedEntity(
            final Integer reId, final RelatedEntityDbSchemaMapper reMapper,
            final RelatedEntityIdentifierDbSchemaMapper reiMapper) {
        RelatedEntity dbRE = RelatedEntityDAO.
                getCurrentRelatedEntityByRelatedEntityId(reId);
        au.org.ands.vocabs.registry.schema.vocabulary201701.
        RelatedEntity targetRelatedEntity =
            reMapper.sourceToTarget(dbRE);
        // Get the related entity identifiers.
        List<RelatedEntityIdentifier>
        targetRelatedEntityIdentifiers =
                targetRelatedEntity.getRelatedEntityIdentifier();
        List<au.org.ands.vocabs.registry.db.entity.
        RelatedEntityIdentifier>
            dbRelatedEntityIdentifiers =
                RelatedEntityIdentifierDAO.
                getCurrentRelatedEntityIdentifierListForRelatedEntity(
                        dbRE.getRelatedEntityId());
        for (au.org.ands.vocabs.registry.db.entity.RelatedEntityIdentifier
                dbREI : dbRelatedEntityIdentifiers) {
            targetRelatedEntityIdentifiers.add(
                    reiMapper.sourceToTarget(dbREI));
        }
        return targetRelatedEntity;
    }

    /** {@inheritDoc} */
    @Override
    protected void deleteOnlyCurrent() {
        for (Integer reId : currentREsAndRelations.keySet()) {
            for (VocabularyRelatedEntity vre
                    : currentREsAndRelations.get(reId)) {
                TemporalUtils.makeHistorical(vre, nowTime());
                vre.setModifiedBy(modifiedBy());
                VocabularyRelatedEntityDAO.updateVocabularyRelatedEntity(
                        em(), vre);
            }
        }
        currentREsAndRelations.clear();
    }

    /** {@inheritDoc} */
    @Override
    protected void makeCurrentIntoDraft() {
        if (currentREsAndRelations.isEmpty()) {
            // Oops, nothing to do!
            return;
        }
        if (!draftREsAndRelations.isEmpty()) {
            // Error
            throw new IllegalArgumentException(
                    "Existing draft; you must delete it first");
        }
        for (Integer reId : currentREsAndRelations.keySet()) {
            for (VocabularyRelatedEntity vre
                    : currentREsAndRelations.get(reId)) {
                TemporalUtils.makeHistorical(vre, nowTime());
                vre.setModifiedBy(modifiedBy());
                VocabularyRelatedEntityDAO.updateVocabularyRelatedEntity(
                        em(), vre);
                // Now make a new draft record.
                VocabularyRelatedEntity newVre =
                        new VocabularyRelatedEntity();
                newVre.setVocabularyId(vocabularyId());
                newVre.setRelatedEntityId(reId);
                newVre.setRelation(vre.getRelation());
                newVre.setModifiedBy(modifiedBy());
                TemporalUtils.makeDraft(newVre);
                VocabularyRelatedEntityDAO.saveVocabularyRelatedEntity(
                        em(), newVre);
                draftREsAndRelations.add(reId, newVre);
            }
        }
        currentREsAndRelations.clear();
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

    /** Get a draft database row associated with this model
     * with a specified related entity Id and relation,
     * if there is such a draft row.
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
     * VocabularyRelatedEntities match updatedVocabulary.
     * @param updatedVocabulary The updated vocabulary.
     */
    private void applyChangesDraft(final Vocabulary updatedVocabulary) {
        // Create sequences.
        // First, any existing draft values.
        List<VocabularyRelatedEntityElement> existingDraftSequence =
                new ArrayList<>();
        for (Integer reId : draftREsAndRelations.keySet()) {
            for (VocabularyRelatedEntity vre
                    : draftREsAndRelations.get(reId)) {
                existingDraftSequence.add(new VocabularyRelatedEntityElement(
                        reId, vre.getRelation(), vre));
            }
        }
        Collections.sort(existingDraftSequence);

        // And now, the updated draft values.
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
                        existingDraftSequence, updatedSequence);
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
            // No action required.
        }

        /** {@inheritDoc} */
        @Override
        public void visitDeleteCommand(
                final VocabularyRelatedEntityElement vree) {
            Integer draftReId = vree.getReId();
            VocabularyRelatedEntity draftVre = vree.getVre();
            // Remove from our own records ...
            draftREsAndRelations.get(draftReId).remove(draftVre);
            // ... and from the database.
            em().remove(draftVre);
        }

        /** {@inheritDoc} */
        @Override
        public void visitInsertCommand(
                final VocabularyRelatedEntityElement vree) {
            Integer insertReId = vree.getReId();
            RelatedEntityRelation insertReRelation = vree.getReRelation();
            VocabularyRelatedEntity draftInsertionVre =
                    new VocabularyRelatedEntity();
            draftInsertionVre.setVocabularyId(vocabularyId());
            draftInsertionVre.setRelatedEntityId(insertReId);
            draftInsertionVre.setRelation(insertReRelation);
            draftInsertionVre.setModifiedBy(modifiedBy());
            TemporalUtils.makeDraft(draftInsertionVre);
            VocabularyRelatedEntityDAO.saveVocabularyRelatedEntity(em(),
                    draftInsertionVre);
            draftREsAndRelations.add(insertReId, draftInsertionVre);
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
                    getDraftDatabaseRow(newReId, newReRelation);
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
