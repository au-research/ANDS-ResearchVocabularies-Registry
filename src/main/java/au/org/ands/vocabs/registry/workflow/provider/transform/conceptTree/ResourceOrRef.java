/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.provider.transform.conceptTree;

import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;

/** An abstract class that is extended by both {@link Resource} and
 * {@link ResourceRef}. The child resources of a {@link Resource}
 * are stored in {@link Resource#children}, a collection of
 * {@link ResourceOrRef} instances.
 * The natural order of instances ({@link #compareTo(ResourceOrRef)})
 * is based on a case-insensitive comparison of the prefLabels.
 */
public abstract class ResourceOrRef
implements Comparable<ResourceOrRef> {

    /** Get the IRI.
     * @return The value of the IRI.
     */
    abstract String getIri();

    /** Get the type of this element, either a concept,
     * or a reference to a concept.
     * @return The value of the type.
     */
    abstract ResourceType getType();

    /** Get the label.
     * @return The value of the label.
     */
    abstract String getLabel();

    /** Get the altLabels.
     * @return The values of the altLabels.
     */
    abstract Set<String> getAltLabels();

    /** Get the definition.
     * @return The value of the definition.
     */
    abstract String getDefinition();

    /** Get the DC Terms description.
     * @return The value of the DC Terms description.
     */
    abstract String getDctermsDescription();

    /** Get the notation.
     * @return The value of the notation.
     */
    abstract String getNotation();

    /** The sort order to use, when sorting by label.
     * The value will only be non-null if sorting by notation
     * is enabled. */
    private Integer labelSortOrder;

    /** Set the sort order to use for this resource-or-reference,
     * when sorting by label.
     * @param aLabelSortOrder The sort order for this
     *      resource-or-reference.
     */
    public void setLabelSortOrder(final Integer aLabelSortOrder) {
        labelSortOrder = aLabelSortOrder;
    }

    /** Get the sort order to use for this resource-or-reference,
     * when sorting by label.
     * @return The sort order for this resource-or-reference.
     */
    public Integer getLabelSortOrder() {
        return labelSortOrder;
    }

    /** The sort order to use, when sorting by notation.
     * The value will only be non-null if sorting by notation
     * is enabled. */
    private Integer notationSortOrder;

    /** Set the sort order to use for this concept-or-reference,
     * when sorting by notation.
     * @param aNotationSortOrder The sort order for this
     *      concept-or-reference.
     */
    public void setNotationSortOrder(final Integer aNotationSortOrder) {
        notationSortOrder = aNotationSortOrder;
    }

    /** Get the sort order to use for this concept-or-reference,
     * when sorting by notation.
     * @return The sort order for this concept-or-reference.
     */
    public Integer getNotationSortOrder() {
        return notationSortOrder;
    }

    /** The sort order to use, when this instance has been inserted
     * into an ordered collection. */
    private Integer orderedCollectionSortOrder;

    /** Set the sort order to use for this resource-or-reference,
     * when this instance has been inserted into an ordered collection.
     * @param anOrderedCollectionSortOrder The sort order for this
     *      resource-or-reference.
     */
    public void setOrderedCollectionSortOrder(
            final Integer anOrderedCollectionSortOrder) {
        orderedCollectionSortOrder = anOrderedCollectionSortOrder;
    }

    /** Get the sort order to use for this resource-or-reference,
     * when sorting members of an ordered collections by the canonical
     * position.
     * @return The sort order for this resource-or-reference.
     */
    public Integer getOrderedCollectionSortOrder() {
        return orderedCollectionSortOrder;
    }

    /** Flag to indicate that this value is a top concept of its context
     * concept scheme. It should either be left as null, or set to true
     * (in which case, its value will be included in the serialization). */
    private Boolean isTopConceptOfContext;

    /** Set the value of the {@link #isTopConceptOfContext} flag.
     * @param anIsTopConceptOfContext The value of the
     *      {@link #isTopConceptOfContext} flag to set.
     */
    public void setIsTopConceptOfContext(
            final Boolean anIsTopConceptOfContext) {
        isTopConceptOfContext = anIsTopConceptOfContext;
    }

    /** Get the value of the {@link #isTopConceptOfContext} flag.
     * @return The value of the {@link #isTopConceptOfContext} flag.
     */
    public Boolean getIsTopConceptOfContext() {
        return isTopConceptOfContext;
    }

    /** {@inheritDoc}
     * Comparison of two instances is based on the following priority:
     * <ol>
     * <li>The sequence of members of an ordered collection.</li>
     * <li>type</li>
     * <li>labels (case-insensitively)</li>
     * <li>IRI</li>
     * </ol>
     * All Concepts with null labels are sorted at the end
     * (by their IRIs).
     */
    @Override
    public int compareTo(final ResourceOrRef otherResourceOrRef) {
        if (otherResourceOrRef == null) {
            // NPE required by the contract specified in
            // the Javadocs of Comparable<T>.
            throw new NullPointerException("Not allowed to compare with null");
        }

        // Ordered collection order trumps everything.
        if (orderedCollectionSortOrder != null) {
            // This and other are siblings in an ordered collection.
            if (orderedCollectionSortOrder
                    < otherResourceOrRef.orderedCollectionSortOrder) {
                return -1;
            } else {
                return 1;
            }
        }

        // Compare types; if not the same, we already know the result.
        int typeComparison = getType().compareToByCategory(
                otherResourceOrRef.getType());
        if (typeComparison < 0) {
            return -1;
        } else if (typeComparison > 0) {
            return 1;
        }
        // We now know they are the "same" type (subject to the
        // explanation given in the compareTypes() method comment).
        // Order top concepts before non-top concepts.
        // It happens that we only have to worry about this at the top
        // level of a concept scheme, and we know the children
        // of a concept scheme are always Resources.
        if (getType() == ResourceType.CONCEPT
                && otherResourceOrRef.getType() == ResourceType.CONCEPT) {
            Resource thisResource = (Resource) this;
            Resource otherResource = (Resource) otherResourceOrRef;
            if (BooleanUtils.isTrue(thisResource.getIsTopConceptOfContext())) {
                if (BooleanUtils.isNotTrue(
                        otherResource.getIsTopConceptOfContext())) {
                    return -1;
                }
            } else {
                if (BooleanUtils.isTrue(
                        otherResource.getIsTopConceptOfContext())) {
                    return 1;
                }
            }
        }
        // Two resources of the same type. Try sorting by label.
        if (getLabel() == null) {
            // This concept has no label. It will be sorted
            // after all concepts that _do_ have labels.
            if (otherResourceOrRef.getLabel() == null) {
                // Both concepts have null labels, so
                // sort by their IRIs.
                return getIri().compareTo(otherResourceOrRef.getIri());
            }
            // The other concept has a label. This concept
            // is sorted after it.
            return 1;
        }
        // This concept has a label.
        if (otherResourceOrRef.getLabel() == null) {
            // The other concept doesn't have a label. It is
            // sorted after this concept.
            return -1;
        }
        // Both this and otherResourceOrRef have labels.
        // Use String case-insensitive comparison on them.
        int labelComparison =
                getLabel().compareToIgnoreCase(
                        otherResourceOrRef.getLabel());
        if (labelComparison != 0) {
            return labelComparison;
        }
        // Identical labels. Fall back to comparing their IRIs.
        return getIri().compareTo(otherResourceOrRef.getIri());
    }

}
