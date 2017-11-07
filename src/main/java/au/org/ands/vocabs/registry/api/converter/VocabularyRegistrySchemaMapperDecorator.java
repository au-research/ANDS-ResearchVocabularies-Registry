/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.converter;

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
        target.setData(JSONSerialization.serializeObjectAsJsonString(
                vocabularyJson));
    }

}
