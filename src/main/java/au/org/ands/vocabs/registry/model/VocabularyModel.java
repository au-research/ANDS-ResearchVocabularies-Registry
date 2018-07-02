/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.model;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.converter.VocabularyRegistrySchemaMapper;
import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.converter.VocabularyDbSchemaMapper;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyIdDAO;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.db.entity.clone.VocabularyClone;
import au.org.ands.vocabs.registry.enums.RegistryEventElementType;
import au.org.ands.vocabs.registry.enums.RegistryEventEventType;
import au.org.ands.vocabs.registry.enums.VocabularyStatus;
import au.org.ands.vocabs.registry.log.RegistryEventUtils;
import au.org.ands.vocabs.registry.schema.vocabulary201701.WorkflowOutcome;
import au.org.ands.vocabs.registry.subscription.Owners;
import au.org.ands.vocabs.registry.utils.SlugGenerator;

/** Vocabulary domain model.
 * This is a representation of a vocabulary as an abstract data type.
 */
public class VocabularyModel extends ModelBase {

    /* The model consists of the following temporally-determined entities:
     *
     * Vocabulary
     *   VocabularyRelatedEntity
     *   VocabularyRelatedVocabulary
     *   Version
     *     VersionArtefact
     *     AccessPoint
     */

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** The current version of the vocabulary record, if there is one. */
    private Vocabulary currentVocabulary;

    /** The draft version of the vocabulary record, if there is one. */
    private Vocabulary draftVocabulary;

    /** The model of the VocabularyRelatedEntities. */
    private VocabularyRelatedEntitiesModel vreModel;

    /** The model of the VocabularyRelatedVocabularies. */
    private VocabularyRelatedVocabulariesModel vrvModel;

    /** The model of the Versions. */
    private VersionsModel versionsModel;

    /** List of all sub-models. */
    private List<ModelBase> subModels = new ArrayList<>();

    /** The outcome of workflow processing, if at least one task did
     * not complete with a SUCCESS outcome. */
    private WorkflowOutcome workflowOutcome;

    /** Set the workflow outcome. Invoked by VersionsModel.
     * @param aWorkflowOutcome The workflowOutcome to set.
     */
    protected void setWorkflowOutcome(final WorkflowOutcome aWorkflowOutcome) {
        workflowOutcome = aWorkflowOutcome;
    }

    /** Construct vocabulary model for a vocabulary.
     * @param anEm The EntityManager to be used to fetch and update
     *      database data.
     * @param aVocabularyId The Id of the vocabulary for which the model
     *      is to be constructed.
     * @throws IllegalArgumentException If aVocabularyId is null.
     */
    public VocabularyModel(final EntityManager anEm,
            final Integer aVocabularyId) {
        if (aVocabularyId == null) {
            logger.error("Attempt to construct vocabulary model with no Id");
            throw new IllegalArgumentException(
                    "Attempt to construct vocabulary model with no Id");
        }
        if (VocabularyIdDAO.getVocabularyIdById(anEm, aVocabularyId) == null) {
            logger.error("Attempt to construct vocabulary model with "
                    + "invalid Id");
            throw new IllegalArgumentException(
                    "Attempt to construct vocabulary model with invalid Id");
        }
        setEm(anEm);
        setVocabularyId(aVocabularyId);
        populateModel();
    }

    // Getters for currentVocabulary and draftVocabulary that are
    // available to sub-models. They may need them in order to construct
    // TaskInfo objects.

    /** Get the current instance of the vocabulary, or null, if there
     * isn't one.
     * @return The current instance of the vocabulary, or null.
     */
    protected Vocabulary getCurrentVocabulary() {
        return currentVocabulary;
    }

    /** Get the draft instance of the vocabulary, or null, if there
     * isn't one.
     * @return The draft instance of the vocabulary, or null.
     */
    protected Vocabulary getDraftVocabulary() {
        return draftVocabulary;
    }

    /** Notify sub-models. */
    @Override
    protected void notifySetNowTime(final LocalDateTime aNowTime) {
        subModels.forEach(sm -> sm.setNowTime(aNowTime));
    }

