/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.tasks;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import au.org.ands.vocabs.registry.enums.SubtaskOperationType;
import au.org.ands.vocabs.registry.enums.SubtaskProviderType;
import au.org.ands.vocabs.registry.workflow.provider.ProviderUtils;
import au.org.ands.vocabs.registry.workflow.provider.WorkflowProvider;

/** Representation of one workflow subtask. */
@XmlRootElement(name = "subtask")
/* This annotation means that properties other than the ones defined
 * here are ignored during parsing. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Subtask implements Comparable<Subtask> {

    /** The provider type of the subtask. */
    private SubtaskProviderType subtaskProviderType;

    /** Set the subtask provider type.
     * @param aSubtaskProviderType The subtask provider type to be set.
     */
    public void setSubtaskProviderType(
            final SubtaskProviderType aSubtaskProviderType) {
        subtaskProviderType = aSubtaskProviderType;
    }

    /** Get the subtask provider type.
     * @return The subtask type.
     */
    public SubtaskProviderType getSubtaskProviderType() {
        return subtaskProviderType;
    }

    /** The operation to be performed. */
    private SubtaskOperationType operation;

    /** Set the subtask operation.
     * @param anOperation The operation to be set.
     */
    public void setOperation(final SubtaskOperationType anOperation) {
        operation = anOperation;
    }

    /** Get the subtask operation.
     * @return The operation.
     */
    public SubtaskOperationType getOperation() {
        return operation;
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

    /** Set the subtask provider. This method takes a String parameter.
     * @param aProvider The subtask provider to be set.
     */
    public void setProvider(final String aProvider) {
        provider = aProvider;
    }

    /** Set the subtask provider. This method takes a parameter
     * that is a class object that implements the {@link WorkflowProvider}
     * interface.
     * @param aProviderClass The class object of the subtask provider
     *      to be set.
     */
    public void setProvider(
            final Class<? extends WorkflowProvider> aProviderClass) {
        provider = ProviderUtils.providerName(aProviderClass);
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

    /** Set the priority based on the default priority set by the provider.
     * Precondition: the provider type, provider name, and operation must
     * have been set.
     */
    public void determinePriority() {
        WorkflowProvider workflowProvider = ProviderUtils.getProvider(
                subtaskProviderType, provider);
        priority = workflowProvider.defaultPriority(operation);
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
