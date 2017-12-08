/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/** MapStruct mapper from Version Artefact database to schema. */
@Mapper
//@DecoratedWith(VersionArtefactDbSchemaMapperDecorator.class)
public interface VersionArtefactDbSchemaMapper {

    /** Singleton instance of this mapper. */
    VersionArtefactDbSchemaMapper INSTANCE =
            Mappers.getMapper(VersionArtefactDbSchemaMapper.class);

    /** MapStruct-generated Mapper from Version Artefact database to schema.
     * @param source The Version entity from the database.
     * @return The schema version of the version.
     */
    @Mapping(source = "versionArtefactId", target = "id")
    // Ignore here the fields that are extracted from JSON data.
    au.org.ands.vocabs.registry.schema.vocabulary201701.VersionArtefact
      sourceToTarget(au.org.ands.vocabs.registry.db.entity.VersionArtefact
              source);

    /* For future expansion, when we expose more details of
     * some of the version artefacts.
     */
    /* * MapStruct-generated Mapper from VersionArtefactJson into an existing
     * schema object.
     * @param data The Version JSON data from the database.
     * @param target The schema version of the version to be updated.
     */
    /*
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    void jsonDataIntoTarget(VersionArtefactJson data,
              @MappingTarget
              au.org.ands.vocabs.registry.schema.vocabulary201701.
              VersionArtefact target);
    */
}
