/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.user;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.context.ApiPaths;
import au.org.ands.vocabs.registry.api.context.SwaggerInterface;
import au.org.ands.vocabs.registry.db.converter.RelatedEntityDbSchemaMapper;
import au.org.ands.vocabs.registry.db.converter.RelatedEntityIdentifierDbSchemaMapper;
import au.org.ands.vocabs.registry.db.dao.RelatedEntityDAO;
import au.org.ands.vocabs.registry.db.dao.RelatedEntityIdentifierDAO;
import au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntity;
import au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntityIdentifier;
import au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntityList;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/** REST web services for getting related entities. */
@Path(ApiPaths.API_RESOURCE + "/" + ApiPaths.VOCABULARIES)
@Api(value = SwaggerInterface.TAG_RESOURCES)
public class GetRelatedEntities {

    // For my info: if adding a method with return type Response
    // where the value returned is a list, wrap it in a GenericEntity.
    // See http://www.adam-bien.com/roller/abien/entry/jax_rs_returning_a_list

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Get the current related entities of a vocabulary, by its vocabulary id.
     * Delegates to {@link #getRelatedEntitiesForVocabularyByIdHelper(Integer)}
     * to do the work.
     * @param vocabularyId The VocabularyId of the related entities
     *      to be fetched.
     * @return The list of related entities, in either XML or JSON format,
     *      or an error result, if there is no such vocabulary. */
    @Path(ApiPaths.VOCABULARY_ID + "/" + ApiPaths.RELATED_ENTITIES)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @GET
    @ApiOperation(value = "Get the current related entities of a vocabulary, "
            + "by its vocabulary id.")
    public final RelatedEntityList getRelatedEntitiesForVocabularyById(
            @ApiParam(value = "The ID of the vocabulary from which to get "
                    + "the current related entities")
            @PathParam("vocabularyId") final Integer vocabularyId) {
        logger.debug("called getRelatedEntitiesForVocabularyById: "
            + vocabularyId);

        RelatedEntityList outputRelatedEntityList = new RelatedEntityList();

        List<RelatedEntity> outputRelatedEntities =
                getRelatedEntitiesForVocabularyByIdHelper(vocabularyId);

        outputRelatedEntityList.getRelatedEntity().
                addAll(outputRelatedEntities);
        return outputRelatedEntityList;
    }

    /** Helper method to do the work of getting the current related
     * entities of a vocabulary, by its vocabulary id.
     * @param vocabularyId The VocabularyId of the related entities
     *      to be fetched.
     * @return The list of related entities.
     */
    public static List<RelatedEntity> getRelatedEntitiesForVocabularyByIdHelper(
            final Integer vocabularyId) {
        List<au.org.ands.vocabs.registry.db.entity.RelatedEntity>
            dbRelatedEntities =
                RelatedEntityDAO.getCurrentRelatedEntitiesForVocabulary(
                    vocabularyId);
        ArrayList<RelatedEntity> outputRelatedEntities = new ArrayList<>();

        RelatedEntityDbSchemaMapper reMapper =
                RelatedEntityDbSchemaMapper.INSTANCE;
        RelatedEntityIdentifierDbSchemaMapper reiMapper =
                RelatedEntityIdentifierDbSchemaMapper.INSTANCE;
        for (au.org.ands.vocabs.registry.db.entity.RelatedEntity dbRE
                : dbRelatedEntities) {
            RelatedEntity targetRelatedEntity = reMapper.sourceToTarget(dbRE);
            outputRelatedEntities.add(targetRelatedEntity);
            // Get the related entity identifiers.
            List<RelatedEntityIdentifier> targetRelatedEntityIdentifiers =
                    targetRelatedEntity.getRelatedEntityIdentifier();
            List<au.org.ands.vocabs.registry.db.entity.RelatedEntityIdentifier>
                dbRelatedEntityIdentifiers =
                    RelatedEntityIdentifierDAO.
                    getCurrentRelatedEntityIdentifierListForRelatedEntity(
                            dbRE.getRelatedEntityId());
            for (au.org.ands.vocabs.registry.db.entity.RelatedEntityIdentifier
                    dbREI : dbRelatedEntityIdentifiers) {
                targetRelatedEntityIdentifiers.add(
                        reiMapper.sourceToTarget(dbREI));
            }
        }
        return outputRelatedEntities;
    }

}
