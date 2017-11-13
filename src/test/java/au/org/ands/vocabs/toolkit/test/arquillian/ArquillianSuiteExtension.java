// This is a very-lightly-modified version of source code that comes from the
// Arquillian Suite Extension project:
// https://github.com/ingwarsw/arquillian-suite-extension

// Please see libdev/arquillian-suite-extension-x.y/README-ANDS.txt
// for details.

// The one ANDS modification is noted where it occurs.
// (The other trivial changes (removal of tabs, Eclipse's automatic insertion
// of some "final" modifiers) are not indicated.)

// ANDS modifications are under the same Apache 2 license as the original
// code.

// Since this is not ANDS code, Checkstyle and deprecation warnings
// are turned off for this file.
// CHECKSTYLE:OFF: ConstantName
// CHECKSTYLE:OFF: JavadocStyle
// CHECKSTYLE:OFF: JavadocVariable
// CHECKSTYLE:OFF: LineLength
// CHECKSTYLE:OFF: LocalVariableName
// CHECKSTYLE:OFF: MagicNumber
package au.org.ands.vocabs.toolkit.test.arquillian;

/*
 * #%L
 * Arquillian suite extension
 * %%
 * Copyright (C) 2013 Ingwar & co.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eu.ingwar.tools.arquillian.extension.suite.ExtendedSuiteContext;
import org.eu.ingwar.tools.arquillian.extension.suite.ExtendedSuiteContextImpl;
import org.eu.ingwar.tools.arquillian.extension.suite.annotations.ArquilianSuiteDeployment;
import org.eu.ingwar.tools.arquillian.extension.suite.annotations.ArquillianSuiteDeployment;
import org.eu.ingwar.tools.arquillian.extension.suite.annotations.ExtendedSuiteScoped;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentScenario;
import org.jboss.arquillian.container.spi.event.DeployManagedDeployments;
import org.jboss.arquillian.container.spi.event.UnDeployManagedDeployments;
import org.jboss.arquillian.container.spi.event.container.BeforeStop;
import org.jboss.arquillian.container.test.impl.client.deployment.event.GenerateDeployment;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.EventContext;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.annotation.ClassScoped;
import org.jboss.arquillian.test.spi.context.ClassContext;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.reflections.Reflections;

/**
 * Arquillian Suite Extension main class.
 *
 * @author Karol Lassak 'Ingwar'
 */
@SuppressWarnings("deprecation")
public class ArquillianSuiteExtension implements LoadableExtension {

    private static final Logger log = Logger.getLogger(ArquillianSuiteExtension.class.getName());
    private static Class<?> deploymentClass;

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(final ExtensionBuilder builder) {
        deploymentClass = getDeploymentClass();
        if (deploymentClass != null) {
            builder.observer(SuiteDeployer.class).context(ExtendedSuiteContextImpl.class);
        } else {
            log.log(Level.WARNING, "arquillian-suite-deployment: Cannot find class annotated with @ArquillianSuiteDeployment, will try normal way..");
        }
    }

    /**
     * Finds class with should produce global deployment PER project.
     *
     * @return class marked with ArquillianSuiteDeployment annotation.
     */
    private static Class<?> getDeploymentClass() {
        // Had a bug that if you open inside eclipse more than one project with @ArquillianSuiteDeployment and is a dependency, the test doesn't run because found more than one @ArquillianSuiteDeployment.
        // Filter the deployment PER project.
        // ANDS modification: the following does not work for the
        // combination of Tomcat/TestNG used in this project. See
        // https://github.com/ingwarsw/arquillian-suite-extension/issues/43
        //        final Reflections reflections = new Reflections(ClasspathHelper.contextClassLoader().getResource(""));
        // Instead, the easiest way to make this work is to specify
        // a parent package name.
        final Reflections reflections = new Reflections("au.org.ands.vocabs");
        // Reflection all classes to search for Annotation @ArquillianSuiteDeployment.
        Set<Class<?>> results = reflections.getTypesAnnotatedWith(ArquillianSuiteDeployment.class, true);
        if (results.isEmpty()) {
            // Keeping compatibility backward.
            results = reflections.getTypesAnnotatedWith(ArquilianSuiteDeployment.class, true);
            if (results.isEmpty()) {
                return null;
            }
        }
        // Verify if has more than one @ArquillianSuiteDeployment.
        if (results.size() > 1) {
            for (final Class<?> type : results) {
                log.log(Level.SEVERE, "arquillian-suite-deployment: Duplicated class annotated with @ArquillianSuiteDeployment: {0}", type.getName());
            }
            throw new IllegalStateException("Duplicated classess annotated with @ArquillianSuiteDeployment");
        }
        // Return the single result.
        return results.iterator().next();
    }

