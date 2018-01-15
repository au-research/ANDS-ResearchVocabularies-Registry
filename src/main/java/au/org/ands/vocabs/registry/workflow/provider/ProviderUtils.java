/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.provider;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.enums.SubtaskProviderType;

/** Utilities for harvester providers. */
public final class ProviderUtils {

    /** Logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Private constructor for a utility class. */
    private ProviderUtils() {
    }

    /** Get the name of a provider. This name is the value that should
     * be provided as the second argument of to {@link
     *  #getProvider(au.org.ands.vocabs.registry.enums.SubtaskProviderType,
     *  String)}.
     * @param classObject The class of the provider.
     * @return The name of the provider. It is the "simple name" of the class,
     *      with any final "Provider" removed, and with the subtask
     *      provider type also removed. For example,
     *      the JsonListTransformProvider class becomes "JsonList".
     */
    public static String providerName(
            final Class<? extends WorkflowProvider> classObject) {
        String packageName = classObject.getPackage().getName();
        packageName = StringUtils.capitalize(
                packageName.substring(packageName.lastIndexOf('.') + 1));
        return classObject.getSimpleName().replaceAll("Provider$", "").
                replaceAll(packageName + "$", "");
    }

    /** Get the class instance of the provider based on the provider type and
     * name.
     * @param providerType The provider type.
     * @param providerName The name of the provider.
     * @return The class instance of the provider, or null if there is an
     *      error fetching it.
     */
    public static Class<? extends WorkflowProvider> getProviderClass(
            final SubtaskProviderType providerType,
            final String providerName) {
        String s = "au.org.ands.vocabs.registry.workflow.provider."
                + providerType.value() + "."
                + providerName + StringUtils.capitalize(providerType.value())
                + "Provider";
        Class<? extends WorkflowProvider> c;
        try {
            c = Class.forName(s).asSubclass(WorkflowProvider.class);
            return c;
        } catch (ClassNotFoundException e) {
            LOGGER.error("ProviderUtils.getProvider(): "
                    + "no such provider class: " + s);
            return null;
        }
    }

    /** Get an instance of the provider based on the provider type and
     * name.
     * @param providerType The provider type.
     * @param providerName The name of the provider.
     * @return An instance of the provider, or null if there is an error during
     *      instantiation.
     */
    public static WorkflowProvider getProvider(
            final SubtaskProviderType providerType,
            final String providerName) {
        String s = "au.org.ands.vocabs.registry.workflow.provider."
                + providerType.value() + "."
                + providerName + StringUtils.capitalize(providerType.value())
                + "Provider";
        Class<? extends WorkflowProvider> c;
        try {
            c = Class.forName(s).asSubclass(WorkflowProvider.class);
        } catch (ClassNotFoundException e) {
            LOGGER.error("ProviderUtils.getProvider(): "
                    + "no such provider class: " + s);
            return null;
        }
        WorkflowProvider workflowProvider = null;
        try {
            workflowProvider = c.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            LOGGER.error("ProviderUtils.getProvider(): "
                    + "can't instantiate provider class for provider class: "
                    + s, e);
            return null;
        }
        if (!(workflowProvider instanceof WorkflowProvider)) {
            LOGGER.error("ProviderUtils.getProvider() bad class:"
                    + workflowProvider.getClass().getName()
                    + ". Class not of type WorkflowProvider");
            return null;
        }
        return workflowProvider;
    }

}
