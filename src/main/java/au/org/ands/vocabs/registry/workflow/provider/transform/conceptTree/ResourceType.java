/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.provider.transform.conceptTree;

import javax.xml.bind.annotation.XmlEnumValue;

/** Enumerated type for resource types.
 * Each type belongs to a "category". Types in the same category
 * will be grouped together in the display. For example,
 * unordered and ordered collections are grouped together.
 */
enum ResourceType {

    /** SKOS Concept Scheme. */
    @XmlEnumValue("concept_scheme")
    CONCEPT_SCHEME("concept scheme", (byte) 0),

    /** SKOS (unordered) Collection. */
    @XmlEnumValue("unordered_collection")
    UNORDERED_COLLECTION("unordered collection", (byte) 1),

    /** SKOS Ordered Collection. */
    @XmlEnumValue("ordered_collection")
    ORDERED_COLLECTION("ordered collection", (byte) 1),

    /** Reference to SKOS (unordered) Collection. */
    @XmlEnumValue("unordered_collection_ref")
    UNORDERED_COLLECTION_REF("unordered collection", (byte) 1),

    /** Reference to SKOS Ordered Collection. */
    @XmlEnumValue("ordered_collection_ref")
    ORDERED_COLLECTION_REF("ordered collection", (byte) 1),

    /** SKOS Concept. */
    @XmlEnumValue("concept")
    CONCEPT("concept", (byte) 2),

    /** Reference to SKOS Concept. */
    @XmlEnumValue("concept_ref")
    CONCEPT_REF("concept", (byte) 2);

    /** A human-readable representation of the type, useful for
     * error messages. */
    private String readable;

    /** The category into which the type is grouped. */
    private byte typeCategory;

    /** Constructor of the resource types.
     * @param aReadable The human-readable representation of the type.
     * @param aTypeCategory The category into which the type is grouped.
     */
    ResourceType(final String aReadable, final byte aTypeCategory) {
        readable = aReadable;
        typeCategory = aTypeCategory;
    }

    /** Get the category into which the type is grouped.
     * @return The category into which the type is grouped.
     */
    private byte getTypeCategory() {
        return typeCategory;
    }

    /** Compare with another resource type, based on the type categories.
     * @param other The resource type to be compared.
     * @return 0, if this and other have the same category;
     *      a negative value, if this is considered to come before other;
     *      a positive value, if this is considered to come after other.
     */
    int compareToByCategory(final ResourceType other) {
        return typeCategory - other.getTypeCategory();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return readable;
    }

//    /** Compare two resource types, based on the type categories.
//     * @param rt1 The first resource type to be compared.
//     * @param rt2 The second resource type to be compared.
//     * @return 0, if rt1 and rt2 have the same category;
//     *      a negative value, if rt1 is considered to come before rt2;
//     *      a positive value, if rt1 is considered to come after rt2.
//     */
//    static int compareByCategory(final ResourceType rt1,
//            final ResourceType rt2) {
//        if (rt1 == null && rt2 == null) {
//            return 0;
//        }
//        if (rt1 != null && rt2 == null) {
//            return -1;
//        }
//        if (rt1 == null && rt2 != null) {
//            return 1;
//        }
//        return rt1.getTypeCategory() - rt2.getTypeCategory();
//    }

}
