The apache-tomcat-7.0.69 directory contains a unpacked copy of the
Apache Tomcat 7.0.69 distribution.

These are the modifications made to it, in order to make it
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
