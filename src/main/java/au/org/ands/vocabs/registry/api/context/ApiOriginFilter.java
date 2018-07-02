/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.context;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerResponseFilter;

import au.org.ands.vocabs.registry.api.auth.AuthConfig;
import au.org.ands.vocabs.registry.log.Analytics;

/** Servlet filter for the registry API that addresses CORS issues.
 * We could have done this as a {@link ContainerResponseFilter},
 * but doing it as a servlet {@link Filter} means it can be
 * applied more generally (e.g., to /swagger.json).
 *
 * Based on the Swagger sample ApiOriginFilter class here:
 * https://github.com/swagger-api/swagger-samples/blob/master/java/java-jersey2/src/main/java/io/swagger/sample/util/ApiOriginFilter.java
 *  */
public class ApiOriginFilter implements Filter {

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
    }

    /** Filter that adds the CORS headers needed to allow API use in
     * browsers, e.g., by Swagger UI.
     */
    @SuppressWarnings("checkstyle:DesignForExtension")
    @Override
    public void doFilter(final ServletRequest request,
            final ServletResponse response,
            final FilterChain chain) throws IOException, ServletException {
        HttpServletRequest hsRequest = (HttpServletRequest) request;
        String origin = hsRequest.getHeader("Origin");
        // If no Origin header was provided, no CORS applies.
        if (origin != null) {
            HttpServletResponse hsResponse = (HttpServletResponse) response;
            // Send back the origin we got ...
            hsResponse.addHeader("Access-Control-Allow-Origin", origin);
            // ... because we allow credentials.
            hsResponse.addHeader("Access-Control-Allow-Credentials", "true");
            // And see the advice at
            // https://www.w3.org/TR/cors/#resource-implementation
            // to add a header "Vary: Origin" in this case.
            hsResponse.addHeader("Vary", "Origin");
            // PUT and DELETE are not allowed by default, so add them.
            // (GET, HEAD, and POST do not need to be mentioned.)
            hsResponse.addHeader("Access-Control-Allow-Methods",
                    "PUT, DELETE");
            hsResponse.addHeader("Access-Control-Allow-Headers",
//                "Content-Type, api_key, Authorization");
                    "Content-Type, Authorization, "
                    + AuthConfig.RDA_COOKIE_NAME
                    + ", "
                    + Analytics.PORTAL_ID
                    + ", "
                    + Analytics.PORTAL_REFERRER
                    );
            hsResponse.addHeader("Access-Control-Expose-Headers",
//                  "Content-Type, api_key, Authorization");
                    // Content-Disposition added to support downloads.
                    "WWW-Authenticate, Location, Content-Disposition");
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

}
