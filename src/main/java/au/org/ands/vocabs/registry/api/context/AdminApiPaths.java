package au.org.ands.vocabs.registry.api.context;
/** See the file "LICENSE" for the full license governing this code. */

/** Web URL paths to the admin API. */
public final class AdminApiPaths {

    /** Private constructor for a utility class. */
    private AdminApiPaths() {
    }

    /** Admin methods of the registry. Doesn't begin with "/api",
     * so that these can be "hidden" to outside users. */
    public static final String API_ADMIN = "/registry/admin";

    /** Subpath for backup methods. */
    public static final String BACKUP = "backup";

    /** Subpath for database methods. */
    public static final String DATABASE = "database";

    /** Subpath for model methods. */
    public static final String MODEL = "model";

    /** Subpath for Solr methods. */
    public static final String SOLR = "solr";

}
