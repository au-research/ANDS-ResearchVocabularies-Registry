/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.utils.fileformat;

import java.util.HashMap;
import java.util.Map;

/** Utilities to support file formats.
 * For the most part, this class is customized for uploads:
 * the file format names match those offered/used in the portal.
 * It will also used during indexing to identify those formats
 * which are supported as Sesame downloads.
 */
public final class FileFormatUtils {

    /** Map from format names to FileFormat objects .*/
    private static Map<String, FileFormat> nameToFormat;

    /** Map from format file extensions to FileFormat objects .*/
    private static Map<String, FileFormat> extensionToFormat;

    /** Map from format MIME typess to FileFormat objects .*/
    private static Map<String, FileFormat> mimeTypeToFormat;

    static {
        nameToFormat = new HashMap<>();
        extensionToFormat = new HashMap<>();
        mimeTypeToFormat = new HashMap<>();

        // Possible future: support both a "long" name and a "short" name,
        // e.g., "OpenDocument Spreadsheet (ODS)" and "ODS".
        addFileFormat("BinaryRDF", "bin", "application/x-binary-rdf", true);
        addFileFormat("CSV", "csv", "text/csv", false);
        // Excel (XLS)
        addFileFormat("XLS", "xls", "application/vnd.ms-excel", false);
        // Excel (XLSX)
        addFileFormat("XLSX", "xlsx",
                "application/vnd.openxmlformats-officedocument."
                + "spreadsheetml.sheet", false);
        addFileFormat("JSON", "json", "application/json", false);
        addFileFormat("JSON-LD", "jsonld", "application/ld+json", true);
        addFileFormat("N-Quads", "nq", "application/n-quads", true);
        addFileFormat("N-Triples", "nt", "application/n-triples", true);
        addFileFormat("N3", "n3", "text/rdf+n3", true);
        // OpenDocument Spreadsheet (ODS)
        addFileFormat("ODS", "ods",
                "application/vnd.oasis.opendocument.spreadsheet", false);
        // OpenDocument Text (ODT)
        addFileFormat("ODT", "odt",
                "application/vnd.oasis.opendocument.text", false);
        addFileFormat("PDF", "pdf", "application/pdf", false);
        addFileFormat("RDF/JSON", "rj", "application/rdf+json", true);
        addFileFormat("RDF/XML", "rdf", "application/rdf+xml", true);
        addFileFormat("TSV", "tsv", "text/tab-separated-values", false);
        addFileFormat("TXT", "txt", "text/plain", false);
        // Sigh, can't support both "TXT" and "TEXT" here.
//        addFileFormat("TEXT", "txt", "text/plain");
        addFileFormat("TriG", "trig", "application/x-trig", true);
        addFileFormat("TriX", "trix", "application/trix", true);
        // Turtle
        addFileFormat("TTL", "ttl", "text/turtle", true);
        addFileFormat("XML", "xml", "application/xml", false);
        addFileFormat("ZIP", "zip", "application/zip", false);
    }

    /** Define a FileFormat, registering it in the three Maps.
     * @param aName The name of the format.
     * @param anExtension The file extension of the format.
     * @param aMimeType The MIME type of the format.
     * @param anIsSesameDownloadFormat Whether or not the format is supported
     *  as a Sesame download format.
     */
    private static void addFileFormat(final String aName,
            final String anExtension,
            final String aMimeType,
            final boolean anIsSesameDownloadFormat) {
        FileFormat fileFormat = new FileFormat(aName, anExtension, aMimeType,
                anIsSesameDownloadFormat);
        nameToFormat.put(aName, fileFormat);
        extensionToFormat.put(anExtension, fileFormat);
        mimeTypeToFormat.put(aMimeType, fileFormat);
    }

    /** Private constructor for a utility class. */
    private FileFormatUtils() {
    }

    /** Get the FileFormat with the corresponding name.
     * @param name The name of the FileFormat to be looked up.
     * @return The FileFormat that corresponds to the name.
     */
    public static FileFormat getFileFormatByName(final String name) {
        return nameToFormat.get(name);
    }

    /** Get the FileFormat with the corresponding extension.
     * @param extension The extension of the FileFormat to be looked up.
     * @return The FileFormat that corresponds to the extension.
     */
    public static FileFormat getFileFormatByExtension(final String extension) {
        return extensionToFormat.get(extension);
    }

    /** Get the FileFormat with the corresponding MIME type.
     * @param mimeType The MIME type of the FileFormat to be looked up.
     * @return The FileFormat that corresponds to the MIME type.
     */
    public static FileFormat getFileFormatByMimeType(final String mimeType) {
        return mimeTypeToFormat.get(mimeType);
    }

}
