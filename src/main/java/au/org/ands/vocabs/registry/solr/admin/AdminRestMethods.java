/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.solr.admin;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.context.ApiPaths;
import au.org.ands.vocabs.registry.api.user.ErrorResult;
import au.org.ands.vocabs.registry.api.user.SimpleResult;
import au.org.ands.vocabs.registry.solr.EntityIndexer;

/** Solr index administration tools available through a REST-like
 * interface. */
@Path(ApiPaths.API_ADMIN + "/" + AdminRestMethods.API_SOLR)
public class AdminRestMethods {

    /** Intermediate path to all the methods in this class. */
    protected static final String API_SOLR = "solr";

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Index all vocabularies. All existing documents in the index
     * are removed first.
     * @return Result of the indexing.
     */
    @Path("index")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @GET
    public final Response indexAll() {
        try {
            EntityIndexer.unindexAllVocabularies();
            EntityIndexer.indexAllVocabularies();
        } catch (IOException | SolrServerException e) {
            logger.error("indexAll: got exception",  e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                    new ErrorResult("Exception: " + e.toString())).build();
        }
        return Response.ok().entity(new SimpleResult("OK")).build();
    }

    /** Index one vocabulary. Any existing document in the index
     * with the same ID is removed first.
     * @param vocabularyId The vocabulary ID of the vocabulary to be indexed.
     * @return Result of the indexing.
     */
    @Path("index" + "/" + ApiPaths.VOCABULARY_ID)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @GET
    public final Response indexOne(@PathParam("vocabularyId") final Integer
            vocabularyId) {
        try {
            EntityIndexer.unindexVocabulary(vocabularyId);
            EntityIndexer.indexVocabulary(vocabularyId);
        } catch (IOException | SolrServerException e) {
            logger.error("indexAll: got exception",  e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                    new ErrorResult("Exception: " + e.toString())).build();
        }
        return Response.ok().entity(new SimpleResult("OK")).build();
    }

}
