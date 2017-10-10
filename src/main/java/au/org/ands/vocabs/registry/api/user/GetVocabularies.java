/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
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
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.jax.rs.annotations.Pac4JProfile;
import org.pac4j.jax.rs.annotations.Pac4JSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.auth.AuthUtils;
import au.org.ands.vocabs.registry.api.context.ApiPaths;
import au.org.ands.vocabs.registry.api.context.SwaggerInterface;
import au.org.ands.vocabs.registry.db.converter.AccessPointDbSchemaMapper;
import au.org.ands.vocabs.registry.db.converter.RelatedEntityDbSchemaMapper;
import au.org.ands.vocabs.registry.db.converter.RelatedEntityIdentifierDbSchemaMapper;
import au.org.ands.vocabs.registry.db.converter.VersionDbSchemaMapper;
import au.org.ands.vocabs.registry.db.converter.VocabularyDbRelatedVocabularySchemaMapper;
import au.org.ands.vocabs.registry.db.converter.VocabularyDbSchemaMapper;
import au.org.ands.vocabs.registry.db.dao.AccessPointDAO;
import au.org.ands.vocabs.registry.db.dao.RelatedEntityDAO;
import au.org.ands.vocabs.registry.db.dao.RelatedEntityIdentifierDAO;
import au.org.ands.vocabs.registry.db.dao.VersionDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.enums.RelatedVocabularyRelation;
import au.org.ands.vocabs.registry.schema.vocabulary201701.AccessPoint;
import au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntity;
import au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntityIdentifier;
import au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntityList;
import au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedVocabulary;
import au.org.ands.vocabs.registry.schema.vocabulary201701.ReverseRelatedVocabulary;
import au.org.ands.vocabs.registry.schema.vocabulary201701.ReverseRelatedVocabularyList;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Version;
import au.org.ands.vocabs.registry.schema.vocabulary201701.VersionList;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary.RelatedEntityRef;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary.RelatedVocabularyRef;
import au.org.ands.vocabs.registry.schema.vocabulary201701.VocabularyList;
import au.org.ands.vocabs.registry.utils.Logging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.ResponseHeader;

/** REST web services for getting vocabularies. */
@Path(ApiPaths.API_RESOURCE + "/" + ApiPaths.VOCABULARIES)
@Api(value = SwaggerInterface.TAG_RESOURCES)
public class GetVocabularies {

    // For my info: if adding a method with return type Response
    // where the value returned is a list, wrap it in a GenericEntity.
    // See http://www.adam-bien.com/roller/abien/entry/jax_rs_returning_a_list

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Get all the current vocabularies. This includes both
     * published and deprecated vocabularies.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param includeDraft If true, also include draft vocabularies.
     * @return The list of vocabularies, in either XML or JSON format. */
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @GET
    @ApiOperation(value = "Get all the current vocabularies. This includes "
            + "both published and deprecated vocabularies.")
    public final VocabularyList getVocabularies(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(value = "If true, also include draft vocabulary records")
            @QueryParam("includeDraft")
            @DefaultValue("false") final boolean includeDraft) {
        if (includeDraft) {
            return getVocabulariesIncludingDraft();
        }
        logger.debug("called getVocabularies");
        List<au.org.ands.vocabs.registry.db.entity.Vocabulary>
            dbVocabularies = VocabularyDAO.getAllCurrentVocabulary();
        VocabularyList outputVocabularyList = new VocabularyList();
        List<Vocabulary> outputVocabularies =
                outputVocabularyList.getVocabulary();

        VocabularyDbSchemaMapper mapper =
                VocabularyDbSchemaMapper.INSTANCE;
        for (au.org.ands.vocabs.registry.db.entity.Vocabulary dbVocabulary
                : dbVocabularies) {
            outputVocabularies.add(mapper.sourceToTarget(dbVocabulary));
        }

        Logging.logRequest(true, request, uriInfo, null,
                "Get all vocabularies");
        return outputVocabularyList;
    }

