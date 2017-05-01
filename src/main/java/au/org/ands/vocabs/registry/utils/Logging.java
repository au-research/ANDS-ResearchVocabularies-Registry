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
import org.slf4j.Marker;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;
import com.maxmind.geoip2.record.Subdivision;

import net.logstash.logback.marker.LogstashMarker;

/** Logging system for logging system actions, e.g., user requests.
 * Implemented using Logback with a Logstash back end.
 */
public final class Logging {

    /** Private constructor for a utility class. */
    private Logging() {
    }

    /** The internal (normal) logger for this class. */
    private static Logger internalLogger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Name of the special Logstash logger. */
    private static final String LOGGER_NAME = "au.org.ands.vocabs.logstash";

    /** The special Logstash logger used for recording system events. */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            LOGGER_NAME);

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
    /** The name of the user agent field inserted into log entries. */
    private static final String USER_AGENT_FIELD = "user_agent";
    /** The name of the username field inserted into log entries. */
    private static final String USERNAME_FIELD = "username";
    /** The name of the uuid field inserted into log entries. */
    private static final String UUID_FIELD = "uuid";

    /** The GeoIP2 database reader used for geo lookups of IP addresses. */
    private static DatabaseReader geoDbReader;

    static {
        String geoIPFile = RegistryProperties.getProperty(
                PropertyConstants.REGISTRY_LOGGING_GEOIPDB);
        if (geoIPFile == null) {
            internalLogger.info("No GeoIP database specified.");
        } else {
            File database = new File(geoIPFile);
            try {
                geoDbReader = new DatabaseReader.Builder(database).build();
            } catch (IOException e) {
                internalLogger.error("Unable to open GeoIP database", e);
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
            internalLogger.error("Unable to parse IP address", e);
            return;
        }

        CityResponse response;
        try {
            response = geoDbReader.city(ipAddress);
        } catch (IOException | GeoIp2Exception e) {
            internalLogger.error("Unable to look up IP address in "
                    + "GeoIP database", e);
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
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile, or null, if there is none.
     * @return The new LogstashMarker, for use in a log entry.
     */
    public static LogstashMarker createBasicMarker(
            final HttpServletRequest request,
            final UriInfo uriInfo,
            final CommonProfile profile) {
        LogstashMarker lm = append(UUID_FIELD, UUID.randomUUID()).
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
        return lm;
    }

    /** Get the internalLogger used for logging system actions. In general,
     * don't use this method; rather, use the various log() and
     * error() methods.
     * @return The internalLogger.
     */
    public static Logger getInstance() {
        return LOGGER;
    }

    /** Log an event.
     * @param arg0 The message to be logged.
     * @see org.slf4j.Logger#info(java.lang.String)
     */
    public static void log(final String arg0) {
        LOGGER.info(arg0);
    }

    /** Log an event.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile, or null, if there is none.
     * @param arg0 The message to be logged.
     * @see org.slf4j.Logger#info(java.lang.String)
     */
    public static void logRequest(final HttpServletRequest request,
            final UriInfo uriInfo,
            final CommonProfile profile,
            final String arg0) {
        LogstashMarker lm = createBasicMarker(request, uriInfo, profile);
        LOGGER.info(lm, arg0);
    }

    /** Log an event.
     * @param arg0 The message to be logged.
     * @param arg1 The parameter of the message.
     * @see org.slf4j.Logger#info(java.lang.String, java.lang.Object)
     */
    public static void log(final String arg0, final Object arg1) {
        LOGGER.info(arg0, arg1);
    }

    /** Log an event.
     * @param arg0 The message to be logged.
     * @param arg1 The parameters of the message.
     * @see org.slf4j.Logger#info(java.lang.String, java.lang.Object[])
     */
    public static void log(final String arg0, final Object... arg1) {
        LOGGER.info(arg0, arg1);
    }

    /** Log an event.
     * @param arg0 The message to be logged.
     * @param arg1 The exception (throwable) to be logged.
     * @see org.slf4j.Logger#info(java.lang.String, java.lang.Throwable)
     */
    public static void log(final String arg0, final Throwable arg1) {
        LOGGER.info(arg0, arg1);
    }

    /** Log an event.
     * @param arg0 The marker data to take into consideration.
     * @param arg1 The message to be logged.
     * @see org.slf4j.Logger#info(org.slf4j.Marker, java.lang.String)
     */
    public static void log(final Marker arg0, final String arg1) {
        LOGGER.info(arg0, arg1);
    }

    /** Log an event.
     * @param arg0 The message to be logged.
     * @param arg1 The first parameter of the message.
     * @param arg2 The second parameter of the message.
     * @see org.slf4j.Logger#info(java.lang.String, java.lang.Object,
     *      java.lang.Object)
     */
    public static void log(final String arg0, final Object arg1,
                           final Object arg2) {
        LOGGER.info(arg0, arg1, arg2);
    }

    /** Log an event.
     * @param arg0 The marker data to take into consideration.
     * @param arg1 The message to be logged.
     * @param arg2 A list of 3 or more parameters.
     * @see org.slf4j.Logger#info(org.slf4j.Marker, java.lang.String,
     *      java.lang.Object)
     */
    public static void log(final Marker arg0, final String arg1,
                    final Object arg2) {
        LOGGER.info(arg0, arg1, arg2);
    }

    /** Log an event.
     * @param arg0 The marker data to take into consideration.
     * @param arg1 The message to be logged.
     * @param arg2 A list of 3 or more parameters.
     * @see org.slf4j.Logger#info(org.slf4j.Marker, java.lang.String,
     *      java.lang.Object[])
     */
    public static void log(final Marker arg0, final String arg1,
                    final Object... arg2) {
        LOGGER.info(arg0, arg1, arg2);
    }

    /** Log an event.
     * @param arg0 The marker data to take into consideration.
     * @param arg1 The message to be logged.
     * @param arg2 The exception (throwable) to be logged.
     * @see org.slf4j.Logger#info(org.slf4j.Marker, java.lang.String,
     *      java.lang.Throwable)
     */
    public static void log(final Marker arg0, final String arg1,
                    final Throwable arg2) {
        LOGGER.info(arg0, arg1, arg2);
    }

    /** Log an event.
     * @param arg0 The marker data to take into consideration.
     * @param arg1 The message to be logged.
     * @param arg2 The first parameter of the message.
     * @param arg3 The second parameter of the message.
     * @see org.slf4j.Logger#info(org.slf4j.Marker, java.lang.String,
     *      java.lang.Object, java.lang.Object)
     */
    public static void log(final Marker arg0, final String arg1,
                    final Object arg2, final Object arg3) {
        LOGGER.info(arg0, arg1, arg2, arg3);
    }

    /** Log an error.
     * @param arg0 The error message to be logged.
     * @see org.slf4j.Logger#error(java.lang.String)
     */
    public static void error(final String arg0) {
        LOGGER.error(arg0);
    }

    /** Log an error.
     * @param arg0 The error message to be logged.
     * @param arg1 The parameter of the message.
     * @see org.slf4j.Logger#error(java.lang.String, java.lang.Object)
     */
    public static void error(final String arg0, final Object arg1) {
        LOGGER.error(arg0, arg1);
    }

    /** Log an error.
     * @param arg0 The error message to be logged.
     * @param arg1 A list of 3 or more parameters.
     * @see org.slf4j.Logger#error(java.lang.String, java.lang.Object[])
     */
    public static void error(final String arg0, final Object... arg1) {
        LOGGER.error(arg0, arg1);
    }

    /** Log an error.
     * @param arg0 The error message to be logged.
     * @param arg1 The exception (throwable) to be logged.
     * @see org.slf4j.Logger#error(java.lang.String, java.lang.Throwable)
     */
    public static void error(final String arg0, final Throwable arg1) {
        LOGGER.error(arg0, arg1);
    }

    /** Log an error.
     * @param arg0 The marker data to take into consideration.
     * @param arg1 The error message to be logged.
     * @see org.slf4j.Logger#error(org.slf4j.Marker, java.lang.String)
     */
    public static void error(final Marker arg0, final String arg1) {
        LOGGER.error(arg0, arg1);
    }

    /** Log an error.
     * @param arg0 The error message to be logged.
     * @param arg1 The first parameter of the message.
     * @param arg2 The second parameter of the message.
     * @see org.slf4j.Logger#error(java.lang.String, java.lang.Object,
     *      java.lang.Object)
     */
    public static void error(final String arg0, final Object arg1,
                             final Object arg2) {
        LOGGER.error(arg0, arg1, arg2);
    }

    /** Log an error.
     * @param arg0 The marker data to take into consideration.
     * @param arg1 The error message to be logged.
     * @param arg2 The parameter of the message.
     * @see org.slf4j.Logger#error(org.slf4j.Marker, java.lang.String,
     *      java.lang.Object)
     */
    public static void error(final Marker arg0, final String arg1,
                             final Object arg2) {
        LOGGER.error(arg0, arg1, arg2);
    }

    /** Log an error.
     * @param arg0 The marker data to take into consideration.
     * @param arg1 The error message to be logged.
     * @param arg2 A list of 3 or more parameters.
     * @see org.slf4j.Logger#error(org.slf4j.Marker, java.lang.String,
     *      java.lang.Object[])
     */
    public static void error(final Marker arg0, final String arg1,
                      final Object... arg2) {
        LOGGER.error(arg0, arg1, arg2);
    }

    /** Log an error.
     * @param arg0 The marker data to take into consideration.
     * @param arg1 The error message to be logged.
     * @param arg2 The exception (throwable) to be logged.
     * @see org.slf4j.Logger#error(org.slf4j.Marker, java.lang.String,
     *      java.lang.Throwable)
     */
    public static void error(final Marker arg0, final String arg1,
                      final Throwable arg2) {
        LOGGER.error(arg0, arg1, arg2);
    }

    /** Log an error.
     * @param arg0 The marker data to take into consideration.
     * @param arg1 The error message to be logged.
     * @param arg2 The first parameter of the message.
     * @param arg3 The second parameter of the message.
     * @see org.slf4j.Logger#error(org.slf4j.Marker, java.lang.String,
     *      java.lang.Object, java.lang.Object)
     */
    public static void error(final Marker arg0, final String arg1,
                      final Object arg2, final Object arg3) {
        LOGGER.error(arg0, arg1, arg2, arg3);
    }


}
