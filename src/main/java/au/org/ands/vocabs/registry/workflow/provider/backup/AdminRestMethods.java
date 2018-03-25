/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.workflow.provider.backup;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.jax.rs.annotations.Pac4JProfile;
import org.pac4j.jax.rs.annotations.Pac4JSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.auth.AuthUtils;
import au.org.ands.vocabs.registry.api.context.AdminApiPaths;
import au.org.ands.vocabs.registry.api.context.ResponseUtils;
import au.org.ands.vocabs.registry.api.context.SwaggerInterface;
import au.org.ands.vocabs.registry.api.user.ErrorResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.ResponseHeader;

/** REST web services for doing backups. */
@Path(AdminApiPaths.API_ADMIN + "/" + AdminApiPaths.BACKUP)
@Api(value = SwaggerInterface.TAG_ADMIN,
        authorizations = {@Authorization(value = SwaggerInterface.BASIC_AUTH),
        @Authorization(value = SwaggerInterface.API_KEY_AUTH)})
public class AdminRestMethods {

    /** Top-level of path for PoolParty admin API methods. */
    private static final String POOLPARTY = "PoolParty";

    /** Path component for PoolParty project Id. */
    private static final String PROJECT_ID = "projectId";

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Backup a PoolParty project.
     * @param profile The caller's security profile.
     * @param pPProjectId A PoolParty project ID.
     * @return The result info, in JSON format,
     * containing all files and some metadata. */
    @Path(POOLPARTY + "/" + "{" + PROJECT_ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    @Pac4JSecurity
    @GET
    @ApiOperation(value = "Backup one PoolParty project.",
            notes = "This method is only available to administrator users.",
            response = HashMap.class)
    @ApiResponses(value = {
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
    public final Response backupPoolPartyProject(
            @ApiParam(hidden = true) @Pac4JProfile
            final CommonProfile profile,
            @PathParam(PROJECT_ID)
            final String pPProjectId) {
        if (!AuthUtils.profileIsSuperuser(profile)) {
            return ResponseUtils.generateForbiddenResponseNotSuperuser();
        }
        HashMap<String, Object> result = new HashMap<>();
        logger.debug("Called doBackup/PoolParty/ " + pPProjectId);
        result.putAll(new PoolPartyBackupProvider().backup(pPProjectId));
        return Response.ok(result).build();
    }

    /** Backup all PoolParty projects.
     * @param profile The caller's security profile.
     * @return The result info, in JSON format,
     * containing all files and some metadata. */
    @Path(POOLPARTY)
    @Produces(MediaType.APPLICATION_JSON)
    @Pac4JSecurity
    @GET
    @ApiOperation(value = "Backup all PoolParty projects.",
            notes = "This method is only available to administrator users.",
            response = HashMap.class)
    @ApiResponses(value = {
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
    public final Response backupAllPoolPartyProjects(
            @ApiParam(hidden = true) @Pac4JProfile
            final CommonProfile profile) {
        if (!AuthUtils.profileIsSuperuser(profile)) {
            return ResponseUtils.generateForbiddenResponseNotSuperuser();
        }
        HashMap<String, Object> result = new HashMap<>();
        logger.debug("Called doBackup/PoolParty/all");
        result.putAll(new PoolPartyBackupProvider().backup(null));
        return Response.ok(result).build();
    }
}
