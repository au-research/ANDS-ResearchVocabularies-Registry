/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.utils;

import au.org.ands.vocabs.toolkit.test.arquillian.ArquillianTestUtils;

/** Enumerated type to select one of the databases. Used as a parameter
 * of the methods of {@link ArquillianTestUtils}.
 */
public enum DatabaseSelector {

    /** The roles database. */
    ROLES,

    /** The Toolkit database. */
    TOOLKIT,

    /** The Registry database. */
    REGISTRY;

    /** Get the database name as a lowercase String that can be used as
     * a component of a filename, or for related purposes.
     * @return The name of the database, as a lowercase String.
     */
    public String getNameLowerCase() {
        return name().toLowerCase();
    }


    /** Get the filename of the DbUnit DTD for this database.
     * @return The filename of the DbUnit DTD for this database. The
     *      filename begins with path "test/".
     */
    public String getDTDFilename() {
        return "test/dbunit-" + name().toLowerCase() + "-export-choice.dtd";
    }

    /** Get the filename of an XML file that contains blank DbUnit test data
     * for this database.
     * @return The filename of an XML file that contains blank DbUnit test data
     *      for this database. The filename begins with path "test/".
     */
    public String getBlankDataFilename() {
        return "test/blank-" + name().toLowerCase() + "-dbunit.xml";
    }

}
