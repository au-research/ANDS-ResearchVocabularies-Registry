/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.solr.admin;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.http.HttpStatus;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient.RemoteSolrException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.jax.rs.annotations.Pac4JProfile;
import org.pac4j.jax.rs.annotations.Pac4JSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.auth.AuthUtils;
import au.org.ands.vocabs.registry.api.context.AdminApiPaths;
import au.org.ands.vocabs.registry.api.context.ApiPaths;
import au.org.ands.vocabs.registry.api.context.ResponseUtils;
import au.org.ands.vocabs.registry.api.context.SwaggerInterface;
import au.org.ands.vocabs.registry.api.user.ErrorResult;
import au.org.ands.vocabs.registry.api.user.SimpleResult;
import au.org.ands.vocabs.registry.log.Logging;
import au.org.ands.vocabs.registry.solr.EntityIndexer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.ResponseHeader;

/** Solr index administration tools available through a REST-like
 * interface. */
@Path(AdminApiPaths.API_ADMIN + "/" + AdminApiPaths.SOLR)
@Api(value = SwaggerInterface.TAG_ADMIN,
        authorizations = {@Authorization(value = SwaggerInterface.BASIC_AUTH),
        @Authorization(value = SwaggerInterface.API_KEY_AUTH)})
public class AdminRestMethods {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Index all vocabularies. All existing documents in the index
     * are removed first.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile.
     * @return Result of the indexing.
     */
    @Path("index")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Pac4JSecurity
    @GET
    @ApiOperation(value = "Force Solr indexing of all vocabularies.",
            notes = "This method is only available to administrator users.",
            response = SimpleResult.class)
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED,
                    message = "Not authenticated",
                    response = ErrorResult.class,
                    responseHeaders = {
                            @ResponseHeader(name = "WWW-Authenticate",
                                    response = String.class)
                            }),
            @ApiResponse(code = HttpStatus.SC_FORBIDDEN,
                    message = "Not authenticated, or not authorized",
                    response = ErrorResult.class)
            })
    public final Response indexAll(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(hidden = true) @Pac4JProfile
            final CommonProfile profile) {
        logger.info("Called indexAll");
        if (!AuthUtils.profileIsSuperuser(profile)) {
            return ResponseUtils.generateForbiddenResponseNotSuperuser();
        }
        try {
            EntityIndexer.indexAllVocabularies();
        } catch (IOException | SolrServerException | RemoteSolrException e) {
            logger.error("indexAll: got exception",  e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                    new ErrorResult("Exception: " + e.toString())).build();
        }
        Logging.logRequest(true, request, uriInfo, profile,
                "Admin: index all vocabularies");
        return Response.ok().entity(new SimpleResult("OK")).build();
    }

    /** Index one vocabulary. Any existing document in the index
     * with the same ID is removed first.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile.
     * @param vocabularyId The vocabulary ID of the vocabulary to be indexed.
     * @return Result of the indexing.
     */
    @Path("index" + "/" + ApiPaths.VOCABULARY_ID)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Pac4JSecurity
    @GET
    @ApiOperation(value = "Force Solr indexing of one vocabulary.",
            notes = "This method is only available to administrator users.",
            response = SimpleResult.class)
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED,
                    message = "Not authenticated",
                    response = ErrorResult.class,
                    responseHeaders = {
                            @ResponseHeader(name = "WWW-Authenticate",
                                    response = String.class)
                            }),
            @ApiResponse(code = HttpStatus.SC_FORBIDDEN,
                    message = "Not authenticated, or not authorized",
                    response = ErrorResult.class)
            })
    public final Response indexOne(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(hidden = true) @Pac4JProfile
            final CommonProfile profile,
            @PathParam("vocabularyId") final Integer
            vocabularyId) {
        logger.info("Called index: " + vocabularyId);
        if (!AuthUtils.profileIsSuperuser(profile)) {
            return ResponseUtils.generateForbiddenResponseNotSuperuser();
        }
        try {
            EntityIndexer.unindexVocabulary(vocabularyId);
            EntityIndexer.indexVocabulary(vocabularyId);
        } catch (IOException | SolrServerException | RemoteSolrException e) {
            logger.error("indexAll: got exception",  e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                    new ErrorResult("Exception: " + e.toString())).build();
        }
        Logging.logRequest(true, request, uriInfo, profile,
                "Admin: index one vocabulary");
        return Response.ok().entity(new SimpleResult("OK")).build();
    }

}
