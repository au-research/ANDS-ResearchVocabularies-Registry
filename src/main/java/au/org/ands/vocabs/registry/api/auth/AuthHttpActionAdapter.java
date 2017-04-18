package au.org.ands.vocabs.registry.api.auth;

import org.apache.http.HttpStatus;
import org.pac4j.core.http.HttpActionAdapter;
import org.pac4j.jax.rs.filters.JaxRsHttpActionAdapter;
import org.pac4j.jax.rs.pac4j.JaxRsConfig;
import org.pac4j.jax.rs.pac4j.JaxRsContext;

import au.org.ands.vocabs.registry.api.user.ErrorResult;

/** An {@link HttpActionAdapter} based on {@link JaxRsHttpActionAdapter},
 * but which returns an entity if authentication fails.
 * Register {@link INSTANCE} using {@link JaxRsConfig#setHttpActionAdapter}.
 */
public class AuthHttpActionAdapter
    implements HttpActionAdapter<Object, JaxRsContext> {

    /** Singleton instance of this class. */
    public static final AuthHttpActionAdapter INSTANCE =
            new AuthHttpActionAdapter();

    /** For an authentication failure, insert an error response. */
    @Override
    public Object adapt(final int code, final JaxRsContext context) {
        if (code == HttpStatus.SC_UNAUTHORIZED) {
            context.getRequestContext().abortWith(
                    context.getAbortBuilder().
                    entity(new ErrorResult("Not authenticated")).
//                    header(HttpConstants.AUTHENTICATE_HEADER,
//                            "Basic realm=\"My realm2\"").
                    build());
        } else
            if (code == HttpStatus.SC_FORBIDDEN) {
            context.getRequestContext().abortWith(
                    context.getAbortBuilder().
                    entity(new ErrorResult("Not authorized")).
                    build());
        } else {
            context.getRequestContext().abortWith(
                    context.getAbortBuilder().build());
        }
        return null;
    }

}
