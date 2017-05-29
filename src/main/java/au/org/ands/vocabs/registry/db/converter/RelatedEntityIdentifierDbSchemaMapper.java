/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/** MapStruct mapper from RelatedEntityIdentifier database to schema. */
@Mapper
public interface RelatedEntityIdentifierDbSchemaMapper {

    /** Singleton instance of this mapper. */
    RelatedEntityIdentifierDbSchemaMapper INSTANCE =
            Mappers.getMapper(RelatedEntityIdentifierDbSchemaMapper.class);

    /** MapStruct-generated Mapper from RelatedEntityIdentifier
     * database to schema.
     * @param source The Version entity from the database.
     * @return The schema version of the version.
     */
    @Mapping(source = "relatedEntityIdentifierId", target = "id")
    au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntityIdentifier
        sourceToTarget(au.org.ands.vocabs.registry.db.entity.
                RelatedEntityIdentifier
                source);

}
