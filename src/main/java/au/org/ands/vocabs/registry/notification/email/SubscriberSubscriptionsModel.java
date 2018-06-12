/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.notification.email;

import java.util.Map;
import java.util.Set;

import au.org.ands.vocabs.registry.notification.VocabularyDifferences;

/** An instance of this class contains all the information about a
 * subscriber and their subscription needed to "fill in the blanks"
 * of the email template.
 */
public class SubscriberSubscriptionsModel {

//    /* The subscriber's subscriber Id. */
//    private Integer subscriberId;

    /** The subscriber's token. */
    private String token;

    // Not yet needed.
//    /** Map of owner Ids to their short names. */
//    private Map<Integer, String> ownerNames;

    /** Map of owner Ids to their full names. */
    private Map<Integer, String> ownerFullNames;

    /** Map of owner Ids to sets of vocabulary Ids. */
    private Map<Integer, Set<Integer>> ownerVocabularies;

    /** Map of vocabulary Ids to the representation of the Registry
     * events recorded for that vocabulary. */
    private Map<Integer, VocabularyDifferences> vocabularyIdMap;

    /** Set of owner Ids for the owners that will be
     * included in the report. */
    private Set<Integer> allOwnerIdsToReport;

    /** Vocabulary Ids for vocabularies for which a report will
     * be given apart from its owner. */
    private Set<Integer> allIndividualVocabularySubscriptions;

    /** Map of system properties needed. */
    private Map<String, String> properties;

    /** Get the subscriber's token.
     * @return The subscriber's token.
     */
    public String getToken() {
        return token;
    }

    /** Set the subscriber's token.
     * @param aToken The subscriber's token to set.
     */
    public void setToken(final String aToken) {
        token = aToken;
    }

    // Not yet needed.
//    /** Get the value of ownerNames.
//     * @return The value of ownerNames.
//     */
//    public Map<Integer, String> getOwnerNames() {
//        return ownerNames;
//    }

    // Not yet needed.
//    /** Set the value of ownerNames.
//     * @param anOwnerNames The value of ownerNames to set.
//     */
//    public void setOwnerNames(final Map<Integer, String> anOwnerNames) {
//        ownerNames = anOwnerNames;
//    }

    /** Get the value of ownerFullNames.
     * @return The value of ownerFullNames.
     */
    public Map<Integer, String> getOwnerFullNames() {
        return ownerFullNames;
    }

    /** Set the value of ownerFullNames.
     * @param anOwnerFullNames The value of ownerFullNames to set.
     */
    public void setOwnerFullNames(final Map<Integer, String> anOwnerFullNames) {
        ownerFullNames = anOwnerFullNames;
    }

    /** Get the value of ownerVocabularies.
     * @return The value of ownerVocabularies.
     */
    public Map<Integer, Set<Integer>> getOwnerVocabularies() {
        return ownerVocabularies;
    }

    /** Set the value of ownerVocabularies.
     * @param anOwnerVocabularies The value of ownerVocabularies.
     */
    public void setOwnerVocabularies(final Map<Integer, Set<Integer>>
    anOwnerVocabularies) {
        ownerVocabularies = anOwnerVocabularies;
    }

    /** Get the value of vocabularyIdMap.
     * @return The value of vocabularyIdMap.
     */
    public Map<Integer, VocabularyDifferences> getVocabularyIdMap() {
        return vocabularyIdMap;
    }

    /** Set the value of vocabularyIdMap.
     * @param aVocabularyIdMap The value of vocabularyIdMap to set.
     */
    public void setVocabularyIdMap(
            final Map<Integer, VocabularyDifferences> aVocabularyIdMap) {
        vocabularyIdMap = aVocabularyIdMap;
    }

    /** Get the value of allOwnerIdsToReport.
     * @return The value of allOwnerIdsToReport.
     */
    public Set<Integer> getAllOwnerIdsToReport() {
        return allOwnerIdsToReport;
    }

    /** Set the value of allOwnerIdsToReport.
     * @param anAllOwnerIdsToReport The value of allOwnerIdsToReport to set.
     */
    public void setAllOwnerIdsToReport(final Set<Integer>
    anAllOwnerIdsToReport) {
        allOwnerIdsToReport = anAllOwnerIdsToReport;
    }

    /** Get the value of allIndividualVocabularySubscriptions.
     * @return The value of allIndividualVocabularySubscriptions.
     */
    public Set<Integer> getAllIndividualVocabularySubscriptions() {
        return allIndividualVocabularySubscriptions;
    }

    /** Set the value of allIndividualVocabularySubscriptions.
     * @param anAllIndividualVocabularySubscriptions The value of
     *      allIndividualVocabularySubscriptions to set.
     */
    public void setAllIndividualVocabularySubscriptions(
            final Set<Integer> anAllIndividualVocabularySubscriptions) {
        allIndividualVocabularySubscriptions =
                anAllIndividualVocabularySubscriptions;
    }

    /** Get the map of system properties.
     * @return The map of system properties.
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /** Set the map of system properties.
     * @param aProperties The map of system properties to set.
     */
    public void setProperties(final Map<String, String> aProperties) {
        properties = aProperties;
    }

}