    /** Get all the current vocabularies, of all status values,
     * including draft.
     * @return The list of vocabularies of all status values,
     *      including draft. */
    public final VocabularyList getVocabulariesIncludingDraft() {
        logger.debug("called getVocabulariesIncludingDraft");
        List<au.org.ands.vocabs.registry.db.entity.Vocabulary>
            dbVocabularies = VocabularyDAO.getAllCurrentVocabulary();
        List<au.org.ands.vocabs.registry.db.entity.Vocabulary>
            dbDraftVocabularies = VocabularyDAO.getAllDraftVocabulary();
        VocabularyList outputVocabularyList = new VocabularyList();
        List<Vocabulary> outputVocabularies =
                outputVocabularyList.getVocabulary();

        VocabularyDbSchemaMapper mapper =
                VocabularyDbSchemaMapper.INSTANCE;
        for (au.org.ands.vocabs.registry.db.entity.Vocabulary dbVocabulary
                : dbVocabularies) {
            outputVocabularies.add(mapper.sourceToTarget(dbVocabulary));
        }
        for (au.org.ands.vocabs.registry.db.entity.Vocabulary dbVocabulary
                : dbDraftVocabularies) {
            outputVocabularies.add(mapper.sourceToTarget(dbVocabulary));
        }

        return outputVocabularyList;
    }

