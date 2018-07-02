/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import javax.ws.rs.DefaultValue;
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
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.dao.AccessPointDAO;
import au.org.ands.vocabs.registry.db.dao.ResourceMapEntryDAO;
import au.org.ands.vocabs.registry.db.entity.AccessPoint;
import au.org.ands.vocabs.registry.db.entity.ResourceMapEntry;
import au.org.ands.vocabs.registry.db.internal.ApSissvoc;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;

/** REST web services for resolution of concept IRIs. */
@Path(ApiPaths.API_SERVICES)
@Api(value = SwaggerInterface.TAG_SERVICES)
public class ResolveIRI {

    /** Error message to return, if no IRI is specified as a query
     * parameter in the request URL. */
    public static final String NO_IRI_SPECIFIED =
            "No IRI specified as query parameter.";

    /** Error message to return, if more than one vocabulary defines
     * the resource. */
    public static final String MULTIPLE_DEFINITIONS =
            "Can't resolve: more than one vocabulary defines the resource";

    /** Error message to return, if an unsupported lookup mode is
    * requested. */
   public static final String UNSUPPORTED_MODE = "Unsupported mode";

   /** Error message to return, if no vocabulary defines
     * the resource. */
   public static final String NO_DEFINITION =
           "Can't resolve: no vocabulary defines the resource";

    /** Error message to return, if what would be sent back to the
     * client is found to be not a valid URL. */
    public static final String CAN_NOT_REDIRECT =
            "Can't redirect: the result is not a valid URL";

    /** Logger for this class. */

    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /* NB: You can't have a @NotNull(message = "The IRI must be specified.")
       annotation on the iri parameter, as violations generate an
       ErrorResult, and we need a plain text answer to go back.
       (If you use the annotation, and it is triggered, you get an
       exception because there is no MessageBodyWriter that can turn
       an ErrorResult into text/plain.)
     */

    /** Resolve an IRI against the global map of resources.
     * An idea for possible future support: query parameter code=xyz,
     * to specify the return status code (other than the default,
     * provided by {@link Response#temporaryRedirect(URI)}, which is 307).
     * @return The list of PoolParty projects, in JSON format,
     * as returned by PoolParty.
     * @param mode The mode of resolution. For now, only "current"
     *      is supported, and this is the default. Specified as a query
     *      parameter.
     * @param iri The IRI to be resolved. This is a required query parameter.
     * @param suffix An optional suffix to be appended to the redirected URL.
     *      Specified as a query parameter.
     */
    @Path(ApiPaths.RESOLVE + "/lookupIRI")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    @ApiOperation(value = "Perform a redirect.",
            code = HttpStatus.SC_TEMPORARY_REDIRECT,
            notes = "Because of standard web browser behaviour on receipt "
                    + "of redirects, it is not possible "
                    + "to use this method in the swagger-ui interface. "
                    + "On a successful redirect, you will either see no "
                    + "response, or the browser will follow the redirect.",
            responseHeaders = {
                    @ResponseHeader(name = "Location",
                            description = "URL of the resource",
                            response = URL.class)
            }, response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_BAD_REQUEST,
                    message = "Invalid parameters",
                    response = String.class),
            @ApiResponse(code = HttpStatus.SC_NOT_FOUND,
                    message = "Not found",
                    response = String.class)})
    public final Response lookupIRI(
            @ApiParam(value = "The mode of resolution. For now, only "
                    + "\"current\" is supported, and this is the default. ")
            @QueryParam("mode") @DefaultValue("current") final String mode,
            @ApiParam(value = "The IRI to be resolved.")
            @QueryParam("iri") final String iri,
            @ApiParam(value = "An optional suffix to be appended to the "
                    + "redirected URL.")
            @QueryParam("suffix") final String suffix) {
        logger.debug("called lookupIRI with mode=" + mode
                + ", iri=" + iri
                + ", suffix=" + suffix);
        if (iri == null) {
            return resourceNotFound(NO_IRI_SPECIFIED);
        }
        // Check the optional mode query parameter. For now, only
        // "current" is supported.
        if (mode != null && !"current".equals(mode)) {
            return resourceNotFound(UNSUPPORTED_MODE + ": " + mode);
        }
        // Check the optional suffix query parameter. Need this bit
        // of logic to generate suffixToAdd, rather than simply using
        // suffix itself, since null converted to a string is "null",
        // not the empty string!
        String suffixToAdd = "";
        if (suffix != null) {
            suffixToAdd = suffix;
        }
        List<ResourceMapEntry> resourceMapEntries =
                ResourceMapEntryDAO.
                    getCurrentOwnedResourceMapEntriesForIRI(iri);
        if (resourceMapEntries.size() == 0) {
            return resourceNotFound(NO_DEFINITION + ": " + iri);
        }
        // It could be that there are multiple definitions of the
        // same resource within the same access point. That is
        // probably a mistake in the vocabulary, but we can
        // still resolve the resource.
        // So, look at the first access point in the results, and
        // see if any of the other returned access points are different.
        ResourceMapEntry firstResourceMapEntry = resourceMapEntries.get(0);
        int accessPointId = firstResourceMapEntry.getAccessPointId();
        for (int i = 1; i < resourceMapEntries.size(); i++) {
            if (resourceMapEntries.get(i).getAccessPointId()
                    != accessPointId) {
                // Found a different access point ID.
                return resourceNotFound(MULTIPLE_DEFINITIONS + ": " + iri);
            }
        }
        // If we reached this point, all resource map entries
        // returned have the same access point ID.
        // We just use the first one returned.
        String redirect = getRedirectForResourceMapEntry(
                firstResourceMapEntry) + suffixToAdd;
        try {
            URI redirectURI = new URI(redirect);
            logger.info("Returning redirect to: " + redirect);
            return Response.temporaryRedirect(redirectURI).build();
            // If supporting codes other than 307, check that
            // the code specified is one of 301, 302, 303, 307, and
            // then use something like:
            // return Response.status(code).location(redirectURI).build();
        } catch (URISyntaxException e) {
            logger.error("Unable to create redirection URI", e);
            return resourceNotFound(CAN_NOT_REDIRECT + ": " + redirect);
        }
    }

    /** Create a response representing HTTP status 404 (Not Found).
     * @param message the String that will be sent back in the body
     *      of the response, as plain text.
     * @return The 404 response.
     */
    private Response resourceNotFound(final String message) {
        return Response.status(Status.NOT_FOUND).entity(message)
                .type("text/plain").build();
    }

    /** The resource endpoint of a SISSVoc access point. Insert this
     * between the access point's portal_data's URI value and the resource
     * IRI, to get the final URL to send back as the redirect. */
    private static final String RESOURCE_ENDPOINT = "/resource?uri=";

    /** Get the IRI to which the ResourceMapEntry is to be redirected.
     * This is a URL which points to the SISSVoc resource endpoint
     * for this resource.
     * @param rme The ResourceMapEntry to be redirected
     * @return A String containing the URL to be sent back as the redirect.
     */
    public static String getRedirectForResourceMapEntry(
            final ResourceMapEntry rme) {
        AccessPoint ap = AccessPointDAO.getCurrentAccessPointByAccessPointId(
                rme.getAccessPointId());
        ApSissvoc apSissvoc = JSONSerialization.deserializeStringAsJson(
                ap.getData(), ApSissvoc.class);
        return apSissvoc.getUrlPrefix() + RESOURCE_ENDPOINT + rme.getIri();
    }

}
