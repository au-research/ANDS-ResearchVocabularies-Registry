/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.toolkit.rest;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.toolkit.db.TaskUtils;
import au.org.ands.vocabs.toolkit.db.model.Task;
import au.org.ands.vocabs.toolkit.provider.harvest.HarvestProvider;
import au.org.ands.vocabs.toolkit.provider.harvest.HarvestProviderUtils;
import au.org.ands.vocabs.toolkit.provider.importer.ImporterProvider;
import au.org.ands.vocabs.toolkit.provider.importer.ImporterProviderUtils;
import au.org.ands.vocabs.toolkit.utils.PropertyConstants;
import au.org.ands.vocabs.toolkit.utils.ToolkitProperties;

/** REST web services for getting info about Toolkit supported services. */
@Path("getInfo")
public class GetInfo {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Injected servlet context. */
    @Context
    private ServletContext context;

    /** Get the list of PoolParty projects.
     * @return The list of PoolParty projects, in JSON format,
     * as returned by PoolParty. */
    @Path("PoolPartyProjects")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public final String getInfoPoolParty() {
        logger.debug("called getInfoPoolParty");
        HarvestProvider provider =
                HarvestProviderUtils.getProvider("PoolParty");
        if (provider == null) {
            logger.error("getInfoPoolParty() unable to get "
                    + "PoolParty harvester provider");
            return "{\"exception\":\"Can't get PoolParty provider.\"}";
        }
        return provider.getInfo();
    }

    /** Get the list of Sesame repositories.
     * @return The list of repositories, in JSON format. */
    @Path("SesameRepositories")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public final Collection<?> getInfoSesame() {
        logger.debug("called getInfoSesame");
        ImporterProvider provider =
                ImporterProviderUtils.getProvider("Sesame");
        if (provider == null) {
            logger.error("getInfoSesame() unable to get "
                    + "Sesame importer provider");
            return new ArrayList<String>();
        }
        return provider.getInfo();
    }

    /** Get a complete list of tasks.
     * @return The list of tasks. */
    @Path("systemHealthCheck")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public final List<Task> systemHealthCheck() {
        logger.debug("called systemHealthCheck");
        List<Task> tasks = TaskUtils.getAllTasks();
        return tasks;
    }

    /** Get the Toolkit version information from the version.properties
     * configuration file.
     * @return The version information.
     */
    @Path("version")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public final HashMap<String, String> getVersion() {
        logger.debug("called getVersion");
        HashMap<String, String> result =
                new HashMap<String, String>();
        result.put("Toolkit.version",
                ToolkitProperties.getProperty(
                        PropertyConstants.TOOLKIT_VERSION));
        result.put("Toolkit.versionTimestamp",
                ToolkitProperties.getProperty(
                        PropertyConstants.TOOLKIT_VERSIONTIMESTAMP));
        result.put("Toolkit.buildDate",
                ToolkitProperties.getProperty(
                        PropertyConstants.TOOLKIT_BUILDDATE));
        return result;
    }


}
