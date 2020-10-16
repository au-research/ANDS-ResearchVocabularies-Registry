/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.validation;

/** Determine the validity of languages specified in vocabulary metadata.
 */
public final class Languages {

    /** Private constructor for a utility class. */
    private Languages() {
    }

    /** Decide if a String is a valid language tag.
     * @param testString The String to be validated.
     * @return true, if testString represents a valid language tag.
     */
    public static boolean isValidLanguage(final String testString) {
        return au.org.ands.vocabs.registry.utils.language.Languages.
                getParsedLanguage(testString).isValid();
    }

    /** Decide if a String is a valid language tag, that is not
     * the same as the primary language of a vocabulary.
     * @param primaryLanguage The primary language of the vocabulary.
     *      It may be null.
     * @param testString The String to be validated. It must not be null.
     * @return true, if testString represents a valid language tag.
     */
    public static boolean isValidLanguageNotPrimary(
            final String primaryLanguage,
            final String testString) {
        return au.org.ands.vocabs.registry.utils.language.Languages.
                getParsedLanguage(testString).isValid()
                && !testString.equals(primaryLanguage);
    }

}
