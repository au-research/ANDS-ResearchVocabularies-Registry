/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.utils;

import static net.logstash.logback.marker.Markers.append;
import static net.logstash.logback.marker.Markers.appendArray;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;

import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;
import com.maxmind.geoip2.record.Subdivision;

import net.logstash.logback.marker.LogstashMarker;

/** Support for analytics. Because it supports the
 * {@link au.org.ands.vocabs.registry.utils.Logging} class,
 * it makes use of the Logstash-Logback utility classes.
 */
public final class Analytics {

    /** Private constructor for a utility class. */
    private Analytics() {
    }

    /** The internal (normal) logger for this class. */
    private static Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** The name of the HTTP header used to indicate the URL of the
     * portal page that is responsible for generating this request.
     * The name "portal referrer" is slightly misleading. The
     * value of this header may indeed correspond to the value of the
     * original {@code HTTP_REFERER} header in some cases,
     * but the value may also be that of the request URI. See
     * the portal controller's {@code set_referrer_for_registry()}
     * method.*/
    public static final String PORTAL_REFERRER = "portal-referrer";

    /** The name of the HTTP header used to indicate that the request
     * came from the portal. */
    public static final String PORTAL_ID = "portal-id";

    // These are private fields.
    // The following fields are defined in alphabetical order
    // of field name.
    /** The name of the city name field inserted into log entries. */
    private static final String GEO_CITY_NAME_FIELD = "city_name";
    /** The name of the country name field inserted into log entries. */
    private static final String GEO_COUNTRY_NAME_FIELD = "country_name";
    /** The name of the location field inserted into log entries. */
    private static final String GEO_LOCATION_FIELD = "location";
    /** The name of the region code field inserted into log entries. */
    private static final String GEO_REGION_CODE_FIELD = "region_code";
    /** The name of the IP address field inserted into log entries. */
    private static final String IP_FIELD = "ip";
    /** The name of the "is_bot" field inserted into log entries. */
    private static final String IS_BOT_FIELD = "is_bot";
    /** The name of the HTTP method field inserted into log entries. */
    private static final String METHOD_FIELD = "method";
    /** The name of the URL path field inserted into log entries. */
    private static final String PATH_FIELD = "path";
    /** The name of the portal ID field inserted into log entries. */
    private static final String PORTAL_ID_FIELD = "portal_id";
    /** The name of the IP address field inserted into log entries,
     * where this is the IP address of the portal that made the request,
     * and the value of the {@code IP_FIELD} is the IP address
     * of the portal user. */
    private static final String PORTAL_IP_FIELD = "portal_ip";
    /** The name of the portal referrer field inserted into log entries. */
    private static final String PORTAL_REFERRER_FIELD =
            "portal_referrer";
    /** The name of the user agent field inserted into log entries. */
    private static final String USER_AGENT_FIELD = "user_agent";
    /** The name of the username field inserted into log entries. */
    private static final String USERNAME_FIELD = "username";
    /** The name of the uuid field inserted into log entries. */
    private static final String UUID_FIELD = "uuid";


    // These are public fields.
    // The following fields are defined in alphabetical order
    // of field name.
    /** The name of the entity status field inserted into log entries.
     * This is used to represent a <i>registry entity's</i> status, e.g.,
     * that a vocabulary has "deprecated" status. */
    public static final String ENTITY_STATUS_FIELD = "entity_status";
    /** The name of the failure reason field inserted into log entries. */
    public static final String FAILURE_REASON = "failure_reason";
    /** The name of the owner field inserted into log entries. */
    public static final String OWNER_FIELD = "owner";
    /** The name of the related entity ID field inserted into log entries. */
    public static final String RELATED_ENTITY_ID_FIELD = "relatedEntity_id";
    /** The name of the success field inserted into log entries. */
    public static final String SUCCESS_FIELD = "success";
    /** The name of the title field inserted into log entries. */
    public static final String TITLE_FIELD = "title";
    /** The name of the vocabulary ID field inserted into log entries. */
    public static final String VOCABULARY_ID_FIELD = "vocabulary_id";
    /** The name of the field inserted into log entries that indicates
     *  whether or not data was modified. */
    public static final String WAS_MODIFIED_FIELD = "was_modified";


