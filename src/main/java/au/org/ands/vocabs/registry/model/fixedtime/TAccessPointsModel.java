/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.model.fixedtime;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.sequence.CommandVisitor;
import org.apache.commons.collections4.sequence.SequencesComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.converter.AccessPointDbSchemaMapper;
import au.org.ands.vocabs.registry.db.dao.AccessPointDAO;
import au.org.ands.vocabs.registry.db.entity.AccessPoint;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.enums.ApSource;
import au.org.ands.vocabs.registry.enums.RegistryEventEventType;
import au.org.ands.vocabs.registry.model.sequence.AccessPointElement;
import au.org.ands.vocabs.registry.notification.VersionDifferences;
import au.org.ands.vocabs.registry.notification.VocabularyDifferences;

/** Fixed-time access points domain model.
 * This is a representation of the access points of a vocabulary,
 * as an abstract data type.
 */
public class TAccessPointsModel extends TModelBase {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** The parent VocabularyModel of this instance. Passed down
     * by VersionsModel. */
    @SuppressWarnings("unused")
    private TVocabularyModel vocabularyModel;

    /** The parent VersionsModel of this instance. Passed down
     * by VersionsModel. */
    @SuppressWarnings("unused")
    private TVersionsModel versionsModel;

    /** The instances of versions, if there are any.
     * The keys are version Ids. Passed down by VersionsModel. */
    private Map<Integer, Version> versions;

    /** The instances of access points, if there are any.
     * The keys are version Ids. */
    private MultivaluedMap<Integer, AccessPoint> aps =
            new MultivaluedHashMap<>();

    /** Construct access points model for a vocabulary.
     * @param anEm The EntityManager to be used to fetch and update
     *      database data.
     * @param aVocabularyId The Id of the vocabulary for which the model
     *      is to be constructed.
     * @param nowTime The fixed time to use to fetch the database entities.
     * @param aVocabularyModel The parent VocabularyModel of this instance.
     * @param aVersionsModel The parent VersionsModel of this instance.
     * @param aVersions The fixed-time version instances of the vocabulary.
     * @throws IllegalArgumentException If aVocabularyId is null.
     */
    public TAccessPointsModel(final EntityManager anEm,
            final Integer aVocabularyId,
            final LocalDateTime nowTime,
            final TVocabularyModel aVocabularyModel,
            final TVersionsModel aVersionsModel,
            final Map<Integer, Version> aVersions) {
        if (aVocabularyId == null) {
            logger.error("Attempt to construct access points model with no Id");
            throw new IllegalArgumentException(
                    "Attempt to construct access points model with no Id");
        }
        setEm(anEm);
        setVocabularyId(aVocabularyId);
        setFixedTime(nowTime);
        vocabularyModel = aVocabularyModel;
        versionsModel = aVersionsModel;
        versions = aVersions;
        populateModel();
    }

