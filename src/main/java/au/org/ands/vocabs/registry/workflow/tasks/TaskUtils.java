/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.dao.AccessPointDAO;
import au.org.ands.vocabs.registry.db.dao.TaskDAO;
import au.org.ands.vocabs.registry.db.dao.VersionArtefactDAO;
import au.org.ands.vocabs.registry.db.dao.VersionDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.db.entity.AccessPoint;
import au.org.ands.vocabs.registry.db.entity.Task;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.VersionArtefact;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.db.internal.ApFile;
import au.org.ands.vocabs.registry.db.internal.VaHarvestPoolparty;
import au.org.ands.vocabs.registry.enums.AccessPointType;
import au.org.ands.vocabs.registry.enums.VersionArtefactType;
import au.org.ands.vocabs.registry.utils.RegistryConfig;
import au.org.ands.vocabs.registry.utils.RegistryFileUtils;
import au.org.ands.vocabs.registry.utils.SlugGenerator;

/** Utility methods for working with tasks.
 * Included here are methods migrated from
 * {@link au.org.ands.vocabs.toolkit.utils.ToolkitFileUtils}.
 */
public final class TaskUtils {

    /** Logger for this class. */
    private static Logger logger;

    /** Private constructor for utility class. */
    private TaskUtils() {
    }

    static {
        logger = LoggerFactory.getLogger(
                MethodHandles.lookup().lookupClass());
    }

    /** Construct a TaskInfo object based on a task id.
     * @param taskId The task's task id
     * @return The TaskInfo object
     */
    public static TaskInfo getTaskInfo(final int taskId) {
        Task task = TaskDAO.getTaskById(taskId);
        if (task == null) {
            logger.error("getTaskInfo: getTaskById returned null; task id:"
                    + taskId);
            return null;
        }
        Vocabulary vocab = VocabularyDAO.getCurrentVocabularyByVocabularyId(
                task.getVocabularyId());
        if (vocab == null) {
            logger.error("getTaskInfo: getVocabularyById returned null; "
                    + "task id:"
                    + taskId + "; vocab id:" + task.getVocabularyId());
            return null;
        }
        Version version = VersionDAO.getCurrentVersionByVersionId(
                task.getVersionId());
        if (version == null) {
            logger.error("getTaskInfo: getVersionById returned null; "
                    + "task id:"
                    + taskId + "; version id:" + task.getVersionId());
            return null;
        }
        if (!version.getVocabularyId().equals(task.getVocabularyId())) {
            logger.error("getTaskInfo: task's vocab id does not match"
                    + " task's version's vocab id; "
                    + "task id:"
                    + taskId + "; vocab id:" + task.getVocabularyId()
                    + "; version's vocab id:" + version.getVocabularyId());
            return null;
        }
        if (vocab.getSlug() == null || vocab.getSlug().trim().isEmpty()) {
            logger.error("getTaskInfo: vocab's slug is empty; "
                    + "task id:"
                    + taskId + "; vocab id:" + task.getVocabularyId());
            return null;
        }
        if (vocab.getOwner() == null || vocab.getOwner().trim().isEmpty()) {
            logger.error("getTaskInfo: vocab's owner is empty; "
                    + "task id:"
                    + taskId + "; vocab id:" + task.getVocabularyId());
            return null;
        }
        if (version.getSlug() == null || version.getSlug().trim().isEmpty()) {
            logger.error("getTaskInfo: version's slug is empty; "
                    + "task id:"
                    + taskId + "; version id:" + task.getVersionId());
            return null;
        }

        TaskInfo taskInfo = new TaskInfo(task, vocab, version);
        return taskInfo;
    }

    /** Pattern that matches characters that should be replaced when
     * converting a timestamp into a file path component. Use with
     * {@link java.util.regex.Matcher#replaceAll(String)}. */
    private static final Pattern TIMESTAMP_PATTERN =
            Pattern.compile("[^0-9A-Z]");

    /** Sanitize a timestamp String by removing characters we don't want
     * to appear in a directory name.
     * @param timestamp The timestamp String to be sanitized.
     * @return The sanitized timestamp String.
     */
    private static String replaceCharactersInTimestamp(final String timestamp) {
        return TIMESTAMP_PATTERN.matcher(timestamp).replaceAll("");
    }

    /** Get the full path of the directory used to store all
     * the files referred to by the task.
     * @param taskInfo The TaskInfo object representing the task.
     * @param requireDirectory If true, make the directory exist.
     * @param extraPath An optional additional path component to be added
     * at the end. If not required, pass in null or an empty string.
     * @return The full path of the directory used to store the
     * vocabulary data.
     */
    public static String getTaskOutputPath(final TaskInfo taskInfo,
            final boolean requireDirectory,
            final String extraPath) {
        // NB: We call makeSlug() on the vocabulary slug, which should
        // (as of ANDS-Registry-Core commit e365392831ae)
        // not really be necessary.
        Path path = Paths.get(RegistryConfig.DATA_FILES_PATH)
                .resolve(SlugGenerator.generateSlug(
                        taskInfo.getVocabulary().getOwner()))
                .resolve(taskInfo.getVocabulary().getVocabularyId().toString())
                .resolve(taskInfo.getVersion().getVersionId().toString())
                .resolve(replaceCharactersInTimestamp(
                        taskInfo.getNowTime().toString()));
        if (requireDirectory) {
            RegistryFileUtils.requireDirectory(path.toString());
        }
        if (extraPath != null && (!extraPath.isEmpty())) {
            path = path.resolve(extraPath);
        }
        return path.toString();
    }

