/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user;

import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import au.org.ands.vocabs.registry.api.context.ApiPaths;
import au.org.ands.vocabs.registry.utils.SlugGenerator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/** REST web service for generating slugs. */
@Path(ApiPaths.API_UTILITIES)
@Api
public class GenerateSlug {

    /** Generate a slug.
     * @param inputString String from which the slug is generated.
     * @return The list of vocabularies, in either XML or JSON format. */
    @Path("generateSlug")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @GET
    @ApiOperation(value = "Generate a slug.")
    public final SimpleResult generateSlug(
            @ApiParam(value = "String from which the slug is generated.",
                required = true)
            @NotNull(message = "The input string must not be null")
            @QueryParam("input")
            final String inputString) {
        return new SimpleResult(SlugGenerator.generateSlug(inputString));
    }

}
