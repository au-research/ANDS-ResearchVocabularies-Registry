/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.model.sequence;

import java.lang.invoke.MethodHandles;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** The Version Id. */
    private Integer versionId;

    /** The AccessPoint Id. */
    private Integer apId;

    /** The database AccessPoint entity, if there is such an existing row
     * of the database; null, otherwise. */
    private AccessPoint dbAP;

    /** The schema AccessPoint entity, if there is one; null, otherwise. */
    private au.org.ands.vocabs.registry.schema.vocabulary201701.AccessPoint
        schemaAP;

    /** Constructor.
     * @param aVersionId The version Id. This can't be null; for a
     *      not-yet-existent version, a version database element should
     *      already have been added by VersionsModel.
     * @param anAPId The access point Id. This can be null, for a
     *      not-yet-existent version. In this case, aDbAP should
     *      be null, and aSchemaAP must not be null.
     * @param aDbAP The database AccessPoint, if there is such an
     *      existing row of the database; null, otherwise.
     * @param aSchemaAP The schema access point entity, if there is one;
     *      null, otherwise.
     */
    public AccessPointElement(final Integer aVersionId,
            final Integer anAPId,
            final AccessPoint aDbAP,
            final au.org.ands.vocabs.registry.schema.vocabulary201701.
                AccessPoint aSchemaAP) {
        versionId = aVersionId;
        apId = anAPId;
        dbAP = aDbAP;
        schemaAP = aSchemaAP;
    }

    /** Get the value of versionId.
     * @return The value of versionId.
     */
    public Integer getVersionId() {
        return versionId;
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

    /** Get the value of schemaAP.
     * @return The value of schemaAP.
     */
    public au.org.ands.vocabs.registry.schema.vocabulary201701.AccessPoint
    getSchemaAP() {
        return schemaAP;
    }

    /** {@inheritDoc}
     * Equality test based on AccessPoint Id; the values
     * of dbVersion and schemaVersion are not used.
     * The fact that apId may be null is taken into account.
     * If apId is null, equality holds iff {@code other == this}.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == null
                || !(other instanceof AccessPointElement)) {
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
                // It is probably overkill to give this so much attention
                // (i.e., code), but we do so "just in case" future work
                // (somehow) makes this important.
                // Both access points have null apIds, so
                // sort by source and the remaining elements.
                // NB: source and discriminator are validated by
                // CheckVocabularyImpl and can be expected to be
                // non-null.
                CompareToBuilder compareToBuilder =
                        new CompareToBuilder().
                        append(schemaAP.getSource(),
                                other.getSchemaAP().getSource()).
                        append(schemaAP.getDiscriminator(),
                                other.getSchemaAP().getDiscriminator());
                if (schemaAP.getDiscriminator().equals(
                        other.getSchemaAP().getDiscriminator())) {
                    // CheckVocabularyImpl confirms that both this's and
                    // other's type-specific subelement are non-null.
                    switch (schemaAP.getDiscriminator()) {
                    case API_SPARQL:
                        compareToBuilder.append(
                                ToStringBuilder.reflectionToString(
                                        schemaAP.getApApiSparql(),
                                        ToStringStyle.SHORT_PREFIX_STYLE),
                                ToStringBuilder.reflectionToString(
                                        other.getSchemaAP().getApApiSparql(),
                                        ToStringStyle.SHORT_PREFIX_STYLE));
                        break;
                    case FILE:
                        compareToBuilder.append(
                                ToStringBuilder.reflectionToString(
                                        schemaAP.getApFile(),
                                        ToStringStyle.SHORT_PREFIX_STYLE),
                                ToStringBuilder.reflectionToString(
                                        other.getSchemaAP().getApFile(),
                                        ToStringStyle.SHORT_PREFIX_STYLE));
                        break;
                    case SESAME_DOWNLOAD:
                        // NB: Adding user-specified access points
                        // of type sesameDownload is not yet supported.
                        // See the case for SESAME_DOWNLOAD in
                        // WorkflowMethods.insertAccessPoint().
                        // If you try, you get a NullPointerException
                        // out of the corresponding visitInsertCommand()
                        // method of AccessPointsModel.
                        // If this functionality is added, revisit the comment
                        // for the SESAME_DOWNLOAD case in
                        // WorkflowMethods.insertAccessPoint(),
                        // and make sure there is a test case for this branch,
                        // i.e., adding multiple instances to the
                        // same version at the same time.
                        compareToBuilder.append(
                                ToStringBuilder.reflectionToString(
                                        schemaAP.getApSesameDownload(),
                                        ToStringStyle.SHORT_PREFIX_STYLE),
                                ToStringBuilder.reflectionToString(
                                        other.getSchemaAP().
                                        getApSesameDownload(),
                                        ToStringStyle.SHORT_PREFIX_STYLE));
                        break;
                    case SISSVOC:
                        compareToBuilder.append(
                                ToStringBuilder.reflectionToString(
                                        schemaAP.getApSissvoc(),
                                        ToStringStyle.SHORT_PREFIX_STYLE),
                                ToStringBuilder.reflectionToString(
                                        other.getSchemaAP().getApSissvoc(),
                                        ToStringStyle.SHORT_PREFIX_STYLE));
                        break;
                    case WEB_PAGE:
                        compareToBuilder.append(
                                ToStringBuilder.reflectionToString(
                                        schemaAP.getApWebPage(),
                                        ToStringStyle.SHORT_PREFIX_STYLE),
                                ToStringBuilder.reflectionToString(
                                        other.getSchemaAP().getApWebPage(),
                                        ToStringStyle.SHORT_PREFIX_STYLE));
                        break;
                    default:
                        logger.error("Unexpected discriminator value! FIX ME!");
                        throw new IllegalArgumentException(
                                "Unexpected discriminator value! FIX ME!");
                    }
                }
                return compareToBuilder.toComparison();
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
