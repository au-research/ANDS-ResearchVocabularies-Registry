/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.utils;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.function.Function;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.utils.RegistryNetUtils;

/** Utility methods for testing the toolkit as a remote client. */
public final class NetClientUtils {

    /** Logger for this class. */
    private static Logger logger;

    static {
        logger = LoggerFactory.getLogger(
                MethodHandles.lookup().lookupClass());
    }

    /** Private constructor for a utility class. */
    private NetClientUtils() {
    }

    /** Perform a GET request of a remote server.
     * Redirects are followed.
     * No MediaType is specified in the request.
     * @param baseURL The base URL of the server.
     * @param path The path to the request; appended to {@code baseURL}.
     * @return The response from the GET request. It is the responsibility
     * of the caller to invoke the {@code close()} method on the response.
     */
    public static Response doGet(final URL baseURL,
            final String path) {
        logger.info("doGet: baseURL = " + baseURL + "; path = " + path);
        Client client = RegistryNetUtils.getClient();
        WebTarget target = client.target(baseURL.toString()).
                path(path);
        Response response = target.request().get();
        return response;
    }

    /** Perform a GET request of a remote server.
     * Redirects are followed.
     * @param baseURL The base URL of the server.
     * @param path The path to the request; appended to {@code baseURL}.
     * @param responseMediaType The MediaType to be requested of the server.
     * @return The response from the GET request. It is the responsibility
     * of the caller to invoke the {@code close()} method on the response.
     */
    public static Response doGet(final URL baseURL,
            final String path, final MediaType responseMediaType) {
        logger.info("doGet: baseURL = " + baseURL + "; path = " + path
                + "; responseMediaType = " + responseMediaType);
        Client client = RegistryNetUtils.getClient();
        WebTarget target = client.target(baseURL.toString()).
                path(path);
        Response response =
                target.request(responseMediaType).get();
        return response;
    }

    /** Perform a GET request of a remote server.
     * Redirects are followed.
     * Additional components are applied to the WebTarget before it
     * is used. These components can be, for example, adding
     * query parameters. No MediaType is
     * specified in the request.
     * @param baseURL The base URL of the server.
     * @param path The path to the request; appended to {@code baseURL}.
     * @param additionalComponents The additional operations applied to
     *      the WebTarget, before it is used.
     * @return The response from the GET request. It is the responsibility
     * of the caller to invoke the {@code close()} method on the response.
     */
    public static Response doGetWithAdditionalComponents(final URL baseURL,
            final String path,
            final Function<WebTarget, WebTarget> additionalComponents) {
        logger.info("doGetWithAdditionalComponents: baseURL = " + baseURL
                + "; path = " + path);
        Client client = RegistryNetUtils.getClient();
        WebTarget target = client.target(baseURL.toString()).path(path);
        target = additionalComponents.apply(target);
        Response response = target.request().get();
        return response;
    }

    /** Perform a GET request of a remote server.
     * Redirects are followed.
     * Additional components are applied to the WebTarget before it
     * is used. These components can be, for example, adding
     * query parameters.
     * @param baseURL The base URL of the server.
     * @param path The path to the request; appended to {@code baseURL}.
     * @param responseMediaType The MediaType to be requested of the server.
     * @param additionalComponents The additional operations applied to
     *      the WebTarget, before it is used.
     * @return The response from the GET request. It is the responsibility
     * of the caller to invoke the {@code close()} method on the response.
     */
    public static Response doGetWithAdditionalComponents(final URL baseURL,
            final String path, final MediaType responseMediaType,
            final Function<WebTarget, WebTarget> additionalComponents) {
        logger.info("doGetWithAdditionalComponents: baseURL = " + baseURL
                + "; path = " + path);
        Client client = RegistryNetUtils.getClient();
        WebTarget target = client.target(baseURL.toString()).path(path);
        target = additionalComponents.apply(target);
        Response response = target.request(responseMediaType).get();
        return response;
    }

    /** Perform a GET request of a remote server.
     * Basic authentication is used; you must provide a username and password.
     * Additional components are applied to the WebTarget before it
     * is used. These components can be, for example, adding
     * query parameters.
     * @param baseURL The base URL of the server.
     * @param path The path to the request; appended to {@code baseURL}.
     * @param responseMediaType The MediaType to be requested of the server.
     * @param username The username to send to the server.
     * @param password The password to send to the server.
     * @param additionalComponents If not null, additional operations
     *      applied to the WebTarget, before it is used.
     * @return The response from the GET request. It is the responsibility
     * of the caller to invoke the {@code close()} method on the response.
     */
    public static Response doGetBasicAuthWithAdditionalComponents(
            final URL baseURL,
            final String path, final MediaType responseMediaType,
            final String username, final String password,
            final Function<WebTarget, WebTarget> additionalComponents) {
        logger.info("doGetBasicAuthWithAdditionalComponents: baseURL = "
            + baseURL + "; path = " + path);
        Client client = RegistryNetUtils.getClientBasicAuthentication();
        WebTarget target = client.target(baseURL.toString()).path(path);
        if (additionalComponents != null) {
            target = additionalComponents.apply(target);
        }
        Response response = target.request(responseMediaType).
                property(HttpAuthenticationFeature.
                        HTTP_AUTHENTICATION_BASIC_USERNAME, username).
                property(HttpAuthenticationFeature.
                        HTTP_AUTHENTICATION_BASIC_PASSWORD, password).get();
        return response;
    }

