/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.model.fixedtime;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.EntityManager;

import au.org.ands.vocabs.registry.notification.VocabularyDifferences;

/** Properties common to fixed-time model classes. */
public abstract class TModelBase {

    /** A local reference to the EntityManager to be used to fetch
     * database data. */
    private EntityManager em;

    /** The time to use as the value of "now" when fetching rows of
     * the database. */
    private LocalDateTime fixedTime;

    /** The vocabulary Id of the vocabulary being modelled. */
    private Integer vocabularyId;

    /** Set the value of em.
     * @param anEm The value of em to set.
     */
    protected final void setEm(final EntityManager anEm) {
        em = anEm;
    }

    /** Set the value of fixedTime.
     * @param aFixedTime The value of fixedTime to set.
     */
    protected final void setFixedTime(final LocalDateTime aFixedTime) {
        fixedTime = aFixedTime;
        notifySetFixedTime(aFixedTime);
    }

    /** Set the value of fixedTime in aggregated model elements.
     * @param afixedTime The value of fixedTime to set.
     */
    protected void notifySetFixedTime(
            @SuppressWarnings("unused") final LocalDateTime afixedTime) {
        // Default action is to do nothing.
    }

    /** Set the value of vocabularyId.
     * @param aVocabularyId The value of vocabularyId to set.
     */
    protected final void setVocabularyId(final Integer aVocabularyId) {
        vocabularyId = aVocabularyId;
    }

    /** Get the value of em.
     * @return The value of em.
     */
    protected final EntityManager em() {
        return em;
    }

    /** Get the value of fixedTime.
     * @return The value of fixedTime.
     */
    protected final LocalDateTime fixedTime() {
        return fixedTime;
    }

    /** Get the value of vocabularyId.
     * @return The value of vocabularyId.
     */
    protected final Integer vocabularyId() {
        return vocabularyId;
    }

    /** Populate the model from the database. */
    protected abstract void populateModel();

    /** Produce a textual description of the model.
     * Each element of the textual description begins with an explanation.
     * If there are specific values included in the element,
     * an explanation of these values is included between a semicolon
     * and a colon.
     * Then the specific values are listed after the colon and a
     * following space, separated by commas.
     * @return A list of Strings, each one of which provides some
     *      information about the model.
     */
    public abstract List<String> describeModel();

    /** Given a partially-complete vocabulary entity in registry schema
     * format, fill it in with details of the fixed-time instance.
     * Sub-models <i>are</i> invoked.
     * @param outputVocabulary The fixed-time instance of the vocabulary,
     *      in registry schema format. Each concrete implementation
     *      is responsible for updating this value as it can.
     * @param includeVersions Whether or not to include version elements.
     *      Note: if includeAccessPoints is true, version elements will be
     *      included, irrespective of the value of includeVersions.
     * @param includeAccessPoints Whether or not to include access point
     *      elements.
     * @param includeRelatedEntitiesAndVocabularies Whether or not to include
     *      full related entity elements, and top-level details of
     *      related vocabularies. NB: this value is ignored, but reserved
     *      for future use. Related Entities and Vocabularies are <i>not</i>
     *      fetched from the database.
     */
    protected abstract void insertIntoSchema(
            au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
            outputVocabulary,
            boolean includeVersions,
            boolean includeAccessPoints,
            boolean includeRelatedEntitiesAndVocabularies);

    /** Compare this instance with another, and report the differences.
     * The other instance must be of the same vocabulary Id, and it is
     * expected to be at a later fixed time than this instance.
     * @param tvmToDiff Another fixed-time instance of the same vocabulary;
     *      it is expected to be at a later time.
     * @param vdiff The VocabularyDifferences instance into which differences
     *      are to be reported.
     */
    protected abstract void diff(TVocabularyModel tvmToDiff,
            VocabularyDifferences vdiff);

}
