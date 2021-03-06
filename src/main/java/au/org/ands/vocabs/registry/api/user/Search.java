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
import au.org.ands.vocabs.registry.solr.SearchRegistryIndex;
import au.org.ands.vocabs.registry.solr.SearchResourcesIndex;
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

    /** The value of the Portal-ID request header that indicates that
     * the request is being issued by the Widget Explorer. We don't
     * include search results in analytics logging in that case.
     */
    private static final String PORTAL_JS_WIDGET = "Portal-JS-widget";

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
    /** Perform a search against the registry Solr index.
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
    @ApiOperation(value = "Perform a search of vocabulary metadata.",
        response = Object.class)
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_BAD_REQUEST,
                    message = "Invalid input",
                    response = ErrorResult.class)
    })
    public Response search(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(value = "Filters that define the search parameters. "
                    + "The filters are specified "
                    + "as a JSON object. Examples of keys/values supported: "
                    + "'\"q\":\"query term\"': query term; "
                    + "'\"pp\":10' number of results per page;"
                    + "'\"p\":3' page number of results; "
                    + "'\"sort\":\"zToA\"': sort order of results; "
                    + "'\"widgetable\":true': select only widgetable "
                    + "vocabularies. Facets are also specified with filters: "
                    + "e.g., '\"publisher\":\"CSIRO\"'. Supported facets: "
                    + "\"access\", \"format\", \"language\", \"licence\", "
                    + "\"publisher\", \"subject_labels\".")
            @NotNull(message = "The filtersJson parameter must not be null")
            @FormParam("filtersJson") final String filtersJson
            ) {
        logger.debug("called search");
        boolean logResults = !PORTAL_JS_WIDGET.equals(
                request.getHeader(Analytics.PORTAL_ID));
        try {
            List<Object> filtersAndResultsExtracted = new ArrayList<>();
            String queryResponse = SearchRegistryIndex.query(filtersJson,
                    filtersAndResultsExtracted, logResults);
            Logging.logRequest(true, request, uriInfo, null,
                    Analytics.EVENT_SEARCH,
                    filtersAndResultsExtracted.toArray());
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

    /** Perform a search against the resources Solr index.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param filtersJson Search filters to be passed on to Solr,
     *      in JSON format.
     * @return The search results, as a JSON object.
     */
    @Path(ApiPaths.SEARCH + "/" + ApiPaths.RESOURCES)
    @FormContentTypeFilter
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.APPLICATION_JSON})
    @POST
    @ApiOperation(value = "Perform a search of vocabulary resources.",
        response = Object.class)
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_BAD_REQUEST,
                    message = "Invalid input",
                    response = ErrorResult.class)
    })
    public Response searchResources(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(value = "Filters that define the search parameters. "
                    + "The filters are specified "
                    + "as a JSON object. Examples of keys/values supported: "
                    + "'\"q\":\"query term\"': query term; "
                    + "'\"pp\":10' number of results per page;"
                    + "'\"p\":3' page number of results; "
                    + "'\"sort\":\"zToA\"': sort order of results; "
                    + "'\"collapse_expand\":false' disable collapse/expand "
                    + "results with the same IRI; "
                    + "'\"language\":\"[\""
                    + SearchResourcesIndex.NO_LANGUAGE
                    + "\",\"en\"]\"': limit to "
                    + "specified languages (to include results for all "
                    + "languages, don't specify a value for this filter). "
                    + "Facets are also specified with filters: "
                    + "e.g., '\"publisher\":\"CSIRO\"'. Supported facets: "
                    + "\"publisher\", \"rdf_type\", \"status\", "
                    + "\"subject_labels\".")
            @NotNull(message = "The filtersJson parameter must not be null")
            @FormParam("filtersJson") final String filtersJson
            ) {
        logger.debug("called searchResources");
        boolean logResults = !PORTAL_JS_WIDGET.equals(
                request.getHeader(Analytics.PORTAL_ID));
        try {
            List<Object> filtersAndResultsExtracted = new ArrayList<>();
            String queryResponse = SearchResourcesIndex.query(filtersJson,
                    filtersAndResultsExtracted, logResults);
            Logging.logRequest(true, request, uriInfo, null,
                    Analytics.EVENT_SEARCH_RESOURCES,
                    filtersAndResultsExtracted.toArray());
            return Response.ok(queryResponse).build();
        } catch (IOException | SolrServerException e) {
            Logging.logRequest(false, request, uriInfo, null,
                    Analytics.EVENT_SEARCH_RESOURCES,
                    Analytics.FAILURE_REASON, "internal error");
            return ResponseUtils.generateInternalServerError(
                    "Error response from Solr");
        } catch (IllegalArgumentException e) {
            Logging.logRequest(false, request, uriInfo, null,
                    Analytics.EVENT_SEARCH_RESOURCES,
                    Analytics.FAILURE_REASON, "validation");
            return ErrorResultUtils.badRequest("Error in request: "
                    + e.getMessage());
        }
    }

}