    /** Get the full path of the directory used to store all
     * harvested data referred to by the task.
     * @param taskInfo The TaskInfo object representing the task.
     * @param requireDirectory If true, make the directory exist.
     * @return The full path of the directory used to store the
     * vocabulary data.
     */
    public static String getTaskHarvestOutputPath(final TaskInfo taskInfo,
            final boolean requireDirectory) {
        String taskHarvestOutputPath = getTaskOutputPath(taskInfo,
                false, RegistryConfig.HARVEST_DATA_PATH);
        if (requireDirectory) {
            RegistryFileUtils.requireDirectory(taskHarvestOutputPath);
        }
        return taskHarvestOutputPath;
    }

    /** Get the full path of (what will be) a new directory used to store
     * transformed data referred to by the task. This is intended
     * to be used as a temporary directory during the transform.
     * If the transform succeeds, call renameTransformTemporaryOutputPath()
     * to rename this directory to become the harvest directory.
     * @param taskInfo The TaskInfo object representing the task.
     * @param transformName The name of the transform being done. This is
     * used in the generation of the path.
     * @param requireDirectory If true, make the directory exist.
     * @return The full path of the directory used to store the
     * transformed data. The directory will be forced to exist if
     *      requireDirectory was true.
     */
    public static String getTaskTransformTemporaryOutputPath(
            final TaskInfo taskInfo,
            final String transformName,
            final boolean requireDirectory) {
        return getTaskOutputPath(taskInfo, requireDirectory,
                "after_" + transformName);
    }

    /** This method is used by transforms that produce new vocabulary
     * data to replace harvested data. If such a transform succeeds,
     * call this method. It renames the original harvest directory, and
     * then renames the temporary directory to become the harvest directory.
     * @param taskInfo The TaskInfo object representing the task.
     * @param transformName The name of the transform that has been done.
     * @return True iff the renaming succeeded.
     */
    public static boolean renameTransformTemporaryOutputPath(
            final TaskInfo taskInfo,
            final String transformName) {
        Path transformOutputPath =
                Paths.get(getTaskOutputPath(taskInfo, false,
                        "after_" + transformName));
        Path harvestPath =
                Paths.get(getTaskHarvestOutputPath(taskInfo, false));
        Path harvestPathDestination =
                Paths.get(getTaskOutputPath(taskInfo, false,
                        "before_" + transformName));
        try {
            // Remove any previous harvestPathDestination
            FileUtils.deleteQuietly(harvestPathDestination.toFile());
            Files.move(harvestPath, harvestPathDestination);
            Files.move(transformOutputPath, harvestPath);
        } catch (IOException e) {
            logger.error("Exception in renameTransformTemporaryOutputPath", e);
            return false;
        }
        return true;
    }

    /** Get the full path of the temporary directory used to store all
     * harvested data for metadata extraction for a PoolParty vocabulary.
     * @param projectId The PoolParty projectId.
     * @return The full path of the directory used to store the
     * vocabulary data.
     */
    public static String getMetadataOutputPath(final String projectId) {
        Path path = Paths.get(RegistryConfig.METADATA_TEMP_FILES_PATH)
                .resolve(SlugGenerator.generateSlug(projectId));
        return path.toString();
    }

    /** Get the full path of the backup directory used to store all
     * backup data for a project.
     * @param projectId The project ID. For now, this will be a PoolParty
     * project ID.
     * @return The full path of the directory used to store the
     * vocabulary data.
     */
    public static String getBackupPath(final String projectId) {
        Path path = Paths.get(RegistryConfig.BACKUP_FILES_PATH)
                .resolve(SlugGenerator.generateSlug(projectId));
        return path.toString();
    }

    /**
     * Get the Sesame repository ID for a vocabulary's version
     * referred to by the task.
     *
     * @param taskInfo
     *            The TaskInfo object representing the task.
     * @return The repository id for the vocabulary with this version.
     */
    public static String getSesameRepositoryId(final TaskInfo taskInfo) {
        // As of ANDS-Registry-Core commit e365392831ae,
        // now use the vocabulary title slug directly from the database.
        return SlugGenerator.generateSlug(
                    taskInfo.getVocabulary().getOwner())
                + "_"
                + taskInfo.getVocabulary().getSlug()
                + "_"
                + taskInfo.getVersion().getSlug();
    }

