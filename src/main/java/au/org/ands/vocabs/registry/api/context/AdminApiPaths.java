package au.org.ands.vocabs.registry.api.context;
/** See the file "LICENSE" for the full license governing this code. */

/** Web URL paths to the admin API. */
public final class AdminApiPaths {

    /** Private constructor for a utility class. */
    private AdminApiPaths() {
    }

    /** Admin methods of the registry. Doesn't begin with "/api",
     * so that these can be "hidden" to outside users. */
    public static final String API_ADMIN = "/adminApi";

    /** Subpath for backup methods. */
    public static final String BACKUP = "backup";

    /** Subpath for database methods. */
    public static final String DATABASE = "database";

    /** Subpath for email notification methods. */
    public static final String EMAIL = "email";

    /** Subpath for model methods. */
    public static final String MODEL = "model";

    /** Subpath for notification methods. */
    public static final String NOTIFICATION = "notification";

    /** Subpath for resource methods. */
    public static final String RESOURCE = "resource";

    /** Subpath for Solr methods. */
    public static final String SOLR = "solr";

    /** Subpath for workflow methods. */
    public static final String WORKFLOW = "workflow";

    /** Subpath for task resources. */
    public static final String TASKS = "tasks";

    /** Subpath for task set resources. */
    public static final String TASK_SET = "taskSet";

    /** Path parameter name for a task resource. */
    public static final String TASK_ID = "{taskId}";

}
