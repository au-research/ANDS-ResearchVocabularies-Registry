/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.converter;

import java.util.List;
import java.util.Map;

import au.org.ands.vocabs.registry.schema.vocabulary201701.Result;
import au.org.ands.vocabs.registry.schema.vocabulary201701.WorkflowOutcome;
import au.org.ands.vocabs.registry.schema.vocabulary201701.WorkflowOutcome.TaskOutcome;
import au.org.ands.vocabs.registry.schema.vocabulary201701.WorkflowOutcome.TaskOutcome.SubtaskOutcome;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;
import au.org.ands.vocabs.registry.workflow.tasks.Task;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;

/** Decorator for MapStruct mapper from a list of task outcomes to schema. */
public abstract class WorkflowOutcomeSchemaMapperDecorator
    implements WorkflowOutcomeSchemaMapper {

    /** The delegate mapper. */
    private final WorkflowOutcomeSchemaMapper delegate;

    /** Constructor that accepts a delegate.
     * @param aDelegate The delegate mapper.
     */
    public WorkflowOutcomeSchemaMapperDecorator(
            final WorkflowOutcomeSchemaMapper aDelegate) {
        delegate = aDelegate;
    }

    /** Decorator method that extends the default mapping behaviour
     * with the implementation, as this can't be done using
     * Mapping annotations.
     * (Don't make this method final; it is extended by the implementation
     * class.)
     */
    @Override
    @SuppressWarnings("checkstyle:DesignForExtension")
    public WorkflowOutcome sourceToTarget(final List<TaskInfo> source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        WorkflowOutcome target = new WorkflowOutcome();
        List<TaskOutcome> outcomes = target.getTaskOutcome();

        boolean vocabularyIdSet = false;
        for (TaskInfo taskInfo : source) {
            if (!vocabularyIdSet) {
                target.setVocabularyId(taskInfo.getVocabulary().
                        getVocabularyId());
                vocabularyIdSet = true;
            }
            outcomes.add(sourceToTarget(taskInfo.getTask()));
        }
        return target;
    }

    /** Decorator method that extends the default mapping behaviour
     * with assigning the task results and mapping the subtasks.
     * (Don't make this method final; it is extended by the implementation
     * class.)
     */
    @Override
    @SuppressWarnings("checkstyle:DesignForExtension")
    public TaskOutcome sourceToTarget(final Task source) {
        if (source == null) {
            return null;
        }
        TaskOutcome target = delegate.sourceToTarget(source);
        Map<String, String> results = source.getResults();
        if (results != null && !results.isEmpty()) {
            List<Result> targetResults = target.getTaskResult();
            for (Map.Entry<String, String> result : results.entrySet()) {
                Result oneResult = new Result();
                oneResult.setResultKey(result.getKey());
                oneResult.setResultValue(result.getValue());
                targetResults.add(oneResult);
            }
        }
        // Because of a defect
        // https://github.com/mapstruct/mapstruct/issues/914
        // we can't map the list of subtasks using a @Mapping
        // attribute in the interface; we have to do it here.
        List<Subtask> subtasks = source.getSubtasks();
        if (subtasks != null) {
            List<SubtaskOutcome> subtaskOutcomes = target.getSubtaskOutcome();
            for (Subtask subtask : subtasks) {
                SubtaskOutcome subtaskOutcome = sourceToTarget(subtask);
                if (subtaskOutcome != null) {
                    subtaskOutcomes.add(subtaskOutcome);
                }
            }
        }
        return target;
    }

    /** Decorator method that extends the default mapping behaviour
     * with assigning the subtask results.
     * (Don't make this method final; it is extended by the implementation
     * class.)
     */
    @Override
    @SuppressWarnings("checkstyle:DesignForExtension")
    public SubtaskOutcome sourceToTarget(final Subtask source) {
        if (source == null) {
            return null;
        }
        SubtaskOutcome target = delegate.sourceToTarget(source);
        Map<String, String> results = source.getResults();
        if (results != null && !results.isEmpty()) {
            List<Result> targetResults = target.getSubtaskResult();
            results.entrySet().stream().map(this::mapResult).
                forEach(targetResults::add);
        }
        return target;
    }

}
