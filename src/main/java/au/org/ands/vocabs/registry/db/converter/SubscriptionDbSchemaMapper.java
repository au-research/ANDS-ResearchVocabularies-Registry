/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.converter;

import javax.persistence.EntityManager;

import org.mapstruct.Context;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/** MapStruct mapper from Subscription database to schema. */
@Mapper
@DecoratedWith(SubscriptionDbSchemaMapperDecorator.class)
public interface SubscriptionDbSchemaMapper {

    /** Singleton instance of this mapper. */
    SubscriptionDbSchemaMapper INSTANCE =
            Mappers.getMapper(SubscriptionDbSchemaMapper.class);

    /** MapStruct-generated Mapper from Subscription database to schema.
     * @param source The Subscription entity from the database.
     * @param em The EntityManager to use to fetch any additional needed
     *      content from the database.
     * @return The schema version of the subscription.
     */
    @Mapping(source = "source.notificationElementType", target = "elementType")
    // We map elementType, but not elementId: _whether_ that is filled in
    // depends on elementType.
    @Mapping(target = "elementId", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    au.org.ands.vocabs.registry.schema.vocabulary201701.Subscription
    sourceToTarget(au.org.ands.vocabs.registry.db.entity.Subscription source,
              @Context EntityManager em);

}
