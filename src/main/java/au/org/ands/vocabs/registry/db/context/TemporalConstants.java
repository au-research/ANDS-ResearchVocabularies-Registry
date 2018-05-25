/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.context;

import java.time.LocalDateTime;

/** Constants for temporal data.
 */
public final class TemporalConstants {

    /** Private constructor for a utility class. */
    private TemporalConstants() {
    }

    /** The end date of entities that are currently valid. */
    public static final LocalDateTime CURRENTLY_VALID_END_DATE =
            LocalDateTime.of(9999, 12, 1, 0, 0);

    /** The start date of entities that are draft data. */
    public static final LocalDateTime DRAFT_START_DATE =
            LocalDateTime.of(9999, 12, 2, 0, 0);

    /** The end date of entities that are draft data. */
    public static final LocalDateTime DRAFT_END_DATE =
            LocalDateTime.of(9999, 12, 3, 0, 0);

    /** The special last notification date used to indicate that a
     * subscriber has never been sent a notification for a subscription. */
    // Maybe of interest: this was first tried as "0001-01-01 00:00".
    // But when persisted (to H2, at least), this came back as
    // "0001-01-03 00:00".
    public static final LocalDateTime NEVER_NOTIFIED =
            LocalDateTime.of(2000, 1, 1, 0, 0);

}
