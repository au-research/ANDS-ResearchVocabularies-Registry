/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.utils;

import java.nio.file.Paths;

/** Utility class providing access to Registry properties. */
public final class RegistryConfig {

    /** This is a utility class. No instantiation. */
    private RegistryConfig() {
    }

    // If it is necessary to debug instantiation of this class,
    // uncomment the following, and add some logging in a static
    // block.
    //    /** Logger. */
    //    private static Logger logger;

    //    static {
    //        logger = LoggerFactory.getLogger(
    //                MethodHandles.lookup().lookupClass());
    //    }

    /** Path to the directory used to store vocabulary files. */
    public static final String DATA_FILES_PATH =
            RegistryProperties.getProperty(
                    PropertyConstants.REGISTRY_VOCABSPATH,
                    "/tmp/vocabs");

    /** Subdirectory of the data directory used to store harvested data. */
    public static final String HARVEST_DATA_PATH = "harvest_data";

    /** Path to the directory used to store temporary files. */
    public static final String UPLOAD_FILES_PATH =
            RegistryProperties.getProperty(
                    PropertyConstants.REGISTRY_UPLOADSPATH,
                    "/tmp/vocabs/registry/temp");

    /** Path to the directory used to store temporary files. */
    public static final String TEMP_FILES_PATH =
            RegistryProperties.getProperty(PropertyConstants.REGISTRY_TEMPPATH,
                    "/tmp/vocabs/temp");

    /** Path to the directory used to store temporary files. */
    public static final String METADATA_TEMP_FILES_PATH =
            Paths.get(TEMP_FILES_PATH).resolve("metadata").toString();

    /** Path to the default top-level output directory used to store files. */
    public static final String ROOT_FILES_PATH =
            RegistryProperties.getProperty(
                    PropertyConstants.REGISTRY_STORAGEPATH,
                    "/tmp/vocabs");

    /** Path to the directory used to store backup files. */
    public static final String BACKUP_FILES_PATH =
            Paths.get(ROOT_FILES_PATH).resolve("backup").toString();

}
