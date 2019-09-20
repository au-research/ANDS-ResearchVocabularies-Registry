/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.http.HttpStatus;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient.RemoteSolrException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.jax.rs.annotations.Pac4JProfile;
import org.pac4j.jax.rs.annotations.Pac4JSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.auth.AuthUtils;
import au.org.ands.vocabs.registry.api.context.ApiPaths;
import au.org.ands.vocabs.registry.api.context.ResponseUtils;
import au.org.ands.vocabs.registry.api.context.SwaggerInterface;
import au.org.ands.vocabs.registry.db.context.DBContext;
import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.db.internal.VocabularyJson;
import au.org.ands.vocabs.registry.enums.VocabularyStatus;
import au.org.ands.vocabs.registry.log.Analytics;
import au.org.ands.vocabs.registry.log.Logging;
import au.org.ands.vocabs.registry.model.ModelMethods;
import au.org.ands.vocabs.registry.model.VocabularyModel;
import au.org.ands.vocabs.registry.solr.EntityIndexer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.ResponseHeader;

/** REST web services for deleting vocabularies. */
@Path(ApiPaths.API_RESOURCE + "/" + ApiPaths.VOCABULARIES)
@Api(value = SwaggerInterface.TAG_RESOURCES,
        authorizations = {@Authorization(value = SwaggerInterface.BASIC_AUTH),
        @Authorization(value = SwaggerInterface.API_KEY_AUTH)})
