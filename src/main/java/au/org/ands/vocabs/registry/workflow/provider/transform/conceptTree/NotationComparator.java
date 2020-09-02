/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.provider.transform.conceptTree;

import java.lang.invoke.MethodHandles;
import java.util.Comparator;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.enums.BrowseFlag;

/** A {@link Comparator} to use for sorting the narrower
 * concepts-or-references of a {@link Resource}, by notation,
 * where the format of the notation is specified as the parameter
 * to the constructor.
 */
public class NotationComparator
implements Comparator<Pair<ResourceOrRef, Integer>> {

    /** Logger for this class. */
    private final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** The format of the notation values; one of the NOTATION_...
     * values. */
    private BrowseFlag notationFormat;

    /** Constructor.
     * @param aNotationFormat The format of the notation values to use
     *      during comparison of values.
     */
    NotationComparator(final BrowseFlag aNotationFormat) {
        notationFormat = aNotationFormat;
    }

    /** Plain text alert message to indicate that because of an
     * error with a notation value, sorting by notation will not
     * be offered on the view page. */
    private static final String SORT_BY_NOTATION_NOT_OFFERED =
            "Sorting by notation will not be offered.";

    /** HTML alert message to indicate that there was an
     * error in one of the notation values, because a floating-point
     * value was expected, but there was more than one decimal point. */
    private static final String FLOAT_MULTIPLE_POINTS_HTML =
            "The notation format was specified to "
                    + "be floating-point numbers, but one of "
                    + "the notation values contains more than "
                    + "one decimal point. (Hint: is the format "
                    + "in fact multi-level hierarchical?) "
                    + SORT_BY_NOTATION_NOT_OFFERED;

    /** HTML alert message to indicate an error in parsing
     * a floating-point value. */
    private static final String FLOAT_OTHER_ERROR_HTML =
            "The notation format was specified to "
                    + "be floating-point numbers, but one of "
                    + "the notation values is not a "
                    + "floating-point number. "
                    + SORT_BY_NOTATION_NOT_OFFERED;

    /** HTML alert message to indicate an error in parsing
     * an Integer value as a component of a dotted notation. */
    private static final String DOTTED_ERROR_HTML =
            "The notation format was specified to "
                    + "be numeric hierarchical, but one of "
                    + "the notation values contains a component that "
                    + "is not a number. "
                    + SORT_BY_NOTATION_NOT_OFFERED;

    /** Create and return a {@link NotationException} based on
     * the contents of a {@link NumberFormatException} that has
     * been thrown when attempting to parse a floating-point value.
     * @param nfe The NumberFormatException that was thrown
     * @param n The String that was being parsed as a floating-point value.
     * @return a NotationException that incorporates an appropriate
     *      alert text.
     */
    private NotationException floatNotationException(
            final NumberFormatException nfe, final String n) {
        logger.error("Exception while parsing float value: " + n
                + "; message: " + nfe.getMessage());
        NotationException ne = new NotationException();
        if ("multiple points".equals(nfe.getMessage())) {
            ne.setAlertHTML(FLOAT_MULTIPLE_POINTS_HTML);
        } else {
            ne.setAlertHTML(FLOAT_OTHER_ERROR_HTML);
        }
        return ne;
    }

    /** Create and return a {@link NotationException} based on
     * the contents of a {@link NumberFormatException} that has
     * been thrown when attempting to parse a numeric value as an Integer.
     * @param nfe The NumberFormatException that was thrown
     * @param n The String that was being parsed as an Integer value.
     * @return a NotationException that incorporates an appropriate
     *      alert text.
     */
    private NotationException dottedNotationException(
            final NumberFormatException nfe, final String n) {
        logger.error("Exception while parsing integer: " + n
                + "; message: " + nfe.getMessage());
        NotationException ne = new NotationException();
        ne.setAlertHTML(DOTTED_ERROR_HTML);
        return ne;
    }

    /* {@inheritDoc} */
    @Override
    public int compare(final Pair<ResourceOrRef, Integer> o1,
            final Pair<ResourceOrRef, Integer> o2) {
        String n1 = o1.getLeft().getNotation();
        String n2 = o2.getLeft().getNotation();
        if (n1 == null || n1.isEmpty()) {
            // o1 has no notation. It will be sorted
            // after all concepts that _do_ have notations.
            if (n2 == null || n2.isEmpty()) {
                // Both concepts have null notations, so
                // fall back to the ordering produced by the original
                // prefLabel/IRI sort.
                return o1.getRight() - o2.getRight();
            }
            // o2 has a notation. o1 is sorted after it.
            return 1;
        }
        // o1 has a notation.
        if (n2 == null || n2.isEmpty()) {
            // o2 doesn't have a notation. It is sorted after o1.
            return -1;
        }
        // Both o1 and o2 have notations.
        int notationComparison;
        switch (notationFormat) {
        case NOTATION_ALPHA:
            notationComparison = n1.compareToIgnoreCase(n2);
            if (notationComparison != 0) {
                return notationComparison;
            }
            break;
        case NOTATION_DOTTED:
            // Adapted from:
            // https://stackoverflow.com/questions/198431/
            //         how-do-you-compare-two-version-strings-in-java
            String[] n1Parts = n1.split("\\.");
            String[] n2Parts = n2.split("\\.");
            int partsLength = Math.max(n1Parts.length, n2Parts.length);
            for (int i = 0; i < partsLength; i++) {
                int n1Part, n2Part;
                if (i < n1Parts.length) {
                    try {
                        n1Part = Integer.parseInt(n1Parts[i]);
                    } catch (NumberFormatException nfe) {
                        throw dottedNotationException(nfe, n1Parts[i]);
                    }
                } else {
                    n1Part = 0;
                }
                if (i < n2Parts.length) {
                    try {
                    n2Part = Integer.parseInt(n2Parts[i]);
                    } catch (NumberFormatException nfe) {
                        throw dottedNotationException(nfe, n2Parts[i]);
                    }
                } else {
                    n2Part = 0;
                }
                if (n1Part < n2Part) {
                    return -1;
                }
                if (n1Part > n2Part) {
                    return 1;
                }
            }
            break;
        case NOTATION_FLOAT:
            // Empty strings have been catered for above, so we won't
            // get an exception for that here.
            float f1;
            float f2;
            try {
                f1 = Float.parseFloat(n1);
            } catch (NumberFormatException nfe) {
                throw floatNotationException(nfe, n1);
            }
            try {
                f2 = Float.parseFloat(n2);
            } catch (NumberFormatException nfe) {
                throw floatNotationException(nfe, n2);
            }
            notationComparison = Float.compare(f1, f2);
            if (notationComparison != 0) {
                return notationComparison;
            }
            break;
        default:
            // Unknown notation format.
            logger.error("Illegal value for notation format: "
                    + notationFormat);
            throw new IllegalArgumentException(
                    "Illegal value for notation format: "
                            + notationFormat);
        }
        // Identical notations. Fall back to the ordering produced
        // by the original prefLabel/IRI sort.
        return o1.getRight() - o2.getRight();
    }
}
