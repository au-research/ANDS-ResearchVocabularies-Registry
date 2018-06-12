/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.log.utils;

import org.apache.commons.lang3.builder.DiffBuilder;
import org.apache.commons.lang3.builder.DiffResult;
import org.apache.commons.lang3.builder.ToStringStyle;

import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.entity.ComparisonUtils;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.db.internal.VersionJson;
import au.org.ands.vocabs.registry.db.internal.VocabularyJson;

/** Utility methods for comparing two registry database entities
 * of the same type.
 * These methods should be used, for example, in constructing
 * human-readable notifications of what has changed.
 *
 * Notes:
 * <ul><li>Whenever changes are made to the registry database structure, the
 * methods of this class should be reviewed to see if they also need to
 * be updated.</li>
   <li>Compare these methods with the corresponding methods in
   {@link ComparisonUtils}.</li>
 * </ul>
 */
public final class EntityDiffUtils {

    /** Private constructor for a utility class. */
    private EntityDiffUtils() {
    }

    // These are public fields.
    // The following fields are defined in alphabetical order
    // of field name.

    /** The name of the acronym field used as a diff label. */
    public static final String ACRONYM = "acronym";
    /** The name of the creation date field used as a diff label. */
    public static final String CREATION_DATE = "creation date";
    /** The name of the description field used as a diff label. */
    public static final String DESCRIPTION = "description";
    /** The name of the import field used as a diff label. */
    public static final String IMPORT = "import";
    /** The name of the licence field used as a diff label. */
    public static final String LICENCE = "licence";
    /** The name of the note field used as a diff label. */
    public static final String NOTE = "note";
    /** The name of the other languages field used as a diff label. */
    public static final String OTHER_LANGUAGES = "other languages";
    /** The name of the owner field used as a diff label. */
    public static final String OWNER = "owner";
    /** The name of the PoolParty Harvest field used as a diff label. */
    public static final String POOLPARTY_HARVEST = "PoolParty Harvest";
    /** The name of the primary language field used as a diff label. */
    public static final String PRIMARY_LANGUAGE = "primary language";
    /** The name of the publish field used as a diff label. */
    public static final String PUBLISH = "publish";
    /** The name of the release date field used as a diff label. */
    public static final String RELEASE_DATE = "release date";
    /** The name of the revision cycle field used as a diff label. */
    public static final String REVISION_CYCLE = "revision cycle";
    /** The name of the slug field used as a diff label. */
    public static final String SLUG = "slug";
    /** The name of the status field used as a diff label. */
    public static final String STATUS = "status";
    /** The name of the subjects field used as a diff label. */
    public static final String SUBJECTS = "subjects";
    /** The name of the title field used as a diff label. */
    public static final String TITLE = "title";
    /** The name of the top concepts field used as a diff label. */
    public static final String TOP_CONCEPTS = "top concepts";

    /** Compare two Vocabulary instances to see if they should be considered
     * "different" for the sake of notifications.
     * The result is a DiffResult, that contains a list of all
     * of the differences.
     * The fields that are compared are:
     * status, slug, owner, title, acronym, description, note,
     * revision cycle, creation date, primary language,
     * other languages, subjects, top concepts, and licence.
     * @param v1 A version that is an existing database entity.
     * @param v2 A version in registry schema format.
     * @return A DiffResult that contains a list of differences between
     *      v1 and v2.
     */
    public static DiffResult diffVocabularies(
            final Vocabulary v1, final Vocabulary v2) {
        VocabularyJson v1Json =
                JSONSerialization.deserializeStringAsJson(v1.getData(),
                        VocabularyJson.class);
        VocabularyJson v2Json =
                JSONSerialization.deserializeStringAsJson(v2.getData(),
                        VocabularyJson.class);
        return new DiffBuilder(v1, v2, ToStringStyle.SHORT_PREFIX_STYLE).
                append(STATUS, v1.getStatus(), v2.getStatus()).
                append(SLUG, v1.getSlug(), v2.getSlug()).
                append(OWNER, v1.getOwner(), v2.getOwner()).
                append(TITLE, v1Json.getTitle(), v2Json.getTitle()).
                append(ACRONYM, v1Json.getAcronym(), v2Json.getAcronym()).
                append(DESCRIPTION,
                        v1Json.getDescription(), v2Json.getDescription()).
                append(NOTE, v1Json.getNote(), v2Json.getNote()).
                append(REVISION_CYCLE,
                        v1Json.getRevisionCycle(), v2Json.getRevisionCycle()).
                append(CREATION_DATE,
                        v1Json.getCreationDate(), v2Json.getCreationDate()).
                append(PRIMARY_LANGUAGE, v1Json.getPrimaryLanguage(),
                        v2Json.getPrimaryLanguage()).
                append(OTHER_LANGUAGES, v1Json.getOtherLanguages(),
                        v2Json.getOtherLanguages()).
                append(SUBJECTS,
                        v1Json.getSubjects().stream().map(
                                SubjectsWithEquals::new).
                        toArray(),
                        v2Json.getSubjects().stream().map(
                                SubjectsWithEquals::new).
                        toArray()).
                append(TOP_CONCEPTS,
                        v1Json.getTopConcepts(), v2Json.getTopConcepts()).
                append(LICENCE, v1Json.getLicence(), v2Json.getLicence()).
                build();
    }

    /** Compare two Version instances to see if they should be considered
     * "different" for the sake of notifications.
     * The result is a DiffResult, that contains a list of all
     * of the differences.
     * The fields that are compared are:
     * status, slug, release date, title, note, and the flags:
     * PoolParty harvest, import, and publish.
     * @param v1 A version that is an existing database entity.
     * @param v2 A version in registry schema format.
     * @return A DiffResult that contains a list of differences between
     *      v1 and v2.
     */
    public static DiffResult diffVersions(final Version v1, final Version v2) {
        VersionJson v1Json =
                JSONSerialization.deserializeStringAsJson(v1.getData(),
                        VersionJson.class);
        VersionJson v2Json =
                JSONSerialization.deserializeStringAsJson(v2.getData(),
                        VersionJson.class);
        return new DiffBuilder(v1, v2, ToStringStyle.SHORT_PREFIX_STYLE).
                append(STATUS, v1.getStatus(), v2.getStatus()).
                append(SLUG, v1.getSlug(), v2.getSlug()).
                append(RELEASE_DATE,
                        v1.getReleaseDate(), v2.getReleaseDate()).
                append(TITLE, v1Json.getTitle(), v2Json.getTitle()).
                append(NOTE, v1Json.getNote(), v2Json.getNote()).
                append(POOLPARTY_HARVEST, v1Json.isDoPoolpartyHarvest(),
                        v2Json.isDoPoolpartyHarvest()).
                append(IMPORT, v1Json.isDoImport(), v2Json.isDoImport()).
                append(PUBLISH, v1Json.isDoPublish(),
                        v2Json.isDoPublish()).
                build();
    }

}
