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

import javax.persistence.EntityManager;

import org.apache.commons.collections4.sequence.CommandVisitor;
import org.apache.commons.collections4.sequence.SequencesComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.converter.VersionRegistrySchemaMapper;
import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.converter.VersionDbSchemaMapper;
import au.org.ands.vocabs.registry.db.dao.VersionDAO;
import au.org.ands.vocabs.registry.db.entity.ComparisonUtils;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.db.entity.clone.VersionClone;
import au.org.ands.vocabs.registry.enums.SubtaskOperationType;
import au.org.ands.vocabs.registry.enums.VocabularyStatus;
import au.org.ands.vocabs.registry.model.sequence.VersionElement;
import au.org.ands.vocabs.registry.workflow.WorkflowMethods;
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

    /** The parent VocabularyModel of this instance. Passed down
     * by VersionsModel. */
    private VocabularyModel vocabularyModel;

    /** The current instances of versions, if there are any.
     * The keys are version Ids. */
    private Map<Integer, Version> currentVersions = new HashMap<>();

    /** The draft instances of versions, if there are any.
     * The keys are version Ids. */
    private Map<Integer, Version> draftVersions = new HashMap<>();

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
        apModel = new AccessPointsModel(em(), vocabularyId(),
                vocabularyModel, this, currentVersions, draftVersions);
        subModels.add(apModel);
        vaModel = new VersionArtefactsModel(em(), vocabularyId(),
                vocabularyModel, this, currentVersions, draftVersions);
        subModels.add(vaModel);
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
        if (!currentVersions.isEmpty()) {
            // Oops, nothing to do!
            return;
        }
        // TO DO: workflow processing.
        // Sub-models first.
        subModels.forEach(sm -> sm.deleteOnlyCurrent());

        for (Version version : currentVersions.values()) {
            TemporalUtils.makeHistorical(version, nowTime());
            version.setModifiedBy(modifiedBy());
            VersionDAO.updateVersion(em(), version);
        }
        currentVersions.clear();
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
        // TO DO: workflow processing.
        // Sub-models first.
        subModels.forEach(sm -> sm.makeCurrentIntoDraft());

        for (Version version : currentVersions.values()) {
            TemporalUtils.makeHistorical(version, nowTime());
            version.setModifiedBy(modifiedBy());
            VersionDAO.updateVersion(em(), version);
            // Now make a new draft record.
            Version draftVersion = VersionClone.INSTANCE.clone(version);
            draftVersion.setModifiedBy(modifiedBy());
            TemporalUtils.makeDraft(draftVersion);
            VersionDAO.saveVersion(em(), draftVersion);
            draftVersions.put(draftVersion.getVersionId(), draftVersion);
        }
        currentVersions.clear();
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

        // Sub-models.
        subModels.forEach(sm ->
            sm.applyChanges(updatedVocabulary));

        // And now run any tasks that have been accumulated along the way.
        processRequiredTasks();
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
                // And that's all, because this is a draft.
            }
        }

        /** {@inheritDoc} */
        @Override
        public void visitDeleteCommand(final VersionElement ve) {
            Version versionToDelete = ve.getDbVersion();
            // Notify submodels first.
            subModels.forEach(sm -> sm.notifyDeleteDraftVersion(
                    versionToDelete.getId()));
            VersionDAO.deleteVersion(em(), versionToDelete);
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
                    newVersion.setVersionId(versionId);
                    TemporalUtils.makeDraft(newVersion);
                    newVersion.setModifiedBy(modifiedBy());
                    VersionDAO.saveVersion(em(), newVersion);
                } else {
                    // Error: we don't know about this version Id.
                    // (Well, it might be a historical version that
                    // the user wanted to restore, but we don't support that.)
                    throw new IllegalArgumentException(
                            "Attempt to update version that does not "
                            + "belong to this vocabulary");
                }
            } else {
                // This is a new version.
                Version newVersion = mapper.sourceToTarget(
                        ve.getSchemaVersion(), nowTime());
                TemporalUtils.makeDraft(newVersion);
                newVersion.setModifiedBy(modifiedBy());
                VersionDAO.saveVersionWithId(em(), newVersion);
                Integer newVersionId = newVersion.getVersionId();
                draftVersions.put(newVersionId, newVersion);
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
                    schemaVersion)) {
                TemporalUtils.makeHistorical(existingVersion, nowTime());
                VersionDAO.updateVersion(em(), existingVersion);
                // Make new current instance with updated details.
                VersionRegistrySchemaMapper mapper =
                        VersionRegistrySchemaMapper.INSTANCE;
                Version newCurrentVersion = mapper.sourceToTarget(
                        schemaVersion);
                TemporalUtils.makeCurrentlyValid(newCurrentVersion, nowTime());
                newCurrentVersion.setModifiedBy(modifiedBy());
                VersionDAO.updateVersion(em(), existingVersion);
                // Update our records (i.e., in this case, overwriting
                // the previous value).
                currentVersions.put(versionId, newCurrentVersion);
                // TO DO: mark this as requiring workflow processing.
                workflowRequired(vocabularyModel.getCurrentVocabulary(),
                        newCurrentVersion);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void visitDeleteCommand(final VersionElement ve) {
            Version versionToDelete = ve.getDbVersion();
            // Notify submodels first.
            subModels.forEach(sm -> sm.notifyDeleteCurrentVersion(
                    versionToDelete.getId()));
            // Make the existing row historical.
            TemporalUtils.makeHistorical(versionToDelete, nowTime());
            versionToDelete.setModifiedBy(modifiedBy());
            VersionDAO.updateVersion(em(), versionToDelete);
            Integer versionId = ve.getVersionId();
            currentVersions.remove(versionId);
            // TO DO: workflow deletion processing.
            workflowRequired(vocabularyModel.getCurrentVocabulary(),
                    versionToDelete);
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
                    Version existingDraft = draftVersions.remove(versionId);
                    mapper.updateTargetFromSource(schemaVersion,
                            existingDraft);
                    TemporalUtils.makeCurrentlyValid(existingDraft, nowTime());
                    existingDraft.setModifiedBy(modifiedBy());
                    VersionDAO.updateVersion(em(), existingDraft);
                    currentVersions.put(versionId, existingDraft);
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
                TemporalUtils.makeCurrentlyValid(newCurrentVersion, nowTime());
                newCurrentVersion.setModifiedBy(modifiedBy());
                VersionDAO.saveVersionWithId(em(), newCurrentVersion);
                Integer newVersionId = newCurrentVersion.getVersionId();
                // Update our records (i.e., in this case, adding
                // a new entry).
                currentVersions.put(newVersionId, newCurrentVersion);
                // And this is a tricky bit: we modify the input data
                // so that the version Id can be seen by submodels.
                schemaVersion.setId(newVersionId);

                // TO DO: mark this as requiring workflow processing.
                workflowRequired(vocabularyModel.getCurrentVocabulary(),
                        newCurrentVersion);
            }
            // Join paths: what follows applies both when reusing
            // an existing database row and when adding a new database row.
            Task task = getTaskForVersion(versionId);
            // Apply the settings for the three version-level flags.
            if (schemaVersion.isDoPoolpartyHarvest()) {
                task.addSubtask(WorkflowMethods.createHarvestPoolPartySubtask(
                        SubtaskOperationType.INSERT,
                        vocabularyModel.getCurrentVocabulary()));
            }
            if (schemaVersion.isDoImport()) {
                task.addSubtask(WorkflowMethods.createImporterSesameSubtask(
                        SubtaskOperationType.INSERT));
            }
            if (ve.getSchemaVersion().isDoPublish()) {
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
            versionTasks.put(versionId, task);
            versionTaskInfos.put(versionId, taskInfo);
        }
    }

    /** Process all of the tasks that have been accumulated. */
    private void processRequiredTasks() {
        for (TaskInfo taskInfo : versionTaskInfos.values()) {
            taskInfo.setEm(em());
            taskInfo.setNowTime(nowTime());
            taskInfo.setModifiedBy(modifiedBy());
            taskInfo.persistAndProcess();
        }
    }

}
