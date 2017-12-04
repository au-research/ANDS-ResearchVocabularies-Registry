/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.model;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.converter.VocabularyRegistrySchemaMapper;
import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.converter.VocabularyDbSchemaMapper;
import au.org.ands.vocabs.registry.db.dao.AccessPointDAO;
import au.org.ands.vocabs.registry.db.dao.VersionDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyIdDAO;
import au.org.ands.vocabs.registry.db.entity.AccessPoint;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.db.entity.clone.VocabularyClone;
import au.org.ands.vocabs.registry.enums.VocabularyStatus;

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

    /** List of all sub-models. */
    private List<ModelBase> subModels = new ArrayList<>();

    /** Map of the current versions and their access points. */
    private MultivaluedMap<Version, AccessPoint> currentVersionsAndAPs =
            new MultivaluedHashMap<>();

    /** Map of the draft versions and their access points. */
    private MultivaluedMap<Version, AccessPoint> draftVersionsAndAPs =
            new MultivaluedHashMap<>();

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
        if (VocabularyIdDAO.getVocabularyIdById(aVocabularyId) == null) {
            logger.error("Attempt to construct vocabulary model with "
                    + "invalid Id");
            throw new IllegalArgumentException(
                    "Attempt to construct vocabulary model with invalid Id");
        }
        setEm(anEm);
        setVocabularyId(aVocabularyId);
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
        currentVocabulary =
                VocabularyDAO.getCurrentVocabularyByVocabularyId(em(),
                        vocabularyId());
        List<Version> currentVersions =
                VersionDAO.getCurrentVersionListForVocabulary(em(),
                        vocabularyId());
        for (Version version : currentVersions) {
            List<AccessPoint> currentAPs =
                    AccessPointDAO.getCurrentAccessPointListForVersion(em(),
                            version.getVersionId());
            currentVersionsAndAPs.addAll(version, currentAPs);
        }

        // Draft
        List<Vocabulary> draftVocabularyList =
                VocabularyDAO.getDraftVocabularyByVocabularyId(em(),
                        vocabularyId());
        if (!draftVocabularyList.isEmpty()) {
            // There can be at most one draft vocabulary row.
            draftVocabulary = draftVocabularyList.get(0);
            List<Version> draftVersions =
                    VersionDAO.getCurrentVersionListForVocabulary(em(),
                            vocabularyId());
            for (Version version : draftVersions) {
                List<AccessPoint> draftAPs =
                        AccessPointDAO.getCurrentAccessPointListForVersion(
                                em(), version.getVersionId());
                draftVersionsAndAPs.addAll(version, draftAPs);
            }
        }

        // Sub-models
        vreModel = new VocabularyRelatedEntitiesModel(em(), vocabularyId());
        subModels.add(vreModel);
        vrvModel = new VocabularyRelatedVocabulariesModel(em(), vocabularyId());
        subModels.add(vrvModel);
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
            for (Version version : currentVersionsAndAPs.keySet()) {
                description.add("Current vocabulary has current version; "
                        + "version Id, Id: " + version.getVersionId()
                        + "," + version.getId());
                for (AccessPoint ap : currentVersionsAndAPs.get(version)) {
                    description.add("Current version has current AP; "
                            + "version Id, AP Id, Id: "
                            + version.getVersionId() + " ,"
                            + ap.getAccessPointId() + ","
                            + ap.getId());
                }
            }
        }
        if (draftVocabulary != null) {
            description.add("Has draft vocabulary; Id: "
                    + draftVocabulary.getId());
            for (Version version : draftVersionsAndAPs.keySet()) {
                description.add("Draft vocabulary has draft version; "
                        + "version Id, Id, meaning: "
                        + version.getVersionId() + ","
                        + version.getId() + ","
                        + TemporalUtils.getTemporalDescription(
                                version).getValue());
                for (AccessPoint ap : draftVersionsAndAPs.get(version)) {
                    description.add("Draft version has draft AP; "
                            + "version Id, AP Id, Id, meaning"
                            + version.getVersionId() + ","
                            + ap.getAccessPointId() + ","
                            + ap.getId() + ","
                            + TemporalUtils.getTemporalDescription(
                                    ap).getValue());
                }
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
        TemporalUtils.makeHistorical(currentVocabulary, nowTime());
        currentVocabulary.setModifiedBy(modifiedBy());
        VocabularyDAO.updateVocabulary(em(), currentVocabulary);
        currentVocabulary = null;
        // Sub-models.
        subModels.forEach(sm -> sm.deleteOnlyCurrent());
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
        TemporalUtils.makeHistorical(currentVocabulary, nowTime());
        currentVocabulary.setModifiedBy(modifiedBy());
        VocabularyDAO.updateVocabulary(em(), currentVocabulary);
        // Now make a new draft record.
        draftVocabulary = VocabularyClone.INSTANCE.
                clone(currentVocabulary);
        draftVocabulary.setModifiedBy(modifiedBy());
        TemporalUtils.makeDraft(draftVocabulary);
        VocabularyDAO.saveVocabulary(em(), draftVocabulary);
        currentVocabulary = null;
        // Sub-models.
        subModels.forEach(sm -> sm.makeCurrentIntoDraft());
    }

    /** {@inheritDoc} */
    @Override
    protected void deleteOnlyDraft() {
        if (draftVocabulary == null) {
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
        if (draftVocabulary != null) {
            em().remove(draftVocabulary);
            draftVocabulary = null;
        }
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
        } else {
            // Add a new draft record.
            draftVocabulary = mapper.sourceToTarget(updatedVocabulary,
                    nowTime());
            TemporalUtils.makeDraft(draftVocabulary);
            draftVocabulary.setModifiedBy(modifiedBy());
            VocabularyDAO.saveVocabulary(em(), draftVocabulary);
        }
    }

    /** Make the database's currently-valid view of the
     * VocabularyRelatedEntities match updatedVocabulary.
     * @param updatedVocabulary The updated vocabulary.
     */
    private void applyChangesCurrent(
            final au.org.ands.vocabs.registry.schema.vocabulary201701.
            Vocabulary updatedVocabulary) {
        if (currentVocabulary != null) {
            TemporalUtils.makeHistorical(currentVocabulary, nowTime());
            currentVocabulary.setModifiedBy(modifiedBy());
            VocabularyDAO.updateVocabulary(em(), currentVocabulary);
        }
        currentVocabulary = draftVocabulary;
        draftVocabulary = null;
        VocabularyRegistrySchemaMapper mapper =
                VocabularyRegistrySchemaMapper.INSTANCE;
        if (currentVocabulary != null) {
            // There was already a draft. Reuse it as the new current instance.
            mapper.updateTargetFromSource(
                    updatedVocabulary, currentVocabulary);
            currentVocabulary.setStartDate(nowTime());
            TemporalUtils.makeCurrentlyValid(currentVocabulary);
            currentVocabulary.setModifiedBy(modifiedBy());
            VocabularyDAO.updateVocabulary(em(), currentVocabulary);
        } else {
            // No existing current vocabulary. Make one.
            currentVocabulary = mapper.sourceToTarget(updatedVocabulary);
            currentVocabulary.setStartDate(nowTime());
            TemporalUtils.makeCurrentlyValid(currentVocabulary);
            currentVocabulary.setModifiedBy(modifiedBy());
            VocabularyDAO.saveVocabulary(em(), currentVocabulary);
        }
    }

}
