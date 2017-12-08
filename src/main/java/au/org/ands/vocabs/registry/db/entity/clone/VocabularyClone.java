/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.entity.clone;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import au.org.ands.vocabs.registry.db.entity.Vocabulary;

/** MapStruct mapper for cloning Vocabulary database entities. */
@Mapper
public interface VocabularyClone {

    /** Singleton instance of this mapper. */
    VocabularyClone INSTANCE = Mappers.getMapper(VocabularyClone.class);

    /** MapStruct-generated Mapper that clones Vocabulary registry database
     * entities.
     * @param source The Vocabulary database entity to be cloned.
     * @return A clone of the vocabulary database entity, except that
     *      it does not have the Id property set.
     */
    @Mapping(target = "id", ignore = true)
    Vocabulary clone(Vocabulary source);

}
