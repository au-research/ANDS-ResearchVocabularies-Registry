/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.roles.utils;

/** Constants used within values stored in the tables of the roles database.
 */
public final class RolesConstants {

    /** Private constructor for a utility class. */
    private RolesConstants() {
    }

//    public static final String AUTH_FUNCTION_DEFAULT_ATTRIBUTE = "PUBLIC";
//
//    public static final String AUTH_FUNCTION_LOGGED_IN_ATTRIBUTE =
//            "AUTHENTICATED_USER";

    /** The name of the functional role representing registry superusers. */
    public static final String AUTH_FUNCTION_SUPERUSER =
            "VOCABS_REGISTRY_SUPERUSER";

    /** The name of the functional role representing users who are allowed to
     * work with all organizations. */
    public static final String AUTH_FUNCTION_ALL_GROUPS =
            "VOCABS_CONTENT_MANAGER";

}
