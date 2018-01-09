/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.model;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.dao.VersionArtefactDAO;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.VersionArtefact;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;
import au.org.ands.vocabs.registry.workflow.WorkflowMethods;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;

/** Version artefacts domain model.
 * This is a representation of the version artefacts of a vocabulary,
 * as an abstract data type.
 */
public class VersionArtefactsModel extends ModelBase {

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

    /** The current instances of version artefacts, if there are any.
     * The keys are version Ids. */
    private MultivaluedMap<Integer, VersionArtefact> currentVAs =
            new MultivaluedHashMap<>();

    /** The draft instances of version artefacts, if there are any.
     * The keys are version Ids. */
    private MultivaluedMap<Integer, VersionArtefact> draftVAs =
            new MultivaluedHashMap<>();

    /** Construct version artefacts model for a vocabulary.
     * @param anEm The EntityManager to be used to fetch and update
     *      database data.
     * @param aVocabularyId The Id of the vocabulary for which the model
     *      is to be constructed.
     * @param aVersionsModel The parent versionsModel of this instance.
     * @param aCurrentVersions The current version instances of the vocabulary.
     * @param aDraftVersions The draft version instances of the vocabulary.
     * @throws IllegalArgumentException If aVocabularyId is null.
     */
    public VersionArtefactsModel(final EntityManager anEm,
            final Integer aVocabularyId,
            final VersionsModel aVersionsModel,
            final Map<Integer, Version> aCurrentVersions,
            final Map<Integer, Version> aDraftVersions) {
        if (aVocabularyId == null) {
            logger.error("Attempt to construct version artefacts model "
                    + "with no Id");
            throw new IllegalArgumentException(
                    "Attempt to construct version artefactss model with no Id");
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
            currentVAs.addAll(versionId,
                    VersionArtefactDAO.getCurrentVersionArtefactListForVersion(
                            em(), versionId));
        }

        // Draft
        for (Integer versionId : draftVersions.keySet()) {
            draftVAs.addAll(versionId,
                    VersionArtefactDAO.getDraftVersionArtefactListForVersion(
                            em(), versionId));
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<String> describeModel() {
        List<String> description = new ArrayList<>();
        if (vocabularyId() == null) {
            description.add("VA | No vocabulary Id");
            return description;
        }
        description.add("VA | Vocabulary; Id: " + vocabularyId());
        if (currentVAs != null) {
            for (Integer vId : currentVAs.keySet()) {
                for (VersionArtefact va
                        : currentVAs.get(vId)) {
                    description.add("VA | Current version has VA; "
                            + "V Id, VA Id: " + vId + ","
                            + va.getVersionArtefactId());
                }
            }
        }
        if (draftVAs != null) {
            for (Integer vId : draftVAs.keySet()) {
                for (VersionArtefact va
                        : draftVAs.get(vId)) {
                    description.add("VA | Draft version has VA; "
                            + "V Id, VA Id: " + vId + ","
                            + va.getVersionArtefactId());
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
     * Version artefacts are private, for now, so this method
     * does nothing. */
    @Override
    protected void insertIntoSchemaFromCurrent(final
            au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
            outputVocabulary,
            final boolean includeVersions,
            final boolean includeAccessPoints,
            final boolean includeRelatedEntitiesAndVocabularies) {
    }

    /** {@inheritDoc}
     * Version artefacts are private, for now, so this method
     * does nothing. */
    @Override
    protected void insertIntoSchemaFromDraft(final
            au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
            outputVocabulary,
            final boolean includeVersions,
            final boolean includeAccessPoints,
            final boolean includeRelatedEntitiesAndVocabularies) {
    }

    /** {@inheritDoc} */
    @Override
    protected void deleteOnlyCurrent() {
        for (Integer vId : currentVAs.keySet()) {
            for (VersionArtefact va : currentVAs.get(vId)) {
                // TO DO: make sure delete workflow has been done!
                accumulateSubtasks(vId,
                        WorkflowMethods.deleteVersionArtefact(va));
                TemporalUtils.makeHistorical(va, nowTime());
                va.setModifiedBy(modifiedBy());
                VersionArtefactDAO.updateVersionArtefact(em(), va);
            }
        }
        currentVAs.clear();
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
        if (currentVAs.isEmpty()) {
            // Oops, nothing to do!
            return;
        }
        if (!draftVAs.isEmpty()) {
            // Error
            throw new IllegalArgumentException(
                    "Existing draft; you must delete it first");
        }
        // TO DO: workflow processing.
        for (Integer vId : currentVAs.keySet()) {
            for (VersionArtefact va : currentVAs.get(vId)) {
                TemporalUtils.makeHistorical(va, nowTime());
                va.setModifiedBy(modifiedBy());
                VersionArtefactDAO.updateVersionArtefact(em(), va);
                // Now make a new draft record.
                VersionArtefact newVA = new VersionArtefact();
                newVA.setVersionId(vId);
                newVA.setVersionArtefactId(va.getVersionArtefactId());
                newVA.setType(va.getType());
                newVA.setData(va.getData());
                newVA.setModifiedBy(modifiedBy());
                TemporalUtils.makeDraft(newVA);
                VersionArtefactDAO.saveVersionArtefact(em(), newVA);
                draftVAs.add(vId, newVA);
            }
        }
        currentVAs.clear();
    }

    /** {@inheritDoc} */
    @Override
    protected void deleteOnlyDraft() {
        deleteDraftDatabaseRows();
    }

    /** {@inheritDoc} */
    @Override
    protected void deleteDraftDatabaseRows() {
        for (Integer vId : draftVAs.keySet()) {
            for (VersionArtefact va : draftVAs.get(vId)) {
                VersionArtefactDAO.deleteVersionArtefact(em(), va);
            }
        }
        draftVAs.clear();
    }

    /** {@inheritDoc} */
    @Override
    protected void notifyDeleteCurrentVersion(final Integer versionId) {
        for (VersionArtefact va : currentVAs.get(versionId)) {
            // TO DO: workflow deletion processing.
            // Make the existing row historical.
            TemporalUtils.makeHistorical(va, nowTime());
            va.setModifiedBy(modifiedBy());
            VersionArtefactDAO.updateVersionArtefact(em(), va);
        }
        // Remove from our own records.
        currentVAs.remove(versionId);
    }

    /** {@inheritDoc} */
    @Override
    protected void notifyDeleteDraftVersion(final Integer versionId) {
        for (VersionArtefact ap : draftVAs.get(versionId)) {
            // Just delete the row;
            // for a draft instance, no workflow is applied.
            VersionArtefactDAO.deleteVersionArtefact(em(), ap);
        }
        // Remove from our own records.
        draftVAs.remove(versionId);
    }

    /** {@inheritDoc}
     * As there are (currently) no user-visible version artefacts,
     * there's nothing to be done here!
     */
    @Override
    protected void applyChanges(final Vocabulary updatedVocabulary) {
    }

}
