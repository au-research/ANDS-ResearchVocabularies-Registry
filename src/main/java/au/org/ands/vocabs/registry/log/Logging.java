/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.log;

import java.lang.invoke.MethodHandles;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;

import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import net.logstash.logback.marker.LogstashMarker;

/** Logging system for logging system actions, e.g., user requests.
 * Implemented using Logback with a Logstash back end.
 */
public final class Logging {

    /** Private constructor for a utility class. */
    private Logging() {
    }

    /** The internal (normal) logger for this class. */
    @SuppressWarnings("unused")
    private static Logger internalLogger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Name of the special Logstash logger. */
    private static final String LOGGER_NAME = "au.org.ands.vocabs.logstash";

    /** The special Logstash logger used for recording system events. */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            LOGGER_NAME);

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
     * @param success Whether or not the operation was completed successfully.
     *      If false, the caller should add details
     *      of the failure as part of <code>otherParameters</code>.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile, or null, if there is none.
     * @param message The message to be logged.
     * @see org.slf4j.Logger#info(java.lang.String)
     */
    public static void logRequest(
            final boolean success,
            final HttpServletRequest request,
            final UriInfo uriInfo,
            final CommonProfile profile,
            final String message) {
        LogstashMarker lm = Analytics.createBasicMarker(
                success, request, uriInfo, profile);
        LOGGER.info(lm, message);
    }

    /** Log an event.
     * @param success Whether or not the operation was completed successfully.
     *      If false, the caller should add details
     *      of the failure as part of <code>otherParameters</code>.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile, or null, if there is none.
     * @param message The message to be logged.
     * @param otherParameters Other values to be logged. This is a list
     *      of pairs of keys and values, of which the keys must be Strings.
     * @see org.slf4j.Logger#info(java.lang.String)
     */
    public static void logRequest(
            final boolean success,
            final HttpServletRequest request,
            final UriInfo uriInfo,
            final CommonProfile profile,
            final String message,
            final Object... otherParameters) {
        LogstashMarker lm = Analytics.createBasicMarker(
                success, request, uriInfo, profile);
        Analytics.updateMarkerWithAdditionalFields(lm, message,
                otherParameters);
        LOGGER.info(lm, message);
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
