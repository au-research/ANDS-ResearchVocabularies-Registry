/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.workflow.tasks;

import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;

/** Class encapsulating all information about a task. */
public class TaskInfo {

    /** Database task object for this task. */
    private au.org.ands.vocabs.registry.db.entity.Task dbTask;

    /** Workflow Task object for this task. */
    private Task task;

    /** Vocabulary object for this task. */
    private Vocabulary vocabulary;

    /** Version object for this task. */
    private Version version;

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
                dbTask.getParams(), Task.class).getSubtasks());
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

}
