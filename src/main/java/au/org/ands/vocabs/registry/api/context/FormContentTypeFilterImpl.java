/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.context;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.FormParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import au.org.ands.vocabs.registry.api.user.ErrorResultUtils;

/** JAX-RS container request filter to be applied to methods
 * that require the Content-Type header to be specified as
 * {@link MediaType#APPLICATION_FORM_URLENCODED}.
 * These are methods that have a parameter annotated as {@link FormParam}.
 * To apply this filter to a method, add the {@link FormContentTypeFilter}
 * annotation to it.
 */
@Provider
@FormContentTypeFilter
public class FormContentTypeFilterImpl implements ContainerRequestFilter {

    /** {@inheritDoc} */
    @Override
    public void filter(final ContainerRequestContext requestContext)
            throws IOException {
        // Inspired by:
        // https://stackoverflow.com/questions/49598680/
        //         handling-mismatching-content-type-in-jersey

        MultivaluedMap<String, String> headers = requestContext.getHeaders();
        List<String> contentType = headers.get(HttpHeaders.CONTENT_TYPE);
        // Require that there _be_ a Content-Type header,
        // and that MediaType.APPLICATION_FORM_URLENCODED be present.
        if (contentType == null || !contentType.contains(
                MediaType.APPLICATION_FORM_URLENCODED)) {
            // Use one of our standard ErrorResult responses.
            Response response = ErrorResultUtils.badRequest(
                    "Missing or incorrect Content-Type; it must be "
                    + MediaType.APPLICATION_FORM_URLENCODED
                    + ".");
            requestContext.abortWith(response);
        }
    }

}