    /** {@inheritDoc} */
    @Override
    protected void populateModel() {
        for (Integer versionId : versions.keySet()) {
            aps.addAll(versionId,
                    AccessPointDAO.getFixedTimeAccessPointListForVersion(em(),
                            versionId, fixedTime()));
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
        description.add("AP | fixed Time; Id: " + fixedTime());
        if (aps != null) {
            for (Integer vId : aps.keySet()) {
                for (AccessPoint ap : ListUtils.emptyIfNull(
                        aps.get(vId))) {
                    description.add("AP | Fixed time version has AP; "
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

    /** Get the map of access points for this instance.
     * @return The map of access points for this instance.
     */
    protected MultivaluedMap<Integer, AccessPoint> getAPs() {
        return aps;
    }

    /** {@inheritDoc}
     * The top-level metadata for versions must already have been
     * inserted into outputVocabulary before invoking this method.
     *  */
    @Override
    protected void insertIntoSchema(final
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
            List<AccessPoint> currentAPList = aps.get(versionId);
            // It shouldn't be empty, but just in case, do a null check.
            if (currentAPList != null) {
                for (AccessPoint ap : currentAPList) {
                    outputAPList.add(apDbMapper.sourceToTarget(ap));
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void diff(final TVocabularyModel tvmToDiff,
            final VocabularyDifferences vdiff) {
        // Create sequences.
        // As we do, we only pay attention to versions that have been
        // diagnosed by TVersionsModel.diff() as UPDATED.
        // We don't report anything for
        // versions that have been diagnosed as CREATED or DELETED.
        // First, the values of this instance.
        List<AccessPointElement> thisSequence = new ArrayList<>();
        for (Entry<Integer, List<AccessPoint>> apList : aps.entrySet()) {
            Integer versionId = apList.getKey();
            if (vdiff.getVersionDiffsForVersion(versionId).getFinalResult()
                    == RegistryEventEventType.UPDATED) {
                for (AccessPoint ap : ListUtils.emptyIfNull(
                        apList.getValue())) {
                    // Only consider those access points with source=USER.
                    if (ap.getSource() == ApSource.USER) {
                        thisSequence.add(new AccessPointElement(
                                versionId, ap.getAccessPointId(), ap, null));
                    }
                }
            }
        }
        Collections.sort(thisSequence);

        // And now, the values of the other instance.
        MultivaluedMap<Integer, AccessPoint> otherAPs =
                tvmToDiff.getTVersionsModel().getTAccessPointsModel().getAPs();

        List<AccessPointElement> otherSequence = new ArrayList<>();
        for (Entry<Integer, List<AccessPoint>> apList : otherAPs.entrySet()) {
            Integer versionId = apList.getKey();
            if (vdiff.getVersionDiffsForVersion(versionId).getFinalResult()
                    == RegistryEventEventType.UPDATED) {
                for (AccessPoint ap : ListUtils.emptyIfNull(
                        apList.getValue())) {
                    // Only consider those access points with source=USER.
                    if (ap.getSource() == ApSource.USER) {
                        otherSequence.add(new AccessPointElement(
                                versionId, ap.getAccessPointId(), ap, null));
                    }
                }
            }
        }
        Collections.sort(otherSequence);

        // Compute difference.
        final SequencesComparator<AccessPointElement> comparator =
                new SequencesComparator<>(
                        thisSequence, otherSequence);
        // Apply the changes.
        comparator.getScript().visit(new DiffVisitor(otherAPs, vdiff));
    }

    /** Visitor class that registers the differences between the access points
     * of two instances of the vocabulary at different fixed times.  */
    private class DiffVisitor
        implements CommandVisitor<AccessPointElement> {

        /** The map of the access points of the other instance of the
         * vocabulary. Not needed for now. May be needed in future, if
         * we support changes to user-specified access points. */
        @SuppressWarnings("unused")
        private MultivaluedMap<Integer, AccessPoint> otherAPs;

        /** The VocabularyDifferences instance into which differences
         * are to be reported. */
        private VocabularyDifferences vdiff;

        /** Constructor.
         * @param anOtherAPs The map of the access points of the other
         *      instance of the vocabulary.
         * @param aVdiff The VocabularyDifferences instance into which
         *      differences are to be reported.
         */
        DiffVisitor(final MultivaluedMap<Integer, AccessPoint> anOtherAPs,
                final VocabularyDifferences aVdiff) {
            otherAPs = anOtherAPs;
            vdiff = aVdiff;
        }

        /** {@inheritDoc} */
        @Override
        public void visitKeepCommand(final AccessPointElement ape) {
            // The metadata of a user-specified access point doesn't
            // change. So there's nothing to report in this case.
        }

        /** {@inheritDoc} */
        @Override
        public void visitDeleteCommand(final AccessPointElement ape) {
            Integer versionId = ape.getVersionId();
            VersionDifferences verdiffs =
                    vdiff.getVersionDiffsForVersion(versionId);
            verdiffs.addVersionDiff(
                    "A user-specified access point was deleted");
        }

        /** {@inheritDoc} */
        @Override
        public void visitInsertCommand(final AccessPointElement ape) {
            Integer versionId = ape.getVersionId();
            VersionDifferences verdiffs =
                    vdiff.getVersionDiffsForVersion(versionId);
            verdiffs.addVersionDiff(
                    "A user-specified access point was added");
        }
    }

}
