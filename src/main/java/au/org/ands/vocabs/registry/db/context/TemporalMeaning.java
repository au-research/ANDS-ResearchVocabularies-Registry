/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.context;

/** Description of the temporal meaning of a temporal entity. */
public enum TemporalMeaning {

    /** Unknown temporal meaning, or unable to determine the
     *  temporal meaning of an entity. */
    UNKNOWN("unknown"),

    /** Historical data. Data that was once valid, but no longer. */
    HISTORICAL("historical"),

    /** Currently-valid data. */
    CURRENT("current"),

    /** Draft data that represents either an addition or modification. */
    DRAFT_ADDITION_OR_MODIFICATION("draft-addition-or-modification"),

    /** Draft data that represents a deletion. */
    DRAFT_DELETION("draft-deletion");

    /** The human-readable form of the meaning. */
    private final String value;

    /** Constructor of the meanings.
     * @param aValue The human-readable form of the meaning.
     */
    TemporalMeaning(final String aValue) {
        value = aValue;
    }

    /** Get the human-readable form of the meaning.
     * @return The human-readable form of the meaning.
     */
    public String getValue() {
        return value;
    }

}
