/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.auth;

import org.pac4j.core.profile.CommonProfile;

import au.org.ands.vocabs.roles.utils.RolesConstants;

/** Utility methods to support authentication and authorization. */
public final class AuthUtils {

    /** Private constructor for a utility class. */
    private AuthUtils() {
    }

    /** Determine if a user's profile makes them
     * a registry "superuser".
     * @param profile The user's profile.
     * @return true, if the user's profile contains the role that makes
     *      them a superuser.
     */
    public static boolean profileIsSuperuser(final CommonProfile profile) {
        return profile.getPermissions().contains(
                RolesConstants.AUTH_FUNCTION_SUPERUSER);
    }

    /** Determine if a user's profile authorizes them to add/modify objects
     * with a particular owner value. Either, the user must be
     * a registry "superuser", or the owner value must be the name
     * of an organisational role which is included in the profile's
     * roles, or the owner value must match the user's username.
     * @param profile The user's profile.
     * @param owner The owner value.
     * @return true, if the user's profile authorizes them to add/modify
     *      objects with the owner value.
     */
    public static boolean ownerIsAuthorizedByOrganisationOrUsername(
            final CommonProfile profile,
            final String owner) {
        return profile.getPermissions().contains(
                RolesConstants.AUTH_FUNCTION_SUPERUSER)
                || profile.getRoles().contains(owner)
                || profile.getUsername().equals(owner);
    }

    /** Determine if a user's profile authorizes them to add/modify objects
     * with a particular owner value. Either, the user must be
     * a registry "superuser", or the owner value must be the name
     * of an organisational role which is included in the profile's
     * roles.
     * @param profile The user's profile.
     * @param owner The owner value.
     * @return true, if the user's profile authorizes them to add/modify
     *      objects with the owner value.
     */
    public static boolean ownerIsAuthorizedByOrganisation(
            final CommonProfile profile,
            final String owner) {
        return profile.getPermissions().contains(
                RolesConstants.AUTH_FUNCTION_SUPERUSER)
                || profile.getRoles().contains(owner);
    }

}
