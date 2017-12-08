/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.converter;

import java.time.LocalDateTime;

import org.mapstruct.Context;
import org.mapstruct.MappingTarget;

import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.internal.VersionJson;

/** MapStruct mapper from Version registry schema to database. */
public abstract class VersionRegistrySchemaMapperDecorator
    implements VersionRegistrySchemaMapper {

    /** The delegate mapper. */
    private final VersionRegistrySchemaMapper delegate;

    /** Constructor that accepts a delegate.
     * @param aDelegate The delegate mapper.
     */
    public VersionRegistrySchemaMapperDecorator(
            final VersionRegistrySchemaMapper aDelegate) {
        delegate = aDelegate;
    }

    /** Decorator method that extends the default mapping behaviour
     * with insertion of the JSON data.
     * (Don't make this method final; it is extended by the implementation
     * class.)
     */
    @Override
    @SuppressWarnings("checkstyle:DesignForExtension")
    public au.org.ands.vocabs.registry.db.entity.Version
        sourceToTarget(final
            au.org.ands.vocabs.registry.schema.vocabulary201701.Version
            source) {
        if (source == null) {
            return null;
        }
        au.org.ands.vocabs.registry.db.entity.Version
            target = delegate.sourceToTarget(source);
        VersionJson versionJson =
                sourceToJsonTarget(source);
        target.setData(JSONSerialization.serializeObjectAsJsonString(
                versionJson));
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
    public au.org.ands.vocabs.registry.db.entity.Version
        sourceToTarget(final
            au.org.ands.vocabs.registry.schema.vocabulary201701.Version
            source, @Context final LocalDateTime draftCreatedDate) {
        if (source == null) {
            return null;
        }
        au.org.ands.vocabs.registry.db.entity.Version
            target = delegate.sourceToTarget(source);
        VersionJson versionJson =
                sourceToJsonTarget(source);
        versionJson.setDraftCreatedDate(draftCreatedDate.toString());
        versionJson.setDraftModifiedDate(draftCreatedDate.toString());
        target.setData(JSONSerialization.serializeObjectAsJsonString(
                versionJson));
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
            au.org.ands.vocabs.registry.schema.vocabulary201701.Version
            source,
            @MappingTarget final au.org.ands.vocabs.registry.db.entity.
            Version target) {
        if (source == null) {
            return;
        }
        delegate.updateTargetFromSource(source, target);
        VersionJson versionJson =
                sourceToJsonTarget(source);
        target.setData(JSONSerialization.serializeObjectAsJsonString(
                versionJson));
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
            au.org.ands.vocabs.registry.schema.vocabulary201701.Version
            source,
            @MappingTarget final au.org.ands.vocabs.registry.db.entity.
            Version target, @Context final LocalDateTime draftModifiedDate) {
        if (source == null) {
            return;
        }
        delegate.updateTargetFromSource(source, target);
        // Unpack the original JSON data in order to get at the draft
        // creation date ...
        VersionJson originalVersionJson =
                JSONSerialization.deserializeStringAsJson(target.getData(),
                        VersionJson.class);
        String draftCreatedDate =
                originalVersionJson.getDraftCreatedDate();
        // ... but create new JSON data from the source.
        VersionJson versionJson =
                sourceToJsonTarget(source);
        versionJson.setDraftCreatedDate(draftCreatedDate);
        versionJson.setDraftModifiedDate(draftModifiedDate.toString());
        target.setData(JSONSerialization.serializeObjectAsJsonString(
                versionJson));
    }

}
