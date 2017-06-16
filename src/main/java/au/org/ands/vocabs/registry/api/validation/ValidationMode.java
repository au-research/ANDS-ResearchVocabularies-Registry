/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.validation;

/** Validation mode to distinguish different behaviours within
 * the validation implementation classes, depending on whether
 * an new entity is being added, or an existing entity is being updated.
 */
public enum ValidationMode {

    /** Create a new entity. */
    CREATE,

    /** Update an existing entity. */
    UPDATE;

}
