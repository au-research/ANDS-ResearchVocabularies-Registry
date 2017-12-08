/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.model.sequence;

import au.org.ands.vocabs.registry.db.entity.Version;

/** A simplified representation of a Version
 * suitable for use in determining changes between two states
 * of the database. It can be thought of as a tuple
 * (versionId) for the purposes of identity and sorting.
 * An existing Version object can be associated,
 * for subsequent use.
 * A schema representation of the dbVersion can also be associated.
 */
public class VersionElement
    implements Comparable<VersionElement> {

    /** The dbVersion Id. */
    private Integer versionId;

    /** The database Version entity, if there is such an existing row
     * of the database; null, otherwise. */
    private Version dbVersion;

    /** The schema Version entity, if there is one; null, otherwise. */
    private au.org.ands.vocabs.registry.schema.vocabulary201701.Version
        schemaVersion;

    /** Constructor.
     * @param aVersionId The version Id. This can be null, for a
     *      not-yet-existent version. In this case, aDbVersion should
     *      be null, and aSchemaVersion must not be null.
     * @param aDbVersion The database Version, if there is such an
     *      existing row of the database; null, otherwise.
     * @param aSchemaVersion The schema version, if there is one;
     *      null, otherwise.
     */
    public VersionElement(final Integer aVersionId,
            final Version aDbVersion,
            final au.org.ands.vocabs.registry.schema.vocabulary201701.Version
                aSchemaVersion) {
        versionId = aVersionId;
        dbVersion = aDbVersion;
        schemaVersion = aSchemaVersion;
    }

    /** Get the value of versionId.
     * @return The value of versionId.
     */
    public Integer getVersionId() {
        return versionId;
    }

    /** Get the value of dbVersion.
     * @return The value of dbVersion.
     */
    public Version getDbVersion() {
        return dbVersion;
    }

    /** Get the value of schemaVersion.
     * @return The value of schemaVersion.
     */
    public au.org.ands.vocabs.registry.schema.vocabulary201701.Version
    getSchemaVersion() {
        return schemaVersion;
    }

    /** {@inheritDoc}
     * Equality test based on Version Id; the values
     * of dbVersion and schemaVersion is not used.
     * The fact that versionId may be null is taken into account.
     * If versionId is null, equality holds iff {@code other == this}.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == null
                || !(other instanceof VocabularyRelatedEntityElement)) {
            return false;
        }
        if (versionId == null) {
            // Even if other has a null versionId, it's a different version.
            return this == other;
        }
        VersionElement versionOther = (VersionElement) other;
        return versionId.equals(versionOther.versionId);
    }

    /** {@inheritDoc}
     * The hash code returned is that of the versionId.
     */
    @Override
    public int hashCode() {
        return versionId.hashCode();
    }

    /** {@inheritDoc}
     * Comparability test based on version Id.
     * That versionId may be null is taken into account; all instances
     * with null versionId are sorted at the end, by schemaVersion title.
     */
    @Override
    public int compareTo(final VersionElement other) {
        if (other == null) {
            // NPE required by the contract specified in
            // the Javadocs of Comparable<T>.
            throw new NullPointerException();
        }
        if (versionId == null) {
            // This version has no versionId. It will be sorted
            // after all versions that _do_ have versionIds.
            if (other.versionId == null) {
                // Both versions have null versionIds, so
                // sort by their schemaVersion titles.
                return schemaVersion.getTitle().compareTo(
                        other.getSchemaVersion().getTitle());
            }
            // The other version has a versionId. This version
            // is sorted after it.
            return 1;
        }
        // This version has an Id.
        if (other.versionId == null) {
            // The other version doesn't have a versionId. It is
            // sorted after this version.
            return -1;
        }
        // Both this and other have versionIds. Compare them.
        return versionId.compareTo(other.versionId);
    }

}
