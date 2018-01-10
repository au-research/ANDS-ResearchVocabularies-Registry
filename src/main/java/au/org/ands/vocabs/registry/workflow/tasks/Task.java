/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlRootElement;

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
     * set, so that {@link #addSubtask(Subtask)} may be invoked
     * immediately after the constructor.
     * The queue is implemented as a SortedSet. This means that (a)
     * duplicates are eliminated, (b) the subtasks are ordered by
     * priority.
     */
    private SortedSet<Subtask> subtasks = new TreeSet<>();

    /** Set the list of subtasks to be performed.
     * @param aSubtasks The list of subtasks to be performed.
     */
    public void setSubtasks(final List<Subtask> aSubtasks) {
        subtasks.clear();
        subtasks.addAll(aSubtasks);
    }

    /** Add a subtask.
     * @param aSubtask The subtask to be added.
     */
    public void addSubtask(final Subtask aSubtask) {
        subtasks.add(aSubtask);
    }

    /** Add a list of subtasks.
     * @param subtaskList The list of subtasks to be added.
     */
    public void addSubtasks(final List<Subtask> subtaskList) {
        subtasks.addAll(subtaskList);
    }

    /** Get the list of subtasks to be performed, sorted by
     * priority.
     *
     * @return The list of subtasks to be performed, sorted by priority.
     */
    public List<Subtask> getSubtasks() {
        return new ArrayList<>(subtasks);
    }

}
