/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.workflow.tasks;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.dao.TaskDAO;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.enums.TaskStatus;

/** Class encapsulating all information about a task. */
public class TaskInfo {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Database task object for this task. */
    private au.org.ands.vocabs.registry.db.entity.Task dbTask;

    /** Workflow Task object for this task. */
    private Task task;

    /** Vocabulary object for this task. */
    private Vocabulary vocabulary;

    /** Version object for this task. */
    private Version version;

    /** EntityManager to use for this task. */
    private EntityManager em;

    /** The time to use as the value of "now" when applying changes
     * to rows of the database. */
    private LocalDateTime nowTime;

    /** The value to use for "modifiedBy" when adding or updating
     * rows of the database. */
    private String modifiedBy;

    /** Constructor.
     * @param aVocabulary The Vocabulary object.
     * @param aVersion The Version object.
     */
    public TaskInfo(final Vocabulary aVocabulary,
            final Version aVersion) {
        vocabulary = aVocabulary;
        version = aVersion;
    }

    /** Constructor.
     * @param aDbTask The Task object as it came from the database.
     * @param aVocabulary The Vocabulary object.
     * @param aVersion The Version object.
     */
    public TaskInfo(final au.org.ands.vocabs.registry.db.entity.Task aDbTask,
            final Vocabulary aVocabulary,
            final Version aVersion) {
        dbTask = aDbTask;
        vocabulary = aVocabulary;
        version = aVersion;
        task = new Task();
        task.setVocabularyId(dbTask.getVocabularyId());
        task.setVersionId(dbTask.getVersionId());
        task.setSubtasks(JSONSerialization.deserializeStringAsJson(
                dbTask.getParams(), new TypeReference<List<Subtask>>() { }));
        // But now reset each subtask's status and discard any results
        // from a previous run.
        List<Subtask> subtasks = task.getSubtasks();
        if (subtasks != null) {
            for (Subtask subtask : subtasks) {
                subtask.setStatus(TaskStatus.NEW);
                subtask.setResults(null);
            }
        }
    }

    /** Constructor.
     * @param aTask The workflow Task object.
     * @param aVocabulary The Vocabulary object.
     * @param aVersion The Version object.
     */
    public TaskInfo(final Task aTask, final Vocabulary aVocabulary,
            final Version aVersion) {
        task = aTask;
        vocabulary = aVocabulary;
        version = aVersion;
    }

    /** Setter for the database Task object.
     * @param aDbTask The Task object as it appears in the database.
     */
    public final void setDbTask(
            final au.org.ands.vocabs.registry.db.entity.Task aDbTask) {
        dbTask = aDbTask;
    }

    /** Getter for the database Task object.
     * @return The database Task object.
     */
    public final au.org.ands.vocabs.registry.db.entity.Task getDbTask() {
        return dbTask;
    }

    /** Getter for the Task object.
     * @return The Task object.
     */
    public final Task getTask() {
        return task;
    }

    /** Getter for the Vocabulary object.
     * @return The Vocabulary object.
     */
    public final Vocabulary getVocabulary() {
        return vocabulary;
    }

    /** Getter for the Version object.
     * @return The Version object.
     */
    public final Version getVersion() {
        return version;
    }

    /** Get the EntityManager to use to process this task.
     * @return The EntityManager to use.
     */
    public EntityManager getEm() {
        return em;
    }

    /** Set the EntityManager to use to process this task.
     * @param anEm The EntityManager to set.
     */
    public void setEm(final EntityManager anEm) {
        em = anEm;
    }

    /** Set the value of nowTime.
     * @param aNowTime The value of nowTime to set.
     */
    public final void setNowTime(final LocalDateTime aNowTime) {
        nowTime = aNowTime;
    }

    /** Get the value of nowTime.
     * @return The value of nowTime.
     */
    public final LocalDateTime getNowTime() {
        return nowTime;
    }

    /** Set the value of modifiedBy.
     * @param aModifiedBy The value of modifiedBy to set.
     */
    public final void setModifiedBy(final String aModifiedBy) {
        modifiedBy = aModifiedBy;
    }

    /** Get the value of modifiedBy.
     * @return The value of modifiedBy.
     */
    public final String getModifiedBy() {
        return modifiedBy;
    }

    /** Persist this task. No processing of the task is done. */
    public void persist() {
        if (em == null) {
            throw new IllegalArgumentException("EntityManager not set");
        }
        if (dbTask == null) {
            dbTask = new au.org.ands.vocabs.registry.db.entity.Task();
            dbTask.setVocabularyId(vocabulary.getVocabularyId());
            dbTask.setVersionId(version.getVersionId());
            dbTask.setStatus(TaskStatus.NEW);
            dbTask.setResponse("");
            dbTask.setParams(JSONSerialization.serializeObjectAsJsonString(
                    task.getSubtasks()));
            // Persist the database entity a first time, so it has an Id.
            TaskDAO.saveTask(em, dbTask);
            logger.info("Persisted task with task Id: " + dbTask.getId());
        } else {
            // Update the database entity with the results.
            dbTask.setStatus(task.getStatus());
            dbTask.setParams(JSONSerialization.serializeObjectAsJsonString(
                    task.getSubtasks()));
            dbTask.setResponse(JSONSerialization.serializeObjectAsJsonString(
                    task.getResults()));
            TaskDAO.updateTask(em, dbTask);
        }
    }

    /** Process this task. If it has not already been persisted,
     * {@link #persist()} will be called first. In any case,
     * {@link #persist()} will be called <i>after</i> processing. */
    public void process() {
        if (dbTask == null) {
            persist();
        }
        new TaskRunner(this).runTask();
        persist();
    }

    /** Process the task. Only the subtasks with a negative priority
     * are executed. If the task has not already been persisted,
     * {@link #persist()} will be called first. In any case,
     * {@link #persist()} will be called <i>after</i> processing. */
    public void processOnlyNegativePrioritySubtasks() {
        if (dbTask == null) {
            persist();
        }
        new TaskRunner(this).runTaskOnlyNegativePrioritySubtasks();
        persist();
    }

    /** Process the remaining subtasks of this task.
     * If the task has not already been persisted,
     * {@link #persist()} will be called first. In any case,
     * {@link #persist()} will be called <i>after</i> processing. */
    public void processRemainingSubtasks() {
        if (dbTask == null) {
            persist();
        }
        new TaskRunner(this).runTaskRemainingSubtasks();
        persist();
    }

}
