/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.converter;

import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import au.org.ands.vocabs.registry.db.internal.RelatedPartyJson;
import au.org.ands.vocabs.registry.db.internal.RelatedServiceJson;
import au.org.ands.vocabs.registry.db.internal.RelatedVocabularyJson;

/** MapStruct mapper from Related Entity registry schema to database. */
@Mapper
@DecoratedWith(RelatedEntityRegistrySchemaMapperDecorator.class)
public interface RelatedEntityRegistrySchemaMapper {

    /** Singleton instance of this mapper. */
    RelatedEntityRegistrySchemaMapper INSTANCE =
            Mappers.getMapper(RelatedEntityRegistrySchemaMapper.class);

    /** MapStruct-generated Mapper from Related Entity registry schema
     * to database.
     * @param source The Related Entity entity from the registry.
     * @return The database version of the related entity.
     */
    // OH! Note carefully the _two_ @Mapping annotations for ids!
    // With the second, but without the first, the generated code invokes
    // setId(source.getId()) _as well as_
    // setRelatedEntityId(source.getId()).
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "id", target = "relatedEntityId")
    @Mapping(target = "startDate", ignore = true)
    @Mapping(target = "endDate", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "data", ignore = true)
    au.org.ands.vocabs.registry.db.entity.RelatedEntity
      sourceToTarget(au.org.ands.vocabs.registry.schema.vocabulary201701.
              RelatedEntity source);

    // One mapper method is required below for each type of related entity.
    // If the following set of methods is modified, the switch statement
    // in the method
    // RelatedEntityRegistrySchemaMapperDecorator.sourceToTarget()
    // must be modified accordingly.

    /** MapStruct-generated Mapper from registry schema to RelatedPartyJson.
     * @param data The schema version of the related entity.
     * @return The component of the database version of the related entity that
     *      is stored as JSON data.
     */
    RelatedPartyJson sourceToPartyJsonTarget(
            au.org.ands.vocabs.registry.schema.vocabulary201701.
                RelatedEntity data);

    /** MapStruct-generated Mapper from registry schema to RelatedServiceJson.
     * @param data The schema version of the related entity.
     * @return The component of the database version of the related entity that
     *      is stored as JSON data.
     */
    RelatedServiceJson sourceToServiceJsonTarget(
            au.org.ands.vocabs.registry.schema.vocabulary201701.
                RelatedEntity data);

    /** MapStruct-generated Mapper from registry schema to
     * RelatedVocabularyJson.
     * @param data The schema version of the related entity.
     * @return The component of the database version of the related entity that
     *      is stored as JSON data.
     */
    RelatedVocabularyJson sourceToVocabularyJsonTarget(
            au.org.ands.vocabs.registry.schema.vocabulary201701.
                RelatedEntity data);

}
