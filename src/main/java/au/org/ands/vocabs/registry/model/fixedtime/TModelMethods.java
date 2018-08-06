/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.model.fixedtime;

import java.time.LocalDateTime;

import javax.persistence.EntityManager;

import au.org.ands.vocabs.registry.notification.VocabularyDifferences;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;

/** The interface provided by the fixed-time model package. API and methods
 * in other parts of the registry should invoke the methods of this class,
 * rather than directly invoking the methods of the model classes
 * in this package.
 */
public final class TModelMethods {

    /** Private constructor for a utility class. */
    private TModelMethods() {
    }

    /** Create an instance of the fixed-time Vocabulary Model for a vocabulary.
     * @param em The EntityManager to be used to fetch and update
     *      database data.
     * @param vocabularyId The Id of the vocabulary for which the model
     *      is to be constructed.
     * @param fixedTime The fixed time to use to fetch the database entities.
     * @return The created and populated TVocabularyModel instance.
     */
    public static TVocabularyModel createTVocabularyModel(
            final EntityManager em,
            final Integer vocabularyId,
            final LocalDateTime fixedTime) {
        return new TVocabularyModel(em, vocabularyId, fixedTime);
    }

    /** Get the fixed-time instance of the vocabulary, in registry schema
     * format. If there is no such instance, null is returned.
     * @param tvm The TVocabularyModel representing the vocabulary.
     * @param includeVersions Whether or not to include version elements.
     * @param includeAccessPoints Whether or not to include access point
     *      elements.
     * @param includeRelatedEntitiesAndVocabularies Whether or not to include
     *      full related entity elements, and top-level details of
     *      related vocabularies.
     * @return The current instance of the vocabulary, in registry schema
     *      format, if there is a current instance; null, otherwise.
     */
    public static Vocabulary getFixedTimeInstance(final TVocabularyModel tvm,
            final boolean includeVersions,
            final boolean includeAccessPoints,
            final boolean includeRelatedEntitiesAndVocabularies) {
        if (tvm.isEmpty()) {
            return null;
        }
        return tvm.getFixedTimeInstance(includeVersions, includeAccessPoints,
                includeRelatedEntitiesAndVocabularies);
    }

    /** Compare two fixed-time instances of a vocabulary, and
     * report the differences. The two instances must have the same
     * vocabulary Id.
     * @param tvm1 The TVocabularyModel representing the vocabulary at one
     *      fixed time.
     * @param tvm2 The TVocabularyModel representing the vocabulary at another
     *      fixed time.
     * @param vdiff The VocabularyDifferences instance into which differences
     *      are to be reported.
     */
    public static void diff(final TVocabularyModel tvm1,
            final TVocabularyModel tvm2,
            final VocabularyDifferences vdiff) {
        tvm1.diff(tvm2, vdiff);
    }

}
