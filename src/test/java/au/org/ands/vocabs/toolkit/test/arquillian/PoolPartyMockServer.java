/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/** Setup and shutdown of the embedded mock server for PoolParty. */
public final class PoolPartyMockServer {

    /** Timeout (in ms) after which to force Jetty's Threads to stop.
     * There doesn't yet seem any need for it to be any longer than 1 ms.
     * If such a setting is not enforced, or the
     * setting is more than a second or so, then at
     * shutdown you'll see SEVERE messages from Tomcat about
     * the remaining Threads and ThreadLocals. */
    private static final long JETTY_STOP_TIMEOUT = 1L;

    /** Path to the stub definitions that will be used by WireMock. */
    public static final String WIREMOCK_ROOT_FILES_PATH =
            ArquillianTestUtils.getClassesPath()
            + "/wiremock";

    /** Private constructor for a utility class. */
    private PoolPartyMockServer() {
    }

    /** Logger. */
    private static Logger logger;

    static {
        logger = LoggerFactory.getLogger(
                MethodHandles.lookup().lookupClass());
    }

    /** The WireMockServer used to mock PoolParty. */
    private static WireMockServer wireMockServer;

    /** The port number on which the mock server is running. */
    private static int port;

//    /** */
//    private static WireMock wireMock;

    /** Get the port number on which the mock server is running.
     * @return the port number.
     */
    public static int getPort() {
        return port;
    }

    /** Setup the embedded mock server for PoolParty. */
    public static void setup() {
        logger.info("In PoolPartyMockServer.setup()");
//      logger.info("WIREMOCK_ROOT_FILES_PATH = " + WIREMOCK_ROOT_FILES_PATH);
        wireMockServer = new WireMockServer(
                WireMockConfiguration.wireMockConfig().
                dynamicPort().
                jettyStopTimeout(JETTY_STOP_TIMEOUT).
                withRootDirectory(WIREMOCK_ROOT_FILES_PATH));
        wireMockServer.start();
        port = wireMockServer.port();
        logger.info("Started PoolParty mock server on port " + port);
        WireMock.configureFor(port);
        // Possible future work: configure multiple instances.
//        wireMock = new WireMock(port);
        // Uncomment as needed during preparation of mocks.
//        configureMocks();
    }

    /** Configure the stubs that are to be mocked, and save them
     * in the WireMock mapping directory.. */
    @SuppressWarnings("unused")
    private static void configureMocks() {
        /* Example:
        // List of all PoolParty projects.
        WireMock.stubFor(get(urlEqualTo(
                "/PoolParty/" + PoolPartyUtils.API_PROJECTS)).
                withHeader(HttpHeaders.ACCEPT,
                        equalTo(MediaType.APPLICATION_JSON)).
                withBasicAuth("ppuser", "pppass").
                willReturn(okJson(
                        "[{\"a\": \"Some content\"}]")));
        */
        WireMock.saveAllMappings();
    }

    /** Shut down the embedded mock server for PoolParty. */
    public static void shutdown() {
        logger.info("In PoolPartyMockServer.shutdown()");
        if (wireMockServer != null) {
            wireMockServer.stop();

            // The stop is mostly effective.
            // However, you will have to expect to see error messages
            // of the following sort:
            // [testng] SEVERE: The web application [/test] created a
            // ThreadLocal with key of type
            // [com.github.tomakehurst.wiremock.client.WireMock$1]
            // (value
            // [com.github.tomakehurst.wiremock.client.WireMock$1@2e6a1a22])
            // and a value of type
            // [com.github.tomakehurst.wiremock.client.WireMock] (value
            // [com.github.tomakehurst.wiremock.client.WireMock@2194b13b])
            // but failed to remove it when the web application was stopped.
            // Threads are going to be renewed over time to try and avoid
            // a probable memory leak.

            // This _doesn't_ work reliably to prevent those messages.
            // The sleep period seems to need to be about 30000 ms
            // to prevent all but one of them.
//            try {
//                Thread.sleep(10000);
//            } catch (InterruptedException e) {
//                logger.error("Interrupted sleep", e);
//            }
        }
    }

}
