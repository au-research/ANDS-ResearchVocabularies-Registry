/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.utils;

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

    /** Path to the directory used to store temporary files. */
    public static final String UPLOAD_FILES_PATH =
            RegistryProperties.getProperty(
                    PropertyConstants.REGISTRY_UPLOADSPATH,
                    "/tmp/vocabs/registry/temp");

}
