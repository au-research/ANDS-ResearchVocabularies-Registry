/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.context.ApiPaths;
import au.org.ands.vocabs.registry.api.context.ResponseUtils;
import au.org.ands.vocabs.registry.api.context.SwaggerInterface;
import au.org.ands.vocabs.registry.solr.SearchIndex;
import au.org.ands.vocabs.registry.utils.Logging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/** REST web services for searching. */
@Path(ApiPaths.API_SERVICES)
@Api(value = SwaggerInterface.TAG_SERVICES)
public class Search {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Perform a search against the Solr index.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param filtersJson Search filters to be passed on to Solr,
     *      in JSON format.
     * @return The search results, in JSON format.
     */
    @Path("search")
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.APPLICATION_JSON})
    @POST
    @ApiOperation(value = "Perform a search.", response = String.class)
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
                    + "e.g., '\"publisher\":\"CSIRO\"'. Supported factes: "
                    + "\"access\", \"format\", \"language\", \"licence\", "
                    + "\"publisher\", \"subject_labels\".")
            @FormParam("filtersJson") final String filtersJson
            ) {
        logger.debug("called search");
        Logging.logRequest(true, request, uriInfo, null,
                "Search");
        try {
            String queryResponse = SearchIndex.query(filtersJson);
            return Response.ok(queryResponse).build();
        } catch (IOException | SolrServerException e) {
            return ResponseUtils.generateInternalServerError(
                    "Error response from Solr");
        }
    }

}
