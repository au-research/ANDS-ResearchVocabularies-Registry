/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.auth;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.codec.digest.HmacUtils;
import org.lorecraft.phparser.SerializedPhpParser;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.CommonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import au.org.ands.vocabs.roles.db.context.RolesUtils;
import au.org.ands.vocabs.roles.db.entity.AuthenticationServiceId;
import au.org.ands.vocabs.roles.utils.PropertyConstants;
import au.org.ands.vocabs.roles.utils.RolesProperties;

/** Authenticator that attempts to verify the contents of the
 * authentication cookie provided by CodeIgniter framework
 * as used by Research Data Australia (RDA).
 */
public class RdaCookieAuthenticator
    implements Authenticator<TokenCredentials> {

    /** The number of milliseconds in a second. */
    private static final long MS_IN_S = 1000L;

    /** Logger for this class. */
    private static Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** The length of the hash attached to the end of the token. */
    private static final int HASH_LENGTH = 40;

    /** The key used in generating HMAC values. */
    private static final String RDA_COOKIE_KEY = RolesProperties.getProperty(
            PropertyConstants.ROLES_RDA_COOKIE_KEY);

    /** The validity time of cookies. */
    private static final int RDA_COOKIE_TIMEOUT;

    /* Initialize RDA_COOKIE_TIMEOUT.
     * Fall back to ROLES_RDA_SESSION_TIMEOUT_DEFAULT, if not specified.
     * Do this, because Integer.parseInt() may throw an exception.
     */
    static {
        int timeout = PropertyConstants.ROLES_RDA_SESSION_TIMEOUT_DEFAULT;
        try {
            timeout = Integer.parseInt(RolesProperties
                    .getProperty(PropertyConstants.ROLES_RDA_SESSION_TIMEOUT));
        } catch (NumberFormatException e) {
            logger.error("Unable to parse setting for "
                    + PropertyConstants.ROLES_RDA_SESSION_TIMEOUT
                    + ": "
                    + RolesProperties.getProperty(
                            PropertyConstants.ROLES_RDA_SESSION_TIMEOUT), e);
        }
        RDA_COOKIE_TIMEOUT = timeout;
    }

    /** The key used in the credentials that contains the time of
     * last activity. */
    private static final String LAST_ACTIVITY =
            "last_activity";

    /** The key used in the credentials that contains the username. */
    private static final String UNIQUE_USER_IDENTIFIER =
            "UNIQUE_USER_IDENTIFIER";
    /** The string that ends the UNIQUE_USER_IDENTIFIER value,
     * and which must be stripped away. */
    private static final String UNIQUE_USER_IDENTIFIER_ENDING =
            "::";
    /** The length of UNIQUE_USER_IDENTIFIER_ENDING. */
    private static final int UNIQUE_USER_IDENTIFIER_ENDING_LENGTH =
            UNIQUE_USER_IDENTIFIER_ENDING.length();

    /** The key used in the credentials that contains the authentication
     * method. */
    private static final String AUTH_METHOD =
            "AUTH_METHOD";

    /** Private Jackson ObjectMapper used for logging of serialized JSON data
     * of authentication tokens into Strings.
     * It is initialized by a static block.
     * We use this rather than the JSONSerialization because
     * we need to adjust the FAIL_ON_EMPTY_BEANS setting. */
    private static ObjectMapper jsonMapper;

    static {
        jsonMapper = new ObjectMapper();
        // Don't serialize null values.  With this, but without the
        // JaxbAnnotationModule module registration
        // above, empty arrays that are values of a key/value pair
        // _would_ still be serialized.
        jsonMapper.setSerializationInclusion(Include.NON_NULL);
        // Need this to cope with "empty"/"null" values in the cookie.
        // For example, the cookie component 's:11:"AUTH_DOMAIN";N;'
        // is parsed into a key/value in which the value is
        // a SerializedPhpParser PhpObject, which can't be serialized
        // into a string. With this setting, serialization will not
        // fail, but will give an empty map, i.e., '"AUTH_DOMAIN":{}'.
        jsonMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    /** Validate against the RDA-issued authentication cookie. */
    @Override
    public void validate(final TokenCredentials credentials,
            final WebContext context) throws HttpAction, CredentialsException {
        if (credentials == null) {
            throwsException("No credential to check");
        }
        String tokenEncoded = credentials.getToken();
        if (CommonHelper.isBlank(tokenEncoded)) {
            throwsException("Token cannot be blank");
        }

        // We have a token to check.
        logger.info("Got a token. Will check it.");

        // It is URL encoded. Step one is to decode it.
        String token = null;
        try {
            token = URLDecoder.decode(tokenEncoded, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error("Doesn't know about UTF-8. Can that happen?", e);
            throw new CredentialsException("Unable to decode cookie");
        }

        // The following mimics the behaviour of CodeIgniter's
        // Session.php.

        // First, get the hash.
        int mapLength = token.length() - HASH_LENGTH;
        if (mapLength <= 0) {
            logger.error("Authentication cookie too short.");
            throw new CredentialsException("Authentication cookie is invalid");
        }
        // Split up the token into the credentials and the HMAC.
        String credentialsSerialized = token.substring(0, mapLength);
        String hmac = token.substring(mapLength);
        logger.info("Credential string: |" + credentialsSerialized
                + "|; hmac = |" + hmac + "|");

        // Check the hash.
        String hmacToCheck = HmacUtils.hmacSha1Hex(RDA_COOKIE_KEY,
                credentialsSerialized);

        int difference = 0;
        for (int i = 0; i < HASH_LENGTH; i++) {
            int xor = hmac.charAt(i) ^ hmacToCheck.charAt(i);
            difference |= xor;
        }
        if (difference != 0) {
            logger.info("HMAC check failed.");
            throw new CredentialsException("HMAC check failed");
        }

        // If we reach this point, the HMAC is OK.
        // Now, parse the authentication details.
        SerializedPhpParser serializedPhpParser =
                new SerializedPhpParser(credentialsSerialized);
        Object result = null;
        try {
            result = serializedPhpParser.parse();
        } catch (IllegalStateException e) {
            logger.info("Error parsing credentials", e);
            throw new CredentialsException("Error parsing credentials");
        }

        logger.info("Credentials as JSON: "
                + serializeObjectAsJsonString(result));
        if (!(result instanceof Map)) {
            logger.error("Unpacked credentials were not a map.");
            throw new CredentialsException(
                    "Unpacked credentials were not a map");
        }
        // Cast to a Map, so that we can extract values.
        Map<?, ?> map = null;
        try {
            map = (Map<?, ?>) result;
        } catch (ClassCastException e) {
            logger.info("ClassCastException that should not happen!", e);
            throw new CredentialsException(
                    "Unpacked credentials were not a map");
        }

        if (!map.containsKey(LAST_ACTIVITY)) {
            logger.error("Credentials don't contain key "
                    + LAST_ACTIVITY);
            throw new CredentialsException("Credentials don't contain key "
                    + LAST_ACTIVITY);
        }
        int lastActivity = (Integer) map.get(LAST_ACTIVITY);
        logger.info("Last activity: "
                + lastActivity);

        Calendar calendar = Calendar.getInstance(
                TimeZone.getTimeZone(RolesProperties.getProperty(
                        PropertyConstants.ROLES_RDA_COOKIE_TIME_ZONE)));
        long secondsSinceEpoch = calendar.getTimeInMillis() / MS_IN_S;
        logger.info("Current time: "
                + secondsSinceEpoch);
        logger.info("Difference: " + (secondsSinceEpoch - lastActivity));
        if (secondsSinceEpoch - lastActivity > RDA_COOKIE_TIMEOUT) {
            logger.error("Credentials valid, but have expired");
            throw new CredentialsException("Credentials expired");
        }

        if (!map.containsKey(UNIQUE_USER_IDENTIFIER)) {
            logger.error("Credentials don't contain key "
                    + UNIQUE_USER_IDENTIFIER
                    + "; this is probably a public user");
            throw new CredentialsException("Credentials don't contain key "
                    + UNIQUE_USER_IDENTIFIER
                    + "; this is probably a public user");
        }
        String uniqueUserIdentifier = (String) map.get(UNIQUE_USER_IDENTIFIER);
        if (!uniqueUserIdentifier.endsWith(UNIQUE_USER_IDENTIFIER_ENDING)) {
            logger.error("Value of " + UNIQUE_USER_IDENTIFIER
                    + " doesn't end with " + UNIQUE_USER_IDENTIFIER_ENDING);
            throw new CredentialsException("Value of " + UNIQUE_USER_IDENTIFIER
                    + " doesn't end with " + UNIQUE_USER_IDENTIFIER_ENDING);
        }
        String username = uniqueUserIdentifier.substring(0,
                uniqueUserIdentifier.length()
                - UNIQUE_USER_IDENTIFIER_ENDING_LENGTH);

        if (!map.containsKey(AUTH_METHOD)) {
            logger.error("Credentials don't contain key "
                    + AUTH_METHOD);
            throw new CredentialsException("Credentials don't contain key "
                    + AUTH_METHOD);
        }
        String authMethodString = (String) map.get(AUTH_METHOD);
        AuthenticationServiceId authType =
                AuthenticationServiceId.valueOf(authMethodString);

        if (!RolesUtils.isValidRole(username, authType)) {
            throw new CredentialsException("Username : '" + username
                    + "' supplied, but doesn't match a valid role");
        }
        final CommonProfile profile = new CommonProfile();
        profile.setId(username);
        profile.addAttribute(Pac4jConstants.USERNAME, username);
        credentials.setUserProfile(profile);
        logger.info("Successful login for: " + username);
    }

    /** Helper method that generates a CredentialsException.
     * @param message The message contained in the exception.
     * @throws CredentialsException Always generated.
     */
    protected void throwsException(final String message)
            throws CredentialsException {
        throw new CredentialsException(message);
    }

    /** Serialize an object into a String in JSON format.
     * This is a private version of the method in JSONSerialization,
     * needed, because we have our own private mapper configured
     * differently.
     * @param object The Object to be serialized.
     * @return The serialization as a JSON String of object.
     */
    private static String serializeObjectAsJsonString(final Object object) {
        try {
            return jsonMapper.writeValueAsString(object);
        } catch (IOException e) {
            logger.error("Unable to serialize as JSON", e);
            return null;
        }
    }

}
