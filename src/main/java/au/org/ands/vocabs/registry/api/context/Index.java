/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.context;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/** Handlers for the top level "front page" of the servlet. */
@Path("/")
public class Index {

    // It should be possible to specify just two methods with:
    // @Path("{a:|index.html?}") @Produces(MediaType.TEXT_HTML)
    // and:
    // @Path("{a:|index.txt}") @Produces(MediaType.TEXT_PLAIN)
    // This works for the paths "index.htm", "index.html", and "index.txt".
    // But it doesn't work in the case where the client
    // requests the path "/" and asks for HTML. In this case, it's
    // the method for TEXT_PLAIN that matches. Why?
    // So, instead, it turns out that we need to have all four of these
    // methods, to cover the possible combinations.

    /** The content to be returned for the index page, in HTML format. */
    public static final String INDEX_HTML_CONTENT =
            "<html><body>Nothing to see here.</body></html>";

    /** The content to be returned for the index page, in plain text format. */
    public static final String INDEX_PLAIN_TEXT_CONTENT =
            "Nothing to see here.";

    /** Return a response for the "index" page for the webapp, in HTML format.
     * This version of the method matches the empty path.
     * @return An "index" response, in HTML format.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response indexHtml() {
        return Response.ok().
                entity(INDEX_HTML_CONTENT).
                build();
    }

    /** Return a response for the "index" page for the webapp, in HTML format.
     * This version of the method matches the paths "index.html"
     * and "index.htm".
     * @return An "index" response, in HTML format.
     */
    @GET
    @Path("{a:index.html?}")
    @Produces(MediaType.TEXT_HTML)
    public Response indexHtml2() {
        return indexHtml();
    }

    /** Return a response for the "index" page for the webapp, in
     * plain text format.
     * This version of the method matches the empty path.
     * @return An "index" response, in plain text format.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response indexPlain() {
        return Response.ok().
                entity(INDEX_PLAIN_TEXT_CONTENT).build();
    }

    /** Return a response for the "index" page for the webapp, in
     * plain text format.
     * This version of the method matches the path "index.txt".
     * @return An "index" response, in plain text format.
     */
    @GET
    @Path("index.txt")
    @Produces(MediaType.TEXT_PLAIN)
    public Response indexPlain2() {
        return indexPlain();
    }

}
