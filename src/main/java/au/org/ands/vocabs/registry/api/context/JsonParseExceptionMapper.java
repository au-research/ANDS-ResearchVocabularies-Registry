/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.context;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonParseException;

import au.org.ands.vocabs.registry.api.user.ErrorResult;

/** Handler for {@link JsonParseException}s thrown during unmarshaling
 * of JSON data provided as input to API calls.
 * For the {@link ExceptionMapper} that handles
 * errors with XML input data, see {@link BadRequestExceptionMapper}.
 */
@Provider
public class JsonParseExceptionMapper
    implements ExceptionMapper<JsonParseException> {

    /** Return a response that is an {@link ErrorResult} which includes
     * the generated parse error.
     */
    @Override
    public Response toResponse(final JsonParseException jpe) {
        return Response.status(Response.Status.BAD_REQUEST).
                entity(new ErrorResult("Bad request: "
                        + jpe.toString())).
                build();
    }

}
