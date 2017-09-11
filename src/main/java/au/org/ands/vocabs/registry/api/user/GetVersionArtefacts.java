/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
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
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.jax.rs.annotations.Pac4JProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.context.ApiPaths;
import au.org.ands.vocabs.registry.api.context.ResponseUtils;
import au.org.ands.vocabs.registry.api.context.SwaggerInterface;
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.converter.VersionArtefactDbSchemaMapper;
import au.org.ands.vocabs.registry.db.dao.VersionArtefactDAO;
import au.org.ands.vocabs.registry.db.internal.VaConceptTree;
import au.org.ands.vocabs.registry.enums.VersionArtefactType;
import au.org.ands.vocabs.registry.schema.vocabulary201701.VersionArtefact;
import au.org.ands.vocabs.registry.schema.vocabulary201701.VersionArtefactList;
import au.org.ands.vocabs.registry.utils.Logging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/** REST web services for getting version artefacts. */
@Path(ApiPaths.API_RESOURCE + "/" + ApiPaths.VERSIONS)
@Api(value = SwaggerInterface.TAG_RESOURCES)
public class GetVersionArtefacts {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Get the current version artefacts for a version.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile.
     * @param versionId The VersionId of the version from which the
     *      version artefacts are to be fetched.
     * @param vaType An optional VersionArtefactType to filter against.
     * @return The list of vocabularies, in either XML or JSON format. */
    @Path(ApiPaths.VERSION_ID + "/" + ApiPaths.VERSION_ARTEFACTS)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @GET
    @ApiOperation(value = "Get the current version artefacts of a version, "
            + "by its version id. Results can be filtered by "
            + "version artefact type.")
//    @ApiResponses(value = {
//            @ApiResponse(code = HttpStatus.SC_BAD_REQUEST,
//                    message = "Invalid input",
//                    response = ErrorResult.class)
//            })
    public final VersionArtefactList getVersionArtefacts(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(hidden = true) @Pac4JProfile final CommonProfile profile,
            @ApiParam(value = "The ID of the version from which to "
                    + "get the version artefacts")
            @PathParam("versionId") final Integer versionId,
            @ApiParam(value = "Version artefact type used to filter results")
            @QueryParam("type") final VersionArtefactType vaType) {
        logger.debug("called getVersionArtefact");

        Logging.logRequest(true, request, uriInfo, profile,
                "Getting version artefacts");
        List<au.org.ands.vocabs.registry.db.entity.VersionArtefact> dbVAs;

        if (vaType == null) {
            dbVAs = VersionArtefactDAO.
            getCurrentVersionArtefactListForVersion(versionId);
        } else {
            dbVAs = VersionArtefactDAO.
            getCurrentVersionArtefactListForVersionByType(versionId, vaType);
        }
        VersionArtefactList outputVAList = new VersionArtefactList();
        List<VersionArtefact> outputVAs = outputVAList.getVersionArtefact();

        VersionArtefactDbSchemaMapper mapper =
                VersionArtefactDbSchemaMapper.INSTANCE;
        for (au.org.ands.vocabs.registry.db.entity.VersionArtefact dbVA
                : dbVAs) {
            outputVAs.add(mapper.sourceToTarget(dbVA));
        }

        Logging.logRequest(true, request, uriInfo, null,
                "Get version artefacts for version");
        return outputVAList;
    }

    /** Get the current concept tree for a version.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile.
     * @param versionId The VersionId of the version from which the
     *      concept tree is to be returned.
     * @return The concept tree, in JSON format. */
    @Path(ApiPaths.VERSION_ID + "/" + ApiPaths.VERSION_ARTEFACTS
            + "/" + ApiPaths.VERSION_ARTEFACTS_CONCEPT_TREE)
    @Produces({MediaType.APPLICATION_JSON})
    @GET
    @ApiOperation(value = "Get the current concept tree of a version, "
            + "by its version id.")
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_BAD_REQUEST,
                    message = "No current concept tree for that version",
                    response = ErrorResult.class)
            })
    public final Response getVersionArtefactConceptTree(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(hidden = true) @Pac4JProfile final CommonProfile profile,
            @ApiParam(value = "The ID of the version from which to "
                    + "get the concept tree")
            @PathParam("versionId") final Integer versionId) {
        logger.debug("called getVersionArtefactConceptTree");

        Logging.logRequest(true, request, uriInfo, profile,
                "Getting version artefact concept tree");
        List<au.org.ands.vocabs.registry.db.entity.VersionArtefact>
            dbVAs = VersionArtefactDAO.
            getCurrentVersionArtefactListForVersionByType(versionId,
                    VersionArtefactType.CONCEPT_TREE);

        if (dbVAs == null || dbVAs.isEmpty()) {
            return Response.status(Status.BAD_REQUEST).entity(
                    new ErrorResult("No concept tree for that version")).
                    build();
        }

        // Assume exactly one.
        // Or more precisely: ignore the possibility that there might
        // be more than one. We choose the "first", whatever that might be.
        au.org.ands.vocabs.registry.db.entity.VersionArtefact treeVA =
                dbVAs.get(0);

        VaConceptTree conceptTree =
                JSONSerialization.deserializeStringAsJson(treeVA.getData(),
                        VaConceptTree.class);

        File conceptTreeFile = new File(conceptTree.getPath());
        if (!conceptTreeFile.exists()) {
            logger.info("getVersionArtefactConceptTree: file not found: "
                    + conceptTree.getPath());
            return ResponseUtils.generateInternalServerError("File not found");
        }

        Logging.logRequest(true, request, uriInfo, null,
                "Get concept tree for version");
        return Response.ok(conceptTreeFile).build();
    }

}