    /**
     *
     */
    public static class SuiteDeployer {

        @Inject // Active some form of ClassContext around our deployments due to assumption bug in AS7 extension.
        private Instance<ClassContext> classContext;
        @Inject
        @ClassScoped
        private InstanceProducer<DeploymentScenario> classDeploymentScenario;
        @Inject
        private Event<UnDeployManagedDeployments> undeployEvent;
        @Inject
        private Event<GenerateDeployment> generateDeploymentEvent;
        @Inject
        private Instance<ExtendedSuiteContext> extendedSuiteContext;
        private DeploymentScenario suiteDeploymentScenario;
        @ExtendedSuiteScoped
        @Inject
        private InstanceProducer<DeploymentScenario> suiteDeploymentScenarioInstanceProducer;
        private boolean suiteDeploymentGenerated;
        private boolean deployDeployments;
        private boolean undeployDeployments;

        /**
         * Method ignoring DeployManagedDeployments events if already deployed.
         *
         * @param eventContext Event to check
         */
        public void blockDeployManagedDeploymentsWhenNeeded(@Observes final EventContext<DeployManagedDeployments> eventContext) {
            if (deployDeployments) {
                deployDeployments = false;
                debug("NOT Blocking DeployManagedDeployments event {0}", eventContext.getEvent().toString());
                eventContext.proceed();
            } else {
                // Do nothing with event.
                debug("Blocking DeployManagedDeployments event {0}", eventContext.getEvent().toString());
            }
        }

        /**
         * Method ignoring GenerateDeployment events if deployment is already done.
         *
         * @param eventContext Event to check
         */
        public void blockGenerateDeploymentWhenNeeded(@Observes final EventContext<GenerateDeployment> eventContext) {
            if (suiteDeploymentGenerated) {
                // Do nothing with event.
                debug("Blocking GenerateDeployment event {0}", eventContext.getEvent().toString());
            } else {
                suiteDeploymentGenerated = true;
                debug("NOT Blocking GenerateDeployment event {0}", eventContext.getEvent().toString());
                eventContext.proceed();
            }
        }

        /**
         * Method ignoring UnDeployManagedDeployments events at runtime.
         *
         * Only at undeploy container we will undeploy all.
         *
         * @param eventContext Event to check
         */
        public void blockUnDeployManagedDeploymentsWhenNeeded(@Observes final EventContext<UnDeployManagedDeployments> eventContext) {
            if (undeployDeployments) {
                undeployDeployments = false;
                debug("NOT Blocking UnDeployManagedDeployments event {0}", eventContext.getEvent().toString());
                eventContext.proceed();
            } else {
                // Do nothing with event.
                debug("Blocking UnDeployManagedDeployments event {0}", eventContext.getEvent().toString());
            }
        }

        /**
         * Startup event.
         *
         * @param event AfterStart event to catch
         */
        public void startup(@Observes(precedence = -100) final BeforeSuite event) {
            debug("Catching AfterStart event {0}", event.toString());
            executeInClassScope(new Callable<Void>() {
                @Override
                public Void call() {
                    generateDeploymentEvent.fire(new GenerateDeployment(new TestClass(deploymentClass)));
                    suiteDeploymentScenario = classDeploymentScenario.get();
                    return null;
                }
            });
            deployDeployments = true;
            extendedSuiteContext.get().activate();
            suiteDeploymentScenarioInstanceProducer.set(suiteDeploymentScenario);
        }

        /**
         * Undeploy event.
         *
         * @param event event to observe
         */
        public void undeploy(@Observes final BeforeStop event) {
            debug("Catching BeforeStop event {0}", event.toString());
            undeployDeployments = true;
            undeployEvent.fire(new UnDeployManagedDeployments());
        }

        /**
         * Calls operation in deployment class scope.
         *
         * @param call Callable to call
         */
        private void executeInClassScope(final Callable<Void> call) {
            try {
                classContext.get().activate(deploymentClass);
                call.call();
            } catch (Exception e) {
                throw new RuntimeException("Could not invoke operation", e); // NOPMD
            } finally {
                classContext.get().deactivate();
            }
        }

        /**
         * Prints debug message.
         *
         * If arquillian.debug flag is set.
         *
         * @param format format of message
         * @param message message objects to format
         */
        private void debug(final String format, final Object... message) {
            Boolean DEBUG = Boolean.valueOf(System.getProperty("arquillian.debug"));
            if (DEBUG) {
                log.log(Level.WARNING, format, message);
            }
        }
    }
}
