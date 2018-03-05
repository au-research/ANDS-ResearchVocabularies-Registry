/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.dbunit.DatabaseUnitException;
import org.dbunit.assertion.comparer.value.ValueComparer;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.datatype.DataType;

import com.fasterxml.jackson.core.type.TypeReference;

import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;
import au.org.ands.vocabs.registry.workflow.tasks.TaskRunner;

/** Class for use with DbUnit to compare expected and actual task
 * database entities. */
public class TaskComparer implements ValueComparer {

    /** Compare expected and actual values from a task. */
    @Override
    public String compare(final ITable expectedTable,
            final ITable actualTable, final int rowNum,
            final String columnName, final DataType dataType,
            final Object expectedValue, final Object actualValue)
            throws DatabaseUnitException {
        switch (columnName) {
        case "PARAMS":
            List<Subtask> expectedSubtasks = JSONSerialization.
            deserializeStringAsJson(
                    (String) expectedValue,
                    new TypeReference<List<Subtask>>() { });
            List<Subtask> actualSubtasks = JSONSerialization.
            deserializeStringAsJson(
                    (String) actualValue,
                    new TypeReference<List<Subtask>>() { });
            if (expectedSubtasks.size() != actualSubtasks.size()) {
                return "Difference in PARAMS size: TASK row "
                        + rowNum + "; expected " + expectedSubtasks.size()
                        + "; actual " + actualSubtasks.size();
            }
            Subtask expectedSubtask;
            Subtask actualSubtask;
            for (int i = 0; i < expectedSubtasks.size(); i++) {
                expectedSubtask = expectedSubtasks.get(i);
                actualSubtask = actualSubtasks.get(i);
                if (!new EqualsBuilder().
                        append(expectedSubtask.getSubtaskProviderType(),
                                actualSubtask.getSubtaskProviderType()).
                        append(expectedSubtask.getOperation(),
                                actualSubtask.getOperation()).
                        append(expectedSubtask.getPriority(),
                                actualSubtask.getPriority()).
                        append(expectedSubtask.getProvider(),
                                actualSubtask.getProvider()).
                        append(expectedSubtask.getSubtaskProperties(),
                                actualSubtask.getSubtaskProperties()).
                        isEquals()) {
                    return "Difference in PARAMS subtask: TASK row "
                            + rowNum + "; subtask " + i;
                }
                String compareResults = equalResults(
                        expectedSubtask.getResults(),
                        actualSubtask.getResults());
                if (compareResults != null) {
                    return "Difference in PARAMS subtask: TASK row "
                            + rowNum + "; subtask " + i
                            + "; " + compareResults;
                }
            }
            break;
        case "RESPONSE":
            Map<String, String> expectedResponse = JSONSerialization.
            deserializeStringAsJson((String) expectedValue,
                    new TypeReference<Map<String, String>>() { });
            Map<String, String> actualResponse = JSONSerialization.
            deserializeStringAsJson((String) actualValue,
                    new TypeReference<Map<String, String>>() { });
            String compareResults = equalResults(expectedResponse,
                    actualResponse);
            if (compareResults != null) {
                return "Difference in RESPONSE subtask: TASK row "
                        + rowNum + ";  " + compareResults;
            }
            break;
        default:
            throw new DatabaseUnitException("Unexpected column name: "
                    + columnName);
        }
        return null;
    }

    /** Compare expected and actual results Maps.
     * @param expectedResults The Map of expected results.
     * @param actualResults The Map of actual results.
     * @return null, if the maps are equal. Otherwise, a String that
     *      gives an explanation of the first difference found.
     */
    private String equalResults(final Map<String, String> expectedResults,
            final Map<String, String> actualResults) {
        if (expectedResults.size() != actualResults.size()) {
            return "Difference in results size: expected "
                    + expectedResults.size()
                    + "; actual " + actualResults.size();
        }
        for (String key : expectedResults.keySet()) {
            // And this is the main idea: skip, if key="timestamp".
            if (key.equals(TaskRunner.TIMESTAMP)) {
                continue;
            }
            if (!expectedResults.get(key).equals(actualResults.get(key))) {
                return "Difference in results for key " + key
                        + ": expected "
                        + expectedResults.get(key)
                        + "; actual " + actualResults.get(key);
            }
        }
        return null;
    }
}
