/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ws.rs.FormParam;
import javax.ws.rs.NameBinding;
import javax.ws.rs.core.MediaType;

/** JAX-RS name binding annotation for a filter to be used on methods
 * that require the Content-Type header to be specified as
 * {@link MediaType#APPLICATION_FORM_URLENCODED}.
 * These are methods that have a parameter annotated as {@link FormParam}.
 * See {@link FormContentTypeFilterImpl} for the implementation.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@NameBinding
public @interface FormContentTypeFilter {
}
