/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.utils;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

/** Utility methods for working with the network. */
public final class RegistryNetUtils {

    /** A shared Client resource, initialized on class loading.
     * Follows redirects. */
    // According to the documentation, FOLLOW_REDIRECTS is the default.
    // Specify it anyway, just in case that changes.
    private static Client client = ClientBuilder.newClient().
            property(ClientProperties.FOLLOW_REDIRECTS, true);

    /** A shared Client resource, initialized on class loading.
     * Does not follow redirects. */
    private static Client clientNoRedirects = ClientBuilder.newClient().
            property(ClientProperties.FOLLOW_REDIRECTS, false);

    /** Jersey Basic authentication feature. Used to initialize
     * clientBasicAuthentication. */
    private static HttpAuthenticationFeature basicAuthFeature =
            HttpAuthenticationFeature.basicBuilder().build();

    /** A shared Client resource, initialized on class loading.
     * Uses basic authentication. */
    private static Client clientBasicAuthentication =
            ClientBuilder.newClient().
            register(basicAuthFeature);

    /** Private constructor for a utility class. */
    private RegistryNetUtils() {
    }

    /** Get the shared Client resource that follows redirects.
     * @return The shared Client resource.
     */
    public static Client getClient() {
        return client;
    }

    /** Get the shared Client resource that does not follow redirects.
     * @return The shared Client resource.
     */
    public static Client getClientNoRedirects() {
        return clientNoRedirects;
    }

    /** Get the shared Client resource that does basic authentication.
     * @return The shared Client resource.
     */
    public static Client getClientBasicAuthentication() {
        return clientBasicAuthentication;
    }

    /** Timeout to use when establishing a connection to an external service. */
    private static final int TARGET_CONNECT_TIMEOUT =
            Integer.valueOf(RegistryProperties.getProperty(
                    PropertyConstants.REGISTRY_NETWORK_TIMEOUT_CONNECT,
                    "10000"));

    /** Timeout to use when reading from an external service. */
    private static final int TARGET_READ_TIMEOUT =
            Integer.valueOf(RegistryProperties.getProperty(
                    PropertyConstants.REGISTRY_NETWORK_TIMEOUT_READ,
                    "10000"));

    /** Set the connect timeout and read timeout on an invocation builder
     * as per the matching Registry properties.
     * The documentation <a href=
     * "https://docs.oracle.com/javase/8/docs/api/java/net/URLConnection.html#setReadTimeout-int-">https://docs.oracle.com/javase/8/docs/api/java/net/URLConnection.html#setReadTimeout-int-</a>
     * says: "If the timeout expires before there is data available for read,
     * a java.net.SocketTimeoutException is raised."
     * @param invocationBuilder The invocation builder for which the
     *  timeouts are to be set.
     */
    public static void setTimeouts(final Invocation.Builder invocationBuilder) {
        // Follow suggestion at:
        // https://stackoverflow.com/questions/19543209/
        //     how-to-set-the-connection-and-read-timeout-with-jersey-2-x
        invocationBuilder.property(ClientProperties.CONNECT_TIMEOUT,
                TARGET_CONNECT_TIMEOUT);
        invocationBuilder.property(ClientProperties.READ_TIMEOUT,
                TARGET_READ_TIMEOUT);
    }

    /** Prepare for shutdown. Call this only in webapp context shutdown! */
    public static void doShutdown() {
        client.close();
        clientNoRedirects.close();
        clientBasicAuthentication.close();
    }

}
