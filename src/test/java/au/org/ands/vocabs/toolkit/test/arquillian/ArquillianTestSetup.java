/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.utils.RegistryConfig;
import au.org.ands.vocabs.toolkit.utils.ApplicationContextListener;

/** Code to be executed for test setup. See also {@link SesameTests}, which
 * has test setup code that must be run before Tomcat starts. */
@Test(groups = "arquillian")
public class ArquillianTestSetup extends ArquillianBaseTest {

    /** Logger. */
    private static Logger logger;

    static {
        logger = LoggerFactory.getLogger(
                MethodHandles.lookup().lookupClass());
    }

    /** Have we run setupSuite on server side at least once? */
    private static boolean setupSuiteRunServerSide;

    /** Set up the suite. This means:
     * clear out the contents of the repository (deleting
     * the directory pointed to by property {@code Registry.storagePath})
     * and install a copy of the LSR.
     * Note: Arquillian invokes this method first on the client side, and
     * then on the server side after deployment.
     * @throws IOException If unable to remove the repository directory
     *      {@code Registry.storagePath}.
     */
    @BeforeSuite(groups = "arquillian")
    public final void setupSuite() throws IOException {
        if (ApplicationContextListener.getServletContext() == null) {
            logger.info("In ArquillianTestSetup.setupSuite() on client side");
        } else {
            // Arquillian runs BeforeSuite/AfterSuite for each test,
            // which I think is not the normal TestNG behaviour!
            // So we use static field setupSuiteRunServerSide to
            // keep track of this code being run.
            if (!setupSuiteRunServerSide) {
                logger.info("In ArquillianTestSetup.setupSuite() "
                        + "on server side for first time");
                setupSuiteRunServerSide = true;
                // logger.info("pwd: " +  Paths.get("").toAbsolutePath());
                logger.info("ROOT_FILES_PATH = " + new File(
                        RegistryConfig.ROOT_FILES_PATH).getAbsolutePath());
                FileUtils.deleteDirectory(new File(
                        RegistryConfig.ROOT_FILES_PATH));
                FileUtils.copyFile(
                        new File(MethodHandles.lookup().lookupClass().
                                getClassLoader().getResource(
                                    "language-subtag-registry").getPath()),
                        new File(RegistryConfig.LSR_FILE_PATH));
                PoolPartyMockServer.setup();
            } else {
                logger.info("In ArquillianTestSetup.setupSuite() "
                        + "on server side not for first time");
            }
        }
    }

    /** This is a null test, the presence of which seems to be necessary
     * to have the BeforeSuite/AfterSuite-annotated methods recognized and
     * run. The negative priority also seems to be required, as
     * despite preserve-order being set true, this method can otherwise
     * be run other than first.
     */
    @Test(priority = -1)
    public void dummyTestForSetup() {
    }

}
