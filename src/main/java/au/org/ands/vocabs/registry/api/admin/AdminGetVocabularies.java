/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.admin;

import java.lang.invoke.MethodHandles;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.http.HttpStatus;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.jax.rs.annotations.Pac4JProfile;
import org.pac4j.jax.rs.annotations.Pac4JSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.auth.AuthUtils;
import au.org.ands.vocabs.registry.api.context.AdminApiPaths;
import au.org.ands.vocabs.registry.api.context.ApiPaths;
import au.org.ands.vocabs.registry.api.context.ResponseUtils;
import au.org.ands.vocabs.registry.api.context.SwaggerInterface;
import au.org.ands.vocabs.registry.api.user.ErrorResult;
import au.org.ands.vocabs.registry.db.converter.VocabularyDbSchemaMapper;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;
import au.org.ands.vocabs.registry.schema.vocabulary201701.VocabularyList;
import au.org.ands.vocabs.registry.utils.Logging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.ResponseHeader;

/** REST web services for getting vocabularies. These methods are
 * for admin users only. */
@Path(AdminApiPaths.API_ADMIN + "/" + AdminApiPaths.RESOURCE
        + "/" + ApiPaths.VOCABULARIES)
@Api(value = SwaggerInterface.TAG_ADMIN,
        authorizations = {@Authorization(value = SwaggerInterface.BASIC_AUTH),
        @Authorization(value = SwaggerInterface.API_KEY_AUTH)})
public class AdminGetVocabularies {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    // NB: This was previously invoked by getVocabularies() if
    // the user provided a query parameter includeDrafts=true.
    // But now, don't support that for all users: it is admin-level
    // access only.
    /** Get all the current vocabularies, of all status values,
     * including draft.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile.
     * @return The list of vocabularies of all status values,
     *      including draft, in either XML or JSON format. */
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Pac4JSecurity
    @GET
    @ApiOperation(value = "Get all the current (published and deprecated) "
            + "and draft vocabularies.",
            notes = "This method is only available to administrator users.",
            response = VocabularyList.class)
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
    public final Response getVocabulariesIncludingDraft(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(hidden = true) @Pac4JProfile
            final CommonProfile profile) {
        logger.debug("called getVocabulariesIncludingDraft");

        if (!AuthUtils.profileIsSuperuser(profile)) {
            return ResponseUtils.generateForbiddenResponseNotSuperuser();
        }

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
            outputVocabularies.add(mapper.sourceToTarget(dbVocabulary, true));
        }
        for (au.org.ands.vocabs.registry.db.entity.Vocabulary dbVocabulary
                : dbDraftVocabularies) {
            outputVocabularies.add(mapper.sourceToTarget(dbVocabulary, true));
        }

        Logging.logRequest(true, request, uriInfo, profile,
                "Admin: get all vocabularies including drafts");
        return Response.ok(outputVocabularyList).build();
    }

}
