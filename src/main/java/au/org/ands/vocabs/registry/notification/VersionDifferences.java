/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.notification;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.builder.Diff;

import au.org.ands.vocabs.registry.enums.RegistryEventEventType;

/** Representation of the differences recorded for a version. */
public class VersionDifferences {

    /** What is considered to be the state of the version at the
     * end of the reporting period, in comparison with the state of
     * the version as it was at the start of the reporting period.
     * This is used to indicate and determine the high-level nature of
     * what is to be captured and reported for the version.
     * E.g., if the value is {@link RegistryEventEventType#DELETED},
     * there may not be any other details.
     */
    private RegistryEventEventType finalResult;

    /** The title to be used in reporting for the version. */
    private String title;

    /** Representation of the differences recorded for the top level
     * of the version. The value is a {@link LinkedHashSet}, which is a set
     * whose iteration order corresponds to the order of insertion. */
    private Set<String> versionDiffs = new LinkedHashSet<>();

    /** Representation of the differences between the values of fields of
     * the version. Initialized to an empty list so that TVersionsModel
     * doesn't have to worry about setting it for the cases of
     * version creation and deletion. */
    private List<Diff<?>> fieldDiffs = new ArrayList<>();

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

    /** Get the title of the version, as it is to be reported in
     * notifications.
     * @return The title to be reported.
     */
    public String getTitle() {
        return title;
    }

    /** Set the title of the version, as it is to be reported in
     * notifications.
     * @param aTitle The title to set.
     */
    public void setTitle(final String aTitle) {
        title = aTitle;
    }

    /** Get the set of differences recorded for the top level of the
     * version.
     * @return The set of top-level differences recorded for the version.
     */
    public Set<String> getVersionDiffs() {
        return versionDiffs;
    }

    /** Add the details of a difference for the top level of the version.
     * @param aVersionDiff A difference for the top level of the version.
     */
    public void addVersionDiff(final String aVersionDiff) {
        versionDiffs.add(aVersionDiff);
    }

    /** Get the list of differences between
     * the values of fields of the version.
     * @return The list of differences between
     *      the values of fields of the version, or null, if not applicable.
     */
    public List<Diff<?>> getFieldDiffs() {
        return fieldDiffs;
    }

    /** Set the list of differences differences between
     * the values of fields of the version.
     * @param aFieldDiffs The list of differences between
     *      the values of fields.
     */
    public void setFieldDiffs(final List<Diff<?>> aFieldDiffs) {
        fieldDiffs = aFieldDiffs;
    }

}
