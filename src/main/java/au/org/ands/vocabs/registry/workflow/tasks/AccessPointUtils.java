/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.tasks;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.EntityManager;

import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.dao.AccessPointDAO;
import au.org.ands.vocabs.registry.db.entity.AccessPoint;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.internal.ApSissvoc;
import au.org.ands.vocabs.registry.enums.AccessPointType;
import au.org.ands.vocabs.registry.enums.ApSource;
import au.org.ands.vocabs.registry.utils.PropertyConstants;
import au.org.ands.vocabs.registry.utils.RegistryProperties;

/** Utilities for working with access points. */
public final class AccessPointUtils {

    /** Private constructor for a utility class. */
    private AccessPointUtils() {
    }

    /** URL that is a prefix to download endpoints. */
    private static String downloadPrefixProperty =
            RegistryProperties.getProperty(
                    PropertyConstants.REGISTRY_DOWNLOADPREFIX);

    /** Compute the URL that gives access to a file access point.
     * @param id The access point Id of the access point.
     * @param baseFilename The base filename of the file.
     * @return The URL of the file access point.
     */
    public static String getDownloadUrlForFileAccessPoint(final Integer id,
            final String baseFilename) {
        return downloadPrefixProperty + id + "/" + baseFilename;
    }

    /** Create a database entity for a system-generated SISSVoc access point
     * for a version. Don't duplicate it, if it already exists.
     * @param em The EntityManager to use.
     * @param modifiedBy The value to use for "modifiedBy" when adding/updating
     *      rows of the database.
     * @param nowTime The date/time being used for this operation.
     * @param version The version for which the access point is to be created.
     * @param urlPrefix The urlPrefix to put into the database entity.
     */
    public static void createSissvocAccessPoint(
            final EntityManager em,
            final String modifiedBy,
            final LocalDateTime nowTime,
            final Version version,
            final String urlPrefix) {
        Integer versionId = version.getVersionId();
        List<AccessPoint> aps =
                AccessPointDAO.getCurrentAccessPointListForVersionByType(
                        versionId, AccessPointType.SISSVOC, em);
        for (AccessPoint ap : aps) {
            if (ap.getSource() == ApSource.USER) {
                // We don't touch user-specified SISSVoc access points.
                continue;
            }
            ApSissvoc apSissvoc = JSONSerialization.deserializeStringAsJson(
                    ap.getData(), ApSissvoc.class);
            if (urlPrefix.equals(apSissvoc.getUrlPrefix())) {
                // Already exists, and good to go.
                return;
            }
            // So we've got a currently-valid system-generated one with a
            // different urlPrefix. Retire this one.
            ap.setModifiedBy(modifiedBy);
            TemporalUtils.makeHistorical(ap, nowTime);
            AccessPointDAO.updateAccessPoint(em, ap);
        }
        // No existing access point with the correct urlPrefix,
        // so create a new one.
        AccessPoint ap = new AccessPoint();
        TemporalUtils.makeCurrentlyValid(ap, nowTime);
        ap.setVersionId(version.getId());
        ap.setModifiedBy(modifiedBy);
        ap.setType(AccessPointType.SISSVOC);
        ap.setSource(ApSource.SYSTEM);

        ApSissvoc apSissvoc = new ApSissvoc();
        apSissvoc.setUrlPrefix(urlPrefix);
        ap.setData(JSONSerialization.serializeObjectAsJsonString(apSissvoc));

        AccessPointDAO.saveAccessPointWithId(em, ap);
    }

}
