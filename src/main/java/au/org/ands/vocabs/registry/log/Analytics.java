/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.log;

import static net.logstash.logback.marker.Markers.append;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlEnum;

import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Continent;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;
import com.maxmind.geoip2.record.Postal;
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
    /** The name of the field that groups together related entity data
     * inserted into log entries. */
    private static final String RE_MAP_FIELD = "related_entity";
    /** The name of the field that groups together search filters
     * inserted into log entries. */
    private static final String SEARCH_FILTERS_MAP_FIELD = "filters";
    /** The name of the field that groups together search results
     * inserted into log entries. */
    private static final String SEARCH_RESULTS_MAP_FIELD = "results";
    /** The name of the field that groups together subscriber data
     * inserted into log entries. */
    private static final String SUBSCRIBER_MAP_FIELD = "subscriber";
    /** The name of the field that groups together subscription data
     * inserted into log entries. */
    private static final String SUBSCRIPTION_MAP_FIELD = "subscription";
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
    /** The name of the continent code field inserted into log entries. */
    private static final String GEO_CONTINENT_CODE_FIELD = "continent_code";
    /** The name of the country code field inserted into log entries. */
    private static final String GEO_COUNTRY_CODE2_FIELD = "country_code2";
    /** The name of the country name field inserted into log entries. */
    private static final String GEO_COUNTRY_NAME_FIELD = "country_name";
    /** The name of the location field inserted into log entries. */
    private static final String GEO_LOCATION_FIELD = "location";
    /** The name of the postal code field inserted into log entries. */
    private static final String GEO_POSTAL_CODE_FIELD = "postal_code";
    /** The name of the region code field inserted into log entries. */
    private static final String GEO_REGION_CODE_FIELD = "region_code";
    /** The name of the region name field inserted into log entries. */
    private static final String GEO_REGION_NAME_FIELD = "region_name";
    /** The name of the timezone field inserted into log entries. */
    private static final String GEO_TIMEZONE_FIELD = "timezone";
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
     * map, the use of the value "status" is unambiguous.
     * Note: the values provided for log entries should be the
     * "values" of the enumerated types, i.e., callers should
     * use something like {@code ....getStatus().value()}.
     * */
    public static final String ENTITY_STATUS_FIELD = "status";
    /** The name of the failure reason field inserted into log entries. */
    public static final String FAILURE_REASON = "failure_reason";
    /** The name of the vocabulary or related entity ID field
     * inserted into log entries. */
    public static final String ID_FIELD = "id";
    /** The name of the notification element id field
     * inserted into log entries. */
    public static final String NOTIFICATION_ELEMENT_ID_FIELD = "element_id";
    /** The name of the notification element owner field
     * inserted into log entries. */
    public static final String NOTIFICATION_ELEMENT_OWNER_FIELD =
            "element_owner";
    /** The name of the notification element type field
     * inserted into log entries. */
    public static final String NOTIFICATION_ELEMENT_TYPE_FIELD = "element_type";
    /** The value to use for the {@link #NOTIFICATION_ELEMENT_TYPE_FIELD}
     * field to indicate that this is for all subscripton types. */
    public static final String NOTIFICATION_ELEMENT_TYPE_ALL = "all";
    /** The value to use for the {@link #NOTIFICATION_ELEMENT_TYPE_FIELD}
     * field to indicate that this is a subscription for an owner (or
     * for all owners). */
    public static final String NOTIFICATION_ELEMENT_TYPE_OWNER = "owner";
    /** The value to use for the {@link #NOTIFICATION_ELEMENT_TYPE_FIELD}
     * field to indicate that this is a subscription for the system. */
    public static final String NOTIFICATION_ELEMENT_TYPE_SYSTEM = "system";
    /** The value to use for the {@link #NOTIFICATION_ELEMENT_TYPE_FIELD}
     * field to indicate that this is a subscription for a vocabulary. */
    public static final String NOTIFICATION_ELEMENT_TYPE_VOCABULARY =
            "vocabulary";
    /** The name of the owner field inserted into log entries. */
    public static final String OWNER_FIELD = "owner";
    /** The name of the search access field inserted into log entries. */
    public static final String SEARCH_ACCESS_FIELD = "access";
    /** The name of the search collapse/expand field inserted into
     * log entries. */
    public static final String SEARCH_COLLAPSE_EXPAND_FIELD = "collapse_expand";
    /** The name of the search count_only field inserted into
     * log entries. */
    public static final String SEARCH_COUNT_ONLY_FIELD = "count_only";
    /** The name of the search result expanded Id field inserted into
     * log entries. */
    public static final String SEARCH_EXPANDED_RESULT_ID_FIELD =
            "expanded_result_ids";
    /** The name of the search result expanded owner field inserted into
     * log entries. */
    public static final String SEARCH_EXPANDED_RESULT_OWNER_FIELD =
            "expanded_result_owners";
    /** The name of the search format field inserted into log entries. */
    public static final String SEARCH_FORMAT_FIELD = "format";
    /** The name of the search language field inserted into log entries. */
    public static final String SEARCH_LANGUAGE_FIELD = "language";
    // Possible future work: support faceting by last_updated values.
