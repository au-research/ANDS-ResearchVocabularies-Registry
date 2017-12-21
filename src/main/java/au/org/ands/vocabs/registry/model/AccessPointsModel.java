/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.model;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.converter.AccessPointDbSchemaMapper;
import au.org.ands.vocabs.registry.db.dao.AccessPointDAO;
import au.org.ands.vocabs.registry.db.entity.AccessPoint;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.enums.VocabularyStatus;
import au.org.ands.vocabs.registry.model.sequence.AccessPointElement;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;
import au.org.ands.vocabs.registry.workflow.WorkflowMethods;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;

/** Access points domain model.
 * This is a representation of the access points of a vocabulary,
 * as an abstract data type.
 */
public class AccessPointsModel extends ModelBase {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

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
     * @param aVersionsModel The parent versionsModel of this instance.
     * @param aCurrentVersions The current version instances of the vocabulary.
     * @param aDraftVersions The draft version instances of the vocabulary.
     * @throws IllegalArgumentException If aVocabularyId is null.
     */
    public AccessPointsModel(final EntityManager anEm,
            final Integer aVocabularyId,
            final VersionsModel aVersionsModel,
            final Map<Integer, Version> aCurrentVersions,
            final Map<Integer, Version> aDraftVersions) {
        if (aVocabularyId == null) {
            logger.error("Attempt to construct versions model with no Id");
            throw new IllegalArgumentException(
                    "Attempt to construct versions model with no Id");
        }
        setEm(anEm);
        setVocabularyId(aVocabularyId);
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
                            + "V Id, relation, meaning: " + vId + ","
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
                accumulateSubtasks(vId, WorkflowMethods.deleteAccessPoint(ap));
                TemporalUtils.makeHistorical(ap, nowTime());
                ap.setModifiedBy(modifiedBy());
                AccessPointDAO.updateAccessPoint(em(), ap);
            }
        }
        currentAPs.clear();
    }

    /** Add workflow subtasks required for a version.
     * @param versionId The version Id.
     * @param subtaskList The list of subtasks to be applied for the version.
     */
    private void accumulateSubtasks(final Integer versionId,
            final List<Subtask> subtaskList) {
        if (subtaskList.isEmpty()) {
            // No subtasks required.
            return;
        }
        versionsModel.workflowRequired(versionId);
        versionsModel.getTaskForVersion(versionId).addSubtasks(subtaskList);
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
                em().remove(ap);
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
        for (AccessPoint ap : currentAPs.get(versionId)) {
            // Just delete the row;
            // for a draft instance, no workflow is applied.
            em().remove(ap);
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
        // TO DO: finish this method.
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





    }

    /** Make the database's currently-valid view of the
     * VocabularyRelatedEntities match updatedVocabulary.
     * @param updatedVocabulary The updated vocabulary.
     */
    private void applyChangesCurrent(final Vocabulary updatedVocabulary) {
        // TO DO: finish this method.
        // Create sequences.
        // First, the current values.
    }

}
