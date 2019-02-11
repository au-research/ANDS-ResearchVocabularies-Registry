/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.log;

import static net.logstash.logback.marker.Markers.append;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
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

import au.org.ands.vocabs.registry.utils.BotDetector;
import au.org.ands.vocabs.registry.utils.PropertyConstants;
import au.org.ands.vocabs.registry.utils.RegistryProperties;
import net.logstash.logback.marker.LogstashMarker;

/** Support for analytics. Because it supports the
 * {@link au.org.ands.vocabs.registry.log.Logging} class,
 * it makes use of the Logstash-Logback utility classes.
 */
public final class Analytics {

    /** Private constructor for a utility class. */
    private Analytics() {
    }

    /** The internal (normal) logger for this class. */
    private static Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    // HTTP headers.

    /** The name of the HTTP header used to indicate that the request
     * came from the portal. */
    public static final String PORTAL_ID = "portal-id";
    /** The name of the HTTP header used to indicate the URL of the
     * portal page that is responsible for generating this request.
     * The name "portal referrer" is slightly misleading. The
     * value of this header may indeed correspond to the value of the
     * original {@code HTTP_REFERER} header in some cases,
     * but the value may also be that of the request URI. See
     * the portal controller's {@code set_referrer_for_registry()}
     * method.*/
    public static final String PORTAL_REFERRER = "portal-referrer";
    /** The name of the HTTP header used to indicate the IP address of the
     * portal page that is responsible for generating this request.
     */
    private static final String PORTAL_REMOTE_ADDRESS = "portal-remote-address";
    /** The name of the HTTP header used to provide the value of the
     * user-agent header that was provided as a request header to the
     * portal page that is responsible for generating this request.
     */
    private static final String PORTAL_USER_AGENT = "portal-user-agent";


    // These are private fields.
    // First, the top-level logging fields that are maps: i.e., they contain
    // subfields.
    // The following fields are defined in alphabetical order
    // of field name.
    /** The name of the field that groups together GeoIP data
     * inserted into log entries. */
    private static final String GEOIP_MAP_FIELD = "geoip";
    /** The name of the field that groups together portal data
     * inserted into log entries. */
    private static final String PORTAL_MAP_FIELD = "portal";
    /** The name of the field that groups together search data
     * inserted into log entries. */
    private static final String SEARCH_MAP_FIELD = "filters";
    /** The name of the field that groups together user data
     * inserted into log entries. */
    private static final String USER_MAP_FIELD = "user";
    /** The name of the field that groups together vocabulary data
     * inserted into log entries. */
    private static final String VOCABULARY_MAP_FIELD = "vocabulary";

    // And now, the other logging fields. Most of these are embedded in one of
    // the maps; the rest are top-level.
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
    private static final String PORTAL_REFERRER_FIELD = "portal_referrer";
    /** The name of the user agent field inserted into log entries. */
    private static final String USER_AGENT_FIELD = "user_agent";
    /** The name of the username field inserted into log entries. */
    private static final String USERNAME_FIELD = "username";
    /** The name of the uuid field inserted into log entries. */
    private static final String UUID_FIELD = "uuid";