    // Names of registry events. These go into the "message" field of the log
    // entry. Use names based on "CRUD", i.e.,
    // "create"/"read"/"update"/"delete".
//    /** The value of a prefix to use in the value of the "message" field
//     * used for log entries generated by the registry. */
//    private static final String EVENT_REGISTRY_PREFIX = "registry_";
    /** The value of the "message" field to use for log entries
     * for creating a vocabulary. */
    public static final String EVENT_CREATE_VOCABULARY =
            "create_vocabulary";
    /** The value of the "message" field to use for log entries
     * for getting a vocabulary. */
    public static final String EVENT_GET_VOCABULARY =
            "read_vocabulary";
    /** The value of the "message" field to use for log entries
     * for updating a vocabulary. */
    public static final String EVENT_UPDATE_VOCABULARY =
            "update_vocabulary";
    /** The value of the "message" field to use for log entries
     * for deleting a vocabulary. */
    public static final String EVENT_DELETE_VOCABULARY =
            "delete_vocabulary";
    /** The value of the "message" field to use for log entries
     * for creating a related entity. */
    public static final String EVENT_CREATE_RELATED_ENTITY =
            "create_relatedEntity";
    /** The value of the "message" field to use for log entries
     * for updating a related entity. */
    public static final String EVENT_UPDATE_RELATED_ENTITY =
            "update_relatedEntity";
    /** The value of the "message" field to use for log entries
     * for deleting a related entity. */
    public static final String EVENT_DELETE_RELATED_ENTITY =
            "delete_relatedEntity";


    /** The GeoIP2 database reader used for geo lookups of IP addresses. */
    private static DatabaseReader geoDbReader;

    static {
        String geoIPFile = RegistryProperties.getProperty(
                PropertyConstants.REGISTRY_LOGGING_GEOIPDB);
        if (geoIPFile == null) {
            logger.info("No GeoIP database specified.");
        } else {
            File database = new File(geoIPFile);
            try {
                geoDbReader = new DatabaseReader.Builder(database).build();
            } catch (IOException e) {
                logger.error("Unable to open GeoIP database", e);
            }
        }
    }

    /** Lookup an IP address in the GeoIP database, and add its location
     * to the Logstash Marker.
     * @param lm The Logstash Marker to which the location element is to
     *      be added.
     * @param ipAddressString The IP address to be looked up.
     */
    private static void addGeoIPInfo(final LogstashMarker lm,
            final String ipAddressString) {
        if (geoDbReader == null) {
            // Can't do anything, since (for some reason) there is
            // no GeoIP database available.
            return;
        }

        InetAddress ipAddress;
        try {
            ipAddress = InetAddress.getByName(ipAddressString);
        } catch (UnknownHostException e) {
            logger.error("Unable to parse IP address", e);
            return;
        }

        CityResponse response;
        try {
            response = geoDbReader.city(ipAddress);
        } catch (IOException | GeoIp2Exception e) {
            // Don't need to log a full stacktrace, as you typically
            // just need to see something like:
            // 2018-02-12 16:26:21,251 [http-bio-8080-exec-7]
            //  ERROR au.org.ands.vocabs.registry.utils.Analytics -
            //  Unable to look up IP address in GeoIP database:
            //  com.maxmind.geoip2.exception.AddressNotFoundException:
            //  The address 0:0:0:0:0:0:0:1 is not in the database.
            logger.error("Unable to look up IP address in "
                    + "GeoIP database: " + e);
            return;
        }

        Country country = response.getCountry();
//        country.getIsoCode()  = 'US'
//        country.getName()     = 'United States'

        Subdivision subdivision = response.getMostSpecificSubdivision();
//        subdivision.getName())    = 'Minnesota'
//        subdivision.getIsoCode()) = 'MN'

        City city = response.getCity();
//        city.getName() = 'Minneapolis'

//        Postal postal = response.getPostal();
//        postal.getCode() = '55455'

        Location location = response.getLocation();
//        location.getLatitude()   = 44.9733
//        location.getLongitude()) = -93.2323

        lm.and(append(GEO_COUNTRY_NAME_FIELD, country.getName())).
                and(append(GEO_CITY_NAME_FIELD, city.getName())).
                and(append(GEO_REGION_CODE_FIELD, subdivision.getIsoCode())).
                and(appendArray(GEO_LOCATION_FIELD,
                location.getLongitude(), location.getLatitude()));
    }

