/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.utils;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.executable.ExecutableValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.validation.CheckRelatedEntity;
import au.org.ands.vocabs.registry.api.validation.CheckVocabulary;
import au.org.ands.vocabs.registry.api.validation.ValidationMode;
import au.org.ands.vocabs.registry.api.validation.ValidationUtils;
import au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntity;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;

/** A utility class with fields of varying types, annotated with
 * validation annotations for the registry schema. In combination with
 * {@link ValidationUtils#getValidator()}, these fields
 * can be used to validate dynamic values.
 * For an example, see how {@link
 * au.org.ands.vocabs.registry.api.validation.CheckRelatedEntityImpl#isValid(au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntity,javax.validation.ConstraintValidatorContext)}
 * validates an email address.
 */
public final class RegistrySchemaValidationHelper {

    /** Logger for this class. */
    private static Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Private constructor for a utility class. A singleton instance
     * is created for use during validation. */
    private RegistrySchemaValidationHelper() {
    }

    /** Singleton instance to pass to validateParameters. */
    private static final RegistrySchemaValidationHelper INSTANCE =
            new RegistrySchemaValidationHelper();

    /** A convenience reference to the executable validator used
     * for validation of parameters. */
    private static final ExecutableValidator EXECUTABLE_VALIDATOR =
            ValidationUtils.getValidator().forExecutables();

    // Vocabularies

    /** A method with a parameter annotated with the {@link CheckVocabulary}
     * validation annotation, with mode CREATE. This method
     * must be marked as public so that it can be found with reflection.
     * However, this method should not be invoked directly.
     * @param newVocabulary The vocabulary to be validated.
     * @return Null.
     */
    public String testNewVocabulary(
            @CheckVocabulary(mode = ValidationMode.CREATE)
            @SuppressWarnings("unused") final Vocabulary newVocabulary) {
        return null;
    }

    /** The name of the {@link testNewVocabulary} method. */
    private static final String NEW_VOCABULARY_METHODNAME = "testNewVocabulary";

    /** Method object corresponding to the {@link testNewVocabulary} method. */
    private static Method newVocabularyMethod;

    static {
        try {
            newVocabularyMethod =
                RegistrySchemaValidationHelper.class.getMethod(
                        NEW_VOCABULARY_METHODNAME, Vocabulary.class);
            logger.info("newVocabularyMethod: " + newVocabularyMethod);
        } catch (NoSuchMethodException | SecurityException e) {
            logger.error("Exception while assigning NEW_VOCABULARY_METHODNAME",
                    e);
        }
    }

    /** A method with a parameter annotated with the {@link CheckVocabulary}
     * validation annotation, with mode UPDATE. This method
     * must be marked as public so that it can be found with reflection.
     * However, this method should not be invoked directly.
     * @param updatedVocabulary The vocabulary to be validated.
     * @return Null.
     */
    public String testUpdatedVocabulary(
            @CheckVocabulary(mode = ValidationMode.UPDATE)
            @SuppressWarnings("unused") final Vocabulary updatedVocabulary) {
        return null;
    }

    /** The name of the {@link testUpdatedVocabulary} method. */
    private static final String UPDATED_VOCABULARY_METHODNAME =
            "testUpdatedVocabulary";

    /** Method object corresponding to the {@link testUpdatedVocabulary}
     * method. */
    private static Method updatedVocabularyMethod;

    static {
        try {
            updatedVocabularyMethod =
                RegistrySchemaValidationHelper.class.getMethod(
                        UPDATED_VOCABULARY_METHODNAME, Vocabulary.class);
        } catch (NoSuchMethodException | SecurityException e) {
            logger.error("Exception while assigning "
                    + "UPDATED_VOCABULARY_METHODNAME", e);
        }
    }

    /** Validate a proposed new vocabulary.
     * @param newVocabulary The vocabulary to be validated.
     * @return The set of constraint violations. (The vocabulary is valid
     *      if this set is empty.)
     */
    public static Set<ConstraintViolation<RegistrySchemaValidationHelper>>
    getNewVocabularyValidation(final Vocabulary newVocabulary) {
        return EXECUTABLE_VALIDATOR.validateParameters(
                INSTANCE, newVocabularyMethod, new Object[] {newVocabulary});
    }