public class DeleteVocabularies {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Delete an instance of a vocabulary.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile.
     * @param vocabularyId The VocabularyId of the vocabulary to be deleted.
     * @param deleteCurrent Whether or not to delete the current version.
     * @param deleteDraft Whether or not to delete the draft version.
     * @return An empty response for success, or an error response. */
    @Path(ApiPaths.VOCABULARY_ID)
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Pac4JSecurity
    @DELETE
    @ApiOperation(value = "Delete a vocabulary.")
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_NO_CONTENT,
                    message = "Delete successful"),
            @ApiResponse(code = HttpStatus.SC_BAD_REQUEST,
                    message = "Invalid input",
                    response = ErrorResult.class),
            @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED,
                    message = "Not authenticated",
                    response = ErrorResult.class,
                    responseHeaders = {
                            @ResponseHeader(name = "WWW-Authenticate",
                                    response = String.class)
                            }),
            @ApiResponse(code = HttpStatus.SC_FORBIDDEN,
                    message = "Not authenticated, or not authorized",
                    response = ErrorResult.class),
            @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR,
                    message = "Internal server error",
                    response = ErrorResult.class)
            })
    public Response deleteVocabulary(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(hidden = true) @Pac4JProfile final CommonProfile profile,
            @ApiParam(value = "The ID of the vocabulary to be deleted.")
            @PathParam("vocabularyId") final Integer vocabularyId,
            @ApiParam(value = "Whether or not to delete the current version.",
                    defaultValue = "false")
            @QueryParam("deleteCurrent") @DefaultValue("false")
            final boolean deleteCurrent,
            @ApiParam(value = "Whether or not to delete the draft version.",
                    defaultValue = "false")
            @QueryParam("deleteDraft") @DefaultValue("false")
            final boolean deleteDraft) {
        logger.debug("called deleteVocabulary");

        if (!deleteCurrent && !deleteDraft) {
            return ErrorResultUtils.badRequest(
                    "Neither deleteCurrent nor deleteDraft was set.");
        }

        Vocabulary draftDbVocabulary = null;
        String draftOwner = null;
        String draftTitle = null;
        if (deleteDraft && VocabularyDAO.hasDraftVocabulary(vocabularyId)) {
            draftDbVocabulary = VocabularyDAO.getDraftVocabularyByVocabularyId(
                    vocabularyId).get(0);
            if (!AuthUtils.ownerIsAuthorizedByOrganisationOrUsername(profile,
                    draftDbVocabulary.getOwner())) {
                return ResponseUtils.generateForbiddenResponseForOwner();
            }
            draftOwner = draftDbVocabulary.getOwner();
            String data = draftDbVocabulary.getData();
            VocabularyJson vocabularyJson =
                    JSONSerialization.deserializeStringAsJson(data,
                            VocabularyJson.class);
            draftTitle = vocabularyJson.getTitle();
        }
        Vocabulary currentDbVocabulary = null;
        VocabularyStatus currentStatus = null;
        String currentOwner = null;
        String currentTitle = null;
        if (deleteCurrent) {
            currentDbVocabulary = VocabularyDAO.
                    getCurrentVocabularyByVocabularyId(vocabularyId);
            if (currentDbVocabulary != null) {
                if (!AuthUtils.ownerIsAuthorizedByOrganisationOrUsername(
                        profile, currentDbVocabulary.getOwner())) {
                    return ResponseUtils.generateForbiddenResponseForOwner();
                }
                currentStatus = currentDbVocabulary.getStatus();
                currentOwner = currentDbVocabulary.getOwner();
                String data = currentDbVocabulary.getData();
                VocabularyJson vocabularyJson =
                        JSONSerialization.deserializeStringAsJson(data,
                                VocabularyJson.class);
                currentTitle = vocabularyJson.getTitle();
            }
        }

        // Don't progress to loading the model, if we didn't find anything.
        // This also covers the case where the vocabularyId is not
        // even valid (and which would thus cause model creation to throw
        // an exception).
        if (draftDbVocabulary == null && currentDbVocabulary == null) {
            return ErrorResultUtils.badRequest(
                    "Vocabulary does not exist in the form(s) requested "
                    + "for deletion.");
        }

        EntityManager em = null;
        EntityTransaction txn = null;

        try {
            em = DBContext.getEntityManager();
            txn = em.getTransaction();
            txn.begin();

            VocabularyModel vm = ModelMethods.createVocabularyModel(em,
                    vocabularyId);
            LocalDateTime now = TemporalUtils.nowUTC();
            if (deleteDraft) {
                ModelMethods.deleteOnlyDraftVocabulary(vm,
                        profile.getUsername(), now);
            }
            if (deleteCurrent) {
                ModelMethods.deleteOnlyCurrentVocabulary(vm,
                        profile.getUsername(), now);
            }

            // And now, commit all of the above changes.
            txn.commit();
            // If we have reached this point, we have success.
            // Analytics logging.
            if (draftTitle != null) {
                Logging.logRequest(true, request, uriInfo, profile,
                        Analytics.EVENT_DELETE_VOCABULARY,
                        Analytics.ID_FIELD, vocabularyId,
                        Analytics.TITLE_FIELD, draftTitle,
                        Analytics.ENTITY_STATUS_FIELD, VocabularyStatus.DRAFT,
                        Analytics.OWNER_FIELD, draftOwner);
            }
            if (currentTitle != null) {
                Logging.logRequest(true, request, uriInfo, profile,
                        Analytics.EVENT_DELETE_VOCABULARY,
                        Analytics.ID_FIELD, vocabularyId,
                        Analytics.TITLE_FIELD, currentTitle,
                        Analytics.ENTITY_STATUS_FIELD, currentStatus,
                        Analytics.OWNER_FIELD, currentOwner);
                // Solr unindexing.
                EntityIndexer.unindexVocabulary(vocabularyId);
            }
            // Successful deletion, and no response body. noContent() creates
            // status code 204.
            return Response.noContent().build();
        } catch (Throwable t) {
            if (txn != null && txn.isActive()) {
                try {
                    logger.error("Exception during transaction; rolling back",
                            t);
                    txn.rollback();
                } catch (Exception e) {
                    logger.error("Rollback failure!", e);
                }
            } else {
                logger.error("Exception other than during transaction: ", t);
            }
            if (t instanceof RemoteSolrException) {
                // The vocabulary has been deleted, but it couldn't be
                // unindexed. Oops.
                logger.error("Solr unindexing failed");
            }
            if (t instanceof IllegalArgumentException) {
                // There was a validation error.
                return ErrorResultUtils.badRequest(
                        "Validation error: " + t.getMessage());
            }
            // Don't throw, but fall through so that the caller sees
            // an error message.
        } finally {
            if (em != null) {
                em.close();
            }
        }

        // Oh dear, we fell through. There was an exception we don't
        // know about, or, at least, can't provide useful feeback about.
        return ErrorResultUtils.internalServerError();
    }

}
