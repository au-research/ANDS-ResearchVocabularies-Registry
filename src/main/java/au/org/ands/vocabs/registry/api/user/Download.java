/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.api.user;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.context.ApiPaths;
import au.org.ands.vocabs.registry.api.context.SwaggerInterface;
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.dao.AccessPointDAO;
import au.org.ands.vocabs.registry.db.dao.VersionDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.db.entity.AccessPoint;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.db.internal.ApFile;
import au.org.ands.vocabs.registry.db.internal.ApSesameDownload;
import au.org.ands.vocabs.registry.utils.RegistryNetUtils;
import au.org.ands.vocabs.registry.utils.SlugGenerator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;

/** REST web services for downloading a vocabulary. */
@Path(ApiPaths.API_RESOURCE + "/" + ApiPaths.DOWNLOADS)
@Api(value = SwaggerInterface.TAG_RESOURCES)
public class Download {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());


    /** Mapping of Sesame Download formats to MIME types. */
    public static final Hashtable<String, String>
    SESAME_FORMAT_TO_MIMETYPE_MAP =
    new Hashtable<>();

    // List taken from:
    // http://rdf4j.org/sesame/2.8/docs/system.docbook?view#content-types
    static {
        SESAME_FORMAT_TO_MIMETYPE_MAP.put("rdf", "application/rdf+xml");
        SESAME_FORMAT_TO_MIMETYPE_MAP.put("nt", "text/plain");
        SESAME_FORMAT_TO_MIMETYPE_MAP.put("ttl", "text/turtle");
        SESAME_FORMAT_TO_MIMETYPE_MAP.put("n3", "text/rdf+n3");
        SESAME_FORMAT_TO_MIMETYPE_MAP.put("nq", "text/x-nquads");
        SESAME_FORMAT_TO_MIMETYPE_MAP.put("json", "application/rdf+json");
        SESAME_FORMAT_TO_MIMETYPE_MAP.put("trix", "application/trix");
        SESAME_FORMAT_TO_MIMETYPE_MAP.put("trig", "application/x-trig");
        SESAME_FORMAT_TO_MIMETYPE_MAP.put("bin", "application/x-binary-rdf");
    }

    /** Mapping of file formats to MIME types. */
    public static final Hashtable<String, String>
    FILE_FORMAT_TO_MIMETYPE_MAP =
    new Hashtable<>();

    // The keys should match those in:
    // ANDS-Registry-Core/applications/portal/vocabs/assets/js/versionCtrl.js
    static {
        FILE_FORMAT_TO_MIMETYPE_MAP.put("RDF/XML", "application/rdf+xml");
        FILE_FORMAT_TO_MIMETYPE_MAP.put("TTL", "text/turtle");
        FILE_FORMAT_TO_MIMETYPE_MAP.put("N-Triples", "text/plain");
        FILE_FORMAT_TO_MIMETYPE_MAP.put("JSON", "application/json");
        FILE_FORMAT_TO_MIMETYPE_MAP.put("TriG", "application/x-trig");
        FILE_FORMAT_TO_MIMETYPE_MAP.put("TriX", "application/trix");
        FILE_FORMAT_TO_MIMETYPE_MAP.put("N3", "text/rdf+n3");
        FILE_FORMAT_TO_MIMETYPE_MAP.put("CSV", "text/csv");
        FILE_FORMAT_TO_MIMETYPE_MAP.put("TSV", "text/csv");
        FILE_FORMAT_TO_MIMETYPE_MAP.put("XLS", "application/vnd.ms-excel");
        FILE_FORMAT_TO_MIMETYPE_MAP.put("XLSX",
                "application/vnd.openxmlformats-officedocument."
                + "spreadsheetml.sheet");
        FILE_FORMAT_TO_MIMETYPE_MAP.put("BinaryRDF",
                "application/x-binary-rdf");
        FILE_FORMAT_TO_MIMETYPE_MAP.put("ODS",
                "application/vnd.oasis.opendocument.spreadsheet");
        FILE_FORMAT_TO_MIMETYPE_MAP.put("ZIP", "application/zip");
        FILE_FORMAT_TO_MIMETYPE_MAP.put("XML", "application/xml");
        FILE_FORMAT_TO_MIMETYPE_MAP.put("TXT", "text/plain");
        FILE_FORMAT_TO_MIMETYPE_MAP.put("ODT",
                "application/vnd.oasis.opendocument.text");
        FILE_FORMAT_TO_MIMETYPE_MAP.put("TEXT", "text/plain");
//        FILE_FORMAT_TO_MIMETYPE_MAP.put("", "");

//        FILE_FORMAT_TO_MIMETYPE_MAP.put("nq", "text/x-nquads");
//        FILE_FORMAT_TO_MIMETYPE_MAP.put("json", "application/rdf+json");
    }



    /** Get the download for an access point. The Path for this
     * method does not include a filename, but allows for a format
     * query parameter.
     * @param response Asynchronous response for this request
     * @param accessPointId Access point id. The id of the access point
     *      in the access_points database table.
     * @param downloadFormat The download format. This may be
     * ignored, depending (for example) on the access point type.
     * Allowed values are the keys of {@link #SESAME_FORMAT_TO_MIMETYPE_MAP}.
     */
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    @Path("{downloadId}")
    @GET
    @ApiOperation(value = "Get a download.",
        notes = "In this version of the download method, a download format is "
                + "specified as a query parameter. "
                + "The response will include a Content-Disposition "
                + "header that specifies a full filename based on the "
                + "underlying data; "
                + "its extension may be different from the value of the "
                + "format query parameter.",
        responseHeaders = {
                @ResponseHeader(name = "Content-Disposition",
                        description = "Content disposition of the download, "
                                + "including a filename",
                        response = String.class)
        },
        response = File.class)
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_NOT_FOUND,
                    message = "No such download.",
                    response = String.class)})
    public final void download(
            @Suspended final AsyncResponse response,
            @ApiParam(value = "The Id of the download.")
            @PathParam("downloadId")
            final int accessPointId,
            @ApiParam(value = "The format of the download. This must be "
                    + "a filename extension without the period, "
                    + "e.g., \"rdf\".")
            @DefaultValue("rdf")
            @QueryParam("format")
            final String downloadFormat) {
        logger.info("Called download: " + accessPointId
                + ", download format: " + downloadFormat);
        AccessPoint ap = AccessPointDAO.getCurrentAccessPointByAccessPointId(
                accessPointId);
        if (ap == null) {
            response.resume(Response.status(Status.NOT_FOUND).
                    type(MediaType.TEXT_PLAIN).
                    entity("Not found: no such access point").build());
            return;
        }

        switch (ap.getType()) {
        case FILE:
            // For now, transforms for file access points are not supported,
            // so we don't look at downloadFormat. In future, we _may_ support
            // transforms for file access points. If that happens, note that
            // downloadWithFilename() and
            // downloadWithFilenameWithoutExtension() delegate to this method,
            // and the downloadFormat parameter may need some care
            // (i.e., it may be wrong, but the wrongness may or may not be
            // a problem).
            fileDownload(response, ap);
            break;
        case SESAME_DOWNLOAD:
            // Have a look at the downloadFormat before proceeding.
            final String mimeType =
                    SESAME_FORMAT_TO_MIMETYPE_MAP.get(downloadFormat);
            if (mimeType == null) {
                response.resume(Response.status(Status.NOT_FOUND).
                        type(MediaType.TEXT_PLAIN).
                        entity("Not found: no such format").build());
                return;
            }
            sesameDownload(response, accessPointId, ap,
                    downloadFormat, mimeType);
            return;
        default:
            logger.error("download: invalid type for access point: "
                    + ap.getType());
            response.resume(Response.status(Status.NOT_FOUND).
                    type(MediaType.TEXT_PLAIN).
                    entity("Invalid access point type").build());
            return;
        }
    }

    /** Get the download for an access point. The Path for this
     * method includes a filename with an extension.
     * The use of this method enables the URL to contain a path component
     * with a filename. This enables the use of e.g., curl or wget
     * to save the download to the "correct" filename.
     * Because Path annotation template variables are greedy, in a
     * case where the request URL contains multiple periods, the
     * extension variable is assigned only the component after
     * the last period.
     * @param response Asynchronous response for this request
     * @param accessPointId Access point id.
     * @param filename The filename specified in the URL. This may
     * be ignored in constructing the response headers.
     * @param extension The download format. This may be
     * ignored, depending (for example) on the access point type. */
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    @Path("{downloadId}/{filename}.{extension}")
    @GET
    @ApiOperation(value = "Get a download.",
        notes = "In this version of the download method, the last component of "
                + "the URL specifies a filename and an extension. "
                + "The response will include a Content-Disposition "
                + "header that specifies a filename and extension based on "
                + "the underlying data; they may be different from the values "
                + "specified.",
        responseHeaders = {
                @ResponseHeader(name = "Content-Disposition",
                        description = "Content disposition of the download, "
                                + "including a filename",
                        response = String.class)
        },
        response = File.class)
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_NOT_FOUND,
                    message = "No such download.",
                    response = String.class)})
    public final void downloadWithFilename(
            @Suspended final AsyncResponse response,
            @ApiParam(value = "The Id of the download.")
            @PathParam("downloadId")
            final int accessPointId,
            @ApiParam(value = "The filename of the download.")
            @PathParam("filename")
            final String filename,
            @ApiParam(value = "The format of the download. This must be "
                    + "a filename extension without the period, "
                    + "e.g., \"rdf\".")
            @DefaultValue("rdf")
            @PathParam("extension")
            final String extension) {
        logger.info("Called downloadWithFilename: " + accessPointId
                + ", filename: " + filename
                + ", extension: " + extension);
        download(response, accessPointId, extension);
    }

    /** Return a file download.
     * @param response The response back to the browser.
     * @param ap The access point.
     */
    private void fileDownload(final AsyncResponse response,
            final AccessPoint ap) {
        ApFile apFile = JSONSerialization.deserializeStringAsJson(ap.getData(),
                ApFile.class);
        String format = apFile.getFormat();
        if (format == null) {
            response.resume(Response.status(Status.NOT_FOUND).
                    type(MediaType.TEXT_PLAIN).
                    entity("Not found: no format specified "
                            + "for access point").build());
            return;
        }
        String responseMimeType =
                FILE_FORMAT_TO_MIMETYPE_MAP.get(
                        format);
        if (responseMimeType == null) {
            response.resume(Response.status(Status.NOT_FOUND).
                    type(MediaType.TEXT_PLAIN).
                    entity("Not found: no such format").build());
            return;
        }

        String localPath = apFile.getPath();
        logger.debug("Getting download from file: " + localPath
                + ", MIME type = " + responseMimeType);
        String downloadFilename = "download";
        try {
            downloadFilename = new File(new URL(apFile.getUrl()).
                    getPath()).getName();
        } catch (MalformedURLException e) {
            logger.error("Unable to parse access point's URL: "
                    + apFile.getUrl(), e);
        }

        InputStream fileStream;
        try {
            fileStream = new FileInputStream(
                    new File(localPath));
        } catch (FileNotFoundException e) {
            logger.error("download: file not found: "
                    + localPath, e);
            response.resume(Response.status(Status.NOT_FOUND).
                    type(MediaType.TEXT_PLAIN).
                    entity("File not found").build());
            return;
        }

        response.resume(Response.ok(fileStream).
                        header("Content-Disposition",
                                "attachment; filename="
                        + downloadFilename).
                        header("Content-Type",
                                responseMimeType + ";charset=UTF-8").
                        build());
    }

    /** Return a download from Sesame.
     * @param response The response back to the browser.
     * @param accessPointId The access point id.
     * @param ap The access point.
     * @param downloadFormat The download format.
     * @param mimeType The MIME type of the download.
     */
    private void sesameDownload(final AsyncResponse response,
            final int accessPointId, final AccessPoint ap,
            final String downloadFormat, final String mimeType) {
        ApSesameDownload apSesameDownload =
                JSONSerialization.deserializeStringAsJson(ap.getData(),
                        ApSesameDownload.class);
        String sesameUri = apSesameDownload.getServerBase();

        final String downloadFilename = downloadFilename(ap, downloadFormat);

        // Prepare the connection to Sesame.
        Client client = RegistryNetUtils.getClient();
        WebTarget target = client.target(sesameUri).
                path("repositories").
                path(apSesameDownload.getRepository()).path("statements");

        logger.debug("Getting download from " + target.toString()
                + ", downloadFormat = " + downloadFormat);

        final Invocation.Builder invocationBuilder =
                target.request(mimeType);

        // Now go into a separate thread to manage the tunneling.
        new Thread(new Runnable() {
            @Override
            public void run() {
                Response sesameResponse = invocationBuilder.get();
                if (sesameResponse.getStatus()
                        >= Response.Status.BAD_REQUEST.getStatusCode()) {
                    logger.error("download from Sesame got an error "
                            + "from Sesame; "
                            + "accessPointId: " + accessPointId);
                    response.resume(Response.status(Status.NOT_FOUND).
                            type(MediaType.TEXT_PLAIN).
                            entity("Not found: no such access point").build());
                    return;
                }

                // Oops, we don't do sesameResponseStream.close().
                // We hope it gets cleaned up in the end.
                InputStream sesameResponseStream =
                        sesameResponse.readEntity(InputStream.class);
                response.resume(Response.ok(sesameResponseStream).
                        header("Content-Disposition",
                                "attachment; filename="
                        + downloadFilename).
                        header("Content-Type",
                                mimeType + ";charset=UTF-8").
                        build());
            } }).start();
    }

    /** Generate the filename to use for the download.
     * @param ap The access point
     * @param downloadFormat The download format
     * @return The generated filename.
     */
    public static String downloadFilename(final AccessPoint ap,
            final String downloadFormat) {
        // Work out the filename that the download should have.
        Version version = VersionDAO.getCurrentVersionByVersionId(
                ap.getVersionId());
        Vocabulary vocabulary =
                VocabularyDAO.getCurrentVocabularyByVocabularyId(
                        version.getVocabularyId());
        final String downloadFilename =
                SlugGenerator.generateSlug(vocabulary.getOwner())
                + "_"
                + vocabulary.getSlug()
                + "_"
                + version.getSlug()
                + "." + downloadFormat;
        return downloadFilename;
    }

}
