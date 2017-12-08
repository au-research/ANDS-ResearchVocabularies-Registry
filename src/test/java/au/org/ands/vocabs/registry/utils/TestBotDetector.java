/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests of the slug generator. */
public class TestBotDetector {

    /** Test if the {@link BotDetector#isBot(String)} method
     * identifies known crawlers as such.
     * @throws IOException If an I/O error occurs.
     */
    @Test
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
    @Test
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