    // These are public fields.
    // The following fields are defined in alphabetical order
    // of field name.
    // NB: some of these are not logging _fields_, but specific
    // _values_ (i.e., of an "enumerated type"). See, e.g.,
    // VOCABULARY_LOOKUP_BY_ID and VOCABULARY_LOOKUP_BY_SLUG.
    /** The name of the entity status field inserted into log entries.
     * This is used to represent a <i>registry entity's</i> status, e.g.,
     * that a vocabulary has "deprecated" status.
     * As this log field is embedded within the {@link #VOCABULARY_MAP_FIELD}
     * map, the use of the value "status" is unambiguous.*/
    public static final String ENTITY_STATUS_FIELD = "status";
    /** The name of the failure reason field inserted into log entries. */
    public static final String FAILURE_REASON = "failure_reason";
    /** The name of the owner field inserted into log entries. */
    public static final String OWNER_FIELD = "owner";
    /** The name of the related entity ID field inserted into log entries. */
    public static final String RELATED_ENTITY_ID_FIELD = "relatedEntity_id";
    /** The name of the search access field inserted into log entries. */
    public static final String SEARCH_ACCESS_FIELD = "access";
    /** The name of the search format field inserted into log entries. */
    public static final String SEARCH_FORMAT_FIELD = "format";
    /** The name of the search language field inserted into log entries. */
    public static final String SEARCH_LANGUAGE_FIELD = "language";
    /** The name of the search licence field inserted into log entries. */
    public static final String SEARCH_LICENCE_FIELD = "licence";
    /** The name of the search p field inserted into log entries. */
    public static final String SEARCH_P_FIELD = "p";
    /** The name of the search pp field inserted into log entries. */
    public static final String SEARCH_PP_FIELD = "pp";
    /** The name of the search publisher field inserted into log entries. */
    public static final String SEARCH_PUBLISHER_FIELD = "publisher";
    /** The name of the search q field inserted into log entries. */
    public static final String SEARCH_Q_FIELD = "q";
    /** The name of the search subject labels field inserted into
     * log entries. */
    public static final String SEARCH_SUBJECT_LABELS_FIELD = "subject";
    /** The name of the search widgetable field inserted into log entries. */
    public static final String SEARCH_WIDGETABLE_FIELD = "widgetable";
    /** The name of the success field inserted into log entries. */
    public static final String SUCCESS_FIELD = "success";
    /** The name of the slug field inserted into log entries. */
    public static final String SLUG_FIELD = "slug";
    /** The name of the title field inserted into log entries. */
    public static final String TITLE_FIELD = "title";
    /** The name of the vocabulary ID field inserted into log entries. */
    public static final String VOCABULARY_ID_FIELD = "id";
    /** The value to use for the {@link #VOCABULARY_LOOKUP_FIELD} field to
     * indicate that the vocabulary was looked up by its resource ID.*/
    public static final String VOCABULARY_LOOKUP_BY_ID = "id";
    /** The value to use for the {@link #VOCABULARY_LOOKUP_FIELD} field to
     * indicate that the vocabulary was looked up by its slug.*/
    public static final String VOCABULARY_LOOKUP_BY_SLUG = "slug";
    /** The name of the vocabulary lookup type field inserted into
     * log entries. */
    public static final String VOCABULARY_LOOKUP_FIELD = "lookup";
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
    /** The value of the "message" field to use for log entries
     * for search. */
    public static final String EVENT_SEARCH = "search";


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

        // If you need to debug the request headers, uncomment the following:
//        Enumeration<String> headerNames = request.getHeaderNames();
//        while (headerNames.hasMoreElements()) {
//            String headerName = headerNames.nextElement();
//            logger.info("HTTP header " + headerName + ": "
//                    + request.getHeader(headerName));
//        }

        // For a request that comes from the portal, we expect these
        // custom request headers:
        //  portal-remote-address=130.1.2.3
        //  portal-user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X ...
        //  portal-referrer=https://vocabs.../viewById/2

        // Get the IP address of _this request_, and then
        // compute the "real" IP address of the client.
        // For portal users, we rely on the portal telling us
        // using the PORTAL_REMOTE_ADDRESS request header.
        // If that header is missing, we "fall back" to the
        // request's IP address.
        String requestRemoteAddress = request.getRemoteAddr();
        String portalRemoteAddress =
                request.getHeader(PORTAL_REMOTE_ADDRESS);
        String remoteAddress;
        if (portalRemoteAddress != null) {
            remoteAddress = portalRemoteAddress;
        } else {
            remoteAddress = requestRemoteAddress;
        }

