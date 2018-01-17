/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.provider;

/** Definition of default priorities for providers. */
public final class DefaultPriorities {

    /** Private constructor for a utility class. */
    private DefaultPriorities() {
    }

    /** Default priority for harvest insert. */
    public static final Integer DEFAULT_HARVEST_INSERT_PRIORITY = 10;
    /** Default priority for harvest delete. */
    public static final Integer DEFAULT_HARVEST_DELETE_PRIORITY =
            -DEFAULT_HARVEST_INSERT_PRIORITY;

    /** Default priority for transform insert. */
    public static final Integer DEFAULT_TRANSFORM_INSERT_PRIORITY = 20;
    /** Default priority for transform delete. */
    public static final Integer DEFAULT_TRANSFORM_DELETE_PRIORITY =
            -DEFAULT_TRANSFORM_INSERT_PRIORITY;

    /** Default priority for import insert. */
    public static final Integer DEFAULT_IMPORT_INSERT_PRIORITY = 30;
    /** Default priority for import delete. */
    public static final Integer DEFAULT_IMPORT_DELETE_PRIORITY =
            -DEFAULT_IMPORT_INSERT_PRIORITY;

    /** Default priority for publish insert. */
    public static final Integer DEFAULT_PUBLISH_INSERT_PRIORITY = 40;
    /** Default priority for publish delete. */
    public static final Integer DEFAULT_PUBLISH_DELETE_PRIORITY =
            -DEFAULT_PUBLISH_INSERT_PRIORITY;

    /** Default priority for backup perform. */
    public static final Integer DEFAULT_BACKUP_PERFORM_PRIORITY = null;

}