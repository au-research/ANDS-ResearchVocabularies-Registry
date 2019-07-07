/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
import au.org.ands.vocabs.registry.api.context.EntityPaths;
import au.org.ands.vocabs.registry.api.context.ResponseUtils;
import au.org.ands.vocabs.registry.api.context.SwaggerInterface;
import au.org.ands.vocabs.registry.api.validation.AuthorizationValidationUtils;
import au.org.ands.vocabs.registry.api.validation.CheckVocabulary;
import au.org.ands.vocabs.registry.api.validation.ValidationMode;
import au.org.ands.vocabs.registry.db.context.DBContext;
import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyIdDAO;
import au.org.ands.vocabs.registry.db.entity.VocabularyId;
import au.org.ands.vocabs.registry.enums.VocabularyStatus;
import au.org.ands.vocabs.registry.log.Analytics;
import au.org.ands.vocabs.registry.log.Logging;
import au.org.ands.vocabs.registry.model.ModelMethods;
import au.org.ands.vocabs.registry.model.VocabularyModel;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;
import au.org.ands.vocabs.registry.solr.EntityIndexer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.ResponseHeader;

/** REST web services for creating and modifying vocabularies. */
@Path(ApiPaths.API_RESOURCE + "/" + ApiPaths.VOCABULARIES)
@Api(value = SwaggerInterface.TAG_RESOURCES,
        authorizations = {@Authorization(value = SwaggerInterface.BASIC_AUTH),
        @Authorization(value = SwaggerInterface.API_KEY_AUTH)})
