/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.provider;

import au.org.ands.vocabs.registry.enums.SubtaskOperationType;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;

/** Interface that defines methods common to all workflow providers. */
public interface WorkflowProvider {

    /** Get the priority that should be specified within a subtask
     * of the specified operation type.
     * @param operationType The operation type of the subtask.
     * @return The priority that should be used, or null, if the
     *      provider does not need/want to specify one.
     */
    Integer defaultPriority(SubtaskOperationType operationType);

    /** Perform a subtask.
     * @param subtask The subtask to be performed.
     */
    void doSubtask(Subtask subtask);

}
