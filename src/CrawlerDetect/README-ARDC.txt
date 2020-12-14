The orig directory contains a selection of files from the
CrawlerDetect project:

https://github.com/JayBizzle/Crawler-Detect

The generate-matcher.sh/generate-matcher.awk files process the two PHP
files in the orig directory, and generate the source of a Java class
that can be used to determine if an HTTP user agent is a web
crawler/bot.

The implementation (i.e., the way that crawlers and exclusions are
used) matches the original PHP implementation.

There is also a test class, TestBotDetector, which contains unit tests
that use the original test data (crawlers.txt and devices.txt).

If the upstream CrawlerDetect package is updated, replace the files in
the orig directory with the updated versions, and run ant to generate
an updated BotDetector.java file.

(You'll find BotDetector.java in the directory
src/main/java/au/org/ands/vocabs/registry/utils,
and TestBotDetector.java in the directory
src/test/java/au/org/ands/vocabs/registry/utils.)

Please then run the TestBotDetector test with the updated data, to
double-check the behaviour of the code.

Hint: the five files to be copied from the Crawler-Detect distribution
into the orig directory are (at the time of writing):

LICENSE
src/Fixtures/Crawlers.php
src/Fixtures/Exclusions.php
tests/crawlers.txt
tests/devices.txt
