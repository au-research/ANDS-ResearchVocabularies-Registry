/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user.subscription;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.log.Logging;
import au.org.ands.vocabs.registry.subscription.Owners;
import au.org.ands.vocabs.registry.subscription.SubscriptionUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;

/** REST web services for deleting subscriptions. */
@Path(ApiPaths.API_SERVICES + "/" + ApiPaths.SUBSCRIPTIONS)
@Api(value = SwaggerInterface.TAG_SERVICES)
public class DeleteSubscriptions {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Delete an instance of an email subscription for a vocabulary.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile.
     * @param token The subscriber's authentication token.
     * @param vocabularyId The VocabularyId of the vocabulary to be
     *      unsubscribed from.
     * @return An empty response for success, or an error response. */
    @Path(ApiPaths.EMAIL + "/"
            + ApiPaths.VOCABULARIES + "/" + ApiPaths.VOCABULARY_ID)
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Pac4JSecurity(clients = AuthConstants.SUBSCRIBER_PARAMETER_CLIENT)
    @DELETE
    @ApiOperation(value = "Delete an email subscription for a vocabulary.")
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_NO_CONTENT,
                    message = "Delete successful"),
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
    public final Response deleteEmailSubscriptionVocabulary(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(hidden = true) @Pac4JProfile final CommonProfile profile,
            @ApiParam(value = "Subscriber authentication token")
            @NotNull(message =
                    "The subscriber authentication token must be specified.")
            @QueryParam(SubscriberAuthenticator.TOKEN)
            @SuppressWarnings("unused") final String token,
            @ApiParam(value = "The ID of the vocabulary to be "
                    + "unsubscribed from.")
            @QueryParam("vocabularyId") final Integer vocabularyId) {
        logger.debug("called deleteEmailSubscriptionVocabulary");
        logger.debug("profile: username: " + profile.getUsername()
                + "; email: " + profile.getEmail());

        Integer subscriberId = Integer.valueOf(profile.getId());

        EntityManager em = null;
        EntityTransaction txn = null;

        try {
            em = DBContext.getEntityManager();
            txn = em.getTransaction();
            txn.begin();

            // Timestamp to use for start/end date values corresponding
            // to this event.
            LocalDateTime now = TemporalUtils.nowUTC();

            SubscriptionUtils.deleteEmailSubscriptionVocabulary(
                    subscriberId, vocabularyId, em, now);
            txn.commit();
            Logging.logRequest(true, request, uriInfo, profile,
                    "Delete email subscription for a vocabulary");
            // Successful deletion, and no response body. noContent() creates
            // status code 204.
            return Response.noContent().build();
        } catch (Throwable t) {
            if (txn != null && txn.isActive()) {
                try {
                    logger.error("Exception during transaction; rolling back",
                            t);
                    txn.rollback();
                } catch (Exception e) {
                    logger.error("Rollback failure!", e);
                }
            } else {
                logger.error("Exception, either during rollback, or "
                        + "outside active transaction", t);
            }
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

    /** Delete an instance of an email subscription for an owner.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile.
     * @param token The subscriber's authentication token.
     * @param owner The owner to unsubscribe from.
     * @return An empty response for success, or an error response. */
    @Path(ApiPaths.EMAIL + "/"
            + ApiPaths.OWNER + "/" + ApiPaths.OWNER_ID)
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Pac4JSecurity(clients = AuthConstants.SUBSCRIBER_PARAMETER_CLIENT)
    @DELETE
    @ApiOperation(value = "Delete an email subscription for an owner.")
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_NO_CONTENT,
                    message = "Delete successful"),
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
    public final Response deleteEmailSubscriptionOwner(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(hidden = true) @Pac4JProfile final CommonProfile profile,
            @ApiParam(value = "Subscriber authentication token")
            @NotNull(message =
                    "The subscriber authentication token must be specified.")
            @QueryParam(SubscriberAuthenticator.TOKEN)
            @SuppressWarnings("unused") final String token,
            @ApiParam(value = "Owner to unsubscribe from")
            @QueryParam("owner") final String owner) {
        logger.debug("called deleteEmailSubscriptionOwner");
        logger.debug("profile: username: " + profile.getUsername()
                + "; email: " + profile.getEmail());

        Integer subscriberId = Integer.valueOf(profile.getId());

        EntityManager em = null;
        EntityTransaction txn = null;

        try {
            Owners.getOwnerId(owner);
        } catch (IllegalArgumentException e) {
            ErrorResultUtils.badRequest("No such owner");
        }

        try {
            em = DBContext.getEntityManager();
            txn = em.getTransaction();
            txn.begin();

            // Timestamp to use for start/end date values corresponding
            // to this event.
            LocalDateTime now = TemporalUtils.nowUTC();

            SubscriptionUtils.deleteEmailSubscriptionOwner(
                    subscriberId, owner, em, now);
            txn.commit();
            Logging.logRequest(true, request, uriInfo, profile,
                    "Delete email subscription for an owner");
            // Successful deletion, and no response body. noContent() creates
            // status code 204.
            return Response.noContent().build();
        } catch (Throwable t) {
            if (txn != null && txn.isActive()) {
                try {
                    logger.error("Exception during transaction; rolling back",
                            t);
                    txn.rollback();
                } catch (Exception e) {
                    logger.error("Rollback failure!", e);
                }
            } else {
                logger.error("Exception, either during rollback, or "
                        + "outside active transaction", t);
            }
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

    /** Delete an instance of an email subscription for the system.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile.
     * @param token The subscriber's authentication token.
     * @return An empty response for success, or an error response. */
    @Path(ApiPaths.EMAIL + "/" + ApiPaths.SYSTEM)
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Pac4JSecurity(clients = AuthConstants.SUBSCRIBER_PARAMETER_CLIENT)
    @DELETE
    @ApiOperation(value = "Delete an email subscription for the system.")
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_NO_CONTENT,
                    message = "Delete successful"),
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
    public final Response deleteEmailSubscriptionSystem(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(hidden = true) @Pac4JProfile final CommonProfile profile,
            @ApiParam(value = "Subscriber authentication token")
            @NotNull(message =
                    "The subscriber authentication token must be specified.")
            @QueryParam(SubscriberAuthenticator.TOKEN)
            @SuppressWarnings("unused") final String token) {
        logger.debug("called deleteEmailSubscriptionSystem");
        logger.debug("profile: username: " + profile.getUsername()
                + "; email: " + profile.getEmail());

        Integer subscriberId = Integer.valueOf(profile.getId());

        EntityManager em = null;
        EntityTransaction txn = null;

        try {
            em = DBContext.getEntityManager();
            txn = em.getTransaction();
            txn.begin();

            // Timestamp to use for start/end date values corresponding
            // to this event.
            LocalDateTime now = TemporalUtils.nowUTC();

            SubscriptionUtils.deleteEmailSubscriptionSystem(
                    subscriberId, em, now);
            txn.commit();
            Logging.logRequest(true, request, uriInfo, profile,
                    "Delete email subscription for the system");
            // Successful deletion, and no response body. noContent() creates
            // status code 204.
            return Response.noContent().build();
        } catch (Throwable t) {
            if (txn != null && txn.isActive()) {
                try {
                    logger.error("Exception during transaction; rolling back",
                            t);
                    txn.rollback();
                } catch (Exception e) {
                    logger.error("Rollback failure!", e);
                }
            } else {
                logger.error("Exception, either during rollback, or "
                        + "outside active transaction", t);
            }
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

    /** Delete all email subscriptions for a subscriber.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile.
     * @param token The subscriber's authentication token.
     * @return An empty response for success, or an error response. */
    @Path(ApiPaths.EMAIL + "/" + ApiPaths.ALL)
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Pac4JSecurity(clients = AuthConstants.SUBSCRIBER_PARAMETER_CLIENT)
    @DELETE
    @ApiOperation(value = "Delete all email subscriptions for a subscriber.")
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_NO_CONTENT,
                    message = "Delete successful"),
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
    public final Response deleteEmailSubscriptionAll(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(hidden = true) @Pac4JProfile final CommonProfile profile,
            @ApiParam(value = "Subscriber authentication token")
            @NotNull(message =
                    "The subscriber authentication token must be specified.")
            @QueryParam(SubscriberAuthenticator.TOKEN)
            @SuppressWarnings("unused") final String token) {
        logger.debug("called deleteEmailSubscriptionAll");
        logger.debug("profile: username: " + profile.getUsername()
                + "; email: " + profile.getEmail());

        Integer subscriberId = Integer.valueOf(profile.getId());

        EntityManager em = null;
        EntityTransaction txn = null;

        try {
            em = DBContext.getEntityManager();
            txn = em.getTransaction();
            txn.begin();

            // Timestamp to use for start/end date values corresponding
            // to this event.
            LocalDateTime now = TemporalUtils.nowUTC();

            SubscriptionUtils.deleteEmailSubscriptionAll(subscriberId, em, now);
            txn.commit();
            Logging.logRequest(true, request, uriInfo, profile,
                    "Delete all email subscriptions for a subscriber");
            // Successful deletion, and no response body. noContent() creates
            // status code 204.
            return Response.noContent().build();
        } catch (Throwable t) {
            if (txn != null && txn.isActive()) {
                try {
                    logger.error("Exception during transaction; rolling back",
                            t);
                    txn.rollback();
                } catch (Exception e) {
                    logger.error("Rollback failure!", e);
                }
            } else {
                logger.error("Exception, either during rollback, or "
                        + "outside active transaction", t);
            }
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
