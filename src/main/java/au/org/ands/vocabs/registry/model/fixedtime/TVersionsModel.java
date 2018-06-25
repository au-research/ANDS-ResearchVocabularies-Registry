/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.model.fixedtime;

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
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.builder.Diff;
import org.apache.commons.lang3.builder.DiffResult;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.converter.VersionDbSchemaMapper;
import au.org.ands.vocabs.registry.db.dao.VersionDAO;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.internal.VersionJson;
import au.org.ands.vocabs.registry.enums.RegistryEventEventType;
import au.org.ands.vocabs.registry.log.utils.EntityDiffUtils;
import au.org.ands.vocabs.registry.model.sequence.VersionElement;
import au.org.ands.vocabs.registry.notification.VersionDifferences;
import au.org.ands.vocabs.registry.notification.VocabularyDifferences;

/** Fixed-time Versions domain model.
 * This is a representation of the versions of a vocabulary,
 * as an abstract data type.
 */
public class TVersionsModel extends TModelBase {

    /* This part of the model consists of the following
     * temporally-determined entities:
     *
     *   Version
     *     AccessPoint
     */

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** The parent VocabularyModel of this instance. Passed in to
     * this class's constructor, and passed down to constructors
     * of sub-models. */
    private TVocabularyModel vocabularyModel;

    /** The instances of versions, if there are any.
     * The keys are version Ids. */
    private Map<Integer, Version> versions = new HashMap<>();

    /** The model of the AccessPoints. */
    private TAccessPointsModel apModel;

    /** List of all sub-models. */
    private List<TModelBase> subModels = new ArrayList<>();

    /** Construct versions model for a vocabulary.
     * @param anEm The EntityManager to be used to fetch and update
     *      database data.
     * @param aVocabularyId The Id of the vocabulary for which the model
     *      is to be constructed.
     * @param aVocabularyModel The parent VocabularyModel of this instance.
     * @param nowTime The fixed time to use to fetch the database entities.
     * @throws IllegalArgumentException If aVocabularyId is null.
     */
    public TVersionsModel(final EntityManager anEm,
            final Integer aVocabularyId,
            final LocalDateTime nowTime,
            final TVocabularyModel aVocabularyModel) {
        if (aVocabularyId == null) {
            logger.error("Attempt to construct versions model with no Id");
            throw new IllegalArgumentException(
                    "Attempt to construct versions model with no Id");
        }
        setEm(anEm);
        setVocabularyId(aVocabularyId);
        setFixedTime(nowTime);
        vocabularyModel = aVocabularyModel;
        populateModel();
    }

