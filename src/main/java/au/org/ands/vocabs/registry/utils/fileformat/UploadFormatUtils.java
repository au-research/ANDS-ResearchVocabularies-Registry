/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.utils.fileformat;

import java.util.HashMap;
import java.util.Map;

/** Utilities to support file formats as used by file uploads.
 * This class is specifically for uploads: the file format names
 *  match those offered/used in the portal.
 */
public final class UploadFormatUtils {

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

        addFileFormat("BinaryRDF", "bin", "application/x-binary-rdf");
        addFileFormat("CSV", "csv", "text/csv");
        addFileFormat("XLS", "xls", "application/vnd.ms-excel");
        addFileFormat("XLSX", "xlsx",
                "application/vnd.openxmlformats-officedocument."
                + "spreadsheetml.sheet");
        addFileFormat("JSON", "json", "application/json");
        addFileFormat("N-Quads", "nq", "application/n-quads");
        addFileFormat("N-Triples", "nt", "application/n-triples");
        addFileFormat("N3", "n3", "text/rdf+n3");
        addFileFormat("ODS", "ods",
                "application/vnd.oasis.opendocument.spreadsheet");
        addFileFormat("ODT", "odt",
                "application/vnd.oasis.opendocument.text");
        addFileFormat("PDF", "pdf", "application/pdf");
//        addFileFormat("RDF/JSON", "rj", "application/rdf+json");
        addFileFormat("RDF/XML", "rdf", "application/rdf+xml");
        addFileFormat("TSV", "tsv", "text/tab-separated-values");
        addFileFormat("TXT", "txt", "text/plain");
        addFileFormat("TEXT", "txt", "text/plain");
        addFileFormat("TriG", "trig", "application/x-trig");
        addFileFormat("TriX", "trix", "application/trix");
        addFileFormat("TTL", "ttl", "text/turtle");
        addFileFormat("XML", "xml", "application/xml");
        addFileFormat("ZIP", "zip", "application/zip");
    }

    /** Define a FileFormat, registering it in the three Maps.
     * @param aName The name of the format.
     * @param anExtension The file extension of the format.
     * @param aMimeType The MIME type of the format.
     */
    private static void addFileFormat(final String aName,
            final String anExtension,
            final String aMimeType) {
        FileFormat fileFormat = new FileFormat(aName, anExtension, aMimeType);
        nameToFormat.put(aName, fileFormat);
        extensionToFormat.put(anExtension, fileFormat);
        mimeTypeToFormat.put(aMimeType, fileFormat);
    }

    /** Private constructor for a utility class. */
    private UploadFormatUtils() {
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
