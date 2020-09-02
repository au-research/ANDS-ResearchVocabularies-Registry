/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.provider.transform.conceptTree;

import java.util.TreeSet;

import au.org.ands.vocabs.registry.enums.BrowseFlag;
import au.org.ands.vocabs.registry.workflow.provider.transform.ConceptTreeTransformProvider;

/** Class for representing the result of the transform. An
 * instance of this class is serialized for storage into the
 * file for the version artefact.
 */
public class ConceptResult {

    /** Get the value of the format of the JSON data. Invoked during
     * serialization into JSON.
     * @return The value of maySortByNotation.
     */
    public String getFormat() {
        return ConceptTreeTransformProvider.CONCEPTS_TREE_FORMAT;
    }

    /** The language of the preferred labels. */
    private String language;

    /** Set the value of language.
     * @param aLanguage The value of language to set.
     */
    public void setLanguage(final String aLanguage) {
        language = aLanguage;
    }

    /** Whether users will be offered the ability to sort by notation. */
    private boolean maySortByNotation = false;

    /** Set the value of maySortByNotation.
     * @param aMaySortByNotation The value of maySortByNotation to set.
     */
    public void setMaySortByNotation(final boolean aMaySortByNotation) {
        maySortByNotation = aMaySortByNotation;
    }

    /** Get the value of maySortByNotation. Invoked during
     * serialization into JSON.
     * @return The value of maySortByNotation.
     */
    public boolean getMaySortByNotation() {
        return maySortByNotation;
    }

    /** In case {@link #maySortByNotation} is true,
     * whether the default sort order is by notation (true)
     * or by preferred label (false). */
    private Boolean defaultSortByNotation;

    /** Set the value of defaultSortByNotation.
     * @param aDefaultSortByNotation The value of defaultSortByNotation
     *      to set.
     */
    public void setDefaultSortByNotation(
            final boolean aDefaultSortByNotation) {
        defaultSortByNotation = aDefaultSortByNotation;
    }

    /** Get the value of defaultSortByNotation. Invoked during
     * serialization into JSON.
     * @return The value of defaultSortByNotation.
     */
    public Boolean getDefaultSortByNotation() {
        return defaultSortByNotation;
    }

    /** If {@link maySortByNotation} is true, the format of
     * notation values. */
    private BrowseFlag notationFormat;

    /** Set the value of notationFormat.
     * @param aNotationFormat The value of notationFormat to set.
     */
    public void setNotationFormat(final BrowseFlag aNotationFormat) {
        notationFormat = aNotationFormat;
    }

    /** Get the value of notationFormat. Invoked during
     * serialization into JSON.
     * @return The value of notationFormat.
     */
    public BrowseFlag getNotationFormat() {
        return notationFormat;
    }

    /** Whether users will be offered the ability to resolve resource IRIs. */
    private boolean mayResolveResources = false;

    /** Set the value of mayResolveResources.
     * @param aMayResolveResources The value of mayResolveResources to set.
     */
    public void setMayResolveResources(final boolean aMayResolveResources) {
        mayResolveResources = aMayResolveResources;
    }

    /** Get the value of mayResolveResources. Invoked during
     * serialization into JSON.
     * @return The value of notationFormat.
     */
    public boolean getMayResolveResources() {
        return mayResolveResources;
    }

    /** Get the value of language. Invoked during serialization into JSON.
     * @return The value of language.
     */
    public String getLanguage() {
        return language;
    }

    /** The roots of the forest of concepts. */
    private TreeSet<ResourceOrRef> forest;

    /** Set the value of forest.
     * @param aForest The value of forest to set.
     */
    public void setForest(final TreeSet<ResourceOrRef> aForest) {
        forest = aForest;
    }

    /** Get the value of forest. Invoked during serialization into JSON.
     * Because the value is ordered, the result of serialization
     * will be a JSON array.
     * @return The value of forest.
     */
    public TreeSet<ResourceOrRef> getForest() {
        return forest;
    }

}
