/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.notification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.builder.Diff;

import au.org.ands.vocabs.registry.enums.RegistryEventEventType;

/** Representation of the Registry events recorded for a vocabulary. */
public class VocabularyDifferences {

    /** The last-recorded event type for the vocabulary. This is used
     * to indicate and determine the high-level nature of what is to be
     * captured and reported for the vocabulary. E.g., if the value is {@link
     * RegistryEventEventType#DELETED}, there may not be any other details.
     *  */
    private RegistryEventEventType finalResult;

    /** The title to be used in reporting for the vocabulary. */
    private String title;

    /** Representation of the differences recorded for the top level
     * of the vocabulary. */
    private Set<String> vocabularyDiffs = new HashSet<>();

    /** Representation of the differences between the values of fields of
     * the vocabulary. */
    private List<Diff<?>> fieldDiffs;

    /** Representation of the differences recorded for the versions
     * of the vocabulary. Keys are version Ids. */
    private Map<Integer, VersionDifferences> versionDiffs = new HashMap<>();

    /** Get the value of finalResult.
     * @return The value of finalResult.
     */
    public RegistryEventEventType getFinalResult() {
        return finalResult;
    }

    /** Set the value of finalResult.
     * @param aFinalResult The value of finalResult to set.
     */
    public void setFinalResult(final RegistryEventEventType aFinalResult) {
        finalResult = aFinalResult;
    }

    /** Get the title of the vocabulary, as it is to be reported in
     * notifications.
     * @return The title to be reported.
     */
    public String getTitle() {
        return title;
    }

    /** Set the title of the vocabulary, as it is to be reported in
     * notifications.
     * @param aTitle The title to set.
     */
    public void setTitle(final String aTitle) {
        title = aTitle;
    }

    /** Get the list of differences recorded for the top level of the
     * vocabulary.
     * @return The list of top-level differences recorded for the vocabulary.
     */
    public Set<String> getVocabularyDiffs() {
        return vocabularyDiffs;
    }

    /** Add the details of a difference for the top level of the vocabulary.
     * @param aVocabularyDiff A difference for the top level of the vocabulary.
     */
    public void addVocabularyDiff(final String aVocabularyDiff) {
        vocabularyDiffs.add(aVocabularyDiff);
    }

    /** Get the list of differences between the values of fields
     * of the vocabulary.
     * @return The list of differences between the values of fields
     *      of the vocabulary, or null, if not applicable.
     */
    public List<Diff<?>> getFieldDiffs() {
        return fieldDiffs;
    }

    /** Set the list of differences between the values of fields
     * of the vocabulary.
     * @param aFieldDiffs The list of differences between
     *      the values of fields.
     */
    public void setFieldDiffs(final List<Diff<?>> aFieldDiffs) {
        fieldDiffs = aFieldDiffs;
    }

    /** Get the map of differences recorded for the versions of the vocabulary.
     * @return The map of differences recorded for the versions of the
     *      vocabulary.
     */
    public Map<Integer, VersionDifferences> getVersionDiffs() {
        return versionDiffs;
    }

    /** Require that the map of differences recorded for the versions
     * of the vocabulary contain an instance for a given version Id,
     * i.e., creating an instance of VersionDifferences as required.
     * @param versionId The version Id, for which there is required to be
     *      a VersionDifferences instance recorded.
     */
    public void requireVersionDiffsForVersion(final Integer versionId) {
        if (!versionDiffs.containsKey(versionId)) {
            versionDiffs.put(versionId, new VersionDifferences());
        }
    }

    /** Get the VersionDifferences instance recorded for a version.
     * @param versionId The version Id, for which the VersionDifferences
     *      instance is to be fetched.
     * @return The VersionDifferences instance recorded for the version, or
     *      null, if there is no such instance.
     */
    public VersionDifferences getVersionDiffsForVersion(
            final Integer versionId) {
        return versionDiffs.get(versionId);
    }

    /** Add the details of a difference for a version of the vocabulary.
     * @param versionId The version Id of the version for which the
     *      difference is to be recorded.
     * @param aVersionDiff A difference for the version.
     */
    public void addVersionDiff(final Integer versionId,
            final String aVersionDiff) {
        VersionDifferences versionDiff = versionDiffs.get(versionId);
        if (versionDiff == null) {
            versionDiff = new VersionDifferences();
            versionDiffs.put(versionId, versionDiff);
        }
        versionDiff.addVersionDiff(aVersionDiff);
    }

    /** Clean up the VersionDifferences instances, by pruning any
     * for which there are no reported differences.
     */
    public void cleanupVersionDiffs() {
        List<Integer> versionDifferencesToPrune = new ArrayList<>();
        for (Entry<Integer, VersionDifferences> versionDiffEntry
                : versionDiffs.entrySet()) {
            VersionDifferences verdiff = versionDiffEntry.getValue();
            if (verdiff.getFinalResult() == RegistryEventEventType.UPDATED
                    && verdiff.getVersionDiffs().isEmpty()
                    && verdiff.getFieldDiffs().isEmpty()) {
                versionDifferencesToPrune.add(versionDiffEntry.getKey());
            }
        }
        for (Integer versionId : versionDifferencesToPrune) {
            versionDiffs.remove(versionId);
        }
    }

    /** Is there nothing to include in the notification for this
     * vocabulary? This method only returns an accurate result
     * <i>after</i> running {@link #cleanupVersionDiffs()}.
     * @return true, if there is nothing to include in the notification
     *      for this vocabulary.
     */
    public boolean isEmpty() {
        return finalResult == RegistryEventEventType.UPDATED
                && vocabularyDiffs.isEmpty()
                && fieldDiffs.isEmpty()
                && versionDiffs.isEmpty();
    }

}
