/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

import javax.persistence.EntityManager;

import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatDtdWriter;
import org.dbunit.ext.h2.H2Connection;
import org.hibernate.internal.SessionImpl;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.toolkit.test.utils.DatabaseSelector;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/** Utility program to generate the DbUnit DTD for one of the databases.
 * Needs to be invoked with a suitable classpath, system properties
 * for configuration files, and a database name as a parameter.
 * See the top-level build.xml for the generate-DTD task, which
 * provides these.
 * */
public final class GenerateDbUnitDTD {

    /** Private constructor for a utility class. */
    private GenerateDbUnitDTD() {
    }

    /** Main program. Generate the DbUnit DTD for one of the databases.
     * @param args One parameter, which is the name of the database for
     *      which the DbUnit DTD is to be generated. For this purpose,
     *      a name is one of the values of the {@link DatabaseSelector} enum.
     */
    public static void main(final String[] args) {
        // Turn off all Logback logging.
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.OFF);

        if (args.length != 1) {
            System.err.println("Must be exactly one command-line parameter");
            System.exit(1);
        }
        DatabaseSelector dbs;
        try {
            dbs = DatabaseSelector.valueOf(args[0]);
        } catch (IllegalArgumentException e) {
            System.err.println("Not a valid database name");
            System.exit(1);
            // Yes, putting return here is silly, but without it,
            // we get an error on the following line that dbs
            // might not be initialized.
            return;
        }

        EntityManager em = ArquillianTestUtils.getEntityManagerForDb(dbs);
        try (Connection conn = em.unwrap(SessionImpl.class).connection()) {
            IDatabaseConnection connection = new H2Connection(conn, null);
            IDataSet dataSet = connection.createDataSet();
            Writer out = new OutputStreamWriter(System.out,
                    StandardCharsets.UTF_8);
            FlatDtdWriter datasetWriter = new FlatDtdWriter(out);
            datasetWriter.setContentModel(FlatDtdWriter.CHOICE);
            datasetWriter.write(dataSet);
            out.close();
        } catch (Exception e) {
            // Catch-all, because we don't know what else might happen
            // in future, and we want a record of that if/when
            // we get a type of exception we haven't seen before.
            System.err.println("Got an exception:");
            e.printStackTrace(System.err);
        }

        em.close();
    }
}
