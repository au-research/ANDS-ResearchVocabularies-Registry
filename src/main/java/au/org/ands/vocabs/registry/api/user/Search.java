/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.http.HttpStatus;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.context.ApiPaths;
import au.org.ands.vocabs.registry.api.context.FormContentTypeFilter;
import au.org.ands.vocabs.registry.api.context.ResponseUtils;
import au.org.ands.vocabs.registry.api.context.SwaggerInterface;
import au.org.ands.vocabs.registry.log.Analytics;
import au.org.ands.vocabs.registry.log.Logging;
import au.org.ands.vocabs.registry.solr.SearchIndex;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/** REST web services for searching. */
@Path(ApiPaths.API_SERVICES)
@Api(value = SwaggerInterface.TAG_SERVICES)
public class Search {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /* There's a "feature" here with the fact that we're using Jersey.
     * The filterJson parameter can also be specified as a
     * <i>query</i> parameter, and Jersey will pass it on.
     * However, that by itself won't work: because we have
     * the @FormContentTypeFilter annotation, you still have
     * to set the Content-Type as "application/x-www-form-urlencoded"
     * (but you don't have to provide a body!).
     * To disable this feature, see the Jersey property
     * ServletProperties.QUERY_PARAMS_AS_FORM_PARAMS_DISABLED
     * (= "jersey.config.servlet.form.queryParams.disabled").
     */
    /** Perform a search against the Solr index.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param filtersJson Search filters to be passed on to Solr,
     *      in JSON format.
     * @return The search results, as a JSON object.
     */
    @Path(ApiPaths.SEARCH)
    @FormContentTypeFilter
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.APPLICATION_JSON})
    @POST
    @ApiOperation(value = "Perform a search.", response = Object.class)
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_BAD_REQUEST,
                    message = "Invalid input",
                    response = ErrorResult.class)
    })
    public final Response search(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(value = "Filters that define the search parameters. "
                    + "The filters are specified "
                    + "as a JSON object. Examples of keys/values supported: "
                    + "'\"q\":\"query term\"': query term; "
                    + "'\"pp\":10' number of results per page;"
                    + "'\"p\":3' page number of results; "
                    + "'\"widgetable\":true': select only widgetable "
                    + "vocabularies. Facets are also specified with filters: "
                    + "e.g., '\"publisher\":\"CSIRO\"'. Supported facets: "
                    + "\"access\", \"format\", \"language\", \"licence\", "
                    + "\"publisher\", \"subject_labels\".")
            @NotNull(message = "The filtersJson parameter must not be null")
            @FormParam("filtersJson") final String filtersJson
            ) {
        logger.debug("called search");
        try {
            List<Object> filtersExtracted = new ArrayList<>();
            String queryResponse = SearchIndex.query(filtersJson,
                    filtersExtracted);
            Logging.logRequest(true, request, uriInfo, null,
                    Analytics.EVENT_SEARCH,
                    filtersExtracted.toArray());
            return Response.ok(queryResponse).build();
        } catch (IOException | SolrServerException e) {
            Logging.logRequest(false, request, uriInfo, null,
                    Analytics.EVENT_SEARCH,
                    Analytics.FAILURE_REASON, "internal error");
            return ResponseUtils.generateInternalServerError(
                    "Error response from Solr");
        } catch (IllegalArgumentException e) {
            Logging.logRequest(false, request, uriInfo, null,
                    Analytics.EVENT_SEARCH,
                    Analytics.FAILURE_REASON, "validation");
            return ErrorResultUtils.badRequest("Error in request: "
                    + e.getMessage());
        }
    }

}
