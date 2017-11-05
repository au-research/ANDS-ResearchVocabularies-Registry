/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.context;

import java.time.LocalDateTime;

/** The getters and setters for start and end dates.
 * All database entity classes that have start and end dates
 * implement this interface.
 */
public interface TemporalColumns {

    /** Get the value of startDate.
     * @return The value of startDate.
     */
    LocalDateTime getStartDate();

    /** Set the value of startDate.
     * @param aStartDate The value of startDate to set.
     */
    void setStartDate(LocalDateTime aStartDate);

    /** Get the value of endDate.
     * @return The value of endDate.
     */
    LocalDateTime getEndDate();

    /** Set the value of endDate.
     * @param anEndDate The value of endDate to set.
     */
    void setEndDate(LocalDateTime anEndDate);

}
