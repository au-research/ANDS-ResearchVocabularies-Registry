The apache-tomcat-7.0.69 directory contains a unpacked copy of the
Apache Tomcat 7.0.69 distribution.

Then, within the webapps directory, a copy of OpenRDF Sesame 2.8.3 has
been inserted.

These are the modifications made to the Tomcat distribution, in order to make it
work with Arquillian testing:

diff -r orig/apache-tomcat-7.0.69/conf/server.xml tomcatmanaged/apache-tomcat-7.0.69/conf/server.xml
71c71
<     <Connector port="8080" protocol="HTTP/1.1"
---
>     <Connector port="8123" protocol="HTTP/1.1"
diff -r orig/apache-tomcat-7.0.69/conf/tomcat-users.xml tomcatmanaged/apache-tomcat-7.0.69/conf/tomcat-users.xml
40a41,44
>
>   <user username="arquillian" password="arquillian"
>         roles="manager-script"/>
>

And this diff is to avoid warnings when using WireMock:

diff orig/apache-tomcat-7.0.69/conf/catalina.properties tomcatmanaged/apache-tomcat-7.0.69/conf/catalina.properties
116c116
< xom-*.jar
---
> xom-*.jar,wiremock-*.jar
