/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.auth;

import java.lang.invoke.MethodHandles;

import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.HttpAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Tests of the RdaCookieAuthenticator class.
 */

public class TestRdaCookieAuthenticator {

    /** Logger for this class. */
    private static Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    @Test
    /** Test that percent-encoded UTF-8 content in cookie data is OK.
     * @throws HttpAction if there is an exception from Pac4J.
     */
    public void testUtf8()
          throws HttpAction {
/*
        String tokenEncoded = "%c3%a4";
        String token = "";

        try {
            token = URLDecoder.decode(tokenEncoded,
                    StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            logger.error("Doesn't know about UTF-8. Can that happen?", e);
            // throw new CredentialsException("Unable to decode cookie");
        }

        Assert.assertEquals(token, "ä");

        byte[] tokenBytes = null;
        try {
            tokenBytes = new PercentCodec().
                    decode(tokenEncoded.getBytes(StandardCharsets.UTF_8));
        } catch (DecoderException e) {
            e.printStackTrace();
            throw e;
        }

        Assert.assertEquals(tokenBytes, new byte[]{(byte)0xc3, (byte)0xa4});
*/
        RdaCookieAuthenticator rca = new RdaCookieAuthenticator();

        logger.info("Trying to validate tokenWithoutUmlaut");
        String tokenWithoutUmlaut =
                "a%3A11%3A%7Bs%3A10%3A%22session_id%22%3Bs%3A32%3A%22"
                + "9344bd7f5a2bccefcd67f0cb0265797c%22%3Bs%3A10%3A%22"
                + "ip_address%22%3Bs%3A14%3A%22130.56.111.109%22%3Bs%3A"
                + "10%3A%22user_agent%22%3Bs%3A84%3A%22Mozilla%2F5.0%20%28"
                + "Macintosh%3B%20Intel%20Mac%20OS%20X%2010.15%3B%20rv%3A104."
                + "0%29%20Gecko%2F20100101%20Firefox%2F104.0%22%3Bs%3A13%3A"
                + "%22last_activity%22%3Bi%3A1663556619%3Bs%3A9%3A%22"
                + "user_data%22%3Bs%3A0%3A%22%22%3Bs%3A21%3A%22"
                + "registry_affiliations%22%3Ba%3A1%3A%7Bi%3A0%3Bs%3A4%3A%22"
                + "ANDS%22%3B%7Ds%3A18%3A%22registry_functions%22%3Ba%3A5%3A"
                + "%7Bi%3A0%3Bs%3A18%3A%22AUTHENTICATED_USER%22%3Bi%3A1%3B"
                + "s%3A17%3A%22ORCA_SOURCE_ADMIN%22%3Bi%3A2%3Bs%3A13%3A%22"
                + "REGISTRY_USER%22%3Bi%3A3%3Bs%3A6%3A%22PUBLIC%22%3Bi%3A4"
                + "%3Bs%3A25%3A%22VOCABS_REGISTRY_SUPERUSER%22%3B%7Ds%3A22"
                + "%3A%22UNIQUE_USER_IDENTIFIER%22%3Bs%3A9%3A%22rwalker"
                + "%3A%3A%22%3Bs%3A18%3A%22USER_FRIENDLY_NAME%22%3Bs%3A14"
                + "%3A%22Richard%20Walker%22%3Bs%3A11%3A%22AUTH_METHOD%22%3B"
                + "s%3A23%3A%22AUTHENTICATION_BUILT_IN%22%3Bs%3A11%3A%22"
                + "AUTH_DOMAIN%22%3Bs%3A24%3A%22researchdata.ands.org.au"
                + "%22%3B%7D7414644c977cc7e6f37281a026d434fb8a653fd5";
        TokenCredentials tc = new TokenCredentials(tokenWithoutUmlaut,
                "test client");

        try {
            rca.validate(tc, null);
            Assert.fail("Validate should not have succeeded");
        } catch (CredentialsException e) {
            Assert.assertEquals(e.getMessage(),
                    "Credentials expired");
        }

        logger.info("Trying to validate tokenWithUmlaut");
        String tokenWithUmlaut =
                "a%3A11%3A%7Bs%3A10%3A%22session_id%22%3Bs%3A32%3A%22"
                + "3b03ffad34d466a0738bcb8c7c5cf546%22%3Bs%3A10%3A%22"
                + "ip_address%22%3Bs%3A14%3A%22130.56.111.109%22%3Bs%3A"
                + "10%3A%22user_agent%22%3Bs%3A84%3A%22Mozilla%2F5.0%20%28"
                + "Macintosh%3B%20Intel%20Mac%20OS%20X%2010.15%3B%20rv%3A104."
                + "0%29%20Gecko%2F20100101%20Firefox%2F104.0%22%3Bs%3A13%3A"
                + "%22last_activity%22%3Bi%3A1663556524%3Bs%3A9%3A%22"
                + "user_data%22%3Bs%3A0%3A%22%22%3Bs%3A21%3A%22"
                + "registry_affiliations%22%3Ba%3A1%3A%7Bi%3A0%3Bs%3A4%3A%22"
                + "ANDS%22%3B%7Ds%3A18%3A%22registry_functions%22%3Ba%3A5%3A"
                + "%7Bi%3A0%3Bs%3A18%3A%22AUTHENTICATED_USER%22%3Bi%3A1%3B"
                + "s%3A17%3A%22ORCA_SOURCE_ADMIN%22%3Bi%3A2%3Bs%3A13%3A%22"
                + "REGISTRY_USER%22%3Bi%3A3%3Bs%3A6%3A%22PUBLIC%22%3Bi%3A"
                + "4%3Bs%3A25%3A%22VOCABS_REGISTRY_SUPERUSER%22%3B%7Ds%3A22"
                + "%3A%22UNIQUE_USER_IDENTIFIER%22%3Bs%3A9%3A%22rwalker"
                + "%3A%3A%22%3Bs%3A18%3A%22USER_FRIENDLY_NAME%22%3Bs%3A15"
                + "%3A%22Richard%20W%C3%A4lker%22%3Bs%3A11%3A%22AUTH_METHOD"
                + "%22%3Bs%3A23%3A%22AUTHENTICATION_BUILT_IN%22%3Bs%3A11%3A%22"
                + "AUTH_DOMAIN%22%3Bs%3A24%3A%22researchdata.ands.org.au"
                + "%22%3B%7D647b9dd8f47ef70adb53add515e737b8f2971b1b";
        tc = new TokenCredentials(tokenWithUmlaut, "test client");
        try {
            rca.validate(tc, null);
            Assert.fail("Validate should not have succeeded");
        } catch (CredentialsException e) {
            Assert.assertEquals(e.getMessage(),
                    "Credentials expired");
        }

        logger.info("Trying to validate tokenThatFails");
        String tokenThatFails =
                "a%3A11%3A%7Bs%3A10%3A%22session_id%22%3Bs%3A32%3A%22"
                + "2ad2f2deec4c69dd391a138dcfa30570%22%3Bs%3A10%3A%22"
                + "ip_address%22%3Bs%3A14%3A%22130.56.111.109%22%3Bs%3A"
                + "10%3A%22user_agent%22%3Bs%3A84%3A%22Mozilla%2F5.0+%28"
                + "Macintosh%3B+Intel+Mac+OS+X+10.15%3B+rv%3A104.0%29+Gecko"
                + "%2F20100101+Firefox%2F104.0%22%3Bs%3A13%3A"
                + "%22last_activity%22%3Bi%3A1663305908%3Bs%3A9%3A%22"
                + "user_data%22%3Bs%3A0%3A%22%22%3Bs%3A21%3A%22"
                + "registry_affiliations%22%3Ba%3A3%3A%7Bi%3A0%3Bs%3A9%3A%22"
                + "EarthChem%22%3Bi%3A1%3Bs%3A6%3A%22GEOROC%22%3Bi%3A2%3B"
                + "s%3A3%3A%22AGN%22%3B%7Ds%3A18%3A%22registry_functions%22"
                + "%3Ba%3A2%3A%7Bi%3A0%3Bs%3A18%3A%22AUTHENTICATED_USER%22"
                + "%3Bi%3A1%3Bs%3A6%3A%22PUBLIC%22%3B%7Ds%3A22%3A%22"
                + "UNIQUE_USER_IDENTIFIER%22%3Bs%3A18%3A%22marthe.kloecking"
                + "%3A%3A%22%3Bs%3A18%3A%22USER_FRIENDLY_NAME%22%3Bs%3A16%"
                + "3A%22Marthe+Kl%C3%B6cking%22%3Bs%3A11%3A%22AUTH_METHOD"
                + "%22%3Bs%3A23%3A%22AUTHENTICATION_BUILT_IN%22%3Bs%3A11%3A"
                + "%22AUTH_DOMAIN%22%3Bs%3A24%3A%22researchdata.ands.org.au"
                + "%22%3B%7D3a379e9d50ca4c44ae4b0872dd2e1f1587bac8e0";
        tokenThatFails =
                "a%3A11%3A%7Bs%3A10%3A%22session_id%22%3Bs%3A32%3A%22"
                + "214b5d0f46f137a571e94d9822643c8c%22%3Bs%3A10%3A%22"
                + "ip_address%22%3Bs%3A14%3A%22130.56.111.109%22%3Bs%3A"
                + "10%3A%22user_agent%22%3Bs%3A84%3A%22Mozilla%2F5.0%20%28"
                + "Macintosh%3B%20Intel%20Mac%20OS%20X%2010.15%3B%20rv%3A104."
                + "0%29%20Gecko%2F20100101%20Firefox%2F104.0%22%3Bs%3A13%3A"
                + "%22last_activity%22%3Bi%3A1663561956%3Bs%3A9%3A%22"
                + "user_data%22%3Bs%3A0%3A%22%22%3Bs%3A21%3A%22"
                + "registry_affiliations%22%3Ba%3A2%3A%7Bi%3A0%3Bs%3A4%3A%22"
                + "ANDS%22%3Bi%3A1%3Bs%3A4%3A%22ANDS%22%3B%7Ds%3A18%3A%22"
                + "registry_functions%22%3Ba%3A5%3A%7Bi%3A0%3Bs%3A18%3A%22"
                + "AUTHENTICATED_USER%22%3Bi%3A1%3Bs%3A17%3A%22"
                + "ORCA_SOURCE_ADMIN%22%3Bi%3A2%3Bs%3A13%3A%22REGISTRY_USER"
                + "%22%3Bi%3A3%3Bs%3A6%3A%22PUBLIC%22%3Bi%3A4%3Bs%3A25%3A%22"
                + "VOCABS_REGISTRY_SUPERUSER%22%3B%7Ds%3A22%3A%22"
                + "UNIQUE_USER_IDENTIFIER%22%3Bs%3A9%3A%22rwalker%3A%3A%22"
                + "%3Bs%3A18%3A%22USER_FRIENDLY_NAME%22%3Bs%3A15%3A%22"
                + "Richard%20W%C3%A4lker%22%3Bs%3A11%3A%22AUTH_METHOD%22"
                + "%3Bs%3A23%3A%22AUTHENTICATION_BUILT_IN%22%3Bs%3A11%3A%22"
                + "AUTH_DOMAIN%22%3Bs%3A24%3A%22researchdata.ands.org.au"
                + "%22%3B%7D748e73d09c9171cf2197059a1aabc88d5b1c7934";
               tc = new TokenCredentials(tokenThatFails, "test client");
        try {
            rca.validate(tc, null);
            Assert.fail("Validate should not have succeeded");
        } catch (CredentialsException e) {
            Assert.assertEquals(e.getMessage(),
                    "Credentials expired");
        }

    }
}
