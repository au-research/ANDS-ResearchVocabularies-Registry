/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.utils.fileformat;

/** Definition of a file format.
 *
 */
public class FileFormat {

    /** The public-facing "name" of the format, e.g., "Turtle", "CSV". */
    private String name;

    /** The file extension of the format, e.g., "ttl", "csv". */
    private String extension;

    /** The MIME type associated with the format, e.g, "text/turtle",
     * "text/csv". */
    private String mimeType;

    /** Whether the format is supported as a Sesame download format. */
    private boolean isSesameDownloadFormat;

    /** Constructor that accepts a name, file extension, MIME type,
     * and a boolean indicating whether or not the format is supported
     * as a Sesame download format, as parameters.
     * @param aName The name of the format.
     * @param anExtension The file extension of the format.
     * @param aMimeType The MIME type of the format.
     * @param anIsSesameDownloadFormat Whether or not the format is supported
     *  as a Sesame download format.
     */
    FileFormat(final String aName, final String anExtension,
            final String aMimeType, final boolean anIsSesameDownloadFormat) {
        name = aName;
        extension = anExtension;
        mimeType = aMimeType;
        isSesameDownloadFormat = anIsSesameDownloadFormat;
    }

    /** Get the name of the format.
     * @return The name of the format. */
    public String getName() {
        return name;
    }

    /** Get the file extension of the format.
     * @return The file extension of the format. */
    public String getExtension() {
        return extension;
    }

    /** Get the MIME type of the format.
     * @return The MIME type of the format. */
    public String getMimeType() {
        return mimeType;
    }

    /** Get whether the format is supported as a download format by Sesame.
     * @return Whether or not the format is supported as a download format
     *  by Sesame. */
    public boolean getIsSesameDownloadFormat() {
        return isSesameDownloadFormat;
    }

}
