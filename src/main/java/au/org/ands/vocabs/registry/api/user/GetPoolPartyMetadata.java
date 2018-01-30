/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.api.user;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.pac4j.jax.rs.annotations.Pac4JSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.context.ApiPaths;
import au.org.ands.vocabs.registry.api.context.SwaggerInterface;
import au.org.ands.vocabs.registry.workflow.provider.harvest.PoolPartyHarvestProvider;
import au.org.ands.vocabs.registry.workflow.provider.transform.GetMetadataTransformProvider;
import au.org.ands.vocabs.registry.workflow.tasks.TaskRunner;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.ResponseHeader;

/** REST web services for getting vocabulary metadata. */
@Path(ApiPaths.API_SERVICES)
@Api(value = SwaggerInterface.TAG_SERVICES,
        authorizations = {@Authorization(value = SwaggerInterface.BASIC_AUTH),
        @Authorization(value = SwaggerInterface.API_KEY_AUTH)})

public class GetPoolPartyMetadata {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Get the list of PoolParty projects.
     * @param pPServerId PoolParty server id.
     * @return The list of PoolParty projects, in JSON format,
     * as returned by PoolParty. */
    @Path("PoolParty/{serverId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Pac4JSecurity
    @GET
    @ApiOperation(value = "Get the list of PoolParty projects.",
            response = Object.class)
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
                    response = ErrorResult.class)
            })
    public final Response getPoolPartyProjects(
            @ApiParam(value = "The PoolParty server Id.",
                    defaultValue = "1")
            @PathParam("serverId") @DefaultValue("1")
            final Integer pPServerId) {
        logger.debug("called getInfoPoolParty");
        PoolPartyHarvestProvider provider = new PoolPartyHarvestProvider();
        String response = provider.getInfo(pPServerId);
        if (response.startsWith("[")) {
            // The PoolParty server should respond with a JSON list.
            return Response.ok(response).build();
        } else {
            return ErrorResultUtils.badRequest(response);
        }
    }

    /** Get metadata for a PoolParty project.
     * @param pPServerId PoolParty server id.
     * @param pPProjectId PoolParty project id.
     * @return The metadata for this PoolParty project, in JSON format,
     * as returned by PoolParty. */
    @Path("PoolParty/{serverId}/{projectId}/metadata")
    @Produces(MediaType.APPLICATION_JSON)
    @Pac4JSecurity
    @GET
    @ApiOperation(value = "Get the metadata for a PoolParty project.",
            response = Object.class)
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
                    response = ErrorResult.class)
            })
    public final Response getPoolPartyProjectMetadata(
            @ApiParam(value = "The PoolParty server Id.",
                defaultValue = "1")
            @PathParam("serverId") @DefaultValue("1")
            final Integer pPServerId,
            @ApiParam(value = "The PoolParty project Id.",
                required = true)
            @NotNull(message = "The project Id must be specified")
            @PathParam("projectId") final String pPProjectId) {
        HashMap<String, Object> result = new HashMap<>();
        logger.info("called getMetadata/poolParty " + pPProjectId);
        Map<String, String> harvestResults =
                new PoolPartyHarvestProvider().getMetadata(
                        pPServerId, pPProjectId);
        if (harvestResults != null
                && harvestResults.containsKey(TaskRunner.ERROR)) {
            // There was a problem getting the data from PoolParty,
            // so stop here.
            return ErrorResultUtils.badRequest(harvestResults.get(
                    TaskRunner.ERROR));
        }
        result.putAll(new GetMetadataTransformProvider().
                extractMetadata(pPProjectId));
        return Response.ok(result).build();
    }
}
