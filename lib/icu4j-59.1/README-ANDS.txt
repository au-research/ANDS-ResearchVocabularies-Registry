Solr comes with an instance of the ICU4J library, but it is an older
version.

In order for the test in the TestLanguageConversion class to pass, a
later version of ICU4J is required. So the ICU4J JAR has been removed
from the lib/solr-x.x directory, and a new version of the JAR has been
included here.

If the Solr library gets updated to a later version that includes a
recent version of ICU4J, then we can "revert" to using that.
