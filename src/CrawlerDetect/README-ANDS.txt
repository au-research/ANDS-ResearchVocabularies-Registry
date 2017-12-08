The orig directory contains a selection of files from CrawlerDetect:

https://github.com/JayBizzle/Crawler-Detect

The generate-matcher.sh/generate-matcher.awk files process
the PHP files in the orig directory, and generate the source of a Java
class that can be used to determine if an HTTP user agent is a web
crawler/bot.

The implementation (i.e., the way that crawlers and exclusions are
used) matches the original PHP implementation.

There is also a test class, TestBotDetector, which contains unit tests
that use the original test data (crawlers.txt and devices.txt).

If the upstream CrawlerDetect package is updated, replace the files in
the orig directory with the updated versions, and run
generate-matcher.sh to generate an updated BotDetector.java file, then
move this into src/main/java/au/org/ands/vocabs/registry/utils.
