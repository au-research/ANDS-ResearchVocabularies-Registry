/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

import au.org.ands.vocabs.toolkit.utils.ApplicationContextListener;

/** Code to be executed for test shutdown. */
@Test(groups = "shutdown")
public class ArquillianTestShutdown extends ArquillianBaseTest {

    /** Logger. */
    private static Logger logger;

    static {
        logger = LoggerFactory.getLogger(
                MethodHandles.lookup().lookupClass());
    }

    /** Shut down the suite.
     * Note: Arquillian invokes this method first on the server side, and
     * then on the client side after all tests are completed.
     */
    @AfterSuite(groups = "shutdown")
    public final void shutdownSuite() {
        if (ApplicationContextListener.getServletContext() == null) {
            logger.info("In ArquillianTestSetup.shutdownSuite() "
                    + "on client side");
        } else {
            logger.info("In ArquillianTestSetup.shutdownSuite() "
                    + "on server side");
            ArquillianTestUtils.closeConnectionsForDbUnit();
            PoolPartyMockServer.shutdown();
        }
    }

    /** This is a null test, the presence of which seems to be necessary
     * to have the BeforeSuite/AfterSuite-annotated methods recognized and
     * run.
     */
    @Test
    public void dummyTestForShutdown() {
    }

}