    /** Get the current instance of a vocabulary, by its vocabulary id.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param vocabularyId The VocabularyId of the vocabulary to be fetched.
     * @param includeVersions Whether or not to include version elements.
     * @param includeAccessPoints Whether or not to include access point
     *      elements.
     * @param includeRelatedEntitiesAndVocabularies Whether or not to include
     *      full related entity elements, and top-level details of
     *      related vocabularies.
     * @return The vocabulary, in either XML or JSON format,
     *      or an error result, if there is no such vocabulary. */
    @Path(ApiPaths.VOCABULARY_ID)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @GET
    @ApiOperation(value = "Get a current vocabulary by its id.",
            response = Vocabulary.class)
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_BAD_REQUEST,
                    message = "No vocabulary with that id",
                    response = ErrorResult.class)
            })
    public final Response getVocabularyById(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(value = "The ID of the vocabulary to get")
            @PathParam("vocabularyId") final Integer vocabularyId,
            @ApiParam(value = "Whether or not to include version elements",
                defaultValue = "false")
            @QueryParam("includeVersions") @DefaultValue("false")
            final boolean includeVersions,
            @ApiParam(value = "Whether or not to include access points. "
                + "Setting this to true forces includeVersions "
                + "also to be true.",
                defaultValue = "false")
            @QueryParam("includeAccessPoints") @DefaultValue("false")
            final boolean includeAccessPoints,
            @ApiParam(value = "Whether or not to include full details of "
                    + "related entities, and top-level details of "
                    + "related vocabularies. If false (the default), only "
                    + "references will be included.",
            defaultValue = "false")
            @QueryParam("includeRelatedEntitiesAndVocabularies")
            @DefaultValue("false")
            final boolean includeRelatedEntitiesAndVocabularies) {
        logger.debug("called getVocabularyById: " + vocabularyId);
        au.org.ands.vocabs.registry.db.entity.Vocabulary
            dbVocabulary = VocabularyDAO.getCurrentVocabularyByVocabularyId(
                    vocabularyId);
        Vocabulary outputVocabulary;

        VocabularyDbSchemaMapper mapper =
                VocabularyDbSchemaMapper.INSTANCE;
        outputVocabulary = mapper.sourceToTarget(dbVocabulary);
        if (outputVocabulary == null) {
            return Response.status(Status.BAD_REQUEST).entity(
                    new ErrorResult("No vocabulary with that id")).build();
        }

        // If includeAccessPoints == true,
        // override any "includeVersions=false" setting.
        if (includeVersions || includeAccessPoints) {
            List<au.org.ands.vocabs.registry.db.entity.Version>
            dbVersions = VersionDAO.getCurrentVersionListForVocabulary(
                    vocabularyId);
            List<Version> outputVersions = outputVocabulary.getVersion();

            VersionDbSchemaMapper versionMapper =
                    VersionDbSchemaMapper.INSTANCE;
            for (au.org.ands.vocabs.registry.db.entity.Version dbVersion
                    : dbVersions) {
                Version version = versionMapper.sourceToTarget(dbVersion);
                outputVersions.add(version);
                if (includeAccessPoints) {
                    List<au.org.ands.vocabs.registry.db.entity.AccessPoint>
                    dbAPs = AccessPointDAO.getCurrentAccessPointListForVersion(
                            dbVersion.getVersionId());
                    List<AccessPoint> outputAPs = version.getAccessPoint();

                    AccessPointDbSchemaMapper accessPointMapper =
                            AccessPointDbSchemaMapper.INSTANCE;
                    for (au.org.ands.vocabs.registry.db.entity.AccessPoint dbAP
                            : dbAPs) {
                        outputAPs.add(accessPointMapper.sourceToTarget(dbAP));
                    }
                }
            }
        }

        // If includeRelatedEntitiesAndVocabularies, get the full details of the
        // related entities, and match them up with the refs
        // we already fetched and included in outputVocabulary.
        // And do similarly for related vocabularies.
        if (includeRelatedEntitiesAndVocabularies) {
            Map<Integer, RelatedEntity> relatedEntities =
                    getRelatedEntitiesForVocabularyByIdHelper(vocabularyId);
            for (RelatedEntityRef rer
                    : outputVocabulary.getRelatedEntityRef()) {
                RelatedEntity related = relatedEntities.get(rer.getId());
                if (related != null) {
                    rer.setRelatedEntity(related);
                } else {
                    logger.error("Internal error: unable to match up "
                            + "related entity. vocabulary_id = {}, "
                            + "related entity ID = {}", vocabularyId,
                            rer.getId());
                }
            }
            Map<Integer, RelatedVocabulary> relatedVocabularies =
                    getRelatedVocabulariesForVocabularyByIdHelper(vocabularyId);
            for (RelatedVocabularyRef rvr
                    : outputVocabulary.getRelatedVocabularyRef()) {
                RelatedVocabulary related = relatedVocabularies.get(
                        rvr.getId());
                if (related != null) {
                    rvr.setRelatedVocabulary(related);
                } else {
                    logger.error("Internal error: unable to match up "
                            + "related vocabulary. vocabulary_id = {}, "
                            + "related vocabulary ID = {}", vocabularyId,
                            rvr.getId());
                }
            }
        }

        Logging.logRequest(true, request, uriInfo, null, "Get a vocabulary");
        return Response.ok().entity(outputVocabulary).build();
    }

    /** Query if the user profile is authorized to modify the
     * current instance of a vocabulary, by its vocabulary id.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile.
     * @param vocabularyId The VocabularyId of the vocabulary to be fetched.
     *      related entity elements.
     * @return A SimpleResult containing a Boolean result indicating if
     *      the user is authorized. */
    @Path(ApiPaths.VOCABULARY_ID + "/owns")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Pac4JSecurity
    @GET
    @ApiOperation(value = "Query if the user has authorization to modify "
            + "the current vocabulary. A Boolean result will be returned.",
            response = SimpleResult.class,
            authorizations = {@Authorization(
                    value = SwaggerInterface.BASIC_AUTH),
                    @Authorization(value = SwaggerInterface.API_KEY_AUTH)})
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_BAD_REQUEST,
                    message = "No vocabulary with that id",
                    response = ErrorResult.class),
            @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED,
                    message = "Not authenticated",
                    response = ErrorResult.class,
                    responseHeaders = {
                            @ResponseHeader(name = "WWW-Authenticate",
                                    response = String.class)
                    })
            })
    public final Response ownsVocabularyById(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(hidden = true) @Pac4JProfile final CommonProfile profile,
            @ApiParam(value = "The ID of the vocabulary to check ownership")
            @PathParam("vocabularyId") final Integer vocabularyId) {
        logger.debug("called ownsVocabularyById: " + vocabularyId);
        au.org.ands.vocabs.registry.db.entity.Vocabulary
            dbVocabulary = VocabularyDAO.getCurrentVocabularyByVocabularyId(
                    vocabularyId);
        if (dbVocabulary == null) {
            return Response.status(Status.BAD_REQUEST).entity(
                    new ErrorResult("No vocabulary with that id")).build();
        }

        return Response.ok().entity(new SimpleResult(
                AuthUtils.ownerIsAuthorizedByOrganisationOrUsername(profile,
                dbVocabulary.getOwner()))).build();
    }

    /** Determine if a vocabulary has a draft instance.
     * @param vocabularyId The VocabularyId of the vocabulary to be checked.
     * @return True, if the vocabulary has a draft instance. False,
     *      if there is no draft instance with that vocabulary id.*/
    @Path(ApiPaths.VOCABULARY_ID + "/hasDraft")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @GET
    @ApiOperation(value = "Determine if a vocabulary has a draft instance.",
            notes = "Returns true, if the vocabulary has a draft instance. "
            + "Returns false, if there is no draft instance with that "
            + "vocabulary id. The result is returned in the booleanValue "
            + "property.",
            response = SimpleResult.class)
    public final Response hasDraftVocabularyById(
            @ApiParam(value = "The ID of the vocabulary to check")
            @PathParam("vocabularyId") final Integer vocabularyId) {
        logger.debug("called hasDraftVocabularyById: " + vocabularyId);
        boolean hasDraft = VocabularyDAO.hasDraftVocabulary(vocabularyId);
        return Response.ok().entity(new SimpleResult(hasDraft)).build();
    }

    /** Get the current versions of a vocabulary, by its vocabulary id.
     * @param vocabularyId The VocabularyId of the versions to be fetched.
     * @return The list of versions, in either XML or JSON format,
     *      or an error result, if there is no such vocabulary. */
    @Path(ApiPaths.VOCABULARY_ID + "/" + ApiPaths.VERSIONS)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @GET
    @ApiOperation(value = "Get the current versions of a vocabulary, "
            + "by its vocabulary id.")
    public final VersionList getVersionsForVocabularyById(
            @ApiParam(value = "The ID of the vocabulary from which to get "
                    + "the current versions")
            @PathParam("vocabularyId") final Integer vocabularyId) {
        logger.debug("called getVersionsForVocabularyById: " + vocabularyId);

        List<au.org.ands.vocabs.registry.db.entity.Version>
            dbVersions = VersionDAO.getCurrentVersionListForVocabulary(
                    vocabularyId);
        VersionList outputVersionList = new VersionList();
        List<Version> outputVersions = outputVersionList.getVersion();

        VersionDbSchemaMapper mapper =
                VersionDbSchemaMapper.INSTANCE;
        for (au.org.ands.vocabs.registry.db.entity.Version dbVersion
                : dbVersions) {
            outputVersions.add(mapper.sourceToTarget(dbVersion));
        }

        return outputVersionList;
    }

    /** Get the current related entities of a vocabulary, by its vocabulary id.
     * Delegates to {@link #getRelatedEntitiesForVocabularyByIdHelper(Integer)}
     * to do the work.
     * @param vocabularyId The VocabularyId of the related entities
     *      to be fetched.
     * @return The list of related entities, in either XML or JSON format,
     *      or an error result, if there is no such vocabulary. */
    @Path(ApiPaths.VOCABULARY_ID + "/" + ApiPaths.RELATED_ENTITIES)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @GET
    @ApiOperation(value = "Get the current related entities of a vocabulary, "
            + "by its vocabulary id.")
    public final RelatedEntityList getRelatedEntitiesForVocabularyById(
            @ApiParam(value = "The ID of the vocabulary from which to get "
                    + "the current related entities")
            @PathParam("vocabularyId") final Integer vocabularyId) {
        logger.debug("called getRelatedEntitiesForVocabularyById: "
            + vocabularyId);

        RelatedEntityList outputRelatedEntityList = new RelatedEntityList();

        Map<Integer, RelatedEntity> outputRelatedEntities =
                getRelatedEntitiesForVocabularyByIdHelper(vocabularyId);

        outputRelatedEntityList.getRelatedEntity().
                addAll(outputRelatedEntities.values());
        return outputRelatedEntityList;
    }

    /** Helper method to do the work of getting the current related
     * entities of a vocabulary, by its vocabulary id.
     * @param vocabularyId The VocabularyId of the related entities
     *      to be fetched.
     * @return The map of related entities, from related entity id
     *      to the related entity.
     */
    private Map<Integer, RelatedEntity>
    getRelatedEntitiesForVocabularyByIdHelper(
            final Integer vocabularyId) {
        List<au.org.ands.vocabs.registry.db.entity.RelatedEntity>
            dbRelatedEntities =
                RelatedEntityDAO.getCurrentRelatedEntitiesForVocabulary(
                    vocabularyId);
        Map<Integer, RelatedEntity> outputRelatedEntities = new HashMap<>();

        RelatedEntityDbSchemaMapper reMapper =
                RelatedEntityDbSchemaMapper.INSTANCE;
        RelatedEntityIdentifierDbSchemaMapper reiMapper =
                RelatedEntityIdentifierDbSchemaMapper.INSTANCE;
        for (au.org.ands.vocabs.registry.db.entity.RelatedEntity dbRE
                : dbRelatedEntities) {
            RelatedEntity targetRelatedEntity = reMapper.sourceToTarget(dbRE);
            outputRelatedEntities.put(dbRE.getRelatedEntityId(),
                    targetRelatedEntity);
            // Get the related entity identifiers.
            List<RelatedEntityIdentifier> targetRelatedEntityIdentifiers =
                    targetRelatedEntity.getRelatedEntityIdentifier();
            List<au.org.ands.vocabs.registry.db.entity.RelatedEntityIdentifier>
                dbRelatedEntityIdentifiers =
                    RelatedEntityIdentifierDAO.
                    getCurrentRelatedEntityIdentifierListForRelatedEntity(
                            dbRE.getRelatedEntityId());
            for (au.org.ands.vocabs.registry.db.entity.RelatedEntityIdentifier
                    dbREI : dbRelatedEntityIdentifiers) {
                targetRelatedEntityIdentifiers.add(
                        reiMapper.sourceToTarget(dbREI));
            }
        }
        return outputRelatedEntities;
    }

    /** Helper method to do the work of getting the current related
     * vocabularies of a vocabulary, by its vocabulary id.
     * @param vocabularyId The VocabularyId of the related vocabularies
     *      to be fetched.
     * @return The map of related vocabularies, from vocabulary id
     *      to the related vocabulary.
     */
    private Map<Integer, RelatedVocabulary>
    getRelatedVocabulariesForVocabularyByIdHelper(
            final Integer vocabularyId) {
        List<au.org.ands.vocabs.registry.db.entity.Vocabulary>
            dbRelatedVocabularies =
                VocabularyDAO.getCurrentRelatedVocabulariesForVocabulary(
                    vocabularyId);
        Map<Integer, RelatedVocabulary> outputRelatedVocabularies =
                new HashMap<>();
        VocabularyDbRelatedVocabularySchemaMapper mapper =
                VocabularyDbRelatedVocabularySchemaMapper.INSTANCE;

        for (au.org.ands.vocabs.registry.db.entity.Vocabulary dbVocabulary
                : dbRelatedVocabularies) {
            RelatedVocabulary targetRelatedVocabulary =
                    mapper.sourceToTarget(dbVocabulary);
            outputRelatedVocabularies.put(dbVocabulary.getVocabularyId(),
                    targetRelatedVocabulary);
        }
        return outputRelatedVocabularies;
    }

    /** Get all vocabularies that are related to a vocabulary.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param vocabularyId The Id of the vocabulary
     *      for which the vocabularies related to it are to be fetched.
     * @return The related entity, in either XML or JSON format,
     *      or an error result, if there is no such related entity. */
    @Path(ApiPaths.VOCABULARY_ID + "/"
            + ApiPaths.REVERSE_RELATED_VOCABULARIES)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @GET
    @ApiOperation(value = "Get all current vocabularies related to a "
            + "related entity, by the related entity's id.")
    public final ReverseRelatedVocabularyList
    getVocabulariesRelatedToVocabularyById(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(value = "The ID of the vocabulary for which "
                    + "to get the vocabularies related to it.")
            @PathParam("vocabularyId") final Integer
                vocabularyId) {
        logger.debug("called getVocabulariesRelatedToVocabularyById: "
            + vocabularyId);

        MultivaluedMap<au.org.ands.vocabs.registry.db.entity.Vocabulary,
            RelatedVocabularyRelation> dbRVs =
            VocabularyDAO.getCurrentVocabulariesForRelatedVocabulary(
                    vocabularyId);

        ReverseRelatedVocabularyList outputRVList =
                new ReverseRelatedVocabularyList();
        List<ReverseRelatedVocabulary> outputRVs =
                outputRVList.getReverseRelatedVocabulary();

        VocabularyDbRelatedVocabularySchemaMapper mapper =
                VocabularyDbRelatedVocabularySchemaMapper.INSTANCE;

        for (Map.Entry<au.org.ands.vocabs.registry.db.entity.Vocabulary,
                List<RelatedVocabularyRelation>> mapElement
                : dbRVs.entrySet()) {
            ReverseRelatedVocabulary revRV = new ReverseRelatedVocabulary();
            revRV.setRelatedVocabulary(mapper.sourceToTarget(
                    mapElement.getKey()));
            List<RelatedVocabularyRelation> rvRelationList =
                    revRV.getRelatedVocabularyRelation();
            rvRelationList.addAll(mapElement.getValue());
            outputRVs.add(revRV);
        }

        Logging.logRequest(true, request, uriInfo, null,
                "Get current vocabularies related to a vocabulary "
                + "by its ID");
        return outputRVList;
    }

}
