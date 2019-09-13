/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user.subscription;

import java.lang.invoke.MethodHandles;
import java.util.List;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
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

import au.org.ands.vocabs.registry.api.auth.AuthConstants;
import au.org.ands.vocabs.registry.api.auth.SubscriberAuthenticator;
import au.org.ands.vocabs.registry.api.context.ApiPaths;
import au.org.ands.vocabs.registry.api.context.SwaggerInterface;
import au.org.ands.vocabs.registry.api.user.ErrorResult;
import au.org.ands.vocabs.registry.api.user.ErrorResultUtils;
import au.org.ands.vocabs.registry.db.context.DBContext;
import au.org.ands.vocabs.registry.db.converter.SubscriptionDbSchemaMapper;
import au.org.ands.vocabs.registry.db.dao.SubscriptionDAO;
import au.org.ands.vocabs.registry.log.Analytics;
import au.org.ands.vocabs.registry.log.Logging;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Subscription;
import au.org.ands.vocabs.registry.schema.vocabulary201701.SubscriptionList;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;

/** REST web services for getting subscriptions. */
@Path(ApiPaths.API_SERVICES + "/" + ApiPaths.SUBSCRIPTIONS)
@Api(value = SwaggerInterface.TAG_SERVICES)
public class GetSubscriptions {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Get details of the email subscriptions for a subscriber.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile.
     * @param token The subscriber's token.
     * @return An empty response for success, or an error response. */
    @Path(ApiPaths.EMAIL)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Pac4JSecurity(clients = AuthConstants.SUBSCRIBER_PARAMETER_CLIENT)
    @GET
    @ApiOperation(value = "Get the email subscriptions for a subscriber.",
        response = SubscriptionList.class)
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED,
                    message = "Not authenticated",
                    response = ErrorResult.class,
                    responseHeaders = {
                            @ResponseHeader(name = "WWW-Authenticate",
                                    response = String.class)
                            }),
            @ApiResponse(code = HttpStatus.SC_BAD_REQUEST,
                    message = "Invalid input",
                    response = ErrorResult.class),
            @ApiResponse(code = HttpStatus.SC_FORBIDDEN,
                    message = "Not authenticated, or not authorized",
                    response = ErrorResult.class),
            @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR,
                    message = "Internal server error",
                    response = ErrorResult.class)
            })
    public Response getEmailSubscriptions(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(hidden = true) @Pac4JProfile final CommonProfile profile,
            @ApiParam(value = "Subscriber authentication token")
            @NotNull(message =
                    "The subscriber authentication token must be specified.")
            @QueryParam(SubscriberAuthenticator.TOKEN)
            @SuppressWarnings("unused") final String token) {
        logger.debug("called getEmailSubscriptions");
        logger.debug("profile: username: " + profile.getUsername()
                + "; email: " + profile.getEmail());

        Integer subscriberId = Integer.valueOf(profile.getId());

        EntityManager em = null;

        try {
            em = DBContext.getEntityManager();

            List<au.org.ands.vocabs.registry.db.entity.Subscription>
            dbSubscriptions = SubscriptionDAO.
                    getCurrentSubscriptionsForSubscriber(subscriberId, em);
            SubscriptionList subscriptionList = new SubscriptionList();
            List<Subscription> outputSubscriptions =
                    subscriptionList.getSubscription();
            SubscriptionDbSchemaMapper subscriptionMapper =
                    SubscriptionDbSchemaMapper.INSTANCE;

            for (au.org.ands.vocabs.registry.db.entity.Subscription
                    dbSubscription: dbSubscriptions) {
                outputSubscriptions.add(subscriptionMapper.sourceToTarget(
                        dbSubscription, em));
            }

            Logging.logRequest(true, request, uriInfo, profile,
                    Analytics.EVENT_GET_SUBSCRIPTION,
                    Analytics.SUBSCRIBER_ID_FIELD, subscriberId,
                    Analytics.SUBSCRIBER_EMAIL_FIELD, profile.getEmail());
            return Response.ok().entity(subscriptionList).build();
        } catch (Throwable t) {
            logger.error("Exception while fetching subcriptions", t);
            // Don't throw, but fall through so that the user sees
            // an error message.
//            throw t;
        } finally {
            if (em != null) {
                em.close();
            }
        }
        // Oh dear, we fell through. There was an exception we don't
        // know about, or, at least, can't provide useful feeback about.
        return ErrorResultUtils.internalServerError();
    }

}
