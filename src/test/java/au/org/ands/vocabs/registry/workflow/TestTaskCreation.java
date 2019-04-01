/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow;

import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.enums.SubtaskOperationType;
import au.org.ands.vocabs.registry.enums.SubtaskProviderType;
import au.org.ands.vocabs.registry.workflow.provider.transform.JsonListTransformProvider;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;
import au.org.ands.vocabs.registry.workflow.tasks.Task;

/** Tests of the {@link Task} and {@link Subtask} classes. */
public class TestTaskCreation {

    /** Helper factory method to create a Subtask suitable for testing.
     * @return A new Subtask instance.
     */
    private Subtask newSubtask() {
        Subtask subtask = new Subtask();
        subtask.setSubtaskProviderType(SubtaskProviderType.TRANSFORM);
        subtask.setProvider(JsonListTransformProvider.class);
        subtask.setOperation(SubtaskOperationType.INSERT);
        subtask.determinePriority();
        return subtask;
    }


    /** Tests of equality and comparison of Subtasks.
     * The {@link Subtask} class has implementations of both
     * {@link Subtask#equals(Object)} and {@link Subtask#compareTo(Subtask)}.
     * This method tests that the natural ordering of Subtasks
     * (i.e., imposed by the {@link Subtask#compareTo(Subtask)} method)
     * is "consistent with equals" in the sense defined by the
     * {@link Comparable} interface.
     * Create two identical subtasks, each of which
     * has subtask properties. Confirm that the two subtasks
     * are equal using {@code equals()} and that {@code compareTo()}
     * of the subtasks returns 0.
     * Create a task and insert both subtasks into it. Confirm
     * that the length of the subtasks list is only 1.
     * Then add a subtask property to each subtask which makes the
     * two subtasks different, and confirm that the two subtasks
     * are not equal using {@code equals()}, and that {@code compareTo()}
     * of the subtasks now returns -1.
     * Create a task and insert both subtasks into it. Confirm
     * that the length of the subtasks list is 2. */
    @Test
    public void testSubtasksWithIdenticalAndDifferentSubtaskProperties() {

        Subtask subtask1 = newSubtask();
        subtask1.addSubtaskProperty("prop1", "abc");
        subtask1.addSubtaskProperty("prop3", "ghi");
        Subtask subtask2 = newSubtask();
        subtask2.addSubtaskProperty("prop3", "ghi");
        subtask2.addSubtaskProperty("prop1", "abc");

        Assert.assertEquals(subtask1, subtask2,
                "Subtasks aren't equal using equals()");

        Assert.assertEquals(subtask1.compareTo(subtask2), 0,
                "Subtasks aren't equal using compareTo()");

        Task task = new Task();
        task.addSubtask(subtask1);
        task.addSubtask(subtask2);

        Assert.assertEquals(task.getSubtasks().size(), 1,
                "Not exactly one Subtask in Subtask list");

        subtask1.addSubtaskProperty("prop2", "def");
        subtask2.addSubtaskProperty("prop2", "deg");

        Assert.assertNotEquals(subtask1, subtask2,
                "Subtasks aren't different using equals()");

        Assert.assertEquals(subtask1.compareTo(subtask2), -1,
                "subtask1 isn't less than subtask2 using compareTo()");

        task = new Task();
        task.addSubtask(subtask1);
        task.addSubtask(subtask2);
        Assert.assertEquals(task.getSubtasks().size(), 2,
                "Not exactly two Subtasks in Subtask list");
    }

}
