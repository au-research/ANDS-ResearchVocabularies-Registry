/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.converter;

import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import au.org.ands.vocabs.registry.db.internal.RelatedPartyJson;
import au.org.ands.vocabs.registry.db.internal.RelatedServiceJson;
import au.org.ands.vocabs.registry.db.internal.RelatedVocabularyJson;

/** MapStruct mapper from RelatedEntity database to schema. */
@Mapper
//@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(RelatedEntityDbSchemaMapperDecorator.class)
public interface RelatedEntityDbSchemaMapper {

    /** Singleton instance of this mapper. */
    RelatedEntityDbSchemaMapper INSTANCE =
            Mappers.getMapper(RelatedEntityDbSchemaMapper.class);

    /** MapStruct-generated Mapper from RelatedEntity database to schema.
     * @param source The RelatedEntity entity from the database.
     * @return The schema version of the related entity.
     */
    @Mapping(source = "relatedEntityId", target = "id")
    // Ignore here the fields that are extracted from JSON data.
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "uri", ignore = true)
    @Mapping(target = "relatedEntityIdentifier", ignore = true)
    au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntity
        sourceToTarget(au.org.ands.vocabs.registry.db.entity.RelatedEntity
                source);

    /** MapStruct-generated Mapper from RelatedPartyJson into an existing
     * schema object.
     * @param data The RelatedEntity JSON data from the database.
     * @param target The schema version of the related entity to be updated.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "relatedEntityIdentifier", ignore = true)
    void jsonDataIntoTarget(RelatedPartyJson data,
              @MappingTarget
              au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntity
              target);

    /** MapStruct-generated Mapper from RelatedServiceJson into an existing
     * schema object.
     * @param data The RelatedEntity JSON data from the database.
     * @param target The schema version of the related entity to be updated.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "relatedEntityIdentifier", ignore = true)
    void jsonDataIntoTarget(RelatedServiceJson data,
              @MappingTarget
              au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntity
              target);

    /** MapStruct-generated Mapper from RelatedVocabularyJson into an existing
     * schema object.
     * @param data The RelatedEntity JSON data from the database.
     * @param target The schema version of the related entity to be updated.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "relatedEntityIdentifier", ignore = true)
    void jsonDataIntoTarget(RelatedVocabularyJson data,
              @MappingTarget
              au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntity
              target);

}
