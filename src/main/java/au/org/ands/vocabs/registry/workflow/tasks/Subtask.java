/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.tasks;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import au.org.ands.vocabs.registry.enums.SubtaskOperationType;
import au.org.ands.vocabs.registry.enums.SubtaskProviderType;
import au.org.ands.vocabs.registry.enums.TaskStatus;
import au.org.ands.vocabs.registry.workflow.provider.ProviderUtils;
import au.org.ands.vocabs.registry.workflow.provider.WorkflowProvider;

/** Representation of one workflow subtask. */
@XmlRootElement(name = "subtask")
/* This annotation means that properties other than the ones defined
 * here are ignored during parsing. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Subtask implements Comparable<Subtask> {

    /** Default constructor. */
    public Subtask() {
    }

    /** Convenience constructor. The priority is assigned by invoking
     * {@link #determinePriority()}.
     * @param aSubtaskProviderType The subtask provider type to be set.
     * @param anOperation The operation to be set.
     * @param aProviderClass The class object of the subtask provider
     *      to be set.
     */
    public Subtask(final SubtaskProviderType aSubtaskProviderType,
            final SubtaskOperationType anOperation,
            final Class<? extends WorkflowProvider> aProviderClass) {
        subtaskProviderType = aSubtaskProviderType;
        operation = anOperation;
        providerClass = aProviderClass;
        provider = ProviderUtils.providerName(aProviderClass);
        determinePriority();
    }

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

    /** The class of the subtask provider. */
    private Class<? extends WorkflowProvider> providerClass;

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

    /** Get the subtask provider class.
     * @return The subtask provider class.
     */
    @XmlTransient
    public Class<? extends WorkflowProvider> getProviderClass() {
        if (providerClass != null) {
            return providerClass;
        }
        if (subtaskProviderType == null || provider == null) {
            throw new IllegalArgumentException(
                    "Either provider type or provider is null.");
        }
        providerClass = ProviderUtils.getProviderClass(subtaskProviderType,
                provider);
        return providerClass;
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

    /** Get the map of additional subtask properties. Note: if there
     * are no properties, this method may return null.
     * @return The map of additional subtask properties, which may be null.
     */
    public Map<String, String> getSubtaskProperties() {
        return subtaskProperties;
    }

    /** Get the value of one subtask property.
     * @param key The name of the property.
     * @return The value of the property, or null, if there is no such
     *      property set.
     */
    public String getSubtaskProperty(final String key) {
        if (subtaskProperties == null) {
            return null;
        }
        return subtaskProperties.get(key);
    }

    /** Add an additional subtask property.
     * @param key The name of the property.
     * @param value The value of the property.
     */
    public void addSubtaskProperty(final String key, final String value) {
        if (subtaskProperties == null) {
            subtaskProperties = new HashMap<>();
        }
        subtaskProperties.put(key, value);
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

    /** Execution status of the subtask, initialized to
     * {@link TaskStatus#NEW}. */
    private TaskStatus status = TaskStatus.NEW;

    /** Get the subtask execution status.
     * @return The execution status.
     */
    public TaskStatus getStatus() {
        return status;
    }

    /** Set the subtask execution status.
     * @param aStatus The execution status to set.
     */
    public void setStatus(final TaskStatus aStatus) {
        status = aStatus;
    }

    /** Results of the subtask. A map of String keys/values. */
    private Map<String, String> results;

    /** Get the subtask execution results.
     * @return The results.
     */
    public Map<String, String> getResults() {
        return results;
    }

    /** Set the subtask execution results.
     * @param aResults The execution results to set.
     */
    public void setResults(final Map<String, String> aResults) {
        results = aResults;
    }

    /** Add one subtask execution result.
     * @param key The key of the result to be added.
     * @param value The value of the result to be added.
     */
    public void addResult(final String key, final String value) {
        if (results == null) {
            results = new HashMap<>();
        }
        results.put(key, value);
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
                // Both versions have null priorities, so have to compare
                // other attributes.
                return new CompareToBuilder().
                        append(subtaskProviderType,
                                other.getSubtaskProviderType()).
                        append(operation, other.getOperation()).
                        append(provider, other.getProvider()).
                        append(subtaskProperties, other.getSubtaskProperties()).
                        toComparison();
//                return 0;
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
        return new CompareToBuilder().
                append(priority, other.getPriority()).
                append(subtaskProviderType,
                        other.getSubtaskProviderType()).
                append(operation, other.getOperation()).
                append(provider, other.getProvider()).
                append(subtaskProperties, other.getSubtaskProperties()).
                toComparison();
    }

    /** {@inheritDoc}
     * Equality test based on all properties.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == null
                || !(other instanceof Subtask)) {
            return false;
        }
        Subtask otherSubtask = (Subtask) other;
        // Note comparison of subtaskProperties, which is a HashMap.
        // This works correctly, because the keys/values are Strings (and not
        // arrays).
        return new EqualsBuilder().
                append(subtaskProviderType,
                        otherSubtask.getSubtaskProviderType()).
                append(operation, otherSubtask.getOperation()).
                append(priority, otherSubtask.getPriority()).
                append(provider, otherSubtask.getProvider()).
                append(subtaskProperties, otherSubtask.getSubtaskProperties()).
                isEquals();
    }

    /** {@inheritDoc}
     * The hash code returned is that of the subtaskProviderType.
     */
    @Override
    public int hashCode() {
        return subtaskProviderType.hashCode();
    }

}