    /**
     * Get the SISSVoc repository ID for a vocabulary's version
     * referred to by the task. It neither begins nor ends with a slash.
     *
     * @param taskInfo
     *            The TaskInfo object representing the task.
     * @return The repository id for the vocabulary with this version.
     */
    public static String getSISSVocRepositoryPath(final TaskInfo taskInfo) {
        // As of ANDS-Registry-Core commit e365392831ae,
        // now use the vocabulary title slug directly from the database.
        return SlugGenerator.generateSlug(
                    taskInfo.getVocabulary().getOwner())
                + "/"
                + taskInfo.getVocabulary().getSlug()
                + "/"
                + taskInfo.getVersion().getSlug();
    }

    /** Size of buffer to use when writing to a ZIP archive. */
    private static final int BUFFER_SIZE = 4096;

    /** Add a file to a ZIP archive.
     * @param zos The ZipOutputStream representing the ZIP archive.
     * @param file The File which is to be added to the ZIP archive.
     * @return True if adding succeeded.
     * @throws IOException Any exception when reading/writing data.
     */
    private static boolean zipFile(final ZipOutputStream zos, final File file)
            throws IOException {
        if (!file.canRead()) {
            logger.error("zipFile can not read " + file.getCanonicalPath());
            return false;
        }
        zos.putNextEntry(new ZipEntry(file.getName()));
        FileInputStream fis = new FileInputStream(file);

        byte[] buffer = new byte[BUFFER_SIZE];
        int byteCount = 0;
        while ((byteCount = fis.read(buffer)) != -1) {
            zos.write(buffer, 0, byteCount);
        }
        fis.close();
        zos.closeEntry();
        return true;
    }

    /** Compress the files in the backup folder for a project.
     * @param projectId The project ID
     * @throws IOException Any exception when reading/writing data.
     */
    public static void compressBackupFolder(final String projectId)
            throws IOException {
        String backupPath = getBackupPath(projectId);
        if (!Files.isDirectory(Paths.get(backupPath))) {
            // No such directory, so nothing to do.
            return;
        }
        String projectSlug = SlugGenerator.generateSlug(projectId);
        // The name of the ZIP file that does/will contain all
        // backups for this project.
        Path zipFilePath = Paths.get(backupPath).resolve(projectSlug + ".zip");
        // A temporary ZIP file. Any existing content in the zipFilePath
        // will be copied into this, followed by any other files in
        // the directory that have not yet been added.
        Path tempZipFilePath = Paths.get(backupPath).resolve("temp" + ".zip");

        File tempZipFile = tempZipFilePath.toFile();
        if (!tempZipFile.exists()) {
            tempZipFile.createNewFile();
        }

        ZipOutputStream tempZipOut = new ZipOutputStream(
                new FileOutputStream(tempZipFile));

        File existingZipFile = zipFilePath.toFile();
        if (existingZipFile.exists()) {
            ZipFile zipIn = new ZipFile(existingZipFile);

            Enumeration<? extends ZipEntry> entries = zipIn.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                logger.debug("compressBackupFolder copying: " + e.getName());
                tempZipOut.putNextEntry(e);
                if (!e.isDirectory()) {
                    copy(zipIn.getInputStream(e), tempZipOut);
                }
                tempZipOut.closeEntry();
            }
            zipIn.close();
        }

        File dir = new File(backupPath);
        File[] files = dir.listFiles();

        for (File source : files) {
            if (!source.getName().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                logger.debug("compressBackupFolder compressing and "
                        + "deleting file: "
                        + source.toString());
                if (zipFile(tempZipOut, source)) {
                    source.delete();
                }
            }
        }

        tempZipOut.flush();
        tempZipOut.close();
        tempZipFile.renameTo(existingZipFile);
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

    /** Get the Paths of all files that should be processed. The
     * list includes all current file access points and PoolParty harvests.
     * @param taskInfo The top-level TaskInfo for the subtask.
     * @return A list of Paths, each of which is a files that should be
     *      processed.
     */
    public static List<Path> getPathsToProcessForVersion(
            final TaskInfo taskInfo) {
        List<Path> paths = new ArrayList<>();
        List<AccessPoint> aps = AccessPointDAO.
                getCurrentAccessPointListForVersionByType(
                        taskInfo.getVersion().getVersionId(),
                        AccessPointType.FILE, taskInfo.getEm());
        for (AccessPoint ap : aps) {
            ApFile apFile = JSONSerialization.deserializeStringAsJson(
                    ap.getData(), ApFile.class);
            paths.add(Paths.get(apFile.getPath()));
        }
        List<VersionArtefact> vas = VersionArtefactDAO.
                getCurrentVersionArtefactListForVersionByType(
                        taskInfo.getVersion().getVersionId(),
                        VersionArtefactType.HARVEST_POOLPARTY,
                        taskInfo.getEm());
        for (VersionArtefact va : vas) {
            VaHarvestPoolparty vaHarvestPoolparty =
                    JSONSerialization.deserializeStringAsJson(
                            va.getData(), VaHarvestPoolparty.class);
            paths.add(Paths.get(vaHarvestPoolparty.getPath()));
        }
        return paths;
    }

}
