/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.entity.clone;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import au.org.ands.vocabs.registry.db.entity.Version;

/** MapStruct mapper for cloning Version database entities. */
@Mapper
public interface VersionClone {

    /** Singleton instance of this mapper. */
    VersionClone INSTANCE = Mappers.getMapper(VersionClone.class);

    /** MapStruct-generated Mapper that clones Version registry database
     * entities.
     * @param source The Version database entity to be cloned.
     * @return A clone of the version database entity, except that
     *      it does not have the Id property set.
     */
    @Mapping(target = "id", ignore = true)
    Version clone(Version source);

}
