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

    /** Delete only the current instance of a vocabulary.
     * If there is no current instance, there is nothing to be done.
     * Otherwise, if there is a draft, the draft is preserved.
     */
    public void deleteOnlyCurrent() {
        if (currentVocabulary == null) {
            // Oops, nothing to do!
            return;
        }
        makeCurrentHistorical(draftVocabulary != null);
    }

    /** For a vocabulary that has a current instance, make
     * that instance into a draft.
     * If there is no current instance, there is nothing to be done.
     * If there is an existing draft, don't use this method: you should use
     * {@link
     *  #deleteOnlyCurrentVocabulary(VocabularyModel, String, LocalDateTime)}
     * instead. However, if there is an existing draft, and you do call
     * this method anyway, it behaves in the same way as that other method.
     */
    public void makeCurrentVocabularyDraft() {
        if (currentVocabulary == null) {
            // Oops, nothing to do!
            return;
        }
        makeCurrentHistorical(true);
    }

    /** {@inheritDoc} */
    @Override
    public void makeCurrentHistorical(final boolean preserveAsDraft) {
        if (currentVocabulary != null) {
            TemporalUtils.makeHistorical(currentVocabulary, nowTime());
            currentVocabulary.setModifiedBy(modifiedBy());
            VocabularyDAO.updateVocabulary(
                        em(), currentVocabulary);
            if (preserveAsDraft) {
                if (draftVocabulary == null) {
                    // No existing draft row; add one.
                    draftVocabulary = VocabularyClone.INSTANCE.
                            clone(currentVocabulary);
                    draftVocabulary.setModifiedBy(modifiedBy());
                    TemporalUtils.makeDraftAdditionOrModification(
                            draftVocabulary);
                    VocabularyDAO.saveVocabulary(em(), draftVocabulary);
                }
            }
            // Sub-models.
            subModels.forEach(sm ->
                sm.makeCurrentHistorical(preserveAsDraft));
        }
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
    public void applyChanges(
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
                    updatedVocabulary, draftVocabulary);
            draftVocabulary.setModifiedBy(modifiedBy());
            VocabularyDAO.updateVocabulary(em(), draftVocabulary);
        } else {
            // Add a new draft record.
            draftVocabulary = mapper.sourceToTarget(updatedVocabulary);
            TemporalUtils.makeDraftAdditionOrModification(draftVocabulary);
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
