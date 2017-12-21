/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.model.sequence;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import au.org.ands.vocabs.registry.db.entity.AccessPoint;

/** A simplified representation of an AccessPoint
 * suitable for use in determining changes between two states
 * of the database. It can be thought of as a tuple
 * (versionId, accessPointId) for the purposes of identity and sorting.
 * An existing AccessPoint object can be associated,
 * for subsequent use.
 * A schema representation of the dbAccessPoint can also be associated.
 */
public class AccessPointElement
    implements Comparable<AccessPointElement> {

    /** The Version Id. */
    private Integer versionId;

    /** The AccessPoint Id. */
    private Integer apId;

    /** The database AccessPoint entity, if there is such an existing row
     * of the database; null, otherwise. */
    private AccessPoint dbAP;

    /** The schema AccessPoint entity, if there is one; null, otherwise. */
    private au.org.ands.vocabs.registry.schema.vocabulary201701.AccessPoint
        schemaVersion;

    /** Constructor.
     * @param aVersionId The version Id. This can't be null; for a
     *      not-yet-existent version, a version database element should
     *      already have been added by VersionsModel.
     * @param anAPId The access point Id. This can be null, for a
     *      not-yet-existent version. In this case, aDbAP should
     *      be null, and aSchemaVersion must not be null.
     * @param aDbAP The database AccessPoint, if there is such an
     *      existing row of the database; null, otherwise.
     * @param aSchemaVersion The schema version, if there is one;
     *      null, otherwise.
     */
    public AccessPointElement(final Integer aVersionId,
            final Integer anAPId,
            final AccessPoint aDbAP,
            final au.org.ands.vocabs.registry.schema.vocabulary201701.
                AccessPoint aSchemaVersion) {
        versionId = aVersionId;
        apId = anAPId;
        dbAP = aDbAP;
        schemaVersion = aSchemaVersion;
    }

    /** Get the value of apId.
     * @return The value of apId.
     */
    public Integer getAPId() {
        return apId;
    }

    /** Get the value of dbAP.
     * @return The value of dbAP.
     */
    public AccessPoint getDbAP() {
        return dbAP;
    }

    /** Get the value of schemaVersion.
     * @return The value of schemaVersion.
     */
    public au.org.ands.vocabs.registry.schema.vocabulary201701.AccessPoint
    getSchemaVersion() {
        return schemaVersion;
    }

    /** {@inheritDoc}
     * Equality test based on AccessPoint Id; the values
     * of dbVersion and schemaVersion is not used.
     * The fact that apId may be null is taken into account.
     * If apId is null, equality holds iff {@code other == this}.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == null
                || !(other instanceof VocabularyRelatedEntityElement)) {
            return false;
        }
        if (apId == null) {
            // Even if other has a null apId, it's a different version.
            return this == other;
        }
        AccessPointElement apOther = (AccessPointElement) other;
        return apId.equals(apOther.apId);
    }

    /** {@inheritDoc}
     * The hash code returned is that of the apId.
     */
    @Override
    public int hashCode() {
        return apId.hashCode();
    }

    /** {@inheritDoc}
     * Comparability test based on version and access point Id.
     * That apId may be null is taken into account; all instances
     * with null apId are sorted at the end, by schemaVersion title.
     */
    @Override
    public int compareTo(final AccessPointElement other) {
        if (other == null) {
            // NPE required by the contract specified in
            // the Javadocs of Comparable<T>.
            throw new NullPointerException();
        }
        // Compare versionIds. We rely on there always being one present.
        int versionComparison = versionId.compareTo(other.versionId);
        if (versionComparison != 0) {
            return versionComparison;
        }
        if (apId == null) {
            // This access point has no apId. It will be sorted
            // after all access points that _do_ have apIds.
            if (other.apId == null) {
                // Both access points have null apIds, so
                // sort by source and the remaining elements.
                return new CompareToBuilder().
                        append(schemaVersion.getSource(),
                                other.getSchemaVersion().getSource()).
                        append(schemaVersion.getDiscriminator(),
                                other.getSchemaVersion().getDiscriminator()).
                        append(ToStringBuilder.reflectionToString(
                                schemaVersion.getApApiSparql(),
                                ToStringStyle.SHORT_PREFIX_STYLE),
                                ToStringBuilder.reflectionToString(
                                other.getSchemaVersion().getApApiSparql(),
                                ToStringStyle.SHORT_PREFIX_STYLE)).
                        append(ToStringBuilder.reflectionToString(
                                schemaVersion.getApFile(),
                                ToStringStyle.SHORT_PREFIX_STYLE),
                                ToStringBuilder.reflectionToString(
                                other.getSchemaVersion().getApFile(),
                                ToStringStyle.SHORT_PREFIX_STYLE)).
                        append(ToStringBuilder.reflectionToString(
                                schemaVersion.getApSesameDownload(),
                                ToStringStyle.SHORT_PREFIX_STYLE),
                                ToStringBuilder.reflectionToString(
                                other.getSchemaVersion().getApSesameDownload(),
                                ToStringStyle.SHORT_PREFIX_STYLE)).
                        append(ToStringBuilder.reflectionToString(
                                schemaVersion.getApSissvoc(),
                                ToStringStyle.SHORT_PREFIX_STYLE),
                                ToStringBuilder.reflectionToString(
                                other.getSchemaVersion().getApSissvoc(),
                                ToStringStyle.SHORT_PREFIX_STYLE)).
                        append(ToStringBuilder.reflectionToString(
                                schemaVersion.getApWebPage(),
                                ToStringStyle.SHORT_PREFIX_STYLE),
                                ToStringBuilder.reflectionToString(
                                other.getSchemaVersion().getApWebPage(),
                                ToStringStyle.SHORT_PREFIX_STYLE)).
                        toComparison();
            }
            // The other version has an apId. This access point
            // is sorted after it.
            return 1;
        }
        // This version has an Id.
        if (other.apId == null) {
            // The other access point doesn't have an apId. It is
            // sorted after this access point.
            return -1;
        }
        // Both this and other have apIds. Compare them.
        return apId.compareTo(other.apId);
    }

}
