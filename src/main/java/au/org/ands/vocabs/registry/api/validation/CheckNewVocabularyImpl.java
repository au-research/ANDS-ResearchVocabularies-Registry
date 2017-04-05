/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.validation;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Iterator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.dao.PoolPartyServerDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary.PoolpartyProject;

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
            /* User can't specify an id for a new vocab.
             * Note: _we_ can't distinguish omitting an id,
             * from specifying an id of 0. */
            valid = false;
            constraintContext.disableDefaultConstraintViolation();
            constraintContext.buildConstraintViolationWithTemplate(
                    "{" + CheckNewVocabulary.INTERFACE_NAME + ".id}").
            addPropertyNode("id").
            addConstraintViolation();
        }

        // status: required
        valid = ValidationUtils.requireFieldNotNull(
                CheckNewVocabulary.INTERFACE_NAME,
                newVocabulary.getStatus(), "status",
                constraintContext, valid);

        // owner: required
        // NB: we can't do authorization checks here.
        valid = ValidationUtils.requireFieldNotEmptyString(
                CheckNewVocabulary.INTERFACE_NAME,
                newVocabulary.getOwner(), "owner",
                constraintContext, valid);

        // slug: optional, but if specified, must not already exist
        String slug = newVocabulary.getSlug();
        if (slug != null) {
            if (!ValidationUtils.isValidSlug(slug)) {
                valid = false;
                constraintContext.disableDefaultConstraintViolation();
                constraintContext.buildConstraintViolationWithTemplate(
                    "{" + CheckNewVocabulary.INTERFACE_NAME + ".slug}").
                addPropertyNode("slug").
                addConstraintViolation();
            } else if (!ValidationUtils.isValidSlug(slug)
                    || VocabularyDAO.isSlugInUse(slug)) {
                valid = false;
                constraintContext.disableDefaultConstraintViolation();
                constraintContext.buildConstraintViolationWithTemplate(
                    "{" + CheckNewVocabulary.INTERFACE_NAME + ".slug.inUse}").
                addPropertyNode("slug").
                addConstraintViolation();
            }
        }

        // title: required
        valid = ValidationUtils.requireFieldNotEmptyString(
                CheckNewVocabulary.INTERFACE_NAME,
                newVocabulary.getTitle(), "title",
                constraintContext, valid);

        // acronym: optional

        // description: required
        valid = ValidationUtils.requireFieldNotEmptyString(
                CheckNewVocabulary.INTERFACE_NAME,
                newVocabulary.getDescription(), "description",
                constraintContext, valid);

        // note: optional

        // subject: at least from ANZSRC-FOR required

        // primaryLanguage: required, and must come from the list
        String primaryLanguage = newVocabulary.getPrimaryLanguage();
        valid = ValidationUtils.
                requireFieldNotEmptyStringAndSatisfiesPredicate(
                        CheckNewVocabulary.INTERFACE_NAME,
                        primaryLanguage,
                        "primaryLanguage",
                        Languages::isValidLanguage,
                        constraintContext, valid);
        // We use primaryLanguage below. Be careful, since it may be null.
        // (If it's null, the vocabulary is invalid, but we keep checking
        // anyway. So the following code mustn't _assume_ it's non-null,
        // otherwise we might get a NullPointerException.)
        // For now, that concern applies to
        // Languages.isValidLanguageNotPrimary(). It does not require
        // the primaryLanguage parameter to be non-null.

        // otherLanguages: optional, and must come from the list
        int olIndex = 0;
        for (Iterator<String> it = newVocabulary.getOtherLanguage().iterator();
                it.hasNext(); olIndex++) {
            String tc = it.next();
            final int index = olIndex;
            // See https://hibernate.atlassian.net/browse/BVAL-191
            // for why we need the otherwise bogus addBeanNode().
            // Without it, we get, e.g., newVocab[1].topConcept
            // instead of newVocab.topConcept[1].
            valid = ValidationUtils.
                    requireFieldNotEmptyStringAndSatisfiesPredicate(
                            CheckNewVocabulary.INTERFACE_NAME,
                            tc, "otherLanguage",
                            lang -> Languages.isValidLanguageNotPrimary(
                                    primaryLanguage, lang),
                            constraintContext,
                            cvb -> cvb.addPropertyNode("otherLanguage").
                                addBeanNode().inIterable().
                                atIndex(index).addConstraintViolation(),
                                valid);
        }

        // Check for duplicates in the list of other languages.
        HashSet<String> otherLanguagesSet = new HashSet<>();
        otherLanguagesSet.addAll(newVocabulary.getOtherLanguage());
        if (otherLanguagesSet.size()
                != newVocabulary.getOtherLanguage().size()) {
            // There is at least one duplicate in the list!
            valid = false;
            constraintContext.disableDefaultConstraintViolation();
            constraintContext.buildConstraintViolationWithTemplate(
                    "{au.org.ands.vocabs.registry.api.validation."
                    + "CheckNewVocabulary.otherLanguage.duplicate}").
            addPropertyNode("otherLanguage").
            addConstraintViolation();
        }

        // licence: optional

        // poolpartyProject: optional
        // If specified, check it is a project in the server specified
        PoolpartyProject poolpartyProject =
                newVocabulary.getPoolpartyProject();
        if (poolpartyProject != null) {
            int serverId = poolpartyProject.getServerId();
            if (PoolPartyServerDAO.getPoolPartyServerById(serverId) == null) {
                valid = false;
                constraintContext.disableDefaultConstraintViolation();
                constraintContext.buildConstraintViolationWithTemplate(
                        "{au.org.ands.vocabs.registry.api.validation."
                        + "CheckNewVocabulary.poolpartyProject.serverId}").
                addPropertyNode("poolpartyProject").
                addPropertyNode("serverId").
                addConstraintViolation();
            }
        }


        // topConcept: optional
        // But, each one specified must be a non-empty string.
        int tcIndex = 0;
        for (Iterator<String> it = newVocabulary.getTopConcept().iterator();
                it.hasNext(); tcIndex++) {
            String tc = it.next();
            final int index = tcIndex;
            // See https://hibernate.atlassian.net/browse/BVAL-191
            // for why we need the otherwise bogus addBeanNode().
            // Without it, we get, e.g., newVocab[1].topConcept
            // instead of newVocab.topConcept[1].
            valid = ValidationUtils.requireFieldNotEmptyString(
                    CheckNewVocabulary.INTERFACE_NAME,
                    tc, "topConcept",
                    constraintContext,
                    cvb -> cvb.addPropertyNode("topConcept").
                        addBeanNode().inIterable().
                        atIndex(index).addConstraintViolation(),
                    valid);
        }

        // Check for duplicates in the list of top concepts.
        HashSet<String> topConceptsSet = new HashSet<>();
        topConceptsSet.addAll(newVocabulary.getTopConcept());
        if (topConceptsSet.size()
                != newVocabulary.getTopConcept().size()) {
            // There is at least one duplicate in the list!
            valid = false;
            constraintContext.disableDefaultConstraintViolation();
            constraintContext.buildConstraintViolationWithTemplate(
                    "{au.org.ands.vocabs.registry.api.validation."
                    + "CheckNewVocabulary.topConcept.duplicate}").
            addPropertyNode("topConcept").
            addConstraintViolation();
        }

        // creationDate: required
        // Must match a regular expression: YYYY, or YYYY-MM, or YYYY-MM-DD.
        valid = ValidationUtils.requireFieldValidDate(
                CheckNewVocabulary.INTERFACE_NAME,
                newVocabulary.getCreationDate(), "creationDate", false,
                constraintContext, valid);

        // revisionCycle: optional

        // relatedEntityRef

        // what else?

        return valid;
    }


}
