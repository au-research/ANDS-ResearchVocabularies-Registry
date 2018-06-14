/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user.subscription;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.auth.SubscriberAuthenticator;
import au.org.ands.vocabs.registry.api.context.ApiPaths;
import au.org.ands.vocabs.registry.api.context.SwaggerInterface;
import au.org.ands.vocabs.registry.api.user.ErrorResult;
import au.org.ands.vocabs.registry.api.user.ErrorResultUtils;
import au.org.ands.vocabs.registry.api.validation.FieldValidationHelper;
import au.org.ands.vocabs.registry.api.validation.ValidationUtils;
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

/** REST web services for adding and updating subscriptions. */
@Path(ApiPaths.API_SERVICES + "/" + ApiPaths.SUBSCRIPTIONS)
@Api(value = SwaggerInterface.TAG_SERVICES)
public class PutSubscriptions {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Create an instance of an email subscription for a vocabulary.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param email The subscriber's email address.
     * @param vocabularyId The VocabularyId of the vocabulary to be
     *      subscribed to.
     * @return An empty response for success, or an error response. */
    @Path(ApiPaths.EMAIL + "/"
            + ApiPaths.VOCABULARIES + "/" + ApiPaths.VOCABULARY_ID)
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    // NB: we _don't_ require authentication for this method!
//    @Pac4JSecurity(clients = AuthConstants.SUBSCRIBER_PARAMETER_CLIENT)
    @POST
    @ApiOperation(value = "Create an email subscription for a vocabulary.",
        code = HttpStatus.SC_CREATED)
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
    public final Response createEmailSubscriptionVocabulary(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(value = "The ID of the vocabulary to be "
                    + "subscribed to.")
            @PathParam("vocabularyId") final Integer vocabularyId,
            @ApiParam(value = "Subscriber email address")
            @NotNull(message =
                    "The subscriber email address must be specified.")
            @QueryParam(SubscriberAuthenticator.SUBSCRIBER_EMAIL_ADDRESS)
            final String email) {
        logger.debug("called createEmailSubscriptionVocabulary");
        logger.debug("email:  " + email);

        if (StringUtils.isBlank(email)) {
            return ErrorResultUtils.badRequest("Blank email address");
        }
        Set<ConstraintViolation<FieldValidationHelper>> emailViolations =
                ValidationUtils.getValidator().
                validateValue(FieldValidationHelper.class,
                FieldValidationHelper.EMAIL_FIELDNAME, email);
        if (!emailViolations.isEmpty()) {
            return ErrorResultUtils.badRequest("Invalid email address");
        }

        EntityManager em = null;
        EntityTransaction txn = null;

        try {
            em = DBContext.getEntityManager();
            txn = em.getTransaction();
            txn.begin();

            // Timestamp to use for start/end date values corresponding
            // to this event.
            LocalDateTime now = TemporalUtils.nowUTC();

            SubscriptionUtils.createEmailSubscriptionVocabulary(
                    email, vocabularyId, em, now);
            txn.commit();
            Logging.logRequest(true, request, uriInfo, null,
                    "Create email subscription for a vocabulary");
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

    /** Create an instance of an email subscription for an owner.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param email The subscriber's email address.
     * @param owner The owner to subscribe to.
     * @return An empty response for success, or an error response. */
    @Path(ApiPaths.EMAIL + "/"
            + ApiPaths.OWNER + "/" + ApiPaths.OWNER_NAME)
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    // NB: we _don't_ require authentication for this method!
//    @Pac4JSecurity(clients = AuthConstants.SUBSCRIBER_PARAMETER_CLIENT)
    @POST
    @ApiOperation(value = "Create an email subscription for an owner.",
        code = HttpStatus.SC_CREATED)
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
    public final Response createEmailSubscriptionOwner(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(value = "Owner to subscribe to. Specify "
                    + Owners.ALL_OWNERS
                    + " to subscribe to notifications for all owners.")
            @PathParam("owner") final String owner,
            @ApiParam(value = "Subscriber email address")
            @NotNull(message =
                    "The subscriber email address must be specified.")
            @QueryParam(SubscriberAuthenticator.SUBSCRIBER_EMAIL_ADDRESS)
            final String email) {
        logger.debug("called createEmailSubscriptionOwner");
        logger.debug("email: " + email);

        if (StringUtils.isBlank(email)) {
            return ErrorResultUtils.badRequest("Blank email address");
        }
        Set<ConstraintViolation<FieldValidationHelper>> emailViolations =
                ValidationUtils.getValidator().
                validateValue(FieldValidationHelper.class,
                FieldValidationHelper.EMAIL_FIELDNAME, email);
        if (!emailViolations.isEmpty()) {
            return ErrorResultUtils.badRequest("Invalid email address");
        }

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

            SubscriptionUtils.createEmailSubscriptionOwner(
                    email, owner, em, now);
            txn.commit();
            Logging.logRequest(true, request, uriInfo, null,
                    "Create email subscription for an owner");
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

    /** Create an instance of an email subscription for the system.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param email The subscriber's email address.
     * @return An empty response for success, or an error response. */
    @Path(ApiPaths.EMAIL + "/" + ApiPaths.SYSTEM)
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    // NB: we _don't_ require authentication for this method!
//    @Pac4JSecurity(clients = AuthConstants.SUBSCRIBER_PARAMETER_CLIENT)
    @POST
    @ApiOperation(value = "Create an email subscription for the system.",
        code = HttpStatus.SC_CREATED)
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
    public final Response createEmailSubscriptionSystem(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @NotNull(message =
                    "The subscriber email address must be specified.")
            @QueryParam(SubscriberAuthenticator.SUBSCRIBER_EMAIL_ADDRESS)
            final String email) {
        logger.debug("called createEmailSubscriptionSystem");
        logger.debug("email: " + email);

        if (StringUtils.isBlank(email)) {
            return ErrorResultUtils.badRequest("Blank email address");
        }
        Set<ConstraintViolation<FieldValidationHelper>> emailViolations =
                ValidationUtils.getValidator().
                validateValue(FieldValidationHelper.class,
                FieldValidationHelper.EMAIL_FIELDNAME, email);
        if (!emailViolations.isEmpty()) {
            return ErrorResultUtils.badRequest("Invalid email address");
        }

        EntityManager em = null;
        EntityTransaction txn = null;

        try {
            em = DBContext.getEntityManager();
            txn = em.getTransaction();
            txn.begin();

            // Timestamp to use for start/end date values corresponding
            // to this event.
            LocalDateTime now = TemporalUtils.nowUTC();

            SubscriptionUtils.createEmailSubscriptionSystem(email, em, now);
            txn.commit();
            Logging.logRequest(true, request, uriInfo, null,
                    "Create email subscription for the system");
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
