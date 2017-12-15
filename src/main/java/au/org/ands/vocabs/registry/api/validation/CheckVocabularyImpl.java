/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.validation;

import static au.org.ands.vocabs.registry.api.validation.CheckVocabulary.INTERFACE_NAME;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.dao.PoolPartyServerDAO;
import au.org.ands.vocabs.registry.db.dao.RelatedEntityDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.db.entity.RelatedEntity;
import au.org.ands.vocabs.registry.enums.ApSource;
import au.org.ands.vocabs.registry.enums.RelatedEntityRelation;
import au.org.ands.vocabs.registry.enums.RelatedVocabularyRelation;
import au.org.ands.vocabs.registry.schema.vocabulary201701.AccessPoint;
import au.org.ands.vocabs.registry.schema.vocabulary201701.ApApiSparql;
import au.org.ands.vocabs.registry.schema.vocabulary201701.ApSissvoc;
import au.org.ands.vocabs.registry.schema.vocabulary201701.ApWebPage;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Version;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary.PoolpartyProject;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary.RelatedEntityRef;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary.RelatedVocabularyRef;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary.Subject;
import au.org.ands.vocabs.registry.utils.SlugGenerator;

/** Validation of input data provided for vocabulary creation.
 */
public class CheckVocabularyImpl
    implements ConstraintValidator<CheckVocabulary, Vocabulary> {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** The validation mode to be used during validation. */
    private ValidationMode mode;

    /** Initialize this instance of the validator.
     * That means: copy the value of the mode parameter into a private field,
     * so that it can be used later by {@link #isValid(Vocabulary,
     * ConstraintValidatorContext)}.
     */
    @Override
    public void initialize(final CheckVocabulary cnv) {
        mode = cnv.mode();
    }

    /** Validate a proposed new or updated vocabulary.
     * @param newVocabulary The vocabulary that is being validated.
     * @return true, if newVocabulary represents a valid vocabulary.
     */
    @SuppressWarnings("checkstyle:MethodLength")
    @Override
    public boolean isValid(final Vocabulary newVocabulary,
            final ConstraintValidatorContext constraintContext) {

        boolean valid = true;
        logger.info("In CheckVocabularyImpl.isValid()");

        // Table of contents of this method:
        // id
        // status
        // owner
        // slug
        // title
        // title passed through slug generation, when slug not provided
        // acronym
        // description
        // note
        // subject
        // primaryLanguage
        // otherLanguage
        // licence
        // poolpartyProject
        // topConcept
        // creationDate
        // revisionCycle
        // relatedEntityRef
        // relatedVocabularyRef
        // version

        // id: mode-specific validation.
        valid = isValidVocabularyId(valid, newVocabulary, constraintContext);

        /* The database instance of the vocabulary, if there is one.
         * This is the draft instance, if there is one; otherwise, it
         * is the current instance. */
        au.org.ands.vocabs.registry.db.entity.Vocabulary existingVocabulary =
                null;
        if (mode == ValidationMode.UPDATE) {
            // Try to get a current instance, then fall back to
            // looking for a draft.
            existingVocabulary =
                VocabularyDAO.getCurrentVocabularyByVocabularyId(
                        newVocabulary.getId());
            if (existingVocabulary == null) {
                List<au.org.ands.vocabs.registry.db.entity.Vocabulary>
                existingDraftInstances =
                        VocabularyDAO.getDraftVocabularyByVocabularyId(
                                newVocabulary.getId());
                if (existingDraftInstances.size() == 0) {
                    valid = false;
                    constraintContext.buildConstraintViolationWithTemplate(
                            "{" + INTERFACE_NAME + ".update.id.unknown}").
                    addPropertyNode("id").
                    addConstraintViolation();
                } else {
                    existingVocabulary = existingDraftInstances.get(0);
                }
            }
        }

        // status: required
        valid = ValidationUtils.requireFieldNotNull(
                INTERFACE_NAME,
                newVocabulary.getStatus(), "status",
                constraintContext, valid);

        // owner: required
        // NB: we can't do authorization checks here.
        valid = ValidationUtils.requireFieldNotEmptyString(
                INTERFACE_NAME,
                newVocabulary.getOwner(), "owner",
                constraintContext, valid);

        // slug: optional, but if specified, must not already exist
        String slug = newVocabulary.getSlug();
        if (mode == ValidationMode.CREATE) {
            // For creation, the slug must be omitted.
            // But if specified, it must be valid, and not already in use.
            if (slug != null) {
                if (!ValidationUtils.isValidSlug(slug)) {
                    valid = false;
                    constraintContext.buildConstraintViolationWithTemplate(
                            "{" + INTERFACE_NAME + ".slug}").
                    addPropertyNode("slug").
                    addConstraintViolation();
                } else if (VocabularyDAO.isSlugInUse(slug)) {
                    valid = false;
                    constraintContext.buildConstraintViolationWithTemplate(
                            "{" + INTERFACE_NAME + ".slug.inUse}").
                    addPropertyNode("slug").
                    addConstraintViolation();
                }
            }
        } else {
            // For update, the slug must be specified, and for now, must
            // be the same as the existing slug. Future work is
            // to allow changing the slug.
            if (slug == null || (existingVocabulary != null
                    && !existingVocabulary.getSlug().equals(slug))) {
                valid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                        "{" + INTERFACE_NAME + ".update.slug}").
                addPropertyNode("slug").
                addConstraintViolation();
            }
        }

        // title: required
        valid = ValidationUtils.requireFieldNotEmptyString(
                INTERFACE_NAME,
                newVocabulary.getTitle(), "title",
                constraintContext, valid);

        // If slug was not specified, but the title was, then pass
        // the title through slug generation and check that instead.
        if (slug == null && newVocabulary.getTitle() != null) {
            String slugFromTitle =
                    SlugGenerator.generateSlug(newVocabulary.getTitle());
            if (VocabularyDAO.isSlugInUse(slugFromTitle)) {
                valid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                    "{" + INTERFACE_NAME
                    + ".title.slugInUse}").
                addPropertyNode("title").
                addConstraintViolation();
            }
        }

        // acronym: optional

        // description: required, and must be valid HTML
        valid = ValidationUtils.requireFieldNotEmptyString(
                INTERFACE_NAME,
                newVocabulary.getDescription(), "description",
                constraintContext, valid);
        valid = ValidationUtils.requireFieldValidHTML(
                INTERFACE_NAME,
                newVocabulary.getDescription(), "description",
                constraintContext, valid);

        // note: optional, but, if specified, must be valid HTML
        valid = ValidationUtils.requireFieldValidHTML(
                INTERFACE_NAME,
                newVocabulary.getNote(), "note",
                constraintContext, valid);

        // subject: at least one from ANZSRC-FOR required
        valid = isValidSubjects(valid, newVocabulary, constraintContext);

        // primaryLanguage: required, and must come from the list
        String primaryLanguage = newVocabulary.getPrimaryLanguage();
        valid = ValidationUtils.
                requireFieldNotEmptyStringAndSatisfiesPredicate(
                        INTERFACE_NAME, primaryLanguage, "primaryLanguage",
                        Languages::isValidLanguage, constraintContext, valid);
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
                            INTERFACE_NAME,
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
            constraintContext.buildConstraintViolationWithTemplate(
                    "{" + INTERFACE_NAME + ".otherLanguage.duplicate}").
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
                constraintContext.buildConstraintViolationWithTemplate(
                        "{" + INTERFACE_NAME + ".poolpartyProject.serverId}").
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
                    INTERFACE_NAME,
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
            constraintContext.buildConstraintViolationWithTemplate(
                    "{" + INTERFACE_NAME + ".topConcept.duplicate}").
            addPropertyNode("topConcept").
            addConstraintViolation();
        }

        // creationDate: required
        // Must match a regular expression: YYYY, or YYYY-MM, or YYYY-MM-DD.
        valid = ValidationUtils.requireFieldValidDate(
                INTERFACE_NAME,
                newVocabulary.getCreationDate(), "creationDate", false,
                constraintContext, valid);

        // revisionCycle: optional

        // relatedEntityRef
        valid = isValidRelatedEntityRefs(valid, newVocabulary,
                constraintContext);

        // relatedVocabularyRef
        valid = isValidRelatedVocabularyRefs(valid, newVocabulary,
                constraintContext);

        // version
        int versionIndex = 0;
        // Keep track of version slugs, so we can check if there are
        // duplicates.
        // Check if there _would_ be duplicate slugs, in the
        // case that slugs are not specified: i.e., because there
        // are duplicate titles, or, there is a version with a
        // title but no slug, where the generated slug matches another
        // version where the slug _is_ specified.
        // For an update, we don't check _here_ that any version Ids
        // provided do in fact belong to this vocabulary; this is done in
        // the submethods of VersionModel.applyChanges().
        Set<String> versionSlugs = new HashSet<>();
        for (Iterator<Version> it =
                newVocabulary.getVersion().iterator();
                it.hasNext(); versionIndex++) {
            Version version = it.next();
            if (!isValidVersion(newVocabulary, versionIndex, version,
                    constraintContext)) {
                valid = false;
            }
            String versionSlug = version.getSlug();
            if (versionSlug == null) {
                versionSlug = SlugGenerator.generateSlug(version.getTitle());
            }
            if (versionSlugs.contains(versionSlug)) {
                valid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                        "{" + INTERFACE_NAME
                        + ".version.slug.duplicate}").
                    addPropertyNode("version").addBeanNode().
                    inIterable().atIndex(versionIndex).
                    addConstraintViolation();
            }
            versionSlugs.add(versionSlug);
        }

        // what else?
        if (!valid) {
            // A custom constraint violation has been added, so don't also
            // add the default violation.
            constraintContext.disableDefaultConstraintViolation();
        }

        return valid;
    }


    /** Validate the vocabulary Id.
     * @param valid The current validity status.
     * @param newVocabulary The vocabulary that is being validated.
     * @param constraintContext The constraint context, into which
     *      validation errors are reported.
     * @return The updated validity status.
     */
    private boolean isValidVocabularyId(final boolean valid,
            final Vocabulary newVocabulary,
            final ConstraintValidatorContext constraintContext) {
        boolean newValid = valid;
        if (mode == ValidationMode.CREATE) {
            // For CREATE: required _not_ to be provided.
            if (newVocabulary.getId() != null) {
                newValid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                        "{" + INTERFACE_NAME + ".create.id}").
                addPropertyNode("id").
                addConstraintViolation();
            }
        } else {
            // For UPDATE: required.
            if (newVocabulary.getId() == null || newVocabulary.getId() <= 0) {
                newValid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                        "{" + INTERFACE_NAME + ".update.id}").
                addPropertyNode("id").
                addConstraintViolation();
            }
        }
        return newValid;
    }

    /** Validate the subject elements.
     * @param valid The current validity status.
     * @param newVocabulary The vocabulary that is being validated.
     * @param constraintContext The constraint context, into which
     *      validation errors are reported.
     * @return The updated validity status.
     */
    private boolean isValidSubjects(final boolean valid,
            final Vocabulary newVocabulary,
            final ConstraintValidatorContext constraintContext) {
        boolean newValid = valid;
        boolean validSubjects = false;
        // Keep track of the local subjects that were specified, so
        // we can check for duplicates at the end. We keep track of their
        // labels.
        int numLocalSubjectsSpecified = 0;
        Set<String> localSubjectsSpecified = new HashSet<>();
        // Also, keep track of the non-local subjects, so we can check
        // for duplicates of them, too. We keep track of their IRIs.
        int numNonLocalSubjectsSpecified = 0;
        Set<String> nonLocalSubjectsSpecified = new HashSet<>();
        int subjectIndex = 0;
        for (Iterator<Subject> it = newVocabulary.getSubject().iterator();
                it.hasNext(); subjectIndex++) {
            Subject subject = it.next();
            if (SubjectSources.LOCAL.equals(subject.getSource())) {
                numLocalSubjectsSpecified++;
                localSubjectsSpecified.add(subject.getLabel());
            } else {
                numNonLocalSubjectsSpecified++;
                nonLocalSubjectsSpecified.add(subject.getIri());
            }
            final int index = subjectIndex;
            newValid = ValidationUtils.
                    requireFieldNotEmptyStringAndSatisfiesPredicate(
                            INTERFACE_NAME,
                            subject.getSource(), "subject.source",
                            SubjectSources::isValidSubjectSource,
                            constraintContext,
                            cvb -> cvb.addPropertyNode("subject").
                                addPropertyNode("source").inIterable().
                                atIndex(index).addConstraintViolation(),
                                valid);
            newValid = ValidationUtils.
                    requireFieldNotEmptyString(
                            INTERFACE_NAME,
                            subject.getLabel(), "subject.label",
                            constraintContext,
                            cvb -> cvb.addPropertyNode("subject").
                                addPropertyNode("label").inIterable().
                                atIndex(index).addConstraintViolation(),
                                valid);
            if (SubjectSources.subjectRequiresIRI(subject)
                    && !SubjectSources.subjectHasValidIRI(subject)) {
                newValid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                    "{" + INTERFACE_NAME + ".subject.unknown}").
                addPropertyNode("subject").addBeanNode().inIterable().
                atIndex(index).addConstraintViolation();
            }
            if (SubjectSources.ANZSRC_FOR.equals(subject.getSource())) {
                validSubjects = true;
            }
        }
        if (!validSubjects) {
            newValid = false;
            constraintContext.buildConstraintViolationWithTemplate(
                "{" + INTERFACE_NAME + ".subject.noAnzsrcFor}").
            addPropertyNode("subject").addConstraintViolation();
        }
        if (numLocalSubjectsSpecified != localSubjectsSpecified.size()) {
            newValid = false;
            constraintContext.buildConstraintViolationWithTemplate(
                "{" + INTERFACE_NAME + ".subject.duplicateLocal}").
            addPropertyNode("subject").addConstraintViolation();
        }
        if (numNonLocalSubjectsSpecified != nonLocalSubjectsSpecified.size()) {
            newValid = false;
            constraintContext.buildConstraintViolationWithTemplate(
                "{" + INTERFACE_NAME + ".subject.duplicateNonLocal}").
            addPropertyNode("subject").addConstraintViolation();
        }
        return newValid;
    }

    /** Validate the related entity ref elements.
     * @param valid The current validity status.
     * @param newVocabulary The vocabulary that is being validated.
     * @param constraintContext The constraint context, into which
     *      validation errors are reported.
     * @return The updated validity status.
     */
    private boolean isValidRelatedEntityRefs(final boolean valid,
            final Vocabulary newVocabulary,
            final ConstraintValidatorContext constraintContext) {
        boolean newValid = valid;
        // Boolean to keep track of when we have seen a publisher.
        boolean publisherSeen = false;
        // We keep track of the related entity relations we've seen.
        // As we go, we check that there are no duplicate related entity ids.
        // For each related entity, we check that there are no duplicate
        // relations.
        // Set of RE IDs we have seen.
        Set<Integer> reIDs = new HashSet<>();
        int reIndex = 0;
        for (Iterator<RelatedEntityRef> it =
                newVocabulary.getRelatedEntityRef().iterator();
                it.hasNext(); reIndex++) {
            RelatedEntityRef reRef = it.next();
            // Check not a previously-seen ID.
            if (reIDs.contains(reRef.getId())) {
                newValid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                    "{" + INTERFACE_NAME + ".relatedEntityRef.duplicateID}").
                addPropertyNode("relatedEntityRef").addBeanNode().inIterable().
                atIndex(reIndex).
                addConstraintViolation();
            }
            reIDs.add(reRef.getId());
            // Check for an existing RE.
            RelatedEntity re =
                    RelatedEntityDAO.getCurrentRelatedEntityByRelatedEntityId(
                            reRef.getId());
            if (re == null) {
                newValid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                    "{" + INTERFACE_NAME + ".relatedEntityRef.unknown}").
                addPropertyNode("relatedEntityRef").addBeanNode().inIterable().
                atIndex(reIndex).
                addConstraintViolation();
                continue;
            }

            // Check no duplicate relations, and that each is allowed.
            // Set of _valid_ relations we have seen for this RE.
            Set<RelatedEntityRelation> reRelations = new HashSet<>();
            for (RelatedEntityRelation relation : reRef.getRelation()) {
                if (reRelations.contains(relation)) {
                    newValid = false;
                    constraintContext.buildConstraintViolationWithTemplate(
                        "{" + INTERFACE_NAME
                        + ".relatedEntityRef.duplicateRelation}").
                    addPropertyNode("relatedEntityRef").
                    addBeanNode().inIterable().atIndex(reIndex).
                    addConstraintViolation();
                }
                // NB: If the relation type specified in the input is not even
                // in the enumerated type (e.g., it is misspelled), then
                // reRef.getRelation() returns null ... and you correctly
                // get an error generated for this.
                if (ValidationUtils.isAllowedRelation(re.getType(),
                        relation)) {
                    reRelations.add(relation);
                } else {
                    newValid = false;
                    constraintContext.buildConstraintViolationWithTemplate(
                            "{" + INTERFACE_NAME
                            + ".relatedEntityRef.badRelation}").
                    addPropertyNode("relatedEntityRef").addBeanNode().
                    inIterable().atIndex(reIndex).
                    addConstraintViolation();
                    continue;
                }
                if (relation == RelatedEntityRelation.PUBLISHED_BY) {
                    publisherSeen = true;
                }
            }
            if (reRelations.isEmpty()) {
                newValid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                    "{" + INTERFACE_NAME + ".relatedEntityRef.noRelation}").
                addPropertyNode("relatedEntityRef").
                addBeanNode().inIterable().atIndex(reIndex).
                addConstraintViolation();
            }
        }

        // Ensure at least one publisher has been specified.
        if (!publisherSeen) {
            newValid = false;
            constraintContext.buildConstraintViolationWithTemplate(
                "{" + INTERFACE_NAME + ".relatedEntityRef.noPublisher}").
            addPropertyNode("relatedEntityRef").
            addConstraintViolation();
        }
        return newValid;
    }

    /** Validate the related vocabulary ref elements.
     * @param valid The current validity status.
     * @param newVocabulary The vocabulary that is being validated.
     * @param constraintContext The constraint context, into which
     *      validation errors are reported.
     * @return The updated validity status.
     */
    private boolean isValidRelatedVocabularyRefs(final boolean valid,
            final Vocabulary newVocabulary,
            final ConstraintValidatorContext constraintContext) {
        boolean newValid = valid;
        // We keep track of the related vocabulary relations we've seen.
        // As we go, we check that there are no duplicate vocabulary ids.
        // For each related vocabulary, we check that there are no duplicate
        // relations.
        // Set of vocabulary IDs we have seen.
        Set<Integer> rvIDs = new HashSet<>();
        int rvIndex = 0;
        for (Iterator<RelatedVocabularyRef> it =
                newVocabulary.getRelatedVocabularyRef().iterator();
                it.hasNext(); rvIndex++) {
            RelatedVocabularyRef rvRef = it.next();
            // Check not a previously-seen ID.
            if (rvIDs.contains(rvRef.getId())) {
                newValid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                    "{" + INTERFACE_NAME
                    + ".relatedVocabularyRef.duplicateID}").
                addPropertyNode("relatedVocabularyRef").addBeanNode().
                inIterable().atIndex(rvIndex).
                addConstraintViolation();
            }
            rvIDs.add(rvRef.getId());
            // Check for an existing vocabulary.
            au.org.ands.vocabs.registry.db.entity.Vocabulary rv =
                    VocabularyDAO.getCurrentVocabularyByVocabularyId(
                            rvRef.getId());
            if (rv == null) {
                newValid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                    "{" + INTERFACE_NAME
                    + ".relatedVocabularyRef.unknown}").
                addPropertyNode("relatedVocabularyRef").addBeanNode().
                inIterable().atIndex(rvIndex).
                addConstraintViolation();
                continue;
            }

            // Check no duplicate relations, and that each is allowed.
            // Set of _valid_ relations we have seen for this vocabulary.
            Set<RelatedVocabularyRelation> rvRelations = new HashSet<>();
            for (RelatedVocabularyRelation relation : rvRef.getRelation()) {
                if (rvRelations.contains(relation)) {
                    newValid = false;
                    constraintContext.buildConstraintViolationWithTemplate(
                        "{" + INTERFACE_NAME
                        + ".relatedVocabularyRef.duplicateRelation}").
                    addPropertyNode("relatedVocabularyRef").
                    addBeanNode().inIterable().atIndex(rvIndex).
                    addConstraintViolation();
                }
                // NB: If the relation type specified in the input is not even
                // in the enumerated type (e.g., it is misspelled), then
                // reRef.getRelation() returns null ... and you correctly
                // get an error generated for this.
                if (ValidationUtils.isAllowedRelation(relation)) {
                    rvRelations.add(relation);
                } else {
                    newValid = false;
                    constraintContext.buildConstraintViolationWithTemplate(
                            "{" + INTERFACE_NAME
                            + ".relatedVocabularyRef.badRelation}").
                    addPropertyNode("relatedVocabularyRef").addBeanNode().
                    inIterable().atIndex(rvIndex).
                    addConstraintViolation();
                    continue;
                }
            }
            if (rvRelations.isEmpty()) {
                newValid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                    "{" + INTERFACE_NAME
                    + ".relatedVocabularyRef.noRelation}").
                addPropertyNode("relatedVocabularyRef").
                addBeanNode().inIterable().atIndex(rvIndex).
                addConstraintViolation();
            }
        }
        return newValid;
    }

    /** Validate a proposed new version.
     * @param newVocabulary The vocabulary that is being validated.
     * @param versionIndex The index of the version being created.
     * @param newVersion The new version that is being created.
     * @param constraintContext The constraint context, into which
     *      validation errors are reported.
     * @return true, if newVersion represents a valid version.
     */
    private boolean isValidVersion(final Vocabulary newVocabulary,
            final int versionIndex,
            final Version newVersion,
            final ConstraintValidatorContext constraintContext) {
        boolean valid = true;
        logger.info("In CheckVocabularyImpl.isValidVersion("
                + versionIndex + ")");

        // Table of contents of this method:
        // id
        // status
        // title
        // slug
        // note
        // releaseDate
        // doPoolpartyHarvest
        // doImport
        // doPublish
        // accessPoint

        // id: mode-specific validation.
        valid = isValidVersionId(valid, newVersion, versionIndex,
                constraintContext);

        // status: required
        valid = ValidationUtils.requireFieldNotNull(
                INTERFACE_NAME,
                newVersion.getStatus(), "version.status",
                constraintContext,
                cvb -> cvb.addPropertyNode("version").
                    addPropertyNode("status").inIterable().
                    atIndex(versionIndex).addConstraintViolation(),
                valid);

        // title
        valid = ValidationUtils.requireFieldNotEmptyString(
                INTERFACE_NAME,
                newVersion.getTitle(), "version.title",
                constraintContext,
                cvb -> cvb.addPropertyNode("version").
                    addPropertyNode("title").inIterable().
                    atIndex(versionIndex).addConstraintViolation(),
                valid);

        // slug: optional, but if specified, must be in the right format
        String slug = newVersion.getSlug();
        if (slug != null) {
            if (!ValidationUtils.isValidSlug(slug)) {
                valid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                    "{" + INTERFACE_NAME + ".version.slug}").
                addPropertyNode("version").
                addPropertyNode("slug").inIterable().atIndex(versionIndex).
                addConstraintViolation();
            }
        }

        // note: optional, but, if specified, must be valid HTML
        valid = ValidationUtils.requireFieldValidHTML(
                INTERFACE_NAME,
                newVersion.getNote(), "version.note",
                constraintContext,
                cvb -> cvb.addPropertyNode("version").
                    addPropertyNode("note").inIterable().
                    atIndex(versionIndex).addConstraintViolation(),
                valid);

        // releaseDate: required
        // Must match a regular expression: YYYY, or YYYY-MM, or YYYY-MM-DD.
        valid = ValidationUtils.requireFieldValidDate(
                INTERFACE_NAME,
                newVersion.getReleaseDate(), "version.creationDate", false,
                constraintContext,
                cvb -> cvb.addPropertyNode("version").
                    addPropertyNode("releaseDate").inIterable().
                    atIndex(versionIndex).addConstraintViolation(),
                valid);

        // doPoolpartyHarvest: only allow it to be true if
        // this is a "PoolParty project", i.e., a PoolParty project ID
        // has been specified for the vocabulary.
        if (BooleanUtils.isTrue(newVersion.isDoPoolpartyHarvest())
                && newVocabulary.getPoolpartyProject() == null) {
            valid = false;
            constraintContext.buildConstraintViolationWithTemplate(
                    "{" + INTERFACE_NAME + ".version.doPoolpartyHarvest}").
                addPropertyNode("version").
                addPropertyNode("doPoolpartyHarvest").inIterable().
                atIndex(versionIndex).
                addConstraintViolation();
        }

        // doImport

        // doPublish

        // accessPoint
        // TO DO: for updates, check that any AP Ids do in fact
        // belong to this version. (Including: check that if an AP Id
        // is specified, so was the version Id!) Hmm, perhaps do this in
        // the model classes.
        int accessPointIndex = 0;
        for (Iterator<AccessPoint> it =
                newVersion.getAccessPoint().iterator();
                it.hasNext(); accessPointIndex++) {
            AccessPoint ap = it.next();
            if (!isValidAccessPoint(versionIndex, newVersion.getId(),
                    accessPointIndex, ap, constraintContext)) {
                valid = false;
            }
        }

        return valid;
    }

    /** Validate the version Id.
     * @param valid The current validity status.
     * @param newVersion The version that is being validated
     * @param versionIndex The index of the version within the vocabulary
     * @param constraintContext The constraint context, into which
     *      validation errors are reported.
     * @return The updated validity status.
     */
    private boolean isValidVersionId(final boolean valid,
            final Version newVersion,
            final int versionIndex,
            final ConstraintValidatorContext constraintContext) {
        if (mode == ValidationMode.UPDATE) {
            // For UPDATE: no further check here.
            // User can update existing versions, and add new ones.
            return valid;
        }
        boolean newValid = valid;
        if (mode == ValidationMode.CREATE) {
            // For CREATE: required _not_ to be provided.
            if (newVersion.getId() != null) {
                newValid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                        "{" + INTERFACE_NAME + ".version.id}").
                addPropertyNode("version").
                addPropertyNode("id").inIterable().atIndex(versionIndex).
                addConstraintViolation();
            }
        }
        return newValid;
    }

    /** Validate a proposed new access point.
     * @param versionIndex The index of the version being created.
     * @param newVersionId The value of id specified in newVersion.
     * @param accessPointIndex The index of the access point being created.
     * @param newAccessPoint The new access point that is being created.
     * @param constraintContext The constraint context, into which
     *      validation errors are reported.
     * @return true, if newAccessPoint represents a valid version.
     */
    @SuppressWarnings("checkstyle:MethodLength")
    private boolean isValidAccessPoint(final int versionIndex,
            final Integer newVersionId,
            final int accessPointIndex,
            final AccessPoint newAccessPoint,
            final ConstraintValidatorContext constraintContext) {
        boolean valid = true;
        logger.info("In CheckVocabularyImpl.isValidAccessPoint("
                + accessPointIndex + ")");

        // Table of contents of this method:
        // id
        // discriminator

        // id: mode-specific validation.
        valid = isValidAccessPointId(valid, versionIndex, newVersionId,
                newAccessPoint, accessPointIndex, constraintContext);

        // discriminator
        if (newAccessPoint.getDiscriminator() == null) {
            valid = false;
            constraintContext.buildConstraintViolationWithTemplate(
                    "{" + INTERFACE_NAME + ".accessPoint.discriminator}").
                addPropertyNode("version").
                addPropertyNode("accessPoint").
                inIterable().atIndex(versionIndex).
                addPropertyNode("discriminator").
                inIterable().atIndex(accessPointIndex).
                addConstraintViolation();
            // In this case, we can't go any further.
            return valid;
        }

        switch (newAccessPoint.getDiscriminator()) {
        case AP_API_SPARQL:
            ApApiSparql apApiSparql = newAccessPoint.getApApiSparql();
            if (apApiSparql == null) {
                valid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                        "{" + INTERFACE_NAME + ".accessPoint.apApiSparql}").
                    addPropertyNode("version").
                    addPropertyNode("accessPoint").
                    inIterable().atIndex(versionIndex).
                    addPropertyNode("apApiSparql").
                    inIterable().atIndex(accessPointIndex).
                    addConstraintViolation();
                // In this case, we can't go any further.
                break;
            }
            if (newAccessPoint.getSource() != ApSource.USER) {
                valid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                        "{" + INTERFACE_NAME
                        + ".accessPoint.apApiSparql.source}").
                    addPropertyNode("version").
                    addPropertyNode("accessPoint").
                    inIterable().atIndex(versionIndex).
                    addPropertyNode("apApiSparql").
                    inIterable().atIndex(accessPointIndex).
                    addPropertyNode("source").
                    addConstraintViolation();
            }
            valid = ValidationUtils.
                    requireFieldNotEmptyStringAndSatisfiesPredicate(
                    INTERFACE_NAME,
                    apApiSparql.getUrl(), "accessPoint.apApiSparql.url",
                    ValidationUtils::isValidURL,
                    constraintContext,
                    cvb -> cvb.addPropertyNode("version").
                    addPropertyNode("accessPoint").
                    inIterable().atIndex(versionIndex).
                    addPropertyNode("apApiSparql").
                    inIterable().atIndex(accessPointIndex).
                    addPropertyNode("url").
                    addConstraintViolation(),
                    valid);
            break;
        case AP_SISSVOC:
            ApSissvoc apSissvoc = newAccessPoint.getApSissvoc();
            if (apSissvoc == null) {
                valid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                        "{" + INTERFACE_NAME + ".accessPoint.apSissvoc}").
                    addPropertyNode("version").
                    addPropertyNode("accessPoint").
                    inIterable().atIndex(versionIndex).
                    addPropertyNode("apSissvoc").
                    inIterable().atIndex(accessPointIndex).
                    addConstraintViolation();
                // In this case, we can't go any further.
                break;
            }
            if (newAccessPoint.getSource() != ApSource.USER) {
                valid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                        "{" + INTERFACE_NAME
                        + ".accessPoint.apSissvoc.source}").
                    addPropertyNode("version").
                    addPropertyNode("accessPoint").
                    inIterable().atIndex(versionIndex).
                    addPropertyNode("apSissvoc").
                    inIterable().atIndex(accessPointIndex).
                    addPropertyNode("source").
                    addConstraintViolation();
            }
            valid = ValidationUtils.
                    requireFieldNotEmptyStringAndSatisfiesPredicate(
                    INTERFACE_NAME,
                    apSissvoc.getUrlPrefix(), "accessPoint.apSissvoc.urlPrefix",
                    ValidationUtils::isValidURL,
                    constraintContext,
                    cvb -> cvb.addPropertyNode("version").
                    addPropertyNode("accessPoint").
                    inIterable().atIndex(versionIndex).
                    addPropertyNode("apSissvoc").
                    inIterable().atIndex(accessPointIndex).
                    addPropertyNode("url").
                    addConstraintViolation(),
                    valid);
            break;
        case AP_WEB_PAGE:
            ApWebPage apWebPage = newAccessPoint.getApWebPage();
            if (apWebPage == null) {
                valid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                        "{" + INTERFACE_NAME + ".accessPoint.apWebPage}").
                    addPropertyNode("version").
                    addPropertyNode("accessPoint").
                    inIterable().atIndex(versionIndex).
                    addPropertyNode("apWebPage").
                    inIterable().atIndex(accessPointIndex).
                    addConstraintViolation();
                // In this case, we can't go any further.
                break;
            }
            valid = ValidationUtils.
                    requireFieldNotEmptyStringAndSatisfiesPredicate(
                    INTERFACE_NAME,
                    apWebPage.getUrl(), "accessPoint.apWebPage.url",
                    ValidationUtils::isValidURL,
                    constraintContext,
                    cvb -> cvb.addPropertyNode("version").
                    addPropertyNode("accessPoint").
                    inIterable().atIndex(versionIndex).
                    addPropertyNode("apWebPage").
                    inIterable().atIndex(accessPointIndex).
                    addPropertyNode("url").
                    addConstraintViolation(),
                    valid);
            break;
        default:
            valid = false;
            constraintContext.buildConstraintViolationWithTemplate(
                    "{" + INTERFACE_NAME
                    + ".accessPoint.discriminator.allowed}").
                addPropertyNode("version").
                addPropertyNode("accessPoint").
                inIterable().atIndex(versionIndex).
                addPropertyNode("discriminator").
                inIterable().atIndex(accessPointIndex).
                addConstraintViolation();
        }

        return valid;
    }

    /** Validate the access point Id.
     * @param valid The current validity status.
     * @param versionIndex The index of the version within the vocabulary
     * @param newVersionId The value of id specified in newVersion. Used to
     *      determine if this is a new or an existing version.
     * @param newAccessPoint The access point that is being validated.
     * @param accessPointIndex The index of the access point within
     *      the version.
     * @param constraintContext The constraint context, into which
     *      validation errors are reported.
     * @return The updated validity status.
     */
    private boolean isValidAccessPointId(final boolean valid,
            final int versionIndex, final Integer newVersionId,
            final AccessPoint newAccessPoint, final int accessPointIndex,
            final ConstraintValidatorContext constraintContext) {
        /* Note: _we_ can't distinguish omitting an id,
         * from specifying an id of 0. */
        boolean newValid = valid;
        if (mode == ValidationMode.CREATE) {
            if (newAccessPoint.getId() != null) {
                /* User can't specify an id for a new access point. */
                newValid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                        "{" + INTERFACE_NAME + ".accessPoint.id}").
                addPropertyNode("version").
                addPropertyNode("accessPoint").
                inIterable().atIndex(versionIndex).
                addPropertyNode("id").
                inIterable().atIndex(accessPointIndex).
                addConstraintViolation();
            }
        } else {
            if (newVersionId == null && newAccessPoint.getId() != null) {
                /* User can't specify an id for an access point of
                 * a new version. */
                newValid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                        "{" + INTERFACE_NAME + ".accessPoint.id}").
                addPropertyNode("version").
                addPropertyNode("accessPoint").
                inIterable().atIndex(versionIndex).
                addPropertyNode("id").
                inIterable().atIndex(accessPointIndex).
                addConstraintViolation();
            }
        }
        return newValid;
    }

}
