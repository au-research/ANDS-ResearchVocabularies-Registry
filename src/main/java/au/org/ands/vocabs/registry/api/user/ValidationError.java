/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user;

import javax.xml.bind.annotation.XmlRootElement;

/** Class for representing one validation error.
 * Based loosely on Jersey's ValidationError class.
 */
@XmlRootElement
public class ValidationError {

    /** The text of the error message. */
    private String message;

    /** The path to the location of the error. */
    private String path;

    /** No-arg constructor. Needed for JAXB. */
    public ValidationError() {
    }

    /** Constructor that accepts the text of the validation error
     * and the path to the error as parameters.
     * @param aMessage The text of the validation error.
     * @param aPath The path to the validation error.
     */
    public ValidationError(final String aMessage, final String aPath) {
        message = aMessage;
        path = aPath;
    }

    /** Set the text of the validation error.
     * @param aMessage The text of the validation error.
     */
    public void setMessage(final String aMessage) {
        message = aMessage;
    }

    /** Get the text of the validation error .
     * @return The text of the validation error.
     */
    public String getMessage() {
        return message;
    }

    /** Set the path to the validation error.
     * @param aPath The path to the validation error.
     */
    public void setPath(final String aPath) {
        path = aPath;
    }

    /** Get the path to the validation error .
     * @return The path to the validation error.
     */
    public String getPath() {
        return path;
    }

}
