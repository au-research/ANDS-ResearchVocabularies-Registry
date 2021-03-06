/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.model;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.EntityManager;

/** Properties common to model classes. */
public abstract class ModelBase {

    /** A local reference to the EntityManager to be used to fetch and
     * update database data. */
    private EntityManager em;

    /** The time to use as the value of "now" when applying changes
     * to rows of the database. */
    private LocalDateTime nowTime;

    /** The value to use for "modifiedBy" when adding or updating
     * rows of the database. */
    private String modifiedBy;

    /** The vocabulary Id of the vocabulary being modelled. */
    private Integer vocabularyId;

    /** Set the value of em.
     * @param anEm The value of em to set.
     */
    protected final void setEm(final EntityManager anEm) {
        em = anEm;
    }

    /** Set the value of nowTime.
     * @param aNowTime The value of nowTime to set.
     */
    public final void setNowTime(final LocalDateTime aNowTime) {
        nowTime = aNowTime;
        notifySetNowTime(aNowTime);
    }

    /** Set the value of nowTime in aggregated model elements.
     * @param aNowTime The value of nowTime to set.
     */
    protected void notifySetNowTime(
            @SuppressWarnings("unused") final LocalDateTime aNowTime) {
        // Default action is to do nothing.
    }

    /** Set the value of modifiedBy.
     * @param aModifiedBy The value of modifiedBy to set.
     */
    public final void setModifiedBy(final String aModifiedBy) {
        modifiedBy = aModifiedBy;
        notifySetModifiedBy(aModifiedBy);
    }

    /** Set the value of modifiedBy in aggregated model elements.
     * @param aModifiedBy The value of modifiedBy to set.
     */
    protected void notifySetModifiedBy(
            @SuppressWarnings("unused") final String aModifiedBy) {
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

    /** Get the value of nowTime.
     * @return The value of nowTime.
     */
    protected final LocalDateTime nowTime() {
        return nowTime;
    }

    /** Get the value of modifiedBy.
     * @return The value of modifiedBy.
     */
    protected final String modifiedBy() {
        return modifiedBy;
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
     * format, fill it in with details of the current instance.
     * Sub-models <i>are</i> invoked.
     * @param outputVocabulary The current instance of the vocabulary,
     *      in registry schema format. Each concrete implementation
     *      is responsible for updating this value as it can.
     * @param includeVersions Whether or not to include version elements.
     *      Note: if includeAccessPoints is true, version elements will be
     *      included, irrespective of the value of includeVersions.
     * @param includeAccessPoints Whether or not to include access point
     *      elements.
     * @param includeRelatedEntitiesAndVocabularies Whether or not to include
     *      full related entity elements, and top-level details of
     *      related vocabularies.
     */
    protected abstract void insertIntoSchemaFromCurrent(
            au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
            outputVocabulary,
            boolean includeVersions,
            boolean includeAccessPoints,
            boolean includeRelatedEntitiesAndVocabularies);

    /** Given a partially-complete vocabulary entity in registry schema
     * format, fill it in with details of the draft instance.
     * Sub-models <i>are</i> invoked.
     * @param outputVocabulary The draft instance of the vocabulary,
     *      in registry schema format. Each concrete implementation
     *      is responsible for updating this value as it can.
     * @param includeVersions Whether or not to include version elements.
     *      Note: if includeAccessPoints is true, version elements will be
     *      included, irrespective of the value of includeVersions.
     * @param includeAccessPoints Whether or not to include access point
     *      elements.
     * @param includeRelatedEntitiesAndVocabularies Whether or not to include
     *      full related entity elements, and top-level details of
     *      related vocabularies.
     */
    protected abstract void insertIntoSchemaFromDraft(
            au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
            outputVocabulary,
            boolean includeVersions,
            boolean includeAccessPoints,
            boolean includeRelatedEntitiesAndVocabularies);

    /** For a vocabulary which exists only as a current instance,
     * make that instance into a draft. This is akin to the "unpublishing"
     * of a published instance back into draft form.
     * Do not invoke this method if there is an existing draft;
     * the caller is responsible for using {@link #deleteOnlyDraft()} first.
     * Sub-models <i>are</i> affected.
     */
    protected abstract void makeCurrentIntoDraft();

    /** Delete the current associated with with this model.
     * Any draft instance is unaffected.
     * Sub-models <i>are</i> affected.
     */
    protected abstract void deleteOnlyCurrent();

    /** Delete the draft associated with with this model.
     * Any current instance is unaffected.
     * Sub-models <i>are</i> affected.
     */
    protected abstract void deleteOnlyDraft();

    /** Delete all of the draft database rows associated with this model.
     * Sub-models <i>are not</i> affected. */
    protected abstract void deleteDraftDatabaseRows();

    /** Apply changes to the database as reflected in a description of
     * an updated Vocabulary, in registry schema format.
     * If updatedVocabulary specifies published or deprecated, this
     * will replace (by deleting) any existing draft.
     * Sub-models <i>are</i> affected.
     * @param updatedVocabulary A description of an updated vocabulary,
     *      in registry schema format.
     */
    protected abstract void applyChanges(
            au.org.ands.vocabs.registry.schema.vocabulary201701.
            Vocabulary updatedVocabulary);

    /** Receive notification of the deletion of the current instance
     * of a version. This method is used by VersionsModel to notify
     * its submodels.
     * @param versionId The version Id of the version whose current instance
     *      is being deleted.
     */
    protected void notifyDeleteCurrentVersion(
            @SuppressWarnings("unused") final Integer versionId) {
        // Default action is to do nothing.
    }

    /** Receive notification of the deletion of the draft instance of a version.
     * This method is used by VersionsModel to notify its submodels.
     * @param versionId The version Id of the version whose draft instance
     *      is being deleted.
     */
    protected void notifyDeleteDraftVersion(
            @SuppressWarnings("unused") final Integer versionId) {
        // Default action is to do nothing.
    }

}
