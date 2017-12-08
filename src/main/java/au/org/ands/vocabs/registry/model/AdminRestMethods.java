/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.model;

import java.util.List;

import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import au.org.ands.vocabs.registry.api.context.AdminApiPaths;
import au.org.ands.vocabs.registry.api.context.ApiPaths;
import au.org.ands.vocabs.registry.api.user.ErrorResult;
import au.org.ands.vocabs.registry.api.user.SimpleResult;
import au.org.ands.vocabs.registry.db.context.DBContext;

/** Vocabulary model administration tools available through a REST-like
 * interface. */
@Path(AdminApiPaths.API_ADMIN + "/" + AdminApiPaths.MODEL)
public class AdminRestMethods {

    /** Generate the model of a vocabulary, and log it.
     * @param vocabularyId The vocabulary ID of the vocabulary whose model
     *      is to be computed and logged.
     * @return Result of the logging.
     */
    @Path("logModel" + "/" + ApiPaths.VOCABULARY_ID)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @GET
    public final Response logModel(@PathParam("vocabularyId") final Integer
            vocabularyId) {
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            new VocabularyModel(em, vocabularyId).logModelDescription();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity(new ErrorResult("No such vocabulary")).build();
        } finally {
            if (em != null) {
                em.close();
            }
        }
        return Response.ok().entity(new SimpleResult("OK")).build();
    }

    /** Generate the model of a vocabulary, and describe it.
     * @param vocabularyId The vocabulary ID of the vocabulary whose model
     *      is to be computed and logged.
     * @return The description of the model of the vocabulary.
     */
    @Path("describeModel" + "/" + ApiPaths.VOCABULARY_ID)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @GET
    public final Response describeModel(
            @PathParam("vocabularyId") final Integer vocabularyId) {
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            List<String> description =
                    new VocabularyModel(em, vocabularyId).describeModel();
            return Response.ok().entity(description).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity(new ErrorResult("No such vocabulary")).build();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

}
