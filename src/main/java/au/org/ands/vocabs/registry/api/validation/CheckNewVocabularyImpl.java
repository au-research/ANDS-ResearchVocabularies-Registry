/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.validation;

import java.lang.invoke.MethodHandles;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;

/** Validation of input data provided for vocabulary creation.
 */
public class CheckNewVocabularyImpl
    implements ConstraintValidator<CheckNewVocabulary, Vocabulary> {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    @Override
    public void initialize(final CheckNewVocabulary cnv) {
        // No initialization required.
    }

    /** Validate a proposed new vocabulary.
     * @param newVocabulary The new vocabulary that is being created.
     * @return true, if newVocabulary represents a valid vocabulary.
     */
    @Override
    public boolean isValid(final Vocabulary newVocabulary,
            final ConstraintValidatorContext constraintContext) {

        boolean valid = true;
        logger.info("In CheckNewVocabularyImpl.isValid()");

        // id: required _not_ to be provided
        if (newVocabulary.getId() != 0) {
            /* Can't specify an id, for a new vocab.
             * Note: we can't distinguish omitting an id,
             * from specifying an id of 0. */
            logger.info("id is not 0");
            valid = false;
            constraintContext.disableDefaultConstraintViolation();
            constraintContext.buildConstraintViolationWithTemplate(
                    "{" + CheckNewVocabulary.INTERFACE_NAME + ".id}").
            addPropertyNode("id").
            addConstraintViolation();
        }


        // status: required
        valid = requireFieldNotNull(CheckNewVocabulary.INTERFACE_NAME,
                newVocabulary.getStatus(), "status",
                constraintContext, valid);
        /*
        if (newVocabulary.getStatus() == null) {
            logger.info("status is null");
            valid = false;
            constraintContext.disableDefaultConstraintViolation();
            constraintContext.buildConstraintViolationWithTemplate(
                    "{au.org.ands.vocabs.registry.api.validation."
                    + "CheckNewVocabulary.status}").
            addConstraintViolation();

        }
        */

        // owner: required
        // NB: we can't do authorization checks here.

        // slug: optional, but if specified, must not already exist

        // title: required

        // acronym: optional

        // description: required
        valid = requireFieldNotEmptyString(CheckNewVocabulary.INTERFACE_NAME,
                newVocabulary.getDescription(), "description",
                constraintContext, valid);

        // note: optional

        // subject: at least from ANZSRC-FOR required

        // primaryLanguage: required

        // otherLanguages: optional, and must come from the list

        // licence: optional

        // poolpartyProject: optional

        // topConcept: required

        // creationDate: required

        // revisionCycle: optional

        // relatedEntityRef

        // what else?

        return valid;
    }

    /** Check that a field of a bean is not null. If it is in fact
     * null, register a constraint violation.
     * @param constraintInterfaceName The name of the interface
     *      for the constraint.
     * @param objectToTest The object that is required to be not null.
     * @param fieldName The name of the field that is being tested.
     * @param constraintContext The constraint context. If there is a
     *      violation, it is recorded here.
     * @param valid The state of validity, up to this point.
     * @return The updated validity state.
    */
    private boolean requireFieldNotNull(final String constraintInterfaceName,
            final Object objectToTest,
            final String fieldName,
            final ConstraintValidatorContext constraintContext,
            final boolean valid) {
        boolean validToReturn = valid;
        if (objectToTest == null) {
            validToReturn = false;
            constraintContext.disableDefaultConstraintViolation();
            constraintContext.buildConstraintViolationWithTemplate(
                    "{" + constraintInterfaceName + "." + fieldName + "}").
            addPropertyNode(fieldName).
            addConstraintViolation();
        }
        return validToReturn;
    }

    /** Check that a field of a bean is not an empty string.
     * If it is in fact an empty string, register a constraint violation.
     * @param constraintInterfaceName The name of the interface
     *      for the constraint.
     * @param stringToTest The object that is required to be not null.
     * @param fieldName The name of the field that is being tested.
     * @param constraintContext The constraint context. If there is a
     *      violation, it is recorded here.
     * @param valid The state of validity, up to this point.
     * @return The updated validity state.
    */
    private boolean requireFieldNotEmptyString(
            final String constraintInterfaceName,
            final String stringToTest,
            final String fieldName,
            final ConstraintValidatorContext constraintContext,
            final boolean valid) {
        boolean validToReturn = valid;
        if (stringToTest == null || stringToTest.isEmpty()) {
            validToReturn = false;
            constraintContext.disableDefaultConstraintViolation();
            constraintContext.buildConstraintViolationWithTemplate(
                    "{" + constraintInterfaceName + "." + fieldName + "}").
            addPropertyNode(fieldName).
            addConstraintViolation();
        }
        return validToReturn;
    }

}
