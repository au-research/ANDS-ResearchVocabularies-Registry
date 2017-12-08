/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.roles.db.context;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.commons.codec.digest.DigestUtils;

import au.org.ands.vocabs.roles.UserInfo;
import au.org.ands.vocabs.roles.db.entity.AuthenticationBuiltIn;
import au.org.ands.vocabs.roles.db.entity.AuthenticationServiceId;
import au.org.ands.vocabs.roles.db.entity.Role;
import au.org.ands.vocabs.roles.db.entity.RoleRelation;
import au.org.ands.vocabs.roles.db.entity.RoleTypeId;
import au.org.ands.vocabs.roles.utils.RolesConstants;

/** Work with database roles. */
public final class RolesUtils {

//    /** Logger for this class. */
//    private static Logger logger = LoggerFactory.getLogger(
//            MethodHandles.lookup().lookupClass());

    /** Private constructor for a utility class. */
    private RolesUtils() {
    }

    /** Get role by id.
     * @param id The id.
     * @return The role.
     */
    public static Role getRoleById(final int id) {
        EntityManager em = DBContext.getEntityManager();
        Role v = em.find(Role.class, id);
        em.close();
        return v;
    }

    /** Get roles by role id.
     * @param roleId role id
     * @return the roles
     */
    public static List<Role> getRolesByRoleId(final String roleId) {
        EntityManager em = DBContext.getEntityManager();
        TypedQuery<Role> query =
                em.createNamedQuery(Role.GET_ROLES_FOR_ROLEID, Role.class).
                setParameter(Role.GET_ROLES_FOR_ROLEID_ROLEID, roleId);
        List<Role> roles = query.getResultList();
        em.close();
        return roles;
    }

    /** Compute the transitive closure of the parent property.
     * @param roleId The roleId to be looked up.
     * @param userInfo The UserInfo data to be looked up. The
     *      isRegistrySuperUser property is set, if the appropriate
     *      role is found.
     */
    private static void updateRoles(final String roleId,
            final UserInfo userInfo) {
        Set<String> roleIdsSeen = new HashSet<>();
        Set<String> roleIdsUnseen = new HashSet<>();
        Set<au.org.ands.vocabs.roles.Role> parentRoles = new HashSet<>();
        // "Prime the pump" by adding the roleId to start from.
        // But it won't itself end up in the roles.
        roleIdsUnseen.add(roleId);
        EntityManager em = DBContext.getEntityManager();
        TypedQuery<au.org.ands.vocabs.roles.Role> query;
        List<au.org.ands.vocabs.roles.Role> roles;
        while (!roleIdsUnseen.isEmpty()) {
            String roleIdToLookUp = roleIdsUnseen.iterator().next();
            roleIdsUnseen.remove(roleIdToLookUp);
            roleIdsSeen.add(roleIdToLookUp);
            query = em.createNamedQuery(
                    RoleRelation.GET_PARENT_ROLES_FOR_ROLEID,
                    au.org.ands.vocabs.roles.Role.class).
            setParameter(RoleRelation.GET_PARENT_ROLES_FOR_ROLEID_ROLEID,
                    roleIdToLookUp);
            roles = query.getResultList();
            for (au.org.ands.vocabs.roles.Role role : roles) {
                String parentRoleId = role.getId();
                if (roleIdsUnseen.contains(parentRoleId)
                        || roleIdsSeen.contains(parentRoleId)) {
                    // We already know about this roleId.
                    continue;
                }
//            logger.info("I have not yet seen role: " + role.getFullName());
                roleIdsUnseen.add(parentRoleId);
                parentRoles.add(role);
                if (parentRoleId.equals(RolesConstants.AUTH_FUNCTION_SUPERUSER)
                        && role.getTypeId() == RoleTypeId.ROLE_FUNCTIONAL) {
                    userInfo.setIsSuperUser(true);
                }
            }
        }
        em.close();
        userInfo.setParentRoles(parentRoles);
    }

    /** Get the UserInfo data associated with a role.
     * @param roleId The role ID to be looked up.
     * @return A UserInfo object containing the role information
     *      associated with the role ID.
     * @throws IllegalArgumentException if there is no matching role.
     */
    public static UserInfo getUserInfoForRole(final String roleId) {
        UserInfo userInfo = new UserInfo();
        List<Role> roles = getRolesByRoleId(roleId);
        if (roles.size() == 0) {
            throw new IllegalArgumentException("No matching role");
        }
        // Sigh, "cope" with duplicates by just taking the first one.
        Role role = roles.get(0);
        userInfo.setId(roleId);
        userInfo.setAuthenticationServiceId(role.getAuthenticationServiceId());
        userInfo.setFullName(role.getName());
        updateRoles(roleId, userInfo);
        if (userInfo.getIsSuperUser() == null) {
            // Didn't find the superuser role anywhere, so make it
            // explicit that this user doesn't have such a role.
            userInfo.setIsSuperUser(false);
        }
        return userInfo;
    }

    /** Check a username/password combination against the built-in
     * roles.
     * @param username The username to be used as a role id to check.
     * @param password The password to be used as a check. Plain text.
     * @return True, if the username/password combination corresponds
     *      to a valid built-in role.
     */
    public static boolean isValidBuiltinRole(final String username,
            final String password) {
        EntityManager em = DBContext.getEntityManager();
        TypedQuery<Boolean> query =
                em.createNamedQuery(
                        AuthenticationBuiltIn.CHECK_USERNAME_PASSWORD,
                        Boolean.class).
                setParameter(
                        AuthenticationBuiltIn.CHECK_USERNAME_PASSWORD_ROLEID,
                        username).
                setParameter(
                        AuthenticationBuiltIn.
                        CHECK_USERNAME_PASSWORD_PASSPHRASE_SHA1,
                        DigestUtils.sha1Hex(password));
        Boolean isValid = query.getSingleResult();
        em.close();
        return isValid;
    }

    /** Check a username/authentication type combination against the
     * user roles.
     * @param username The username to be used as a role id to check.
     * @param authenticationType The authentication type to be used as a check.
     * @return True, if the username/authentication type combination corresponds
     *      to a valid user role.
     */
    public static boolean isValidRole(final String username,
            final AuthenticationServiceId authenticationType) {
        EntityManager em = DBContext.getEntityManager();
        TypedQuery<Boolean> query =
                em.createNamedQuery(
                        Role.CHECK_ROLE_EXISTS_ENABLED,
                        Boolean.class).
                setParameter(
                        Role.CHECK_ROLE_EXISTS_ENABLED_ROLEID,
                        username).
                setParameter(
                        Role.CHECK_ROLE_EXISTS_ENABLED_AUTHENTICATION_TYPE,
                        authenticationType);
        Boolean isValid = query.getSingleResult();
        em.close();
        return isValid;
    }

}
