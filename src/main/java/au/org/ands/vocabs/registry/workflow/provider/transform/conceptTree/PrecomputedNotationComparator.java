/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.provider.transform.conceptTree;

import java.util.Comparator;

/** A {@link Comparator} to use for sorting the narrower
 * concepts-or-references of a {@link Resource}, based on
 * an already-assigned notation sort order.
 */
public class PrecomputedNotationComparator
implements Comparator<ResourceOrRef> {

    /** {@inheritDoc} */
    @Override
    public int compare(final ResourceOrRef o1, final ResourceOrRef o2) {
        return o1.getNotationSortOrder() - o2.getNotationSortOrder();
    }
}
