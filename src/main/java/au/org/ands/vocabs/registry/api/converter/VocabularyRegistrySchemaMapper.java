/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.converter;

import java.time.LocalDateTime;

import org.mapstruct.Context;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import au.org.ands.vocabs.registry.db.internal.VocabularyJson;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;

/** MapStruct mapper from Vocabulary registry schema to database. */
@Mapper
@DecoratedWith(VocabularyRegistrySchemaMapperDecorator.class)
public interface VocabularyRegistrySchemaMapper {

    /** Singleton instance of this mapper. */
    VocabularyRegistrySchemaMapper INSTANCE =
            Mappers.getMapper(VocabularyRegistrySchemaMapper.class);

    /** MapStruct-generated Mapper from Vocabulary registry schema to database.
     * @param source The Vocabulary entity from the registry.
     * @return The database version of the vocabulary.
     */
    // OH! Note carefully the _two_ @Mapping annotations for ids!
    // With the second, but without the first, the generated code invokes
    // setId(source.getId()) _as well as_
    // setVocabularyId(source.getId()).
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "id", target = "vocabularyId")
    @Mapping(target = "startDate", ignore = true)
    @Mapping(target = "endDate", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "data", ignore = true)
    au.org.ands.vocabs.registry.db.entity.Vocabulary
      sourceToTarget(au.org.ands.vocabs.registry.schema.vocabulary201701.
              Vocabulary source);

    /** MapStruct-generated Mapper from Vocabulary registry schema to database.
     * This mapper is for use in creating a draft instance; the date of
     * creation of this draft is specified as a context parameter.
     * @param source The Vocabulary entity from the registry.
     * @param draftCreatedDate This is to be a new draft record, and this
     *      is the value of draftCreatedDate to be set.
     * @return The database version of the vocabulary.
     */
    // OH! Note carefully the _two_ @Mapping annotations for ids!
    // With the second, but without the first, the generated code invokes
    // setId(source.getId()) _as well as_
    // setVocabularyId(source.getId()).
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "source.id", target = "vocabularyId")
    @Mapping(target = "startDate", ignore = true)
    @Mapping(target = "endDate", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "data", ignore = true)
    au.org.ands.vocabs.registry.db.entity.Vocabulary
      sourceToTarget(au.org.ands.vocabs.registry.schema.vocabulary201701.
              Vocabulary source,
              @Context LocalDateTime draftCreatedDate);


    /** MapStruct-generated Mapper from Vocabulary registry schema to database,
     * that updates an existing database entity.
     * This is to be a current instance; any draft creation/modification dates
     * are removed.
     * @param source The Vocabulary entity from the registry.
     * @param target The database version of the vocabulary being updated.
     */
    // OH! Note carefully the _two_ @Mapping annotations for ids!
    // With the second, but without the first, the generated code invokes
    // setId(source.getId()) _as well as_
    // setVocabularyId(source.getId()).
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "id", target = "vocabularyId")
    @Mapping(target = "startDate", ignore = true)
    @Mapping(target = "endDate", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "data", ignore = true)
    void updateTargetFromSource(
            au.org.ands.vocabs.registry.schema.vocabulary201701.
            Vocabulary source,
            @MappingTarget au.org.ands.vocabs.registry.db.entity.
            Vocabulary target);

    /** MapStruct-generated Mapper from Vocabulary registry schema to database,
     * that updates an existing database entity.
     * This mapper is for use in updating a draft instance; the date of
     * modification of this draft is specified as a context parameter.
     * @param source The Vocabulary entity from the registry.
     * @param target The database version of the vocabulary being updated.
     */
    // OH! Note carefully the _two_ @Mapping annotations for ids!
    // With the second, but without the first, the generated code invokes
    // setId(source.getId()) _as well as_
    // setVocabularyId(source.getId()).
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "source.id", target = "vocabularyId")
    @Mapping(target = "startDate", ignore = true)
    @Mapping(target = "endDate", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "data", ignore = true)
    void updateTargetFromSource(
            au.org.ands.vocabs.registry.schema.vocabulary201701.
            Vocabulary source,
            @MappingTarget au.org.ands.vocabs.registry.db.entity.
            Vocabulary target,
            @Context LocalDateTime draftModifiedDate);

    /** MapStruct-generated Mapper from registry schema to VocabularyJson.
     * @param data The schema version of the vocabulary.
     * @return The component of the database version of the vocabulary that
     *      is stored as JSON data.
     */
    @Mapping(source = "otherLanguage", target = "otherLanguages")
    @Mapping(source = "subject", target = "subjects")
    @Mapping(source = "topConcept", target = "topConcepts")
    @Mapping(target = "draftCreatedDate", ignore = true)
    @Mapping(target = "draftModifiedDate", ignore = true)
    VocabularyJson sourceToJsonTarget(
            au.org.ands.vocabs.registry.schema.vocabulary201701.
                Vocabulary data);

    /** MapStruct-generated Mapper from schema subject data
     * to database.
     * @param source The schema version of the subject data.
     * @return The subject data for the database.
     */
    au.org.ands.vocabs.registry.db.internal.VocabularyJson.Subjects
    subjectSourceToTarget(Vocabulary.Subject source);

    /** MapStruct-generated Mapper from schema PoolParty data
     * to database.
     * @param source The PoolParty data from the database.
     * @return The schema version of the PoolParty data.
     */
    au.org.ands.vocabs.registry.db.internal.VocabularyJson.
    PoolpartyProject
    poolpartyProjectSourceToTarget(Vocabulary.PoolpartyProject source);

}
