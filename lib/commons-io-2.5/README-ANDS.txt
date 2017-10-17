OpenRDF Sesame 2.8.3 comes with an instance of the commons-io library,
but it is an older version.

In order for the embedded Solr instance used in automated testing to
work, a later version of commons-io is required.

So the commons-io JAR has been removed from the lib/openrdf-2.8.3
directory, and a new version of the JAR has been included here.

If the OpenRDF-Sesame library gets updated to a later version (or,
more probably, to RDF4J) that includes a recent version of commons-io,
then we can "revert" to using that.
