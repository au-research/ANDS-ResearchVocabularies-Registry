/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.entity.clone;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import au.org.ands.vocabs.registry.db.entity.AccessPoint;

/** MapStruct mapper for cloning AccessPoint database entities. */
@Mapper
public interface AccessPointClone {

    /** Singleton instance of this mapper. */
    AccessPointClone INSTANCE = Mappers.getMapper(AccessPointClone.class);

    /** MapStruct-generated Mapper that clones AccessPoint registry database
     * entities.
     * @param source The AccessPoint database entity to be cloned.
     * @return A clone of the access point database entity, except that
     *      it does not have the Id property set.
     */
    @Mapping(target = "id", ignore = true)
    AccessPoint clone(AccessPoint source);

}
