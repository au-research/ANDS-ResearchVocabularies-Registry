/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.validation;

import static au.org.ands.vocabs.registry.api.validation.CheckRelatedEntity.INTERFACE_NAME;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintViolation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.enums.RelatedEntityIdentifierType;
import au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntity;
import au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntityIdentifier;

/** Validation of input data provided for related entity creation.
 */
public class CheckRelatedEntityImpl
    implements ConstraintValidator<CheckRelatedEntity, RelatedEntity> {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** The validation mode to be used during validation. */
    private ValidationMode mode;

    /** Initialize this instance of the validator.
     * That means: copy the value of the mode parameter into a private field,
     * so that it can be used later by {@link #isValid(RelatedEntity,
     * ConstraintValidatorContext)}.
     */
    @Override
    public void initialize(final CheckRelatedEntity cnv) {
        mode = cnv.mode();
    }

    /** Validate a proposed new or updated related entity.
     * @param relatedEntity The related entity that is being created or updated.
     * @return true, if relatedEntity represents a valid related entity.
     */
    @Override
    @SuppressWarnings("checkstyle:MethodLength")
    public boolean isValid(final RelatedEntity relatedEntity,
            final ConstraintValidatorContext constraintContext) {

        logger.debug("In CheckRelatedEntityImpl.isValid()");

        // Start by assuming validity.
        boolean valid = true;
        // Convenience boolean flag.
        boolean isNew = mode == ValidationMode.CREATE;

        // Table of contents of this method:
        // id
        // type
        // owner
        // title
        // email
        // phone
        // url
        // identifiers

        // id: if mode == CREATE, required _not_ to be provided
        if (isNew && relatedEntity.getId() != null) {
            /* User can't specify an id for a new related entity. */
            valid = ValidationUtils.requireFieldNotNull(
                    INTERFACE_NAME + ".create",
                    relatedEntity.getId(), "id",
                    constraintContext, valid);
        }
        // id: if mode == UPDATE, _required_ to be provided
        if (!isNew && relatedEntity.getId() == null) {
            /* User must specify an id for an update. */
            valid = false;
            constraintContext.buildConstraintViolationWithTemplate(
                    "{" + INTERFACE_NAME + ".update.id}").
            addPropertyNode("id").
            addConstraintViolation();
        }

        // type
        if (relatedEntity.getType() == null) {
            // It is null if no value is supplied, or if a value is
            // supplied, but it is not one of the allowed values.
            valid = false;
            constraintContext.buildConstraintViolationWithTemplate(
                    "{" + INTERFACE_NAME + ".type}").
            addPropertyNode("type").
            addConstraintViolation();
        }

        // owner: required
        // NB: we can't do authorization checks here.
        valid = ValidationUtils.requireFieldNotEmptyString(
                INTERFACE_NAME,
                relatedEntity.getOwner(), "owner",
                constraintContext, valid);

        // title: required
        valid = ValidationUtils.requireFieldNotEmptyString(
                INTERFACE_NAME,
                relatedEntity.getTitle(), "title",
                constraintContext, valid);

        // email
        String email = relatedEntity.getEmail();
        if (email != null) {
            Set<ConstraintViolation<FieldValidationHelper>> emailViolations =
                    ValidationUtils.getValidator().
                    validateValue(FieldValidationHelper.class,
                    FieldValidationHelper.EMAIL_FIELDNAME, email);
            if (!emailViolations.isEmpty()) {
                valid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                        "{" + INTERFACE_NAME + ".email}").
                addPropertyNode("email").
                addConstraintViolation();
            }
        }

        // phone
        // It is optional, but if specified, must be non-empty.
        // Possible future: use Google's libphonenumber library.
        String phone = relatedEntity.getPhone();
        if (phone != null) {
            valid = ValidationUtils.requireFieldNotEmptyString(
                    INTERFACE_NAME, phone, "phone",
                    constraintContext, valid);
        }

        // url
        int urlIndex = 0;
        for (Iterator<String> it = relatedEntity.getUrl().iterator();
                it.hasNext(); urlIndex++) {
            String url = it.next();
            final int index = urlIndex;
            valid = ValidationUtils.
                    requireFieldNotEmptyStringAndSatisfiesPredicate(
                            INTERFACE_NAME, url, "url",
                            ValidationUtils::isValidURL,
                            constraintContext,
                            cvb -> cvb.addPropertyNode("url").addBeanNode().
                            inIterable().atIndex(index).
                            addConstraintViolation(),
                            valid);
        }

        // identifiers
        // newIdentifiers is a Map used to keep track of all the
        // _different_ identifiers specified in the request body.
        Map<RelatedEntityIdentifierType, Set<String>> newIdentifiers =
                new HashMap<>();
        int identifierIndex = 0;
        for (Iterator<RelatedEntityIdentifier> it =
                relatedEntity.getRelatedEntityIdentifier().iterator();
                it.hasNext(); identifierIndex++) {
            RelatedEntityIdentifier rei = it.next();
            RelatedEntityIdentifierType identifierType =
                    rei.getIdentifierType();
            Set<String> setForType =
                    newIdentifiers.get(identifierType);
            if (setForType == null) {
                setForType = new HashSet<>();
                newIdentifiers.put(identifierType, setForType);
            }
            setForType.add(rei.getIdentifierValue());
            // id:  if mode == CREATE, required _not_ to be provided
            if (isNew && rei.getId() != null) {
                /* User can't specify an id for a new related entity
                   identifier. */
                valid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                        "{" + INTERFACE_NAME + ".relatedEntityIdentifier.id}").
                addPropertyNode("relatedEntityIdentifier").
                addPropertyNode("id").
                inIterable().atIndex(identifierIndex).
                addConstraintViolation();
            }
            if (rei.getIdentifierType() == null) {
                // It is null if no value is supplied, or if a value is
                // supplied, but it is not one of the allowed values.
                valid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                        "{" + INTERFACE_NAME
                        + ".relatedEntityIdentifier.identifierType}").
                addPropertyNode("relatedEntityIdentifier").
                addPropertyNode("identifierType").
                inIterable().atIndex(identifierIndex).
                addConstraintViolation();
            } else {
                // NB: so we only try to validate the value, if there
                // is a valid type. This should be (and is!) noted
                // in the error message for the type.
                final int index = identifierIndex;
                // Slight abuse of the predicate parameter here ...
                // we ignore the lambda's string parameter, and pass in the
                // whole identifier to the helper method.
                valid = ValidationUtils.
                        requireFieldNotEmptyStringAndSatisfiesPredicate(
                            INTERFACE_NAME, rei.getIdentifierValue(),
                            "relatedEntityIdentifier.identifierValue",
                            v -> ValidationUtils.
                                isValidRelatedEntityIdentifier(rei),
                            constraintContext,
                            cvb -> cvb.addPropertyNode(
                                    "relatedEntityIdentifier").
                                addPropertyNode("identifierValue").
                                inIterable().atIndex(index).
                                addConstraintViolation(),
                            valid);
            }
        }
        // Add up all the _different_ identifiers we have seen.
        int identifiersInMap = 0;
        for (Set<String> s : newIdentifiers.values()) {
            identifiersInMap += s.size();
        }
        if (identifiersInMap != identifierIndex) {
            valid = false;
            constraintContext.buildConstraintViolationWithTemplate(
                    "{" + INTERFACE_NAME
                    + ".relatedEntityIdentifier.duplicate}").
            addPropertyNode("relatedEntityIdentifier").
            addConstraintViolation();
        }

        return valid;
    }

}
