/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.utils;

import java.util.Locale;

import au.org.ands.vocabs.registry.db.entity.AccessPoint;
import au.org.ands.vocabs.registry.db.entity.ResourceMapEntry;
import au.org.ands.vocabs.registry.db.entity.ResourceOwnerHost;
import au.org.ands.vocabs.registry.db.entity.Task;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;

/** Constants for use with DbUnit. */
public final class DbUnitConstants {

    /** Private constructor for a utility class. */
    private DbUnitConstants() {
    }

    /** The name of the database table underlying the AccessPoint entity
     * class, converted to upper-case for use by DbUnit. */
    public static final String ACCESSPOINT_TABLE_NAME =
            AccessPoint.TABLE_NAME.toUpperCase(Locale.ROOT);

    /** The name of the database table underlying the ResourceMapEntry entity
     * class, converted to upper-case for use by DbUnit. */
    public static final String RESOURCEMAPENTRY_TABLE_NAME =
            ResourceMapEntry.TABLE_NAME.toUpperCase(Locale.ROOT);

    /** The name of the database table underlying the ResourceOwnerHost entity
     * class, converted to upper-case for use by DbUnit. */
    public static final String RESOURCEOWNERHOST_TABLE_NAME =
            ResourceOwnerHost.TABLE_NAME.toUpperCase(Locale.ROOT);

    /** The name of the database table underlying the Task entity
     * class, converted to upper-case for use by DbUnit. */
    public static final String TASK_TABLE_NAME =
            Task.TABLE_NAME.toUpperCase(Locale.ROOT);

    /** The name of the database table underlying the Version entity
     * class, converted to upper-case for use by DbUnit. */
    public static final String VERSION_TABLE_NAME =
            Version.TABLE_NAME.toUpperCase(Locale.ROOT);

    /** The name of the database table underlying the Vocabulary entity
     * class, converted to upper-case for use by DbUnit. */
    public static final String VOCABULARY_TABLE_NAME =
            Vocabulary.TABLE_NAME.toUpperCase(Locale.ROOT);

}
