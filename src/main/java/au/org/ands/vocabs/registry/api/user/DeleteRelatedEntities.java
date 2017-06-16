/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.http.HttpStatus;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.jax.rs.annotations.Pac4JProfile;
import org.pac4j.jax.rs.annotations.Pac4JSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.auth.AuthUtils;
import au.org.ands.vocabs.registry.api.context.ApiPaths;
import au.org.ands.vocabs.registry.api.context.ResponseUtils;
import au.org.ands.vocabs.registry.api.context.SwaggerInterface;
import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.dao.RegistryEventDAO;
import au.org.ands.vocabs.registry.db.dao.RelatedEntityDAO;
import au.org.ands.vocabs.registry.db.dao.RelatedEntityIdentifierDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyRelatedEntityDAO;
import au.org.ands.vocabs.registry.db.entity.RegistryEvent;
import au.org.ands.vocabs.registry.enums.RegistryEventElementType;
import au.org.ands.vocabs.registry.enums.RegistryEventEventType;
import au.org.ands.vocabs.registry.utils.Analytics;
import au.org.ands.vocabs.registry.utils.Logging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.ResponseHeader;

/** REST web services for deleting related entities. */
@Path(ApiPaths.API_RESOURCE + "/" + ApiPaths.RELATED_ENTITIES)
@Api(value = SwaggerInterface.TAG_RESOURCES,
        authorizations = {@Authorization(value = SwaggerInterface.BASIC_AUTH),
        @Authorization(value = SwaggerInterface.API_KEY_AUTH)})
public class DeleteRelatedEntities {

    // For my info: if adding a method with return type Response
    // where the value returned is a list, wrap it in a GenericEntity.
    // See http://www.adam-bien.com/roller/abien/entry/jax_rs_returning_a_list

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Delete a related entity.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile.
     * @param relatedEntityId The ID of the related entity to be deleted.
     * @return The response indicating success or failure,
     *      in either XML or JSON format. */
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Pac4JSecurity
    @Path(ApiPaths.RELATED_ENTITY_ID)
    @DELETE
    // Seems to be a defect in Swagger Annotations, that we can't
    // specify code = HttpStatus.SC_NO_CONTENT here and omit the response type,
    // but the code has to be specified as an @ApiResponse.
    @ApiOperation(value = "Delete a related entity.")
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_NO_CONTENT,
                    message = "Delete successful"),
            @ApiResponse(code = HttpStatus.SC_NOT_FOUND,
                    message = "No such related entity",
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
            @ApiResponse(code = HttpStatus.SC_CONFLICT,
                    message = "Can't delete related entity, as it is in use. "
                            + "See response constraintViolations for "
                            + "vocabulary IDs of vocabularies that use it.",
                    response = ErrorResult.class)
            })
    public final Response deleteRelatedEntity(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(hidden = true) @Pac4JProfile final CommonProfile profile,
            @ApiParam(value = "The ID of the related entity to be deleted")
            @PathParam("relatedEntityId") final Integer relatedEntityId) {
        logger.debug("called deleteRelatedEntity");

        au.org.ands.vocabs.registry.db.entity.RelatedEntity relatedEntity =
                RelatedEntityDAO.getCurrentRelatedEntityByRelatedEntityId(
                        relatedEntityId);

        if (relatedEntity == null) {
            // Possible future work: distinguish between:
            // (a) never existed: HTTP status code 404
            // (b) there is historical data: HTTP status code 410
            return Response.status(Status.NOT_FOUND).entity(
                    new ErrorResult("No related entity with that id")).build();
        }

        if (!AuthUtils.ownerIsAuthorizedByOrganisationOrUsername(profile,
                relatedEntity.getOwner())) {
            return ResponseUtils.generateForbiddenResponseForOwner();
        }

        // Check for uses by current vocabularies.

        List<ValidationError> validationErrors = new ArrayList<>();
        // Check REs with matching top-level properties.
        for (Integer vocabularyId
                : VocabularyRelatedEntityDAO.getVocabularyIdsWithRelatedEntity(
                        relatedEntityId)) {
            ValidationError ve = new ValidationError();
            ve.setMessage("Related entity in use; path "
                    + "has the ID of the vocabulary");
            ve.setPath(vocabularyId.toString());
            validationErrors.add(ve);
        }
        if (!validationErrors.isEmpty()) {
            logger.info("Attempt to delete a related entity that is "
                    + "still in use");
            // TODO analytics logging
            ErrorResult errorResult =
                    new ErrorResult("Won't delete related entity");
            errorResult.setConstraintViolation(validationErrors);
            return Response.status(Status.CONFLICT).entity(errorResult).build();
        }

        LocalDateTime now = TemporalUtils.nowUTC();

        // Delete the identifiers first.
        for (au.org.ands.vocabs.registry.db.entity.RelatedEntityIdentifier rei
                :  RelatedEntityIdentifierDAO.
                    getCurrentRelatedEntityIdentifierListForRelatedEntity(
                            relatedEntityId)) {
            rei.setEndDate(now);
            // Persist the deleted related entity identifier.
            RelatedEntityIdentifierDAO.updateRelatedEntityIdentifier(rei);
        }

        relatedEntity.setEndDate(now);
        // Persist the deleted related entity.
        RelatedEntityDAO.updateRelatedEntity(relatedEntity);

        // This is a registry event; log it.
        RegistryEvent re = new RegistryEvent();
        re.setElementType(RegistryEventElementType.RELATED_ENTITIES);
        re.setElementId(relatedEntityId);
        re.setEventDate(now);
        re.setEventType(RegistryEventEventType.DELETED);
        re.setEventUser(profile.getUsername());
        // TODO: put something sensible in the details.
        re.setEventDetails("");
        RegistryEventDAO.saveRegistryEvent(re);

        // Analytics logging.
        Logging.logRequest(request, uriInfo, profile,
                Analytics.EVENT_DELETE_RELATED_ENTITY,
                Analytics.RELATED_ENTITY_ID_FIELD,
                    relatedEntityId,
                Analytics.TITLE_FIELD, relatedEntity.getTitle(),
                Analytics.OWNER_FIELD, relatedEntity.getOwner());

        // Successful deletion, and no response body. noContent() creates
        // status code 204.
        return Response.noContent().build();
    }

}