    /** Notify sub-models. */
    @Override
    protected void notifySetModifiedBy(final String aModifiedBy) {
        subModels.forEach(sm -> sm.setModifiedBy(aModifiedBy));
    }

    /** {@inheritDoc} */
    @Override
    protected void populateModel() {
        // Current
        currentVocabulary =
                VocabularyDAO.getCurrentVocabularyByVocabularyId(em(),
                        vocabularyId());
        // Draft
        List<Vocabulary> draftVocabularyList =
                VocabularyDAO.getDraftVocabularyByVocabularyId(em(),
                        vocabularyId());
        if (!draftVocabularyList.isEmpty()) {
            // There can be at most one draft vocabulary row.
            draftVocabulary = draftVocabularyList.get(0);
        }

        // Sub-models
        vreModel = new VocabularyRelatedEntitiesModel(em(), vocabularyId());
        subModels.add(vreModel);
        vrvModel = new VocabularyRelatedVocabulariesModel(em(), vocabularyId());
        subModels.add(vrvModel);
        versionsModel = new VersionsModel(em(), vocabularyId(), this);
        subModels.add(versionsModel);
    }

    /** {@inheritDoc} */
    @Override
    public List<String> describeModel() {
        List<String> description = new ArrayList<>();
        if (vocabularyId() == null) {
            description.add("No vocabulary Id");
            return description;
        }
        description.add("Vocabulary; Id: " + vocabularyId());
        if (currentVocabulary != null) {
            description.add("Has current vocabulary; Id: "
                    + currentVocabulary.getId());
        }
        if (draftVocabulary != null) {
            description.add("Has draft vocabulary; Id: "
                    + draftVocabulary.getId());
        }

        // Sub-models.
        subModels.forEach(sm -> description.addAll(sm.describeModel()));

        return description;
    }

    /** Log the textual description of the model of the vocabulary. */
    public void logModelDescription() {
        List<String> description = describeModel();
        for (String line : description) {
            logger.info(line);
        }
    }

    /** Determine if this model represents a vocabulary that has
     * a current instance.
     * @return True, iff there is a current instance.
     */
    public boolean hasCurrent() {
        return currentVocabulary != null;
    }

    /** Determine if this model represents a vocabulary that has
     * a draft instance.
     * @return True, iff there is a draft instance.
     */
    public boolean hasDraft() {
        return draftVocabulary != null;
    }

    /** Get the current instance of the vocabulary, in registry schema
     * format. If there is no current instance, null is returned.
     * @param includeVersions Whether or not to include version elements.
     * @param includeAccessPoints Whether or not to include access point
     *      elements.
     * @param includeRelatedEntitiesAndVocabularies Whether or not to include
     *      full related entity elements, and top-level details of
     *      related vocabularies.
     * @return The current instance of the vocabulary, in registry schema
     *      format, if there is a current instance; null, otherwise.
     */
    public au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
    getCurrent(final boolean includeVersions,
            final boolean includeAccessPoints,
            final boolean includeRelatedEntitiesAndVocabularies) {
        if (currentVocabulary == null) {
            return null;
        }
        au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
        outputVocabulary;

        VocabularyDbSchemaMapper mapper =
                VocabularyDbSchemaMapper.INSTANCE;
        outputVocabulary = mapper.sourceToTarget(currentVocabulary, false);
        // Sub-models.
        subModels.forEach(sm -> sm.insertIntoSchemaFromCurrent(outputVocabulary,
                includeVersions, includeAccessPoints,
                includeRelatedEntitiesAndVocabularies));
        if (workflowOutcome != null) {
            outputVocabulary.setWorkflowOutcome(workflowOutcome);
        }
        return outputVocabulary;
    }

