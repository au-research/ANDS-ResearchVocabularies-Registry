ANDS note:

The following logging libraries required by this version of Sesame
have been removed, as newer versions of them were required to support
logstash-logback-encoder.

jcl-over-slf4j-1.7.10.jar
logback-classic-1.1.2.jar
logback-core-1.1.2.jar
slf4j-api-1.7.10.jar

The newer versions of these JARs can be found in the logback-* and
slf4j-* subdirectories of the lib directory.

The following libraries required by this version of Sesame have been
removed, as newer versions of them were required to support geoip2.

jackson-annotations-2.4.4.jar
jackson-core-2.4.4.jar
jackson-databind-2.4.4.jar

The newer versions of these JARs can be found in the jackson-x.y.z
subdirectory of the lib directory.

The following library required by this version of Sesame has been
removed, as a newer version was required to support the embedded Solr
used for automated testing.

commons-io-2.4.jar

The newer version of this JAR can be found in the commons-io-x.y
subdirectory of the lib directory.

The following libraries required by this version of Sesame have been
removed, as newer versions were required to support the embedded Solr
used for automated testing.

httpclient-4.4.jar
httpclient-cache-4.4.jar
httpcore-4.4.jar

They have been replaced with these libraries, in this directory:

httpclient-4.5.7.jar
httpclient-cache-4.5.7.jar
httpcore-4.4.11.jar

(Hmm, maybe move them into a different directory or directories, like
we did with the other updated libraries?)
