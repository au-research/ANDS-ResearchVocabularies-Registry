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

    /** Default priority for "transform before importer" insert. */
    public static final Integer
            DEFAULT_TRANSFORM_BEFORE_IMPORTER_INSERT_PRIORITY = 20;
    /** Default priority for "transform before importer" delete.
     * Note that such subtasks are done <i>after</i> importer deletes! */
    public static final Integer
            DEFAULT_TRANSFORM_BEFORE_IMPORTER_DELETE_PRIORITY =
            -DEFAULT_TRANSFORM_BEFORE_IMPORTER_INSERT_PRIORITY;

    /** Default priority for importer insert. */
    public static final Integer DEFAULT_IMPORTER_INSERT_PRIORITY = 30;
    /** Default priority for importer delete. */
    public static final Integer DEFAULT_IMPORTER_DELETE_PRIORITY =
            -DEFAULT_IMPORTER_INSERT_PRIORITY;

    /** Default priority for publish insert. */
    public static final Integer DEFAULT_PUBLISH_INSERT_PRIORITY = 40;
    /** Default priority for publish delete. */
    public static final Integer DEFAULT_PUBLISH_DELETE_PRIORITY =
            -DEFAULT_PUBLISH_INSERT_PRIORITY;

    /** Default priority for "transform after importer" insert.
     * Historical note: this constant was originally set to a value
     * between {@link #DEFAULT_IMPORTER_INSERT_PRIORITY} and
     * {@link #DEFAULT_PUBLISH_INSERT_PRIORITY}, but was then
     * repurposed to serve also for subtasks that must be done
     * after publish subtasks. Now that we also have
     * {@link #DEFAULT_TRANSFORM_AFTER_PUBLISH_INSERT_PRIORITY},
     * the value has been left as-is for now, but it could be adjusted to
     * fit where it was "supposed" to be. */
    public static final Integer
            DEFAULT_TRANSFORM_AFTER_IMPORTER_INSERT_PRIORITY = 50;
    /** Default priority for "transform after importer" delete.
     * Note that such subtasks are done <i>before</i> importer deletes! */
    public static final Integer
            DEFAULT_TRANSFORM_AFTER_IMPORTER_DELETE_PRIORITY =
            -DEFAULT_TRANSFORM_AFTER_IMPORTER_INSERT_PRIORITY;

    /** Default priority for "transform after publish" insert. */
    public static final Integer
            DEFAULT_TRANSFORM_AFTER_PUBLISH_INSERT_PRIORITY = 50;
    /** Default priority for "transform after publish" delete.
     * Note that such subtasks are done <i>before</i> publish deletes! */
    public static final Integer
            DEFAULT_TRANSFORM_AFTER_PUBLISH_DELETE_PRIORITY =
            -DEFAULT_TRANSFORM_AFTER_PUBLISH_INSERT_PRIORITY;

    /** Default priority for backup perform. */
    public static final Integer DEFAULT_BACKUP_PERFORM_PRIORITY = null;

}
