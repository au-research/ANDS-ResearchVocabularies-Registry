/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.context;

import javax.ws.rs.core.Response;

import au.org.ands.vocabs.registry.api.user.ErrorResult;

/** Utility methods to support generation of JAX-RS Responses. */
public final class ResponseUtils {

    /** Private constructor for a utility class. */
    private ResponseUtils() {
    }

    /** Error message to use when explaining that the user is not
     * authorized to do something, because they are not a superuser. */
    private static final String NOT_SUPERUSER =
            "Not authorized to perform this operation. "
            + "An administrator-level user is required.";

    /** Generate a response with status code {@link Response.Status#FORBIDDEN}
     * and body that explains that the user is not authorized to
     * perform the operation, because they are not a superuser.
     * @return The generated Response.
     */
    public static Response generateForbiddenResponseNotSuperuser() {
        return Response.status(Response.Status.FORBIDDEN).
                entity(new ErrorResult(NOT_SUPERUSER)).build();
    }

    /** Error message to use when explaining that the user is not
     * authorized to add/modify/delete data with the owner they specified. */
    private static final String NOT_AUTHORIZED_FOR_OWNER =
            "Not authorized to add/modify/delete data with that owner";

    /** Generate a response with status code {@link Response.Status#FORBIDDEN}
     * and body that explains that the user is not authorized to
     * add/modify/delete data with the owner they specified.
     * @return The generated Response.
     */
    public static Response generateForbiddenResponseForOwner() {
        return Response.status(Response.Status.FORBIDDEN).
                entity(new ErrorResult(NOT_AUTHORIZED_FOR_OWNER)).build();
    }

    /** Generate a response with status code
     * {@link Response.Status#INTERNAL_SERVER_ERROR}
     * and body that explains something about the error.
     * @param message The error message.
     * @return The generated Response.
     */
    public static Response generateInternalServerError(final String message) {
        return Response.serverError().
                entity(new ErrorResult(message)).build();
    }

}
