/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import javax.ws.rs.core.MultivaluedMap;
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
import au.org.ands.vocabs.registry.api.context.EntityPaths;
import au.org.ands.vocabs.registry.api.context.ResponseUtils;
import au.org.ands.vocabs.registry.api.context.SwaggerInterface;
import au.org.ands.vocabs.registry.api.converter.RelatedEntityIdentifierRegistrySchemaMapper;
import au.org.ands.vocabs.registry.api.converter.RelatedEntityRegistrySchemaMapper;
import au.org.ands.vocabs.registry.api.validation.CheckRelatedEntity;
import au.org.ands.vocabs.registry.api.validation.ValidationMode;
import au.org.ands.vocabs.registry.db.context.DBContext;
import au.org.ands.vocabs.registry.db.context.TemporalConstants;
import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.converter.RelatedEntityDbSchemaMapper;
import au.org.ands.vocabs.registry.db.converter.RelatedEntityIdentifierDbSchemaMapper;
import au.org.ands.vocabs.registry.db.dao.RegistryEventDAO;
import au.org.ands.vocabs.registry.db.dao.RelatedEntityDAO;
import au.org.ands.vocabs.registry.db.dao.RelatedEntityIdentifierDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.db.entity.ComparisonUtils;
import au.org.ands.vocabs.registry.db.entity.RegistryEvent;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.enums.RegistryEventElementType;
import au.org.ands.vocabs.registry.enums.RegistryEventEventType;
import au.org.ands.vocabs.registry.enums.RelatedEntityRelation;
import au.org.ands.vocabs.registry.log.Analytics;
import au.org.ands.vocabs.registry.log.Logging;
import au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntity;
import au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntityIdentifier;
import au.org.ands.vocabs.registry.solr.EntityIndexer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.ResponseHeader;

/** REST web services for creating and modifying related entities. */
@Path(ApiPaths.API_RESOURCE + "/" + ApiPaths.RELATED_ENTITIES)
@Api(value = SwaggerInterface.TAG_RESOURCES,
        authorizations = {@Authorization(value = SwaggerInterface.BASIC_AUTH),
        @Authorization(value = SwaggerInterface.API_KEY_AUTH)})
