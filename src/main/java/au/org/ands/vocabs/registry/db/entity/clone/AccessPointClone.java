/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.entity.clone;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import au.org.ands.vocabs.registry.db.entity.AccessPoint;
import au.org.ands.vocabs.registry.db.internal.ApApiSparql;
import au.org.ands.vocabs.registry.db.internal.ApFile;
import au.org.ands.vocabs.registry.db.internal.ApSesameDownload;
import au.org.ands.vocabs.registry.db.internal.ApSissvoc;
import au.org.ands.vocabs.registry.db.internal.ApWebPage;

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

    /** MapStruct-generated Mapper that clones ApApiSparql registry database
     * entities.
     * @param source The ApApiSparql database entity to be cloned.
     * @return A clone of the ApApiSparql database entity.
     */
    ApApiSparql clone(ApApiSparql source);

    /** MapStruct-generated Mapper that clones ApFile registry database
     * entities.
     * @param source The ApFile database entity to be cloned.
     * @return A clone of the ApFile database entity.
     */
    ApFile clone(ApFile source);

    /** MapStruct-generated Mapper that clones ApSesameDownload
     * registry database entities.
     * @param source The ApSesameDownload database entity to be cloned.
     * @return A clone of the ApSesameDownload database entity.
     */
    ApSesameDownload clone(ApSesameDownload source);

    /** MapStruct-generated Mapper that clones ApSissvoc registry database
     * entities.
     * @param source The ApSissvoc database entity to be cloned.
     * @return A clone of the ApSissvoc database entity.
     */
    ApSissvoc clone(ApSissvoc source);

    /** MapStruct-generated Mapper that clones ApWebPage registry database
     * entities.
     * @param source The ApWebPage database entity to be cloned.
     * @return A clone of the ApWebPage database entity.
     */
    ApWebPage clone(ApWebPage source);

}
