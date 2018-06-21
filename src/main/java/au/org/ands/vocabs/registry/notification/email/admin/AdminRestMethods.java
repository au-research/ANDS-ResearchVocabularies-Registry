/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.notification.email.admin;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
import au.org.ands.vocabs.registry.api.context.ResponseUtils;
import au.org.ands.vocabs.registry.api.context.SwaggerInterface;
import au.org.ands.vocabs.registry.api.user.ErrorResult;
import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.log.Logging;
import au.org.ands.vocabs.registry.notification.email.SendEmailNotifications;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.ResponseHeader;

/** Email notification administration tools available through a REST-like
 * interface. */
@Path(AdminApiPaths.API_ADMIN + "/" + AdminApiPaths.NOTIFICATION)
@Api(value = SwaggerInterface.TAG_ADMIN,
        authorizations = {@Authorization(value = SwaggerInterface.BASIC_AUTH),
        @Authorization(value = SwaggerInterface.API_KEY_AUTH)})
public class AdminRestMethods {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Generate email notifications.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile.
     * @param startDate The start date of registry events to consider.
     *      If not specified, defaults to a week before the current
     *      date/time, and endDate is taken as the current date/time
     *      (irrespective of any value given for endDate).
     * @param endDate The end date of registry events to consider.
     *      If not specified, defaults to the current date/time.
     * @return The task.
     */
    @Path(AdminApiPaths.EMAIL)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Pac4JSecurity
    @GET
    @ApiOperation(value = "Generate email notifications.",
            notes = "This method is only available to administrator users.",
            response = String.class)
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
    public final Response generateNotificationEmails(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(hidden = true) @Pac4JProfile
            final CommonProfile profile,
            @ApiParam(value = "Start date/time of registry events to consider. "
                    + "Values are specified in UTC. "
                    + "If not specified, defaults to a week before the "
                    + "current date/time, and endDate is taken as the "
                    + "current date/time (irrespective of any value "
                    + "given for endDate)")
            @QueryParam("startDate") final String startDate,
            @ApiParam(value = "End date/time of registry events to consider. "
                    + "Values are specified in UTC. "
                    + "If not specified, defaults to the current date/time.")
            @QueryParam("endDate") final String endDate) {
        logger.info("Called generateNotificationEmails");
        if (!AuthUtils.profileIsSuperuser(profile)) {
            Logging.logRequest(false, request, uriInfo, profile,
                    "Send email notifications");
            return ResponseUtils.generateForbiddenResponseNotSuperuser();
        }

        LocalDateTime startDateValue;
        LocalDateTime endDateValue;
        try {
            if (startDate != null) {
                if (endDate != null) {
                    // Both startDate and endDate specified.
                    startDateValue = LocalDateTime.parse(startDate);
                    endDateValue = LocalDateTime.parse(endDate);
                } else {
                    // Only startDate specified.
                    startDateValue = LocalDateTime.parse(startDate);
                    endDateValue = TemporalUtils.nowUTC();
                }
            } else {
                // startDate wasn't specified, so don't pass any arguments,
                // even if endDate _was_ specified.
                endDateValue = TemporalUtils.nowUTC();
                startDateValue = endDateValue.minusWeeks(1);
            }
        } catch (DateTimeParseException e) {
            logger.error("Error while parsing a date", e);
            Logging.logRequest(false, request, uriInfo, profile,
                    "Send email notifications");
            return ResponseUtils.generateInternalServerError(
                    "Error during parsing of either startDate or endDate");
        }

        SendEmailNotifications notifier = new SendEmailNotifications();
        try {
            notifier.main(startDateValue, endDateValue);
        } catch (IOException e) {
            Logging.logRequest(false, request, uriInfo, profile,
                    "Send email notifications");
            logger.error("Returning an error, because there was an exception",
                    e);
            return ResponseUtils.generateInternalServerError(
                    "Error during processing");
        }

        Logging.logRequest(true, request, uriInfo, profile,
                "Send email notifications");

        return Response.ok("OK").build();
    }

}
