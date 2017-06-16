/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/** MapStruct mapper from Related Entity Identifier registry schema
 * to database. */
@Mapper
//@DecoratedWith(RelatedEntityIdentifierRegistrySchemaMapperDecorator.class)
public interface RelatedEntityIdentifierRegistrySchemaMapper {

    /** Singleton instance of this mapper. */
    RelatedEntityIdentifierRegistrySchemaMapper INSTANCE =
            Mappers.getMapper(
                    RelatedEntityIdentifierRegistrySchemaMapper.class);

    /** MapStruct-generated Mapper from Related Entity Identifier
     * registry schema to database.
     * @param source The Related Entity Identifier entity from the registry.
     * @return The database version of the related entity identifier.
     */
    // OH! Note carefully the _two_ @Mapping annotations for ids!
    // With the second, but without the first, the generated code invokes
    // setId(source.getId()) _as well as_
    // setRelatedEntityIdentifierId(source.getId()).
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "id", target = "relatedEntityIdentifierId")
    @Mapping(target = "startDate", ignore = true)
    @Mapping(target = "endDate", ignore = true)
    @Mapping(target = "relatedEntityId", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    au.org.ands.vocabs.registry.db.entity.RelatedEntityIdentifier
      sourceToTarget(au.org.ands.vocabs.registry.schema.vocabulary201701.
              RelatedEntityIdentifier source);

}
