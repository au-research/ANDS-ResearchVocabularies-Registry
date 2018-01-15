/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.model;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.collections4.sequence.CommandVisitor;
import org.apache.commons.collections4.sequence.SequencesComparator;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.converter.AccessPointDbSchemaMapper;
import au.org.ands.vocabs.registry.db.dao.AccessPointDAO;
import au.org.ands.vocabs.registry.db.entity.AccessPoint;
import au.org.ands.vocabs.registry.db.entity.ComparisonUtils;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.enums.AccessPointType;
import au.org.ands.vocabs.registry.enums.SubtaskOperationType;
import au.org.ands.vocabs.registry.enums.VocabularyStatus;
import au.org.ands.vocabs.registry.model.sequence.AccessPointElement;
import au.org.ands.vocabs.registry.workflow.WorkflowMethods;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;

/** Access points domain model.
 * This is a representation of the access points of a vocabulary,
 * as an abstract data type.
 */
public class AccessPointsModel extends ModelBase {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** The parent VocabularyModel of this instance. Passed down
     * by VersionsModel. */
    private VocabularyModel vocabularyModel;

    /** The parent VersionsModel of this instance. Passed down
     * by VersionsModel. */
    private VersionsModel versionsModel;

    /** The current instances of versions, if there are any.
     * The keys are version Ids. Passed down by VersionsModel. */
    private Map<Integer, Version> currentVersions;

    /** The draft instances of versions, if there are any.
     * The keys are version Ids. Passed down by VersionsModel. */
    private Map<Integer, Version> draftVersions;

    /** The current instances of access points, if there are any.
     * The keys are version Ids. */
    private MultivaluedMap<Integer, AccessPoint> currentAPs =
            new MultivaluedHashMap<>();

    /** The draft instances of access points, if there are any.
     * The keys are version Ids. */
    private MultivaluedMap<Integer, AccessPoint> draftAPs =
            new MultivaluedHashMap<>();

    /** Construct access points model for a vocabulary.
     * @param anEm The EntityManager to be used to fetch and update
     *      database data.
     * @param aVocabularyId The Id of the vocabulary for which the model
     *      is to be constructed.
     * @param aVocabularyModel The parent VocabularyModel of this instance.
     * @param aVersionsModel The parent VersionsModel of this instance.
     * @param aCurrentVersions The current version instances of the vocabulary.
     * @param aDraftVersions The draft version instances of the vocabulary.
     * @throws IllegalArgumentException If aVocabularyId is null.
     */
    public AccessPointsModel(final EntityManager anEm,
            final Integer aVocabularyId,
            final VocabularyModel aVocabularyModel,
            final VersionsModel aVersionsModel,
            final Map<Integer, Version> aCurrentVersions,
            final Map<Integer, Version> aDraftVersions) {
        if (aVocabularyId == null) {
            logger.error("Attempt to construct access points model with no Id");
            throw new IllegalArgumentException(
                    "Attempt to construct access points model with no Id");
        }
        setEm(anEm);
        setVocabularyId(aVocabularyId);
        vocabularyModel = aVocabularyModel;
        versionsModel = aVersionsModel;
        currentVersions = aCurrentVersions;
        draftVersions = aDraftVersions;
        populateModel();
    }

