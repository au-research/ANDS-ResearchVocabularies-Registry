/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.validation;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder;

/** Utility methods to support validation. */
public final class ValidationUtils {

    /** Private constructor for a utility class. */
    private ValidationUtils() {
    }

    /** The length of a String that is a date in "YYYY" format. */
    private static final int YYYY_LENGTH = 4;

    /** The length of a String that is a date in "YYYY-MM" format. */
    private static final int YYYY_MM_LENGTH = 7;

    /** The length of a String that is a date in "YYYY-MM-DD" format. */
    private static final int YYYY_MM_DD_LENGTH = 10;

    /** DateTimeFormatter that represents a value containing a year,
     * month, and day. */
    private static final DateTimeFormatter YYYY_MM_DD =
            DateTimeFormatter.ISO_LOCAL_DATE;
    /* Reuse ISO_LOCAL_DATE, which is already set to STRICT resolution. */

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
    public static boolean requireFieldNotNull(
            final String constraintInterfaceName,
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
     * @param stringToTest The String that is required to be not null.
     * @param fieldName The name of the field that is being tested.
     * @param constraintContext The constraint context. If there is a
     *      violation, it is recorded here.
     * @param valid The state of validity, up to this point.
     * @return The updated validity state.
    */
    public static boolean requireFieldNotEmptyString(
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

    /** Check that a field of a bean is not an empty string, and
     * also satisfies a property specified as a predicate.
     * If it is in fact an empty string, or does not satisfy the
     * property, register a constraint violation.
     * @param constraintInterfaceName The name of the interface
     *      for the constraint.
     * @param stringToTest The String that is required to be not null.
     * @param fieldName The name of the field that is being tested.
     * @param predicate The Predicate to be tested for the String.
     * @param constraintContext The constraint context. If there is a
     *      violation, it is recorded here.
     * @param valid The state of validity, up to this point.
     * @return The updated validity state.
    */
    public static boolean requireFieldNotEmptyStringAndSatisfiesPredicate(
            final String constraintInterfaceName,
            final String stringToTest,
            final String fieldName,
            final Predicate<String> predicate,
            final ConstraintValidatorContext constraintContext,
            final boolean valid) {
        boolean validToReturn = valid;
        if (stringToTest == null || stringToTest.isEmpty()
                || !predicate.test(stringToTest)) {
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
     * @param stringToTest The String that is required to be not null.
     * @param fieldName The name of the field that is being tested.
     * @param constraintContext The constraint context. If there is a
     *      violation, it is recorded here.
     * @param nodeModifier A consumer of the ConstraintViolationBuilder
     *      to qualify the location of the error. The caller is
     *      responsible for specying a consumer that adds all location
     *      information, including,
     *      e.g., invoking {@code addPropertyNode()},
     *      and for invoking {@code addConstraintViolation()} at the end.
     * @param valid The state of validity, up to this point.
     * @return The updated validity state.
    */
    public static boolean requireFieldNotEmptyString(
            final String constraintInterfaceName,
            final String stringToTest,
            final String fieldName,
            final ConstraintValidatorContext constraintContext,
            final Consumer<ConstraintViolationBuilder> nodeModifier,
            final boolean valid) {
        boolean validToReturn = valid;
        if (stringToTest == null || stringToTest.isEmpty()) {
            validToReturn = false;
            constraintContext.disableDefaultConstraintViolation();
            nodeModifier.accept(constraintContext.
                    buildConstraintViolationWithTemplate(
                    "{" + constraintInterfaceName + "." + fieldName + "}"));
        }
        return validToReturn;
    }

    /** Check that a field of a bean is not an empty string, and
     * also satisfies a property specified as a predicate..
     * If it is in fact an empty string, register a constraint violation.
     * @param constraintInterfaceName The name of the interface
     *      for the constraint.
     * @param stringToTest The String that is required to be not null.
     * @param fieldName The name of the field that is being tested.
     * @param predicate The Predicate to be tested for the String.
     * @param constraintContext The constraint context. If there is a
     *      violation, it is recorded here.
     * @param nodeModifier A consumer of the ConstraintViolationBuilder
     *      to qualify the location of the error. The caller is
     *      responsible for specying a consumer that adds all location
     *      information, including,
     *      e.g., invoking {@code addPropertyNode()},
     *      and for invoking {@code addConstraintViolation()} at the end.
     * @param valid The state of validity, up to this point.
     * @return The updated validity state.
    */
    public static boolean requireFieldNotEmptyStringAndSatisfiesPredicate(
            final String constraintInterfaceName,
            final String stringToTest,
            final String fieldName,
            final Predicate<String> predicate,
            final ConstraintValidatorContext constraintContext,
            final Consumer<ConstraintViolationBuilder> nodeModifier,
            final boolean valid) {
        boolean validToReturn = valid;
        if (stringToTest == null || stringToTest.isEmpty()
                || !predicate.test(stringToTest)) {
            validToReturn = false;
            constraintContext.disableDefaultConstraintViolation();
            nodeModifier.accept(constraintContext.
                    buildConstraintViolationWithTemplate(
                    "{" + constraintInterfaceName + "." + fieldName + "}"));
        }
        return validToReturn;
    }


    /** Check that a field of a bean is a valid date, according to
     * the supported formats: "YYYY", "YYYY-MM", and "YYYY-MM-DD".
     * If it is not valid, register a constraint violation.
     * @param constraintInterfaceName The name of the interface
     *      for the constraint.
     * @param dateToTest The String that is required to be a valid date.
     * @param fieldName The name of the field that is being tested.
     * @param mayBeEmpty If the field is allowed to be missing/empty.
     *      If this is true, validation passes if the field is null
     *      or an empty string. If this is false, a date value
     *      must be provided.
     * @param constraintContext The constraint context. If there is a
     *      violation, it is recorded here.
     * @param valid The state of validity, up to this point.
     * @return The updated validity state.
    */
    public static boolean requireFieldValidDate(
            final String constraintInterfaceName,
            final String dateToTest,
            final String fieldName,
            final boolean mayBeEmpty,
            final ConstraintValidatorContext constraintContext,
            final boolean valid) {
        /* validToReturn: what will be the return value of this method. */
        boolean validToReturn = valid;
        /* validDate: whether dateToTest is valid. */
        boolean validDate = true;
        /* Any extra help we get. For the moment, that means an error
         * message that resulting from parsing. */
        String extraErrorInfo = "";
        // First, basic tests of the format: a value must be provided,
        // and must match a regular expression.
        if (dateToTest == null || dateToTest.isEmpty()) {
            validDate = mayBeEmpty;
            // And that's all the checking we do in this case.
        } else {
            // A value was supplied. First, decide what format
            // has been provided; we do that by checking the value
            // against the three permitted lengths.
            String dateAsYYYYMMDD = dateToTest;
            if (dateAsYYYYMMDD.length() == YYYY_LENGTH) {
                dateAsYYYYMMDD += "-01";
                extraErrorInfo += "; (NB: no month was specified, so '-01' "
                        + "was temporarily inserted during validation)";
            }
            if (dateAsYYYYMMDD.length() == YYYY_MM_LENGTH) {
                dateAsYYYYMMDD += "-01";
                extraErrorInfo += "; (NB: no day was specified, so '-01' "
                        + "was temporarily inserted during validation)";
            }
            if (dateAsYYYYMMDD.length() != YYYY_MM_DD_LENGTH) {
                validDate = false;
                extraErrorInfo += "; value must be either "
                        + YYYY_LENGTH + ", "
                        + YYYY_MM_LENGTH + ", or "
                        + YYYY_MM_DD_LENGTH
                        + " characters long";
            }
            if (validDate) {
                try {
                    YYYY_MM_DD.parse(dateAsYYYYMMDD);
                } catch (DateTimeParseException dte) {
                    validDate = false;
                    extraErrorInfo += "; " + dte.getMessage();
                }
            }
        }
        if (!validDate) {
            validToReturn = false;
            constraintContext.disableDefaultConstraintViolation();
            constraintContext.buildConstraintViolationWithTemplate(
                    "{" + constraintInterfaceName + "." + fieldName + "}"
                    + extraErrorInfo).
            addPropertyNode(fieldName).
            addConstraintViolation();
        }
        return validToReturn;
    }

    /** String of a regular expression used to match valid slugs.
     * Does not contain anchors to the beginning and end of the string,
     * so use with {@link java.util.regex.Matcher#matches()}. */
    private static final String SLUG_REGEX_STRING = "[a-z0-9-]+";

    /** Regular expression used to match valid slugs. */
    private static final Pattern SLUG_PATTERN =
            Pattern.compile(SLUG_REGEX_STRING);

    /** Maximum allowed length of a slug. */
    private static final int SLUG_MAX_LENGTH = 50;

    /** Determine if a user-specified slug is valid, i.e., has the
     * correct format. That means, it has only the allowed characters,
     * and is not too long.
     * @param slug The slug value to be tested.
     * @return true, if the slug value is valid.
     */
    public static boolean isValidSlug(final String slug) {
        if (slug == null || slug.isEmpty()
                || slug.length() > SLUG_MAX_LENGTH) {
            return false;
        }
        return SLUG_PATTERN.matcher(slug).matches();
    }

}
