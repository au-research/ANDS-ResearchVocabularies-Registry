/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.dbunit.Assertion;
import org.dbunit.DatabaseUnitException;
import org.dbunit.assertion.comparer.value.ValueComparer;
import org.dbunit.assertion.comparer.value.ValueComparers;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.DefaultDataSet;
import org.dbunit.dataset.DefaultTable;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.xml.FlatDtdWriter;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.h2.H2Connection;
import org.dbunit.operation.DatabaseOperation;
import org.hibernate.HibernateException;
import org.hibernate.internal.SessionImpl;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.manager.RepositoryInfo;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.repository.manager.RepositoryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.FileAssert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import au.org.ands.vocabs.registry.subscription.Owners;
import au.org.ands.vocabs.registry.utils.PropertyConstants;
import au.org.ands.vocabs.registry.utils.RegistryProperties;
import au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector;
import au.org.ands.vocabs.toolkit.test.utils.NetClientUtils;
import au.org.ands.vocabs.toolkit.utils.ApplicationContextListener;

/** Support methods for testing with Arquillian. */
public final class ArquillianTestUtils {

    /** Logger. */
    private static Logger logger;

    /** The {@link ClassLoader} of this class. Used for invoking
     * {@link ClassLoader#getResourceAsStream(String)}. */
    private static ClassLoader classLoader;

    static {
        logger = LoggerFactory.getLogger(
                MethodHandles.lookup().lookupClass());
        classLoader = MethodHandles.lookup().lookupClass().
                getClassLoader();
    }

    /** Private constructor for a utility class. */
    private ArquillianTestUtils() {
    }

