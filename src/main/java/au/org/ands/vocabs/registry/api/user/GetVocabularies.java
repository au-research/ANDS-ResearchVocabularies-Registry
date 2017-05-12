/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user;

import java.lang.invoke.MethodHandles;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
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
import au.org.ands.vocabs.registry.schema.vocabulary201701.AccessPoint;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Version;
import au.org.ands.vocabs.registry.schema.vocabulary201701.VersionList;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;
import au.org.ands.vocabs.registry.schema.vocabulary201701.VocabularyList;
import au.org.ands.vocabs.registry.utils.Logging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

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
//    @Pac4JSecurity(authorizers = AuthConstants.IS_AUTHENTICATED)
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

        Logging.logRequest(request, uriInfo, null, "Get all vocabularies");
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
            final boolean includeAccessPoints) {
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

        Logging.logRequest(request, uriInfo, null, "Get a vocabulary");
        return Response.ok().entity(outputVocabulary).build();
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

}
