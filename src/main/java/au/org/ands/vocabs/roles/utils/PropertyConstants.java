/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.roles.utils;

/** Definition of constants that refer to names of properties used
 * by the Roles system. Use these values as the parameter of
 * {@link RolesProperties#getProperty(String)} and related methods.
 */
public final class PropertyConstants {

    /** Private constructor for a utility class. */
    private PropertyConstants() {
    }

    /* Authentication. */

    /** Name of the authentication cookies that come from
     * Research Data Australia (RDA). */
    public static final String ROLES_RDA_COOKIE_NAME =
            "Roles.rdaCookieName";

    /** A default/fallback value for ROLES_RDA_COOKIE_NAME. */
    public static final String ROLES_RDA_COOKIE_NAME_DEFAULT =
            "ands_authentication";

    /** Key used to generate hashes appended to authentication cookies
     * that come from Research Data Australia (RDA). */
    public static final String ROLES_RDA_COOKIE_KEY =
            "Roles.rdaCookieKey";

    /** The time zone of the issuer of cookies.
     * The "last_activity" values contained in the cookies are specified
     * as seconds since the Unix epoch, but the epoch is relative to a
     * particular time zone! So, specify here the time zone with which
     * to interpret the last_activity values.
     * The time zone names are the Java names. */
    public static final String ROLES_RDA_COOKIE_TIME_ZONE =
            "Roles.rdaCookieTimeZone";

    /** The validity time of an authentication cookie that comes from
     * RDA, in seconds. The value specified here should match the value
     * set in the portal's global_config.php. */
    public static final String ROLES_RDA_SESSION_TIMEOUT =
            "Roles.rdaSessionTimeout";

    /** A default/fallback value for ROLES_RDA_SESSION_TIMEOUT. */
    public static final int ROLES_RDA_SESSION_TIMEOUT_DEFAULT =
            7200;

//  /** Roles . */
//  public static final String ROLES_ =
//          "Roles.";



}
