/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.auth;

import java.lang.invoke.MethodHandles;

import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.CommonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.roles.db.utils.RolesUtils;

/** Authenticator that accepts a username/password contained in
 * HTTP basic authentication, and checks them against the roles
 * databases's built-in authentication table.
 */
public class BuiltinAuthenticator
    implements Authenticator<UsernamePasswordCredentials> {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Validate against the "built-in" usernames/passwords.
     */
    @Override
    public void validate(final UsernamePasswordCredentials credentials,
            final WebContext context) throws HttpAction, CredentialsException {
        if (credentials == null) {
            throwsException("No credential to check");
        }
        String username = credentials.getUsername();
        String password = credentials.getPassword();
        if (CommonHelper.isBlank(username)) {
            throwsException("Username cannot be blank");
        }
        if (CommonHelper.isBlank(password)) {
            throwsException("Password cannot be blank");
        }

        // We have a username and password to check. Look them up.

        if (!RolesUtils.isValidBuiltinRole(username, password)) {
//            throw HttpAction.unauthorized("Username : '" + username
//                    + "' supplied, but either wrong password or no such user",
//                    context, "My realm");

            throwsException("Username : '" + username
                    + "' supplied, but either wrong password or no such user");
        }
        final CommonProfile profile = new CommonProfile();
        profile.setId(username);
        profile.addAttribute(Pac4jConstants.USERNAME, username);
        credentials.setUserProfile(profile);
        logger.info("Successful login for: " + username);
        // To think about: send back a JWT as a cookie.
//        context.addResponseCookie(arg0);
    }

    /** Helper method that generates a CredentialsException.
     * @param message The message contained in the exception.
     * @throws CredentialsException Always generated.
     */
    protected void throwsException(final String message)
            throws CredentialsException {
        throw new CredentialsException(message);
    }

}
