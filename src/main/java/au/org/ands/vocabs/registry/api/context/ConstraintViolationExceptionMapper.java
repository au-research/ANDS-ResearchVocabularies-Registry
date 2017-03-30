/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.context;

import java.lang.invoke.MethodHandles;

import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.validation.internal.ValidationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.user.ErrorResult;

/** An {@link ExceptionMapper} that intercepts
 * {@link ConstraintViolationException}s,
 * and returns a response that is an {@link ErrorResult} which includes
 * reasons for the error, e.g., details of a parse error.
 * {@link ConstraintViolationException}s can be generated when validating
 * input data which is either XML or JSON.
 */
@Provider
public class ConstraintViolationExceptionMapper
    implements ExceptionMapper<ConstraintViolationException> {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Return a response that is an {@link ErrorResult} which includes
     * details of the underlying constraint violations.
     */
    @Override
    public Response toResponse(final ConstraintViolationException cve) {
        // Use Jersey's internal helper class to get the status code
        // that would have been returned.
        Status responseStatus = ValidationHelper.getResponseStatus(cve);
        if (responseStatus == Status.INTERNAL_SERVER_ERROR) {
            logger.error("Constraint violation that means a 500 "
                    + "was returned", cve);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                    build();
        }

        // Otherwise, BAD_REQUEST.

        ErrorResult errorResult =
                new ErrorResult("Bad request: Constraint violations");
        // Again, use Jersey's internal helper class. This time, to
        // get a "prettier" version of the violations. We can't
        // directly serialize the set of violations returned by
        // cve.getConstraintViolations(), because
        // ConstraintViolation is an interface, and interfaces can't
        // be serialized by JAXB.
        errorResult.setConstraintViolations(
                ValidationHelper.constraintViolationToValidationErrors(cve));

        return Response.status(Response.Status.BAD_REQUEST).
                entity(errorResult).build();
    }

}
