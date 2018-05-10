/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.model;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.commons.collections4.sequence.CommandVisitor;
import org.apache.commons.collections4.sequence.SequencesComparator;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.converter.VersionRegistrySchemaMapper;
import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.converter.VersionDbSchemaMapper;
import au.org.ands.vocabs.registry.db.dao.VersionDAO;
import au.org.ands.vocabs.registry.db.entity.ComparisonUtils;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.db.entity.clone.VersionClone;
import au.org.ands.vocabs.registry.db.internal.VersionJson;
import au.org.ands.vocabs.registry.enums.RegistryEventElementType;
import au.org.ands.vocabs.registry.enums.RegistryEventEventType;
import au.org.ands.vocabs.registry.enums.SubtaskOperationType;
import au.org.ands.vocabs.registry.enums.SubtaskProviderType;
import au.org.ands.vocabs.registry.enums.TaskStatus;
import au.org.ands.vocabs.registry.enums.VocabularyStatus;
import au.org.ands.vocabs.registry.log.RegistryEventUtils;
import au.org.ands.vocabs.registry.model.sequence.VersionElement;
import au.org.ands.vocabs.registry.schema.vocabulary201701.WorkflowOutcome;
import au.org.ands.vocabs.registry.workflow.WorkflowMethods;
import au.org.ands.vocabs.registry.workflow.converter.WorkflowOutcomeSchemaMapper;
import au.org.ands.vocabs.registry.workflow.provider.harvest.PoolPartyHarvestProvider;
import au.org.ands.vocabs.registry.workflow.provider.importer.SesameImporterProvider;
import au.org.ands.vocabs.registry.workflow.provider.publish.SISSVocPublishProvider;
import au.org.ands.vocabs.registry.workflow.provider.transform.ResourceMapTransformProvider;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;
import au.org.ands.vocabs.registry.workflow.tasks.Task;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;

/** Versions domain model.
 * This is a representation of the versions of a vocabulary,
 * as an abstract data type.
 */
public class VersionsModel extends ModelBase {

    /* This part of the model consists of the following
     * temporally-determined entities:
     *
     *   Version
     *     VersionArtefact
     *     AccessPoint
     */

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** The parent VocabularyModel of this instance. Passed in to
     * this class's constructor, and passed down to constructors
     * of sub-models. */
    private VocabularyModel vocabularyModel;

    /** The current instances of versions, if there are any.
     * The keys are version Ids. */
    private Map<Integer, Version> currentVersions = new HashMap<>();

    /** The draft instances of versions, if there are any.
     * The keys are version Ids. */
    private Map<Integer, Version> draftVersions = new HashMap<>();

    /** Used by {@link
     * #applyChanges(au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary)}
     * to keep track of whether a version has both
     * the doImport and doPublish flags set. This is then used by
     * {@link #addImpliedSubtasks()} to determine
     * whether to add or delete additional workflow subtasks. */
    private Map<Integer, Boolean> versionHasImportAndPublish = new HashMap<>();

    /** The model of the AccessPoints. */
    private AccessPointsModel apModel;

    /** The model of the VersionArtefacts. */
    private VersionArtefactsModel vaModel;

    /** List of all sub-models. */
    private List<ModelBase> subModels = new ArrayList<>();

    /** The TaskInfo objects containing the tasks to be performed
     * for versions, if there are any.
     * The keys are version Ids. */
    private Map<Integer, TaskInfo> versionTaskInfos = new HashMap<>();

    /** The tasks to be performed for versions, if there are any.
     * The keys are version Ids. */
    private Map<Integer, Task> versionTasks = new HashMap<>();

    /** Flag used to keep track of whether a version was updated during
     * {@link
     * #applyChanges(au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary)}.
     * Used to determine whether or not to invoke {@link #populateSubmodels()}.
     */
    private boolean versionsUpdated;

    /** Construct versions model for a vocabulary.
     * @param anEm The EntityManager to be used to fetch and update
     *      database data.
     * @param aVocabularyId The Id of the vocabulary for which the model
     *      is to be constructed.
     * @param aVocabularyModel The parent VocabularyModel of this instance.
     * @throws IllegalArgumentException If aVocabularyId is null.
     */
    public VersionsModel(final EntityManager anEm,
            final Integer aVocabularyId,
            final VocabularyModel aVocabularyModel) {
        if (aVocabularyId == null) {
            logger.error("Attempt to construct versions model with no Id");
            throw new IllegalArgumentException(
                    "Attempt to construct versions model with no Id");
        }
        setEm(anEm);
        setVocabularyId(aVocabularyId);
        vocabularyModel = aVocabularyModel;
        populateModel();
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
        List<Version> versionList =
                VersionDAO.getCurrentVersionListForVocabulary(
                        em(), vocabularyId());
        for (Version version : versionList) {
            currentVersions.put(version.getVersionId(), version);
        }

        // Draft
        versionList =
                VersionDAO.getDraftVersionListForVocabulary(
                        em(), vocabularyId());
        for (Version version : versionList) {
            draftVersions.put(version.getVersionId(), version);
        }

        // Sub-models
        populateSubmodels();
    }

