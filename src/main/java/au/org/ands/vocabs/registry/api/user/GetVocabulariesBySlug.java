/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
import au.org.ands.vocabs.registry.db.converter.AccessPointDbSchemaMapper;
import au.org.ands.vocabs.registry.db.converter.VersionDbSchemaMapper;
import au.org.ands.vocabs.registry.db.converter.VocabularyDbSchemaMapper;
import au.org.ands.vocabs.registry.db.dao.AccessPointDAO;
import au.org.ands.vocabs.registry.db.dao.VersionDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.log.Analytics;
import au.org.ands.vocabs.registry.log.Logging;
import au.org.ands.vocabs.registry.schema.vocabulary201701.AccessPoint;
import au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntity;
import au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedVocabulary;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Version;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary.RelatedEntityRef;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary.RelatedVocabularyRef;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/** REST web services for getting vocabularies, by slug rather than
 * by Id. */
@Path(ApiPaths.API_RESOURCE + "/" + ApiPaths.VOCABULARIES_BY_SLUG)
@Api(value = SwaggerInterface.TAG_RESOURCES)
public class GetVocabulariesBySlug {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Get the current instance of a vocabulary, by its slug.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param slug The slug of the vocabulary to be fetched.
     * @param includeVersions Whether or not to include version elements.
     *      If enabled, versions are returned in reverse order of release date.
     * @param includeAccessPoints Whether or not to include access point
     *      elements.
     * @param includeRelatedEntitiesAndVocabularies Whether or not to include
     *      full related entity elements, and top-level details of
     *      related vocabularies.
     * @return The vocabulary, in either XML or JSON format,
     *      or an error result, if there is no such vocabulary. */
    @Path(ApiPaths.SLUG)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @GET
    @ApiOperation(value = "Get a current vocabulary by its slug.",
            response = Vocabulary.class)
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_BAD_REQUEST,
                    message = "No vocabulary with that slug",
                    response = ErrorResult.class)
            })
    public final Response getVocabularyBySlug(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(value = "The slug of the vocabulary to get.")
            @NotNull(message = "The slug must be specified.")
            @PathParam("slug") final String slug,
            @ApiParam(value = "Whether or not to include version elements. "
                    + "If enabled, versions are returned in "
                    + "reverse order of release date.",
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
        logger.debug("called getVocabularyBySlug: " + slug);
        au.org.ands.vocabs.registry.db.entity.Vocabulary
            dbVocabulary = VocabularyDAO.getCurrentVocabularyBySlug(slug);
        Vocabulary outputVocabulary;

        VocabularyDbSchemaMapper mapper =
                VocabularyDbSchemaMapper.INSTANCE;
        outputVocabulary = mapper.sourceToTarget(dbVocabulary, true);
        if (outputVocabulary == null) {
            return Response.status(Status.BAD_REQUEST).entity(
                    new ErrorResult("No vocabulary with that slug")).build();
        }

        Integer vocabularyId = dbVocabulary.getVocabularyId();
        // If includeAccessPoints == true,
        // override any "includeVersions=false" setting.
        if (includeVersions || includeAccessPoints) {
            List<au.org.ands.vocabs.registry.db.entity.Version>
            dbVersions = VersionDAO.
            getCurrentVersionListForVocabularyByReleaseDate(
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
                    GetVocabularies.getRelatedEntitiesForVocabularyByIdHelper(
                            vocabularyId);
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
                    GetVocabularies.
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

        Logging.logRequest(true, request, uriInfo, null,
                Analytics.EVENT_GET_VOCABULARY,
                Analytics.VOCABULARY_LOOKUP_FIELD,
                    Analytics.VOCABULARY_LOOKUP_BY_SLUG,
                Analytics.ID_FIELD, vocabularyId,
                Analytics.TITLE_FIELD, outputVocabulary.getTitle(),
                Analytics.ENTITY_STATUS_FIELD,
                    outputVocabulary.getStatus(),
                Analytics.OWNER_FIELD, outputVocabulary.getOwner());
        return Response.ok().entity(outputVocabulary).build();
    }

}
