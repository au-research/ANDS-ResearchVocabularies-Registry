/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.tasks;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.xml.bind.annotation.XmlRootElement;

import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.enums.TaskStatus;

/** Representation of one workflow task. */
@XmlRootElement(name = "task")
public class Task {

    /** The vocabulary Id. */
    private Integer vocabularyId;

    /** Get the value of vocabularyId.
     * @return The value of vocabularyId.
     */
    public Integer getVocabularyId() {
        return vocabularyId;
    }

    /** Set the value of vocabularyId.
     * @param aVocabularyId The value of vocabularyId to set.
     */
    public void setVocabularyId(final Integer aVocabularyId) {
        vocabularyId = aVocabularyId;
    }

    /** The version Id. */
    private Integer versionId;

    /** Get the value of versionId.
     * @return The value of versionId.
     */
    public Integer getVersionId() {
        return versionId;
    }

    /** Set the value of versionId.
     * @param aVersionId The value of versionId to set.
     */
    public void setVersionId(final Integer aVersionId) {
        versionId = aVersionId;
    }

    /** The queue of subtasks to be performed. Initialized to an empty
     * list, so that {@link #addSubtask(Subtask)} may be invoked
     * immediately after the constructor. */
    private List<Subtask> subtasks = new ArrayList<>();

    /** Set the list of subtasks to be performed.
     * @param aSubtasks The list of subtasks to be performed.
     */
    public void setSubtasks(final List<Subtask> aSubtasks) {
        subtasks = aSubtasks;
    }

    /** Add a subtask.
     * @param aSubtask The subtask to be added.
     */
    public void addSubtask(final Subtask aSubtask) {
        subtasks.add(aSubtask);
    }

    /** Get the list of subtasks to be performed, sorted by
     * priority.
     *
     * @return The list of subtasks to be performed, sorted by priority.
     */
    public List<Subtask> getSubtasks() {
        subtasks.sort(null);
        return subtasks;
    }

    /** Persist and process this task.
     * @param em The EntityManager to use to persist the task.
     */
    public void persistAndProcess(final EntityManager em) {
        au.org.ands.vocabs.registry.db.entity.Task dbTask =
                new au.org.ands.vocabs.registry.db.entity.Task();
        dbTask.setVocabularyId(vocabularyId);
        dbTask.setVersionId(versionId);
        dbTask.setStatus(TaskStatus.NEW);
        dbTask.setResponse("");
        dbTask.setParams(JSONSerialization.serializeObjectAsJsonString(
                subtasks));
        /* TO DO: uncomment when we're ready.
        TaskDAO.saveTask(em, dbTask);
        */
        // TO DO: now process it.
    }

}
