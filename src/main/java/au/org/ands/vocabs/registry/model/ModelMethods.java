/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.model;

import java.time.LocalDateTime;

import javax.persistence.EntityManager;

import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;

/** The interface provided by the model package. API and methods
 * in other parts of the registry should invoke the methods of this class,
 * rather than directly invoking the methods of the model classes
 * in this package.
 */
public final class ModelMethods {

    /** Private constructor for a utility class. */
    private ModelMethods() {
    }

    /** Create an instance of the Vocabulary Model for a vocabulary.
     * @param em The EntityManager to be used to fetch and update
     *      database data.
     * @param vocabularyId The Id of the vocabulary for which the model
     *      is to be constructed.
     * @return The created and populated VocabularyModel instance.
     */
    public static VocabularyModel createVocabularyModel(final EntityManager em,
            final Integer vocabularyId) {
        return new VocabularyModel(em, vocabularyId);
    }

    /** Get the current instance of the vocabulary, in registry schema
     * format. If there is no current instance, null is returned.
     * @param vm The VocabularyModel representing the vocabulary.
     * @param includeVersions Whether or not to include version elements.
     * @param includeAccessPoints Whether or not to include access point
     *      elements.
     * @param includeRelatedEntitiesAndVocabularies Whether or not to include
     *      full related entity elements, and top-level details of
     *      related vocabularies.
     * @return The current instance of the vocabulary, in registry schema
     *      format, if there is a current instance; null, otherwise.
     */
    public static Vocabulary getCurrent(final VocabularyModel vm,
            final boolean includeVersions,
            final boolean includeAccessPoints,
            final boolean includeRelatedEntitiesAndVocabularies) {
        if (!vm.hasCurrent()) {
            return null;
        }
        return vm.getCurrent(includeVersions, includeAccessPoints,
                includeRelatedEntitiesAndVocabularies);
    }

    /** Get the draft instance of the vocabulary, in registry schema
     * format. If there is no draft instance, null is returned.
     * @param vm The VocabularyModel representing the vocabulary.
     * @param includeVersions Whether or not to include version elements.
     * @param includeAccessPoints Whether or not to include access point
     *      elements.
     * @param includeRelatedEntitiesAndVocabularies Whether or not to include
     *      full related entity elements, and top-level details of
     *      related vocabularies.
     * @return The draft instance of the vocabulary, in registry schema
     *      format, if there is a draft instance; null, otherwise.
     */
    public static Vocabulary getDraft(final VocabularyModel vm,
            final boolean includeVersions,
            final boolean includeAccessPoints,
            final boolean includeRelatedEntitiesAndVocabularies) {
        if (!vm.hasDraft()) {
            return null;
        }
        return vm.getDraft(includeVersions, includeAccessPoints,
                includeRelatedEntitiesAndVocabularies);
    }

    /** Delete only the current version of a vocabulary.
     * If there is a draft, the draft is preserved.
     * @param vm The VocabularyModel representing the vocabulary.
     * @param modifiedBy The value to use for "modifiedBy" when adding
     *      or updating rows of the database.
     * @param nowTime The time to use as the value of "now" when
     *      applying changes to rows of the database.
     */
    public static void deleteOnlyCurrentVocabulary(final VocabularyModel vm,
            final String modifiedBy, final LocalDateTime nowTime) {
        vm.setModifiedBy(modifiedBy);
        vm.setNowTime(nowTime);
        vm.deleteOnlyCurrent();
    }

    /** Delete only the draft version of a vocabulary.
     * If there is a current instance, it is is preserved.
     * @param vm The VocabularyModel representing the vocabulary.
     * @param modifiedBy The value to use for "modifiedBy" when adding
     *      or updating rows of the database.
     * @param nowTime The time to use as the value of "now" when
     *      applying changes to rows of the database.
     */
    public static void deleteOnlyDraftVocabulary(final VocabularyModel vm,
            final String modifiedBy, final LocalDateTime nowTime) {
        vm.setModifiedBy(modifiedBy);
        vm.setNowTime(nowTime);
        vm.deleteOnlyDraft();
    }

    /** For a vocabulary that has a current instance, make
     * that instance into a draft only. This is akin to "unpublishing"
     * the vocabulary but retaining it as a draft.
     * If there is an existing draft, don't use this method: use {@link
     *  #deleteOnlyCurrentVocabulary(VocabularyModel, String, LocalDateTime)}
     * instead.
     * @param vm The VocabularyModel representing the vocabulary.
     * @param modifiedBy The value to use for "modifiedBy" when adding
     *      or updating rows of the database.
     * @param nowTime The time to use as the value of "now" when
     *      applying changes to rows of the database.
     */
    public static void makeCurrentVocabularyDraft(final VocabularyModel vm,
            final String modifiedBy, final LocalDateTime nowTime) {
        vm.setModifiedBy(modifiedBy);
        vm.setNowTime(nowTime);
        vm.makeCurrentIntoDraft();
    }

    /** Apply changes to a vocabulary model using data specified
     * in registry schema format.
     * @param vm The VocabularyModel representing the vocabulary.
     * @param modifiedBy The value to use for "modifiedBy" when adding
     *      or updating rows of the database.
     * @param nowTime The time to use as the value of "now" when
     *      applying changes to rows of the database.
     * @param updatedVocabulary The updates to be applied to the model.
     */
    public static void applyChanges(final VocabularyModel vm,
            final String modifiedBy, final LocalDateTime nowTime,
            final Vocabulary updatedVocabulary) {
        vm.setModifiedBy(modifiedBy);
        vm.setNowTime(nowTime);
        vm.applyChanges(updatedVocabulary);
    }

}