    /** Populate the sub-models. Invoke this method inside
     * {@link #populateModel()}, and when it is desired to "refresh"
     * the sub-models, e.g., after running tasks.
     */
    private void populateSubmodels() {
        subModels.clear();
        apModel = new AccessPointsModel(em(), vocabularyId(),
                vocabularyModel, this, currentVersions, draftVersions);
        subModels.add(apModel);
        vaModel = new VersionArtefactsModel(em(), vocabularyId(),
                vocabularyModel, this, currentVersions, draftVersions);
        subModels.add(vaModel);
        notifySetNowTime(nowTime());
        notifySetModifiedBy(modifiedBy());
    }

    /** {@inheritDoc} */
    @Override
    public List<String> describeModel() {
        List<String> description = new ArrayList<>();
        if (vocabularyId() == null) {
            description.add("Versions | No vocabulary Id");
            return description;
        }
        description.add("Versions | Vocabulary; Id: " + vocabularyId());
        if (!currentVersions.isEmpty()) {
            for (Version version : currentVersions.values()) {
                description.add("Versions | Current version; "
                        + "Id, Version Id: "
                        + version.getId() + ","
                        + version.getVersionId());
            }
        }
        if (!draftVersions.isEmpty()) {
            for (Version version : draftVersions.values()) {
                description.add("Versions | Draft version; "
                        + "Id, Version Id: "
                        + version.getId() + ","
                        + version.getVersionId());
                }
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

    /** {@inheritDoc} */
    @Override
    protected void insertIntoSchemaFromCurrent(final
            au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
            outputVocabulary,
            final boolean includeVersions,
            final boolean includeAccessPoints,
            final boolean includeRelatedEntitiesAndVocabularies) {
        // Don't go any further if we are not going to be adding
        // anything.
        // NB: if includeAccessPoints == true, this will
        // override the case that includeVersions == false.
        if (!(includeVersions || includeAccessPoints)
                || currentVersions.isEmpty()) {
            return;
        }

        List<au.org.ands.vocabs.registry.schema.vocabulary201701.Version>
        outputVersions = outputVocabulary.getVersion();
        au.org.ands.vocabs.registry.schema.vocabulary201701.Version
        outputVersion;

        VersionDbSchemaMapper mapper = VersionDbSchemaMapper.INSTANCE;

        for (Version version : currentVersions.values()) {
            outputVersion = mapper.sourceToTarget(version);
            outputVersions.add(outputVersion);
        }
        // Sub-models.
        subModels.forEach(sm -> sm.insertIntoSchemaFromCurrent(outputVocabulary,
                includeVersions, includeAccessPoints,
                includeRelatedEntitiesAndVocabularies));
    }

    /** {@inheritDoc} */
    @Override
    protected void insertIntoSchemaFromDraft(final
            au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
            outputVocabulary,
            final boolean includeVersions,
            final boolean includeAccessPoints,
            final boolean includeRelatedEntitiesAndVocabularies) {
        // Don't go any further if we are not going to be adding
        // anything.
        // NB: if includeAccessPoints == true, this will
        // override the case that includeVersions == false.
        if (!(includeVersions || includeAccessPoints)
                || draftVersions.isEmpty()) {
            return;
        }

        List<au.org.ands.vocabs.registry.schema.vocabulary201701.Version>
        outputVersions = outputVocabulary.getVersion();
        au.org.ands.vocabs.registry.schema.vocabulary201701.Version
        outputVersion;

        VersionDbSchemaMapper mapper = VersionDbSchemaMapper.INSTANCE;

        for (Version version : draftVersions.values()) {
            outputVersion = mapper.sourceToTarget(version);
            outputVersions.add(outputVersion);
        }
        // Sub-models.
        subModels.forEach(sm -> sm.insertIntoSchemaFromDraft(outputVocabulary,
                includeVersions, includeAccessPoints,
                includeRelatedEntitiesAndVocabularies));
    }

    /** {@inheritDoc} */
    @Override
    protected void deleteOnlyCurrent() {
        if (currentVersions.isEmpty()) {
            // Oops, nothing to do!
            return;
        }
        // Sub-models first.
        subModels.forEach(sm -> sm.deleteOnlyCurrent());

        for (Version version : currentVersions.values()) {
            TemporalUtils.makeHistorical(version, nowTime());
            version.setModifiedBy(modifiedBy());
            VersionDAO.updateVersion(em(), version);
            // Add a registry event.
            RegistryEventUtils.createRegistryEvent(
                    em(), RegistryEventElementType.VERSIONS,
                    version.getVersionId(), nowTime(),
                    RegistryEventEventType.DELETED, modifiedBy(),
                    version, null);
        }
        // TO DO: workflow processing is done here, but need to confirm
        // if this is the right place/way to do it.
        // And now run any tasks that have been accumulated along the way.
        addImpliedSubtasks();
        boolean ranATask = processRequiredTasks();
        // Make this model correct again first ...
        currentVersions.clear();
        // ... now we can repopulate sub-models, if we need to.
        if (ranATask) {
            // If we ran at least one task, repopulate the sub-models
            // so that they are up-to-date again.
            populateSubmodels();
            constructWorkflowOutcome();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void makeCurrentIntoDraft() {
        if (currentVersions.isEmpty()) {
            // Oops, nothing to do!
            return;
        }
        if (!draftVersions.isEmpty()) {
            // Error
            throw new IllegalArgumentException(
                    "Existing draft; you must delete it first");
        }
        // Sub-models first.
        subModels.forEach(sm -> sm.makeCurrentIntoDraft());

        for (Version version : currentVersions.values()) {
            TemporalUtils.makeHistorical(version, nowTime());
            version.setModifiedBy(modifiedBy());
            VersionDAO.updateVersion(em(), version);
            // Add a registry event for deletion of the current row.
            RegistryEventUtils.createRegistryEvent(
                    em(), RegistryEventElementType.VERSIONS,
                    version.getVersionId(), nowTime(),
                    RegistryEventEventType.DELETED, modifiedBy(),
                    version, null);
            // Now make a new draft record.
            Version draftVersion = VersionClone.INSTANCE.clone(version);
            draftVersion.setModifiedBy(modifiedBy());
            TemporalUtils.makeDraft(draftVersion);
            VersionDAO.saveVersion(em(), draftVersion);
            // Add a registry event.
            RegistryEventUtils.createRegistryEvent(
                    em(), RegistryEventElementType.VERSIONS,
                    version.getVersionId(), nowTime(),
                    RegistryEventEventType.CREATED, modifiedBy(),
                    null, draftVersion);
            draftVersions.put(draftVersion.getVersionId(), draftVersion);
        }
        // TO DO: workflow processing is done here, but need to confirm
        // if this is the right place/way to do it.
        // And now run any tasks that have been accumulated along the way.
        addImpliedSubtasks();
        boolean ranATask = processRequiredTasks();
        // Make this model correct again first ...
        currentVersions.clear();
        // ... now we can repopulate sub-models, if we need to.
        if (ranATask) {
            // If we ran at least one task, repopulate the sub-models
            // so that they are up-to-date again.
            populateSubmodels();
            constructWorkflowOutcome();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void deleteOnlyDraft() {
        if (draftVersions.isEmpty()) {
            // Oops, nothing to do!
            return;
        }
        deleteDraftDatabaseRows();
        // Sub-models.
        subModels.forEach(sm -> sm.deleteOnlyDraft());
    }

    /** {@inheritDoc} */
    @Override
    protected void deleteDraftDatabaseRows() {
        for (Version version : draftVersions.values()) {
            // For now, it is OK just to delete the database rows.
            // In future, if the publication workflow is applied to
            // drafts, more work will have be done here.
            VersionDAO.deleteVersion(em(), version);
            // Add a registry event.
            RegistryEventUtils.createRegistryEvent(
                    em(), RegistryEventElementType.VERSIONS,
                    version.getVersionId(), nowTime(),
                    RegistryEventEventType.DELETED, modifiedBy(),
                    version, null);
        }
        draftVersions.clear();
    }

    /** {@inheritDoc} */
    @Override
    protected void applyChanges(
            final au.org.ands.vocabs.registry.schema.vocabulary201701.
            Vocabulary updatedVocabulary) {
        VocabularyStatus status = updatedVocabulary.getStatus();
        if (status == VocabularyStatus.DRAFT) {
            applyChangesDraft(updatedVocabulary);
        } else {
            applyChangesCurrent(updatedVocabulary);
        }

        if (versionsUpdated) {
            // We may have added a new version, so we need to refresh
            // the sub-models before we delegate to their applyChanges().
            populateSubmodels();
            // Reset, "just in case".
            versionsUpdated = false;
        }
        // Sub-models.
        subModels.forEach(sm ->
            sm.applyChanges(updatedVocabulary));

        // And now run any tasks that have been accumulated along the way.
        addImpliedSubtasks();
        boolean ranATask = processRequiredTasks();
        if (ranATask) {
            // If we ran at least one task, repopulate the sub-models
            // so that they are up-to-date again.
            populateSubmodels();
            constructWorkflowOutcome();
        }
    }

    /** Make the database's draft view of the Versions match updatedVocabulary.
     * @param updatedVocabulary The updated vocabulary.
     */
    private void applyChangesDraft(
            final au.org.ands.vocabs.registry.schema.vocabulary201701.
            Vocabulary updatedVocabulary) {
        // Create sequences.
        // First, any existing draft values. All of these have
        // version Ids.
        List<VersionElement> existingDraftSequence = new ArrayList<>();
        for (Entry<Integer, Version> versionEntry : draftVersions.entrySet()) {
            existingDraftSequence.add(new VersionElement(
                    versionEntry.getKey(), versionEntry.getValue(), null));
        }
        Collections.sort(existingDraftSequence);

        // And now, the updated draft values. Each one may or may
        // not have a version Id; if not, version.getId() is null.
        List<VersionElement> updatedSequence = new ArrayList<>();
        // Also, put the versions that have version Ids into a map,
        // so that they can be found later by visitKeepCommand().
        Map<Integer,
        au.org.ands.vocabs.registry.schema.vocabulary201701.Version>
        updatedVersions = new HashMap<>();
        updatedVocabulary.getVersion().forEach(
                version -> {
                    updatedSequence.add(new VersionElement(
                            version.getId(), null, version));
                    if (version.getId() != null) {
                        updatedVersions.put(version.getId(), version);
                    }
                });
        Collections.sort(updatedSequence);

        // Compute difference.
        final SequencesComparator<VersionElement> comparator =
                new SequencesComparator<>(
                        existingDraftSequence, updatedSequence);
        // Apply the changes.
        comparator.getScript().visit(new UpdateDraftVisitor(updatedVersions));
    }

    /** Visitor class that applies a sequence of updates to the
     * draft view of the database. */
    private class UpdateDraftVisitor
        implements CommandVisitor<VersionElement> {

        /** The map of updated versions. Keys are version Ids; values
         * are the versions in registry schema format. */
        private Map<Integer,
        au.org.ands.vocabs.registry.schema.vocabulary201701.Version>
        updatedVersions;

        /** Constructor that accepts the map of updated versions.
         * @param anUpdatedVersions The map of updated versions.
         */
        UpdateDraftVisitor(final Map<Integer,
                au.org.ands.vocabs.registry.schema.vocabulary201701.Version>
                anUpdatedVersions) {
            updatedVersions = anUpdatedVersions;
        }

        /** {@inheritDoc} */
        @Override
        public void visitKeepCommand(final VersionElement ve) {
            // This could contain metadata updates, so apply them
            // to the existing draft row.
            Integer versionId = ve.getVersionId();
            Version existingVersion = ve.getDbVersion();
            au.org.ands.vocabs.registry.schema.vocabulary201701.Version
            schemaVersion = updatedVersions.get(versionId);

            if (!ComparisonUtils.isEqualVersion(existingVersion,
                    schemaVersion)) {
                // Update existing draft instance with new details.
                VersionRegistrySchemaMapper mapper =
                        VersionRegistrySchemaMapper.INSTANCE;
                mapper.updateTargetFromSource(schemaVersion, existingVersion,
                        nowTime());
                existingVersion.setModifiedBy(modifiedBy());
                VersionDAO.updateVersion(em(), existingVersion);
                // Add a registry event.
                RegistryEventUtils.createRegistryEvent(
                        em(), RegistryEventElementType.VERSIONS,
                        existingVersion.getVersionId(), nowTime(),
                        RegistryEventEventType.UPDATED, modifiedBy(),
                        existingVersion, existingVersion);
                // And that's all, because this is a draft.
            }
        }

        /** {@inheritDoc} */
        @Override
        public void visitDeleteCommand(final VersionElement ve) {
            Version versionToDelete = ve.getDbVersion();
            // Notify submodels first.
            subModels.forEach(sm -> sm.notifyDeleteDraftVersion(
                    versionToDelete.getVersionId()));
            VersionDAO.deleteVersion(em(), versionToDelete);
            // Add a registry event.
            RegistryEventUtils.createRegistryEvent(
                    em(), RegistryEventElementType.VERSIONS,
                    versionToDelete.getVersionId(), nowTime(),
                    RegistryEventEventType.DELETED, modifiedBy(),
                    versionToDelete, null);
            draftVersions.remove(ve.getVersionId());
            // And that's all, since we are updating a draft.
        }

        /** {@inheritDoc} */
        @Override
        public void visitInsertCommand(final VersionElement ve) {
            VersionRegistrySchemaMapper mapper =
                    VersionRegistrySchemaMapper.INSTANCE;
            Integer versionId = ve.getVersionId();
            au.org.ands.vocabs.registry.schema.vocabulary201701.Version
            schemaVersion = ve.getSchemaVersion();
            if (versionId != null) {
                // There's a version Id, so check if it's in the
                // set of current instances.
                if (currentVersions.containsKey(versionId)) {
                    // We don't need to create a new version Id, but
                    // we do need to add a draft row for it.
                    Version newVersion = mapper.sourceToTarget(
                            schemaVersion, nowTime());
                    newVersion.setVocabularyId(vocabularyId());
                    newVersion.setVersionId(versionId);
                    TemporalUtils.makeDraft(newVersion);
                    newVersion.setModifiedBy(modifiedBy());
                    VersionDAO.saveVersion(em(), newVersion);
                    // Add a registry event.
                    RegistryEventUtils.createRegistryEvent(
                            em(), RegistryEventElementType.VERSIONS,
                            versionId, nowTime(),
                            RegistryEventEventType.CREATED, modifiedBy(),
                            null, newVersion);
                    draftVersions.put(versionId, newVersion);
                    versionsUpdated = true;
                } else {
                    // Error: we don't know about this version Id.
                    // (Well, it might be a historical version that
                    // the user wanted to restore, but we don't support that.)
                    throw new IllegalArgumentException(
                            "Attempt to update version that does not "
                            + "belong to this vocabulary; Id = " + versionId);
                }
            } else {
                // This is a new version.
                Version newVersion = mapper.sourceToTarget(
                        ve.getSchemaVersion(), nowTime());
                newVersion.setVocabularyId(vocabularyId());
                TemporalUtils.makeDraft(newVersion);
                newVersion.setModifiedBy(modifiedBy());
                VersionDAO.saveVersionWithId(em(), newVersion);
                Integer newVersionId = newVersion.getVersionId();
                // Add a registry event.
                RegistryEventUtils.createRegistryEvent(
                        em(), RegistryEventElementType.VERSIONS,
                        newVersionId, nowTime(),
                        RegistryEventEventType.CREATED, modifiedBy(),
                        null, newVersion);
                // Future work: uncomment the following, if/when we
                // support workflow for drafts:
//              // And now we can put a value into versionHasImportAndPublish.
//              versionHasImportAndPublish.put(versionId,
//                      BooleanUtils.isTrue(schemaVersion.isDoImport())
//                      && BooleanUtils.isTrue(schemaVersion.isDoPublish()));
                draftVersions.put(newVersionId, newVersion);
                versionsUpdated = true;
                // And this is a tricky bit: we modify the input data
                // so that the version Id can be seen by submodels.
                schemaVersion.setId(newVersionId);
                // And that's all, because this is a draft.
            }
        }
    }

    /** Make the database's currently-valid view of the
     * Versions match updatedVocabulary.
     * @param updatedVocabulary The updated vocabulary.
     */
    private void applyChangesCurrent(
            final au.org.ands.vocabs.registry.schema.vocabulary201701.
            Vocabulary updatedVocabulary) {
        // Create sequences.
        // First, the current values. All of these have
        // version Ids.
        List<VersionElement> currentSequence = new ArrayList<>();
        for (Entry<Integer, Version> versionEntry
                : currentVersions.entrySet()) {
            currentSequence.add(new VersionElement(
                    versionEntry.getKey(), versionEntry.getValue(), null));
        }
        Collections.sort(currentSequence);

        // And now, the updated values. Each one may or may
        // not have a version Id; if not, version.getId() is null.
        List<VersionElement> updatedSequence = new ArrayList<>();
        // Also, put the versions that have version Ids into a map,
        // so that they can be found later by visitKeepCommand().
        Map<Integer,
        au.org.ands.vocabs.registry.schema.vocabulary201701.Version>
        updatedVersions = new HashMap<>();
        versionHasImportAndPublish.clear();
        updatedVocabulary.getVersion().forEach(
                version -> {
                    updatedSequence.add(new VersionElement(
                            version.getId(), null, version));
                    if (version.getId() != null) {
                        updatedVersions.put(version.getId(), version);
                        versionHasImportAndPublish.put(version.getId(),
                                BooleanUtils.isTrue(version.isDoImport())
                                && BooleanUtils.isTrue(version.isDoPublish()));
                    }
                });
        Collections.sort(updatedSequence);

        // Compute difference.
        final SequencesComparator<VersionElement> comparator =
                new SequencesComparator<>(
                        currentSequence, updatedSequence);
        // Apply the changes.
        comparator.getScript().visit(new UpdateCurrentVisitor(updatedVersions));
        // Delete any remaining draft rows.
        deleteDraftDatabaseRows();
    }

    /** Visitor class that applies a sequence of updates to the
     * currently-valid view of the database. */
    private class UpdateCurrentVisitor
        implements CommandVisitor<VersionElement> {

        /** The map of updated versions. Keys are version Ids; values
         * are the versions in registry schema format. */
        private Map<Integer,
        au.org.ands.vocabs.registry.schema.vocabulary201701.Version>
        updatedVersions;

        /** Constructor that accepts the map of updated versions.
         * @param anUpdatedVersions The map of updated versions.
         */
        UpdateCurrentVisitor(final Map<Integer,
                au.org.ands.vocabs.registry.schema.vocabulary201701.Version>
                anUpdatedVersions) {
            updatedVersions = anUpdatedVersions;
        }

        /** {@inheritDoc} */
        @Override
        public void visitKeepCommand(final VersionElement ve) {
            // This could contain metadata updates. If so, make the
            // existing current row historical, and add a new current row.
            Integer versionId = ve.getVersionId();
            Version existingVersion = ve.getDbVersion();
            au.org.ands.vocabs.registry.schema.vocabulary201701.Version
            schemaVersion = updatedVersions.get(versionId);

            if (!ComparisonUtils.isEqualVersion(existingVersion,
                    schemaVersion)
                    || BooleanUtils.isTrue(schemaVersion.isForceWorkflow())) {
                TemporalUtils.makeHistorical(existingVersion, nowTime());
                existingVersion.setModifiedBy(modifiedBy());
                VersionDAO.updateVersion(em(), existingVersion);
                // Make new current instance with updated details.
                VersionRegistrySchemaMapper mapper =
                        VersionRegistrySchemaMapper.INSTANCE;
                Version newCurrentVersion = mapper.sourceToTarget(
                        schemaVersion);
                newCurrentVersion.setVocabularyId(vocabularyId());
                TemporalUtils.makeCurrentlyValid(newCurrentVersion, nowTime());
                newCurrentVersion.setModifiedBy(modifiedBy());
                VersionDAO.saveVersion(em(), newCurrentVersion);
                // Add a registry event.
                RegistryEventUtils.createRegistryEvent(
                        em(), RegistryEventElementType.VERSIONS,
                        versionId, nowTime(),
                        RegistryEventEventType.UPDATED, modifiedBy(),
                        existingVersion, newCurrentVersion);
                // Update our records (i.e., in this case, overwriting
                // the previous value).
                currentVersions.put(versionId, newCurrentVersion);
                versionsUpdated = true;
                workflowRequired(vocabularyModel.getCurrentVocabulary(),
                        newCurrentVersion);
                Task task = getTaskForVersion(versionId);
                // Now do the following:
                // Compare the before/after settings of the various
                // doXYZ flags.
                VersionJson existingVersionJson =
                        JSONSerialization.deserializeStringAsJson(
                                existingVersion.getData(), VersionJson.class);
                VersionJson newCurrentVersionJson =
                        JSONSerialization.deserializeStringAsJson(
                                newCurrentVersion.getData(), VersionJson.class);
                // Sorry for the spaghetti.
                // The structure of each of these is:
                //   Is workflow forced, or did the value of the flag change?
                //   If yes, then:
                //     Is the flag now set to true? If so, do an INSERT.
                //     Otherwise (it is now false): do a DELETE.
                // The "tricky" bit is managing any follow-on. For now,
                // means that a (re-)harvest can also force a (re-)import.
                if (BooleanUtils.isTrue(schemaVersion.isForceWorkflow())
                        || (changedBoolean(
                                newCurrentVersionJson.isDoPoolpartyHarvest(),
                                existingVersionJson.isDoPoolpartyHarvest()))) {
                    if (BooleanUtils.isTrue(
                            newCurrentVersionJson.isDoPoolpartyHarvest())) {
                        task.addSubtask(WorkflowMethods.
                                createHarvestPoolPartySubtask(
                                        SubtaskOperationType.INSERT,
                                        vocabularyModel.
                                        getCurrentVocabulary()));
                        // And also do a (re-)import and metadata re-insertion,
                        // if doImport is set.
                        if (BooleanUtils.isTrue(
                                newCurrentVersionJson.isDoImport())) {
                            task.addSubtask(WorkflowMethods.
                                    createImporterSesameSubtask(
                                            SubtaskOperationType.INSERT));
                            task.addSubtask(WorkflowMethods.
                                    createSesameInsertMetadataSubtask(
                                            SubtaskOperationType.PERFORM));
                        }
                    } else {
                        // If this is not even a PoolParty project, this
                        // will schedule a subtask which does nothing.
                        task.addSubtask(WorkflowMethods.
                                createHarvestPoolPartySubtask(
                                        SubtaskOperationType.DELETE,
                                        vocabularyModel.
                                        getCurrentVocabulary()));
                    }
                }
                if (BooleanUtils.isTrue(schemaVersion.isForceWorkflow())
                        || (changedBoolean(
                                newCurrentVersionJson.isDoImport(),
                                existingVersionJson.isDoImport()))) {
                    if (BooleanUtils.isTrue(
                            newCurrentVersionJson.isDoImport())) {
                        task.addSubtask(WorkflowMethods.
                                createImporterSesameSubtask(
                                        SubtaskOperationType.INSERT));
                    } else {
                        task.addSubtask(WorkflowMethods.
                                createImporterSesameSubtask(
                                        SubtaskOperationType.DELETE));
                    }
                }
                if (BooleanUtils.isTrue(schemaVersion.isForceWorkflow())
                        || (changedBoolean(
                                newCurrentVersionJson.isDoPublish(),
                                existingVersionJson.isDoPublish()))) {
                    if (BooleanUtils.isTrue(
                            newCurrentVersionJson.isDoPublish())) {
                        task.addSubtask(WorkflowMethods.
                                createPublishSissvocSubtask(
                                        SubtaskOperationType.INSERT));
                    } else {
                        task.addSubtask(WorkflowMethods.
                                createPublishSissvocSubtask(
                                        SubtaskOperationType.DELETE));
                    }
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public void visitDeleteCommand(final VersionElement ve) {
            Version versionToDelete = ve.getDbVersion();
            // Notify submodels first.
            subModels.forEach(sm -> sm.notifyDeleteCurrentVersion(
                    versionToDelete.getVersionId()));
            // Make the existing row historical.
            TemporalUtils.makeHistorical(versionToDelete, nowTime());
            versionToDelete.setModifiedBy(modifiedBy());
            VersionDAO.updateVersion(em(), versionToDelete);
            Integer versionId = ve.getVersionId();
            currentVersions.remove(versionId);
            // (I think) we don't need to do anything about workflow
            // processing here, as it is all handled by the two sub-models.
            // E.g., we don't need to examine the various doXYZ flags here, as
            // AccessPointsModel takes care of creating the necessary
            // DELETE subtasks.

            // Add a registry event.
            RegistryEventUtils.createRegistryEvent(
                    em(), RegistryEventElementType.VERSIONS,
                    versionId, nowTime(),
                    RegistryEventEventType.DELETED, modifiedBy(),
                    versionToDelete, null);
        }

        /** {@inheritDoc} */
        @Override
        public void visitInsertCommand(final VersionElement ve) {
            VersionRegistrySchemaMapper mapper =
                    VersionRegistrySchemaMapper.INSTANCE;
            Integer versionId = ve.getVersionId();
            au.org.ands.vocabs.registry.schema.vocabulary201701.Version
            schemaVersion = ve.getSchemaVersion();
            if (versionId != null) {
                // There's a version Id, so check if it's in the
                // set of draft instances.
                if (draftVersions.containsKey(versionId)) {
                    // Reuse this draft row, making it no longer a draft.
                    // Oops, be very aware that this causes a temporary
                    // inconsistency between this model and sub-models,
                    // and it gets worse when we return to applyChanges()
                    // and dispose of the current instances of sub-models
                    // by invoking populateSubmodels(): there are then
                    // "orphan" database rows (for now, just AccessPoints).
                    // So see the last part of
                    // AccessPointsMode.populateModel() to see how this
                    // inconsistency is dealt with.
                    Version existingDraft = draftVersions.remove(versionId);
                    // Add a registry event representing deletion of the draft.
                    RegistryEventUtils.createRegistryEvent(
                            em(), RegistryEventElementType.VERSIONS,
                            versionId, nowTime(),
                            RegistryEventEventType.DELETED, modifiedBy(),
                            existingDraft, null);

                    mapper.updateTargetFromSource(schemaVersion,
                            existingDraft);
                    TemporalUtils.makeCurrentlyValid(existingDraft, nowTime());
                    existingDraft.setModifiedBy(modifiedBy());
                    VersionDAO.updateVersion(em(), existingDraft);
                    // Add a registry event representing creation of the
                    // current instance.
                    RegistryEventUtils.createRegistryEvent(
                            em(), RegistryEventElementType.VERSIONS,
                            versionId, nowTime(),
                            RegistryEventEventType.CREATED, modifiedBy(),
                            null, existingDraft);
                    currentVersions.put(versionId, existingDraft);
                    versionsUpdated = true;
                    workflowRequired(vocabularyModel.getCurrentVocabulary(),
                            existingDraft);
                } else {
                    // Error: we don't know about this version Id.
                    // (Well, it might be a historical version that
                    // the user wanted to restore, but we don't support that.)
                    throw new IllegalArgumentException(
                            "Attempt to update version that does not "
                            + "belong to this vocabulary");
                }
            } else {
                // New row required.
                Version newCurrentVersion = mapper.sourceToTarget(
                        schemaVersion);
                newCurrentVersion.setVocabularyId(vocabularyId());
                TemporalUtils.makeCurrentlyValid(newCurrentVersion, nowTime());
                newCurrentVersion.setModifiedBy(modifiedBy());
                VersionDAO.saveVersionWithId(em(), newCurrentVersion);
                Integer newVersionId = newCurrentVersion.getVersionId();
                // Add a registry event.
                RegistryEventUtils.createRegistryEvent(
                        em(), RegistryEventElementType.VERSIONS,
                        newVersionId, nowTime(),
                        RegistryEventEventType.CREATED, modifiedBy(),
                        null, newCurrentVersion);
                // Update our records (i.e., in this case, adding
                // a new entry).
                versionId = newVersionId;
                // And now we can put a value into versionHasImportAndPublish.
                versionHasImportAndPublish.put(versionId,
                        BooleanUtils.isTrue(schemaVersion.isDoImport())
                        && BooleanUtils.isTrue(schemaVersion.isDoPublish()));

                currentVersions.put(newVersionId, newCurrentVersion);
                versionsUpdated = true;
                // And this is a tricky bit: we modify the input data
                // so that the version Id can be seen by submodels.
                schemaVersion.setId(newVersionId);
                workflowRequired(vocabularyModel.getCurrentVocabulary(),
                        newCurrentVersion);
            }
            // Join paths: what follows applies both when reusing
            // an existing database row and when adding a new database row.
            // NB: _either_ way we got here, workflowRequired() has already
            // been invoked.
            Task task = getTaskForVersion(versionId);
            // Apply the settings for the three version-level flags.
            if (BooleanUtils.isTrue(schemaVersion.isDoPoolpartyHarvest())) {
                task.addSubtask(WorkflowMethods.createHarvestPoolPartySubtask(
                        SubtaskOperationType.INSERT,
                        vocabularyModel.getCurrentVocabulary()));
            }
            if (BooleanUtils.isTrue(schemaVersion.isDoImport())) {
                task.addSubtask(WorkflowMethods.createImporterSesameSubtask(
                        SubtaskOperationType.INSERT));
            }
            // And if doing _both_ a PoolParty harvest and a Sesame import,
            // also do metadata insertion.
            if (BooleanUtils.isTrue(schemaVersion.isDoPoolpartyHarvest())
                    && BooleanUtils.isTrue(schemaVersion.isDoImport())) {
                task.addSubtask(WorkflowMethods.
                        createSesameInsertMetadataSubtask(
                                SubtaskOperationType.PERFORM));
            }
            if (BooleanUtils.isTrue(ve.getSchemaVersion().isDoPublish())) {
                task.addSubtask(WorkflowMethods.createPublishSissvocSubtask(
                        SubtaskOperationType.INSERT));
            }
        }
    }

    /** Get the workflow TaskInfo associated with a version, if there is one.
     * @param versionId The version Id of the version.
     * @return The TaskInfo instance associated with the version, or null,
     *      if there is not (yet) such an instance.
     */
    protected TaskInfo getTaskInfoForVersion(final Integer versionId) {
        return versionTaskInfos.get(versionId);
    }

    /** Get the workflow task associated with a version, if there is one.
     * @param versionId The version Id of the version.
     * @return The Task instance associated with the version, or null,
     *      if there is not (yet) such an instance.
     */
    protected Task getTaskForVersion(final Integer versionId) {
        return versionTasks.get(versionId);
    }

    /** Mark a version as requiring workflow processing. This associates
     * new TaskInfo and Task instances to the version, if there are not already
     * such instances assigned.
     * @param vocabulary The vocabulary instance to be marked as requiring
     *      workflow processing.
     * @param version The version instance to be marked as requiring
     *      workflow processing.
     */
    protected void workflowRequired(final Vocabulary vocabulary,
            final Version version) {
        Integer versionId = version.getVersionId();
        TaskInfo taskInfo = versionTaskInfos.get(versionId);
        if (taskInfo == null) {
            Task task = new Task();
            task.setVocabularyId(vocabularyId());
            task.setVersionId(versionId);
            taskInfo = new TaskInfo(task, vocabulary, version);
            taskInfo.setModifiedBy(modifiedBy());
            taskInfo.setNowTime(nowTime());
            versionTasks.put(versionId, task);
            versionTaskInfos.put(versionId, taskInfo);
        }
    }

    /** To any existing Tasks, add subtasks that are implied by
     * the subtasks already present. For now, that means adding
     * subtasks for the JsonList, JsonTree, and ResourceMapTransform providers.
     */
    private void addImpliedSubtasks() {
        for (Task task : versionTasks.values()) {
            boolean hasHarvestInsert = false;
            boolean hasImportInsert = false;
            boolean hasPublishInsert = false;
            boolean hasHarvestDelete = false;
            boolean hasImportDelete = false;
            boolean hasPublishDelete = false;
            for (Subtask subtask : task.getSubtasks()) {
                if (subtask.getProviderClass().equals(
                        PoolPartyHarvestProvider.class)) {
                    if (subtask.getOperation() == SubtaskOperationType.DELETE) {
                        hasHarvestDelete = true;
                    } else {
                        hasHarvestInsert = true;
                    }
                }
                if (subtask.getProviderClass().equals(
                        SesameImporterProvider.class)) {
                    if (subtask.getOperation() == SubtaskOperationType.DELETE) {
                        hasImportDelete = true;
                    } else {
                        hasImportInsert = true;
                    }
                }
                if (subtask.getProviderClass().equals(
                        SISSVocPublishProvider.class)) {
                    if (subtask.getOperation() == SubtaskOperationType.DELETE) {
                        hasPublishDelete = true;
                    } else {
                        hasPublishInsert = true;
                    }
                }
            }
            // Revisit the following conditional after we've had some
            // experience of the behaviour!
            // The idea is: for now, if there's any change to harvesting,
            // we redo the concept transforms. We rely on the fact
            // that both of the concept transforms do an "untransform"
            // in the case that there's no vocabulary data.
            // This has the acceptable/desirable side-effect of adding
            // these concept transforms in the
            // case of "force workflow", because force workflow always adds
            // either a PoolParty harvest or an unharvest!
            if (hasHarvestDelete || hasHarvestInsert) {
                List<Subtask> conceptTransformSubtasks = new ArrayList<>();
                WorkflowMethods.addConceptTransformSubtasks(
                        conceptTransformSubtasks);
                task.addSubtasks(conceptTransformSubtasks);
            }
            // We would "normally" require _both_ hasImportInsert and
            // hasPublishInsert to justify running the transform.
            // By checking versionHasImportAndPublish, we handle the cases
            // where one of the required INSERT operations was done by an
            // earlier API call, and we're now seeing the other one.
            if (BooleanUtils.isTrue(versionHasImportAndPublish.get(
                    task.getVersionId()))
                    && (hasImportInsert || hasPublishInsert)) {
                task.addSubtask(new Subtask(SubtaskProviderType.TRANSFORM,
                        SubtaskOperationType.PERFORM,
                        ResourceMapTransformProvider.class));
                continue;
            }
            if (hasImportDelete || hasPublishDelete) {
                task.addSubtask(new Subtask(SubtaskProviderType.TRANSFORM,
                        SubtaskOperationType.DELETE,
                        ResourceMapTransformProvider.class));
            }
        }
    }

    /** Process all of the tasks that have been accumulated.
     * @return True, if at least one task was run.
     */
    private boolean processRequiredTasks() {
        boolean ranATask = false;
        // First, persist all.
        for (TaskInfo taskInfo : versionTaskInfos.values()) {
            // Only do something if there is at least one subtask!
            if (!taskInfo.getTask().getSubtasks().isEmpty()) {
                taskInfo.setEm(em());
                taskInfo.setNowTime(nowTime());
                taskInfo.setModifiedBy(modifiedBy());
                taskInfo.persist();
            }
        }
        // Then process all.
        for (TaskInfo taskInfo : versionTaskInfos.values()) {
            // Only do something if there is at least one subtask!
            if (!taskInfo.getTask().getSubtasks().isEmpty()) {
                taskInfo.process();
                ranATask = true;
            }
        }
        return ranATask;
    }

    /** Construct a workflow-outcome element to return, if there
     * was a task that did not complete successfully.
     */
    private void constructWorkflowOutcome() {
        List<TaskInfo> taskInfos = versionTaskInfos.values().stream().
            filter(taskInfo ->
                taskInfo.getTask().getStatus() != TaskStatus.SUCCESS).
            collect(Collectors.toList());
        if (!taskInfos.isEmpty()) {
            WorkflowOutcomeSchemaMapper mapper =
                    WorkflowOutcomeSchemaMapper.INSTANCE;
            WorkflowOutcome workflowOutcome = mapper.sourceToTarget(taskInfos);
            vocabularyModel.setWorkflowOutcome(workflowOutcome);
            logger.info("workflowOutcome: "
                    + JSONSerialization.serializeObjectAsJsonString(
                            workflowOutcome));
        }
    }

    /** Compare two Boolean values, returning true if their canonical
     * representations are different.
     * For the purposes of this method, the canonical representation
     * of null is null; otherwise, the canonical representation of
     * a non-null value is either Boolean.TRUE or Boolean.FALSE.
     * The purpose of this method is to support a case where (somehow)
     * one of the libraries we rely on creates a Boolean object that
     * is not one of the two instances Boolean.TRUE or Boolean.FALSE.
     * @param b1 One of the Boolean values to be compared; it may be null.
     * @param b2 The other Boolean value to be compared; it may be null.
     * @return True, iff the canonicalized values of b1 and b2 are different.
     */
    public boolean changedBoolean(final Boolean b1, final Boolean b2) {
        return BooleanUtils.toInteger(b1, 1, 0, 2)
                != BooleanUtils.toInteger(b2, 1, 0, 2);
    }

}