public class PutRelatedEntities {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Create a new related entity.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile.
     * @param newRelatedEntity The related entity to be created.
     * @return The new related entity, in either XML or JSON format. */
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Pac4JSecurity
    @POST
    @ApiOperation(value = "Create a new related entity.",
            code = HttpStatus.SC_CREATED,
            responseHeaders = {
                    @ResponseHeader(name = "Location",
                            description = "URL of the newly-created "
                                    + "related entity",
                            response = URL.class)
            },
            response = RelatedEntity.class)
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
            @ApiResponse(code = HttpStatus.SC_CONFLICT,
                    message = "Duplicate related entity. See response "
                            + "constraintViolations for conflicting entities.",
                    response = ErrorResult.class)
            })
    public Response createRelatedEntity(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(hidden = true) @Pac4JProfile final CommonProfile profile,
            @ApiParam(value = "The related entity to be added.",
                required = true)
            @NotNull(message = "The related entity must not be null")
            @CheckRelatedEntity final RelatedEntity newRelatedEntity) {
        logger.debug("called createRelatedEntity");

        if (!AuthUtils.ownerIsAuthorizedByOrganisationOrUsername(profile,
                newRelatedEntity.getOwner())) {
            Logging.logRequest(false, request, uriInfo, profile,
                    Analytics.EVENT_CREATE_RELATED_ENTITY,
                    Analytics.FAILURE_REASON,
                        "authorization",
                    Analytics.TITLE_FIELD,
                        newRelatedEntity.getTitle(),
                    Analytics.OWNER_FIELD,
                        newRelatedEntity.getOwner());
            return ResponseUtils.generateForbiddenResponseForOwner();
        }
        String username = profile.getUsername();

        // All of the mappers we will need.
        RelatedEntityRegistrySchemaMapper reToDbMapper =
                RelatedEntityRegistrySchemaMapper.INSTANCE;
        RelatedEntityIdentifierRegistrySchemaMapper reiToDbMapper =
                RelatedEntityIdentifierRegistrySchemaMapper.INSTANCE;
        RelatedEntityDbSchemaMapper reFromDbMapper =
                RelatedEntityDbSchemaMapper.INSTANCE;
        RelatedEntityIdentifierDbSchemaMapper reiFromDbMapper =
                RelatedEntityIdentifierDbSchemaMapper.INSTANCE;

        au.org.ands.vocabs.registry.db.entity.RelatedEntity
            newDbRelatedEntity = reToDbMapper.sourceToTarget(newRelatedEntity);

        // Check for duplicates.
        // NB: we don't need to check for duplicate identifiers _within_ the
        // request body; that is handled by @CheckRelatedEntity.

        List<ValidationError> validationErrors = new ArrayList<>();
        // Check REs with matching top-level properties.
        for (au.org.ands.vocabs.registry.db.entity.RelatedEntity re
                : RelatedEntityDAO.getMatchingRelatedEntities(
                        newDbRelatedEntity)) {
            ValidationError ve = new ValidationError();
            ve.setMessage("Related entity with matching details; path "
                    + "has the ID of the related entity");
            ve.setPath(re.getRelatedEntityId().toString());
            validationErrors.add(ve);
        }
        // Check for existing related entities with a matching identifier.
        for (RelatedEntityIdentifier rei
                : newRelatedEntity.getRelatedEntityIdentifier()) {
            for (au.org.ands.vocabs.registry.db.entity.RelatedEntity re
                    : RelatedEntityDAO.
                        getRelatedEntitiesWithMatchingIdentifier(
                                newRelatedEntity.getOwner(),
                                rei.getIdentifierType(),
                                rei.getIdentifierValue())) {
                ValidationError ve = new ValidationError();
                ve.setMessage("Related entity with matching identifier; path "
                        + "has the ID of the related entity");
                ve.setPath(re.getRelatedEntityId().toString());
                validationErrors.add(ve);
            }
        }
        if (!validationErrors.isEmpty()) {
            logger.info("Attempt to create a new related entity or identifier "
                    + "that would be a duplicate of an existing one");
            Logging.logRequest(false, request, uriInfo, profile,
                    Analytics.EVENT_CREATE_RELATED_ENTITY,
                    Analytics.FAILURE_REASON,
                        "duplicate",
                    Analytics.TITLE_FIELD,
                        newRelatedEntity.getTitle(),
                    Analytics.OWNER_FIELD,
                        newRelatedEntity.getOwner());
            ErrorResult errorResult =
                    new ErrorResult("Duplicate related entity or identifier");
            errorResult.setConstraintViolation(validationErrors);
            return Response.status(Status.CONFLICT).entity(errorResult).build();
        }

        // No notion of drafts for related entities:
        // always use current date/time as start date.
        LocalDateTime now = TemporalUtils.nowUTC();
        newDbRelatedEntity.setStartDate(now);
        newDbRelatedEntity.setEndDate(
                TemporalConstants.CURRENTLY_VALID_END_DATE);
        newDbRelatedEntity.setModifiedBy(username);
        // Persist the related entity.
        RelatedEntityDAO.saveRelatedEntityWithId(newDbRelatedEntity);

        // Now translate back into registry schema.
        RelatedEntity reReturned = reFromDbMapper.sourceToTarget(
                newDbRelatedEntity);
        List<RelatedEntityIdentifier> reiReturned =
                reReturned.getRelatedEntityIdentifier();

        // Extract and persist the identifiers.
        for (RelatedEntityIdentifier rei
                : newRelatedEntity.getRelatedEntityIdentifier()) {
            au.org.ands.vocabs.registry.db.entity.RelatedEntityIdentifier
                newDbREI = reiToDbMapper.sourceToTarget(rei);
            // Must manually point it back to the parent entity.
            newDbREI.setRelatedEntityId(
                    newDbRelatedEntity.getRelatedEntityId());
            newDbREI.setStartDate(now);
            newDbREI.setEndDate(TemporalConstants.CURRENTLY_VALID_END_DATE);
            newDbREI.setModifiedBy(username);
            // Persist the identifier.
            RelatedEntityIdentifierDAO.saveRelatedEntityIdentifierWithId(
                    newDbREI);
            reiReturned.add(reiFromDbMapper.sourceToTarget(newDbREI));
        }

        // This is a registry event; log it.
        RegistryEvent re = new RegistryEvent();
        re.setElementType(RegistryEventElementType.RELATED_ENTITIES);
        re.setElementId(newDbRelatedEntity.getRelatedEntityId());
        // NB: newDbVocab.getStartDate() would not be correct for a draft!
        re.setEventDate(now);
        re.setEventType(RegistryEventEventType.CREATED);
        re.setEventUser(profile.getUsername());
        // To be done: put something sensible in the details.
        re.setEventDetails("");
        RegistryEventDAO.saveRegistryEvent(re);

        // Analytics logging.
        Logging.logRequest(true, request, uriInfo, profile,
                Analytics.EVENT_CREATE_RELATED_ENTITY,
                Analytics.ID_FIELD, newDbRelatedEntity.getRelatedEntityId(),
                Analytics.TITLE_FIELD, newDbRelatedEntity.getTitle(),
                Analytics.OWNER_FIELD, newDbRelatedEntity.getOwner());

        return Response.created(EntityPaths.getURIOfEntity(newDbRelatedEntity)).
                entity(reReturned).build();
    }

    /** Update a related entity.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile.
     * @param relatedEntityId The ID of the related entity to be updated.
     * @param updatedRelatedEntity The new value of the related entity.
     * @return The updated related entity, in either XML or JSON format. */
    @SuppressWarnings("checkstyle:MethodLength")
    @Path(ApiPaths.RELATED_ENTITY_ID)
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Pac4JSecurity
    @PUT
    @ApiOperation(value = "Update a related entity.",
            response = RelatedEntity.class)
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
            @ApiResponse(code = HttpStatus.SC_CONFLICT,
                    message = "Duplicate related entity. See response "
                            + "constraintViolations for conflicting entities.",
                    response = ErrorResult.class),
            @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR,
                    message = "Internal server error",
                    response = ErrorResult.class)
            })
    public Response updateRelatedEntity(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(hidden = true) @Pac4JProfile final CommonProfile profile,
            @ApiParam(value = "The ID of the related entity to be updated.")
            @PathParam("relatedEntityId") final Integer relatedEntityId,
            @ApiParam(value = "The new value of the related entity.",
                required = true)
            @NotNull(message = "The related entity must not be null.")
            @CheckRelatedEntity(mode = ValidationMode.UPDATE)
            final RelatedEntity updatedRelatedEntity) {
        logger.debug("called updateRelatedEntity");

        // Keep track of whether we actually do any database changes.
        // We only add a registry event if we changed something.
        boolean reWasUpdated = false;

        // We manage our own transaction. Because we do merges, it is
        // at least polite to include the initial queries within the same
        // transaction.
        // TAKE NOTE: All DAO methods used below are the variants that take an
        // EntityManager as the first parameter.
        // (Historical note: I moved the beginning of the
        // transaction up front, and introduced the variant DAO methods that
        // take an EntityManager parameter, in order to try to fix an exception
        // that was occurring during database updates. I subsequently realised
        // that the exception's underlying cause was completely unrelated:
        // there was an error in RelatedEntityIdentifierRegistrySchemaMapper
        // to do with the mapping of ids. Having done the work, I decided
        // to leave it.)
        EntityManager em = null;
        EntityTransaction txn = null;

        try {
            em = DBContext.getEntityManager();
            txn = em.getTransaction();
            txn.begin();

            au.org.ands.vocabs.registry.db.entity.RelatedEntity
            existingDbRelatedEntity =
                RelatedEntityDAO.getCurrentRelatedEntityByRelatedEntityId(
                        em, relatedEntityId);

            if (existingDbRelatedEntity == null) {
                // Possible future work: distinguish between:
                // (a) never existed: HTTP status code 404
                // (b) there is historical data: HTTP status code 410
                Logging.logRequest(false, request, uriInfo, profile,
                        Analytics.EVENT_UPDATE_RELATED_ENTITY,
                        Analytics.FAILURE_REASON,
                            "No related entity with that id",
                        Analytics.ID_FIELD, relatedEntityId,
                        Analytics.TITLE_FIELD,
                            updatedRelatedEntity.getTitle(),
                        Analytics.OWNER_FIELD,
                            updatedRelatedEntity.getOwner());
                return Response.status(Status.NOT_FOUND).entity(
                        new ErrorResult("No related entity with that id")).
                        build();
            }

            if (!AuthUtils.ownerIsAuthorizedByOrganisationOrUsername(profile,
                    existingDbRelatedEntity.getOwner())) {
                Logging.logRequest(false, request, uriInfo, profile,
                        Analytics.EVENT_UPDATE_RELATED_ENTITY,
                        Analytics.FAILURE_REASON,
                            "authorization",
                        Analytics.ID_FIELD, relatedEntityId,
                        Analytics.TITLE_FIELD,
                            updatedRelatedEntity.getTitle(),
                        Analytics.OWNER_FIELD,
                            updatedRelatedEntity.getOwner());
                return ResponseUtils.generateForbiddenResponseForOwner();
            }

            List<ValidationError> validationErrors = new ArrayList<>();

            // Check that the ID has not been altered.
            if (updatedRelatedEntity.getId().intValue() != relatedEntityId) {
                ValidationError ve = new ValidationError();
                ve.setMessage("Related entity ID in request body does not "
                        + "match the value of the path parameter");
                ve.setPath("id");
                validationErrors.add(ve);
            }

            if (!existingDbRelatedEntity.getOwner().equals(
                    updatedRelatedEntity.getOwner())) {
                // Owner has changed. Need to check that the user still has
                // permission to be the owner.
                if (!AuthUtils.ownerIsAuthorizedByOrganisationOrUsername(
                        profile, updatedRelatedEntity.getOwner())) {
                    Logging.logRequest(false, request, uriInfo, profile,
                            Analytics.EVENT_UPDATE_RELATED_ENTITY,
                            Analytics.FAILURE_REASON,
                                "authorization",
                            Analytics.ID_FIELD, relatedEntityId,
                            Analytics.TITLE_FIELD,
                                updatedRelatedEntity.getTitle(),
                            Analytics.OWNER_FIELD,
                                updatedRelatedEntity.getOwner());
                    return ResponseUtils.generateForbiddenResponseForOwner();
                }
            }

            String username = profile.getUsername();

            // All of the mappers we will need.
            RelatedEntityRegistrySchemaMapper reToDbMapper =
                    RelatedEntityRegistrySchemaMapper.INSTANCE;
            RelatedEntityIdentifierRegistrySchemaMapper reiToDbMapper =
                    RelatedEntityIdentifierRegistrySchemaMapper.INSTANCE;
            RelatedEntityDbSchemaMapper reFromDbMapper =
                    RelatedEntityDbSchemaMapper.INSTANCE;
            RelatedEntityIdentifierDbSchemaMapper reiFromDbMapper =
                    RelatedEntityIdentifierDbSchemaMapper.INSTANCE;

            au.org.ands.vocabs.registry.db.entity.RelatedEntity
                updatedDbRelatedEntity = reToDbMapper.sourceToTarget(
                        updatedRelatedEntity);

            // Did the top-level data stay the same?
            boolean sameTopLevel = ComparisonUtils.isEqualRelatedEntity(
                    existingDbRelatedEntity, updatedDbRelatedEntity);

            // Check for duplicates.
            // NB: we don't need to check for duplicate identifiers _within_ the
            // request body; that is handled by @CheckRelatedEntity.

            // Check REs with matching top-level properties.
            for (au.org.ands.vocabs.registry.db.entity.RelatedEntity re
                    : RelatedEntityDAO.getMatchingRelatedEntities(
                            em, updatedDbRelatedEntity)) {
                // The following conditional is a difference from
                // createRelatedEntity(): since this RE is already in the
                // database, it has to be eliminated from the matching process!
                if (!re.getRelatedEntityId().equals(relatedEntityId)) {
                    ValidationError ve = new ValidationError();
                    ve.setMessage("Related entity with matching details; path "
                            + "has the ID of the related entity");
                    ve.setPath(re.getRelatedEntityId().toString());
                    validationErrors.add(ve);
                }
            }
            // Check for existing related entities with a matching identifier.
            for (RelatedEntityIdentifier rei
                    : updatedRelatedEntity.getRelatedEntityIdentifier()) {
                for (au.org.ands.vocabs.registry.db.entity.RelatedEntity re
                        : RelatedEntityDAO.
                        getRelatedEntitiesWithMatchingIdentifier(em,
                                    updatedRelatedEntity.getOwner(),
                                    rei.getIdentifierType(),
                                    rei.getIdentifierValue())) {
                    // The following conditional is a difference from
                    // createRelatedEntity(): since this RE is already in the
                    // database, it has to be eliminated from the matching
                    // process!
                    if (!re.getRelatedEntityId().equals(relatedEntityId)) {
                        ValidationError ve = new ValidationError();
                        ve.setMessage("Related entity with matching "
                                + "identifier; path has the ID of the "
                                + "related entity");
                        ve.setPath(re.getRelatedEntityId().toString());
                        validationErrors.add(ve);
                    }
                }
            }

            // Analyse the identifiers.
            // First, get the existing identifiers and put them into a Map.
            List<au.org.ands.vocabs.registry.db.entity.RelatedEntityIdentifier>
            existingDbREIs = RelatedEntityIdentifierDAO.
            getCurrentRelatedEntityIdentifierListForRelatedEntity(em,
                    relatedEntityId);
            Map<Integer, au.org.ands.vocabs.registry.db.entity.
            RelatedEntityIdentifier> existingDbREIMap = new HashMap<>();
            // deletedIdentifiers will store the IDs of the identifiers
            // that the client is requesting to be deleted. It is a set that
            // is initialized with the IDs of the existing identifiers.
            // Then, IDs will be _removed_ from the set as we subsequently
            // process the list of identifiers included in the request body.
            Set<Integer> deletedIdentifiers = new HashSet<>();
            for (au.org.ands.vocabs.registry.db.entity.
                    RelatedEntityIdentifier rei : existingDbREIs) {
                existingDbREIMap.put(rei.getRelatedEntityIdentifierId(), rei);
                deletedIdentifiers.add(rei.getRelatedEntityIdentifierId());
            }
            // Now, go through the identifiers in the request body and work out
            // which ones are being added, updated, and deleted, and which
            // are unmodified.
            // See also the definition and priming of deletedIdentifiers
            // just above.
            List<RelatedEntityIdentifier> newIdentifiers = new ArrayList<>();
            List<au.org.ands.vocabs.registry.db.entity.RelatedEntityIdentifier>
                updatedIdentifiers = new ArrayList<>();
            List<RelatedEntityIdentifier> unmodifiedIdentifiers =
                    new ArrayList<>();

            for (RelatedEntityIdentifier rei
                    : updatedRelatedEntity.getRelatedEntityIdentifier()) {
                Integer reiID = rei.getId();
                // Is it new, or does it have an ID we know about?
                if (reiID == null) {
                    // New.
                    newIdentifiers.add(rei);
                } else {
                    if (!existingDbREIMap.containsKey(reiID)) {
                        ValidationError ve = new ValidationError();
                        ve.setMessage("Related entity identifier ID "
                                + "specified, but does not correspond with "
                                + "an identifier of the related entity; path "
                                + "has the ID of the related entity "
                                + "identifier");
                        ve.setPath(reiID.toString());
                        validationErrors.add(ve);
                        continue;
                    }
                    // We do know about this one, and we know that it is
                    // not being deleted.
                    deletedIdentifiers.remove(reiID);
                    // Now check if there has been a modification.
                    au.org.ands.vocabs.registry.db.entity.
                    RelatedEntityIdentifier reiDb =
                        reiToDbMapper.sourceToTarget(rei);

                    if (ComparisonUtils.isEqualRelatedEntityIdentifier(
                            existingDbREIMap.get(reiID), reiDb)) {
                        unmodifiedIdentifiers.add(rei);
                    } else {
                        updatedIdentifiers.add(reiDb);
                    }
                }
            }

            if (!validationErrors.isEmpty()) {
                Logging.logRequest(false, request, uriInfo, profile,
                        Analytics.EVENT_UPDATE_RELATED_ENTITY,
                        Analytics.FAILURE_REASON, "validation",
                        Analytics.ID_FIELD, relatedEntityId,
                        Analytics.TITLE_FIELD,
                            updatedDbRelatedEntity.getTitle(),
                        Analytics.OWNER_FIELD,
                            updatedDbRelatedEntity.getOwner());
                ErrorResult errorResult =
                        new ErrorResult("Because of validation errors, won't "
                                + "update related entity");
                errorResult.setConstraintViolation(validationErrors);
                return Response.status(Status.CONFLICT).entity(errorResult).
                        build();
            }

            // Timestamp to use for start/end date values corresponding
            // to this event.
            LocalDateTime now = TemporalUtils.nowUTC();

            // reReturned (and reiReturned, defined below) are used to represent
            // the value that will be returned to the caller.
            RelatedEntity reReturned;

            // Persist the RelatedEntity, if it has changed.
            // No notion of drafts for related entities:
            // always use current date/time as start date.
            if (!sameTopLevel) {
                existingDbRelatedEntity.setEndDate(now);
                RelatedEntityDAO.updateRelatedEntity(em,
                        existingDbRelatedEntity);
                updatedDbRelatedEntity.setStartDate(now);
                updatedDbRelatedEntity.setEndDate(
                        TemporalConstants.CURRENTLY_VALID_END_DATE);
                updatedDbRelatedEntity.setModifiedBy(username);
                // Persist the related entity.
                RelatedEntityDAO.saveRelatedEntity(em, updatedDbRelatedEntity);
                reWasUpdated = true;
            }
            reReturned = reFromDbMapper.sourceToTarget(
                    updatedDbRelatedEntity);
            List<RelatedEntityIdentifier> reiReturned =
                    reReturned.getRelatedEntityIdentifier();

            // Persist changes to the identifiers, in the sequence:
            // deleted, modified, added.
            // First, deleted identifiers.
            for (Integer reiID : deletedIdentifiers) {
                au.org.ands.vocabs.registry.db.entity.RelatedEntityIdentifier
                dbREI = existingDbREIMap.get(reiID);
                dbREI.setEndDate(now);
                // Hmm/oops, can't do the following. It seems the database
                // schema doesn't support recording the identity of
                // the user who deleted this entity within the table
                // itself: it must be recorded somewhere else (e.g.,
                // as the registry event).
//                dbREI.setModifiedBy(username);
                // Persist the deleted identifier.
                RelatedEntityIdentifierDAO.updateRelatedEntityIdentifier(em,
                        dbREI);
                reWasUpdated = true;
            }

            // Second, modified identifiers.
            for (au.org.ands.vocabs.registry.db.entity.RelatedEntityIdentifier
                    newDbREI : updatedIdentifiers) {
                Integer dbReiID = newDbREI.getRelatedEntityIdentifierId();
                au.org.ands.vocabs.registry.db.entity.RelatedEntityIdentifier
                    oldDbREI = existingDbREIMap.get(dbReiID);
                oldDbREI.setEndDate(now);
                RelatedEntityIdentifierDAO.updateRelatedEntityIdentifier(em,
                        oldDbREI);

                logger.info("rei id = " + newDbREI.getId());
                newDbREI.setRelatedEntityIdentifierId(dbReiID);
                // Must manually point it back to the parent entity.
                newDbREI.setRelatedEntityId(
                        updatedDbRelatedEntity.getRelatedEntityId());
                newDbREI.setStartDate(now);
                newDbREI.setEndDate(TemporalConstants.CURRENTLY_VALID_END_DATE);
                newDbREI.setModifiedBy(username);
                // Persist the identifier.
                RelatedEntityIdentifierDAO.saveRelatedEntityIdentifier(em,
                        newDbREI);
                reWasUpdated = true;
                reiReturned.add(reiFromDbMapper.sourceToTarget(newDbREI));
            }

            // Third, added identifiers.
            for (RelatedEntityIdentifier rei : newIdentifiers) {
                au.org.ands.vocabs.registry.db.entity.RelatedEntityIdentifier
                newDbREI = reiToDbMapper.sourceToTarget(rei);
                // Must manually point it back to the parent entity.
                newDbREI.setRelatedEntityId(
                        updatedDbRelatedEntity.getRelatedEntityId());
                newDbREI.setStartDate(now);
                newDbREI.setEndDate(TemporalConstants.CURRENTLY_VALID_END_DATE);
                newDbREI.setModifiedBy(username);
                // Persist the identifier.
                RelatedEntityIdentifierDAO.saveRelatedEntityIdentifierWithId(
                        em, newDbREI);
                reWasUpdated = true;
                reiReturned.add(reiFromDbMapper.sourceToTarget(newDbREI));
            }

            // And while we're here, copy over the unmodified identifiers
            // into the value that will be returned to the caller.
            for (RelatedEntityIdentifier rei : unmodifiedIdentifiers) {
                reiReturned.add(rei);
            }

            // Only commit, with the addition of a registry event,
            // if we actually changed the database.
            if (reWasUpdated) {
                // This is a registry event; log it.
                RegistryEvent re = new RegistryEvent();
                re.setElementType(RegistryEventElementType.RELATED_ENTITIES);
                re.setElementId(updatedDbRelatedEntity.getRelatedEntityId());
                // NB: newDbVocab.getStartDate() would not be correct
                // for a draft!
                re.setEventDate(now);
                re.setEventType(RegistryEventEventType.UPDATED);
                re.setEventUser(profile.getUsername());
                // To be done: put something sensible in the details.
                re.setEventDetails("");
                RegistryEventDAO.saveRegistryEvent(em, re);

                // And now, commit all of the above changes.
                txn.commit();
            } else {
                // No database changes were made, so roll back.
                txn.rollback();
            }
            // If we have reached this point, we have success.
            // Analytics logging.
            Logging.logRequest(true, request, uriInfo, profile,
                    Analytics.EVENT_UPDATE_RELATED_ENTITY,
                    Analytics.ID_FIELD, relatedEntityId,
                    Analytics.TITLE_FIELD, updatedDbRelatedEntity.getTitle(),
                    Analytics.OWNER_FIELD, updatedDbRelatedEntity.getOwner(),
                    Analytics.WAS_MODIFIED_FIELD, reWasUpdated);

            // Check
            // https://ardc-services.atlassian.net/wiki/spaces/PROJ/pages/
            //   3376942/Vocabulary+Solr+documents+and+queries+New+Generation
            // (and the EntityIndexer class!)
            // to see how REs are involved in Solr indexing.
            // It seems: only publisher titles are indexed.
            // Therefore, re-index of a vocabulary is needed when and only when:
            //       (a) this RE is a publisher of the vocabulary,
            //   AND (b) the RE's title has changed.
            // So now, see if/what we need to re-index.
            if (!existingDbRelatedEntity.getTitle().equals(
                    updatedDbRelatedEntity.getTitle())) {
                // The title has changed. Find all uses of this as a publisher.
                MultivaluedMap<Vocabulary, RelatedEntityRelation> dbRVs =
                VocabularyDAO.getCurrentVocabulariesForRelatedEntity(
                        relatedEntityId);
                for (Map.Entry<Vocabulary, List<RelatedEntityRelation>>
                    mapElement : dbRVs.entrySet()) {
                    if (mapElement.getValue().contains(
                            RelatedEntityRelation.PUBLISHED_BY)) {
                        Integer vocabularyId =
                                mapElement.getKey().getVocabularyId();
                        logger.info("RE with ID " + relatedEntityId
                                + " was updated; re-indexing vocabulary "
                                + "with ID: " + vocabularyId);
                        EntityIndexer.indexVocabulary(vocabularyId);
                    }
                }
            }

            return Response.ok().entity(reReturned).build();
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
                logger.error("Exception, either during rollback, or "
                        + "outside active transaction", t);
            }
            // Don't throw, but fall through so that the user sees
            // an error message.
//            throw t;
        } finally {
            if (em != null) {
                em.close();
            }
        }

        // If we fell through to here: ouch.
        Logging.logRequest(false, request, uriInfo, profile,
                Analytics.EVENT_UPDATE_RELATED_ENTITY,
                Analytics.FAILURE_REASON, "internal error",
                Analytics.ID_FIELD, relatedEntityId,
                Analytics.TITLE_FIELD,
                    updatedRelatedEntity.getTitle(),
                Analytics.OWNER_FIELD,
                    updatedRelatedEntity.getOwner());
        return ErrorResultUtils.internalServerError();
    }

}
