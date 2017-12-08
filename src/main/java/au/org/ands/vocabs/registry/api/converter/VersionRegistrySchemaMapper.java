/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.converter;

import java.time.LocalDateTime;

import org.mapstruct.Context;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import au.org.ands.vocabs.registry.db.internal.VersionJson;

/** MapStruct mapper from Version registry schema to database. */
@Mapper
@DecoratedWith(VersionRegistrySchemaMapperDecorator.class)
public interface VersionRegistrySchemaMapper {

    /** Singleton instance of this mapper. */
    VersionRegistrySchemaMapper INSTANCE =
            Mappers.getMapper(VersionRegistrySchemaMapper.class);

    /** MapStruct-generated Mapper from Version registry schema to database.
     * @param source The Version entity from the registry.
     * @return The database version of the version.
     */
    // OH! Note carefully the _two_ @Mapping annotations for ids!
    // With the second, but without the first, the generated code invokes
    // setId(source.getId()) _as well as_
    // setVersionId(source.getId()).
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "id", target = "versionId")
    @Mapping(target = "vocabularyId", ignore = true)
    @Mapping(target = "startDate", ignore = true)
    @Mapping(target = "endDate", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "data", ignore = true)
    au.org.ands.vocabs.registry.db.entity.Version
    sourceToTarget(au.org.ands.vocabs.registry.schema.vocabulary201701.
              Version source);

    /** MapStruct-generated Mapper from Version registry schema to database.
     * This mapper is for use in creating a draft instance; the date of
     * creation of this draft is specified as a context parameter.
     * @param source The Version entity from the registry.
     * @param draftCreatedDate This is to be a new draft record, and this
     *      is the value of draftCreatedDate to be set.
     * @return The database version of the version.
     */
    // OH! Note carefully the _two_ @Mapping annotations for ids!
    // With the second, but without the first, the generated code invokes
    // setId(source.getId()) _as well as_
    // setVersionId(source.getId()).
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "source.id", target = "versionId")
    @Mapping(target = "vocabularyId", ignore = true)
    @Mapping(target = "startDate", ignore = true)
    @Mapping(target = "endDate", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "data", ignore = true)
    au.org.ands.vocabs.registry.db.entity.Version
    sourceToTarget(au.org.ands.vocabs.registry.schema.vocabulary201701.
              Version source,
              @Context LocalDateTime draftCreatedDate);

    /** MapStruct-generated Mapper from Version registry schema to database,
     * that updates an existing database entity.
     * @param source The Version entity from the registry.
     * @param target The database version of the version being updated.
     */
    // OH! Note carefully the _two_ @Mapping annotations for ids!
    // With the second, but without the first, the generated code invokes
    // setId(source.getId()) _as well as_
    // setVocabularyId(source.getId()).
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "id", target = "versionId")
    @Mapping(target = "vocabularyId", ignore = true)
    @Mapping(target = "startDate", ignore = true)
    @Mapping(target = "endDate", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "data", ignore = true)
    void updateTargetFromSource(
            au.org.ands.vocabs.registry.schema.vocabulary201701.
            Version source,
            @MappingTarget au.org.ands.vocabs.registry.db.entity.
            Version target);

    /** MapStruct-generated Mapper from Version registry schema to database,
     * that updates an existing database entity.
     * This mapper is for use in updating a draft instance; the date of
     * modification of this draft is specified as a context parameter.
     * @param source The Version entity from the registry.
     * @param target The database version of the version being updated.
     * @param draftModifiedDate The value of draftModifiedDate to be set.
     */
    // OH! Note carefully the _two_ @Mapping annotations for ids!
    // With the second, but without the first, the generated code invokes
    // setId(source.getId()) _as well as_
    // setVocabularyId(source.getId()).
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "source.id", target = "versionId")
    @Mapping(target = "vocabularyId", ignore = true)
    @Mapping(target = "startDate", ignore = true)
    @Mapping(target = "endDate", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "data", ignore = true)
    void updateTargetFromSource(
            au.org.ands.vocabs.registry.schema.vocabulary201701.
            Version source,
            @MappingTarget au.org.ands.vocabs.registry.db.entity.
            Version target,
            @Context LocalDateTime draftModifiedDate);

    /** MapStruct-generated Mapper from registry schema to VersionJson.
     * @param data The schema version of the version.
     * @return The component of the database version of the version that
     *      is stored as JSON data.
     */
    @Mapping(target = "draftCreatedDate", ignore = true)
    @Mapping(target = "draftModifiedDate", ignore = true)
    VersionJson sourceToJsonTarget(
            au.org.ands.vocabs.registry.schema.vocabulary201701.
                Version data);
}
