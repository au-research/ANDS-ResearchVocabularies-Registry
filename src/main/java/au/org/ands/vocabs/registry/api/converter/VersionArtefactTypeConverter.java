/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.converter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

import au.org.ands.vocabs.registry.enums.VersionArtefactType;

/** Converter for VersionArtefactType values provided to API calls.
 * This is needed, because VersionArtefactType doesn't satisfy
 * the requirements for autoconversion by JAX-RS: it has a fromValue()
 * method, not a fromString() method. So this class provides
 * a converter that implements the latter using the former.
 */
@Provider
public class VersionArtefactTypeConverter implements ParamConverterProvider {

    /** Support conversion between VersionArtefactType and String.
     */
    @Override
    public <T> ParamConverter<T> getConverter(final Class<T> rawType,
            final Type genericType,
            final Annotation[] annotations) {
        if (!rawType.getName().equals(VersionArtefactType.class.getName())) {
            // We only convert instances of VersionArtefactType.
            return null;
        }

        return new ParamConverter<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public T fromString(final String value) {
                try {
                    return (T) (VersionArtefactType.fromValue(value));
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Given value (" + value
                            + ") can't be converted to "
                            + "VersionArtefactType");
                }
            }

            @Override
            public String toString(final T value) {
                return ((VersionArtefactType) value).value();
            }
        };
    }

}