    /** Perform a GET request of a remote server.
     * Do not follow redirects.
     * Additional components are applied to the WebTarget before it
     * is used. These components can be, for example, adding
     * query parameters. No MediaType is
     * specified in the request.
     * @param baseURL The base URL of the server.
     * @param path The path to the request; appended to {@code baseURL}.
     * @param additionalComponents The additional operations applied to
     *      the WebTarget, before it is used.
     * @return The response from the GET request. It is the responsibility
     * of the caller to invoke the {@code close()} method on the response.
     */
    public static Response doGetWithAdditionalComponentsNoRedirects(
            final URL baseURL,
            final String path,
            final Function<WebTarget, WebTarget> additionalComponents) {
        logger.info("doGetWithAdditionalComponentsNoRedirects: baseURL = "
            + baseURL + "; path = " + path);
        Client client = RegistryNetUtils.getClientNoRedirects();
        WebTarget target = client.target(baseURL.toString()).path(path);
        target = additionalComponents.apply(target);
        Response response = target.request().get();
        return response;
    }

    /** Perform a PUT request of a remote server.
     * Basic authentication is used; you must provide a username and password.
     * Additional components are applied to the WebTarget before it
     * is used. These components can be, for example, adding
     * query parameters.
     * @param baseURL The base URL of the server.
     * @param path The path to the request; appended to {@code baseURL}.
     * @param responseMediaType The MediaType to be requested of the server.
     * @param username The username to send to the server.
     * @param password The password to send to the server.
     * @param additionalComponents The additional operations applied to
     *      the WebTarget, before it is used.
     * @param body The body to be sent. The content will be sent with
     *      MediaType XML.
     * @return The response from the GET request. It is the responsibility
     * of the caller to invoke the {@code close()} method on the response.
     */
    public static Response doPutBasicAuthWithAdditionalComponentsXml(
            final URL baseURL,
            final String path, final MediaType responseMediaType,
            final String username, final String password,
            final Function<WebTarget, WebTarget> additionalComponents,
            final String body) {
        logger.info("doGetBasicAuthWithAdditionalComponents: baseURL = "
            + baseURL + "; path = " + path);
        Client client = RegistryNetUtils.getClientBasicAuthentication();
        WebTarget target = client.target(baseURL.toString()).path(path);
        target = additionalComponents.apply(target);
        Response response = target.request(responseMediaType).
                property(HttpAuthenticationFeature.
                        HTTP_AUTHENTICATION_BASIC_USERNAME, username).
                property(HttpAuthenticationFeature.
                        HTTP_AUTHENTICATION_BASIC_PASSWORD, password).
                put(Entity.xml(body));
        return response;
    }


    /** Perform a DELETE request of a remote server.
     * Basic authentication is used; you must provide a username and password.
     * Additional components are applied to the WebTarget before it
     * is used. These components can be, for example, adding
     * query parameters.
     * @param baseURL The base URL of the server.
     * @param path The path to the request; appended to {@code baseURL}.
     * @param responseMediaType The MediaType to be requested of the server.
     * @param username The username to send to the server.
     * @param password The password to send to the server.
     * @param additionalComponents The additional operations applied to
     *      the WebTarget, before it is used.
     * @return The response from the DELETE request. It is the responsibility
     * of the caller to invoke the {@code close()} method on the response.
     */
    public static Response doDeleteWithAdditionalComponents(final URL baseURL,
            final String path, final MediaType responseMediaType,
            final String username, final String password,
            final Function<WebTarget, WebTarget> additionalComponents) {
        logger.info("doDelete: baseURL = " + baseURL + "; path = " + path
                + "; responseMediaType = " + responseMediaType);
        Client client = RegistryNetUtils.getClientBasicAuthentication();
        WebTarget target = client.target(baseURL.toString()).
                path(path);
        target = additionalComponents.apply(target);
        Response response =
                target.request(responseMediaType).
                property(HttpAuthenticationFeature.
                        HTTP_AUTHENTICATION_BASIC_USERNAME, username).
                property(HttpAuthenticationFeature.
                        HTTP_AUTHENTICATION_BASIC_PASSWORD, password).
                delete();
        return response;
    }

}