        // Start with SUCCESS_FIELD ...
        LogstashMarker lm = append(SUCCESS_FIELD, success);

        // ... and now decorate with additional fields.
        updateMarkerWithBasicFields(lm, request, uriInfo);
        updateMarkerWithUserFields(lm, request, profile, remoteAddress);
        updateMarkerWithPortalFields(lm, request, requestRemoteAddress,
                portalRemoteAddress);
        // For GeoIP information, we look up the portal address, if
        // one was provided.
        addGeoIPInfo(lm, remoteAddress);
        return lm;
    }

    /** Update a LogstashMarker with basic data from the request.
     * The identification data includes a randomly-generated UUID to
     * use for this log entry.
     * The method is extracted from the request,
     * and the path from the uriInfo.
     * @param lm The LogstashMarker to be updated with basic data.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     */
    private static void updateMarkerWithBasicFields(
            final LogstashMarker lm,
            final HttpServletRequest request,
            final UriInfo uriInfo) {
        lm.and(append(UUID_FIELD, UUID.randomUUID())).
                and(append(METHOD_FIELD, request.getMethod())).
                and(append(PATH_FIELD, uriInfo.getPath()));
    }

    /** Update a LogstashMarker with user identification data.
     * The IP address and user agent are extracted from the request.
     * If there is a user profile, the username is extracted.
     * @param lm The LogstashMarker to be updated with user identification data.
     * @param request The HTTP request.
     * @param profile The caller's security profile, or null, if there is none.
     * @param remoteAddress The value to be logged as the user's IP address.
     */
    private static void updateMarkerWithUserFields(
            final LogstashMarker lm,
            final HttpServletRequest request,
            final CommonProfile profile,
            final String remoteAddress) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put(IP_FIELD, remoteAddress);
        // Allow a portal-user-agent header to override user-agent.
        String userAgent = request.getHeader(PORTAL_USER_AGENT);
        if (userAgent == null) {
            userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        }
        if (userAgent != null) {
            // Log the user agent string, and whether it is a
            // known crawler/bot.
            userMap.put(USER_AGENT_FIELD, userAgent);
            userMap.put(IS_BOT_FIELD, BotDetector.isBot(userAgent));
        }
        if (profile != null) {
            userMap.put(USERNAME_FIELD, profile.getId());
        }
        lm.and(append(USER_MAP_FIELD, userMap));
    }

    /** Update a LogstashMarker with portal identification data.
     * The ID of the portal request, the portal's IP address, and
     * the portal's HTTP referrer data, are added.
     * @param lm The LogstashMarker to be updated with portal data.
     * @param request The HTTP request.
     * @param requestRemoteAddress The IP address of the Registry request.
     * @param portalRemoteAddress The IP address of the Portal, if this
     *      has been provided.
     */
    private static void updateMarkerWithPortalFields(
            final LogstashMarker lm,
            final HttpServletRequest request,
            final String requestRemoteAddress,
            final String portalRemoteAddress) {
        Map<String, Object> portalMap = new HashMap<>();
        // If there's a PORTAL-REMOTE-ADDRESS, store the portal's
        // own IP address.
        // Yes, the following looks wrong at first sight.
        // The point is: we test "portalRemoteAddress != null" to
        // see if this request came from the portal. If so,
        // the portal's IP address is _this request's_ IP address,
        // i.e., requestRemoteAddress. In this case, the value
        // of portalRemoteAddress will be stored in the log entry
        // by updateMarkerWithUserFields() as "user.ip".
        if (portalRemoteAddress != null) {
            portalMap.put(PORTAL_IP_FIELD, requestRemoteAddress);
        }
        // If there's a PORTAL-REMOTE-REFERRER, store it.
        String portalReferrer =
                request.getHeader(PORTAL_REFERRER);
        if (portalReferrer != null) {
            portalMap.put(PORTAL_REFERRER_FIELD, portalReferrer);
        }

        String portalId = request.getHeader(PORTAL_ID);
        if (portalId != null) {
            // This request came from the portal.
            portalMap.put(PORTAL_ID_FIELD, portalId);
        }
        lm.and(append(PORTAL_MAP_FIELD, portalMap));
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

        Map<String, Object> geoMap = new HashMap<>();
        geoMap.put(GEO_COUNTRY_NAME_FIELD, country.getName());
        geoMap.put(GEO_CITY_NAME_FIELD, city.getName());
        geoMap.put(GEO_REGION_CODE_FIELD, subdivision.getIsoCode());
        geoMap.put(GEO_LOCATION_FIELD,
                new Double[]
                        {location.getLongitude(), location.getLatitude()});
//        geoMap.put(, );
        lm.and(append(GEOIP_MAP_FIELD, geoMap));
    }

    /** Update a LogstashMarker with additional fields. Depending on
     * the message parameter, certain fields are grouped into subfields.
     * @param lm An existing LogstashMarker which is to have additional
     *      fields added.
     * @param message The message to be logged. This method uses it to
     *      do message-specific handling of the additional fields.
     * @param otherFields Other fields to be logged. This is a list
     *      of pairs of keys and values, of which the keys must be Strings.
     */
    public static void updateMarkerWithAdditionalFields(
            final LogstashMarker lm,
            final String message, final Object[] otherFields) {
        int otherFieldsLength = otherFields.length;
        if (otherFieldsLength % 2 != 0) {
            logger.error("Logging otherFields was not a list "
                    + "of pairs of keys/values");
            return;
        }


        boolean useVocabularyMap = false;
        Map<String, Object> vocabularyMap = new HashMap<>();

        boolean useSearchMap = false;
        Map<String, Object> searchMap = new HashMap<>();


        switch (message) {
        case Analytics.EVENT_GET_VOCABULARY:
        case Analytics.EVENT_CREATE_VOCABULARY:
        case Analytics.EVENT_UPDATE_VOCABULARY:
            useVocabularyMap = true;
            break;
        case Analytics.EVENT_SEARCH:
            useSearchMap = true;
            break;
        default:
            break;
        }

        for (int i = 0; i < otherFieldsLength; i = i + 2) {
            if (!(otherFields[i] instanceof String)) {
                logger.error("Logging otherFields has a key "
                        + "that is not a String: " + otherFields[i]);
            } else {
                String key = (String) otherFields[i];
                switch (key) {
                case VOCABULARY_ID_FIELD:
                case TITLE_FIELD:
                case SLUG_FIELD:
                case ENTITY_STATUS_FIELD:
                case OWNER_FIELD:
                case VOCABULARY_LOOKUP_FIELD:
                    if (useVocabularyMap) {
                        vocabularyMap.put(key, otherFields[i + 1]);
                        break;
                    }
                    // if !useVocabularyMap, fall through to default.
                case SEARCH_ACCESS_FIELD:
                case SEARCH_FORMAT_FIELD:
                case SEARCH_LANGUAGE_FIELD:
                case SEARCH_LICENCE_FIELD:
                case SEARCH_P_FIELD:
                case SEARCH_PP_FIELD:
                case SEARCH_PUBLISHER_FIELD:
                case SEARCH_Q_FIELD:
                case SEARCH_SUBJECT_LABELS_FIELD:
                case SEARCH_WIDGETABLE_FIELD:
                    if (useSearchMap) {
                        searchMap.put(key, otherFields[i + 1]);
                        break;
                    }
                    // if !useSearchMap, fall through to default.
                default:
                    lm.and(append(key, otherFields[i + 1]));
                    break;
                }
            }
        }
        if (useVocabularyMap) {
            lm.and(append(VOCABULARY_MAP_FIELD, vocabularyMap));
        }
        if (useSearchMap) {
            lm.and(append(SEARCH_MAP_FIELD, searchMap));
        }

    }

}
