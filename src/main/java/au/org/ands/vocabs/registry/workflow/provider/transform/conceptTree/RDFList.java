/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.provider.transform.conceptTree;

import org.openrdf.model.Value;

/** Representation of an RDF list, with some inbuilt validity checks.
 */
public class RDFList {

    /** Singleton instance used to represent {@code rdf:nil}. We need this
     * so that we can distinguish the case of the field {@link #rest} being
     * null (i.e., there being no value) from the case of being {@code rdf:nil}.
     */
    public static final RDFList NIL = new RDFList(null);

    /** The IRI of the resource. */
    @SuppressWarnings("checkstyle:VisibilityModifier")
    protected String iri;

    /** The first element of the list. */
    private Value first;

    /** The rest of the list. */
    private RDFList rest;

    /** Constructor with an IRI specified.
     * @param anIRI The IRI of the RDFList. This should not be {@code null},
     *      except when creating the special value {@code #NIL}.
     */
    RDFList(final String anIRI) {
        iri = anIRI;
    }

    /** Get the IRI.
     * @return The value of the IRI.
     */
    public String getIri() {
        return iri;
    }

    /** Get the first element of the list.
     * @return The first element of the list.
     */
    public Value getFirst() {
        return first;
    }

    /** Set the first element of the list.
     * @param aFirst The first element of the list.
     * @throws IllegalArgumentException If the first element of the list
     *      has already been set, to a value different from aFirst.
     */
    public void setFirst(final Value aFirst) {
        if (first != null && !first.equals(aFirst)) {
            throw new IllegalArgumentException("Attempt to change the first "
                    + "element of a list to a different value");
        }
        first = aFirst;
    }

    /** Get the rest of the list.
     * @return The rest of the list.
     */
    public RDFList getRest() {
        return rest;
    }

    /** Get the rest of the list.
     * @param aRest The rest of the list.
     * @throws IllegalArgumentException If the rest of the list
     *      has already been set, to a value different from aRest.
     */
    public void setRest(final RDFList aRest) {
        if (rest != null && aRest != rest) {
            throw new IllegalArgumentException("Attempt to change the rest "
                    + "of a list to a different value");
        }
        rest = aRest;
    }

    /** Enumerated type for representing the validity of this list. */
    enum Validity {
        /** The validity has not yet been computed. */
        UNKNOWN,
        /** This list contains a cycle. */
        CYCLE,
        /** This list is known to be valid. */
        VALID;
    }

    /** The status of the validity of this list. */
    private Validity validity = Validity.UNKNOWN;

    /** Get the validity of this list.
     * @return The validity of this list.
     */
    public Validity getValidity() {
        return validity;
    }

    /** Set the validity of this list.
     * @param aValidity The validity to set.
     */
    public void setValidity(final Validity aValidity) {
        validity = aValidity;
    }

}
