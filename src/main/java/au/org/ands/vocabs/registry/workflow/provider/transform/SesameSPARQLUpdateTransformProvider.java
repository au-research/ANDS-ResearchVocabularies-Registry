/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.workflow.provider.transform;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;

import org.openrdf.model.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.enums.SubtaskOperationType;
import au.org.ands.vocabs.registry.enums.TaskStatus;
import au.org.ands.vocabs.registry.workflow.provider.DefaultPriorities;
import au.org.ands.vocabs.registry.workflow.provider.WorkflowProvider;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;
import au.org.ands.vocabs.registry.workflow.tasks.TaskRunner;

/**
 * Transform provider for running a SPARQL update on a Sesame repository. In
 * case we need to do a transform provider that operates on "raw" RDF files, see
 * https://groups.google.com/d/msg/sesame-users/fJctKX_vNEs/a1gm7rqD3L0J for how
 * to do it.
 */
public class SesameSPARQLUpdateTransformProvider implements WorkflowProvider {

    /** Logger for this class. */
    private final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Apply a SPARQL update to the Sesame repository for the version.
     * @param taskInfo The top-level TaskInfo for the subtask.
     * @param subtask The subtask to be performed.
     */
    public final void transform(final TaskInfo taskInfo,
            final Subtask subtask) {
        String sparqlUpdateText = subtask.getSubtaskProperty("sparql_update");
        if (sparqlUpdateText == null) {
            subtask.setStatus(TaskStatus.ERROR);
            subtask.addResult(TaskRunner.ERROR,
                    "No SPARQL update statement specified.");
            return;
        }
        boolean success = SesameTransformUtils.runUpdate(taskInfo, subtask,
                sparqlUpdateText, new HashMap<String, Value>());
        if (success) {
            subtask.setStatus(TaskStatus.SUCCESS);
        } else {
            subtask.setStatus(TaskStatus.ERROR);
        }
    }

    /** Undo the metadata insertion from the Sesame repository for the version.
     * This is a no-op.
     * @param taskInfo The top-level TaskInfo for the subtask.
     * @param subtask The subtask to be performed.
     */
    public final void untransform(
            @SuppressWarnings("unused") final TaskInfo taskInfo,
            @SuppressWarnings("unused") final Subtask subtask) {
    }

    /** {@inheritDoc} */
    @Override
    public Integer defaultPriority(final SubtaskOperationType operationType) {
        switch (operationType) {
        case INSERT:
        case PERFORM:
            return DefaultPriorities.
                    DEFAULT_TRANSFORM_AFTER_IMPORTER_INSERT_PRIORITY;
        case DELETE:
            return DefaultPriorities.
                    DEFAULT_TRANSFORM_AFTER_IMPORTER_DELETE_PRIORITY;
        default:
            // Unknown operation type!
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void doSubtask(final TaskInfo taskInfo, final Subtask subtask) {
        switch (subtask.getOperation()) {
        case INSERT:
        case PERFORM:
            transform(taskInfo, subtask);
            break;
        case DELETE:
            untransform(taskInfo, subtask);
            break;
        default:
            logger.error("Unknown operation!");
           break;
        }
    }

}
