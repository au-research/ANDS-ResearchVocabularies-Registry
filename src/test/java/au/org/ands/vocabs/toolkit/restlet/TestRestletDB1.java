package au.org.ands.vocabs.toolkit.restlet;

import java.lang.invoke.MethodHandles;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.toolkit.db.model.Todo;
import au.org.ands.vocabs.toolkit.utils.ToolkitProperties;

/** Testing restlet. */
@Path("testingDB")
public class TestRestletDB1 {

    /** The logger. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Access to persistence context. */
    private EntityManager entityManager =
            Persistence.createEntityManagerFactory("ANDS-Vocabs-Toolkit",
                    ToolkitProperties.getProperties()).
                    createEntityManager();

    /** getMessage.
     * @return the message. */
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public final String getMessage() {
        logger.info("Running TestRestletDB1.getMessage().");

        // Read the existing entries and log
        Query q = entityManager.createQuery("select t from Todo t");
        @SuppressWarnings("unchecked")
        List<Todo> todoList = q.getResultList();
        for (Todo todo : todoList) {
          logger.info(todo.toString());
        }
        logger.info("Size: " + todoList.size());

        return "Hello World! Again!";

        }


}