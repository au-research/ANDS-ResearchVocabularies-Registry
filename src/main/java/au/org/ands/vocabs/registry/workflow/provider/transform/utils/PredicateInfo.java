/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.provider.transform.utils;

import au.org.ands.vocabs.registry.workflow.provider.transform.ResourceDocsTransformProvider;

/** Bean class to represent information about an RDF predicate,
 * as needed by {@link ResourceDocsTransformProvider}.
 */
public class PredicateInfo {

    /** The name of the field to use in a generated Solr document. */
    private String fieldName;

    /** Whether a subject may have multiple values for this predicate,
     * without them being distinguished by language tags. */
    private boolean mayHaveMultipleObjects;

    /** Whether a subject may have multiple values for this predicate,
     * that are distinguished by language tags. */
    private boolean mayHaveLanguageSpecificObjects;

    /** Constructor.
     * @param aFieldName The name of the field.
     * @param aMayHaveMultipleObjects Whether for a given subject,
     *      there may be multiple objects for this predicate,
     *      that are not distinguished by language tags.
     * @param aMayHaveLanguageSpecificObjects Whether for a given subject,
     *      there may be multiple objects for this predicate,
     *      that are distinguished by language tags.
     */
    public PredicateInfo(final String aFieldName,
            final boolean aMayHaveMultipleObjects,
            final boolean aMayHaveLanguageSpecificObjects) {
        fieldName = aFieldName;
        mayHaveMultipleObjects = aMayHaveMultipleObjects;
        mayHaveLanguageSpecificObjects = aMayHaveLanguageSpecificObjects;
    }

    /** Get the name of the field to use for the Solr document.
     * @return The name of the field.
     */
    public String getFieldName() {
        return fieldName;
    }

    /** Wet the name of the field to use for the Solr document.
     * @param aFieldName The name of the field.
     */
    public void setFieldName(final String aFieldName) {
        fieldName = aFieldName;
    }

    /** For a given subject, may there be multiple objects for this predicate,
     * that are not distinguished by language tags?
     * @return Whether for a given subject, there may be
     *      multiple objects for this predicate,
     *      that are not distinguished by language tags.
     */
    public boolean isMayHaveMultipleObjects() {
        return mayHaveMultipleObjects;
    }

    /** Specify whether, for a given subject, there may be
     * multiple objects for this predicate,
     * that are not distinguished by language tags.
     * @param aMayHaveMultipleObjects Whether for a given subject,
     *      there may be multiple objects for this predicate,
     *      that are not distinguished by language tags.
     */
    public void setMayHaveMultipleObjects(
            final boolean aMayHaveMultipleObjects) {
        mayHaveMultipleObjects = aMayHaveMultipleObjects;
    }

    /** For a given subject, may there be multiple objects for this predicate,
     * that are distinguished by language tags?
     * @return Whether for a given subject, there may be
     *      multiple objects for this predicate,
     *      that are distinguished by language tags.
     */
    public boolean isMayHaveLanguageSpecificObjects() {
        return mayHaveLanguageSpecificObjects;
    }

    /** Specify whether, for a given subject, there may be
     * multiple objects for this predicate,
     * that are distinguished by language tags.
     * @param aMayHaveLanguageSpecificObjects Whether for a given subject,
     *      there may be multiple objects for this predicate,
     *      that are distinguished by language tags.
     */
    public void setMayHaveLanguageSpecificObjects(
            final boolean aMayHaveLanguageSpecificObjects) {
        mayHaveLanguageSpecificObjects = aMayHaveLanguageSpecificObjects;
    }

}
