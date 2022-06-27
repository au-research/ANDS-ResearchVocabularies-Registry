/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.auth;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.definition.CommonProfileDefinition;
import org.pac4j.core.util.CommonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.dao.SubscriberDAO;
import au.org.ands.vocabs.registry.db.dao.SubscriberEmailAddressDAO;
import au.org.ands.vocabs.registry.db.entity.Subscriber;
import au.org.ands.vocabs.registry.db.entity.SubscriberEmailAddress;

/** Methods for authenticating a subscriber. */
public class SubscriberAuthenticator
    implements Authenticator<TokenCredentials> {

    /** Logger for this class. */
    private static Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Query parameter used to specify a subscriber email address. */
    public static final String SUBSCRIBER_EMAIL_ADDRESS =
            "emailAddress";

    /** Query parameter used to specify a subscriber ID. */
    public static final String SUBSCRIBER_ID = "subscriberId";

    /** Query parameter used to specify a subscriber token. */
    public static final String TOKEN = "token";

    /** Validate a Subscriber using their token. */
    @Override
    public void validate(final TokenCredentials credentials,
            final WebContext context) throws HttpAction, CredentialsException {
        if (credentials == null) {
            throwsException("No credential to check");
        }
        String token = credentials.getToken();
        if (CommonHelper.isBlank(token)) {
            throwsException("Token cannot be blank");
        }

        // We have a token to check.
        logger.debug("Got a token. Will check it.");

        // The format is expected to be "2_rf6FlQkBj", with an underscore
        // separating the subscriber ID from the "password-like" component.
        // Yes, 2 + 1 = 3. We expect 2 components, but we allow for
        // splitting into one additional component, so we can see if the
        // caller provided _exactly_ 2 components.
        String[] tokenSplit =  token.split("_", 2 + 1);
        if (tokenSplit.length != 2) {
            throwsException("Token not in correct format: "
                    + "wrong number of components");
        }

        Integer id = null;
        try {
            id = Integer.valueOf(tokenSplit[0]);
        } catch (NumberFormatException nfe) {
            throwsException("Token not in correct format: no ID component");
        }

        Subscriber subscriber;
        SubscriberEmailAddress sea = null;
        subscriber = SubscriberDAO.getCurrentSubscriberBySubscriberId(id);
        if (subscriber == null) {
            throwsException("No current subscriber with that ID");
        }

        if (!token.equals(subscriber.getToken())) {
            throwsException("Incorrect token for subscriber");
        }

        // Now get their email address.
        List<SubscriberEmailAddress> seaList = SubscriberEmailAddressDAO.
                getCurrentSubscriberEmailAddressListForSubscriber(id);
        if (seaList.size() > 0) {
            sea = seaList.get(0);
        } else {
            throwsException("No email address defined for subscriber ID");
        }
        // Override anything provided by the caller.
        String emailAddress = sea.getEmailAddress();

        // We made it. Encode the subscriber as a profile.

        final CommonProfile profile = new CommonProfile();
        String idString = Integer.toString(id);
        profile.setId(idString);
        profile.addAttribute(Pac4jConstants.USERNAME, idString);
        profile.addAttribute(CommonProfileDefinition.EMAIL, emailAddress);
        credentials.setUserProfile(profile);
        logger.info("Successful subscriber credentials for: " + idString);
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
