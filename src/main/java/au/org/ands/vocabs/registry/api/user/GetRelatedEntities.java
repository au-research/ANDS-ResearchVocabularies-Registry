/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user;

import java.lang.invoke.MethodHandles;
import java.util.List;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.context.ApiPaths;
import au.org.ands.vocabs.registry.api.context.SwaggerInterface;
import au.org.ands.vocabs.registry.db.converter.RelatedEntityDbSchemaMapper;
import au.org.ands.vocabs.registry.db.converter.RelatedEntityIdentifierDbSchemaMapper;
import au.org.ands.vocabs.registry.db.dao.RelatedEntityDAO;
import au.org.ands.vocabs.registry.db.dao.RelatedEntityIdentifierDAO;
import au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntity;
import au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntityIdentifier;
import au.org.ands.vocabs.registry.utils.Logging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/** REST web services for getting related entities. */
@Path(ApiPaths.API_RESOURCE + "/" + ApiPaths.RELATED_ENTITIES)
@Api(value = SwaggerInterface.TAG_RESOURCES)
public class GetRelatedEntities {

    // For my info: if adding a method with return type Response
    // where the value returned is a list, wrap it in a GenericEntity.
    // See http://www.adam-bien.com/roller/abien/entry/jax_rs_returning_a_list

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Get a current related entity, by its related entity id.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param relatedEntityId The RelatedEntityId of the related entity
     *      to be fetched.
     * @return The related entity, in either XML or JSON format,
     *      or an error result, if there is no such related entity. */
    @Path(ApiPaths.RELATED_ENTITY_ID)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @GET
    @ApiOperation(value = "Get a current related entity by its id.",
            response = RelatedEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_BAD_REQUEST,
                    message = "No related entity with that id",
                    response = ErrorResult.class)
            })
    public final Response getRelatedEntityById(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(value = "The ID of the related entity to get ")
            @PathParam("relatedEntityId") final Integer relatedEntityId) {
        logger.debug("called getRelatedEntityById: "
            + relatedEntityId);

        au.org.ands.vocabs.registry.db.entity.RelatedEntity
        dbRE = RelatedEntityDAO.getCurrentRelatedEntityByRelatedEntityId(
                relatedEntityId);
        RelatedEntity outputRE;

        RelatedEntityDbSchemaMapper reMapper =
                RelatedEntityDbSchemaMapper.INSTANCE;
        RelatedEntityIdentifierDbSchemaMapper reiMapper =
                RelatedEntityIdentifierDbSchemaMapper.INSTANCE;

        outputRE = reMapper.sourceToTarget(dbRE);
        if (outputRE == null) {
            return Response.status(Status.BAD_REQUEST).entity(
                new ErrorResult("No related entity with that id")).build();
        }

        List<RelatedEntityIdentifier> outputRelatedEntityIdentifiers =
                outputRE.getRelatedEntityIdentifier();
        List<au.org.ands.vocabs.registry.db.entity.RelatedEntityIdentifier>
            dbRelatedEntityIdentifiers =
                RelatedEntityIdentifierDAO.
                getCurrentRelatedEntityIdentifierListForRelatedEntity(
                        dbRE.getRelatedEntityId());
        for (au.org.ands.vocabs.registry.db.entity.RelatedEntityIdentifier
                dbREI : dbRelatedEntityIdentifiers) {
            outputRelatedEntityIdentifiers.add(
                    reiMapper.sourceToTarget(dbREI));
        }

        Logging.logRequest(request, uriInfo, null, "Get a vocabulary");
        return Response.ok().entity(outputRE).build();
    }

}
