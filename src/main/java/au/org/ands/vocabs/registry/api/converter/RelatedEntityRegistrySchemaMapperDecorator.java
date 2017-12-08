/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.converter;

import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.internal.RelatedEntityCommon;

/** MapStruct mapper from Related Entity registry schema to database. */
public abstract class RelatedEntityRegistrySchemaMapperDecorator
    implements RelatedEntityRegistrySchemaMapper {

    /** The delegate mapper. */
    private final RelatedEntityRegistrySchemaMapper delegate;

    /** Constructor that accepts a delegate.
     * @param aDelegate The delegate mapper.
     */
    public RelatedEntityRegistrySchemaMapperDecorator(
            final RelatedEntityRegistrySchemaMapper aDelegate) {
        delegate = aDelegate;
    }

    /** Decorator method that extends the default mapping behaviour
     * with insertion of the JSON data.
     * It is basically a big "switch" statement
     * that delegates to the mappers for the various subclasses
     * of RelatedEntityCommon.
     * (Don't make this method final; it is extended by the implementation
     * class.)
     */
    @Override
    @SuppressWarnings("checkstyle:DesignForExtension")
    public au.org.ands.vocabs.registry.db.entity.RelatedEntity
        sourceToTarget(final
            au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntity
            source) {
        if (source == null) {
            return null;
        }
        au.org.ands.vocabs.registry.db.entity.RelatedEntity
            target = delegate.sourceToTarget(source);
        RelatedEntityCommon reJson = null;
        // There should be a better way than this switch statement.
        // See also RelatedEntityDbSchemaMapperDecorator.jsonDataIntoTarget().
        switch (source.getType()) {
        case PARTY:
            reJson = sourceToPartyJsonTarget(source);
            break;
        case SERVICE:
            reJson = sourceToServiceJsonTarget(source);
            break;
        case VOCABULARY:
            reJson = sourceToVocabularyJsonTarget(source);
            break;
        default:
            // Can't happen!
            break;
        }
        target.setData(JSONSerialization.serializeObjectAsJsonString(
                reJson));
        return target;
    }

}
