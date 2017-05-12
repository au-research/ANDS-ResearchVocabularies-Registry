/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.validation;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

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
    @SuppressWarnings("checkstyle:MethodLength")
    @Override
    public boolean isValid(final Vocabulary newVocabulary,
            final ConstraintValidatorContext constraintContext) {

        boolean valid = true;
        logger.info("In CheckNewVocabularyImpl.isValid()");

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

        // id: required _not_ to be provided
        if (newVocabulary.getId() != 0) {
            /* User can't specify an id for a new vocab.
             * Note: _we_ can't distinguish omitting an id,
             * from specifying an id of 0. */
            valid = false;
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
                constraintContext.buildConstraintViolationWithTemplate(
                    "{" + CheckNewVocabulary.INTERFACE_NAME + ".slug}").
                addPropertyNode("slug").
                addConstraintViolation();
            } else if (VocabularyDAO.isSlugInUse(slug)) {
                valid = false;
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

        // If slug was not specified, but the title was, then pass
        // the title through slug generation and check that instead.
        if (slug == null && newVocabulary.getTitle() != null) {
            String slugFromTitle =
                    SlugGenerator.generateSlug(newVocabulary.getTitle());
            if (VocabularyDAO.isSlugInUse(slugFromTitle)) {
                valid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                    "{" + CheckNewVocabulary.INTERFACE_NAME
                    + ".title.slugInUse}").
                addPropertyNode("title").
                addConstraintViolation();
            }
        }

        // acronym: optional

        // description: required, and must be valid HTML
        valid = ValidationUtils.requireFieldNotEmptyString(
                CheckNewVocabulary.INTERFACE_NAME,
                newVocabulary.getDescription(), "description",
                constraintContext, valid);
        valid = ValidationUtils.requireFieldValidHTML(
                CheckNewVocabulary.INTERFACE_NAME,
                newVocabulary.getDescription(), "description",
                constraintContext, valid);

        // note: optional, but, if specified, must be valid HTML
        valid = ValidationUtils.requireFieldValidHTML(
                CheckNewVocabulary.INTERFACE_NAME,
                newVocabulary.getNote(), "note",
                constraintContext, valid);

        // subject: at least one from ANZSRC-FOR required
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
            valid = ValidationUtils.
                    requireFieldNotEmptyStringAndSatisfiesPredicate(
                            CheckNewVocabulary.INTERFACE_NAME,
                            subject.getSource(), "subject.source",
                            SubjectSources::isValidSubjectSource,
                            constraintContext,
                            cvb -> cvb.addPropertyNode("subject").
                                addPropertyNode("source").inIterable().
                                atIndex(index).addConstraintViolation(),
                                valid);
            valid = ValidationUtils.
                    requireFieldNotEmptyString(
                            CheckNewVocabulary.INTERFACE_NAME,
                            subject.getLabel(), "subject.label",
                            constraintContext,
                            cvb -> cvb.addPropertyNode("subject").
                                addPropertyNode("label").inIterable().
                                atIndex(index).addConstraintViolation(),
                                valid);
            if (SubjectSources.subjectRequiresIRI(subject)
                    && !SubjectSources.subjectHasValidIRI(subject)) {
                valid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                    "{" + CheckNewVocabulary.INTERFACE_NAME
                    + ".subject.unknown}").
                addPropertyNode("subject").addBeanNode().inIterable().
                atIndex(index).
                addConstraintViolation();
            }
            if (SubjectSources.ANZSRC_FOR.equals(subject.getSource())) {
                validSubjects = true;
            }
        }
        if (!validSubjects) {
            valid = false;
            constraintContext.buildConstraintViolationWithTemplate(
                "{" + CheckNewVocabulary.INTERFACE_NAME
                + ".subject.noAnzsrcFor}").
            addPropertyNode("subject").
            addConstraintViolation();
        }
        if (numLocalSubjectsSpecified != localSubjectsSpecified.size()) {
            valid = false;
            constraintContext.buildConstraintViolationWithTemplate(
                "{" + CheckNewVocabulary.INTERFACE_NAME
                + ".subject.duplicateLocal}").
            addPropertyNode("subject").
            addConstraintViolation();
        }
        if (numNonLocalSubjectsSpecified != nonLocalSubjectsSpecified.size()) {
            valid = false;
            constraintContext.buildConstraintViolationWithTemplate(
                "{" + CheckNewVocabulary.INTERFACE_NAME
                + ".subject.duplicateNonLocal}").
            addPropertyNode("subject").
            addConstraintViolation();
        }

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
        boolean validRelatedEntities = false;
        // We keep track of the related entity relations we've seen
        // for each related entity. At the end, we will check that
        // there were no duplicate pairs (related entity, relation).
        Map<Integer, Set<RelatedEntityRelation>> allRERefs = new HashMap<>();
        int reIndex = 0;
        for (Iterator<RelatedEntityRef> it =
                newVocabulary.getRelatedEntityRef().iterator();
                it.hasNext(); reIndex++) {
            RelatedEntityRef reRef = it.next();
            if (!allRERefs.containsKey(reRef.getId())) {
                allRERefs.put(reRef.getId(), new HashSet<>());
            }
            allRERefs.get(reRef.getId()).add(reRef.getRelation());
            RelatedEntity re =
                    RelatedEntityDAO.getCurrentRelatedEntityByRelatedEntityId(
                            reRef.getId());
            if (re == null) {
                valid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                    "{" + CheckNewVocabulary.INTERFACE_NAME
                    + ".relatedEntityRef.unknown}").
                addPropertyNode("relatedEntityRef").addBeanNode().inIterable().
                atIndex(reIndex).
                addConstraintViolation();
                continue;
            }
            // NB: If the relation type specified in the input is not even
            // in the enumerated type (e.g., it is misspelled), then
            // reRef.getRelation() returns null ... and you correctly
            // get an error generated for this.
            if (!ValidationUtils.isAllowedRelation(re.getType(),
                    reRef.getRelation())) {
                valid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                        "{" + CheckNewVocabulary.INTERFACE_NAME
                        + ".relatedEntityRef.badRelation}").
                addPropertyNode("relatedEntityRef").addBeanNode().inIterable().
                atIndex(reIndex).
                addConstraintViolation();
                continue;
            }
            if (reRef.getRelation() == RelatedEntityRelation.PUBLISHED_BY) {
                validRelatedEntities = true;
            }
        }
        if (!validRelatedEntities) {
            valid = false;
            constraintContext.buildConstraintViolationWithTemplate(
                "{" + CheckNewVocabulary.INTERFACE_NAME
                + ".relatedEntityRef.noPublisher}").
            addPropertyNode("relatedEntityRef").
            addConstraintViolation();
        }
        // Add up the sizes of all the Sets within allRERefs.
        int totalRefs = allRERefs.values().stream().
                map(s -> s.size()).reduce(0, (a, b) -> a + b);
        if (totalRefs != newVocabulary.getRelatedEntityRef().size()) {
            // There's a duplicate instance of a relation to a related entity.
            valid = false;
            constraintContext.buildConstraintViolationWithTemplate(
                "{" + CheckNewVocabulary.INTERFACE_NAME
                + ".relatedEntityRef.duplicate}").
            addPropertyNode("relatedEntityRef").
            addConstraintViolation();
        }

        // relatedVocabularyRef
        // We keep track of the related vocabulary relations we've seen
        // for each related vocabulary. At the end, we will check that
        // there were no duplicate pairs (related vocabulary, relation).
        Map<Integer, Set<RelatedVocabularyRelation>> allRVRefs =
                new HashMap<>();
        int rvIndex = 0;
        for (Iterator<RelatedVocabularyRef> it =
                newVocabulary.getRelatedVocabularyRef().iterator();
                it.hasNext(); reIndex++) {
            RelatedVocabularyRef rvRef = it.next();
            if (!allRVRefs.containsKey(rvRef.getId())) {
                allRVRefs.put(rvRef.getId(), new HashSet<>());
            }
            allRVRefs.get(rvRef.getId()).add(rvRef.getRelation());
            au.org.ands.vocabs.registry.db.entity.Vocabulary vocab =
                    VocabularyDAO.getCurrentVocabularyByVocabularyId(
                            rvRef.getId());
            if (vocab == null) {
                valid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                    "{" + CheckNewVocabulary.INTERFACE_NAME
                    + ".relatedVocabularyRef.unknown}").
                addPropertyNode("relatedVocabularyRef").addBeanNode().
                inIterable().atIndex(rvIndex).
                addConstraintViolation();
                continue;
            }
            // NB: If the relation type specified in the input is not even
            // in the enumerated type (e.g., it is misspelled), then
            // reRef.getRelation() returns null ... and you correctly
            // get an error generated for this.
            if (rvRef.getRelation() == null) {
                valid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                        "{" + CheckNewVocabulary.INTERFACE_NAME
                        + ".relatedVocabularyRef.badRelation}").
                addPropertyNode("relatedVocabularyRef").addBeanNode().
                inIterable().atIndex(reIndex).
                addConstraintViolation();
                continue;
            }
        }
        // Add up the sizes of all the Sets within allRVRefs.
        totalRefs = allRVRefs.values().stream().
                map(s -> s.size()).reduce(0, (a, b) -> a + b);
        if (totalRefs != newVocabulary.getRelatedVocabularyRef().size()) {
            // There's a duplicate instance of a relation to a related entity.
            valid = false;
            constraintContext.buildConstraintViolationWithTemplate(
                "{" + CheckNewVocabulary.INTERFACE_NAME
                + ".relatedVocabularyRef.duplicate}").
            addPropertyNode("relatedVocabularyRef").
            addConstraintViolation();
        }

        // version
        int versionIndex = 0;
        // Keep track of version slugs, so we can check if there are
        // duplicates.
        // Check if there _would_ be duplicate slugs, in the
        // case that slugs are not specified: i.e., because there
        // are duplicate titles, or, there is a version with a
        // title but no slug, where the generated slug matches another
        // version where the slug _is_ specified.
        Set<String> versionSlugs = new HashSet<>();
        for (Iterator<Version> it =
                newVocabulary.getVersion().iterator();
                it.hasNext(); versionIndex++) {
            Version version = it.next();
            if (!isValidVersion(versionIndex, version, constraintContext)) {
                valid = false;
            }
            String versionSlug = version.getSlug();
            if (versionSlug == null) {
                versionSlug = SlugGenerator.generateSlug(version.getTitle());
            }
            if (versionSlugs.contains(versionSlug)) {
                valid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                        "{" + CheckNewVocabulary.INTERFACE_NAME
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

    /** Validate a proposed new version.
     * @param versionIndex The index of the version being created.
     * @param newVersion The new version that is being created.
     * @param constraintContext The constraint context, into which
     *      validation errors are reported.
     * @return true, if newVersion represents a valid version.
     */
    private boolean isValidVersion(final int versionIndex,
            final Version newVersion,
            final ConstraintValidatorContext constraintContext) {
        boolean valid = true;
        logger.info("In CheckNewVocabularyImpl.isValidVersion("
                + versionIndex + ")");

        // Table of contents of this method:
        // id
        // status
        // title
        // slug
        // note
        // releaseDate
        // doImport
        // doPublish
        // accessPoint

        // id: required _not_ to be provided
        if (newVersion.getId() != 0) {
            /* User can't specify an id for a new version.
             * Note: _we_ can't distinguish omitting an id,
             * from specifying an id of 0. */
            valid = false;
            constraintContext.buildConstraintViolationWithTemplate(
                    "{" + CheckNewVocabulary.INTERFACE_NAME + ".version.id}").
            addPropertyNode("version").
            addPropertyNode("id").inIterable().atIndex(versionIndex).
            addConstraintViolation();
        }

        // status: required
        valid = ValidationUtils.requireFieldNotNull(
                CheckNewVocabulary.INTERFACE_NAME,
                newVersion.getStatus(), "version.status",
                constraintContext,
                cvb -> cvb.addPropertyNode("version").
                    addPropertyNode("status").inIterable().
                    atIndex(versionIndex).addConstraintViolation(),
                valid);

        // title
        valid = ValidationUtils.requireFieldNotEmptyString(
                CheckNewVocabulary.INTERFACE_NAME,
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
                    "{" + CheckNewVocabulary.INTERFACE_NAME + ".version.slug}").
                addPropertyNode("version").
                addPropertyNode("slug").inIterable().atIndex(versionIndex).
                addConstraintViolation();
            }
        }

        // note: optional, but, if specified, must be valid HTML
        valid = ValidationUtils.requireFieldValidHTML(
                CheckNewVocabulary.INTERFACE_NAME,
                newVersion.getNote(), "version.note",
                constraintContext,
                cvb -> cvb.addPropertyNode("version").
                    addPropertyNode("note").inIterable().
                    atIndex(versionIndex).addConstraintViolation(),
                valid);

        // releaseDate: required
        // Must match a regular expression: YYYY, or YYYY-MM, or YYYY-MM-DD.
        valid = ValidationUtils.requireFieldValidDate(
                CheckNewVocabulary.INTERFACE_NAME,
                newVersion.getReleaseDate(), "version.creationDate", false,
                constraintContext,
                cvb -> cvb.addPropertyNode("version").
                    addPropertyNode("releaseDate").inIterable().
                    atIndex(versionIndex).addConstraintViolation(),
                valid);

        // doImport: we can't distinguish between a missing value,
        // and the value specified as false.

        // doPublish: we can't distinguish between a missing value,
        // and the value specified as false.

        // accessPoint
        int accessPointIndex = 0;
        for (Iterator<AccessPoint> it =
                newVersion.getAccessPoint().iterator();
                it.hasNext(); accessPointIndex++) {
            AccessPoint ap = it.next();
            if (!isValidAccessPoint(versionIndex, accessPointIndex,
                    ap, constraintContext)) {
                valid = false;
            }
        }

        return valid;
    }

    /** Validate a proposed new access point.
     * @param versionIndex The index of the version being created.
     * @param accessPointIndex The index of the access point being created.
     * @param newAccessPoint The new access point that is being created.
     * @param constraintContext The constraint context, into which
     *      validation errors are reported.
     * @return true, if newAccessPoint represents a valid version.
     */
    @SuppressWarnings("checkstyle:MethodLength")
    private boolean isValidAccessPoint(final int versionIndex,
            final int accessPointIndex,
            final AccessPoint newAccessPoint,
            final ConstraintValidatorContext constraintContext) {
        boolean valid = true;
        logger.info("In CheckNewVocabularyImpl.isValidAccessPoint("
                + accessPointIndex + ")");

        // Table of contents of this method:
        // id
        // discriminator

        // id: required _not_ to be provided
        if (newAccessPoint.getId() != 0) {
            /* User can't specify an id for a new access point.
             * Note: _we_ can't distinguish omitting an id,
             * from specifying an id of 0. */
            valid = false;
            constraintContext.buildConstraintViolationWithTemplate(
                    "{" + CheckNewVocabulary.INTERFACE_NAME
                    + ".accessPoint.id}").
            addPropertyNode("version").
            addPropertyNode("accessPoint").
            inIterable().atIndex(versionIndex).
            addPropertyNode("id").
            inIterable().atIndex(accessPointIndex).
            addConstraintViolation();
        }

        // discriminator
        if (newAccessPoint.getDiscriminator() == null) {
            valid = false;
            constraintContext.buildConstraintViolationWithTemplate(
                    "{" + CheckNewVocabulary.INTERFACE_NAME
                    + ".accessPoint.discriminator}").
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
                        "{" + CheckNewVocabulary.INTERFACE_NAME
                        + ".accessPoint.apApiSparql}").
                    addPropertyNode("version").
                    addPropertyNode("accessPoint").
                    inIterable().atIndex(versionIndex).
                    addPropertyNode("apApiSparql").
                    inIterable().atIndex(accessPointIndex).
                    addConstraintViolation();
                // In this case, we can't go any further.
                break;
            }
            if (apApiSparql.getSource() != ApSource.USER) {
                valid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                        "{" + CheckNewVocabulary.INTERFACE_NAME
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
                    CheckNewVocabulary.INTERFACE_NAME,
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
                        "{" + CheckNewVocabulary.INTERFACE_NAME
                        + ".accessPoint.apSissvoc}").
                    addPropertyNode("version").
                    addPropertyNode("accessPoint").
                    inIterable().atIndex(versionIndex).
                    addPropertyNode("apSissvoc").
                    inIterable().atIndex(accessPointIndex).
                    addConstraintViolation();
                // In this case, we can't go any further.
                break;
            }
            if (apSissvoc.getSource() != ApSource.USER) {
                valid = false;
                constraintContext.buildConstraintViolationWithTemplate(
                        "{" + CheckNewVocabulary.INTERFACE_NAME
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
                    CheckNewVocabulary.INTERFACE_NAME,
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
                        "{" + CheckNewVocabulary.INTERFACE_NAME
                        + ".accessPoint.apWebPage}").
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
                    CheckNewVocabulary.INTERFACE_NAME,
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
                    "{" + CheckNewVocabulary.INTERFACE_NAME
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

}
