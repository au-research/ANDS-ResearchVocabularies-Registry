/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.converter;

import java.util.HashMap;

import org.mapstruct.MappingTarget;

import au.org.ands.vocabs.registry.db.internal.RelatedEntityCommon;
import au.org.ands.vocabs.registry.db.internal.RelatedPartyJson;
import au.org.ands.vocabs.registry.db.internal.RelatedServiceJson;
import au.org.ands.vocabs.registry.db.internal.RelatedVocabularyJson;
import au.org.ands.vocabs.registry.enums.RelatedEntityType;

/** MapStruct mapper from RelatedEntity database to schema. */
@SuppressWarnings("checkstyle:DesignForExtension")
public abstract class RelatedEntityDbSchemaMapperDecorator
    implements RelatedEntityDbSchemaMapper {

    /** Map from RelatedEntityType to the corresponding subclass of the
     * database JSON RelatedEntityCommon class that represents it. */
    private HashMap<RelatedEntityType, Class<? extends RelatedEntityCommon>>
        dbJsonClassMap = new HashMap<>();

    /** The delegate mapper. */
    private final RelatedEntityDbSchemaMapper delegate;

    /** Constructor that accepts a delegate.
     * @param aDelegate The delegate mapper.
     */
    public RelatedEntityDbSchemaMapperDecorator(
            final RelatedEntityDbSchemaMapper aDelegate) {
        delegate = aDelegate;

        dbJsonClassMap.put(RelatedEntityType.PARTY, RelatedPartyJson.class);
        dbJsonClassMap.put(RelatedEntityType.SERVICE, RelatedServiceJson.class);
        dbJsonClassMap.put(RelatedEntityType.VOCABULARY,
                RelatedVocabularyJson.class);
    }

    /** Decorator method that extends the default mapping behaviour
     * with extraction of the JSON data.
     * (Don't make this method final; it is extended by the implementation
     * class.)
     */
    @Override
    public au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntity
    sourceToTarget(final au.org.ands.vocabs.registry.db.entity.RelatedEntity
            source) {
        if (source == null) {
            return null;
        }
        au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntity
            target = delegate.sourceToTarget(source);
        if (source.getData() != null) {
            RelatedEntityCommon data;
            Class<? extends RelatedEntityCommon> dbTargetClass =
                    dbJsonClassMap.get(source.getType());
            data = JSONSerialization.deserializeStringAsJson(source.getData(),
                            dbTargetClass);
            jsonDataIntoTarget(data, target);
        }
        return target;
    }

    /** Hand-coded mapper from RelatedEntityCommon into an existing
     * schema object. It is basically a big "switch" statement
     * that delegates to the mappers for the various subclasses
     * of RelatedEntityCommon.
     * It's a shame to have to code it this way. See
     * https://github.com/mapstruct/mapstruct/issues/131
     * for an open issue for MapStruct that would simplify the
     * implementation of this method.
     * @param data The AccessPoint JSON data from the database.
     * @param target The schema version of the access point to be updated.
     */
    void jsonDataIntoTarget(final RelatedEntityCommon data,
              @MappingTarget final
              au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntity
              target) {
        // There should be a better way than this "switch" statement.
        // See also RelatedEntityRegistrySchemaMapperDecorator.sourceToTarget().
        if (data instanceof RelatedPartyJson) {
            jsonDataIntoTarget((RelatedPartyJson) data, target);
        } else if (data instanceof RelatedServiceJson) {
            jsonDataIntoTarget((RelatedServiceJson) data, target);
        } else if (data instanceof RelatedVocabularyJson) {
            jsonDataIntoTarget((RelatedVocabularyJson) data, target);
        }
    }

}
