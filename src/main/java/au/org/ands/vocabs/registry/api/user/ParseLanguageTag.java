/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.context.ApiPaths;
import au.org.ands.vocabs.registry.api.context.SwaggerInterface;
import au.org.ands.vocabs.registry.schema.vocabulary201701.LanguageDetails;
import au.org.ands.vocabs.registry.schema.vocabulary201701.LanguageList;
import au.org.ands.vocabs.registry.utils.language.Languages;
import au.org.ands.vocabs.registry.utils.language.ParsedLanguage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/** REST web service for parsing a language tag. */
@Path(ApiPaths.API_UTILITIES + "/" + ApiPaths.LANGUAGES)
@Api(value = SwaggerInterface.TAG_UTILITIES)
public class ParseLanguageTag {

    /** Logger for this class. */
    private final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Parse a language tag.
     * @param tag Langauge tag, in the format specified in BCP 47.
     * @return The parsing of the language tag. */
    @Path("parseLanguageTag")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @GET
    @ApiOperation(value = "Parse a BCP 47 language tag.",
            response = LanguageDetails.class,
            notes = "If the tag is valid, the response "
                    + "will contain the canonical form of the tag, and"
                    + "its full description. If the tag is "
                    + "invalid, the response will contain the errors "
                    + "generated during parsing.")
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_BAD_REQUEST,
                    message = "Invalid tag",
                    response = ErrorResult.class)
            })
    public Response parseLanguageTag(
            @ApiParam(value = "Language tag to be parsed.",
                required = true)
            @NotNull(message = "The language tag must not be null")
            @QueryParam("tag")
            final String tag) {
        ParsedLanguage parsedLanguage = Languages.getParsedLanguage(tag);
        if (parsedLanguage == null) {
            return ErrorResultUtils.badRequest(
                    "Invalid language tag (internal error)");
        }
        if (!parsedLanguage.isValid()) {
            // In this method, we return precise error messages.
            ErrorResult errorResult = new ErrorResult("Invalid language tag");
            List<ValidationError> validationErrors = new ArrayList<>();
            for (String error : parsedLanguage.getErrors()) {
                validationErrors.add(new ValidationError(error, "tag"));
            }
            errorResult.setConstraintViolation(validationErrors);
            return Response.status(Status.BAD_REQUEST).entity(errorResult).
                    build();
        }
        LanguageDetails languageDetails = new LanguageDetails();
        languageDetails.setTag(parsedLanguage.getCanonicalForm());
        languageDetails.setDescription(parsedLanguage.getDescription());
        return Response.ok().entity(languageDetails).build();
    }

    /** Parse several language tags.
     * @param tags Langauge tags, in the format specified in BCP 47.
     * @return The parsing of the language tags.
     *      The result contains only parsings of the valid tags;
     *      any invalid language tags are ignored. */
    @Path("parseLanguageTags")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @GET
    @ApiOperation(value = "Parse several BCP 47 language tags.",
            response = LanguageList.class,
            notes = "The response contains an entry for every valid tag. "
                    + "Invalid tags are ignored.")
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_BAD_REQUEST,
                    message = "Invalid tag list",
                    response = ErrorResult.class)
            })
    public Response parseLanguageTags(
            @ApiParam(value = "Language tags to be parsed.",
                required = true)
            @NotNull(message = "The language tags must not be null")
            @QueryParam("tags")
            final List<String> tags) {
        LanguageList languageList = new LanguageList();
        List<LanguageDetails> languageDetailsList =
                languageList.getLanguageDetails();
        for (String tag : tags) {
            ParsedLanguage parsedLanguage = Languages.getParsedLanguage(tag);
            if (parsedLanguage == null) {
                logger.error("Invalid language tag (internal error): " + tag);
                return ErrorResultUtils.badRequest(
                        "Invalid language tag (internal error)");
            }
            if (!parsedLanguage.isValid()) {
                // In this method, we ignore invalid tags.
                continue;
            }
            LanguageDetails languageDetails = new LanguageDetails();
            languageDetails.setTag(parsedLanguage.getCanonicalForm());
            languageDetails.setDescription(parsedLanguage.getDescription());
            languageDetailsList.add(languageDetails);
        }
        return Response.ok().entity(languageList).build();
    }

}
