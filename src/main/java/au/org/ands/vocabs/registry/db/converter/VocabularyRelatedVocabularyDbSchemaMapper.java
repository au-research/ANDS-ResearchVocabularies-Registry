/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/** MapStruct mapper from VocabularyRelatedVocabulary database to schema. */
@Mapper
public interface VocabularyRelatedVocabularyDbSchemaMapper {

    /** Singleton instance of this mapper. */
    VocabularyRelatedVocabularyDbSchemaMapper INSTANCE =
            Mappers.getMapper(VocabularyRelatedVocabularyDbSchemaMapper.class);

    /** MapStruct-generated Mapper from VocabularyRelatedVocabulary
     * database to schema.
     * @param source The VocabularyRelatedVocabulary entity from the database.
     * @return The schema version of the related vocabulary reference.
     */
    @Mapping(source = "relatedVocabularyId", target = "id")
    au.org.ands.vocabs.registry.schema.vocabulary201701.
    Vocabulary.RelatedVocabularyRef
        sourceToTarget(
            au.org.ands.vocabs.registry.db.entity.VocabularyRelatedVocabulary
                source);

}
