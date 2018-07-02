/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.model.fixedtime;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.builder.Diff;
import org.apache.commons.lang3.builder.DiffResult;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.converter.VocabularyDbSchemaMapper;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyIdDAO;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.db.internal.VocabularyJson;
import au.org.ands.vocabs.registry.log.utils.EntityDiffUtils;
import au.org.ands.vocabs.registry.notification.VocabularyDifferences;

/** Fixed-time Vocabulary domain model.
 * This is a representation of a vocabulary as an abstract data type.
 */
public class TVocabularyModel extends TModelBase {

    /* The model consists of the following temporally-determined entities:
     *
     * Vocabulary
     *   Version
     *     AccessPoint
     */

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** The instance of the vocabulary entity, if there is one. */
    private Vocabulary vocabulary;

    /** The model of the Versions. */
    private TVersionsModel versionsModel;

    /** List of all sub-models. */
    private List<TModelBase> subModels = new ArrayList<>();

    /** Construct vocabulary model for a vocabulary.
     * @param anEm The EntityManager to be used to fetch and update
     *      database data.
     * @param aVocabularyId The Id of the vocabulary for which the model
     *      is to be constructed.
     * @param nowTime The fixed time to use to fetch the database entities.
     * @throws IllegalArgumentException If aVocabularyId is null.
     */
    public TVocabularyModel(final EntityManager anEm,
            final Integer aVocabularyId,
            final LocalDateTime nowTime) {
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
        setFixedTime(nowTime);
        populateModel();
    }

    /** Get the instance of the vocabulary, or null, if there isn't one.
     * @return The instance of the vocabulary, or null.
     */
    protected Vocabulary getVocabulary() {
        return vocabulary;
    }

    /** Convenience method for reporting the title, as contained in the
     * vocabulary instance.
     * @return The vocabulary title, or null, if the vocabulary instance
     *      is null.
     */
    public String getVocabularyTitle() {
        if (vocabulary == null) {
            return null;
        }
        return JSONSerialization.deserializeStringAsJson(vocabulary.getData(),
                VocabularyJson.class).getTitle();
    }

    /** Convenience method for reporting the owner, as contained in the
     * vocabulary instance.
     * @return The vocabulary owner, or null, if the vocabulary instance
     *      is null.
     */
    public String getVocabularyOwner() {
        if (vocabulary == null) {
            return null;
        }
        return vocabulary.getOwner();
    }

    /** {@inheritDoc} */
    @Override
    protected void populateModel() {
        vocabulary =
                VocabularyDAO.getFixedTimeVocabularyByVocabularyId(em(),
                        vocabularyId(), fixedTime());

        // Sub-models
        versionsModel = new TVersionsModel(em(), vocabularyId(), fixedTime(),
                this);
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
        if (vocabulary != null) {
            description.add("Has vocabulary instance; Id: "
                    + vocabulary.getId());
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

    /** Query if this model is empty, i.e., if there is no instance
     * of the vocabulary at the fixed time..
     * @return True, iff the model is empty at the fixed time.
     */
    public boolean isEmpty() {
        return vocabulary == null;
    }

    /** Get the TVersionsModel instance for this instance.
     * @return The TVersionsModel instance for this instance.
     */
    protected TVersionsModel getTVersionsModel() {
        return versionsModel;
    }

    /** Get the fixed-time instance of the vocabulary, in registry schema
     * format. If there is no instance, null is returned.
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
    getFixedTimeInstance(final boolean includeVersions,
            final boolean includeAccessPoints,
            final boolean includeRelatedEntitiesAndVocabularies) {
        if (vocabulary == null) {
            return null;
        }
        au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
        outputVocabulary;

        VocabularyDbSchemaMapper mapper = VocabularyDbSchemaMapper.INSTANCE;
        outputVocabulary = mapper.sourceToTarget(vocabulary, false);
        // Sub-models.
        subModels.forEach(sm -> sm.insertIntoSchema(outputVocabulary,
                includeVersions, includeAccessPoints,
                includeRelatedEntitiesAndVocabularies));
        return outputVocabulary;
    }

    /** {@inheritDoc} */
    @Override
    protected void insertIntoSchema(final
            au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
            outputVocabulary,
            final boolean includeVersions,
            final boolean includeAccessPoints,
            final boolean includeRelatedEntitiesAndVocabularies) {
        // No action here. Everything else happens in sub-models.
    }

    /** {@inheritDoc} */
    @Override
    protected void diff(final TVocabularyModel tvmToDiff,
            final VocabularyDifferences vdiff) {
        Vocabulary finalVocabulary = tvmToDiff.getVocabulary();
        if (vocabulary == null) {
            if (finalVocabulary == null) {
                // Nothing doin'.
                return;
            }
            // This is a creation.
            vdiff.addVocabularyDiff("The vocabulary was created");
            // For now, no more to this report, so don't bother to fall through
            // to sub-model processing.
            return;
        } else if (finalVocabulary == null) {
            // This is a deletion.
            vdiff.addVocabularyDiff("The vocabulary was deleted");
            // For now, no more to this report, so don't bother to fall through
            // to sub-model processing.
            return;
        } else {
            // This is an update.
            DiffResult diffResult = EntityDiffUtils.diffVocabularies(
                    vocabulary, finalVocabulary);
            // Post-process diffResult to extract difference information
            // that is to be reported separately.
            // Only copy into diffList the differences _not_ extracted.
            List<Diff<?>> diffList = new ArrayList<>();
            for (Diff<?> diff : diffResult) {
                switch (diff.getFieldName()) {
                case EntityDiffUtils.STATUS:
                    vdiff.addVocabularyDiff("Status updated to "
                            + WordUtils.capitalizeFully(
                                    diff.getRight().toString()));
                    break;
                default:
                    diffList.add(diff);
                }
            }
            vdiff.setFieldDiffs(diffList);
        }
        // In practice, we've only reached this point if this is an update.
        subModels.forEach(sm -> sm.diff(tvmToDiff, vdiff));
        // And now do a cleanup.
        vdiff.cleanupVersionDiffs();
    }

}