    /** {@inheritDoc} */
    @Override
    protected void populateModel() {
        List<Version> versionList =
                VersionDAO.getFixedTimeVersionListForVocabulary(
                        em(), vocabularyId(), fixedTime());
        for (Version version : versionList) {
            versions.put(version.getVersionId(), version);
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
        apModel = new TAccessPointsModel(em(), vocabularyId(), fixedTime(),
                vocabularyModel, this, versions);
        subModels.add(apModel);
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
        if (!versions.isEmpty()) {
            for (Version version : versions.values()) {
                description.add("Versions | Fixed time version; "
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

    /** Get the map of versions for this instance.
     * @return The map of versions for this instance.
     */
    protected Map<Integer, Version> getVersions() {
        return versions;
    }

    /** Get the TAccessPointsModel instance for this instance.
     * @return The TAccessPointsModel instance for this instance.
     */
    protected TAccessPointsModel getTAccessPointsModel() {
        return apModel;
    }

    /** {@inheritDoc} */
    @Override
    protected void insertIntoSchema(final
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
                || versions.isEmpty()) {
            return;
        }

        List<au.org.ands.vocabs.registry.schema.vocabulary201701.Version>
        outputVersions = outputVocabulary.getVersion();
        au.org.ands.vocabs.registry.schema.vocabulary201701.Version
        outputVersion;

        VersionDbSchemaMapper mapper = VersionDbSchemaMapper.INSTANCE;

        for (Version version : versions.values()) {
            outputVersion = mapper.sourceToTarget(version);
            outputVersions.add(outputVersion);
        }
        // Sub-models.
        subModels.forEach(sm -> sm.insertIntoSchema(outputVocabulary,
                includeVersions, includeAccessPoints,
                includeRelatedEntitiesAndVocabularies));
    }

    /** {@inheritDoc} */
    @Override
    protected void diff(final TVocabularyModel tvmToDiff,
            final VocabularyDifferences vdiff) {
        // Create sequences.
        // First, the values of this instance.
        List<VersionElement> thisSequence = new ArrayList<>();
        for (Entry<Integer, Version> versionEntry : versions.entrySet()) {
            thisSequence.add(new VersionElement(
                    versionEntry.getKey(), versionEntry.getValue(), null));
        }
        Collections.sort(thisSequence);

        // And now, the values of the other instance.
        Map<Integer, Version> otherVersions = tvmToDiff.getTVersionsModel().
                getVersions();

        List<VersionElement> otherSequence = new ArrayList<>();
        for (Entry<Integer, Version> versionEntry : otherVersions.entrySet()) {
            otherSequence.add(new VersionElement(
                    versionEntry.getKey(), versionEntry.getValue(), null));
        }
        Collections.sort(otherSequence);

        // Compute difference.
        final SequencesComparator<VersionElement> comparator =
                new SequencesComparator<>(thisSequence, otherSequence);
        // Apply the changes.
        comparator.getScript().visit(new DiffVisitor(otherVersions, vdiff));

        subModels.forEach(sm -> sm.diff(tvmToDiff, vdiff));
    }

    /** Visitor class that registers the differences between the versions
     * of two instances of the vocabulary at different fixed times. */
    private class DiffVisitor
        implements CommandVisitor<VersionElement> {

        /** The map of the versions of the other instance of the vocabulary. */
        private Map<Integer, Version> otherVersions;

        /** The VocabularyDifferences instance into which differences
         * are to be reported. */
        private VocabularyDifferences vdiff;

        /** Constructor.
         * @param anOtherVersions The map of the versions of the other
         *      instance of the vocabulary.
         * @param aVdiff The VocabularyDifferences instance into which
         *      differences are to be reported.
         */
        DiffVisitor(final Map<Integer, Version> anOtherVersions,
                final VocabularyDifferences aVdiff) {
            otherVersions = anOtherVersions;
            vdiff = aVdiff;
        }

        /** {@inheritDoc} */
        @Override
        public void visitKeepCommand(final VersionElement ve) {
            // This could contain metadata updates.
            // ... Or, maybe not.
            // But we create a VersionDifferences instance in any case,
            // because we will go on to visit TAccessPointsModel.diff(),
            // which assumes its existence.
            Integer versionId = ve.getVersionId();

            DiffResult diffResult = EntityDiffUtils.diffVersions(
                    versions.get(versionId), otherVersions.get(versionId));

            vdiff.requireVersionDiffsForVersion(versionId);
            VersionDifferences verdiffs =
                    vdiff.getVersionDiffsForVersion(versionId);
            VersionJson versionJson =
                    JSONSerialization.deserializeStringAsJson(
                            ve.getDbVersion().getData(), VersionJson.class);
            // We set to UPDATED, because it's neither CREATED nor
            // DELETED. But that doesn't mean there was actually a change!
            verdiffs.setFinalResult(RegistryEventEventType.UPDATED);
            verdiffs.setTitle(versionJson.getTitle());
            // Post-process diffResult to extract difference information
            // that is to be reported separately.
            // Only copy into diffList the differences _not_ extracted.
            List<Diff<?>> diffList = new ArrayList<>();
            for (Diff<?> diff : diffResult) {
                switch (diff.getFieldName()) {
                case EntityDiffUtils.STATUS:
                    verdiffs.addVersionDiff("Status updated to "
                            + WordUtils.capitalizeFully(
                                    diff.getRight().toString()));
                    break;
                case EntityDiffUtils.POOLPARTY_HARVEST:
                    // Decision made 2016-06-25 _not_ to report changes.
                    // If the user changes this flag, they will have to
                    // change something _else_ as well, so they will
                    // get the report for _that_.
                    break;
                case EntityDiffUtils.IMPORT:
                    if (BooleanUtils.isTrue((Boolean) diff.getRight())) {
                        verdiffs.addVersionDiff(
                                "Version published via a SPARQL endpoint");
                    } else {
                        verdiffs.addVersionDiff(
                                "Version no longer published via a "
                                + "SPARQL endpoint");
                    }
                    break;
                case EntityDiffUtils.PUBLISH:
                    if (BooleanUtils.isTrue((Boolean) diff.getRight())) {
                        verdiffs.addVersionDiff(
                                "Version published via the Linked Data API");
                    } else {
                        verdiffs.addVersionDiff(
                                "Version no longer published via the "
                                + "Linked Data API");
                    }
                    break;
                case EntityDiffUtils.SLUG:
                    // Decision made 2016-06-25 _not_ to report slug changes.
                    break;
                default:
                    diffList.add(diff);
                }
            }
            verdiffs.setFieldDiffs(diffList);
        }

        /** {@inheritDoc} */
        @Override
        public void visitDeleteCommand(final VersionElement ve) {
            Integer versionId = ve.getVersionId();

            vdiff.requireVersionDiffsForVersion(versionId);
            VersionDifferences verdiffs =
                    vdiff.getVersionDiffsForVersion(versionId);
            VersionJson versionJson =
                    JSONSerialization.deserializeStringAsJson(
                            ve.getDbVersion().getData(), VersionJson.class);
            verdiffs.setFinalResult(RegistryEventEventType.DELETED);
            verdiffs.setTitle(versionJson.getTitle());
            verdiffs.addVersionDiff("Deleted");
        }

        /** {@inheritDoc} */
        @Override
        public void visitInsertCommand(final VersionElement ve) {
            Integer versionId = ve.getVersionId();
            Version version = ve.getDbVersion();
            vdiff.requireVersionDiffsForVersion(versionId);
            VersionDifferences verdiffs =
                    vdiff.getVersionDiffsForVersion(versionId);
            VersionJson versionJson =
                    JSONSerialization.deserializeStringAsJson(
                            version.getData(), VersionJson.class);
            verdiffs.setFinalResult(RegistryEventEventType.CREATED);
            verdiffs.setTitle(versionJson.getTitle());
            verdiffs.addVersionDiff("Added");
            verdiffs.addVersionDiff("Status set to "
                    + WordUtils.capitalizeFully(
                            version.getStatus().toString()));
        }
    }

}
