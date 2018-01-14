/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.provider;

import au.org.ands.vocabs.registry.enums.SubtaskOperationType;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;

/** Interface that defines methods common to all workflow providers. */
public interface WorkflowProvider {

    /** Get the priority that should be specified within a subtask
     * of the specified operation type.
     * The default implementation returns null.
     * @param operationType The operation type of the subtask.
     * @return The priority that should be used, or null, if the
     *      provider does not need/want to specify one.
     */
    default Integer defaultPriority(@SuppressWarnings("unused")
        final SubtaskOperationType operationType) {
        return null;
    }

    /** Perform a subtask.
     * @param taskInfo The top-level TaskInfo for the subtask.
     * @param subtask The subtask to be performed.
     */
    void doSubtask(TaskInfo taskInfo, Subtask subtask);

}
