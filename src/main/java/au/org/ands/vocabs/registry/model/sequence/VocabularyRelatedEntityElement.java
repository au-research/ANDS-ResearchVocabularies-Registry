/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.model.sequence;

import au.org.ands.vocabs.registry.db.entity.VocabularyRelatedEntity;
import au.org.ands.vocabs.registry.enums.RelatedEntityRelation;

/** A simplified representation of a VocabularyRelatedEntity
 * suitable for use in determining changes between two states
 * of the database. It can be thought of as a tuple
 * (reId, reRelation) for the purposes of identity and sorting.
 * An existing VocabularyRelatedEntity object can be associated,
 * for subsequent use.
 */
public class VocabularyRelatedEntityElement
    implements Comparable<VocabularyRelatedEntityElement> {

    /** The related entity Id. */
    private Integer reId;

    /** The related entity relation. */
    private RelatedEntityRelation reRelation;

    /** The VocabularyRelatedEntity, if there is such an existing row
     * of the database; null, otherwise. */
    private VocabularyRelatedEntity vre;

    /** Constructor.
     * @param anReId The related entity Id.
     * @param anReRelation The related entity relation.
     * @param aVRE The VocabularyRelatedEntity, if there is such an
     *      existing row of the database; null, otherwise.
     */
    public VocabularyRelatedEntityElement(final Integer anReId,
            final RelatedEntityRelation anReRelation,
            final VocabularyRelatedEntity aVRE) {
        reId = anReId;
        reRelation = anReRelation;
        vre = aVRE;
    }

    /** Get the value of reId.
     * @return The value of reId.
     */
    public Integer getReId() {
        return reId;
    }

    /** Get the value of reRelation.
     * @return The value of reRelation.
     */
    public RelatedEntityRelation getReRelation() {
        return reRelation;
    }

    /** Get the value of vre.
     * @return The value of vre.
     */
    public VocabularyRelatedEntity getVre() {
        return vre;
    }

    /** {@inheritDoc}
     * Equality test based on RE Id and RE relation; the value
     * of vre is not used.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == null
                || !(other instanceof VocabularyRelatedEntityElement)) {
            return false;
        }
        VocabularyRelatedEntityElement vreOther =
                (VocabularyRelatedEntityElement) other;
        return reId.equals(vreOther.reId) && reRelation == vreOther.reRelation;
    }

    /** {@inheritDoc}
     * The hash code returned is that of the reId.
     */
    @Override
    public int hashCode() {
        return reId.hashCode();
    }

    /** {@inheritDoc}
     * Comparability test based on RE Id, then RE relation.
     */
    @Override
    public int compareTo(final VocabularyRelatedEntityElement other) {
        if (other == null) {
            // NPE required by the contract specified in
            // the Javadocs of Comparable<T>.
            throw new NullPointerException();
        }
        if (!reId.equals(other.reId)) {
            return reId.compareTo(other.reId);
        }
        return reRelation.compareTo(other.reRelation);
    }


}