public class PutVocabularies {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Create a new vocabulary.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile.
     * @param newVocabulary The vocabulary to be created.
     * @return The new vocabulary, in either XML or JSON format. */
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Pac4JSecurity
    @POST
    @ApiOperation(value = "Create a new vocabulary.",
            code = HttpStatus.SC_CREATED,
            responseHeaders = {
                    @ResponseHeader(name = "Location",
                            description = "URL of the newly-created vocabulary",
                            response = URL.class)
            },
            response = Vocabulary.class)
    @ApiResponses(value = {
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
    public final Response createVocabulary(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(hidden = true) @Pac4JProfile final CommonProfile profile,
            @ApiParam(value = "The vocabulary to be added.",
                required = true)
            @NotNull(message = "The vocabulary must not be null")
            @CheckVocabulary final Vocabulary newVocabulary) {
        logger.debug("called createVocabulary");

        if (!AuthUtils.ownerIsAuthorizedByOrganisationOrUsername(profile,
                newVocabulary.getOwner())) {
            return ResponseUtils.generateForbiddenResponseForOwner();
        }

        List<ValidationError> validationErrors =
                AuthorizationValidationUtils.checkAuthorizationForContent(
                        profile, newVocabulary, ValidationMode.CREATE);
        if (!validationErrors.isEmpty()) {
            ErrorResult errorResult =
                    new ErrorResult("Won't create vocabulary.");
            errorResult.setConstraintViolation(validationErrors);
            return Response.status(Response.Status.BAD_REQUEST).
                    entity(errorResult).build();
        }

        EntityManager em = null;
        EntityTransaction txn = null;

        try {
            em = DBContext.getEntityManager();
            txn = em.getTransaction();
            txn.begin();

            // The model requires a vocabulary Id, so make a new one.
            VocabularyId vocabularyId = new VocabularyId();
            VocabularyIdDAO.saveVocabularyId(em, vocabularyId);
            // That needs to be visible to subsequent queries, so that
            // ModelMethods.createVocabularyModel() will find it.
            // Because it has an auto-increment column, it _is_
            // flushed ... within the current transaction.
            // So need to make sure that all subsequent database access
            // is done using the _same_ EntityManager.
            Integer newVocabularyId = vocabularyId.getId();
            logger.info("Created new vocabulary Id: " + newVocabularyId);

            VocabularyModel vm = ModelMethods.createVocabularyModel(em,
                    newVocabularyId);
            LocalDateTime now = TemporalUtils.nowUTC();
            ModelMethods.applyChanges(vm, profile.getUsername(),
                    now, newVocabulary);

            // And now, commit all of the above changes; the db is
            // read-only from here.
            txn.commit();
            // If we have reached this point, we have success.
            // Analytics logging.
            Logging.logRequest(true, request, uriInfo, profile,
                    Analytics.EVENT_CREATE_VOCABULARY,
                    Analytics.ID_FIELD, newVocabularyId,
                    Analytics.TITLE_FIELD, newVocabulary.getTitle(),
                    Analytics.SLUG_FIELD, newVocabulary.getSlug(),
                    Analytics.ENTITY_STATUS_FIELD, newVocabulary.getStatus(),
                    Analytics.OWNER_FIELD, newVocabulary.getOwner());
            Vocabulary newVocabularyResponse;
            if (newVocabulary.getStatus() == VocabularyStatus.DRAFT) {
                newVocabularyResponse =
                        ModelMethods.getDraft(vm, true, true, true);
            } else {
                newVocabularyResponse =
                        ModelMethods.getCurrent(vm, true, true, true);
                // Solr indexing.
                EntityIndexer.indexVocabulary(newVocabularyId);
            }
            return Response.created(EntityPaths.getURIOfEntity(
                    newVocabularyResponse)).
                    entity(newVocabularyResponse).build();
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
                // The vocabulary has been created, but it couldn't be
                // indexed. Oops.
                logger.error("Solr indexing failed");
            }
            if (t instanceof IllegalArgumentException) {
                // There was a validation error.
                ErrorResult errorResult =
                        new ErrorResult("Validation error: " + t.getMessage());
                return Response.status(Response.Status.BAD_REQUEST).
                        entity(errorResult).build();
            }
            // Don't throw, but fall through so that the user sees
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

    /** Update a vocabulary.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile.
     * @param vocabularyId The VocabularyId of the vocabulary to be updated.
     * @param updatedVocabulary The vocabulary to be created.
     * @return The new vocabulary, in either XML or JSON format. */
    @SuppressWarnings("checkstyle:MethodLength")
    @Path(ApiPaths.VOCABULARY_ID)
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Pac4JSecurity
    @PUT
    @ApiOperation(value = "Update a vocabulary.", response = Vocabulary.class)
    @ApiResponses(value = {
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
    public final Response updateVocabulary(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(hidden = true) @Pac4JProfile final CommonProfile profile,
            @ApiParam(value = "The ID of the vocabulary to be updated.",
                 required = true)
            @PathParam("vocabularyId") final Integer vocabularyId,
            @ApiParam(value = "The vocabulary to be updated.",
                required = true)
            @NotNull(message = "The vocabulary must not be null.")
            @CheckVocabulary(mode = ValidationMode.UPDATE)
            final Vocabulary updatedVocabulary) {
        logger.debug("called updateVocabulary");

        if (!vocabularyId.equals(updatedVocabulary.getId())) {
            // Id parameter must match what's inside the metadata.
            logger.error("Vocabulary Id parameter doesn't match metadata.");
            List<ValidationError> validationErrors = new ArrayList<>();
            ValidationError ve = new ValidationError();
            ve.setMessage("Vocabulary Id parameter doesn't match metadata.");
            ve.setPath(vocabularyId.toString());
            validationErrors.add(ve);
            ErrorResult errorResult =
                    new ErrorResult("Won't update vocabulary.");
            errorResult.setConstraintViolation(validationErrors);
            Logging.logRequest(false, request, uriInfo, profile,
                    Analytics.EVENT_UPDATE_VOCABULARY,
                    Analytics.FAILURE_REASON, "validation",
                    Analytics.ID_FIELD, vocabularyId,
                    Analytics.TITLE_FIELD, updatedVocabulary.getTitle(),
                    Analytics.SLUG_FIELD, updatedVocabulary.getSlug(),
                    Analytics.ENTITY_STATUS_FIELD,
                        updatedVocabulary.getStatus(),
                    Analytics.OWNER_FIELD, updatedVocabulary.getOwner());
            return Response.status(Response.Status.BAD_REQUEST).
                    entity(errorResult).build();
        }

        // Need to have permissions on the existing draft, if there is one.
        List<au.org.ands.vocabs.registry.db.entity.Vocabulary>
        draftDbVocabularyList = VocabularyDAO.getDraftVocabularyByVocabularyId(
                vocabularyId);
        if (draftDbVocabularyList.size() > 0) {
            if (!AuthUtils.ownerIsAuthorizedByOrganisationOrUsername(profile,
                    draftDbVocabularyList.get(0).getOwner())) {
                Logging.logRequest(false, request, uriInfo, profile,
                        Analytics.EVENT_UPDATE_VOCABULARY,
                        Analytics.FAILURE_REASON, "authorization",
                        Analytics.ID_FIELD, vocabularyId,
                        Analytics.TITLE_FIELD, updatedVocabulary.getTitle(),
                        Analytics.SLUG_FIELD, updatedVocabulary.getSlug(),
                        Analytics.ENTITY_STATUS_FIELD,
                            updatedVocabulary.getStatus(),
                        Analytics.OWNER_FIELD, updatedVocabulary.getOwner());
                return ResponseUtils.generateForbiddenResponseForOwner();
            }
        }
        // Need to have permissions on the current instance, if there is one.
        au.org.ands.vocabs.registry.db.entity.Vocabulary
        dbVocabulary = VocabularyDAO.getCurrentVocabularyByVocabularyId(
                vocabularyId);
        if (dbVocabulary != null && !AuthUtils.
                ownerIsAuthorizedByOrganisationOrUsername(profile,
                dbVocabulary.getOwner())) {
            Logging.logRequest(false, request, uriInfo, profile,
                    Analytics.EVENT_UPDATE_VOCABULARY,
                    Analytics.FAILURE_REASON, "authorization",
                    Analytics.ID_FIELD, vocabularyId,
                    Analytics.TITLE_FIELD, updatedVocabulary.getTitle(),
                    Analytics.SLUG_FIELD, updatedVocabulary.getSlug(),
                    Analytics.ENTITY_STATUS_FIELD,
                        updatedVocabulary.getStatus(),
                    Analytics.OWNER_FIELD, updatedVocabulary.getOwner());
            return ResponseUtils.generateForbiddenResponseForOwner();
        }

        List<ValidationError> validationErrors =
                AuthorizationValidationUtils.checkAuthorizationForContent(
                        profile, updatedVocabulary, ValidationMode.UPDATE);
        if (!validationErrors.isEmpty()) {
            ErrorResult errorResult =
                    new ErrorResult("Won't update vocabulary.");
            errorResult.setConstraintViolation(validationErrors);
            Logging.logRequest(false, request, uriInfo, profile,
                    Analytics.EVENT_UPDATE_VOCABULARY,
                    Analytics.FAILURE_REASON, "validation",
                    Analytics.ID_FIELD, vocabularyId,
                    Analytics.TITLE_FIELD, updatedVocabulary.getTitle(),
                    Analytics.SLUG_FIELD, updatedVocabulary.getSlug(),
                    Analytics.ENTITY_STATUS_FIELD,
                        updatedVocabulary.getStatus(),
                    Analytics.OWNER_FIELD, updatedVocabulary.getOwner());
            return Response.status(Response.Status.BAD_REQUEST).
                    entity(errorResult).build();
        }

        EntityManager em = null;
        EntityTransaction txn = null;

        try {
            em = DBContext.getEntityManager();
            txn = em.getTransaction();
            txn.begin();

            Integer updatedVocabularyId = updatedVocabulary.getId();

            VocabularyModel vm = ModelMethods.createVocabularyModel(em,
                    updatedVocabularyId);
            LocalDateTime now = TemporalUtils.nowUTC();
            ModelMethods.applyChanges(vm, profile.getUsername(),
                    now, updatedVocabulary);

            // And now, commit all of the above changes; the db is
            // read-only from here.
            txn.commit();
            // If we have reached this point, we have success.
            // Analytics logging.
            Logging.logRequest(true, request, uriInfo, profile,
                    Analytics.EVENT_UPDATE_VOCABULARY,
                    Analytics.ID_FIELD, vocabularyId,
                    Analytics.TITLE_FIELD, updatedVocabulary.getTitle(),
                    Analytics.SLUG_FIELD, updatedVocabulary.getSlug(),
                    Analytics.ENTITY_STATUS_FIELD,
                        updatedVocabulary.getStatus(),
                    Analytics.OWNER_FIELD, updatedVocabulary.getOwner());
            Vocabulary updatedVocabularyResponse;
            if (updatedVocabulary.getStatus() == VocabularyStatus.DRAFT) {
                updatedVocabularyResponse =
                        ModelMethods.getDraft(vm, true, true, true);
            } else {
                updatedVocabularyResponse =
                        ModelMethods.getCurrent(vm, true, true, true);
                // Solr indexing.
                EntityIndexer.indexVocabulary(updatedVocabularyId);
            }
            return Response.created(EntityPaths.getURIOfEntity(
                    updatedVocabularyResponse)).
                    entity(updatedVocabularyResponse).build();
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
                // The vocabulary has been updated, but it couldn't be
                // indexed. Oops.
                logger.error("Solr indexing failed");
                // We fall through to returning an internal server error.
            }
            if (t instanceof IllegalArgumentException) {
                // There was a validation error.
                Logging.logRequest(false, request, uriInfo, profile,
                        Analytics.EVENT_UPDATE_VOCABULARY,
                        Analytics.FAILURE_REASON, "validation",
                        Analytics.ID_FIELD, vocabularyId,
                        Analytics.TITLE_FIELD, updatedVocabulary.getTitle(),
                        Analytics.SLUG_FIELD, updatedVocabulary.getSlug(),
                        Analytics.ENTITY_STATUS_FIELD,
                            updatedVocabulary.getStatus(),
                        Analytics.OWNER_FIELD, updatedVocabulary.getOwner());
                ErrorResult errorResult =
                        new ErrorResult("Validation error: " + t.getMessage());
                return Response.status(Response.Status.BAD_REQUEST).
                        entity(errorResult).build();
            }
            // Don't throw, but fall through so that the user sees
            // an error message.
        } finally {
            if (em != null) {
                em.close();
            }
        }

        // Oh dear, we fell through. There was an exception we don't
        // know about, or, at least, can't provide useful feeback about.
        Logging.logRequest(false, request, uriInfo, profile,
                Analytics.EVENT_UPDATE_VOCABULARY,
                Analytics.FAILURE_REASON, "internal error",
                Analytics.ID_FIELD, vocabularyId,
                Analytics.TITLE_FIELD, updatedVocabulary.getTitle(),
                Analytics.SLUG_FIELD, updatedVocabulary.getSlug(),
                Analytics.ENTITY_STATUS_FIELD,
                    updatedVocabulary.getStatus(),
                Analytics.OWNER_FIELD, updatedVocabulary.getOwner());
        return ErrorResultUtils.internalServerError();
    }

}
