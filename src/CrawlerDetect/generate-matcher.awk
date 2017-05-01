BEGIN {
print "/** See the file \"LICENSE\" for the full license governing this code. */"
print ""
print "package au.org.ands.vocabs.registry.utils;"
print ""
print "import java.util.regex.Pattern;"
print ""
print "/** Utility method for detection of bots, based on HTTP user agent."
print " * Used during logging, to output a field that indicates whether or not"
print " * the request came from a bot."
print " *"
print " * This class is auto-generated. Do not edit it!"
print " * See the directory src/CrawlerDetect for sources."
print " */"
print "public final class BotDetector {"
print ""
print "    /** Private constructor for a utility class. */"
print "    private BotDetector() {"
print "    }"
print ""
print "    /** A regular expression that matches known exclusions. */"
print "    @SuppressWarnings(\"checkstyle:LineLength\")"
print "    private static final String EXCLUSIONS_REGEX ="
print "            \"(\""
firstmatch=1
}

# There are two input files: exclusions and crawlers.
# This matches the switch-over between the two files.
FNR!=NR && FNR==1 {
print "            + \")\";"
print ""
print "    /** A regular expression that matches known crawlers. */"
print "    @SuppressWarnings(\"checkstyle:LineLength\")"
print "    private static final String CRAWLERS_REGEX ="
print "            \"(\""
firstmatch=1
}

/\s+'/ {
    str = $0
    # Trim beginning of line
    sub(/^\s+'/, "" , str)
    # Trim end of line; special treatment for last line of exclusions.
    sub(/'(,( \/\/.*)?)?$/, "" , str)
    gsub(/\\\//, "/" , str)
    gsub(/\\/, "\\\\" , str)
    # Special treatment for Zend pattern
    gsub(/\\\\\\\\/, "\\\\\\\\", str)
    if (firstmatch==1) {
        printf "            + \"%s\"\n", str
    } else {
        printf "            + \"|\" + \"%s\"\n", str
    }
    firstmatch=0
}

END {

print "            + \")\";"
print ""
print "    /** The compiled version of the regular expression that matches"
print "     * known exclusions. */"
print "    private static Pattern exclusionsPattern;"
print ""
print "    /** The compiled version of the regular expression that matches"
print "     * known crawlers. */"
print "    private static Pattern crawlersPattern;"
print ""
print "    static {"
print "        exclusionsPattern = Pattern.compile(EXCLUSIONS_REGEX,"
print "                Pattern.CASE_INSENSITIVE);"
print "        crawlersPattern = Pattern.compile(CRAWLERS_REGEX,"
print "                Pattern.CASE_INSENSITIVE);"
print "    }"
print ""
print "    /** Determine if an HTTP user agent represents a known bot."
print "     * @param userAgent The user agent contained in HTTP headers."
print "     * @return true, if the user agent is a known bot."
print "     */"
print "    public static boolean isBot(final String userAgent) {"
print "        String exclusionsRemoved = exclusionsPattern.matcher(userAgent)."
print "                replaceAll(\"\");"
print "        return crawlersPattern.matcher(exclusionsRemoved).find();"
print "    }"
print ""
print "}"

}
