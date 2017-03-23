/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.auth;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import org.pac4j.jax.rs.features.JaxRsContextFactoryProvider;
import org.pac4j.jax.rs.features.Pac4JSecurityFeature;
import org.pac4j.jax.rs.jersey.features.Pac4JValueFactoryProvider;

/** JAX-RS Feature that registers the pac4j security settings for
 * the application. */
@Provider
public class AuthConfigRegistration implements Feature {

    /** Register the various pac4j providers that we use. */
    @Override
    public boolean configure(final FeatureContext context) {
        // The following works just fine in a standalone Tomcat,
        // but not when we run Arquillian testing in an embededded
        // Tomcat.
//        context.register(new ServletJaxRsContextFactoryProvider());

        // So, for now, use this instead:
        context.register(new JaxRsContextFactoryProvider());
        // We need to switch to using a managed container for
        // Arquillian testing. When we have done that, remove
        // the previous line, and uncomment the line that registers
        // ServletJaxRsContextFactoryProvider.

        context.register(new Pac4JSecurityFeature());
        context.register(new Pac4JValueFactoryProvider.Binder());
        return true;
    }

}
