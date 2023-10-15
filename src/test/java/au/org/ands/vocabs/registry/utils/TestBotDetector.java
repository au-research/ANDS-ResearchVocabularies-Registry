/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.toolkit.test.arquillian.ArquillianBaseTest;

/** Unit tests of the slug generator. */
// Some methods of this class may be (indeed, are) excluded selectively
// by the test suite; see the test suite XML file to see what's excluded.
// We use the @RunAsClient annotation, as the crawlers.txt and
// devices.txt aren't available on the server.
@Test
public class TestBotDetector extends ArquillianBaseTest {

    /** Test if the {@link BotDetector#isBot(String)} method
     * identifies known crawlers as such.
     * @throws IOException If an I/O error occurs.
     */
    @Test
    @RunAsClient
    public void testAreBots() throws IOException {
        File botFile = new File("src/CrawlerDetect/orig/crawlers.txt");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(botFile), StandardCharsets.UTF_8))) {
            String line = br.readLine();
            while (line != null) {
                Assert.assertTrue(BotDetector.isBot(line),
                        "False negative: " + line);
                line = br.readLine();
            }
        }
    }

    /** Test if the {@link BotDetector#isBot(String)} method
     * identifies things known not to be crawlers as such.
     * @throws IOException If an I/O error occurs.
     */
    // We exclude this in the full test run, as it's expensive to run,
    // at about 50 seconds!
    @Test
    @RunAsClient
    public void testAreNotBots() throws IOException {
        File devicesFile = new File("src/CrawlerDetect/orig/devices.txt");
        try (BufferedReader br =
                new BufferedReader(new InputStreamReader(
                        new FileInputStream(devicesFile),
                        StandardCharsets.UTF_8))) {
            String line = br.readLine();
            while (line != null) {
                Assert.assertFalse(BotDetector.isBot(line),
                        "False positive: " + line);
                line = br.readLine();
            }
        }
    }

}
