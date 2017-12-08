/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.model.sequence;

import au.org.ands.vocabs.registry.db.entity.VocabularyRelatedVocabulary;
import au.org.ands.vocabs.registry.enums.RelatedVocabularyRelation;

/** A simplified representation of a VocabularyRelatedVocabulary
 * suitable for use in determining changes between two states
 * of the database. It can be thought of as a tuple
 * (vocabularyId, rvRelation) for the purposes of identity and sorting.
 * An existing VocabularyRelatedVocabulary object can be associated,
 * for subsequent use.
 */
public class VocabularyRelatedVocabularyElement
    implements Comparable<VocabularyRelatedVocabularyElement> {

    /** The related vocabulary Id. */
    private Integer rvId;

    /** The related vocabulary relation. */
    private RelatedVocabularyRelation rvRelation;

    /** The VocabularyRelatedVocabulary, if there is such an existing row
     * of the database; null, otherwise. */
    private VocabularyRelatedVocabulary vrv;

    /** Constructor.
     * @param anRvId The related vocabulary Id.
     * @param anRvRelation The related vocabulary relation.
     * @param aVRV The VocabularyRelatedVocabulary, if there is such an
     *      existing row of the database; null, otherwise.
     */
    public VocabularyRelatedVocabularyElement(final Integer anRvId,
            final RelatedVocabularyRelation anRvRelation,
            final VocabularyRelatedVocabulary aVRV) {
        rvId = anRvId;
        rvRelation = anRvRelation;
        vrv = aVRV;
    }

    /** Get the value of rvId.
     * @return The value of rvId.
     */
    public Integer getRvId() {
        return rvId;
    }

    /** Get the value of rvRelation.
     * @return The value of rvRelation.
     */
    public RelatedVocabularyRelation getRvRelation() {
        return rvRelation;
    }

    /** Get the value of vrv.
     * @return The value of vrv.
     */
    public VocabularyRelatedVocabulary getVrv() {
        return vrv;
    }

    /** {@inheritDoc}
     * Equality test based on RV Id and RV relation; the value
     * of vrv is not used.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == null
                || !(other instanceof VocabularyRelatedVocabularyElement)) {
            return false;
        }
        VocabularyRelatedVocabularyElement vrvOther =
                (VocabularyRelatedVocabularyElement) other;
        return rvId.equals(vrvOther.rvId) && rvRelation == vrvOther.rvRelation;
    }

    /** {@inheritDoc}
     * The hash code returned is that of the rvId.
     */
    @Override
    public int hashCode() {
        return rvId.hashCode();
    }

    /** {@inheritDoc}
     * Comparability test based on RV Id, then RV relation.
     */
    @Override
    public int compareTo(final VocabularyRelatedVocabularyElement other) {
        if (other == null) {
            // NPE required by the contract specified in
            // the Javadocs of Comparable<T>.
            throw new NullPointerException();
        }
        if (!rvId.equals(other.rvId)) {
            return rvId.compareTo(other.rvId);
        }
        return rvRelation.compareTo(other.rvRelation);
    }

}
