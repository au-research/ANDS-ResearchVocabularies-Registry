/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Tests of the PutUpload class. */
public class TestPutUpload {

    /** Tests of the {@link PutUpload#sanitizeFilename(String)} method. */
    @Test
    public void testSanitizeFilename() {
        Assert.assertEquals(PutUpload.sanitizeFilename("abc.123.xml"),
                "abc_123.xml");
        Assert.assertEquals(PutUpload.sanitizeFilename("abc..2.123.xml"),
                "abc_2_123.xml");
        Assert.assertEquals(PutUpload.sanitizeFilename("abc 2.xml"),
                "abc_2.xml");
        Assert.assertEquals(PutUpload.sanitizeFilename("abc\t2.xml"),
                "abc_2.xml");
        Assert.assertEquals(PutUpload.sanitizeFilename("abc\n2.xml"),
                "abc_2.xml");
        Assert.assertEquals(PutUpload.sanitizeFilename("abc \t\f2.xml"),
                "abc_2.xml");
        Assert.assertEquals(PutUpload.sanitizeFilename("/etc/passwd"),
                "passwd");
        Assert.assertEquals(PutUpload.sanitizeFilename("C:\\Document\\abc.ttl"),
                "abc.ttl");
        Assert.assertEquals(PutUpload.sanitizeFilename("abc#2.xml"),
                "abc_2.xml");
        // This one isn't so pretty. But we don't expect to see it
        // in real life.
        Assert.assertEquals(PutUpload.sanitizeFilename(
                "http://xyz/abc%20def.ttl#abc"), "abc_20def.ttl_abc");
    }

    /** Tests of the {@link PutUpload#FILENAME_PATTERN} regular expression. */
    @Test
    public void testFilenamePattern() {
        Assert.assertFalse(PutUpload.FILENAME_PATTERN.
                matcher("").matches());
        Assert.assertFalse(PutUpload.FILENAME_PATTERN.
                matcher(".abc").matches());
        Assert.assertFalse(PutUpload.FILENAME_PATTERN.
                matcher("abc.").matches());
        Assert.assertFalse(PutUpload.FILENAME_PATTERN.
                matcher(".").matches());
        Assert.assertFalse(PutUpload.FILENAME_PATTERN.
                matcher("abc.def.ttl").matches());
        Assert.assertTrue(PutUpload.FILENAME_PATTERN.
                matcher("abc123_4.ttl").matches());
    }

}
