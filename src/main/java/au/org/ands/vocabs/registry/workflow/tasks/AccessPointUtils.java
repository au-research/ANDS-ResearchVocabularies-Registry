/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.tasks;

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
}
