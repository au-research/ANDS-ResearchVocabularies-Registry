/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.admin;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.http.HttpStatus;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.jax.rs.annotations.Pac4JProfile;
import org.pac4j.jax.rs.annotations.Pac4JSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import au.org.ands.vocabs.registry.api.auth.AuthUtils;
import au.org.ands.vocabs.registry.api.context.AdminApiPaths;
import au.org.ands.vocabs.registry.api.context.ResponseUtils;
import au.org.ands.vocabs.registry.api.context.SwaggerInterface;
import au.org.ands.vocabs.registry.api.user.ErrorResult;
import au.org.ands.vocabs.registry.api.user.ErrorResultUtils;
import au.org.ands.vocabs.registry.db.context.DBContext;
import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.dao.TaskDAO;
import au.org.ands.vocabs.registry.db.dao.VersionDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.log.Logging;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;
import au.org.ands.vocabs.registry.workflow.tasks.Task;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.ResponseHeader;

/** Workflow administration tools available through a REST-like
 * interface. */
@Path(AdminApiPaths.API_ADMIN + "/" + AdminApiPaths.WORKFLOW)
@Api(value = SwaggerInterface.TAG_ADMIN,
        authorizations = {@Authorization(value = SwaggerInterface.BASIC_AUTH),
        @Authorization(value = SwaggerInterface.API_KEY_AUTH)})
public class AdminRestMethods {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Get a task, by Id.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile.
     * @param taskId The task Id.
     * @return The task.
     */
    @Path(AdminApiPaths.TASKS + "/" + AdminApiPaths.TASK_ID)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Pac4JSecurity
    @GET
    @ApiOperation(value = "Get a task.",
            notes = "This method is only available to administrator users.",
            response = Task.class)
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_BAD_REQUEST,
                    message = "Invalid input",
                    response = ErrorResult.class),
            @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED,
                    message = "Not authenticated",
                    response = ErrorResult.class,
                    responseHeaders = {
                            @ResponseHeader(name = "WWW-Authenticate",
                                    response = String.class)
                            }),
            @ApiResponse(code = HttpStatus.SC_FORBIDDEN,
                    message = "Not authenticated, or not authorized",
                    response = ErrorResult.class)
            })
    public Response getTask(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(hidden = true) @Pac4JProfile
            final CommonProfile profile,
            @ApiParam(value = "The ID of the task to get.")
            @PathParam("taskId") final Integer taskId) {
        logger.info("Called getTask, id: " + taskId);
        if (!AuthUtils.profileIsSuperuser(profile)) {
            return ResponseUtils.generateForbiddenResponseNotSuperuser();
        }

        au.org.ands.vocabs.registry.db.entity.Task dbTask =
                TaskDAO.getTaskById(taskId);

        if (dbTask == null) {
            return ErrorResultUtils.badRequest("No task with that id");
        }

        Task task = new Task();
        task.setVocabularyId(dbTask.getVocabularyId());
        task.setVersionId(dbTask.getVersionId());
        task.setStatus(dbTask.getStatus());
        task.setSubtasks(JSONSerialization.deserializeStringAsJson(
                dbTask.getParams(), new TypeReference<List<Subtask>>() { }));
        task.setResults(JSONSerialization.deserializeStringAsJson(
                dbTask.getResponse(),
                new TypeReference<HashMap<String, String>>() { }));
        Logging.logRequest(true, request, uriInfo, profile,
                "Admin: get task by Id");
        return Response.ok(task).build();
    }

    /** Run a task, by Id.
     * @param request The HTTP request.
     * @param uriInfo The UriInfo of the request.
     * @param profile The caller's security profile.
     * @param taskId The task Id.
     * @return The task.
     */
    @Path(AdminApiPaths.TASKS + "/" + AdminApiPaths.TASK_ID)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Pac4JSecurity
    @PUT
    @ApiOperation(value = "Run a task.",
            notes = "This method is only available to administrator users. "
                    + "The task must exist in the database. There must be "
                    + "a current instance of the vocabulary and version.",
            response = Task.class)
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED,
                    message = "Not authenticated",
                    response = ErrorResult.class,
                    responseHeaders = {
                            @ResponseHeader(name = "WWW-Authenticate",
                                    response = String.class)
                            }),
            @ApiResponse(code = HttpStatus.SC_FORBIDDEN,
                    message = "Not authenticated, or not authorized",
                    response = ErrorResult.class)
            })
    public Response runTask(
            @Context final HttpServletRequest request,
            @Context final UriInfo uriInfo,
            @ApiParam(hidden = true) @Pac4JProfile
            final CommonProfile profile,
            @ApiParam(value = "The ID of the task to run.")
            @PathParam("taskId") final Integer taskId) {
        logger.info("Called runTask, id: " + taskId);
        if (!AuthUtils.profileIsSuperuser(profile)) {
            return ResponseUtils.generateForbiddenResponseNotSuperuser();
        }

        EntityManager em = null;
        EntityTransaction txn = null;
        Task task = null;

        try {
            em = DBContext.getEntityManager();
            txn = em.getTransaction();
            txn.begin();

            au.org.ands.vocabs.registry.db.entity.Task dbTask =
                    TaskDAO.getTaskById(em, taskId);
            Vocabulary vocabulary =
                    VocabularyDAO.getCurrentVocabularyByVocabularyId(em,
                            dbTask.getVocabularyId());
            Version version =
                    VersionDAO.getCurrentVersionByVersionId(em,
                            dbTask.getVersionId());

            TaskInfo taskInfo = new TaskInfo(dbTask, vocabulary, version);
            taskInfo.setEm(em);
            taskInfo.setNowTime(TemporalUtils.nowUTC());
            taskInfo.setModifiedBy(profile.getUsername());
            taskInfo.process();
            txn.commit();
            task = taskInfo.getTask();
            Logging.logRequest(true, request, uriInfo, profile,
                    "Admin: run task by Id");
            return Response.ok(task).build();
        } catch (Throwable t) {
            if (txn != null && txn.isActive()) {
                try {
                    logger.error("Exception during transaction; rolling back",
                            t);
                    txn.rollback();
                } catch (Exception e) {
                    logger.error("Rollback failure!", e);
                }
            } else {
                logger.error("Exception other than during transaction: ", t);
            }
        } finally {
            if (em != null) {
                em.close();
            }
        }

        // If we reach here, there was a failure.
        Logging.logRequest(false, request, uriInfo, profile,
                "Admin: run task by Id");
        return ResponseUtils.generateInternalServerError("Exception: see log");
    }


}
