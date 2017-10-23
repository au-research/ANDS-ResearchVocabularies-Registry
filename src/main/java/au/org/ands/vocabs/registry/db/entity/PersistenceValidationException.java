/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.entity;

/** Exceptions generated during attempts to persist database entities.
 */
public class PersistenceValidationException extends Exception {

    /** Generated UID for serialization. */
    private static final long serialVersionUID = 280385767026465403L;

    /** The details of the exception. */
    private String details;

    /** Represent an exception thrown as part of validating an
     * entity in preparation for persisting it.
     * @param aDetails The details of the persistence validation exception.
     */
    public PersistenceValidationException(final String aDetails) {
        super(aDetails);
        details = aDetails;
    }

    @Override
    public String toString() {
        return "PersistenceValidationException: " + details;
    }

}
