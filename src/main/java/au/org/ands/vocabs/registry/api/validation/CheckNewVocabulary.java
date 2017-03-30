/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/** Annotation for adding validation of Registry Schema Vocabulary
 * instances provided as input to API methods. */
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CheckNewVocabularyImpl.class)
@Documented
public @interface CheckNewVocabulary {

    /** The fully-qualified name of this interface. */
    String INTERFACE_NAME =
            "au.org.ands.vocabs.registry.api.validation.CheckNewVocabulary";

    /** Get the default message template for violations.
     * @return The default message template. */
    String message() default
        "{" + INTERFACE_NAME + "}";

    /** Get the groups used for violation checking.
     * @return The groups used for violation checking.  */
    Class<?>[] groups() default { };

    /** Get the violation payloads.
     * @return The violation payloads. */
    Class<? extends Payload>[] payload() default { };

}
