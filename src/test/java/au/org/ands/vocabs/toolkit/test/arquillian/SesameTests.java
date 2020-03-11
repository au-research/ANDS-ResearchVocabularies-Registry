/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.toolkit.test.arquillian;

import java.io.File;
import java.lang.invoke.MethodHandles;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import au.org.ands.vocabs.toolkit.test.utils.TestPropertyConstants;
import au.org.ands.vocabs.toolkit.utils.ToolkitProperties;

/** TestNG test suite specifically to initialize the Sesame instance
 * used for testing. The code that's here is here because it can't be
 * in {@link ArquillianTestSetup}. This code sets system properties that
 * must set <i>before</i> the managed Tomcat server is started;
 * the code in {@link ArquillianTestSetup} runs only after Tomcat
 * has been started.
 */
@Test(groups = "sesame")
public class SesameTests {

    /** Logger. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Initialize the Sesame instance we use for testing. In practice,
     * that means setting system properties for the location of
     * the repositories and log files. The test directory which contains
     * those subdirectories is deleted here. */
    @BeforeSuite
    public final void setupSesame() {
        logger.info("In setupSesame in SesameTests");
        final String tomcatDirectory = ToolkitProperties.getProperty(
                TestPropertyConstants.TEST_TOMCAT_DIRECTORY);
        final File catalinaHome = new File(tomcatDirectory);
        // Always start the this Tomcat with a scratch work area.
        FileUtils.deleteQuietly(catalinaHome);
        // For now, no need to (re)create that directory. In future,
        // if it is required, use
        // FileUtils.forceMkdir(new File(catalinaHome));
        // or, just to make a more specific subdirectory:
        // FileUtils.forceMkdir(new File(catalinaHome, "webapps"));

        // Force Sesame to store repositories here, rather
        // than elsewhere (e.g., in ~/.aduna, etc.).
        String sesameBaseDir = new File(tomcatDirectory,
                "openrdf").getAbsolutePath();
        String sesameLoggingDir = new File(tomcatDirectory,
                "adunalogging").getAbsolutePath();
        System.setProperty("SESAME_BASEDIR", sesameBaseDir);
        System.setProperty("SESAME_LOGGING_DIR", sesameLoggingDir);
    }

}
