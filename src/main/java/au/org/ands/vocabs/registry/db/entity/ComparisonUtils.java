/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.entity;

import org.apache.commons.lang3.builder.EqualsBuilder;

import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.internal.VersionJson;

/** Utility methods for comparing two registry database entities
 * of the same type, and for comparing a registry database entity
 * with a registry schema entity.
 * These methods should be used, for example, to determine if an
 * API update request should cause a new version of an entity to be added
 * to the database.
 *
 * Notes:
 * <ul><li>Whenever changes are made to the registry database structure, the
 * methods of this class should be reviewed to see if they also need to
 * be updated.</li>
 * <li>In general, these methods do not involve entity identifier fields in
 * comparisons. It is the responsibility of the caller to do any such
 * comparisons that are required.</li>
 * </ul>
 */
public final class ComparisonUtils {

    /** Private constructor for a utility class. */
    private ComparisonUtils() {
    }

    /** Compare two versions to see if they should be considered
     * "different" for the sake of the registry database.
     * The fields that are compared are:
     *
     * @param v1 A version that is an existing database entity
     * @param v2 A version in registry schema format
     * @return true, if the two versions should be considered
     *      to be the same.
     */
    public static boolean isEqualVersion(final Version v1,
            final au.org.ands.vocabs.registry.schema.vocabulary201701.
            Version v2) {
        VersionJson versionJson =
                JSONSerialization.deserializeStringAsJson(v1.getData(),
                        VersionJson.class);
        return new EqualsBuilder().
                append(v1.getStatus(), v2.getStatus()).
                append(v1.getSlug(), v2.getSlug()).
                append(v1.getReleaseDate(), v2.getReleaseDate()).
                append(versionJson.getTitle(), v2.getTitle()).
                append(versionJson.getNote(), v2.getNote()).isEquals();
    }

    /** Compare two related entities to see if they should be considered
     * "different" for the sake of the registry database.
     * The fields that are compared are: type, title, data.
     * @param re1 One of the two related entities being compared.
     * @param re2 The other related entity being compared.
     * @return true, if the two related entity entities should be considered
     *      to be the same.
     */
    public static boolean isEqualRelatedEntity(final RelatedEntity re1,
            final RelatedEntity re2) {
        return new EqualsBuilder().
                append(re1.getType(), re2.getType()).
                append(re1.getTitle(), re2.getTitle()).
                append(re1.getData(), re2.getData()).isEquals();
    }

    /** Compare two related entity identifiers to see if they should be
     * considered "different" for the sake of the registry database.
     * The fields that are compared are: identifier type, identifier value.
     * @param rei1 One of the two related entity identifiers being compared.
     * @param rei2 The other related entity identifier being compared.
     * @return true, if the two related entity identifier entities should be
     *      considered to be the same.
     */
    public static boolean isEqualRelatedEntityIdentifier(
            final RelatedEntityIdentifier rei1,
            final RelatedEntityIdentifier rei2) {
        return new EqualsBuilder().
                append(rei1.getIdentifierType(), rei2.getIdentifierType()).
                append(rei1.getIdentifierValue(), rei2.getIdentifierValue()).
                isEquals();
    }

}