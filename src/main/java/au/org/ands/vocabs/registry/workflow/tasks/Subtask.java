/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.tasks;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import au.org.ands.vocabs.registry.enums.SubtaskType;

/** Representation of one workflow subtask. */
@XmlRootElement(name = "subtask")
/* This annotation means that properties other than the ones defined
 * here are ignored during parsing. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Subtask implements Comparable<Subtask> {

    /** The type of the subtask. */
    private SubtaskType subtaskType;

    /** Set the subtask type.
     * @param aSubtaskType The subtask type to be set.
     */
    public void setSubtaskType(final SubtaskType aSubtaskType) {
        subtaskType = aSubtaskType;
    }

    /** Get the subtask type.
     * @return The subtask type.
     */
    public SubtaskType getSubtaskType() {
        return subtaskType;
    }

    /** The priority of the subtask. Lower values are higher priorities. */
    private Integer priority;

    /** Set the subtask priority.
     * @param aPriority The subtask priority to be set.
     */
    public void setPriority(final Integer aPriority) {
        priority = aPriority;
    }

    /** Get the subtask priority.
     * @return The subtask priority.
     */
    public Integer getPriority() {
        return priority;
    }

    /** The name of the subtask provider. */
    private String provider;

    /** Set the subtask provider.
     * @param aProvider The subtask provider to be set.
     */
    public void setProvider(final String aProvider) {
        provider = aProvider;
    }

    /** Get the subtask provider.
     * @return The subtask provider.
     */
    public String getProvider() {
        return provider;
    }

    /** Additional, subtask-specific properties. */
    private Map<String, String> subtaskProperties;

    /** Set the map of additional subtask properties.
     * @param aSubtaskProperties The map of additional properties to be set.
     */
    public void setSubtaskProperties(
            final Map<String, String> aSubtaskProperties) {
        subtaskProperties = new HashMap<>(aSubtaskProperties);
    }

    /** Get the map of additional subtask properties.
     * @return The map of additional subtask properties.
     */
    public Map<String, String> getSubtaskProperties() {
        return subtaskProperties;
    }

    /** {@inheritDoc}
     * Comparability test based on priority.
     * Values with null priority are sorted at the end.
     */
    @Override
    public int compareTo(final Subtask other) {
        if (other == null) {
            // NPE required by the contract specified in
            // the Javadocs of Comparable<T>.
            throw new NullPointerException();
        }
        if (priority == null) {
            // This version has no priority. It will be sorted
            // after all subtasks that _do_ have priorities.
            if (other.priority == null) {
                // Both versions have null priorities, so
                // consider them equal.
                return 0;
            }
            // The other version has a priority. This subtask
            // is sorted after it.
            return 1;
        }
        // This version has a priority.
        if (other.priority == null) {
            // The other subtask doesn't have a priority. It is
            // sorted after this subtask.
            return -1;
        }
        // Both this and other have priorities. Compare them.
        return priority.compareTo(other.priority);
    }

}