    /** Get the draft instance of the vocabulary, in registry schema
     * format. If there is no draft instance, null is returned.
     * @param includeVersions Whether or not to include version elements.
     * @param includeAccessPoints Whether or not to include access point
     *      elements.
     * @param includeRelatedEntitiesAndVocabularies Whether or not to include
     *      full related entity elements, and top-level details of
     *      related vocabularies.
     * @return The draft instance of the vocabulary, in registry schema
     *      format, if there is a draft instance; null, otherwise.
     */
    public au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
    getDraft(final boolean includeVersions,
            final boolean includeAccessPoints,
            final boolean includeRelatedEntitiesAndVocabularies) {
        if (draftVocabulary == null) {
            return null;
        }
        au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
        outputVocabulary;

        VocabularyDbSchemaMapper mapper =
                VocabularyDbSchemaMapper.INSTANCE;
        outputVocabulary = mapper.sourceToTarget(draftVocabulary, false);
        // Sub-models.
        subModels.forEach(sm -> sm.insertIntoSchemaFromDraft(outputVocabulary,
                includeVersions, includeAccessPoints,
                includeRelatedEntitiesAndVocabularies));
        if (workflowOutcome != null) {
            outputVocabulary.setWorkflowOutcome(workflowOutcome);
        }
        return outputVocabulary;
    }

    /** {@inheritDoc} */
    @Override
    protected void insertIntoSchemaFromCurrent(final
            au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
            outputVocabulary,
            final boolean includeVersions,
            final boolean includeAccessPoints,
            final boolean includeRelatedEntitiesAndVocabularies) {
        // No action here. Everything else happens in sub-models.
    }

    /** {@inheritDoc} */
    @Override
    protected void insertIntoSchemaFromDraft(final
            au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
            outputVocabulary,
            final boolean includeVersions,
            final boolean includeAccessPoints,
            final boolean includeRelatedEntitiesAndVocabularies) {
        // No action here. Everything else happens in sub-models.
    }

    /** {@inheritDoc} */
    @Override
    protected void deleteOnlyCurrent() {
        if (currentVocabulary == null) {
            // Oops, nothing to do!
            return;
        }
        // Sub-models first.
        subModels.forEach(sm -> sm.deleteOnlyCurrent());

        TemporalUtils.makeHistorical(currentVocabulary, nowTime());
        currentVocabulary.setModifiedBy(modifiedBy());
        VocabularyDAO.updateVocabulary(em(), currentVocabulary);

        // Add a registry event.
        RegistryEventUtils.createRegistryEvent(
                em(), RegistryEventElementType.VOCABULARIES,
                vocabularyId(), nowTime(),
                RegistryEventEventType.DELETED, modifiedBy(),
                currentVocabulary, null);

        currentVocabulary = null;
    }

    /** {@inheritDoc} */
    @Override
    protected void makeCurrentIntoDraft() {
        if (currentVocabulary == null) {
            // Oops, nothing to do!
            return;
        }
        if (draftVocabulary != null) {
            // Error
            throw new IllegalArgumentException(
                    "Existing draft; you must delete it first");
        }
        // Sub-models first.
        subModels.forEach(sm -> sm.makeCurrentIntoDraft());

        TemporalUtils.makeHistorical(currentVocabulary, nowTime());
        currentVocabulary.setModifiedBy(modifiedBy());
        VocabularyDAO.updateVocabulary(em(), currentVocabulary);
        // Add a registry event for deletion of the current row.
        RegistryEventUtils.createRegistryEvent(
                em(), RegistryEventElementType.VOCABULARIES,
                vocabularyId(), nowTime(),
                RegistryEventEventType.DELETED, modifiedBy(),
                currentVocabulary, null);
        // Now make a new draft record.
        draftVocabulary = VocabularyClone.INSTANCE.
                clone(currentVocabulary);
        // For Vocabulary instances, we represent draft status not only
        // in the start_date/end_date columns, but also in the
        // status field.
        draftVocabulary.setStatus(VocabularyStatus.DRAFT);
        draftVocabulary.setModifiedBy(modifiedBy());
        TemporalUtils.makeDraft(draftVocabulary);
        VocabularyDAO.saveVocabulary(em(), draftVocabulary);
        // Add a registry event for creation of the draft row.
        RegistryEventUtils.createRegistryEvent(
                em(), RegistryEventElementType.VOCABULARIES,
                vocabularyId(), nowTime(),
                RegistryEventEventType.CREATED, modifiedBy(),
                null, draftVocabulary);
        currentVocabulary = null;
    }

