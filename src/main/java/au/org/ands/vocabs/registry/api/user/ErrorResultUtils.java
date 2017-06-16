/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/** Utility methods for creating error results for API calls. */
public final class ErrorResultUtils {

    /** Private constructor for a utility class. */
    private ErrorResultUtils() {
    }

    /** Generate a JAX-RS Response for an internal server error.
     * It will have status code {@link Status#INTERNAL_SERVER_ERROR}.
     * @return A JAX-RS Response representing an internal server error.
     */
    public static Response internalServerError() {
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                new ErrorResult("An internal error occurred.")).build();
    }

}
