/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.utils;

import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sparql.SPARQLRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.context.AdminApiPaths;
import au.org.ands.vocabs.registry.db.dao.SubjectResolverEntryDAO;
import au.org.ands.vocabs.registry.db.dao.SubjectResolverSourceDAO;
import au.org.ands.vocabs.registry.db.entity.SubjectResolverEntry;
import au.org.ands.vocabs.registry.db.entity.SubjectResolverSource;

/** Utility class for populating the subject_resolver table based on
 * the contents of the subject_resolver_sources table.
 * The populate method is accessible only via a loopback interface.
 */
@Path(AdminApiPaths.API_ADMIN + "/" + AdminApiPaths.DATABASE)
public class PopulateSubjectResolver {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** SPARQL Query to fetch subjects. Subjects are SKOS Concepts.
     * Select only English labels (or fall back to unspecified
     * language). Notations are optional. */
    private static final String SUBJECT_QUERY_STRING =
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n"
            + "SELECT DISTINCT ?iri ?label ?notation WHERE {\n"
            + "  ?iri a skos:Concept .\n"
            + "  ?iri skos:prefLabel ?label .\n"
            + "  FILTER ( lang(?label) IN (\"\", \"en\"))\n"
            + "  OPTIONAL { ?iri skos:notation ?notation }\n"
            + "}";

    /** Populate the subject_resolver table with the subjects extracted
     * from the sources given in the subject_resolver_sources table.
     * @param request The HTTP request.
     * @return Text indicating success.
     */
    @Path("populateSubjectResolver")
    @GET
    public String populateSubjectResolver(
            @Context final HttpServletRequest request) {
        logger.info("In populateSubjectResolver");
        // Only allow access from a loopback interface.
        String remoteAddress = request.getRemoteAddr();
        InetAddress ipAddress;
        try {
            ipAddress = InetAddress.getByName(remoteAddress);
        } catch (UnknownHostException e) {
            logger.error("Unable to parse IP address", e);
            return "Unable to parse caller's IP address";
        }

        if (!ipAddress.isLoopbackAddress()) {
            return "Not localhost; forbidden";
        }

        // First, clean out any existing entries.
        SubjectResolverEntryDAO.deleteAllResolverEntries();
        // Get the sources we will use ...
        List<SubjectResolverSource> subjectResolverSources =
                SubjectResolverSourceDAO.getAllSubjectResolverSource();
        // ... and iterate over them.
        for (SubjectResolverSource source
                : subjectResolverSources) {
            logger.info("Getting subjects from source: " + source.getSource());
            try {
                Repository repo = new SPARQLRepository(source.getIri());
                repo.initialize();
                RepositoryConnection conn = repo.getConnection();
                try {
                    TupleQuery tupleQuery = conn.prepareTupleQuery(
                            QueryLanguage.SPARQL, SUBJECT_QUERY_STRING);
                    TupleQueryResult result = tupleQuery.evaluate();
                    try {
                        while (result.hasNext()) {
                            BindingSet bindingSet = result.next();
                            Value iri = bindingSet.getValue("iri");
                            Value label = bindingSet.getValue("label");
                            Value notation = bindingSet.getValue("notation");
                            SubjectResolverEntry sre =
                                    new SubjectResolverEntry();
                            sre.setSource(source.getSource());
                            sre.setIri(iri.stringValue());
                            sre.setLabel(label.stringValue());
                            // Cope with there being no notation.
                            String notationString = "";
                            if (notation != null) {
                                notationString = notation.stringValue();
                            }
                            sre.setNotation(notationString);
                            SubjectResolverEntryDAO.saveSubjectResolverEntry(
                                    sre);
                        }
                    } finally {
                        result.close();
                    }
                } finally {
                    conn.close();
                }
            } catch (OpenRDFException e) {
                logger.error("Got an exception", e);
                return "There was an exception: " + e.toString();
            }
        }
        return "Done.";
    }

}
