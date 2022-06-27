The JRE 8 standalone JAR of this release of WireMock contains a file
`module-info.class`, compiled for Java 9.

That's normally not a problem at all when running with Java 8. But you
_will_ see an error message if, e.g., you include it in a deployment
to Tomcat as part of some testing you're doing.

You'll see something like this:

```
INFO: Starting Servlet Engine: Apache Tomcat/7.0.61
Jun 03, 2022 3:54:24 PM org.apache.catalina.startup.ContextConfig processAnnotationsJar
SEVERE: Unable to process Jar entry [module-info.class] from Jar [jar:file:/Users/rwalker/Documents/workspace/.metadata/.plugins/org.eclipse.wst.server.core/tmp0/wtpwebapps/vocabs-registry/WEB-INF/lib/wiremock-jre8-standalone-2.26.3.jar!/] for annotations
org.apache.tomcat.util.bcel.classfile.ClassFormatException: Invalid byte tag in constant pool: 19
	at org.apache.tomcat.util.bcel.classfile.Constant.readConstant(Constant.java:97)
	at org.apache.tomcat.util.bcel.classfile.ConstantPool.<init>(ConstantPool.java:55)
	at org.apache.tomcat.util.bcel.classfile.ClassParser.readConstantPool(ClassParser.java:177)
	at org.apache.tomcat.util.bcel.classfile.ClassParser.parse(ClassParser.java:85)
	at org.apache.catalina.startup.ContextConfig.processAnnotationsStream(ContextConfig.java:2089)
	at org.apache.catalina.startup.ContextConfig.processAnnotationsJar(ContextConfig.java:1965)
	at org.apache.catalina.startup.ContextConfig.processAnnotationsUrl(ContextConfig.java:1931)
	at org.apache.catalina.startup.ContextConfig.processAnnotations(ContextConfig.java:1916)
	at org.apache.catalina.startup.ContextConfig.webConfig(ContextConfig.java:1330)
	at org.apache.catalina.startup.ContextConfig.configureStart(ContextConfig.java:889)
	at org.apache.catalina.startup.ContextConfig.lifecycleEvent(ContextConfig.java:386)
	at org.apache.catalina.util.LifecycleSupport.fireLifecycleEvent(LifecycleSupport.java:117)
	at org.apache.catalina.util.LifecycleBase.fireLifecycleEvent(LifecycleBase.java:90)
	at org.apache.catalina.core.StandardContext.startInternal(StandardContext.java:5416)
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:150)
	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1575)
	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1565)
	at java.util.concurrent.FutureTask.run(FutureTask.java:266)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at java.lang.Thread.run(Thread.java:750)

```

It seems there's nothing to worry about, and nothing to be done.

Even recent releases of the JRE 8 standalone JAR, e.g.,
`wiremock-jre8-standalone-2.33.2.jar`, contain a file
`META-INF/versions/9/module-info.class` compiled with Java 9.

Note that we use WireMock _only_ for testing: it is _not_ included in
the generated WAR file.
