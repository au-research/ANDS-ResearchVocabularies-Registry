/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.auth;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import org.pac4j.jax.rs.features.Pac4JSecurityFeature;
import org.pac4j.jax.rs.jersey.features.Pac4JValueFactoryProvider;
import org.pac4j.jax.rs.servlet.features.ServletJaxRsContextFactoryProvider;

/** JAX-RS Feature that registers the pac4j security settings for
 * the application. */
@Provider
public class AuthConfigRegistration implements Feature {

    /** Register the various pac4j providers that we use. */
    @Override
    public boolean configure(final FeatureContext context) {
        context.register(new ServletJaxRsContextFactoryProvider());
        context.register(new Pac4JSecurityFeature());
        context.register(new Pac4JValueFactoryProvider.Binder());
        return true;
    }

}