    /** {@inheritDoc} */
    @Override
    protected void populateModel() {
        // Current
        for (Integer versionId : currentVersions.keySet()) {
            currentAPs.addAll(versionId,
                    AccessPointDAO.getCurrentAccessPointListForVersion(em(),
                            versionId));
        }

        // Draft
        for (Integer versionId : draftVersions.keySet()) {
            draftAPs.addAll(versionId,
                    AccessPointDAO.getDraftAccessPointListForVersion(em(),
                            versionId));
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<String> describeModel() {
        List<String> description = new ArrayList<>();
        if (vocabularyId() == null) {
            description.add("AP | No vocabulary Id");
            return description;
        }
        description.add("AP | Vocabulary; Id: " + vocabularyId());
        if (currentAPs != null) {
            for (Integer vId : currentAPs.keySet()) {
                for (AccessPoint ap
                        : currentAPs.get(vId)) {
                    description.add("AP | Current version has AP; "
                            + "V Id, AP Id: " + vId + ","
                            + ap.getAccessPointId());
                }
            }
        }
        if (draftAPs != null) {
            for (Integer vId : draftAPs.keySet()) {
                for (AccessPoint ap
                        : draftAPs.get(vId)) {
                    description.add("AP | Draft version has AP; "
                            + "V Id, AP Id: " + vId + ","
                            + ap.getAccessPointId());
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

    /** {@inheritDoc}
     * The top-level metadata for versions must already have been
     * inserted into outputVocabulary before invoking this method.
     *  */
    @Override
    protected void insertIntoSchemaFromCurrent(final
            au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
            outputVocabulary,
            final boolean includeVersions,
            final boolean includeAccessPoints,
            final boolean includeRelatedEntitiesAndVocabularies) {

        if (!includeAccessPoints) {
            // Access points were not requested.
            return;
        }
        List<au.org.ands.vocabs.registry.schema.vocabulary201701.Version>
        outputVersionList = outputVocabulary.getVersion();

        AccessPointDbSchemaMapper apDbMapper =
                AccessPointDbSchemaMapper.INSTANCE;

        for (au.org.ands.vocabs.registry.schema.vocabulary201701.
                Version outputVersion : outputVersionList) {
            Integer versionId = outputVersion.getId();
            List<au.org.ands.vocabs.registry.schema.vocabulary201701.
            AccessPoint> outputAPList = outputVersion.getAccessPoint();
            for (AccessPoint ap : currentAPs.get(versionId)) {
                outputAPList.add(apDbMapper.sourceToTarget(ap));
            }
        }
    }

    /** {@inheritDoc}
     * The top-level metadata for versions must already have been
     * inserted into outputVocabulary before invoking this method.
     *  */
    @Override
    protected void insertIntoSchemaFromDraft(final
            au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
            outputVocabulary,
            final boolean includeVersions,
            final boolean includeAccessPoints,
            final boolean includeRelatedEntitiesAndVocabularies) {

        if (!includeAccessPoints) {
            // Access points were not requested.
            return;
        }
        List<au.org.ands.vocabs.registry.schema.vocabulary201701.Version>
        outputVersionList = outputVocabulary.getVersion();

        AccessPointDbSchemaMapper apDbMapper =
                AccessPointDbSchemaMapper.INSTANCE;

        for (au.org.ands.vocabs.registry.schema.vocabulary201701.
                Version outputVersion : outputVersionList) {
            Integer versionId = outputVersion.getId();
            List<au.org.ands.vocabs.registry.schema.vocabulary201701.
            AccessPoint> outputAPList = outputVersion.getAccessPoint();
            for (AccessPoint ap : draftAPs.get(versionId)) {
                outputAPList.add(apDbMapper.sourceToTarget(ap));
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void deleteOnlyCurrent() {
        for (Integer vId : currentAPs.keySet()) {
            for (AccessPoint ap : currentAPs.get(vId)) {
                // TO DO: make sure delete workflow has been done!
                accumulateSubtasks(vocabularyModel.getCurrentVocabulary(),
                        currentVersions.get(vId),
                        WorkflowMethods.deleteAccessPoint(ap));
                TemporalUtils.makeHistorical(ap, nowTime());
                ap.setModifiedBy(modifiedBy());
                AccessPointDAO.updateAccessPoint(em(), ap);
            }
        }
        currentAPs.clear();
    }

    /** Add workflow subtasks required for a version. Use this version
     * of the method to ensure that the workflowRequired() method
     * is called.
     * @param vocabulary The vocabulary instance for which workflow
     *      subtasks are to be added.
     * @param version The version instance for which workflow
     *      subtasks are to be added.
     * @param subtaskList The list of subtasks to be applied for the version.
     */
    private void accumulateSubtasks(final Vocabulary vocabulary,
            final Version version,
            final List<Subtask> subtaskList) {
        if (subtaskList == null || subtaskList.isEmpty()) {
            // No subtasks required.
            return;
        }
        Integer versionId = version.getVersionId();
        versionsModel.workflowRequired(vocabulary, version);
        versionsModel.getTaskForVersion(versionId).addSubtasks(subtaskList);
    }

    /** Add workflow subtasks required for a version. Use this version
     * of the method only if you are sure that the workflowRequired() method
     * has already been called.
     * @param version The version instance for which workflow
     *      subtasks are to be added.
     * @param subtaskList The list of subtasks to be applied for the version.
     */
    private void accumulateSubtasks(final Version version,
            final List<Subtask> subtaskList) {
        if (subtaskList == null || subtaskList.isEmpty()) {
            // No subtasks required.
            return;
        }
        Integer versionId = version.getVersionId();
        versionsModel.getTaskForVersion(versionId).addSubtasks(subtaskList);
    }

    /** Add one workflow subtask required for a version.
     * @param vocabulary The vocabulary instance for which workflow
     *      subtasks are to be added.
     * @param version The version instance for which workflow
     *      subtasks are to be added.
     * @param subtask The subtask to be applied for the version.
     */
    private void accumulateSubtask(final Vocabulary vocabulary,
            final Version version,
            final Subtask subtask) {
        Integer versionId = version.getVersionId();
        versionsModel.workflowRequired(vocabulary, version);
        versionsModel.getTaskForVersion(versionId).addSubtask(subtask);
    }

    /** {@inheritDoc} */
    @Override
    protected void makeCurrentIntoDraft() {
        if (currentAPs.isEmpty()) {
            // Oops, nothing to do!
            return;
        }
        if (!draftAPs.isEmpty()) {
            // Error
            throw new IllegalArgumentException(
                    "Existing draft; you must delete it first");
        }
        // TO DO: workflow processing.
        for (Integer vId : currentAPs.keySet()) {
            for (AccessPoint ap : currentAPs.get(vId)) {
                TemporalUtils.makeHistorical(ap, nowTime());
                ap.setModifiedBy(modifiedBy());
                AccessPointDAO.updateAccessPoint(em(), ap);
                // Now make a new draft record.
                AccessPoint newAP = new AccessPoint();
                newAP.setVersionId(vId);
                newAP.setAccessPointId(ap.getAccessPointId());
                newAP.setType(ap.getType());
                newAP.setSource(ap.getSource());
                newAP.setData(ap.getData());
                newAP.setModifiedBy(modifiedBy());
                TemporalUtils.makeDraft(newAP);
                AccessPointDAO.saveAccessPoint(em(), newAP);
                draftAPs.add(vId, newAP);
            }
        }
        currentAPs.clear();
    }

    /** {@inheritDoc} */
    @Override
    protected void deleteOnlyDraft() {
        deleteDraftDatabaseRows();
    }

    /** {@inheritDoc} */
    @Override
    protected void deleteDraftDatabaseRows() {
        for (Integer vId : draftAPs.keySet()) {
            for (AccessPoint ap : draftAPs.get(vId)) {
                AccessPointDAO.deleteAccessPoint(em(), ap);
            }
        }
        draftAPs.clear();
    }

    /** {@inheritDoc} */
    @Override
    protected void notifyDeleteCurrentVersion(final Integer versionId) {
        for (AccessPoint ap : currentAPs.get(versionId)) {
            // TO DO: workflow deletion processing.
            // Make the existing row historical.
            TemporalUtils.makeHistorical(ap, nowTime());
            ap.setModifiedBy(modifiedBy());
            AccessPointDAO.updateAccessPoint(em(), ap);
        }
        // Remove from our own records.
        currentAPs.remove(versionId);
    }

    /** {@inheritDoc} */
    @Override
    protected void notifyDeleteDraftVersion(final Integer versionId) {
        for (AccessPoint ap : draftAPs.get(versionId)) {
            // Just delete the row;
            // for a draft instance, no workflow is applied.
            AccessPointDAO.deleteAccessPoint(em(), ap);
        }
        // Remove from our own records.
        draftAPs.remove(versionId);
    }

    /** {@inheritDoc}
     * Before invoking this method, fill in all version Ids for new versions:
     * refer to the two visitInsertCommand methods in VersionsModel.
     * Before invoking this method, apply all requested deletion of versions,
     * using the notifyDeleteCurrentVersion and notifyDeleteDraftVersion
     * methods.
     * This method deals only with access points of new and updated versions.
     */
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
    }

    /** Make the database's draft view of the
     * VocabularyRelatedEntities match updatedVocabulary.
     * @param updatedVocabulary The updated vocabulary.
     */
    private void applyChangesDraft(
            final au.org.ands.vocabs.registry.schema.vocabulary201701.
            Vocabulary updatedVocabulary) {
        // Create sequences.
        // First, any existing draft values. All of these have AP Ids.
        List<AccessPointElement> existingDraftSequence = new ArrayList<>();
        for (Entry<Integer, List<AccessPoint>> apList : draftAPs.entrySet()) {
            Integer versionId = apList.getKey();
            for (AccessPoint ap : apList.getValue()) {
                existingDraftSequence.add(new AccessPointElement(
                    versionId, ap.getAccessPointId(), ap, null));
            }
        }
        Collections.sort(existingDraftSequence);

        // And now, the updated draft values. Note the comment for
        // applyChanges() above: each version _does_ have a version Id,
        // because any missing ones were supplied by VersionsModel.
        List<AccessPointElement> updatedSequence = new ArrayList<>();
        // Also, put the access points that have access point Ids into a map,
        // so that they can be found later by visitKeepCommand().
        Map<Integer,
        au.org.ands.vocabs.registry.schema.vocabulary201701.AccessPoint>
        updatedAPs = new HashMap<>();
        updatedVocabulary.getVersion().forEach(
                version -> {
                    Integer versionId = version.getId();
                    version.getAccessPoint().forEach(
                            ap -> {
                                updatedSequence.add(new AccessPointElement(
                                        versionId, ap.getId(), null, ap));
                                if (ap.getId() != null) {
                                    updatedAPs.put(ap.getId(), ap);
                                }
                            });
                });
        Collections.sort(updatedSequence);

        // Compute difference.
        final SequencesComparator<AccessPointElement> comparator =
                new SequencesComparator<>(
                        existingDraftSequence, updatedSequence);
        // Apply the changes.
        comparator.getScript().visit(new UpdateDraftVisitor(updatedAPs));
    }

    /** See if there is an existing, current AccessPoint with
     * a given version Id and access point Id.
     * @param versionId The version Id.
     * @param apId The access point Id.
     * @return The current AccessPoint, if it exists, or null, if there
     *      is no such existing, current access point.
     */
    private AccessPoint getExistingCurrentAPByIds(final Integer versionId,
            final Integer apId) {
        List<AccessPoint> aps = currentAPs.get(versionId);
        if (aps == null) {
            return null;
        }
        for (AccessPoint ap : aps) {
            if (ap.getAccessPointId().equals(apId)) {
                return ap;
            }
        }
        return null;
    }

    /** Visitor class that applies a sequence of updates to the
     * draft view of the database. */
    private class UpdateDraftVisitor
        implements CommandVisitor<AccessPointElement> {

        /** The map of updated access points. Keys are access point Ids; values
         * are the access points in registry schema format. */
        private Map<Integer,
        au.org.ands.vocabs.registry.schema.vocabulary201701.AccessPoint>
        updatedAPs;

        /** Constructor that accepts the map of updated access points.
         * @param anUpdatedAPs The map of updated access points.
         */
        UpdateDraftVisitor(final Map<Integer,
                au.org.ands.vocabs.registry.schema.vocabulary201701.
                AccessPoint> anUpdatedAPs) {
            updatedAPs = anUpdatedAPs;
        }

        /** {@inheritDoc} */
        @Override
        public void visitKeepCommand(final AccessPointElement ape) {
            // Possible future work: support metadata updates.
            Integer apId = ape.getAPId();
            AccessPoint existingAP = ape.getDbAP();
            au.org.ands.vocabs.registry.schema.vocabulary201701.AccessPoint
            schemaAP = updatedAPs.get(apId);

            if (!ComparisonUtils.isEqualAP(existingAP,
                    schemaAP)) {
                throw new IllegalArgumentException(
                        "Changing details of an existing access point "
                        + "is not supported");
            }
        }

        /** {@inheritDoc} */
        @Override
        public void visitDeleteCommand(final AccessPointElement ape) {
            AccessPoint draftAp = ape.getDbAP();
            AccessPointDAO.deleteAccessPoint(em(), draftAp);
            draftAPs.get(ape.getVersionId()).remove(draftAp);
            // And that's all, since we are updating a draft.
        }

        /** {@inheritDoc} */
        @Override
        public void visitInsertCommand(final AccessPointElement ape) {
            // See if there's an AP Id. We normally expect
            // there _not_ to be. But if there is,
            // it had better belong to the version already.
            Integer versionId = ape.getVersionId();
            Integer apId = ape.getAPId();
            au.org.ands.vocabs.registry.schema.vocabulary201701.AccessPoint
            schemaAP = ape.getSchemaAP();
            if (apId != null) {
                // There's an AP Id, so check if it's in the
                // set of current instances.
                AccessPoint currentAP = getExistingCurrentAPByIds(
                        versionId, apId);
                if (currentAP != null) {
                    // Possible future work: support metadata updates.
                    // For now, we don't support changes.
                    if (!ComparisonUtils.isEqualAP(currentAP,
                            schemaAP)) {
                        throw new IllegalArgumentException(
                                "Changing details of an existing access point "
                                + "is not supported");
                    }
                    // We don't need to create a new version Id, but
                    // we do need to add a draft row for it.
                    Version draftVersion = draftVersions.get(versionId);
                    versionsModel.workflowRequired(
                            vocabularyModel.getDraftVocabulary(), draftVersion);
                    TaskInfo taskInfo =
                            versionsModel.getTaskInfoForVersion(versionId);
                    Pair<AccessPoint, List<Subtask>> insertResult =
                    WorkflowMethods.insertAccessPoint(em(), null, taskInfo,
                            true, modifiedBy(), nowTime(), schemaAP);
                    AccessPoint insertedAP = insertResult.getLeft();
                    if (insertedAP != null) {
                        draftAPs.add(versionId, insertedAP);
                    }
                    accumulateSubtasks(draftVersion, insertResult.getRight());
                } else {
                    // Error: we don't know about this AP Id.
                    // (Well, it might be a historical version that
                    // the user wanted to restore, but we don't support that.)
                    throw new IllegalArgumentException(
                            "Attempt to update access point that does not "
                            + "belong to this version");
                }
            } else {
                // This is a new access point.
                Version draftVersion = draftVersions.get(versionId);
                versionsModel.workflowRequired(
                        vocabularyModel.getDraftVocabulary(), draftVersion);
                TaskInfo taskInfo =
                        versionsModel.getTaskInfoForVersion(versionId);
                Pair<AccessPoint, List<Subtask>> insertResult =
                WorkflowMethods.insertAccessPoint(em(), null, taskInfo, true,
                        modifiedBy(), nowTime(), schemaAP);
                AccessPoint insertedAP = insertResult.getLeft();
                if (insertedAP != null) {
                    draftAPs.add(versionId, insertedAP);
                }
                accumulateSubtasks(draftVersion, insertResult.getRight());
                // And that's all, because this is a draft.
            }
        }
    }

    /** Make the database's currently-valid view of the
     * VocabularyRelatedEntities match updatedVocabulary.
     * @param updatedVocabulary The updated vocabulary.
     */
    private void applyChangesCurrent(
            final au.org.ands.vocabs.registry.schema.vocabulary201701.
            Vocabulary updatedVocabulary) {
        // TO DO: finish this method.
        // Create sequences.
        // First, the current values. All of these have AP Ids.
        List<AccessPointElement> currentSequence = new ArrayList<>();
        for (Entry<Integer, List<AccessPoint>> apList : currentAPs.entrySet()) {
            Integer versionId = apList.getKey();
            for (AccessPoint ap : apList.getValue()) {
                currentSequence.add(new AccessPointElement(
                    versionId, ap.getAccessPointId(), ap, null));
            }
        }
        Collections.sort(currentSequence);

        // And now, the updated values. Note the comment for
        // applyChanges() above: each version _does_ have a version Id,
        // because any missing ones were supplied by VersionsModel.
        List<AccessPointElement> updatedSequence = new ArrayList<>();
        // Link access points to the updated Versions, so that the
        // visitor can work out if extra subtasks are required.
        // Keys are version Ids.
        Map<Integer,
        au.org.ands.vocabs.registry.schema.vocabulary201701.Version>
        updatedVersions = new HashMap<>();
        // Also, put the access points that have access point Ids into a map,
        // so that they can be found later by visitKeepCommand().
        // Keys are access point Ids.
        Map<Integer,
        au.org.ands.vocabs.registry.schema.vocabulary201701.AccessPoint>
        updatedAPs = new HashMap<>();
        updatedVocabulary.getVersion().forEach(
                version -> {
                    Integer versionId = version.getId();
                    updatedVersions.put(versionId, version);
                    version.getAccessPoint().forEach(
                            ap -> {
                                updatedSequence.add(new AccessPointElement(
                                        versionId, ap.getId(), null, ap));
                                if (ap.getId() != null) {
                                    updatedAPs.put(ap.getId(), ap);
                                }
                            });
                });
        Collections.sort(updatedSequence);

        // Compute difference.
        final SequencesComparator<AccessPointElement> comparator =
                new SequencesComparator<>(
                        currentSequence, updatedSequence);
        // Apply the changes.
        comparator.getScript().visit(new UpdateCurrentVisitor(updatedVersions,
                updatedAPs));
        // Delete any remaining draft rows.
        // TO DO: decide if this is what is to be done. May
        // need to do workflow processing?
        deleteDraftDatabaseRows();
    }

    /** See if there is an existing, draft AccessPoint with
     * a given version Id and access point Id.
     * @param versionId The version Id.
     * @param apId The access point Id.
     * @return The draft AccessPoint, if it exists, or null, if there
     *      is no such existing, draft access point.
     */
    private AccessPoint getExistingDraftAPByIds(final Integer versionId,
            final Integer apId) {
        List<AccessPoint> aps = draftAPs.get(versionId);
        if (aps == null) {
            return null;
        }
        for (AccessPoint ap : aps) {
            if (ap.getAccessPointId().equals(apId)) {
                return ap;
            }
        }
        return null;
    }

    /** Visitor class that applies a sequence of updates to the
     * currently-valid view of the database. */
    private class UpdateCurrentVisitor
        implements CommandVisitor<AccessPointElement> {

        /** The map of updated access points. Keys are access point Ids; values
         * are the access points in registry schema format. */
        private Map<Integer,
        au.org.ands.vocabs.registry.schema.vocabulary201701.Version>
        updatedVersions;

        /** The map of updated access points. Keys are access point Ids; values
         * are the access points in registry schema format. */
        private Map<Integer,
        au.org.ands.vocabs.registry.schema.vocabulary201701.AccessPoint>
        updatedAPs;

        /** Constructor that accepts the map of updated access points.
         * @param anUpdatedVersions The map of updated versions.
         * @param anUpdatedAPs The map of updated access points.
         */
        UpdateCurrentVisitor(final Map<Integer,
                au.org.ands.vocabs.registry.schema.vocabulary201701.
                Version> anUpdatedVersions,
                final Map<Integer,
                au.org.ands.vocabs.registry.schema.vocabulary201701.
                AccessPoint> anUpdatedAPs) {
            updatedVersions = anUpdatedVersions;
            updatedAPs = anUpdatedAPs;
        }

        /** {@inheritDoc} */
        @Override
        public void visitKeepCommand(final AccessPointElement ape) {
            // Possible future work: support metadata updates.
            Integer apId = ape.getAPId();
            AccessPoint existingAP = ape.getDbAP();
            au.org.ands.vocabs.registry.schema.vocabulary201701.AccessPoint
            schemaAP = updatedAPs.get(apId);

            if (!ComparisonUtils.isEqualAP(existingAP,
                    schemaAP)) {
                throw new IllegalArgumentException(
                        "Changing details of an existing access point "
                        + "is not supported");
            }
        }

        /** {@inheritDoc} */
        @Override
        public void visitDeleteCommand(final AccessPointElement ape) {
            Integer versionId = ape.getVersionId();
            AccessPoint apToDelete = ape.getDbAP();
            List<Subtask> subtaskList =
                    WorkflowMethods.deleteAccessPoint(apToDelete);
            if (subtaskList == null) {
                // No more to do.
                // Make the existing row historical.
                TemporalUtils.makeHistorical(apToDelete, nowTime());
                apToDelete.setModifiedBy(modifiedBy());
                AccessPointDAO.updateAccessPoint(em(), apToDelete);
                currentAPs.get(versionId).remove(apToDelete);
            } else {
                accumulateSubtasks(vocabularyModel.getCurrentVocabulary(),
                        currentVersions.get(versionId), subtaskList);
            }
            // Was a file access point deleted, and does the version have
            // the import flag set? If so, need to force re-importing.
            if (apToDelete.getType() == AccessPointType.FILE
                    && updatedVersions.get(versionId).isDoImport()) {
                accumulateSubtask(vocabularyModel.getCurrentVocabulary(),
                        currentVersions.get(versionId),
                        WorkflowMethods.createImporterSesameSubtask(
                                SubtaskOperationType.INSERT));
            }
        }

        /** {@inheritDoc} */
        @Override
        public void visitInsertCommand(final AccessPointElement ape) {
            // See if there's an AP Id. We normally expect
            // there _not_ to be. But if there is,
            // it had better belong to the version already.
            Integer versionId = ape.getVersionId();
            Version currentVersion = currentVersions.get(versionId);
            Integer apId = ape.getAPId();
            au.org.ands.vocabs.registry.schema.vocabulary201701.AccessPoint
            schemaAP = ape.getSchemaAP();
            // Keep track of the AccessPoint as actually inserted, so
           // that it can be used after the if statement is completed,
            // irrespective of the path taken.
            AccessPoint accessPoint = null;
            if (apId != null) {
                // There's an AP Id, so check if it's in the
                // set of draft instances.
                AccessPoint draftAP = getExistingDraftAPByIds(versionId, apId);
                if (draftAP != null) {
                    // Possible future work: support metadata updates.
                    // For now, we don't support changes.
                    if (!ComparisonUtils.isEqualAP(draftAP,
                            schemaAP)) {
                        throw new IllegalArgumentException(
                                "Changing details of an existing access point "
                                + "is not supported");
                    }
                    // Reuse this draft row, making it no longer a draft.
                    versionsModel.workflowRequired(
                            vocabularyModel.getCurrentVocabulary(),
                            currentVersion);
                    TaskInfo taskInfo =
                            versionsModel.getTaskInfoForVersion(versionId);
                    Pair<AccessPoint, List<Subtask>> insertResult =
                    WorkflowMethods.insertAccessPoint(em(), draftAP, taskInfo,
                            false, modifiedBy(), nowTime(), schemaAP);
                    draftAPs.get(versionId).remove(draftAP);
                    currentAPs.add(versionId, draftAP);
                    accumulateSubtasks(currentVersion, insertResult.getRight());
                    accessPoint = draftAP;
                    // Possible future work required: check if there is
                    // any other subtask to be done.
                } else {
                    // Error: we don't know about this version Id.
                    // (Well, it might be a historical version that
                    // the user wanted to restore, but we don't support that.)
                    throw new IllegalArgumentException(
                            "Attempt to update access point that does not "
                            + "belong to this version");
                }
            } else {
                // This is a new access point.
                versionsModel.workflowRequired(
                        vocabularyModel.getCurrentVocabulary(),
                        currentVersion);
                TaskInfo taskInfo =
                        versionsModel.getTaskInfoForVersion(versionId);
                Pair<AccessPoint, List<Subtask>> insertResult =
                WorkflowMethods.insertAccessPoint(em(), null, taskInfo, false,
                        modifiedBy(), nowTime(), schemaAP);
                AccessPoint insertedAP = insertResult.getLeft();
                if (insertedAP != null) {
                    currentAPs.add(versionId, insertedAP);
                }
                accumulateSubtasks(currentVersion, insertResult.getRight());
                accessPoint = insertedAP;
                // Possible future work required: check if there is
                // any other subtask to be done.
            }
            // Was a file access point added, and does the version have
            // the import flag set? If so, need to force importing.
            if (accessPoint.getType() == AccessPointType.FILE
                    && updatedVersions.get(versionId).isDoImport()) {
                accumulateSubtask(vocabularyModel.getCurrentVocabulary(),
                        currentVersions.get(versionId),
                        WorkflowMethods.createImporterSesameSubtask(
                                SubtaskOperationType.INSERT));
            }
        }
    }

}