    /** Get the contents of a file from the test directory as a String,
     * given its filename.
     * This is a method to be called client-side, from the top level
     * of the Registry code. The path is interpreted relative to
     * the current working directory.
     * @param filename The filename of the resource.
     * @return A {@link String} containing the contents the resource.
     */
    static String getTestFileAsString(
            final String filename) {
        File file = new File(
                ArquillianBaseTest.RESOURCES_DEPLOY_PATH
                + "/" + filename);
        try {
            return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Can't load resource: "
                    + filename);
        }
    }

    /** Get an {@link InputStream} for a file, given its filename.
     * @param filename The filename of the resource.
     * @return An {@link InputStream} for the resource.
     */
    static InputStream getResourceAsInputStream(
            final String filename) {
        InputStream inputStream = classLoader.getResourceAsStream(filename);
        if (inputStream == null) {
            throw new IllegalArgumentException("Can't load resource: "
                    + filename);
        }
        return inputStream;
    }

    /** Get the contents of a file as a String, given its filename.
     * @param filename The filename of the resource.
     * @return A {@link String} containing the contents the resource.
     */
    static String getResourceAsString(
            final String filename) {
        InputStream inputStream = classLoader.getResourceAsStream(filename);
        if (inputStream == null) {
            throw new IllegalArgumentException("Can't load resource: "
                    + filename);
        }
        try {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Can't load resource: "
                    + filename);
        }
    }

    /** The registry temporary path. */
    private static String tempPath =
        RegistryProperties.getProperty(PropertyConstants.REGISTRY_TEMPPATH);

    /** The registry temporary path, as a path. */
    private static Path tempPathPath = Paths.get(tempPath);

    /** Get the path to use for a test-specific temporary directory.
     * @param testName The name of the test method. It will be used
     *      as part of the path.
     * @return The full path to the temporary directory for the test.
     */
    public static Path getTempPathForTest(final String testName) {
        return tempPathPath.resolve(testName);
    }

    /** Copy all of the test data files for a test. The test-specific
     * temporary directory is created (having been removed, if it already
     * exists) and the files are copied into it.
     * @param testName The name of the test method.
     * @throws IOException If there is an error copying a file.
     */
    public static void copyTempFilesForTest(final String testName)
        throws IOException {
        Path testTempPath = getTempPathForTest(testName);
        File testTempPathFile = testTempPath.toFile();
        FileUtils.deleteQuietly(testTempPathFile);
        testTempPathFile.mkdirs();
        String sourceDirectory = classLoader.getResource(
                "test/tests/" + testName + "/files").getPath();
        try (DirectoryStream<Path> stream =
                Files.newDirectoryStream(Paths.get(sourceDirectory))) {
            // Iterate over every file in the test source directory.
            for (Path entry: stream) {
                // Copy the file to the temporary directory.
                Files.copy(entry, testTempPath.resolve(entry.getFileName()));
            }
        }
    }

    /** Assert that the temporary directory for a test is not empty,
     * i.e., it contains at least one file.
     * @param testName The name of the test method.
     */
    public static void assertTempForTestNotEmpty(final String testName) {
        Path tempDir = getTempPathForTest(testName);
        Assert.assertTrue(Files.isDirectory(tempDir),
                "Temporary directory expected to exist, but does not: "
                + tempDir);
        try (Stream<Path> stream = Files.list(tempDir)) {
            Assert.assertTrue(stream.findAny().isPresent(),
                    "Temporary directory expected to be non-empty, "
                            + "but is empty: " + tempDir);
        } catch (IOException e) {
            Assert.fail("IOException while opening temp directory", e);
        }
    }

    /** Assert that the temporary directory for a test has a particular
     * number of files in it.
     * @param testName The name of the test method.
     * @param expectedNumberOfFiles The expected number of files.
     */
    public static void assertTempForTestHasFiles(final String testName,
            final int expectedNumberOfFiles) {
        Path tempDir = getTempPathForTest(testName);
        Assert.assertTrue(Files.isDirectory(tempDir),
                "Temporary directory expected to exist, but does not: "
                + tempDir);
        try (Stream<Path> stream = Files.list(tempDir)) {
            Assert.assertEquals(stream.count(),
                    expectedNumberOfFiles,
                    "Temporary directory expected to have "
                    + expectedNumberOfFiles
                    + " but did not: " + tempDir);
        } catch (IOException e) {
            Assert.fail("IOException while opening temp directory", e);
        }
    }

    /** Assert that the temporary directory for a test is empty.
     * @param testName The name of the test method.
     */
    public static void assertTempForTestEmpty(final String testName) {
        Path tempDir = getTempPathForTest(testName);
        Assert.assertTrue(Files.isDirectory(tempDir),
                "Temporary directory expected to exist, but does not: "
                + tempDir);
        try (Stream<Path> stream = Files.list(tempDir)) {
            Assert.assertTrue(!stream.findAny().isPresent(),
                    "Temporary directory expected to be empty, "
                    + "but is not: " + tempDir);
        } catch (IOException e) {
            Assert.fail("IOException while opening temp directory", e);
        }
    }

    /** The registry uploads path. */
    private static String uploadsPath =
        RegistryProperties.getProperty(PropertyConstants.REGISTRY_UPLOADSPATH);

    /** The registry uploads path, as a path. */
    private static Path uploadsPathPath = Paths.get(uploadsPath);

    /** Copy all of the upload data files for a test. Any existing
     * files in the uploads directory are removed first.
     * @param testName The name of the test method.
     * @throws IOException If there is an error copying a file.
     */
    public static void copyUploadsFilesForTest(final String testName)
        throws IOException {
        File uploadsPathFile = uploadsPathPath.toFile();
        FileUtils.deleteQuietly(uploadsPathFile);
        uploadsPathFile.mkdirs();
        String sourceDirectory = classLoader.getResource(
                "test/tests/" + testName + "/uploads").getPath();
        try (DirectoryStream<Path> stream =
                Files.newDirectoryStream(Paths.get(sourceDirectory))) {
            // Iterate over every file in the test source directory.
            for (Path entry: stream) {
                // Copy the file to the temporary directory.
                Files.copy(entry, uploadsPathPath.resolve(entry.getFileName()));
            }
        }
    }

    /** Get the absolute path to {@code /WEB-INF/classes} within the web app.
     * Only works within the container.
     * @return The absolute path to the {@code /WEB-INF/classes} directory
     * within the running webapp.
     */
    public static String getClassesPath() {
        return ApplicationContextListener.getServletContext().
                getRealPath("/WEB-INF/classes");
    }

    /** Add our standard replacement settings to the dataset.
     * For now, we support: replacing two consecutive apostrophes ({@code ''})
     * with a double-quote ({@code "}), replacing the string
     * {@code {CLASSES}} with the absolute path to the {@code /WEB-INF/classes}
     * directory in the running webapp, replacing the string
     * {@code {TEMP}} with the absolute path to the registry temporary path,
     * replacing the string
     * {@code {VOCABS}} with the path to the registry vocabs path,
     * replacing the string
     * {@code {SPECS}} with the path to the registry specs path,
     * replacing the string
     * {@code {MOCKSERVERPORT}} with the port number of the mock server,
     * and replacing the string
     * {@code {DOWNLOADPREFIX}} with the path to the registry download prefix.
     * @param dataset The dataset to which the replacement settings
     * are to be added.
     */
    public static void addReplacementSubstringsToDataset(
            final ReplacementDataSet dataset) {
        dataset.addReplacementSubstring("''", "\"");
        String classesPath = getClassesPath();
        dataset.addReplacementSubstring("{CLASSES}", classesPath);
        dataset.addReplacementSubstring("{TEMP}", tempPath);
        dataset.addReplacementSubstring("{VOCABS}",
                RegistryProperties.getProperty(
                        PropertyConstants.REGISTRY_VOCABSPATH));
        dataset.addReplacementSubstring("{SPECS}",
                RegistryProperties.getProperty(
                        PropertyConstants.SISSVOC_SPECSPATH));
        dataset.addReplacementSubstring("{MOCKSERVERPORT}",
                Integer.toString(PoolPartyMockServer.getPort()));
        dataset.addReplacementSubstring("{DOWNLOADPREFIX}",
                RegistryProperties.getProperty(
                        PropertyConstants.REGISTRY_DOWNLOADPREFIX));
    }

    /* Methods for the database(s). */

    /** Get an EntityManager for the selected database.
     * @param dbs The database for which an EntityManager is to be created.
     * @return The created EntityManager.
     */
    public static EntityManager getEntityManagerForDb(
            final DatabaseSelector dbs) {
        if (dbs == null) {
            // Booboo in the test method!
            return null;
        }
        switch (dbs) {
        case ROLES:
            return au.org.ands.vocabs.roles.db.context.DBContext.
                    getEntityManager();
        case TOOLKIT:
            return au.org.ands.vocabs.toolkit.db.DBContext.
                    getEntityManager();
        case REGISTRY:
            return au.org.ands.vocabs.registry.db.context.DBContext.
                    getEntityManager();
        default:
            return null;
        }
    }

    /** The EntityManagers used across DbUnit methods. */
    private static Map<DatabaseSelector, EntityManager>
    entityManagersForDbUnit = new HashMap<>();

    /** The JDBC Connections used across DbUnit methods. */
    private static Map<DatabaseSelector, Connection> jdbcConnectionsForDbUnit =
            new HashMap<>();

    /** The DbUnit Connections used across DbUnit methods. */
    private static Map<DatabaseSelector, IDatabaseConnection>
        iDatabaseConnectionsForDbUnit = new HashMap<>();


    /** Get the shared JDBC Connection for DbUnit methods for the selected
     * database.
     * @param dbs The database for which the Connection is to be fetched.
     * @return The Connection to use for DbUnit methods.
     */
    private static synchronized Connection getJDBCConnectionForDbUnit(
            final DatabaseSelector dbs) {
        if (dbs == null) {
            // Booboo in the test method!
            return null;
        }
        Connection conn = jdbcConnectionsForDbUnit.get(dbs);
        if (conn != null) {
            return conn;
        }
        EntityManager em = getEntityManagerForDb(dbs);
        entityManagersForDbUnit.put(dbs, em);
        logger.info("In getJDBCConnectionForDbUnit; setting shared "
                + "JDBC Connection for: " + dbs);
        conn = em.unwrap(SessionImpl.class).connection();
        jdbcConnectionsForDbUnit.put(dbs, conn);
        return conn;
    }

    /** Get the shared DbUnit IDatabaseConnection for DbUnit methods
     * for the selected database.
     * @param dbs The database for which the Connection is to be fetched.
     * @return The Connection to use for DbUnit methods.
     * @throws DatabaseUnitException If a problem with DbUnit.
     */
    private static synchronized IDatabaseConnection
    getIDatabaseConnectionForDbUnit(final DatabaseSelector dbs)
            throws DatabaseUnitException {
        if (dbs == null) {
            // Booboo in the test method!
            return null;
        }
        IDatabaseConnection idc = iDatabaseConnectionsForDbUnit.get(dbs);
        if (idc != null) {
            return idc;
        }
        Connection conn = getJDBCConnectionForDbUnit(dbs);
        // For H2, essential to specify "PUBLIC" as the schema.
        // Otherwise, when working with the roles database, DbUnit
        // thinks that the ROLES table also has the columns of H2's
        // INFORMATION_SCHEMA.ROLES table!
        idc = new H2Connection(conn, "PUBLIC");
        iDatabaseConnectionsForDbUnit.put(dbs, idc);
        if (dbs == DatabaseSelector.ROLES) {
            // Just for the roles database, we need to allow empty values.
            idc.getConfig().setProperty(
                    DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, true);
        }
        return idc;
    }

    /** The blank datasets used across DbUnit methods.
     * They have all the expected tables defined, but no rows.
     * Lazily initialized. */
    private static Map<DatabaseSelector, IDataSet>
        blankIDataSetsForDbUnit = new HashMap<>();

    /** Get the shared DbUnit empty IDataSet for DbUnit methods
     * for the selected database.
     * @param dbs The database for which the Connection is to be fetched.
     * @return The Connection to use for DbUnit methods.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If reading the DTD fails.
     */
    private static synchronized IDataSet
    getBlankIDataSetForDbUnit(final DatabaseSelector dbs)
            throws DatabaseUnitException, IOException {
        if (dbs == null) {
            // Booboo in the test method!
            return null;
        }
        IDataSet ids = blankIDataSetsForDbUnit.get(dbs);
        if (ids != null) {
            return ids;
        }

        // Initialize a blank DataSet with all the tables defined.
        FlatXmlDataSet blankXmlDataSet = new FlatXmlDataSetBuilder()
                .setMetaDataSetFromDtd(getResourceAsInputStream(
                        dbs.getDTDFilename()))
                .build(getResourceAsInputStream(
                        dbs.getBlankDataFilename()));
        // But in fact, blankXmlDataSet now contains one row for
        // each table, with null in each column.
        // We want to get rid of those dummy rows.
        // The way to do it seems to be to create a fresh DataSet,
        // initialized with fresh ITables, where we use only the
        // table metadata from blankXmlDataSet.
        ITable[] itables = blankXmlDataSet.getTables();
        DefaultDataSet defaultDataSet = new DefaultDataSet();
        for (ITable itable : itables) {
            defaultDataSet.addTable(new DefaultTable(
                    itable.getTableMetaData()));
        }
        ids = defaultDataSet;
        blankIDataSetsForDbUnit.put(dbs, ids);
        return ids;
    }

    /** Close all shared Connections for DbUnit methods. */
    public static void closeConnectionsForDbUnit() {
        logger.info("In closeConnectionsForDbUnit");
        for (IDatabaseConnection conn
                : iDatabaseConnectionsForDbUnit.values()) {
            try {
                logger.info("Closing a shared IDatabaseConnection");
                conn.close();
            } catch (SQLException e) {
                logger.error("SQLException while closing DbUnit connection",
                        e);
            }
        }
        for (EntityManager em : entityManagersForDbUnit.values()) {
            em.close();
        }
        iDatabaseConnectionsForDbUnit.clear();
        jdbcConnectionsForDbUnit.clear();
        entityManagersForDbUnit.clear();
    }


    /** Clear a database. Tables are truncated, and sequence values
     * for auto-incrementing columns are reset.
     * @param dbs The database which is to be cleared.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     */
    public static void clearDatabase(final DatabaseSelector dbs) throws
        DatabaseUnitException, HibernateException, IOException, SQLException {
        Connection conn = getJDBCConnectionForDbUnit(dbs);
        IDatabaseConnection connection = getIDatabaseConnectionForDbUnit(dbs);

        FlatXmlDataSet dataset = new FlatXmlDataSetBuilder()
                .setMetaDataSetFromDtd(getResourceAsInputStream(
                        dbs.getDTDFilename()))
                .build(getResourceAsInputStream(
                        dbs.getBlankDataFilename()));

        // Delete the contents of the tables referred to in
        // the dataset.
        DatabaseOperation.DELETE_ALL.execute(connection, dataset);

        // Now reset the sequences used for auto-increment columns.
        // As DbUnit does not provide direct support for this,
        // this is H2-specific!
        // Inspired by: http://stackoverflow.com/questions/8523423
        // Get the names of the tables referred to in the dataset
        // used for blanking ...
        String[] tableNames = dataset.getTableNames();
        // ... and convert that into a string of the form:
        // "'TABLE_1', 'TABLE_2', 'TABLE_3'"
        String tableNamesForQuery =
                Arrays.asList(tableNames).stream()
                .map(i -> "'" + i.toString() + "'")
                .collect(Collectors.joining(", "));

        // This query gets the names of the sequences used by
        // the tables in the dataset.
        String getSequencesQuery =
                "SELECT SEQUENCE_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                    + "WHERE TABLE_SCHEMA='PUBLIC' AND TABLE_NAME IN ("
                    + tableNamesForQuery
                    + ") AND SEQUENCE_NAME IS NOT NULL";

        Set<String> sequences = new HashSet<>();
        try (Statement s = conn.createStatement();
                ResultSet rs = s.executeQuery(getSequencesQuery)) {
            while (rs.next()) {
                sequences.add(rs.getString(1));
            }
            for (String seq : sequences) {
                s.executeUpdate("ALTER SEQUENCE "
                        + seq + " RESTART WITH 1");
            }
        }
        // Force commit at the JDBC level, as closing the EntityManager
        // does a rollback!
        conn.commit();

        // And now flush any of our own caches. For now, that means the Owner
        // cache on top of the Registry database.
        if (dbs == DatabaseSelector.REGISTRY) {
            Owners.clear();
        }
    }

    /** Load a DbUnit test file into a database.
     * The file is loaded as a {@code FlatXmlDataSet}.
     * To make it more convenient to enter JSON data, the dataset is
     * wrapped as a {@code ReplacementDataSet}, and all instances
     * of '' (two contiguous apostrophes) are replaced with "
     * (one double quote).
     * @param dbs The database into which the test file is to be loaded.
     * @param testName The name of the test method. Used to generate
     *      the filename of the file to load.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     */
    public static void loadDbUnitTestFile(final DatabaseSelector dbs,
            final String testName) throws
        DatabaseUnitException, HibernateException, IOException, SQLException {
        Connection conn = getJDBCConnectionForDbUnit(dbs);
        IDatabaseConnection connection = getIDatabaseConnectionForDbUnit(dbs);

        FlatXmlDataSet xmlDataset = new FlatXmlDataSetBuilder()
                .setMetaDataSetFromDtd(getResourceAsInputStream(
                        dbs.getDTDFilename()))
                .build(getResourceAsInputStream(
                        "test/tests/"
                        + testName
                        + "/input-" + dbs.getNameLowerCase()
                        + "-dbunit.xml"));
        ReplacementDataSet dataset = new ReplacementDataSet(xmlDataset);
        addReplacementSubstringsToDataset(dataset);
        logger.info("doing clean_insert");
        DatabaseOperation.CLEAN_INSERT.execute(connection, dataset);
        // Force commit at the JDBC level, as closing the EntityManager
        // does a rollback!
        conn.commit();
    }

    /** Load a DbUnit test file into a database as an update.
     * The file is loaded as a {@code FlatXmlDataSet}.
     * To make it more convenient to enter JSON data, the dataset is
     * wrapped as a {@code ReplacementDataSet}, and all instances
     * of '' (two contiguous apostrophes) are replaced with "
     * (one double quote).
     * The data is loaded in as an update.
     * @param dbs The database into which the test file is to be loaded.
     * @param testName The name of the test method. Used to generate
     *      the path to the file to load.
     * @param filename The name of the file to be loaded.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem getting test data for DbUnit.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     */
    public static void loadDbUnitTestFileAsUpdate(final DatabaseSelector dbs,
            final String testName,
            final String filename) throws
        DatabaseUnitException, HibernateException, IOException, SQLException {
        Connection conn = getJDBCConnectionForDbUnit(dbs);
        IDatabaseConnection connection = getIDatabaseConnectionForDbUnit(dbs);

        FlatXmlDataSet xmlDataset = new FlatXmlDataSetBuilder()
                .setMetaDataSetFromDtd(getResourceAsInputStream(
                        dbs.getDTDFilename()))
                .build(getResourceAsInputStream(
                        "test/tests/"
                        + testName
                        + "/" + filename));
        ReplacementDataSet dataset = new ReplacementDataSet(xmlDataset);
        addReplacementSubstringsToDataset(dataset);
        logger.info("doing update");
        DatabaseOperation.UPDATE.execute(connection, dataset);
        // Force commit at the JDBC level, as closing the EntityManager
        // does a rollback!
        conn.commit();
    }

    /** Export the DbUnit database schema of a database as a DTD.
     * @param dbs The database for which the DTD is to be exported.
     * @param dtdExportFilename The name of the file into which the
     *      DTD export is to go.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws IOException If a problem opening or closing the output file.
     */
    public static void exportDbUnitDTD(final DatabaseSelector dbs,
            final String dtdExportFilename) throws
        DatabaseUnitException, SQLException, IOException {
        IDatabaseConnection connection = getIDatabaseConnectionForDbUnit(dbs);

        IDataSet dataSet = connection.createDataSet();
        Writer out =
                new OutputStreamWriter(new FileOutputStream(
                        dtdExportFilename), StandardCharsets.UTF_8);
        FlatDtdWriter datasetWriter = new FlatDtdWriter(out);
        datasetWriter.setContentModel(FlatDtdWriter.CHOICE);
        // You could also use the sequence model, which is the default:
        // datasetWriter.setContentModel(FlatDtdWriter.SEQUENCE);
        datasetWriter.write(dataSet);
        out.close();
    }

    /** Do a full export of a database in DbUnit format.
     * @param dbs The database which is to be exported.
     * @param exportFilename The name of the file into which the
     *      export is to go.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws IOException If a problem writing the export.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     */
    public static void exportFullDbUnitData(final DatabaseSelector dbs,
            final String exportFilename) throws
        DatabaseUnitException, HibernateException, IOException, SQLException {
        IDatabaseConnection connection = getIDatabaseConnectionForDbUnit(dbs);
        IDataSet fullDataSet = connection.createDataSet();
        FlatXmlDataSet.write(fullDataSet,
                new FileOutputStream(exportFilename));
    }

    /** Client-side clearing of a database.
     * @param dbs The database to be cleared.
     * @param baseURL The base URL to use to connect to the Toolkit.
     */
    public static void clientClearDatabase(final DatabaseSelector dbs,
            final URL baseURL) {
        logger.info("In clientClearDatabase()");
        Response response = null;
        try {
            response = NetClientUtils.doGetWithAdditionalComponents(
                    baseURL, "testing/clearDB",
                    webTarget -> webTarget.queryParam("db", dbs));

            Assert.assertEquals(response.getStatusInfo().getFamily(),
                    Family.SUCCESSFUL,
                    "clientClearDatabase response status");
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    /** Client-side loading of a database.
     * @param dbs The database into which data is to be loaded.
     * @param baseURL The base URL to use to connect to the Toolkit.
     * @param testName The name of the test method. Used to generate
     *      the filename of the file to load.
     */
    public static void clientLoadDbUnitTestFile(final DatabaseSelector dbs,
            final URL baseURL,
            final String testName) {
        logger.info("In clientLoadDbUnitTestFile()");
        Response response = null;
        try {
            response = NetClientUtils.doGetWithAdditionalComponents(
                    baseURL, "testing/loadDB",
                    webTarget -> webTarget.queryParam("db", dbs)
                        .queryParam("testName", testName));

            Assert.assertEquals(response.getStatusInfo().getFamily(),
                    Family.SUCCESSFUL,
                    "clientLoadDbUnitTestFile response status");
        } finally {
            response.close();
        }
    }

    /** Client-side loading of a database as an update.
     * @param dbs The database into which data is to be loaded.
     * @param baseURL The base URL to use to connect to the Toolkit.
     * @param testName The name of the test method. Used to generate
     *      the path to the file to load.
     * @param filename The name of the file to be loaded.
     */
    public static void clientLoadDbUnitTestFileAsUpdate(
            final DatabaseSelector dbs,
            final URL baseURL, final String testName, final String filename) {
        logger.info("In clientLoadDbUnitTestFileAsUpdate()");
        Response response = null;
        try {
            response = NetClientUtils.doGetWithAdditionalComponents(
                    baseURL, "testing/loadDBAsUpdate",
                    webTarget ->
                        webTarget.queryParam("db", dbs)
                            .queryParam("testName", testName)
                            .queryParam("filename", filename));

            Assert.assertEquals(response.getStatusInfo().getFamily(),
                    Family.SUCCESSFUL,
                    "clientLoadDbUnitTestFileAsUpdate response status");
        } finally {
            response.close();
        }
    }

    /** Get the current contents of a database.
     * @param dbs The database from which the data is to be fetched.
     * @return The current contents of the database.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     */
    public static IDataSet getDatabaseCurrentContents(
            final DatabaseSelector dbs) throws
            DatabaseUnitException, HibernateException, SQLException {
        IDatabaseConnection connection = getIDatabaseConnectionForDbUnit(dbs);
        IDataSet databaseDataSet = connection.createDataSet();
        return databaseDataSet;
    }

    /** Get the current contents of a database table.
     * @param dbs The database from which the data is to be fetched.
     * @param tableName The name of the database table to be fetched.
     * @return The current contents of the database table.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     */
    public static ITable getDatabaseTableCurrentContents(
            final DatabaseSelector dbs,
            final String tableName) throws
            DatabaseUnitException, HibernateException, SQLException {
        IDatabaseConnection connection = getIDatabaseConnectionForDbUnit(dbs);
        IDataSet databaseDataSet = connection.createDataSet();
        ITable currentTable = databaseDataSet.getTable(tableName);
        return currentTable;
    }

    /** Get the contents of a dataset in a file containing expected
     * database contents.
     * @param dbs The database into which the test file is to be loaded.
     * @param filename The filename of the file containing the expected
     *      database contents.
     * @return The contents of the database table.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws IOException If reading the DTD fails.
     */
    public static IDataSet getDatabaseTableExpectedContents(
            final DatabaseSelector dbs,
            final String filename) throws
            DatabaseUnitException, IOException {
        FlatXmlDataSet xmlDataset = new FlatXmlDataSetBuilder()
                .setMetaDataSetFromDtd(getResourceAsInputStream(
                        dbs.getDTDFilename()))
                .build(getResourceAsInputStream(
                        filename));
        ReplacementDataSet dataset = new ReplacementDataSet(xmlDataset);
        addReplacementSubstringsToDataset(dataset);
        return dataset;
    }

    /** Get the current contents of a database and the expected
     * contents, and assert their equality.
     * @param dbs The database from which the data is to be fetched.
     * @param filename The filename of the file containing the expected
     *      database contents.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws IOException If reading the DTD fails.
     */
    public static void compareDatabaseCurrentAndExpectedContents(
            final DatabaseSelector dbs,
            final String filename) throws
            DatabaseUnitException, HibernateException, SQLException,
            IOException {
        IDatabaseConnection connection = getIDatabaseConnectionForDbUnit(dbs);
        IDataSet databaseDataSet = connection.createDataSet();
        FlatXmlDataSet xmlDataset = new FlatXmlDataSetBuilder()
                .setMetaDataSetFromDtd(getResourceAsInputStream(
                        dbs.getDTDFilename()))
                .build(getResourceAsInputStream(
                        filename));
        ReplacementDataSet expectedDataset =
                new ReplacementDataSet(xmlDataset);
        addReplacementSubstringsToDataset(expectedDataset);

        // Make a combined dataset that ensures that we have all of
        // the tables defined. This means that the file doesn't have
        // to mention tables that aren't used in the test.
        IDataSet combinedDataSet = new CompositeDataSet(
                getBlankIDataSetForDbUnit(dbs), expectedDataset);

        Assertion.assertEquals(combinedDataSet, databaseDataSet);
    }

    /** Map of ValueComparers to use when comparing tasks. */
    private static Map<String, Map<String, ValueComparer>>
        valueComparersForTasks;

    static {
        valueComparersForTasks = new HashMap<>();
        Map<String, ValueComparer> innerMap = new HashMap<>();
        TaskComparer taskComparer = new TaskComparer();
        innerMap.put("PARAMS", taskComparer);
        innerMap.put("RESPONSE", taskComparer);
        valueComparersForTasks.put("TASKS", innerMap);
    }

    /** Get the current contents of a database and the expected
     * contents, and assert their equality, apart from timestamps
     * in the tasks table.
     * @param dbs The database from which the data is to be fetched.
     * @param filename The filename of the file containing the expected
     *      database contents.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws IOException If reading the DTD fails.
     */
    public static void
    compareDatabaseCurrentAndExpectedContentsIgnoreTaskTimestamps(
            final DatabaseSelector dbs,
            final String filename) throws
            DatabaseUnitException, HibernateException, SQLException,
            IOException {
        IDatabaseConnection connection = getIDatabaseConnectionForDbUnit(dbs);
        IDataSet databaseDataSet = connection.createDataSet();
        FlatXmlDataSet xmlDataset = new FlatXmlDataSetBuilder()
                .setMetaDataSetFromDtd(getResourceAsInputStream(
                        dbs.getDTDFilename()))
                .build(getResourceAsInputStream(
                        filename));
        ReplacementDataSet expectedDataset =
                new ReplacementDataSet(xmlDataset);
        addReplacementSubstringsToDataset(expectedDataset);

        // Make a combined dataset that ensures that we have all of
        // the tables defined. This means that the file doesn't have
        // to mention tables that aren't used in the test.
        IDataSet combinedDataSet = new CompositeDataSet(
                getBlankIDataSetForDbUnit(dbs), expectedDataset);

        Assertion.assertWithValueComparer(combinedDataSet, databaseDataSet,
                ValueComparers.isActualEqualToExpected,
                valueComparersForTasks);
    }

    /** Map of ValueComparers to use when comparing subscribers. */
    private static Map<String, Map<String, ValueComparer>>
        valueComparersForSubscribers;

    static {
        valueComparersForSubscribers = new HashMap<>();
        Map<String, ValueComparer> innerMap = new HashMap<>();
        SubscriberComparer subscriberComparer = new SubscriberComparer();
        innerMap.put("TOKEN", subscriberComparer);
        valueComparersForSubscribers.put("SUBSCRIBERS", innerMap);
    }

    /** Get the current contents of a database and the expected
     * contents, and assert their equality, apart from tokens
     * in the subscribers table.
     * @param dbs The database from which the data is to be fetched.
     * @param filename The filename of the file containing the expected
     *      database contents.
     * @throws DatabaseUnitException If a problem with DbUnit.
     * @throws HibernateException If a problem getting the underlying
     *          JDBC connection.
     * @throws SQLException If DbUnit has a problem performing
     *           performing JDBC operations.
     * @throws IOException If reading the DTD fails.
     */
    public static void
    compareDatabaseCurrentAndExpectedContentsIgnoreSubscriberTokens(
            final DatabaseSelector dbs,
            final String filename) throws
            DatabaseUnitException, HibernateException, SQLException,
            IOException {
        IDatabaseConnection connection = getIDatabaseConnectionForDbUnit(dbs);
        IDataSet databaseDataSet = connection.createDataSet();
        FlatXmlDataSet xmlDataset = new FlatXmlDataSetBuilder()
                .setMetaDataSetFromDtd(getResourceAsInputStream(
                        dbs.getDTDFilename()))
                .build(getResourceAsInputStream(
                        filename));
        ReplacementDataSet expectedDataset =
                new ReplacementDataSet(xmlDataset);
        addReplacementSubstringsToDataset(expectedDataset);

        // Make a combined dataset that ensures that we have all of
        // the tables defined. This means that the file doesn't have
        // to mention tables that aren't used in the test.
        IDataSet combinedDataSet = new CompositeDataSet(
                getBlankIDataSetForDbUnit(dbs), expectedDataset);

        Assertion.assertWithValueComparer(combinedDataSet, databaseDataSet,
                ValueComparers.isActualEqualToExpected,
                valueComparersForSubscribers);
    }

    /** Compare two files containing JSON, asserting that they contain
     * the same content.
     * @param testFilename The filename of the file containing the generated
     *      content. An TestNG assertion is made that this file exists.
     * @param correctFilename The filename of the file containing the correct
     *      value.
     * @throws IOException If reading either file fails.
     */
    public static void compareJsonFiles(final String testFilename,
            final String correctFilename) throws IOException {
        File testFile = new File(testFilename);
        FileAssert.assertFile(testFile,
                "Test file (" + testFilename + ") is not "
                        + "a proper file");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode testJson;
        JsonNode correctJson;
        // IOException not caught here, but allowed to propagate.
        testJson = mapper.readTree(new File(testFilename));
        correctJson = mapper.readTree(new File(correctFilename));
        Assert.assertEquals(testJson, correctJson);
        // NB This uses a top-level equality test done by TestNG.
        // There is also a top-level equality test implemented by Jackson:
        // correctJson.equals(testJson). The TestNG one seems to give
        // the same end result, but gives better diagnostics in case
        // a difference is found.
    }

    /** Compare JSON content provided as a JsonNode, with expected content
     * stored in a file, asserting that they contain the same content.
     * @param testJson A JsonNode containing the actual JSON data.
     * @param correctFilename The filename of the file containing the
     *      expected JSON data.
     * @throws IOException If reading from the file fails.
     */
    public static void compareJsonNodeWithFile(final JsonNode testJson,
            final String correctFilename) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode correctJson;
        // IOException not caught here, but allowed to propagate.
        correctJson = mapper.readTree(new File(correctFilename));
        Assert.assertEquals(testJson, correctJson);
        // NB This uses a top-level equality test done by TestNG.
        // There is also a top-level equality test implemented by Jackson:
        // correctJson.equals(testJson). The TestNG one seems to give
        // the same end result, but gives better diagnostics in case
        // a difference is found.
    }


    /** Compare JSON content provided as a String, with expected content
     * stored in a file, asserting that they contain the same content.
     * @param testString A String containing the actual JSON data.
     * @param correctFilename The filename of the file containing the
     *      expected JSON data.
     * @throws IOException If reading from the file fails.
     */
    public static void compareJsonStringWithFile(final String testString,
            final String correctFilename) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode testJson;
        JsonNode correctJson;
        // IOException not caught here, but allowed to propagate.
        testJson = mapper.readTree(testString);
        correctJson = mapper.readTree(new File(correctFilename));
        Assert.assertEquals(testJson, correctJson);
        // NB This uses a top-level equality test done by TestNG.
        // There is also a top-level equality test implemented by Jackson:
        // correctJson.equals(testJson). The TestNG one seems to give
        // the same end result, but gives better diagnostics in case
        // a difference is found.
    }

    /** Path to files contained in the WireMock directory. */
    private static final Path WIREMOCK_FILES_PATH_CLIENT_SIDE =
            Paths.get(ArquillianBaseTest.RESOURCES_DEPLOY_PATH,
                    "wiremock", "__files");

    /** Resolve a filename relative to the directory in which the
     * WireMock stubbed content is stored.
     * @param filename The basename of the file to be looked up. For example,
     *      "body-PoolParty-api-projects.json".
     * @return The absolute path of the file looked up. For example,
     *      "/full/path/wiremock/__files/body-PoolParty-api-projects.json".
     */
    public static String clientResolveWireMockStubbedContentFilename(
            final String filename) {
        return WIREMOCK_FILES_PATH_CLIENT_SIDE.resolve(filename).toString();
    }

    /** URL to access the Sesame server. */
    private static String sesameServer = RegistryProperties.getProperty(
            PropertyConstants.SESAME_IMPORTER_SERVERURL);

    /** Remove all of the existing Sesame repositories.
     * @throws RepositoryException If there is a problem connecting with
     *   Sesame.
     * @throws RepositoryConfigException If there is a repository
     *  configuration problem.
     */
    public static void removeAllSesameRepositories()
            throws RepositoryConfigException, RepositoryException {
        RepositoryManager manager =
                RepositoryProvider.getRepositoryManager(sesameServer);
        // Parameter true means: omit the SYSTEM repository.
        for (RepositoryInfo repo : manager.getAllRepositoryInfos(true)) {
            logger.info("Cleaning up Sesame repository: " + repo.getId());
            manager.removeRepository(repo.getId());
        }
    }

}