    /** Create a LogstashMarker with identification data, and basic data
     * from the request.
     * The identification data includes a randomly-generated UUID to
     * use for this log entry.
     * The IP address, method, and user agent are extracted from the request,
     * and the path from the uriInfo. If there is a user profile,
     * the username is extracted.
     * Geolocation data is added, based on the IP address.
     * There is an important trick to the handling of IP addresses.
     * If the request contains a header "portal-remote-address",
     * then this request is deemed to have been generated internally
     * by the portal, and the value of this header is the "real"
     * IP address of the client.
     * The value of this header is used as the IP address of the request,
     * and the IP address of the portal (i.e., the value of the
     * request's "official" remote address) is stored in a separate field.
     * If a header "portal-remote-address" is provided, there should
     * also be headers "portal-referrer" containing the URL
     * of the portal page, and "portal-user-agent", which is used
     * as the log entry's user agent in place of the portal's own.
     * @param success Whether or not the operation was completed successfully.
     *      If false, the caller should then add more details
     *      of the failure to the returned LogstashMarker.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile, or null, if there is none.
     * @return The new LogstashMarker, for use in a log entry.
     */
    public static LogstashMarker createBasicMarker(
            final boolean success,
            final HttpServletRequest request,
            final UriInfo uriInfo,
            final CommonProfile profile) {

//        portal-remote-address=130.1.2.3
//        portal-user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) ...
//        portal-referrer=https://vocabs.../viewById/2

        String requestRemoteAddress = request.getRemoteAddr();

        String portalRemoteAddress =
                request.getHeader("portal-remote-address");
        String portalReferrer =
                request.getHeader(PORTAL_REFERRER);
        String remoteAddress;
        if (portalRemoteAddress != null) {
            remoteAddress = portalRemoteAddress;
        } else {
            remoteAddress = requestRemoteAddress;
        }

        LogstashMarker lm = append(UUID_FIELD, UUID.randomUUID()).
                and(append(SUCCESS_FIELD, success)).
                and(append(IP_FIELD, remoteAddress)).
                and(append(METHOD_FIELD, request.getMethod())).
                and(append(PATH_FIELD, uriInfo.getPath()));
        // If there's a portal-remote-address, store the portal's
        // own IP address.
        if (portalRemoteAddress != null) {
            lm.and(append(PORTAL_IP_FIELD, requestRemoteAddress));
        }
        // If there's a portal-remote-referrer, store it.
        if (portalReferrer != null) {
            lm.and(append(PORTAL_REFERRER_FIELD, portalReferrer));
        }

        // Allow a portal-user-agent header to override user-agent.
        String userAgent = request.getHeader("portal-user-agent");
        if (userAgent == null) {
            userAgent = request.getHeader("user-agent");
        }
        if (userAgent != null) {
            // Log the user agent string, and whether it is a
            // known crawler/bot.
            lm.and(append(USER_AGENT_FIELD, userAgent)).
                and(append(IS_BOT_FIELD, BotDetector.isBot(userAgent)));
        }
        String portalId = request.getHeader(PORTAL_ID);
        if (portalId != null) {
            // This request came from the portal.
            lm.and(append(PORTAL_ID_FIELD, portalId));
        }
        if (profile != null) {
            lm.and(append(USERNAME_FIELD, profile.getId()));
        }
        addGeoIPInfo(lm, remoteAddress);
        return lm;
    }

    /** Update a LogstashMarker with identification data, and basic data
     * from the request.
     * The identification data includes a randomly-generated UUID to
     * use for this log entry.
     * The IP address, method, and user agent are extracted from the request,
     * and the path from the uriInfo. If there is a user profile,
     * the username is extracted.
     * Geolocation data is added, based on the IP address.
     * @param lm The LogstashMarker to be updated with basic data.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile, or null, if there is none.
     */
    public static void updateMarkerWithBasicFields(
            final LogstashMarker lm,
            final HttpServletRequest request,
            final UriInfo uriInfo,
            final CommonProfile profile) {
        lm.and(append(UUID_FIELD, UUID.randomUUID())).
                and(append(IP_FIELD, request.getRemoteAddr())).
                and(append(METHOD_FIELD, request.getMethod())).
                and(append(PATH_FIELD, uriInfo.getPath()));
        String userAgent = request.getHeader("user-agent");
        if (userAgent != null) {
            lm.and(append(USER_AGENT_FIELD, userAgent)).
                and(append(IS_BOT_FIELD, BotDetector.isBot(userAgent)));
        }
        if (profile != null) {
            lm.and(append(USERNAME_FIELD, profile.getId()));
        }
        addGeoIPInfo(lm, request.getRemoteAddr());
    }


}
