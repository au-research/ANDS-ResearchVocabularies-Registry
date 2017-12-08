/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for working with files. */
public final class RegistryFileUtils {

    /** Logger for this class. */
    private static Logger logger;

    /** Private constructor for utility class. */
    private RegistryFileUtils() {
    }

    static {
        logger = LoggerFactory.getLogger(
                MethodHandles.lookup().lookupClass());
    }

    /** Require the existence of a directory. Create it, if it
     * does not already exist.
     * @param dir The full pathname of the required directory.
     */
    public static void requireDirectory(final String dir) {
        File oDir = new File(dir);
        if (!oDir.exists()) {
            oDir.mkdirs();
        }
    }

    /** Get the full path of the file used to store an uploaded
     * file.
     * @param uploadId The ID of the uploaded file.
     * @return The full path of the file used to store the
     * vocabulary data.
     */
    public static String getUploadFilename(final int uploadId) {
        Path path = Paths.get(RegistryConfig.UPLOAD_FILES_PATH)
                .resolve(Integer.toString(uploadId));
        return path.toString();
    }

    /** Save an upload input stream to a file.
     * @param fileName The base name of the file to create
     * @param inputStream The input stream to be written
     * @return Whether or not the save was a success.
     */
    public static boolean saveUploadInputStreamToFile(
            final String fileName,
            final InputStream inputStream) {
        String dirName = RegistryConfig.UPLOAD_FILES_PATH;
        String filePath = dirName
                + File.separator + fileName;
        requireDirectory(dirName);
        File oFile = new File(filePath);
        // See, e.g.,
        // http://stackoverflow.com/questions/9852978/
        //        write-a-file-in-utf-8-using-filewriter-java
        try (OutputStream writer =
                new FileOutputStream(oFile)) {
            copy(inputStream, writer);
            writer.close();
            return true;
        } catch (IOException e) {
            logger.error("Exception in RegistryFileUtils."
                    + "saveInputStreamToFile(): ", e);
            return false;
        }
    }

    /** Save an input stream to a file.
     * @param dirName The full directory name
     * @param fileName The base name of the file to create
     * @param inputStream The input stream to be written
     */
    public static void saveInputStreamToFile(
            final String dirName, final String fileName,
            final InputStream inputStream) {
        String filePath = dirName
                + File.separator + fileName;
        requireDirectory(dirName);
        File oFile = new File(filePath);
        // See, e.g.,
        // http://stackoverflow.com/questions/9852978/
        //        write-a-file-in-utf-8-using-filewriter-java
        try (OutputStream writer =
                new FileOutputStream(oFile)) {
            copy(inputStream, writer);
            writer.close();
        } catch (IOException e) {
            logger.error("Exception in RegistryFileUtils."
                    + "saveInputStreamToFile(): ", e);
        }
    }

    /** Size of buffer to use for copying files. */
    private static final int COPY_BUFFER_SIZE = 4096 * 1024;

    /** Copy the contents of an InputStream into an OutputStream.
     * @param input The content to be copied.
     * @param output The destination of the content being copied.
     * @throws IOException Any IOException during read/write.
     */
    public static void copy(final InputStream input,
            final OutputStream output) throws IOException {
        int bytesRead;
        byte[] buffer = new byte[COPY_BUFFER_SIZE];

        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

}
