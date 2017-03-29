/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.context;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.NoContentException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.UnmarshalException;

import au.org.ands.vocabs.registry.api.user.ErrorResult;

/** An {@link ExceptionMapper} that intercepts {@link BadRequestException}s,
 * and returns a response that is an {@link ErrorResult} which includes
 * reasons for the error, e.g., details of a parse error.
 * {@link BadRequestException}s are generated when parsing XML input data,
 * but not JSON input data. For the {@link ExceptionMapper} that handles
 * errors with JSON input data, see {@link JsonParseExceptionMapper}.
 */
@Provider
public class BadRequestExceptionMapper
    implements ExceptionMapper<BadRequestException> {

    /** Return a response that is an {@link ErrorResult} which includes
     * details of the underlying error (e.g., an error message generated
     * by the XML parser).
     */
    @Override
    public Response toResponse(final BadRequestException bre) {
        Throwable cause = bre.getCause();
        if (cause instanceof NoContentException) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity(new ErrorResult("Missing request data")).
                    build();
        }
        if (cause instanceof UnmarshalException) {
            Throwable secondCause = cause.getCause();
            return Response.status(Response.Status.BAD_REQUEST).
                    entity(new ErrorResult("Unmarshal exception: "
                            + secondCause.toString())).
                    build();
        }
        StringWriter errorStackTrace = new StringWriter();
        bre.printStackTrace(new PrintWriter(errorStackTrace));
        StringWriter causeStackTrace = new StringWriter();
        if (cause != null) {
            cause.printStackTrace(new PrintWriter(causeStackTrace));
        } else {
            causeStackTrace.write("no cause provided");
        }
        return Response.status(Response.Status.BAD_REQUEST).
                entity(new ErrorResult("Bad request: "
                        + errorStackTrace
                        + "; cause: " + causeStackTrace)).
                build();
    }

}
