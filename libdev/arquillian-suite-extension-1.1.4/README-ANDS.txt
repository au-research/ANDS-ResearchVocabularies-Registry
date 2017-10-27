This version of the Arquillian Suite Extension doesn't work "out of
the box" for this project.

There is a customized version of the ArquillianSuiteExtension class in
src/test/java/au/org/ands/vocabs/toolkit/test/arquillian/ArquillianSuiteExtension.java.

For the record, the original JAR file is contained here as
arquillian-suite-extension-1.1.4.jar-orig.

The JAR file actually used, arquillian-suite-extension-1.1.4-ANDS.jar,
has been created as follows:

unzip arquillian-suite-extension-1.1.4.jar-orig META-INF/services/org.jboss.arquillian.core.spi.LoadableExtension

The resulting file
META-INF/services/org.jboss.arquillian.core.spi.LoadableExtension
was then edited; its existing contents, the one line:

org.eu.ingwar.tools.arquillian.extension.suite.ArquillianSuiteExtension

was replaced with:

au.org.ands.vocabs.toolkit.test.arquillian.ArquillianSuiteExtension

Then the updated JAR was created as follows:

cp arquillian-suite-extension-1.1.4.jar-orig arquillian-suite-extension-1.1.4-ANDS.jar
zip arquillian-suite-extension-1.1.4-ANDS.jar META-INF/services/org.jboss.arquillian.core.spi.LoadableExtension
