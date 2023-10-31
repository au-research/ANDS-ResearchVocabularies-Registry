/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.validation;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.user.ValidationError;
import au.org.ands.vocabs.registry.enums.RelatedEntityRelation;
import au.org.ands.vocabs.registry.enums.RelatedEntityType;
import au.org.ands.vocabs.registry.enums.RelatedVocabularyRelation;
import au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntityIdentifier;
import au.org.ands.vocabs.registry.utils.SlugGenerator;

/** Utility methods to support validation. */
public final class ValidationUtils {

    /** Logger for this class. */
    private static Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Private constructor for a utility class. */
    private ValidationUtils() {
    }

    /** A validator that can be used within our own custom validators.
     * Initialized within a static block. */
    private static Validator validator;

    static {
        ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /** Get the utility validator instance.
     * @return The Validator instance. */
    public static Validator getValidator() {
        return validator;
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

    /** Maximum length of some fields. Intended to keep length of
     * vocabulary and version JSON data under control. */
    private static final int MAX_FIELD_LENGTH = 10000;

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
        return requireFieldNotNull(constraintInterfaceName,
                 objectToTest, fieldName, constraintContext, null,
                 valid);
    }

    /** Check that a field of a bean is not null. If it is in fact
     * null, register a constraint violation.
     * @param constraintInterfaceName The name of the interface
     *      for the constraint.
     * @param objectToTest The object that is required to be not null.
     * @param fieldName The name of the field that is being tested.
     * @param constraintContext The constraint context. If there is a
     *      violation, it is recorded here.
     * @param nodeModifier If null, the generated ConstraintViolationBuilder
     *      has {@code addPropertyNode(fieldName).addConstraintViolation()}
     *      invoked.
     *      If not null, this is taken to be a consumer of the
     *      ConstraintViolationBuilder
     *      to qualify the location of the error, and the caller is then
     *      responsible for specying a consumer that adds all location
     *      information, including,
     *      e.g., invoking {@code addPropertyNode()},
     *      and for invoking {@code addConstraintViolation()} at the end.
     * @param valid The state of validity, up to this point.
     * @return The updated validity state.
    */
    public static boolean requireFieldNotNull(
            final String constraintInterfaceName,
            final Object objectToTest,
            final String fieldName,
            final ConstraintValidatorContext constraintContext,
            final Consumer<ConstraintViolationBuilder> nodeModifier,
            final boolean valid) {
        boolean validToReturn = valid;
        if (objectToTest == null) {
            validToReturn = false;
            ConstraintViolationBuilder cvb = constraintContext.
                    buildConstraintViolationWithTemplate(
                    "{" + constraintInterfaceName + "." + fieldName + "}");
            if (nodeModifier != null) {
                nodeModifier.accept(cvb);
            } else {
                cvb.addPropertyNode(fieldName).addConstraintViolation();
            }
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
        return requireFieldNotEmptyString(constraintInterfaceName,
                stringToTest, fieldName, constraintContext, null,
                valid);
    }

    /** Check that a field of a bean is not an empty string.
     * If it is in fact an empty string, register a constraint violation.
     * @param constraintInterfaceName The name of the interface
     *      for the constraint.
     * @param stringToTest The String that is required to be not null.
     * @param fieldName The name of the field that is being tested.
     * @param constraintContext The constraint context. If there is a
     *      violation, it is recorded here.
     * @param nodeModifier If null, the generated ConstraintViolationBuilder
     *      has {@code addPropertyNode(fieldName).addConstraintViolation()}
     *      invoked.
     *      If not null, this is taken to be a consumer of the
     *      ConstraintViolationBuilder
     *      to qualify the location of the error, and the caller is then
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
            ConstraintViolationBuilder cvb = constraintContext.
                    buildConstraintViolationWithTemplate(
                    "{" + constraintInterfaceName + "." + fieldName + "}");
            if (nodeModifier != null) {
                nodeModifier.accept(cvb);
            } else {
                cvb.addPropertyNode(fieldName).addConstraintViolation();
            }
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
        return requireFieldNotEmptyStringAndSatisfiesPredicate(
                constraintInterfaceName, stringToTest, fieldName,
                predicate, constraintContext, null, valid);
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
     * @param nodeModifier If null, the generated ConstraintViolationBuilder
     *      has {@code addPropertyNode(fieldName).addConstraintViolation()}
     *      invoked.
     *      If not null, this is taken to be a consumer of the
     *      ConstraintViolationBuilder
     *      to qualify the location of the error, and the caller is then
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
            ConstraintViolationBuilder cvb = constraintContext.
                    buildConstraintViolationWithTemplate(
                    "{" + constraintInterfaceName + "." + fieldName + "}");
            if (nodeModifier != null) {
                nodeModifier.accept(cvb);
            } else {
                cvb.addPropertyNode(fieldName).addConstraintViolation();
            }
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
        return requireFieldValidDate(constraintInterfaceName,
                dateToTest, fieldName, mayBeEmpty, constraintContext, null,
                valid);
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
     * @param nodeModifier If null, the generated ConstraintViolationBuilder
     *      has {@code addPropertyNode(fieldName).addConstraintViolation()}
     *      invoked.
     *      If not null, this is taken to be a consumer of the
     *      ConstraintViolationBuilder
     *      to qualify the location of the error, and the caller is then
     *      responsible for specying a consumer that adds all location
     *      information, including,
     *      e.g., invoking {@code addPropertyNode()},
     *      and for invoking {@code addConstraintViolation()} at the end.
     * @param valid The state of validity, up to this point.
     * @return The updated validity state.
    */
    public static boolean requireFieldValidDate(
            final String constraintInterfaceName,
            final String dateToTest,
            final String fieldName,
            final boolean mayBeEmpty,
            final ConstraintValidatorContext constraintContext,
            final Consumer<ConstraintViolationBuilder> nodeModifier,
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
            ConstraintViolationBuilder cvb = constraintContext.
                    buildConstraintViolationWithTemplate(
                    "{" + constraintInterfaceName + "." + fieldName + "}"
                    + extraErrorInfo);
            if (nodeModifier != null) {
                nodeModifier.accept(cvb);
            } else {
                cvb.addPropertyNode(fieldName).addConstraintViolation();
            }
        }
        return validToReturn;
    }

    /** Determine if a user-specified slug is valid, i.e., has the
     * correct format. That means, it has only the allowed characters,
     * and is not too long.
     * @param slug The slug value to be tested.
     * @return true, if the slug value is valid.
     */
    public static boolean isValidSlug(final String slug) {
        if (StringUtils.isBlank(slug)) {
            return false;
        }
        // Slug generation is idempotent. So, the proposed slug
        // is valid iff it comes out of the slug generator unchanged.
        return slug.equals(SlugGenerator.generateSlug(slug));
    }

    /** Whitelist for jsoup to use to validate HTML. Initialized
     * in a static block to {@link Whitelist#basic()},
     * and customized further with regard to "a" tags. */
    private static Whitelist validWhitelist;

    static {
        validWhitelist = Whitelist.basic();
        validWhitelist.removeEnforcedAttribute("a", "rel");
        validWhitelist.addAttributes("a", "href", "rel", "target");
        validWhitelist.preserveRelativeLinks(true);
    }

    /** Whitelist for jsoup to use to clean HTML. Initialized
     * in a static block to {@link Whitelist#basic()},
     * and customized further with regard to "a" tags. */
    private static Whitelist cleanWhitelist;

    static {
        cleanWhitelist = Whitelist.basic();
        cleanWhitelist.addEnforcedAttribute("a", "target", "_blank");
        cleanWhitelist.addEnforcedAttribute("a", "rel",
                "nofollow noopener noreferrer");
        cleanWhitelist.preserveRelativeLinks(true);
    }

    /** Utility method to determine if a String value contains
     * an HTML body fragment that parses correctly and contains
     * only the permitted tags/attributes.
     * @param stringToTest The String to be tested.
     * @return true, if stringToTest is valid.
     */
    public static boolean isValidHTML(final String stringToTest) {
        return Jsoup.isValid(stringToTest, validWhitelist);
    }

    /** Utility method to clean a a String value so that it
     * contains the required attributes.
     * @param stringToClean The String to be cleaned.
     * @return The cleaned string.
     */
    public static String cleanHTML(final String stringToClean) {
        return Jsoup.clean(stringToClean, cleanWhitelist);
    }

    /** Check that a field of a bean contains only acceptable HTML.
     * Here, "acceptable" means according to jsoup's "basic"
     * whitelist. A field value of null is also considered to
     * be "acceptable", so if the field is required, you must
     * test for this separately.
     * @param constraintInterfaceName The name of the interface
     *      for the constraint.
     * @param stringToTest The String that is required to have valid
     *      HTML. It must already have been checked to be a non-empty
     *      string. If null, or an empty string is passed in, return
     *      immediately with the value of valid.
     * @param fieldName The name of the field that is being tested.
     * @param constraintContext The constraint context. If there is a
     *      violation, it is recorded here.
     * @param valid The state of validity, up to this point.
     * @return The updated validity state.
    */
    public static boolean requireFieldValidHTML(
            final String constraintInterfaceName,
            final String stringToTest,
            final String fieldName,
            final ConstraintValidatorContext constraintContext,
            final boolean valid) {
        return requireFieldValidHTML(constraintInterfaceName,
                stringToTest, fieldName, constraintContext, null,
                valid);
    }

    /** Check that a field of a bean contains only acceptable HTML.
     * Here, "acceptable" means according to jsoup's "basic"
     * whitelist. A field value of null is also considered to
     * be "acceptable", so if the field is required, you must
     * test for this separately.
     * @param constraintInterfaceName The name of the interface
     *      for the constraint.
     * @param stringToTest The String that is required to have valid
     *      HTML. It must already have been checked to be a non-empty
     *      string. If null, or an empty string is passed in, return
     *      immediately with the value of valid.
     * @param fieldName The name of the field that is being tested.
     * @param constraintContext The constraint context. If there is a
     *      violation, it is recorded here.
     * @param nodeModifier If null, the generated ConstraintViolationBuilder
     *      has {@code addPropertyNode(fieldName).addConstraintViolation()}
     *      invoked.
     *      If not null, this is taken to be a consumer of the
     *      ConstraintViolationBuilder
     *      to qualify the location of the error, and the caller is then
     *      responsible for specying a consumer that adds all location
     *      information, including,
     *      e.g., invoking {@code addPropertyNode()},
     *      and for invoking {@code addConstraintViolation()} at the end.
     * @param valid The state of validity, up to this point.
     * @return The updated validity state.
    */
    public static boolean requireFieldValidHTML(
            final String constraintInterfaceName,
            final String stringToTest,
            final String fieldName,
            final ConstraintValidatorContext constraintContext,
            final Consumer<ConstraintViolationBuilder> nodeModifier,
            final boolean valid) {
        if (stringToTest == null || stringToTest.isEmpty()) {
            return valid;
        }
        boolean validToReturn = valid;

        if (!isValidHTML(stringToTest)) {
            validToReturn = false;
            ConstraintViolationBuilder cvb = constraintContext.
                    buildConstraintViolationWithTemplate(
                    "{" + constraintInterfaceName + "." + fieldName
                    + ".html}");
            if (nodeModifier != null) {
                nodeModifier.accept(cvb);
            } else {
                cvb.addPropertyNode(fieldName).addConstraintViolation();
            }
        }
        return validToReturn;
    }

    /** Check that a field of a bean contains text that is "not too long",
     * so that it will be able to be persisted in the JSON data field.
     * @param constraintInterfaceName The name of the interface
     *      for the constraint.
     * @param stringToTest The String that is required to be less than
     *      or equal to a maximum length.
     *      It must already have been checked to be a non-empty
     *      string. If null, or an empty string is passed in, return
     *      immediately with the value of valid.
     * @param fieldName The name of the field that is being tested.
     * @param constraintContext The constraint context. If there is a
     *      violation, it is recorded here.
     * @param valid The state of validity, up to this point.
     * @return The updated validity state.
    */
    public static boolean requireFieldLessThanMaxLength(
            final String constraintInterfaceName,
            final String stringToTest,
            final String fieldName,
            final ConstraintValidatorContext constraintContext,
            final boolean valid) {
        return requireFieldLessThanMaxLength(constraintInterfaceName,
                stringToTest, fieldName, constraintContext, null,
                valid);
    }

    /** Check that a field of a bean contains text that is "not too long",
     * so that it will be able to be persisted in the JSON data field.
     * @param constraintInterfaceName The name of the interface
     *      for the constraint.
     * @param stringToTest The String that is required to be less than
     *      or equal to a maximum length.
     *      It must already have been checked to be a non-empty
     *      string. If null, or an empty string is passed in, return
     *      immediately with the value of valid.
     * @param fieldName The name of the field that is being tested.
     * @param constraintContext The constraint context. If there is a
     *      violation, it is recorded here.
     * @param nodeModifier If null, the generated ConstraintViolationBuilder
     *      has {@code addPropertyNode(fieldName).addConstraintViolation()}
     *      invoked.
     *      If not null, this is taken to be a consumer of the
     *      ConstraintViolationBuilder
     *      to qualify the location of the error, and the caller is then
     *      responsible for specying a consumer that adds all location
     *      information, including,
     *      e.g., invoking {@code addPropertyNode()},
     *      and for invoking {@code addConstraintViolation()} at the end.
     * @param valid The state of validity, up to this point.
     * @return The updated validity state.
    */
    public static boolean requireFieldLessThanMaxLength(
            final String constraintInterfaceName,
            final String stringToTest,
            final String fieldName,
            final ConstraintValidatorContext constraintContext,
            final Consumer<ConstraintViolationBuilder> nodeModifier,
            final boolean valid) {
        if (stringToTest == null || stringToTest.isEmpty()) {
            return valid;
        }
        boolean validToReturn = valid;

        if (stringToTest.length() > MAX_FIELD_LENGTH) {
            validToReturn = false;
            ConstraintViolationBuilder cvb = constraintContext.
                    buildConstraintViolationWithTemplate(
                    "{" + constraintInterfaceName + "." + fieldName
                    + ".length}");
            if (nodeModifier != null) {
                nodeModifier.accept(cvb);
            } else {
                cvb.addPropertyNode(fieldName).addConstraintViolation();
            }
        }
        return validToReturn;
    }

    /** The set of allowed relations for related entities that are parties.
     * Initialized in a static block. */
    private static final HashSet<RelatedEntityRelation>
        ALLOWED_RELATIONS_FOR_PARTY = new HashSet<>();

    /** The set of allowed relations for related entities that are services.
     * Initialized in a static block. */
    private static final HashSet<RelatedEntityRelation>
        ALLOWED_RELATIONS_FOR_SERVICE = new HashSet<>();

    /** The set of allowed relations for related entities that are
     * external vocabularies. Initialized in a static block. */
    private static final HashSet<RelatedEntityRelation>
        ALLOWED_RELATIONS_FOR_EXTERNAL_VOCABULARY = new HashSet<>();

    /** The set of allowed relations for related entities that are
     * internal vocabularies. Initialized in a static block. */
    private static final HashSet<RelatedVocabularyRelation>
        ALLOWED_RELATIONS_FOR_INTERNAL_VOCABULARY = new HashSet<>();

    static {
        // Business rules as specified in:
        // https://ardc-services.atlassian.net/wiki/spaces/PROJ/pages/
        //   3377172/Vocabularies+for+vocabulary+schema
        ALLOWED_RELATIONS_FOR_PARTY.add(RelatedEntityRelation.CONSUMER_OF);
        ALLOWED_RELATIONS_FOR_PARTY.add(RelatedEntityRelation.HAS_AUTHOR);
        ALLOWED_RELATIONS_FOR_PARTY.add(RelatedEntityRelation.HAS_CONTRIBUTOR);
        ALLOWED_RELATIONS_FOR_PARTY.add(RelatedEntityRelation.IMPLEMENTED_BY);
        ALLOWED_RELATIONS_FOR_PARTY.add(RelatedEntityRelation.POINT_OF_CONTACT);
        ALLOWED_RELATIONS_FOR_PARTY.add(RelatedEntityRelation.PUBLISHED_BY);
        ALLOWED_RELATIONS_FOR_PARTY.add(RelatedEntityRelation.IS_FUNDED_BY);

        ALLOWED_RELATIONS_FOR_SERVICE.add(
                RelatedEntityRelation.HAS_ASSOCIATION_WITH);
        ALLOWED_RELATIONS_FOR_SERVICE.add(RelatedEntityRelation.IS_USED_BY);
        ALLOWED_RELATIONS_FOR_SERVICE.add(
                RelatedEntityRelation.IS_PRESENTED_BY);

        ALLOWED_RELATIONS_FOR_EXTERNAL_VOCABULARY.add(
                RelatedEntityRelation.ENRICHES);
        ALLOWED_RELATIONS_FOR_EXTERNAL_VOCABULARY.add(
                RelatedEntityRelation.HAS_ASSOCIATION_WITH);
        ALLOWED_RELATIONS_FOR_EXTERNAL_VOCABULARY.add(
                RelatedEntityRelation.IS_DERIVED_FROM);
        ALLOWED_RELATIONS_FOR_EXTERNAL_VOCABULARY.add(
                RelatedEntityRelation.IS_PART_OF);

        ALLOWED_RELATIONS_FOR_INTERNAL_VOCABULARY.add(
                RelatedVocabularyRelation.ENRICHES);
        ALLOWED_RELATIONS_FOR_INTERNAL_VOCABULARY.add(
                RelatedVocabularyRelation.HAS_ASSOCIATION_WITH);
        ALLOWED_RELATIONS_FOR_INTERNAL_VOCABULARY.add(
                RelatedVocabularyRelation.IS_DERIVED_FROM);
        ALLOWED_RELATIONS_FOR_INTERNAL_VOCABULARY.add(
                RelatedVocabularyRelation.IS_PART_OF);
    }

    /** Decide whether a vocabulary may have a particular relation with
     * a related entity of a certain type, according to the business
     * rules about relations.
     * @param type The type of the related entity.
     * @param relation The relation being tested.
     * @return true, if the vocabulary is allowed to have the relation
     *      to the related entity.
     */
    public static boolean isAllowedRelation(final RelatedEntityType type,
            final RelatedEntityRelation relation) {
        switch (type) {
        case PARTY:
            return ALLOWED_RELATIONS_FOR_PARTY.contains(relation);
        case SERVICE:
            return ALLOWED_RELATIONS_FOR_SERVICE.contains(relation);
        case VOCABULARY:
            return ALLOWED_RELATIONS_FOR_EXTERNAL_VOCABULARY.contains(relation);
        default:
            // Can't happen.
            logger.error("isAllowedRelation: Unknown RelatedEntityType!");
            return false;
        }
    }

    /** Decide whether a vocabulary may have a particular relation with
     * a related internal vocabulary, according to the business
     * rules about relations.
     * @param relation The relation being tested.
     * @return true, if the vocabulary is allowed to have the relation
     *      to the related vocabulary.
     */
    public static boolean isAllowedRelation(
            final RelatedVocabularyRelation relation) {
        return ALLOWED_RELATIONS_FOR_INTERNAL_VOCABULARY.contains(relation);
    }

    /** Validator for validating URLs. */
    private static final UrlValidator URL_VALIDATOR = new UrlValidator();

    /** URI scheme for JavaScript. String comparison may rely on the fact
     * that this is all lowercase. */
    private static final String JAVASCRIPT = "javascript";

    /** Determine if a string is a valid URL, i.e., has the
     * correct format.
     * This implemented using Apache Commons Validator.
     * Only schemes "http", "https", and "ftp" are permitted.
     * The host "localhost" is not permitted.
     * @param url The URL value to be tested.
     * @return true, if the url value is valid.
     */
    public static boolean isValidURL(final String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        return URL_VALIDATOR.isValid(url);
    }

    /** Determine if a string is a valid URI, i.e., has the
     * correct format.
     * The URI must be absolute; that is, it must include a scheme component.
     * @param aUri The URI value to be tested.
     * @return true, if the URI value is valid.
     */
    public static boolean isValidURI(final String aUri) {
        if (aUri == null || aUri.isEmpty()) {
            return false;
        }
        try {
            URI uri = new URI(aUri);
            // We're going to lowercase the scheme further down, so we
            // have to make sure that it's non-null first.
            String scheme = uri.getScheme();
            if (scheme == null) {
                return false;
            }
            // We explicitly disallow JavaScript.
            if (JAVASCRIPT.equals(scheme.toLowerCase(Locale.ROOT))) {
                return false;
            }
            return uri.isAbsolute();
        } catch (URISyntaxException e) {
            // Otherwise, parsing failed, and it is not a valid URI.
            return false;
        }
    }

    /** A String containing only those characters which are used in
     * the base 32 representation of ROR values.
     */
    private static final String ROR_CHARS = "0123456789abcdefghjkmnpqrstvwxyz";

    /** Basic regular expression for RORs, just for making sure that
     * a value to be validated has the correct length and only uses
     * the correct characters.
     * The first character must be 0; the next six characters must be
     * valid base 32, and the final two characters must be decimal digits.
     */
    private static final String ROR_REGEX = "0[" + ROR_CHARS + "]{6}"
            + "[0-9]{2}";

    /** Compiled version of the regular expression for RORs. */
    private static final Pattern ROR_PATTERN =
            Pattern.compile(ROR_REGEX);

    /** Determine if a String value represents a valid ROR.
     * How to validate an ROR? It seems now-deleted <a target="_blank"
     * href="https://twitter.com/JoakimPhilipson/status/1098194723397922817">this
     *  tweeted reply by Martin Fenner</a> was the only documentation.
     * There's now <a target="_blank"
     * href="https://ror.readme.io/docs/ror-identifier-pattern">this
     * documentation</a>.
     * I.e., valid values are as follows:
     * <ul>
     *   <li>Valid values are exactly 9 characters long.
     *     For example, <code>03yrm5c26</code>.</li>
     *   <li>The first character is always <code>0</code>.</li>
     *   <li>The following six characters represent a numeric ID.
     *     These six characters are to be interpreted as a base-32 value,
     *     where the number/letter values used are as shown here:
     *     <a target="_blank"
     * href="https://www.crockford.com/base32.html">https://www.crockford.com/base32.html</a>.
     *     For example, the value <code>3yrm5c</code> is the decimal value
     *     132927660.</li>
     *   <li>The final two characters are a checksum. These two characters
     *     are to be interpreted as a decimal value; e.g., if the final
     *     two characters are <code>26</code>, the checksum is the decimal
     *     value 26.</li>
     *   <li>To compute the expected checksum, compute
     *     98 - ((n * 100) % 97), where n is the decimal value of the
     *     numeric ID. E.g., 98 - ((132927660 * 100) % 97) = 26.</li>
     * </ul>
     * @param ror The String to be validated as an ROR.
     * @return true, iff ror represents a valid ROR.
     */
    // Magic numbers: 5, 7, 97, 98, 100
    @SuppressWarnings("checkstyle:MagicNumber")
    public static boolean isValidROR(final String ror) {
        // An explicit null check is required first, as matcher() throws
        // an NPE on a null actual parameter.
        if (ror == null) {
            return false;
        }
        if (!ROR_PATTERN.matcher(ror).matches()) {
            return false;
        }
        // The value is the correct syntax. Now attempt to decode it.
        // Discard the leading 0, and break up into the value proper
        // and the checksum.
        String rorIdString = ror.substring(1, 7);
        String checksumString = ror.substring(7);
        long intValue = 0;
        for (int i = 0; i < rorIdString.length(); i++) {
            intValue = (intValue << 5)
                    + ROR_CHARS.indexOf(rorIdString.charAt(i));
        }
        // logger.info("intValue: " + intValue);
        int checksumInt = Integer.valueOf(checksumString);
        long remainder = 98 - ((intValue * 100) % 97);
        // logger.info("Computed remainder: " + remainder);
        return remainder == checksumInt;
    }

    /** A convenience reference to the class object for
     * {@link FieldValidationHelper}. Used in {@link
     * #isValidRelatedEntityIdentifier(RelatedEntityIdentifier)}. */
    private static final Class<FieldValidationHelper> FVH_CLASS =
            FieldValidationHelper.class;

    /** Determine if a related entity identifier is valid, according
     * to the rules for each identifier type.
     * Prerequisite: the type is not null, and value is non-empty.
     * @param rei The identifier to be validated.
     * @return true, if the identifier is valid.
     */
    public static boolean isValidRelatedEntityIdentifier(
            final RelatedEntityIdentifier rei) {
        String value = rei.getIdentifierValue();
        switch (rei.getIdentifierType()) {
        case AU_ANL_PEAU:
            return validator.validateValue(FVH_CLASS,
                    FieldValidationHelper.AU_ANL_PEAU_FIELDNAME,
                    value).isEmpty();
        case DOI:
            return validator.validateValue(FVH_CLASS,
                    FieldValidationHelper.DOI_FIELDNAME, value).isEmpty();
        case HANDLE:
            return validator.validateValue(FVH_CLASS,
                    FieldValidationHelper.HANDLE_FIELDNAME,
                    value).isEmpty();
        case INFOURI:
            // First: must be a valid URI.
            // For now, use Java's provided way. May need to be modified
            // if users provide values that are erroneously rejected.
            try {
                new URI(value);
            } catch (URISyntaxException | NullPointerException e) {
                return false;
            }
            // Second: must also begin with "info:".
            return validator.validateValue(FVH_CLASS,
                    FieldValidationHelper.INFOURI_FIELDNAME,
                    value).isEmpty();
        case ISIL:
            return validator.validateValue(FVH_CLASS,
                    FieldValidationHelper.ISIL_FIELDNAME, value).isEmpty();
        case ISNI:
            return validator.validateValue(FVH_CLASS,
                    FieldValidationHelper.ISNI_FIELDNAME, value).isEmpty();
        case LOCAL:
            // Anything goes;
            return true;
        case ORCID:
            return validator.validateValue(FVH_CLASS,
                    FieldValidationHelper.ORCID_FIELDNAME, value).isEmpty();
        case PURL:
            return validator.validateValue(FVH_CLASS,
                    FieldValidationHelper.PURL_FIELDNAME,
                    value).isEmpty() && isValidURL(value);
        case RESEARCHER_ID:
            return validator.validateValue(FVH_CLASS,
                    FieldValidationHelper.RESEARCHER_ID_FIELDNAME,
                    value).isEmpty();
        case ROR:
            return isValidROR(value);
        case URI:
            return isValidURI(value);
        case VIAF:
            return validator.validateValue(FVH_CLASS,
                    FieldValidationHelper.VIAF_FIELDNAME, value).isEmpty();
        default:
            // Can't happen! (Unless we added a new identifier type, but
            // failed to update this method.)
            logger.error("isValidRelatedEntityIdentifier: Attempted to "
                    + "validate an identifier of a type "
                    + "we don't know about: update this method!");
            break;
        }
        return true;
    }

    /**
     * Convert the constraint violations contained in
     * the given exception into a list of
     * validation errors that can be returned to the caller.
     * This method is based on Jersey's "default" implementation
     * in its ValidationHelper class.
     * @param cve The exception containing all of the constraint violations.
     * @return A list of validation errors ready for serialization.
     */
    public static List<ValidationError> constraintViolationToValidationErrors(
            final ConstraintViolationException cve) {
        return cve.getConstraintViolations().stream().map(
                v -> new ValidationError(v.getMessage(),
                        v.getPropertyPath().toString())
        ).collect(Collectors.toList());
    }

}
