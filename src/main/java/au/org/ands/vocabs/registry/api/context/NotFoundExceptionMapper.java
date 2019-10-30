/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.context;

import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import au.org.ands.vocabs.registry.api.user.ErrorResult;

/** Handler for {@link NotFoundException}s.
 */
@Priority(Priorities.ENTITY_CODER)
@Provider
public class NotFoundExceptionMapper
    implements ExceptionMapper<NotFoundException> {

    /** The message to be included in "not found" responses. */
    public static final String NOT_FOUND = "Not found";

    /** The message to be included in "not found" responses, in HTML format.  */
    public static final String NOT_FOUND_HTML =
            "<html><body>" + NOT_FOUND + "</body></html>";

    /** The headers of the original HTTP request, injected by JAX-RS. */
    @Context private HttpHeaders headers;

    /** Return a response for missing content, i.e., for a "404".
     * The response is an {@link ErrorResult}, if the client
     * requested one of our known formats (i.e., JSON and XML).
     * If the client requested HTML, then an HTML response is provided;
     * otherwise, plain text is returned.
     */
    @Override
    public Response toResponse(final NotFoundException nfe) {
        ResponseBuilder rb = Response.status(Response.Status.NOT_FOUND);

        MediaType mediaType = null;
        // The following is inspired by one of the answers at:
        // https://stackoverflow.com/questions/3227360/
        //         jax-rs-jersey-custom-exception-with-xml-or-json
        List<MediaType> acceptableMediaTypes =
                headers.getAcceptableMediaTypes();
        if (acceptableMediaTypes != null && acceptableMediaTypes.size() > 0) {
            for (MediaType acceptableMediaType : acceptableMediaTypes) {
                if (acceptableMediaType.equals(
                        MediaType.APPLICATION_JSON_TYPE)
                        || acceptableMediaType.equals(
                                MediaType.APPLICATION_XML_TYPE)) {
                    rb.entity(new ErrorResult(NOT_FOUND));
                    mediaType = acceptableMediaType;
                    break;
                } else if (acceptableMediaType.equals(
                        MediaType.TEXT_HTML_TYPE)) {
                    rb.entity(NOT_FOUND_HTML);
                    mediaType = acceptableMediaType;
                }
            }
        }
        if (mediaType != null) {
            rb = rb.type(mediaType);
        } else {
            rb.entity(NOT_FOUND);
            rb.type(MediaType.TEXT_PLAIN_TYPE);
        }
        return rb.build();
    }

}
