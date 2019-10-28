/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
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
import au.org.ands.vocabs.registry.db.converter.VocabularyDbRelatedVocabularySchemaMapper;
import au.org.ands.vocabs.registry.db.dao.RelatedEntityDAO;
import au.org.ands.vocabs.registry.db.dao.RelatedEntityIdentifierDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.enums.RelatedEntityRelation;
import au.org.ands.vocabs.registry.enums.RelatedEntityType;
import au.org.ands.vocabs.registry.log.Logging;
import au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntity;
import au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntityIdentifier;
import au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntityList;
import au.org.ands.vocabs.registry.schema.vocabulary201701.ReverseRelatedVocabulary;
import au.org.ands.vocabs.registry.schema.vocabulary201701.ReverseRelatedVocabularyList;
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
    public Response getRelatedEntityById(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(value = "The ID of the related entity to get.")
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

        Logging.logRequest(true, request, uriInfo, null,
                "Get a current related entity by its ID");
        return Response.ok().entity(outputRE).build();
    }

    /** Get all current related entities, optionally, filtering by type.
     * Results do not include related entity identifiers.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param relatedEntityType The RelatedEntityType used to filter the
     *      list of related entities to be fetched.
     * @return The list of related entities, in either XML or JSON format. */
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @GET
    @ApiOperation(value = "Get all current related entities, optionally, "
            + "filtered by related entity type. Results do not include "
            + "related entity identifiers.")
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_BAD_REQUEST,
                    message = "No related entity with that id",
                    response = ErrorResult.class)
            })
    public RelatedEntityList getRelatedEntities(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(value = "The type of the related entities to get.")
            @QueryParam("relatedEntityType") final RelatedEntityType
                relatedEntityType) {
        logger.debug("called getRelatedEntities: relatedEntityType = "
            + relatedEntityType);

        List<au.org.ands.vocabs.registry.db.entity.RelatedEntity>
            dbREs;
        if (relatedEntityType == null) {
            dbREs = RelatedEntityDAO.getAllCurrentRelatedEntity();
        } else {
            dbREs = RelatedEntityDAO.getAllCurrentRelatedEntityByType(
                    relatedEntityType);
        }
        RelatedEntityList outputREList = new RelatedEntityList();
        List<RelatedEntity> outputREs =
                outputREList.getRelatedEntity();

        RelatedEntityDbSchemaMapper mapper =
                RelatedEntityDbSchemaMapper.INSTANCE;

        for (au.org.ands.vocabs.registry.db.entity.RelatedEntity dbRE
                : dbREs) {
            outputREs.add(mapper.sourceToTarget(dbRE));
        }

        Logging.logRequest(true, request, uriInfo, null,
                "Get all current related entities; type = "
                        + relatedEntityType);
        return outputREList;
    }

    /** Get all vocabularies that are related to a related entity.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param relatedEntityId The RelatedEntityId of the related entity
     *      for which the vocabularies related to it are to be fetched.
     * @return The related entity, in either XML or JSON format,
     *      or an error result, if there is no such related entity. */
    @Path(ApiPaths.RELATED_ENTITY_ID + "/"
            + ApiPaths.REVERSE_RELATED_VOCABULARIES)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @GET
    @ApiOperation(value = "Get all current vocabularies related to a "
            + "related entity, by the related entity's id.")
    public ReverseRelatedVocabularyList
    getVocabulariesRelatedToRelatedEntityById(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(value = "The ID of the related entity for which "
                    + "to get the vocabularies related to it.")
            @PathParam("relatedEntityId") final Integer relatedEntityId) {
        logger.debug("called getVocabulariesRelatedToRelatedEntityById: "
            + relatedEntityId);

        MultivaluedMap<au.org.ands.vocabs.registry.db.entity.Vocabulary,
            RelatedEntityRelation> dbRVs =
            VocabularyDAO.getCurrentVocabulariesForRelatedEntity(
                    relatedEntityId);

        ReverseRelatedVocabularyList outputRVList =
                new ReverseRelatedVocabularyList();
        List<ReverseRelatedVocabulary> outputRVs =
                outputRVList.getReverseRelatedVocabulary();

        VocabularyDbRelatedVocabularySchemaMapper mapper =
                VocabularyDbRelatedVocabularySchemaMapper.INSTANCE;

        for (Map.Entry<au.org.ands.vocabs.registry.db.entity.Vocabulary,
                List<RelatedEntityRelation>> mapElement : dbRVs.entrySet()) {
            ReverseRelatedVocabulary revRV = new ReverseRelatedVocabulary();
            revRV.setRelatedVocabulary(mapper.sourceToTarget(
                    mapElement.getKey()));
            List<RelatedEntityRelation> rvRelationList =
                    revRV.getRelatedEntityRelation();
            rvRelationList.addAll(mapElement.getValue());
            outputRVs.add(revRV);
        }

        Logging.logRequest(true, request, uriInfo, null,
                "Get current vocabularies related to a related entity "
                + "by its ID");
        return outputRVList;
    }

}
