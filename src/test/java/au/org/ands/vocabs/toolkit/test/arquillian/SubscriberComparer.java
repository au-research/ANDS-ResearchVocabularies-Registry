/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dbunit.DatabaseUnitException;
import org.dbunit.assertion.comparer.value.ValueComparer;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.datatype.DataType;

/** Class for use with DbUnit to compare expected and actual Subscriber
 * database entities. */
public class SubscriberComparer implements ValueComparer {

    /** A Pattern that describes a valid subscriber token. */
    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("(\\d)+_[A-Za-z0-9]+");

    /** Compare expected and actual values from a subscriber. */
    @Override
    public String compare(final ITable expectedTable,
            final ITable actualTable, final int rowNum,
            final String columnName, final DataType dataType,
            final Object expectedValue, final Object actualValue)
            throws DatabaseUnitException {
        switch (columnName) {
        case "TOKEN":
            String expectedToken = (String) expectedValue;
            String actualToken = (String) actualValue;
            Matcher expectedMatcher = TOKEN_PATTERN.matcher(expectedToken);
            if (!expectedMatcher.matches()) {
                return "Problem with expected subscriber token: "
                        + "SUBSCRIBER row " + rowNum
                        + "; expected token value (" + expectedToken
                        + ") is not in the right format";
            }
            String expectedId = expectedMatcher.group(1);
            Matcher actualMatcher = TOKEN_PATTERN.matcher(actualToken);
            if (!actualMatcher.matches()) {
                return "Problem with actual subscriber token: "
                        + "SUBSCRIBER row " + rowNum
                        + "; actual token value (" + actualToken
                        + ") is not in the right format";
            }
            String actualId = actualMatcher.group(1);
            if (!expectedId.equals(actualId)) {
                return "Difference in TOKEN Id: SUBSCRIBER row "
                        + rowNum + "; expected " + expectedToken
                        + "; actual " + actualToken;
            }
            break;
        default:
            throw new DatabaseUnitException("Unexpected column name: "
                    + columnName);
        }
        return null;
    }

}
