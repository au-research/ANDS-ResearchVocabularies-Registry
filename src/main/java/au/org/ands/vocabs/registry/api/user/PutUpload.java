/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.jax.rs.annotations.Pac4JProfile;
import org.pac4j.jax.rs.annotations.Pac4JSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.auth.AuthUtils;
import au.org.ands.vocabs.registry.api.context.ApiPaths;
import au.org.ands.vocabs.registry.api.context.EntityPaths;
import au.org.ands.vocabs.registry.api.context.ResponseUtils;
import au.org.ands.vocabs.registry.api.context.SwaggerInterface;
import au.org.ands.vocabs.registry.db.context.DBContext;
import au.org.ands.vocabs.registry.db.dao.UploadDAO;
import au.org.ands.vocabs.registry.db.entity.Upload;
import au.org.ands.vocabs.registry.utils.Logging;
import au.org.ands.vocabs.registry.utils.RegistryFileUtils;
import au.org.ands.vocabs.registry.utils.fileformat.FileFormat;
import au.org.ands.vocabs.registry.utils.fileformat.UploadFormatUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.ResponseHeader;

/** REST web services for uploading files. */
@Path(ApiPaths.API_RESOURCE)
@Api(value = SwaggerInterface.TAG_RESOURCES,
        authorizations = {@Authorization(value = SwaggerInterface.BASIC_AUTH),
        @Authorization(value = SwaggerInterface.API_KEY_AUTH)})