    /** Validate a proposed updated vocabulary.
     * @param updatedVocabulary The vocabulary to be validated.
     * @return The set of constraint violations. (The vocabulary is valid
     *      if this set is empty.)
     */
    public static Set<ConstraintViolation<RegistrySchemaValidationHelper>>
    getUpdatedVocabularyValidation(final Vocabulary updatedVocabulary) {
        return EXECUTABLE_VALIDATOR.validateParameters(
                INSTANCE, updatedVocabularyMethod,
                new Object[] {updatedVocabulary});
    }

    // Related entities

    /** A method with a parameter annotated with the {@link CheckRelatedEntity}
     * validation annotation, with mode CREATE. This method
     * must be marked as public so that it can be found with reflection.
     * However, this method should not be invoked directly.
     * @param newRelatedEntity The related entity to be validated.
     * @return Null.
     */
    public String testNewRelatedEntity(
            @CheckRelatedEntity(mode = ValidationMode.CREATE)
            @SuppressWarnings("unused") final RelatedEntity newRelatedEntity) {
        return null;
    }

    /** The name of the {@link testNewRelatedEntity} method. */
    private static final String NEW_RELATED_ENTITY_METHODNAME =
            "testNewRelatedEntity";

    /** Method object corresponding to the {@link testNewRelatedEntity}
     * method. */
    private static Method newRelatedEntityMethod;

    static {
        try {
            newRelatedEntityMethod =
                RegistrySchemaValidationHelper.class.getMethod(
                        NEW_RELATED_ENTITY_METHODNAME, RelatedEntity.class);
            logger.info("newRelatedEntityMethod: " + newRelatedEntityMethod);
        } catch (NoSuchMethodException | SecurityException e) {
            logger.error("Exception while assigning "
                    + "NEW_RELATED_ENTITY_METHODNAME", e);
        }
    }

    /** A method with a parameter annotated with the {@link CheckRelatedEntity}
     * validation annotation, with mode UPDATE. This method
     * must be marked as public so that it can be found with reflection.
     * However, this method should not be invoked directly.
     * @param updatedRelatedEntity The related entity to be validated.
     * @return Null.
     */
    public String testUpdatedRelatedEntity(
            @CheckRelatedEntity(mode = ValidationMode.UPDATE)
            @SuppressWarnings("unused")
            final RelatedEntity updatedRelatedEntity) {
        return null;
    }

    /** The name of the {@link testUpdatedRelatedEntity} method. */
    private static final String UPDATED_RELATED_ENTITY_METHODNAME =
            "testUpdatedRelatedEntity";

    /** Method object corresponding to the {@link testUpdatedRelatedEntity}
     * method. */
    private static Method updatedRelatedEntityMethod;

    static {
        try {
            updatedRelatedEntityMethod =
                RegistrySchemaValidationHelper.class.getMethod(
                        UPDATED_RELATED_ENTITY_METHODNAME, RelatedEntity.class);
        } catch (NoSuchMethodException | SecurityException e) {
            logger.error("Exception while assigning "
                    + "UPDATED_RELATED_ENTITY_METHODNAME", e);
        }
    }

    /** Validate a proposed new related entity.
     * @param newRelatedEntity The related entity to be validated.
     * @return The set of constraint violations. (The related entity is valid
     *      if this set is empty.)
     */
    public static Set<ConstraintViolation<RegistrySchemaValidationHelper>>
    getNewRelatedEntityValidation(final RelatedEntity newRelatedEntity) {
        return EXECUTABLE_VALIDATOR.validateParameters(
                INSTANCE, newRelatedEntityMethod,
                new Object[] {newRelatedEntity});
    }

    /** Validate a proposed updated related entity.
     * @param updatedRelatedEntity The related entity to be validated.
     * @return The set of constraint violations. (The related entity is valid
     *      if this set is empty.)
     */
    public static Set<ConstraintViolation<RegistrySchemaValidationHelper>>
    getUpdatedRelatedEntityValidation(
            final RelatedEntity updatedRelatedEntity) {
        return EXECUTABLE_VALIDATOR.validateParameters(
                INSTANCE, updatedRelatedEntityMethod,
                new Object[] {updatedRelatedEntity});
    }

}
