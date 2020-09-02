/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.provider.transform.conceptTree;

/** An exception class to use when an error is found in one of
 * the notation values.
 */
public class NotationException extends RuntimeException {

    /** Serial version UID for serialization. */
    private static final long serialVersionUID = 622153466578384982L;

    /** The HTML text of the alert to be provided to the user. */
    private String alertHTML;

    /** Set the HTML text of the alert to be provided to the user.
     * @param anAlertHTML The HTML text of the alert.
     */
    public void setAlertHTML(final String anAlertHTML) {
        alertHTML = anAlertHTML;
    }

    /** Set the HTML text of the alert to be provided to the user.
     * @return The HTML text of the alert.
     */
    public String getAlertHTML() {
        return alertHTML;
    }
}
