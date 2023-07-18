/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.workflow.tasks;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
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

    /** Result key in which an alert message is stored, in HTML format. */
    public static final String ALERT_HTML = "alert-html";

    /** Result key in which a private information message is stored. */
    public static final String INFO_PRIVATE = "info-private";

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** The TaskInfo object for this task. */
    private TaskInfo taskInfo;

    /** The Task object for this task. */
    private Task task;

    /** Constructor.
     * @param aTaskInfo The TaskInfo structure describing this task.
     */
    public TaskRunner(final TaskInfo aTaskInfo) {
        taskInfo = aTaskInfo;
    }

    /** Run the task. All of the subtasks are run in sequence until
     * completion, or until one of the subtasks generates an exception
     * or returns status {@link TaskStatus#ERROR}.
     * Unlike {@link #runTaskRemainingSubtasks()}, the existing value of
     * the task status and of each subtask status is not examined before
     * execution, so use this method to force execution of the task.
     * See the pair of methods {@link #runTaskOnlyNegativePrioritySubtasks()}
     * and {@link #runTaskRemainingSubtasks()} for a way of splitting up
     * execution of the subtasks of the task.
     * The task status is set to {@link TaskStatus#ERROR} if any of the
     * executed subtasks completes with a status other than
     * {@link TaskStatus#PARTIAL} or {@link TaskStatus#SUCCESS}.
     * Otherwise, the task status is set to {@link TaskStatus#PARTIAL}
     * if any of the executed subtasks completes with that status.
     * Otherwise, the task status is set to {@link TaskStatus#SUCCESS}.
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
        // Did at least one subtask complete only partially?
        boolean partial = false;
        TaskStatus lastSubtaskStatus;
        for (Subtask subtask : subtasks) {
            logger.debug("runTask Got subtask: "
                    + JSONSerialization.serializeObjectAsJsonString(subtask));
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
            if (lastSubtaskStatus == TaskStatus.PARTIAL) {
                partial = true;
            } else if (lastSubtaskStatus != TaskStatus.SUCCESS) {
                logger.error("Subtask did not complete successfully: "
                        + JSONSerialization.serializeObjectAsJsonString(task));
                addTimestamp();
                task.setStatus(TaskStatus.ERROR);
                task.addResult(ERROR, "Error in subtask.");
                task.addResult(RESPONSE, GENERIC_ERROR_RESPONSE);
                return;
            }
        }
        if (partial) {
            task.setStatus(TaskStatus.PARTIAL);
            task.addResult(RESPONSE, "All subtasks completed; at least one "
                    + "completed only partially.");
        } else {
            task.setStatus(TaskStatus.SUCCESS);
            task.addResult(RESPONSE, "All subtasks completed successfully.");
        }
        addTimestamp();
    }

    /** Run the task. The subtasks with a negative priority are run in sequence.
     * If <i>all</i> of the task's subtasks have a negative priority,
     * this method behaves in the same way as {@link #runTask()}.
     * Otherwise, task execution stops before executing any of the subtasks
     * that do not have a negative priority. (NB: this means subtasks that
     * have a priority that is non-negative or {@code null}.)
     * The task status is set to {@link TaskStatus#ERROR} if any of the
     * executed subtasks completes with a status other than
     * {@link TaskStatus#PARTIAL} or {@link TaskStatus#SUCCESS}.
     * The setting of the task status otherwise depends on whether
     * all of the subtasks were executed, i.e., if they <i>all</i> have
     * a negative priority.
     * If it turns out that all subtasks are executed, and all complete
     * without error, the task status is set as it would have been by
     * {@link #runTask()}: the task status is set to {@link TaskStatus#PARTIAL}
     * if any of the executed subtasks completes with that status,
     * and otherwise, the task status is set to {@link TaskStatus#SUCCESS}.
     * But if at least one subtask was skipped, the task status is left
     * untouched, which (probably) means it will remain as
     * {@link TaskStatus#NEW}.
     * So, this method will only set the task status to
     * {@link TaskStatus#SUCCESS} if all subtasks were executed successfully,
     * and were executed completely (i.e., not partially).
     * If there indeed remain unexecuted subtasks, a caller will probably
     * follow up by invoking {@link #runTaskRemainingSubtasks()}.
     */
    public final void runTaskOnlyNegativePrioritySubtasks() {
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
        // Did at least one subtask complete only partially?
        boolean partial = false;
        // Was at least one subtask skipped, because it does not have
        // a negative priority?
        boolean subtaskSkipped = false;
        TaskStatus lastSubtaskStatus;
        for (Subtask subtask : subtasks) {
            logger.debug("runTaskOnlyNegativePrioritySubtasks Got subtask: "
                    + JSONSerialization.serializeObjectAsJsonString(subtask));
            Integer priority = subtask.getPriority();
            if (priority == null || priority >= 0) {
                logger.debug("This is runTaskOnlyNegativePrioritySubtasks, "
                        + "and this subtask does not have a negative priority, "
                        + "so it will not be executed (for now), and task "
                        + "execution will stop.");
                subtaskSkipped = true;
                break;
            }
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
            if (lastSubtaskStatus == TaskStatus.PARTIAL) {
                partial = true;
            } else if (lastSubtaskStatus != TaskStatus.SUCCESS) {
                logger.error("Subtask did not complete successfully: "
                        + JSONSerialization.serializeObjectAsJsonString(task));
                addTimestamp();
                task.setStatus(TaskStatus.ERROR);
                task.addResult(ERROR, "Error in subtask.");
                task.addResult(RESPONSE, GENERIC_ERROR_RESPONSE);
                return;
            }
        }
        if (subtaskSkipped) {
            // In this case, we don't touch the task status,
            // but we do add a task result and set the timestamp.
            task.addResult(RESPONSE,
                    "Any/all negative-priority subtasks completed; "
                    + "at least one subtask was not executed.");
            addTimestamp();
            return;
        }
        // If we reach this point, then all of the subtasks were
        // executed, and we finish up exactly as per runTask().
        if (partial) {
            task.setStatus(TaskStatus.PARTIAL);
            task.addResult(RESPONSE, "All subtasks completed; at least one "
                    + "completed only partially.");
        } else {
            task.setStatus(TaskStatus.SUCCESS);
            task.addResult(RESPONSE, "All subtasks completed successfully.");
        }
        addTimestamp();
    }

    /** Run the subtasks of a task that have status {@link TaskStatus#NEW}.
     * If the task has never previously been run, this method behaves in the
     * same way as {@link #runTask()}.
     * If the task status has already been set to {@link TaskStatus#ERROR},
     * {@link TaskStatus#PARTIAL}, or {@link TaskStatus#SUCCESS},
     * the method returns without executing any subtasks.
     * Otherwise, task execution skips all subtasks which have a status
     * other than {@link TaskStatus#NEW}.
     * The task status is set to {@link TaskStatus#ERROR} if any of the
     * executed subtasks completes with a status other than
     * {@link TaskStatus#PARTIAL} or {@link TaskStatus#SUCCESS}.
     * Otherwise, the task status is set to {@link TaskStatus#PARTIAL}
     * if any subtask, <i>including any previously-executed subtask</i>,
     * has completed with that status.
     * Otherwise, the task status is set to {@link TaskStatus#SUCCESS}.
     */
    public final void runTaskRemainingSubtasks() {
        task = taskInfo.getTask();
        if (task.getStatus() == TaskStatus.ERROR
                || task.getStatus() == TaskStatus.PARTIAL
                || task.getStatus() == TaskStatus.SUCCESS) {
            // This task has been done to "completion": either all
            // of the subtasks have been executed, or one of them
            // returned with an error or exception. So we return immediately,
            // without even updating the timestamp.
            return;
        }
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
        // Did at least one subtask complete only partially?
        boolean partial = false;
        TaskStatus lastSubtaskStatus;
        for (Subtask subtask : subtasks) {
            logger.debug("runTaskRemainingSubtasks Got subtask: "
                    + JSONSerialization.serializeObjectAsJsonString(subtask));
            if (subtask.getStatus() != TaskStatus.NEW) {
                // Already executed; we will skip this subtask.
                if (subtask.getStatus() == TaskStatus.PARTIAL) {
                    // But we do keep track of a previous PARTIAL status.
                    partial = true;
                }
                logger.debug("Subtask skipped, as status is not NEW");
                continue;
            }
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
            if (lastSubtaskStatus == TaskStatus.PARTIAL) {
                partial = true;
            } else if (lastSubtaskStatus != TaskStatus.SUCCESS) {
                logger.error("Subtask did not complete successfully: "
                        + JSONSerialization.serializeObjectAsJsonString(task));
                addTimestamp();
                task.setStatus(TaskStatus.ERROR);
                task.addResult(ERROR, "Error in subtask.");
                task.addResult(RESPONSE, GENERIC_ERROR_RESPONSE);
                return;
            }
        }
        if (partial) {
            task.setStatus(TaskStatus.PARTIAL);
            task.addResult(RESPONSE, "All subtasks completed; at least one "
                    + "completed only partially.");
        } else {
            task.setStatus(TaskStatus.SUCCESS);
            task.addResult(RESPONSE, "All subtasks completed successfully.");
        }
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
