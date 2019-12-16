/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.validation;

import java.io.StringWriter;

/** A variant of the {@link StringWriter} class that restricts write
 * requests so that the size of the underlying buffer will not exceed
 * a specified value. (But the limit may be slightly exceeded for the
 * purposes of adding a note <i>indicating</i> that there was
 * an attempt to exceed the limit.)
 */
public class LimitedSizeStringWriter extends StringWriter {

    /** Default size limit. This is value used by the default constructor. */
    public static final int DEFAULT_SIZE_LIMIT = 10000;

    /** The limit on the number of characters that may be written to
     * the underlying buffer. But note that if an attempt is made to
     * exceed the limit, some extra text will be appended to indicate
     * that the content is being truncated. This extra text may mean
     * that the length of the underlying buffer <i>does</i> exceed
     * the limit.
     */
    private int limit;

    /** Flag to keep track if a request has come in that would cause the
     * limit to be exceeded. Used to make sure that the extra text
     * appended to the buffer when this happens is only appended once.
     */
    private boolean limitExceeded = false;

    /** A count of the number of characters that have been written
     * to the underlying buffer.
     */
    private int count = 0;

    /** Default constructor. The size limit defaults to the value of
     * {@link DEFAULT_SIZE_LIMIT}.
     */
    public LimitedSizeStringWriter() {
        limit = DEFAULT_SIZE_LIMIT;
    }

    /** Constructor that allows the caller to specify the size limit.
     * @param aLimit The limit on the number of characters that may be
     *      written.
     */
    public LimitedSizeStringWriter(final int aLimit) {
        limit = aLimit;
    }

    /** The the write allowed? It is allowed if the limit has not
     * already been exceeded, and there is space to store the additional
     * content.
     * @param extra The number of extra characters that the caller would
     *      like to append to the buffer.
     * @return True, if the write will be allowed.
     */
    private boolean mayWrite(final int extra) {
        return !limitExceeded && (count + extra <= limit);
    }

    /** Make the {@link limitExceeded} flag true. If it was not already
     * true, append a note to the buffer indicating that the limit
     * has been exceeded.
     */
    private void setLimitExceeded() {
        if (!limitExceeded) {
            super.write("... (limit exceeded)");
            limitExceeded = true;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void write(final char[] aCbuf, final int aOff, final int aLen) {
        if (mayWrite(aLen)) {
            super.write(aCbuf, aOff, aLen);
            count = getBuffer().length();
        } else {
            setLimitExceeded();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void write(final int aC) {
        if (mayWrite(1)) {
            super.write(aC);
            count = getBuffer().length();
        } else {
            setLimitExceeded();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void write(final String aStr) {
        if (mayWrite(aStr.length())) {
            super.write(aStr);
            count = getBuffer().length();
        } else {
            setLimitExceeded();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void write(final String aStr, final int aOff, final int aLen) {
        // TODO Auto-generated method stub
        if (mayWrite(aLen)) {
            super.write(aStr, aOff, aLen);
            count = getBuffer().length();
        } else {
            setLimitExceeded();
        }
    }

}
