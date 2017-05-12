/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/** MapStruct mapper from VocabularyRelatedEntity database to schema. */
@Mapper
public interface VocabularyRelatedEntityDbSchemaMapper {

    /** Singleton instance of this mapper. */
    VocabularyRelatedEntityDbSchemaMapper INSTANCE =
            Mappers.getMapper(VocabularyRelatedEntityDbSchemaMapper.class);

    /** MapStruct-generated Mapper from VocabularyRelatedEntity
     * database to schema.
     * @param source The VocabularyRelatedEntity entity from the database.
     * @return The schema version of the related entity reference.
     */
    @Mapping(source = "relatedEntityId", target = "id")
    au.org.ands.vocabs.registry.schema.vocabulary201701.
    Vocabulary.RelatedEntityRef
        sourceToTarget(
                au.org.ands.vocabs.registry.db.entity.VocabularyRelatedEntity
                source);

}
