/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.validation;

import java.util.HashSet;
import java.util.List;

import au.org.ands.vocabs.registry.db.dao.SubjectResolverEntryDAO;
import au.org.ands.vocabs.registry.db.dao.SubjectResolverSourceDAO;
import au.org.ands.vocabs.registry.db.entity.SubjectResolverEntry;
import au.org.ands.vocabs.registry.db.entity.SubjectResolverSource;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary.Subject;

/** Determine the validity of subject sources specified in vocabulary metadata.
 */
public final class SubjectSources {

    /** Private constructor for a utility class. */
    private SubjectSources() {
    }

    /** Subject source string for the ANZSRC-FOR vocabulary. Every
     * vocabulary must have a subject drawn from this vocabulary. */
    public static final String ANZSRC_FOR = "anzsrc-for";

    /** Subject source string for the ANZSRC-SEO vocabulary. */
    public static final String ANZSRC_SEO = "anzsrc-seo";

    /** Subject source string for the GCMD vocabulary. */
    public static final String GCMD = "gcmd";

    /** Subject source string for local subject terms. */
    public static final String LOCAL = "local";


    /** Set of valid subject sources. */
    private static final HashSet<String> VALID_SUBJECT_SOURCES =
            new HashSet<>();

    static {
        VALID_SUBJECT_SOURCES.add(ANZSRC_FOR);
        VALID_SUBJECT_SOURCES.add(ANZSRC_SEO);
        VALID_SUBJECT_SOURCES.add(GCMD);
        VALID_SUBJECT_SOURCES.add(LOCAL);
    }

    /** Set of subject sources for which IRIs can be resolved
     * to labels and notations. */
    private static final HashSet<String> RESOLVING_SUBJECT_SOURCES =
            new HashSet<>();

    /** Is {@link RESOLVING_SUBJECT_SOURCES} populated? */
    private static boolean resolvingSubjectSourcesPopulated = false;

    /** Decide if a String is a valid subject source.
     * @param testString The String to be validated.
     * @return true, if testString represents a valid subject source.
     */
    public static boolean isValidSubjectSource(final String testString) {
        return VALID_SUBJECT_SOURCES.contains(testString);
    }

    /** Decide if a Subject must include an IRI for it to be valid.
     * @param subject The subject to be examined, if it must include an IRI.
     * @return true, if subject must include an IRI.
     */
    public static boolean subjectRequiresIRI(final Subject subject) {
        if (!resolvingSubjectSourcesPopulated) {
            populateResolvingSubjectSources();
        }
        return RESOLVING_SUBJECT_SOURCES.contains(subject.getSource());
    }

    /** Populate the {@link RESOLVING_SUBJECT_SOURCES} set with
     * the sources for which we can resolve subjects.
     */
    private static void populateResolvingSubjectSources() {
        if (resolvingSubjectSourcesPopulated) {
            // Already done.
            return;
        }
        List<SubjectResolverSource> subjectResolverSources =
                SubjectResolverSourceDAO.getAllSubjectResolverSource();
        for (SubjectResolverSource subjectResolverSource
                : subjectResolverSources) {
            RESOLVING_SUBJECT_SOURCES.add(subjectResolverSource.getSource());
        }
    }

    /** Decide if a Subject that has a source for which we resolve subjects,
     * contains a valid IRI.
     * @param subject The subject to be examined, if it has a valid IRI for
     *      this subject source.
     * @return true, if subject has a valid IRI for this subject source.
     */
    public static boolean subjectHasValidIRI(final Subject subject) {
        return SubjectResolverEntryDAO.doesSubjectResolve(subject.getSource(),
                subject.getIri());
    }

    /** Resolve a Subject, based on its source and iri values.
     * The subject's label and notation are set to the values obtained
     * from the subject resolver.
     * If the Subject does not resolve (i.e., it can not be found
     * in the subject database), it is left untouched.
     * @param subject The subject to be resolved.
     */
    public static void resolveSubject(final Subject subject) {
        List<SubjectResolverEntry> subjectResolverEntries =
                SubjectResolverEntryDAO.getSubjectResolverEntries(
                        subject.getSource(), subject.getIri());
        if (subjectResolverEntries.size() == 1) {
            subject.setLabel(subjectResolverEntries.get(0).getLabel());
            subject.setNotation(subjectResolverEntries.get(0).getNotation());
        }
    }

}
