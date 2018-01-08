/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.workflow.tasks;

import au.org.ands.vocabs.registry.db.entity.Task;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;

/** Class encapsulating all information about a task. */
public class TaskInfo {

    /** Task object for this task. */
    private Task task;

    /** Vocabulary object for this task. */
    private Vocabulary vocabulary;

    /** Version object for this task. */
    private Version version;

    /** Constructor.
     * @param aTask The Task object
     * @param aVocabulary The Vocabulary object
     * @param aVersion The Version object
     */
    public TaskInfo(final Task aTask, final Vocabulary aVocabulary,
            final Version aVersion) {
        task = aTask;
        vocabulary = aVocabulary;
        version = aVersion;
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
