/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.hibernate.internal.SessionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;

import au.org.ands.vocabs.registry.db.context.DBContext;

/** Support program as part of the redirection of SPARQL and SISSVoc
 *  URLs containing "current" as the version identifier. Use this
 *  as a RewriteMap of type "prg" in Apache HTTP server. */
public final class RewriteCurrent {

    /** Logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Private constructor for utility class. */
    private RewriteCurrent() {
    }

    /** Query string for accessing the current version. */
    private static final String QUERY_STRING =
            "SELECT ver.slug from vocabularies voc, versions ver "
            + "WHERE voc.end_date = '9999-12-01 00:00:00' "
            + "AND ver.end_date ='9999-12-01 00:00:00' "
            + "AND voc.slug= ? "
            + "AND voc.status='PUBLISHED' "
            + "AND ver.vocabulary_id = voc.vocabulary_id "
            + "AND ver.status='CURRENT'";

    /** Main program. Reads lines from standard input, until EOF reached.
     * For each line read, treat that as a vocabulary slug, and look
     * that up in the database to find the slug of the current version,
     * if there is one, and as long as it has been published.
     * Output the version slug.
     * If there is no such current version, output the string "NULL"
     * as required by the Apache HTTP Server RewriteMap.
     * NB, this relies on the uniqueness of slugs. Won't work if we
     * allow multiple owners with the same slug.
     * Logging absolutely must be set up so as not to display any
     * logging on standard output (as would happen by default for Hibernate).
     * Specify a custom logback.xml with -Dlogback.configurationFile=...
     * on the command-line to do this.
     * @param args Command-line arguments.
     */
    public static void main(final String[] args) {
        // This initializes the database connection using the properties
        // defined in the toolkit.properties file.
        EntityManager em = DBContext.getEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        // The following as a courtesy, but probably not needed.
        trans.setRollbackOnly();
        try {
            // Get the raw JDBC connection, for efficiency.
            // NB: This is Hibernate specific!
            Connection conn = em.unwrap(SessionImpl.class).connection();
            PreparedStatement stmt = conn.prepareStatement(QUERY_STRING);
            BufferedReader input =
                    new BufferedReader(new InputStreamReader(System.in,
                            StandardCharsets.UTF_8));
            String vocabularySlug;
            String versionSlug;
            vocabularySlug = input.readLine();
            while (vocabularySlug != null && vocabularySlug.length() != 0) {
                stmt.setString(1, vocabularySlug);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        versionSlug = rs.getString(1);
                    } else {
                        // No answer.
                        versionSlug = "NULL";
                    }
                } catch (CommunicationsException e) {
                    // Handle the specific case of the Connection object
                    // timing out. Close down, and try once more.
                    // If an exception happens here, the program will exit.
                    // Oops? Good enough for now, unless/until we observe
                    // any other type of behaviour.
                    stmt.close();
                    em.close();
                    em = DBContext.getEntityManager();
                    trans = em.getTransaction();
                    trans.begin();
                    trans.setRollbackOnly();
                    conn = em.unwrap(SessionImpl.class).connection();
                    stmt = conn.prepareStatement(QUERY_STRING);
                    stmt.setString(1, vocabularySlug);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            versionSlug = rs.getString(1);
                        } else {
                            // No answer.
                            versionSlug = "NULL";
                        }
                    }
                }
                System.out.println(versionSlug);
                System.out.flush();
                vocabularySlug = input.readLine();
            }
        } catch (Exception e) {
            // Catch-all, because we don't know what else might happen
            // in future, and we want a record of that if/when
            // we get a type of exception we haven't seen before.
            LOGGER.error("Exception in RewriteCurrent", e);
        }
        // Tidy up as a courtesy ... although, rollback() will throw
        // an exception if the connection has timed out.
        em.getTransaction().rollback();
        em.close();
    }

}
