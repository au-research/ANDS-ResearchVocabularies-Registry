/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.entity;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.enums.BrowseFlag;

/** Parse a list of BrowseFlags and present the results in a way that
 * can be more easily consumed.
 */
public class BrowseFlagsParsed {

    /** Logger for this class. */
    private final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Whether users should be offered the ability to sort by notation. */
    private boolean maySortByNotation = false;

    /** Whether users should be offered the ability to sort by notation.
     * @return Whether users should be offered the ability to sort by notation.
     */
    public boolean isMaySortByNotation() {
        return maySortByNotation;
    }

    /** In case {@link #maySortByNotation} is true, whether the
     * default sort order is by notation (true)
     * or by preferred label (false). */
    private boolean defaultSortByNotation = false;

    /** In case {@link #maySortByNotation} is true, whether the
     * default sort order is by notation (true)
     * or by preferred label (false).
     * @return Whether the default sort order is by notation (true)
     *      or by preferred label (false).
     */
    public boolean isDefaultSortByNotation() {
        return defaultSortByNotation;
    }

    /** If {@link maySortByNotation} is true, the format of
     * notation values. */
    private BrowseFlag notationFormat = null;

    /** If {@link maySortByNotation} is true, the format of
     * notation values.
     * @return If {@link maySortByNotation} is true, the format of
     *      notation values.
     */
    public BrowseFlag getNotationFormat() {
        return notationFormat;
    }

    /** In case {@link #maySortByNotation} is true, whether the
     * notation values should be displayed by default. */
    private boolean defaultDisplayNotation = false;

    /** In case {@link #maySortByNotation} is true, whether the
     * notation values should be displayed by default.
     * @return Whether the notation values should be displayed by default.
     */
    public boolean isDefaultDisplayNotation() {
        return defaultDisplayNotation;
    }

    /** Whether concept schemes should be taken into account. */
    private boolean includeConceptSchemes = false;

    /** Whether concept schemes should be taken into account.
     * @return Whether concept schemes should be taken into account.
     */
    public boolean isIncludeConceptSchemes() {
        return includeConceptSchemes;
    }

    /** Whether collections should be taken into account. */
    private boolean includeCollections = false;

    /** Whether collections should be taken into account.
     * @return Whether collections should be taken into account.
     */
    public boolean isIncludeCollections() {
        return includeCollections;
    }

    /** Whether resource IRIs are expected to resolve. */
    private boolean mayResolveResources = false;

    /** Whether resource IRIs are expected to resolve.
     * @return Whether resource IRIs are expected to resolve.
     */
    public boolean isMayResolveResources() {
        return mayResolveResources;
    }

    /** Parse and internalize a list of BrowseFlags. The results are
     * then available using the getters.
     * @param browseFlags The list of BrowseFlags to be parsed.
     */
    public BrowseFlagsParsed(final List<BrowseFlag> browseFlags) {
        // Parse the flags from the version.
        for (BrowseFlag browseFlag : browseFlags) {
            switch (browseFlag) {
            case DEFAULT_SORT_BY_NOTATION:
                defaultSortByNotation = true;
                break;
            case INCLUDE_COLLECTIONS:
                includeCollections = true;
                break;
            case INCLUDE_CONCEPT_SCHEMES:
                includeConceptSchemes = true;
                break;
            case MAY_RESOLVE_RESOURCES:
                mayResolveResources = true;
                break;
            case MAY_SORT_BY_NOTATION:
                maySortByNotation = true;
                break;
            case NOTATION_ALPHA:
            case NOTATION_DOTTED:
            case NOTATION_FLOAT:
                notationFormat = browseFlag;
                break;
            case DEFAULT_DISPLAY_NOTATION:
                defaultDisplayNotation = true;
                break;
            default:
                logger.error("Encountered an unexpected browse flag: "
                        + browseFlag);
                break;
            }
        }
    }

}
