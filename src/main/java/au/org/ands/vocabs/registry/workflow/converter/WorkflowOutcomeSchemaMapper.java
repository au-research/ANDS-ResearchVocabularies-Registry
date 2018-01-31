/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.converter;

import java.util.List;
import java.util.Map;

import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import au.org.ands.vocabs.registry.schema.vocabulary201701.Result;
import au.org.ands.vocabs.registry.schema.vocabulary201701.WorkflowOutcome;
import au.org.ands.vocabs.registry.schema.vocabulary201701.WorkflowOutcome.TaskOutcome;
import au.org.ands.vocabs.registry.schema.vocabulary201701.WorkflowOutcome.TaskOutcome.SubtaskOutcome;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;
import au.org.ands.vocabs.registry.workflow.tasks.Task;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;

/** MapStruct mapper from a list of task outcomes to schema. */
@Mapper
//@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(WorkflowOutcomeSchemaMapperDecorator.class)
public interface WorkflowOutcomeSchemaMapper {

    /** Singleton instance of this mapper. */
    WorkflowOutcomeSchemaMapper INSTANCE =
            Mappers.getMapper(WorkflowOutcomeSchemaMapper.class);

    /** MapStruct-generated Mapper from a list of TaskInfo instances to schema.
     * @param source The list of TaskInfo instances.
     * @return The schema representation of the outcome of workflow processing.
     */
    default WorkflowOutcome sourceToTarget(@SuppressWarnings("unused")
        final List<TaskInfo> source) {
        return null;
    }

    /** MapStruct-generated Mapper from a workflow Task instance to schema.
     * @param source The Task instance.
     * @return The schema representation of the outcome of processing
     *      of the task.
     */
    // Because of a defect https://github.com/mapstruct/mapstruct/issues/914
    // we can't map the list of subtasks here. It's done in the decorator
    // method. So instead of:
    //  @Mapping(target = "subtaskOutcome", source = "subtasks")
    // we do:
    @Mapping(target = "subtaskOutcome", ignore = true)
    @Mapping(target = "taskResult", ignore = true)
    TaskOutcome sourceToTarget(Task source);

    /** MapStruct-generated Mapper from a workflow Subtask instance to schema.
     * @param source The Subtask instance.
     * @return The schema representation of the outcome of processing
     *      of the subtask.
     */
    @Mapping(target = "subtaskResult", ignore = true)
    SubtaskOutcome sourceToTarget(Subtask source);

    /** MapStruct-generated Mapper from one workflow Subtask result
     * to schema.
     * @param result One Subtask result.
     * @return The schema representation of the subtask result.
     */
    @Mapping(source = "key", target = "resultKey")
    @Mapping(source = "value", target = "resultValue")
    Result mapResult(Map.Entry<String, String> result);
}
