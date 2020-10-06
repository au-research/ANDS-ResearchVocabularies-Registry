/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.provider.transform.conceptTree;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Class for representing a reference to a Resource, to be used as
 * values of Sets that store resources-or-references. Every instance
 * of this class is linked to an existing Resource instance;
 * indeed, this class's only constructor
 * requires that a non-null Resource be passed in as a parameter.
 * The class provides getter methods for IRI, type, prefLabel, and
 * notation (i.e., enough to support sorting),
 * but <i>not</i> for narrower Resources. The point is to enable
 * serialization into JSON without getting into infinite loops.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class ResourceRef extends ResourceOrRef {

    /** Logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** The linked Resource instance. */
    private Resource resource;

    /** The type of the resource. */
    private ResourceType type;

    /** Constructor with an IRI specified.
     * @param aResource The Resource, of which this is a reference.
     *      Must be non-null.
     * @throws IllegalArgumentException Thrown if {@code aResource == null}.
     */
    ResourceRef(final Resource aResource) {
        if (aResource == null) {
            LOGGER.error("Won't make a ResourceRef with a null Resource");
            throw new IllegalArgumentException("Won't make a ResourceRef "
                    + "with a null Resource");
        }
        resource = aResource;
        switch (resource.getType()) {
        case CONCEPT:
            type = ResourceType.CONCEPT_REF;
            break;
        case CONCEPT_REF:
        case UNORDERED_COLLECTION_REF:
        case ORDERED_COLLECTION_REF:
        case CONCEPT_SCHEME:
            LOGGER.error("Won't make a ResourceRef "
                    + "for a concept scheme or another ResourceRef");
            throw new IllegalArgumentException("Won't make a ResourceRef "
                    + "for a concept scheme or another ResourceRef");
        case ORDERED_COLLECTION:
            type = ResourceType.ORDERED_COLLECTION_REF;
            break;
        case UNORDERED_COLLECTION:
            type = ResourceType.UNORDERED_COLLECTION_REF;
            break;
        default:
            LOGGER.error("ResourceRef constructor: missing case in switch");
            throw new IllegalArgumentException("ResourceRef constructor: "
                    + "missing case in switch");
        }
    }

    /** Get the IRI.
     * @return The value of the IRI.
     */
    @Override
    public String getIri() {
        return resource.getIri();
    }

    /** Get the URL of the resource, if there is one. There will be such
     * a value if the {@code mayResolveResources} browse flag was set, and
     * the IRI contains non-ASCII characters.
     * @return The value of the URL of the resource, or null, if
     *      there isn't one.
     */
    public String getUrl() {
        return resource.getUrl();
    }

    /* {@inheritDoc} */
    @Override
    public ResourceType getType() {
        return type;
    }

    /** {@inheritDoc}
     * Equality test based on IRI. But there should be only one
     * instance of an IRI in a Set or Map of Resources.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == null || !(other instanceof ResourceRef)) {
            return false;
        }
        ResourceRef otherConcept = (ResourceRef) other;
        return getIri().equals(otherConcept.getIri());
    }

    /** {@inheritDoc}
     * The hash code returned is that of the IRI.
     */
    @Override
    public int hashCode() {
        return getIri().hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getIri();
    }

    /** {@inheritDoc} */
    @Override
    public String getLabel() {
        return resource.getLabel();
    }

    /** {@inheritDoc} */
    @Override
    public String getDefinition() {
        return resource.getDefinition();
    }

    /** {@inheritDoc} */
    @Override
    public String getNotation() {
        return resource.getNotation();
    }

    /** {@inheritDoc} */
    @Override
    public String getDctermsDescription() {
        return resource.getDctermsDescription();
    }

}