//  /** The name of the search last updated field inserted into log entries. */
//  public static final String SEARCH_LAST_UPDATED_FIELD = "last_updated";
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
    /** The name of the search RDF type field inserted into log entries. */
    public static final String SEARCH_RDF_TYPE_FIELD = "rdf_type";
    /** The name of the search result Id field inserted into log entries. */
    public static final String SEARCH_RESULT_ID_FIELD = "result_ids";
    /** The name of the search result num_found field
     * inserted into log entries. */
    public static final String SEARCH_RESULT_NUM_FOUND_FIELD = "num_found";
    /** The name of the search result owner field inserted into log entries. */
    public static final String SEARCH_RESULT_OWNER_FIELD = "result_owners";
    /** The name of the search sort field inserted into log entries. */
    public static final String SEARCH_SORT_ORDER_FIELD = "sort";
    /** The name of the search subject labels field inserted into
     * log entries. */
    public static final String SEARCH_SUBJECT_LABELS_FIELD = "subject";
    /** The name of the search version status field inserted into
     * log entries. */
    public static final String SEARCH_VERSION_STATUS_FIELD = "version_status";
    /** The name of the search widgetable field inserted into log entries. */
    public static final String SEARCH_WIDGETABLE_FIELD = "widgetable";
    /** The name of the slug field inserted into log entries. */
    public static final String SLUG_FIELD = "slug";
    /** The name of the success field inserted into log entries. */
    public static final String SUCCESS_FIELD = "success";
    /** The name of the subscriber email field inserted into log entries. */
    public static final String SUBSCRIBER_EMAIL_FIELD = "subscriber_email";
    /** The name of the subscriber id field inserted into log entries. */
    public static final String SUBSCRIBER_ID_FIELD = "subscriber_id";
    /** The name of the title field inserted into log entries. */
    public static final String TITLE_FIELD = "title";
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
     * for getting a list of vocabularies. */
    public static final String EVENT_GET_VOCABULARY_LIST =
            "read_vocabulary_list";
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
     * for searching the vocabulary metadata. */
    public static final String EVENT_SEARCH = "search";
    /** The value of the "message" field to use for log entries
     * for searching the vocabulary resources. */
    public static final String EVENT_SEARCH_RESOURCES = "search_resources";

    /** The value of the "message" field to use for log entries
     * for creating a subscription. */
    public static final String EVENT_CREATE_SUBSCRIPTION =
            "create_subscription";
    /** The value of the "message" field to use for log entries
     * for getting subscriptions. */
    public static final String EVENT_GET_SUBSCRIPTION =
            "read_subscription";
    /** The value of the "message" field to use for log entries
     * for deleting a subscription. */
    public static final String EVENT_DELETE_SUBSCRIPTION =
            "delete_subscription";

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

    /** Close the GeoIP2 database reader.
     * Invoke this method during webapp shutdown.
     * After invoking this method,
     * you must no longer invoke any of the methods of this class. */
    public static void shutdown() {
        if (geoDbReader != null) {
            try {
                geoDbReader.close();
            } catch (IOException e) {
                logger.error("Unable to close GeoIP database", e);
            }
            // Set to null anyway, "just in case" there's another
            // invocation of a method of this class.
            geoDbReader = null;
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

        if (ipAddress.isLoopbackAddress()) {
            // We won't get any information for a loopback address.
            // Substitute the publicly-visible IP address instead.
            try {
                ipAddress = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                logger.error("Unable to get publicly-visible IP "
                        + "for localhost", e);
                // Don't return, but keep going with what we already have.
            }
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

        Postal postal = response.getPostal();
//        postal.getCode() = '55455'

        Location location = response.getLocation();
//        location.getLatitude()   = 44.9733
//        location.getLongitude()) = -93.2323

        Continent continent = response.getContinent();

        Map<String, Object> geoMap = new HashMap<>();
        String value;
        value = country.getName();
        if (value != null) {
            geoMap.put(GEO_COUNTRY_NAME_FIELD, value);
        }
        value = city.getName();
        if (value != null) {
            geoMap.put(GEO_CITY_NAME_FIELD, value);
        }
        value = subdivision.getIsoCode();
        if (value != null) {
            geoMap.put(GEO_REGION_CODE_FIELD, value);
        }
        if (location.getLongitude() != null) {
            geoMap.put(GEO_LOCATION_FIELD,
                    new Double[]
                            {location.getLongitude(), location.getLatitude()});
        }
        value = continent.getCode();
        if (value != null) {
            geoMap.put(GEO_CONTINENT_CODE_FIELD, value);
        }
        value = country.getIsoCode();
        if (value != null) {
            geoMap.put(GEO_COUNTRY_CODE2_FIELD, value);
        }
        value = postal.getCode();
        if (value != null) {
            geoMap.put(GEO_POSTAL_CODE_FIELD, value);
        }
        value = subdivision.getName();
        if (value != null) {
            geoMap.put(GEO_REGION_NAME_FIELD, value);
        }
        value = location.getTimeZone();
        if (value != null) {
            geoMap.put(GEO_TIMEZONE_FIELD, value);
        }
//        value = ;
//        if (value != null) {
//            geoMap.put(, value);
//        }
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
    @SuppressWarnings("checkstyle:MethodLength")
    public static void updateMarkerWithAdditionalFields(
            final LogstashMarker lm,
            final String message, final Object[] otherFields) {
        int otherFieldsLength = otherFields.length;
        if (otherFieldsLength % 2 != 0) {
            logger.error("Logging otherFields was not a list "
                    + "of pairs of keys/values");
            return;
        }

        // Maps for storing nested fields, and flags to indicate
        // whether or not they are to be used.

        boolean useVocabularyMap = false;
        Map<String, Object> vocabularyMap = new HashMap<>();

        boolean useREMap = false;
        Map<String, Object> reMap = new HashMap<>();

        boolean useSearchMaps = false;
        Map<String, Object> searchFiltersMap = new HashMap<>();
        Map<String, Object> searchResultsMap = new HashMap<>();

        boolean useSubscriberMap = false;
        Map<String, Object> subscriberMap = new HashMap<>();

        boolean useSubscriptionMap = false;
        Map<String, Object> subscriptionMap = new HashMap<>();

        switch (message) {
        case EVENT_GET_VOCABULARY:
        case EVENT_CREATE_VOCABULARY:
        case EVENT_UPDATE_VOCABULARY:
            useVocabularyMap = true;
            break;
        case EVENT_CREATE_RELATED_ENTITY:
        case EVENT_UPDATE_RELATED_ENTITY:
        case EVENT_DELETE_RELATED_ENTITY:
            useREMap = true;
            break;
        case EVENT_SEARCH:
        case EVENT_SEARCH_RESOURCES:
            useSearchMaps = true;
            break;
        case EVENT_GET_SUBSCRIPTION:
            useSubscriberMap = true;
            break;
        case EVENT_CREATE_SUBSCRIPTION:
        case EVENT_DELETE_SUBSCRIPTION:
            useSubscriptionMap = true;
            useSubscriberMap = true;
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
                // moved will be set to true, if the key/value is stored
                // away (i.e., "moved") into one of the maps.
                boolean moved = false;
                switch (key) {
                case ID_FIELD:
                case TITLE_FIELD:
                case OWNER_FIELD:
                    if (useVocabularyMap) {
                        vocabularyMap.put(key, otherFields[i + 1]);
                        moved = true;
                    } else if (useREMap) {
                        reMap.put(key, otherFields[i + 1]);
                        moved = true;
                    }
                    break;
                case ENTITY_STATUS_FIELD:
                    // For this field, we accept instances of our
                    // jaxc-generated enumerated types. We "rewrite"
                    // to get the result of applying its value() method ...
                    otherFields[i + 1] = getEntityStatusValue(
                            otherFields[i + 1]);
                    // ... then fall through to see if it should be moved.
                case SLUG_FIELD:
                case VOCABULARY_LOOKUP_FIELD:
                    if (useVocabularyMap) {
                        vocabularyMap.put(key, otherFields[i + 1]);
                        moved = true;
                    }
                    break;
                case WAS_MODIFIED_FIELD:
                    if (useREMap) {
                        reMap.put(key, otherFields[i + 1]);
                        moved = true;
                    }
                    break;
                case SEARCH_ACCESS_FIELD:
                case SEARCH_COLLAPSE_EXPAND_FIELD:
                case SEARCH_COUNT_ONLY_FIELD:
                case SEARCH_FORMAT_FIELD:
                case SEARCH_LANGUAGE_FIELD:
                case SEARCH_LICENCE_FIELD:
                case SEARCH_P_FIELD:
                case SEARCH_PP_FIELD:
                case SEARCH_PUBLISHER_FIELD:
                case SEARCH_Q_FIELD:
                case SEARCH_RDF_TYPE_FIELD:
                case SEARCH_SORT_ORDER_FIELD:
                case SEARCH_SUBJECT_LABELS_FIELD:
                case SEARCH_VERSION_STATUS_FIELD:
                case SEARCH_WIDGETABLE_FIELD:
                    if (useSearchMaps) {
                        searchFiltersMap.put(key, otherFields[i + 1]);
                        moved = true;
                    }
                    break;
                case SEARCH_EXPANDED_RESULT_ID_FIELD:
                case SEARCH_EXPANDED_RESULT_OWNER_FIELD:
                case SEARCH_RESULT_ID_FIELD:
                case SEARCH_RESULT_NUM_FOUND_FIELD:
                case SEARCH_RESULT_OWNER_FIELD:
                    if (useSearchMaps) {
                        searchResultsMap.put(key, otherFields[i + 1]);
                        moved = true;
                    }
                    break;
                case SUBSCRIBER_ID_FIELD:
                case SUBSCRIBER_EMAIL_FIELD:
                    if (useSubscriberMap) {
                        subscriberMap.put(key, otherFields[i + 1]);
                        moved = true;
                    }
                    break;
                case NOTIFICATION_ELEMENT_TYPE_FIELD:
                case NOTIFICATION_ELEMENT_ID_FIELD:
                case NOTIFICATION_ELEMENT_OWNER_FIELD:
                    if (useSubscriptionMap) {
                        subscriptionMap.put(key, otherFields[i + 1]);
                        moved = true;
                    }
                    break;
                default:
                    // The key will be added at the top level, below.
                    break;
                }
                if (!moved) {
                    lm.and(append(key, otherFields[i + 1]));
                }
            }
        }
        if (useVocabularyMap) {
            lm.and(append(VOCABULARY_MAP_FIELD, vocabularyMap));
        }
        if (useREMap) {
            lm.and(append(RE_MAP_FIELD, reMap));
        }
        if (useSearchMaps) {
            lm.and(append(SEARCH_FILTERS_MAP_FIELD, searchFiltersMap));
            // Do emptiness check, as it _will_ be empty for
            // Widget Explorer search.
            if (!searchResultsMap.isEmpty()) {
                lm.and(append(SEARCH_RESULTS_MAP_FIELD, searchResultsMap));
            }
        }
        if (useSubscriberMap) {
            lm.and(append(SUBSCRIBER_MAP_FIELD, subscriberMap));
        }
        if (useSubscriptionMap) {
            lm.and(append(SUBSCRIPTION_MAP_FIELD, subscriptionMap));
        }

    }

    /** For values that are to go into log entries, if they are
     * instances of jaxc-generated enumerated types, invoke the
     * {@code value()} method on them to get the value as given
     * in the Registry Schema, rather than the value that would
     * be given by {@code toString()}. The difference is usually
     * that the return value is in lower case.
     * @param o A value to be processed.
     * @return If o is a String, then o is returned as-is. If o is
     *      an instance of a jaxc-generated enumerated type, and the
     *      class has a {@code value()} method, then
     *      the value returned is the result of applying that method.
     *      Otherwise, the value returned is the result of applying
     *      the {@code toString()} method.
     */
    private static String getEntityStatusValue(final Object o) {
        if (o instanceof String) {
            return (String) o;
        }
        Class<?> clazz = o.getClass();
        if (clazz.isAnnotationPresent(XmlEnum.class)) {
            Method method;
            try {
                method = clazz.getMethod("value", (Class<?>[]) null);
            } catch (NoSuchMethodException | SecurityException e) {
                return o.toString();
            }
            try {
                return (String) method.invoke(o, (Object[]) null);
            } catch (IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                return o.toString();
            }
        }
        return o.toString();
    }

}
