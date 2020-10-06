/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.converter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

import au.org.ands.vocabs.registry.enums.RelatedEntityType;

/** Converter for RelatedEntityType values provided to API calls.
 * This is needed, because RelatedEntityType doesn't satisfy
 * the requirements for autoconversion by JAX-RS: it has a fromValue()
 * method, not a fromString() method. So this class provides
 * a converter that implements the latter using the former.
 */
@Provider
public class RelatedEntityTypeConverter implements ParamConverterProvider {

    /** Support conversion between RelatedEntityType and String.
     */
    @Override
    public <T> ParamConverter<T> getConverter(final Class<T> rawType,
            final Type genericType,
            final Annotation[] annotations) {
        if (!rawType.getName().equals(RelatedEntityType.class.getName())) {
            // We only convert instances of RelatedEntityType.
            return null;
        }

        return new ParamConverter<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public T fromString(final String value) {
                try {
                    return (T) (RelatedEntityType.fromValue(value));
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Given value (" + value
                            + ") can't be converted to "
                            + "RelatedEntityType");
                }
            }

            @Override
            public String toString(final T value) {
                return ((RelatedEntityType) value).value();
            }
        };
    }

}
