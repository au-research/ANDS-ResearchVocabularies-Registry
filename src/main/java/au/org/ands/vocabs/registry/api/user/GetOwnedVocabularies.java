/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.http.HttpStatus;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.jax.rs.annotations.Pac4JProfile;
import org.pac4j.jax.rs.annotations.Pac4JSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.context.ApiPaths;
import au.org.ands.vocabs.registry.api.context.SwaggerInterface;
import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.db.internal.VocabularyJson;
import au.org.ands.vocabs.registry.log.Analytics;
import au.org.ands.vocabs.registry.log.Logging;
import au.org.ands.vocabs.registry.schema.vocabulary201701.OwnedVocabulary;
import au.org.ands.vocabs.registry.schema.vocabulary201701.OwnedVocabularyList;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.ResponseHeader;

/** REST web services for getting vocabularies owned by the caller. */
@Path(ApiPaths.API_RESOURCE + "/" + ApiPaths.OWNED_VOCABULARIES)
@Api(value = SwaggerInterface.TAG_RESOURCES,
        authorizations = {@Authorization(value = SwaggerInterface.BASIC_AUTH),
        @Authorization(value = SwaggerInterface.API_KEY_AUTH)})
public class GetOwnedVocabularies {

    // For my info: if adding a method with return type Response
    // where the value returned is a list, wrap it in a GenericEntity.
    // See http://www.adam-bien.com/roller/abien/entry/jax_rs_returning_a_list

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Get a list of all vocabularies owned by the caller.
     * The caller must be authenticated.
     * NB: For now, does not take superuser status into account.
     * Doing so could be future work.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile.
     * @return The list of vocabularies owned by the caller, in either
     *      XML or JSON format. */
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Pac4JSecurity
    @GET
    @ApiOperation(value = "Get a list of vocabularies owned by the caller.",
        notes = "The result list contains one element for each vocabulary "
                + "owned by the caller. "
                + "The status attribute will be \"draft\" if there is only "
                + "a draft instance of the vocabulary; in this case, "
                + "hasDraft will be true. "
                + "Otherwise, status will be either "
                + "\"published\" or \"deprecated\", and hasDraft will be "
                + "true if there is also a draft instance.")
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED,
                    message = "Not authenticated",
                    response = ErrorResult.class,
                    responseHeaders = {
                            @ResponseHeader(name = "WWW-Authenticate",
                                    response = String.class)
                            })
            })
    public OwnedVocabularyList getOwnedVocabularies(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(hidden = true) @Pac4JProfile
            final CommonProfile profile) {
        logger.debug("called getOwnedVocabularies");

        List<String> roles = new ArrayList<>();

        // Add organisational roles.
        roles.addAll(profile.getRoles());
        // And add the user's own role.
        roles.add(profile.getId());
        List<au.org.ands.vocabs.registry.db.entity.Vocabulary>
        dbVocabularies = VocabularyDAO.getOwnedVocabularies(roles);
        OwnedVocabularyList outputVocabularyList = new OwnedVocabularyList();
        List<OwnedVocabulary> outputVocabularies =
                outputVocabularyList.getOwnedVocabulary();

        Map<Integer, OwnedVocabulary> ownedVocabulariesMap = new
                HashMap<>();
        for (au.org.ands.vocabs.registry.db.entity.Vocabulary vocabulary
                : dbVocabularies) {
            Integer vocabularyId = vocabulary.getVocabularyId();
            OwnedVocabulary existingOwnedVocabulary =
                    ownedVocabulariesMap.get(vocabularyId);
            if (existingOwnedVocabulary == null) {
                OwnedVocabulary newOwnedVocabulary =
                        new OwnedVocabulary();
                // For now, too simple to worth bothering about making a
                // MapStruct mapper for this. Just copy the fields.
                newOwnedVocabulary.setId(vocabularyId);
                newOwnedVocabulary.setStatus(vocabulary.getStatus());
                newOwnedVocabulary.setOwner(vocabulary.getOwner());
                newOwnedVocabulary.setSlug(vocabulary.getSlug());
                VocabularyJson vocabularyJson = JSONSerialization.
                        deserializeStringAsJson(vocabulary.getData(),
                                VocabularyJson.class);
                newOwnedVocabulary.setTitle(vocabularyJson.getTitle());
                // This relies on the vocabularies that come back
                // from the database being ordered by
                // start date, so that we see a current instance before
                // a draft. If there are both, the status will be set
                // to false by the "else" branch in a later iteration
                // of the loop.
                newOwnedVocabulary.setHasDraft(
                        TemporalUtils.isDraft(vocabulary));
                ownedVocabulariesMap.put(vocabularyId, newOwnedVocabulary);
            } else {
                // We've already seen it, so this must be a draft row
                // for the same vocabulary.
                existingOwnedVocabulary.setHasDraft(true);
            }
        }

        outputVocabularies.addAll(ownedVocabulariesMap.values());

        Logging.logRequest(true, request, uriInfo, profile,
                Analytics.EVENT_GET_VOCABULARY_LIST);

        return outputVocabularyList;
    }

}
