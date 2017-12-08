/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.converter;

import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import au.org.ands.vocabs.registry.db.internal.VocabularyJson;

/** MapStruct mapper from Vocabulary database to Related Vocabulary schema. */
@Mapper
//@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(VocabularyDbRelatedVocabularySchemaMapperDecorator.class)
public interface VocabularyDbRelatedVocabularySchemaMapper {

    /** Singleton instance of this mapper. */
    VocabularyDbRelatedVocabularySchemaMapper INSTANCE =
            Mappers.getMapper(VocabularyDbRelatedVocabularySchemaMapper.class);

    /** MapStruct-generated Mapper from Vocabulary database to schema.
     * @param source The Vocabulary entity from the database.
     * @return The schema version of the vocabulary.
     */
    @Mapping(source = "vocabularyId", target = "id")
    @Mapping(target = "acronym", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "title", ignore = true)
    au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedVocabulary
      sourceToTarget(au.org.ands.vocabs.registry.db.entity.Vocabulary source);

    /** MapStruct-generated Mapper from VocabularyJson into an existing
     * schema object.
     * @param data The Vocabulary JSON data from the database.
     * @param target The schema version of the vocabulary to be updated.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "status", ignore = true)
    void jsonDataIntoTarget(VocabularyJson data,
              @MappingTarget
              au.org.ands.vocabs.registry.schema.vocabulary201701.
              RelatedVocabulary
              target);

}
