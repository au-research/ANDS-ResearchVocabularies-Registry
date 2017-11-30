/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.converter;

import java.time.LocalDateTime;

import org.mapstruct.Context;
import org.mapstruct.MappingTarget;

import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.internal.VocabularyJson;

/** MapStruct mapper from Vocabulary registry schema to database. */
public abstract class VocabularyRegistrySchemaMapperDecorator
    implements VocabularyRegistrySchemaMapper {

    /** The delegate mapper. */
    private final VocabularyRegistrySchemaMapper delegate;

    /** Constructor that accepts a delegate.
     * @param aDelegate The delegate mapper.
     */
    public VocabularyRegistrySchemaMapperDecorator(
            final VocabularyRegistrySchemaMapper aDelegate) {
        delegate = aDelegate;
    }

    /** Decorator method that extends the default mapping behaviour
     * with insertion of the JSON data.
     * (Don't make this method final; it is extended by the implementation
     * class.)
     */
    @Override
    @SuppressWarnings("checkstyle:DesignForExtension")
    public au.org.ands.vocabs.registry.db.entity.Vocabulary
        sourceToTarget(final
            au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
            source) {
        if (source == null) {
            return null;
        }
        au.org.ands.vocabs.registry.db.entity.Vocabulary
            target = delegate.sourceToTarget(source);
        VocabularyJson vocabularyJson =
                sourceToJsonTarget(source);
        target.setData(JSONSerialization.serializeObjectAsJsonString(
                vocabularyJson));
        return target;
    }

    /** Decorator method that extends the default mapping behaviour
     * with insertion of the JSON data.
     * This version of the method is for creating a draft.
     * (Don't make this method final; it is extended by the implementation
     * class.)
     */
    @Override
    @SuppressWarnings("checkstyle:DesignForExtension")
    public au.org.ands.vocabs.registry.db.entity.Vocabulary
        sourceToTarget(final
            au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
            source, @Context final LocalDateTime draftCreatedDate) {
        if (source == null) {
            return null;
        }
        au.org.ands.vocabs.registry.db.entity.Vocabulary
            target = delegate.sourceToTarget(source);
        VocabularyJson vocabularyJson =
                sourceToJsonTarget(source);
        vocabularyJson.setDraftCreatedDate(draftCreatedDate.toString());
        vocabularyJson.setDraftModifiedDate(draftCreatedDate.toString());
        target.setData(JSONSerialization.serializeObjectAsJsonString(
                vocabularyJson));
        return target;
    }

    /** Decorator method that extends the default mapping behaviour
     * with insertion of the JSON data.
     * (Don't make this method final; it is extended by the implementation
     * class.)
     */
    @Override
    @SuppressWarnings("checkstyle:DesignForExtension")
    public void
    updateTargetFromSource(final
            au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
            source,
            @MappingTarget final au.org.ands.vocabs.registry.db.entity.
            Vocabulary target) {
        if (source == null) {
            return;
        }
        delegate.updateTargetFromSource(source, target);
        VocabularyJson vocabularyJson =
                sourceToJsonTarget(source);
        vocabularyJson.setDraftCreatedDate(null);
        vocabularyJson.setDraftModifiedDate(null);
        target.setData(JSONSerialization.serializeObjectAsJsonString(
                vocabularyJson));
    }

    /** Decorator method that extends the default mapping behaviour
     * with insertion of the JSON data.
     * This version of the method is for updating an existing draft.
     * (Don't make this method final; it is extended by the implementation
     * class.)
     */
    @Override
    @SuppressWarnings("checkstyle:DesignForExtension")
    public void
    updateTargetFromSource(final
            au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
            source,
            @MappingTarget final au.org.ands.vocabs.registry.db.entity.
            Vocabulary target, @Context final LocalDateTime draftModifiedDate) {
        if (source == null) {
            return;
        }
        delegate.updateTargetFromSource(source, target);
        // Unpack the original JSON data in order to get at the draft
        // creation date ...
        VocabularyJson originalVocabularyJson =
                JSONSerialization.deserializeStringAsJson(target.getData(),
                        VocabularyJson.class);
        String draftCreatedDate =
                originalVocabularyJson.getDraftCreatedDate();
        // ... but create new JSON data from the source.
        VocabularyJson vocabularyJson =
                sourceToJsonTarget(source);
        vocabularyJson.setDraftCreatedDate(draftCreatedDate);
        vocabularyJson.setDraftModifiedDate(draftModifiedDate.toString());
        target.setData(JSONSerialization.serializeObjectAsJsonString(
                vocabularyJson));
    }

}
