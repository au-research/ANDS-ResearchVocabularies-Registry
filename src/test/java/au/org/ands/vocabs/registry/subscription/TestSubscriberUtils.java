/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.subscription;

import java.lang.invoke.MethodHandles;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Tests of the SubscriberUtils class. */
public class TestSubscriberUtils {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** A Pattern that describes a valid subscriber token. */
    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("\\d+_[A-Za-z0-9]+");

    /** Run tests of the {@link SubscriberUtils#createToken(Integer)}
     * method. */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void testCreateToken1() {

        String result;

        result = SubscriberUtils.createToken(1);
        logger.info("testCreateToken1 generated token 1: " + result);

        Assert.assertTrue(result.startsWith("1_"),
                "Token doesn't begin with subscriber Id");
        Assert.assertEquals(result.length(), SubscriberUtils.TOKEN_LENGTH + 2,
                "Token is the wrong length");
        Assert.assertTrue(TOKEN_PATTERN.matcher(result).matches(),
                "Token is not in the expected format");

        result = SubscriberUtils.createToken(23);
        logger.info("testCreateToken1 generated token 2: " + result);

        Assert.assertTrue(result.startsWith("23_"),
                "Token doesn't begin with subscriber Id");
        Assert.assertEquals(result.length(), SubscriberUtils.TOKEN_LENGTH + 3,
                "Token is the wrong length");
        Assert.assertTrue(TOKEN_PATTERN.matcher(result).matches(),
                "Token is not in the expected format");
    }

}
