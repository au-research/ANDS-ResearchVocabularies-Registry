/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

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

    // TO DO: make use of allowedExtensions!

    /** A set containing the extensions permitted on the filenames
     * of uploaded files. */
    private static Set<String> allowedExtensions;

    static {
        // cf. setting in portal controllers/vocabs.php/upload().

        allowedExtensions = new HashSet<>();
        allowedExtensions.add("csv");
        allowedExtensions.add("json");
        allowedExtensions.add("n3");
        allowedExtensions.add("nt");
        allowedExtensions.add("ods");
        allowedExtensions.add("pdf");
        allowedExtensions.add("rdf");
        allowedExtensions.add("trig");
        allowedExtensions.add("trix");
        allowedExtensions.add("tsv");
        allowedExtensions.add("ttl");
        allowedExtensions.add("txt");
        allowedExtensions.add("xls");
        allowedExtensions.add("xlsx");
        allowedExtensions.add("xml");
        allowedExtensions.add("zip");
    }


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
    @ApiOperation(value = "Upload a file. The response contains the ID "
            + "of the resulting upload in the integerValue field.",
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

        // TO DO: make use of allowedExtensions!
        // And otherwise, do validation, e.g., of the format parameter

        // TO DO: normalize the filename, e.g., to remove characters
        // that could be dodgy when included in HTML, e.g., "<",.

        logger.debug("upload format: " + format);
        logger.debug("filename: " + fileDisposition.getFileName());
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

        Upload upload = new Upload();
        upload.setModifiedBy(profile.getUsername());
        upload.setOwner(owner);
        upload.setFormat(format);
        upload.setFilename(fileDisposition.getFileName());

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
            return Response.created(EntityPaths.getURIOfEntity(upload)).
                    entity(new SimpleResult(upload.getId())).build();
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

}
