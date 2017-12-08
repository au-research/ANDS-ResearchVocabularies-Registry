/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.converter;

import au.org.ands.vocabs.registry.db.internal.VocabularyJson;

/** MapStruct mapper from Vocabulary database to Related Vocabulary schema. */
public abstract class VocabularyDbRelatedVocabularySchemaMapperDecorator
    implements VocabularyDbRelatedVocabularySchemaMapper {

    /** The delegate mapper. */
    private final VocabularyDbRelatedVocabularySchemaMapper delegate;

    /** Constructor that accepts a delegate.
     * @param aDelegate The delegate mapper.
     */
    public VocabularyDbRelatedVocabularySchemaMapperDecorator(
            final VocabularyDbRelatedVocabularySchemaMapper aDelegate) {
        delegate = aDelegate;
    }

    /** Decorator method that extends the default mapping behaviour
     * with extraction of the JSON data.
     * (Don't make this method final; it is extended by the implementation
     * class.)
     */
    @Override
    @SuppressWarnings("checkstyle:DesignForExtension")
    public au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedVocabulary
    sourceToTarget(final au.org.ands.vocabs.registry.db.entity.Vocabulary
            source) {
        if (source == null) {
            return null;
        }

        // Basic fields, handled for us by MapStruct.
        au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedVocabulary
            target = delegate.sourceToTarget(source);

        // And last, the JSON.
        if (source.getData() != null) {
            VocabularyJson data =
                    JSONSerialization.deserializeStringAsJson(source.getData(),
                            VocabularyJson.class);
            jsonDataIntoTarget(data, target);
        }
        return target;
    }

}
