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
}
