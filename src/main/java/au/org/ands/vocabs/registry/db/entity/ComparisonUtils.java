/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.entity;

import java.lang.invoke.MethodHandles;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.internal.ApApiSparql;
import au.org.ands.vocabs.registry.db.internal.ApFile;
import au.org.ands.vocabs.registry.db.internal.ApSesameDownload;
import au.org.ands.vocabs.registry.db.internal.ApSissvoc;
import au.org.ands.vocabs.registry.db.internal.ApWebPage;
import au.org.ands.vocabs.registry.db.internal.VersionJson;
import au.org.ands.vocabs.registry.log.utils.EntityDiffUtils;

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
   <li>Compare these methods with the corresponding methods in
   {@link EntityDiffUtils}.</li>
 * <li>In general, these methods do not involve entity identifier fields in
 * comparisons. It is the responsibility of the caller to do any such
 * comparisons that are required.</li>
 * </ul>
 */
public final class ComparisonUtils {

    /** Private constructor for a utility class. */
    private ComparisonUtils() {
    }

    /** Logger. */
    private static Logger logger;

    static {
        logger = LoggerFactory.getLogger(
                MethodHandles.lookup().lookupClass());
    }

    /** Compare two versions to see if they should be considered
     * "different" for the sake of the registry database.
     * The fields that are compared are:
     * status, slug, release date, title, note, the flags:
     * PoolParty harvest, import, and publish, and the browse flags.
     * @param v1 A version that is an existing database entity.
     * @param v2 A version in registry schema format.
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
                append(versionJson.getNote(), v2.getNote()).
                append(versionJson.isDoPoolpartyHarvest(),
                        v2.isDoPoolpartyHarvest()).
                append(versionJson.isDoImport(), v2.isDoImport()).
                append(versionJson.isDoPublish(), v2.isDoPublish()).
                append(versionJson.getBrowseFlag(), v2.getBrowseFlag()).
                isEquals();
    }

    /** Compare two access points to see if they should be considered
     * "different" for the sake of the registry database.
     * The fields that are compared are:
     * type, source. If these are equal, type-specific fields are
     * compared. For apiSparql: url; for file: format and uploadId;
     * for sesameDownload: urlPrefix; for sissvoc: urlPrefix;
     * for webPage: url.
     * @param ap1 An access point that is an existing database entity.
     * @param ap2 An access point in registry schema format.
     * @return true, if the two access points should be considered
     *      to be the same.
     */
    public static boolean isEqualAP(final AccessPoint ap1,
            final au.org.ands.vocabs.registry.schema.vocabulary201701.
            AccessPoint ap2) {
        boolean equalsTopLevel = new EqualsBuilder().
                append(ap1.getType(), ap2.getDiscriminator()).
                append(ap1.getSource(), ap2.getSource()).
                isEquals();
        if (!equalsTopLevel) {
            return false;
        }
        // Have to compare by type.
        EqualsBuilder eb = new EqualsBuilder();
        switch (ap1.getType()) {
        case API_SPARQL:
            ApApiSparql apApiSparql = JSONSerialization.
                deserializeStringAsJson(ap1.getData(), ApApiSparql.class);
            eb.append(apApiSparql.getUrl(), ap2.getApApiSparql().getUrl());
            break;
        case FILE:
            ApFile apFile = JSONSerialization.
                deserializeStringAsJson(ap1.getData(), ApFile.class);
            au.org.ands.vocabs.registry.schema.vocabulary201701.ApFile
            schemaApFile = ap2.getApFile();
            eb.append(apFile.getFormat(), schemaApFile.getFormat()).
                append(apFile.getUploadId(), schemaApFile.getUploadId());
            break;
        case SESAME_DOWNLOAD:
            // Shouldn't be asking for comparison of these (yet).
            ApSesameDownload apSesameDownload = JSONSerialization.
                deserializeStringAsJson(ap1.getData(), ApSesameDownload.class);
            eb.append(apSesameDownload.getUrlPrefix(),
                    ap2.getApSesameDownload().getUrlPrefix());
            break;
        case SISSVOC:
            ApSissvoc apSissvoc = JSONSerialization.
                deserializeStringAsJson(ap1.getData(), ApSissvoc.class);
            eb.append(apSissvoc.getUrlPrefix(),
                    ap2.getApSissvoc().getUrlPrefix());
            break;
        case WEB_PAGE:
            ApWebPage apWebPage = JSONSerialization.
                deserializeStringAsJson(ap1.getData(), ApWebPage.class);
            eb.append(apWebPage.getUrl(), ap2.getApWebPage().getUrl());
            break;
        default:
            logger.error("isEqualAP called with AP with unknown type: "
                    + ap1.getType());
            return false;
        }
        return eb.isEquals();
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
        // Please note the dependence on a "canonical" order of the
        // keys in the serialization of the JSON-flavoured data column.
        // See the comment in the static
        // block of JSONSerialization. And see also
        // https://intranet.ands.org.au/display/PROJ/
        //   Vocabulary+Registry+mappers+between+database+and+schema+objects
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
