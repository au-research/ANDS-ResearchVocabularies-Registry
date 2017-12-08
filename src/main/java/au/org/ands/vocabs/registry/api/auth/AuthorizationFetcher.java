/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.auth;

import java.lang.invoke.MethodHandles;

import org.pac4j.core.authorization.generator.AuthorizationGenerator;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.definition.CommonProfileDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.roles.Role;
import au.org.ands.vocabs.roles.UserInfo;
import au.org.ands.vocabs.roles.db.context.RolesUtils;
import au.org.ands.vocabs.roles.db.entity.RoleTypeId;

/** Fetch authorization details for the profile of an authenticated user. */
public class AuthorizationFetcher
implements AuthorizationGenerator<CommonProfile> {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Attribute added to a profile to mark it as complete. This is
     * then used to recognize a profile that has come from the cache.
     */
    private static final String COMPLETE_PROFILE = "complete";

    /** Add authorization details to a profile.
     * This means getting the roles for the user.
     * @param ctx The web context.
     * @param profile The profile to be updated.
     * @return The updated profile.
     */
    @Override
    public CommonProfile generate(final WebContext ctx,
            final CommonProfile profile) {
        logger.info("auth processing: " + profile.getId());
        logger.info("full incoming profile: " + profile);
        if (profile.getAttribute(COMPLETE_PROFILE) != null) {
            logger.info("attribute " + COMPLETE_PROFILE
                    + " is non-null; returning");
            return profile;
        }
        try {
            UserInfo userInfo = RolesUtils
                    .getUserInfoForRole(profile.getId());
            logger.info("got some roles");
            // Store the UserInfo itself, so that it can be retrieved
            // later (e.g., to get full names of organisational roles).
            profile.addAttribute(AuthConstants.USER_INFO, userInfo);
            // Now fill in the rest of the profile.
            profile.addAttribute(CommonProfileDefinition.DISPLAY_NAME,
                    userInfo.getFullName());
            // Iterate over (what we consider) roles.
            // our organisational roles become pac4j roles;
            // our functional roles become pac4j permissions.
            for (Role role : userInfo.getParentRoles()) {
                if (role.getTypeId() == RoleTypeId.ROLE_ORGANISATIONAL) {
                    profile.addRole(role.getId());
                } else if (role.getTypeId() == RoleTypeId.ROLE_FUNCTIONAL) {
                    profile.addPermission(role.getId());
                }
            }
        } catch (IllegalArgumentException e) {
            // No role found.
            logger.info("No roles found");
        }
        // Add permissions for consistency with additional functional roles
        // provided by CodeIgniter.
        profile.addPermission(
                AuthConstants.AUTH_FUNCTION_DEFAULT_ATTRIBUTE);
        profile.addPermission(
                AuthConstants.AUTH_FUNCTION_LOGGED_IN_ATTRIBUTE);

        // Mark the profile as "complete". This is then used at the
        // beginning of this method to detect a profile that has
        // been fetched from the cache.
        profile.addAttribute(COMPLETE_PROFILE, true);
        return profile;
    }

}