    /** {@inheritDoc} */
    @Override
    protected void deleteOnlyDraft() {
        if (draftVocabulary == null) {
            // Oops, nothing to do!
            return;
        }
        // Sub-models first.
        subModels.forEach(sm -> sm.deleteOnlyDraft());
        deleteDraftDatabaseRows();
    }

    /** {@inheritDoc} */
    @Override
    protected void deleteDraftDatabaseRows() {
        if (draftVocabulary != null) {
            VocabularyDAO.deleteVocabulary(em(), draftVocabulary);
            // Add a registry event.
            RegistryEventUtils.createRegistryEvent(
                    em(), RegistryEventElementType.VOCABULARIES,
                    vocabularyId(), nowTime(),
                    RegistryEventEventType.DELETED, modifiedBy(),
                    draftVocabulary, null);
            draftVocabulary = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void applyChanges(
            final au.org.ands.vocabs.registry.schema.vocabulary201701.
            Vocabulary updatedVocabulary) {
        // First this model, so that the Vocabulary entity is available
        // to sub-models.
        prepareUpdatedVocabulary(updatedVocabulary);
        VocabularyStatus status = updatedVocabulary.getStatus();
        if (status == VocabularyStatus.DRAFT) {
            applyChangesDraft(updatedVocabulary);
        } else {
            applyChangesCurrent(updatedVocabulary);
        }
        // Now the Vocabulary entity is ready to be accessed by sub-models,
        // in case they need it to make a TaskInfo object.
        subModels.forEach(sm ->
            sm.applyChanges(updatedVocabulary));

        // And now do any updating of any other subsystems that need
        // to "know" about new/updated vocabulary metadata.

        // Subscription/notification subsystem.
        Owners.requireOwner(updatedVocabulary.getOwner());
    }

    /** Incoming vocabulary metadata in registry schema may be incomplete.
     * In particular, there may be slugs missing from top-level
     * vocabulary and version metadata.
     * Prepare the incoming data to be used by the model, by
     * making the necessary adjustments.
     * @param updatedVocabulary The vocabulary metada in registry schema
     *      format, that may need adjustment before it can be used.
     */
    private void prepareUpdatedVocabulary(
            final au.org.ands.vocabs.registry.schema.vocabulary201701.
            Vocabulary updatedVocabulary) {
        // Create slugs, where needed.
        if (updatedVocabulary.getSlug() == null) {
            updatedVocabulary.setSlug(SlugGenerator.generateSlug(
                    updatedVocabulary.getTitle()));
        }
        for (au.org.ands.vocabs.registry.schema.vocabulary201701.Version
                version : updatedVocabulary.getVersion()) {
            if (version.getSlug() == null) {
                version.setSlug(SlugGenerator.generateSlug(
                        version.getTitle()));
            }
        }
    }

    /** Make the database's draft view of the
     * Vocabulary match updatedVocabulary.
     * @param updatedVocabulary The updated vocabulary.
     */
    private void applyChangesDraft(
            final au.org.ands.vocabs.registry.schema.vocabulary201701.
            Vocabulary updatedVocabulary) {
        VocabularyRegistrySchemaMapper mapper =
                VocabularyRegistrySchemaMapper.INSTANCE;
        if (draftVocabulary != null) {
            // Already a draft. Update it.
            mapper.updateTargetFromSource(
                    updatedVocabulary, draftVocabulary, nowTime());
            draftVocabulary.setModifiedBy(modifiedBy());
            VocabularyDAO.updateVocabulary(em(), draftVocabulary);
            // Add a registry event.
            RegistryEventUtils.createRegistryEvent(
                    em(), RegistryEventElementType.VOCABULARIES,
                    vocabularyId(), nowTime(),
                    RegistryEventEventType.UPDATED, modifiedBy(),
                    draftVocabulary, draftVocabulary);
        } else {
            // Add a new draft record.
            draftVocabulary = mapper.sourceToTarget(updatedVocabulary,
                    nowTime());
            // updatedVocabulary doesn't have the vocabulary Id.
            draftVocabulary.setVocabularyId(vocabularyId());
            TemporalUtils.makeDraft(draftVocabulary);
            draftVocabulary.setModifiedBy(modifiedBy());
            VocabularyDAO.saveVocabulary(em(), draftVocabulary);
            // Add a registry event.
            RegistryEventUtils.createRegistryEvent(
                    em(), RegistryEventElementType.VOCABULARIES,
                    vocabularyId(), nowTime(),
                    RegistryEventEventType.CREATED, modifiedBy(),
                    null, draftVocabulary);
        }
    }

    /** Make the database's currently-valid view of the
     * Vocabulary match updatedVocabulary.
     * @param updatedVocabulary The updated vocabulary.
     */
    private void applyChangesCurrent(
            final au.org.ands.vocabs.registry.schema.vocabulary201701.
            Vocabulary updatedVocabulary) {
        // Registry event type, used to distinguish creation and update;
        // used as the type of registry event to generate for the new/updated
        // current instance.
        // I changed my mind about the expression used to determine
        // whether or not it is CREATED. I started with:
//            (currentVocabulary == null) && (draftVocabulary == null);
        // but now it is the following. With the previous expression,
        // if you started with a draft before publishing it, you wouldn't
        // see a creation event.
        RegistryEventEventType reeType;
        if (currentVocabulary == null) {
            reeType = RegistryEventEventType.CREATED;
        } else {
            reeType = RegistryEventEventType.UPDATED;
            TemporalUtils.makeHistorical(currentVocabulary, nowTime());
            currentVocabulary.setModifiedBy(modifiedBy());
            VocabularyDAO.updateVocabulary(em(), currentVocabulary);
        }
        // We now say goodbye to the old value of currentVocabulary.
        // If draftVocabulary is non-null, it will then be re-purposed as
        // the row for the new/updated current instance.
        // Hmm, because of the introduction of createRegistryEvent(),
        // we do now need to keep the old value of currentVocabulary
        // around, so that it can be used, if needed, to provide
        // the (surrogate key) id of the old Vocabulary row.
        Vocabulary oldCurrentVocabulary = currentVocabulary;
        currentVocabulary = draftVocabulary;
        draftVocabulary = null;
        VocabularyRegistrySchemaMapper mapper =
                VocabularyRegistrySchemaMapper.INSTANCE;
        if (currentVocabulary != null) {
            // There was already a draft row.
            // Reuse it as the new current instance.
            // Add a registry event for deletion of the draft.
            RegistryEventUtils.createRegistryEvent(
                    em(), RegistryEventElementType.VOCABULARIES,
                    vocabularyId(), nowTime(),
                    RegistryEventEventType.DELETED, modifiedBy(),
                    currentVocabulary, null);
            mapper.updateTargetFromSource(
                    updatedVocabulary, currentVocabulary);
            currentVocabulary.setStartDate(nowTime());
            TemporalUtils.makeCurrentlyValid(currentVocabulary);
            currentVocabulary.setModifiedBy(modifiedBy());
            VocabularyDAO.updateVocabulary(em(), currentVocabulary);
        } else {
            // No existing row to use for new/updated current instance.
            // Make one.
            currentVocabulary = mapper.sourceToTarget(updatedVocabulary);
            // updatedVocabulary doesn't have the vocabulary Id.
            currentVocabulary.setVocabularyId(vocabularyId());
            currentVocabulary.setStartDate(nowTime());
            TemporalUtils.makeCurrentlyValid(currentVocabulary);
            currentVocabulary.setModifiedBy(modifiedBy());
            VocabularyDAO.saveVocabulary(em(), currentVocabulary);
        }

        // Join paths, for the registry event.
        // Add a registry event for creation/upate of the current instance.
        // Either reeType == UPDATED, and oldCurrentVocabulary != null,
        // or     reeType == CREATED, and oldCurrentVocabulary == null.
        RegistryEventUtils.createRegistryEvent(
                em(), RegistryEventElementType.VOCABULARIES,
                vocabularyId(), nowTime(),
                reeType, modifiedBy(),
                oldCurrentVocabulary, currentVocabulary);
    }

}