public class PutUpload {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Regular expression that specifies what is a valid filename,
     * after the sanitization process has been applied. It specifies
     * a format in which there is exactly one period, with at least
     * one other character before and after it. No begin/end anchors
     * need to be included in the regular expression, as this is to be
     * used with {@link java.util.regex.Matcher#matches()}.
     * This pattern has unit tests in
     * {@link TestPutUpload#testFilenamePattern()}. */
    public static final Pattern FILENAME_PATTERN =
            Pattern.compile("[^.]+\\.[^.]+");

    /** Create a new upload.
     * The size of allowed uploads should be locked down in the
     * Apache HTTP Server config using a LimitRequestBody directive
     * on the path to this method.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile.
     * @param owner The owner to store against the upload.
     * @param format The format of the file being uploaded.
     * @param file The uploaded file.
     * @param fileDisposition Metadata about the file upload, including
     *      the filename.
     * @return The list of vocabularies, in either XML or JSON format. */
    @Path(ApiPaths.UPLOADS)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Pac4JSecurity
    @POST
    @ApiOperation(value = "Upload a file.",
            notes = " The response contains the ID "
                    + "of the resulting upload in the integerValue field, "
                    + "and the sanitized filename in the stringValue field.",
            code = HttpStatus.SC_CREATED,
            responseHeaders = {
                    @ResponseHeader(name = "Location",
                      description = "URL of the newly-created upload",
                            response = URL.class)
            },
            response = SimpleResult.class)
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_BAD_REQUEST,
                    message = "Invalid input",
                    response = ErrorResult.class),
            @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED,
                    message = "Not authenticated",
                    response = ErrorResult.class,
                    responseHeaders = {
                            @ResponseHeader(name = "WWW-Authenticate",
                                    response = String.class)
                            }),
            @ApiResponse(code = HttpStatus.SC_FORBIDDEN,
                    message = "Not authenticated, or not authorized",
                    response = ErrorResult.class),
            @ApiResponse(code = HttpStatus.SC_REQUEST_TOO_LONG,
                    message = "Uploaded content too large",
                    response = Void.class),
            })
    public final Response createUpload(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(hidden = true) @Pac4JProfile final CommonProfile profile,
            @ApiParam(value = "The owner of the upload",
                required = true)
            @NotNull(message = "The owner must not be null")
            @QueryParam("owner") final String owner,
            @ApiParam(value = "The type of the upload",
                required = true)
            @NotNull(message = "The format must not be null")
            @QueryParam("format") final String format,
            @ApiParam(value = "The file to upload",
                required = true)
            @NotNull(message = "The file must not be null")
            @FormDataParam("file") final InputStream file,
            @FormDataParam("file") final FormDataContentDisposition
                    fileDisposition) {
        logger.debug("called createUpload");

        String filename = fileDisposition.getFileName();

        logger.debug("upload format: " + format);
        logger.debug("filename: " + filename);
        logger.debug("name: " + fileDisposition.getName());
        logger.debug("size: " + fileDisposition.getSize());
        logger.debug("type: " + fileDisposition.getType());
        logger.debug("creationDate: " + fileDisposition.getCreationDate());

        Logging.logRequest(true, request, uriInfo, profile,
                "Create upload, format: " + format);

        if (!AuthUtils.ownerIsAuthorizedByOrganisationOrUsername(profile,
                owner)) {
            return ResponseUtils.generateForbiddenResponseForOwner();
        }

        // Check the format.
        FileFormat formatFromFormat =
                UploadFormatUtils.getFileFormatByName(format);
        if (formatFromFormat == null) {
            return ErrorResultUtils.badRequest("Unsupported format.");
        }

        // Validate and normalize the filename.
        if (StringUtils.isEmpty(filename)) {
            return ErrorResultUtils.badRequest("Filename not specified.");
        }
        filename = sanitizeFilename(filename);
        if (!FILENAME_PATTERN.matcher(filename).matches()) {
            return ErrorResultUtils.badRequest(
                    "Filename not in the correct format, e.g., myfile.xml.");
        }

        // Now check that the extension matches the format.
        // Convert the extension to lower case before looking up.
        String extension = FilenameUtils.getExtension(filename).toLowerCase();
        FileFormat formatFromExtension =
                UploadFormatUtils.getFileFormatByExtension(extension);
        if (formatFromFormat != formatFromExtension) {
            return ErrorResultUtils.badRequest(
                    "Filename extension does not correspond to the format.");
        }

        Upload upload = new Upload();
        upload.setModifiedBy(profile.getUsername());
        upload.setOwner(owner);
        upload.setFormat(format);
        upload.setFilename(filename);

        EntityManager em = null;
        EntityTransaction txn = null;

        try {
            em = DBContext.getEntityManager();
            txn = em.getTransaction();
            txn.begin();

            // Record in the database.
            UploadDAO.saveUpload(em, upload);

            // Next, save the file, using the ID from the just-created
            // upload entity.
            File outputFile = new File(RegistryFileUtils.getUploadFilename(
                    upload.getId()));
            logger.info("Filename for upload: " + outputFile);
            boolean saveSuccess =
                    RegistryFileUtils.saveUploadInputStreamToFile(
                    upload.getId().toString(), file);
            if (!saveSuccess) {
                // Escape from here. The database transaction will
                // be rolled back, and an error status will be returned
                // to the client.
                throw new IOException("Unable to save upload.");
            }
            // And now, commit all of the above changes.
            txn.commit();
            SimpleResult simpleResult = new SimpleResult(upload.getId());
            simpleResult.setStringValue(filename);
            return Response.created(EntityPaths.getURIOfEntity(upload)).
                    entity(simpleResult).build();
        } catch (Throwable t) {
            if (txn != null && txn.isActive()) {
                try {
                    logger.error("Exception during transaction; rolling back",
                            t);
                    txn.rollback();
                } catch (Exception e) {
                    logger.error("Rollback failure!", e);
                }
            }
            // Don't throw, but fall through so that the user sees
            // an error message.
        } finally {
            if (em != null) {
                em.close();
            }
        }

        // If we fell through to here: ouch.
        return ErrorResultUtils.internalServerError();
    }

    // Optimize search/replace by compiling regular expressions.

    /** Pattern that matches everything up to and including the last
     * slash and/or backslash. Use with
     * {@link java.util.regex.Matcher#replaceFirst(String)}. */
    private static final Pattern SLASH_BACKSLASH_PATTERN =
            Pattern.compile("^.*[/\\\\]");

    /** Pattern that matches any period that is not the last period. Use with
     * {@link java.util.regex.Matcher#replaceAll(String)}. */
    private static final Pattern ONE_EXTENSION_PATTERN =
            Pattern.compile("\\.(?![^.]+$)");

    /** Pattern that matches characters that are considered reserved
     * for the purposes of filenames. See the section
     * "Reserved characters and words" listed at the <a
     * href="https://en.wikipedia.org/wiki/Filename#Reserved_characters_and_words">Wikipedia
     * entry for Filename</a>.
     * However, slash and backslash are not matched, as all instances
     * will already have been removed using {@link #SLASH_BACKSLASH_PATTERN}.
     * And, we also match all Unicode whitespace and control characters.
     * Use with
     * {@link java.util.regex.Matcher#replaceAll(String)}.
     */
    private static final Pattern RESERVED_CHARACTERS_PATTERN =
            Pattern.compile("\\?|%|\\*|:|\\||\"|<|>|\\p{Space}|\\p{Cntrl}");

    /** Pattern that matches contiguous sequences of underscores. Use with
     * {@link java.util.regex.Matcher#replaceAll(String)}. */
    private static final Pattern UNDERSCORES_PATTERN =
            Pattern.compile("_+");

    /** Sanitize a filename.
     * Remove any prefixed path, i.e., everything up to the last slash
     * and/or backslash. (Most/all browsers don't include the path,
     * but who knows?)
     * Enforce at most one extension, by replacing all but the last period
     * with an underscore.
     * Replace all occurrences of ?, %, *, :, |, &quot;, &lt;, &gt;,
     * Unicode whitespace, and Unicode control characters with an underscore.
     * (Any slashes and backslashes were already removed in the first step.)
     * The list of characters to be replaced is based on the
     * "Reserved characters and words" listed at the <a
     * href="https://en.wikipedia.org/wiki/Filename#Reserved_characters_and_words">Wikipedia
     * entry for Filename</a>.
     * Contiguous underscores are then coalesced.
     * This method has unit tests in
     * {@link TestPutUpload#testSanitizeFilename()}.
     * @param filename The filename to be sanitized.
     * @return The sanitized filename.
     */
    public static String sanitizeFilename(final String filename) {
        // Implementation inspired by FCKeditor's Utils class.
        // Remove any beginning path, i.e. everything up to the last slash
        // and/or backslash.
        String name = SLASH_BACKSLASH_PATTERN.matcher(filename).
                replaceFirst("");
        // Ensure at most one extension,
        // by replacing all but the last period with an underscore.
        name = ONE_EXTENSION_PATTERN.matcher(name).replaceAll("_");
        // Replace any remaining "Reserved characters and words" as per
        // https://en.wikipedia.org/wiki/Filename#Reserved_characters_and_words
        // (but not periods, and we already removed slashes and backslashes),
        // and Unicode whitespace and control characters, with underscores.
        name = RESERVED_CHARACTERS_PATTERN.matcher(name).replaceAll("_");
        // Coalesce contiguous underscores. We live with the fact that
        // the user might actually have provided a filename with contiguous
        // underscores.
        return UNDERSCORES_PATTERN.matcher(name).replaceAll("_");
    }

}
