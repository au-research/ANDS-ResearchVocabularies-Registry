/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.workflow.tasks;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.enums.TaskStatus;
import au.org.ands.vocabs.registry.workflow.provider.ProviderUtils;
import au.org.ands.vocabs.registry.workflow.provider.WorkflowProvider;

/** Top level runner for registry workflow tasks. */
public class TaskRunner {

    /** A generic error response, to be used as the value corresponding
     * to key {{@link #RESPONSE}. */
    private static final String GENERIC_ERROR_RESPONSE = "Error running task.";

    /** Result key in which a completion timestamp is stored. */
    public static final String TIMESTAMP = "timestamp";

    /** Result key in which an overall response message is stored. */
    public static final String RESPONSE = "response";

    /** Result key in which a stacktrace is stored. */
    public static final String STACKTRACE = "stacktrace";

    /** Result key in which an error message is stored. */
    public static final String ERROR = "error";

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** The TaskInfo object for this task. */
    private TaskInfo taskInfo;

    /** The Task object for this task. */
    private Task task;

    /** The results of running the task. */
    private HashMap<String, String> results = new HashMap<>();

    /** Constructor.
     * @param aTaskInfo The TaskInfo structure describing this task.
     */
    public TaskRunner(final TaskInfo aTaskInfo) {
        taskInfo = aTaskInfo;
    }

    /** Run the task.
     */
    public final void runTask() {
        task = taskInfo.getTask();
        List<Subtask> subtasks = task.getSubtasks();
        if (subtasks == null || subtasks.size() == 0) {
            // Nothing to do.
            task.setStatus(TaskStatus.ERROR);
            task.addResult(ERROR, "No subtasks specified, or invalid"
                    + " format.");
            task.addResult(RESPONSE, GENERIC_ERROR_RESPONSE);
            addTimestamp();
            return;
        }
        boolean success = false;
        TaskStatus lastSubtaskStatus;
        for (Subtask subtask : subtasks) {
            logger.debug("Got subtask: " + subtask.toString());
            logger.debug("subtask type: " + subtask.getSubtaskProviderType());
            WorkflowProvider provider = ProviderUtils.getProvider(
                    subtask.getProviderClass());
            try {
                provider.doSubtask(taskInfo, subtask);
            } catch (Throwable t) {
                logger.error("Subtask threw an exception", t);
                subtask.setStatus(TaskStatus.ERROR);
                subtask.addResult(ERROR, "Exception in subtask.");
                subtask.addResult(STACKTRACE, ExceptionUtils.getStackTrace(t));
            }
            addTimestamp(subtask);
            lastSubtaskStatus = subtask.getStatus();
            if (lastSubtaskStatus != TaskStatus.SUCCESS) {
                logger.error("Subtask did not complete successfully: "
                        + task);
                addTimestamp();
                task.setStatus(lastSubtaskStatus);
                task.addResult(ERROR, "Error in subtask.");
                task.addResult(RESPONSE, GENERIC_ERROR_RESPONSE);
                return;
            }
        }
        task.setStatus(TaskStatus.SUCCESS);
        task.addResult(RESPONSE, "All tasks completed.");
//        results.put("output_path", TaskUtils.getTaskOutputPath(taskInfo,
//                null));
        addTimestamp();
    }

//  /** Format to use for timestamps. */
//  private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /** Add a timestamp to the results map of the Task. */
    public final void addTimestamp() {
//        final SimpleDateFormat dateFormat =
//                new SimpleDateFormat(TIMESTAMP_FORMAT, Locale.ROOT);
//        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        // Try using toString() instead of dateFormat.format(...).
        taskInfo.getTask().addResult(TIMESTAMP,
                TemporalUtils.nowUTC().toString());
    }
    /**
     * Add a timestamp to the results map of a Subtask.
     * @param subtask The Subtask to which the timestamp is to be added.
     */
    public final void addTimestamp(final Subtask subtask) {
//        final SimpleDateFormat dateFormat =
//                new SimpleDateFormat(TIMESTAMP_FORMAT, Locale.ROOT);
//        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        // Try using toString() instead of dateFormat.format(...).
        subtask.addResult(TIMESTAMP, TemporalUtils.nowUTC().toString());
    }

}
