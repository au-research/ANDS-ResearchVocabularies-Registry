/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.converter;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import au.org.ands.vocabs.registry.db.dao.VocabularyRelatedEntityDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyRelatedVocabularyDAO;
import au.org.ands.vocabs.registry.db.entity.VocabularyRelatedEntity;
import au.org.ands.vocabs.registry.db.entity.VocabularyRelatedVocabulary;
import au.org.ands.vocabs.registry.db.internal.VocabularyJson;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary.RelatedEntityRef;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary.RelatedVocabularyRef;

/** MapStruct mapper from Vocabulary database to schema. */
public abstract class VocabularyDbSchemaMapperDecorator
    implements VocabularyDbSchemaMapper {

    /** The delegate mapper. */
    private final VocabularyDbSchemaMapper delegate;

    /** Constructor that accepts a delegate.
     * @param aDelegate The delegate mapper.
     */
    public VocabularyDbSchemaMapperDecorator(
            final VocabularyDbSchemaMapper aDelegate) {
        delegate = aDelegate;
    }

    /** Decorator method that extends the default mapping behaviour
     * with extraction of the JSON data.
     * (Don't make this method final; it is extended by the implementation
     * class.)
     */
    @Override
    @SuppressWarnings("checkstyle:DesignForExtension")
    public au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
    sourceToTarget(final au.org.ands.vocabs.registry.db.entity.Vocabulary
            source) {
        if (source == null) {
            return null;
        }

        // Basic fields, handled for us by MapStruct.
        au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
            target = delegate.sourceToTarget(source);

        // Related entity refs.
        MultivaluedMap<Integer, VocabularyRelatedEntity> vreMap =
                VocabularyRelatedEntityDAO.
                getCurrentVocabularyRelatedEntitiesForVocabulary(
                        source.getVocabularyId());
        List<RelatedEntityRef> rerList = target.getRelatedEntityRef();
        VocabularyRelatedEntityDbSchemaMapper vredbMapper =
                VocabularyRelatedEntityDbSchemaMapper.INSTANCE;
        for (Map.Entry<Integer, List<VocabularyRelatedEntity>>
            vreMapElement : vreMap.entrySet()) {
            RelatedEntityRef reRef = null;
            for (VocabularyRelatedEntity vre : vreMapElement.getValue()) {
                if (reRef == null) {
                    reRef = vredbMapper.sourceToTarget(vre);
                }
                reRef.getRelation().add(vre.getRelation());
            }
            rerList.add(reRef);
        }

        // Related vocabulary refs.
        List<VocabularyRelatedVocabulary> vrvList =
                VocabularyRelatedVocabularyDAO.
                getCurrentVocabularyRelatedVocabulariesForVocabulary(
                        source.getVocabularyId());
        List<RelatedVocabularyRef> rvrList = target.getRelatedVocabularyRef();
        VocabularyRelatedVocabularyDbSchemaMapper vrvdbMapper =
                VocabularyRelatedVocabularyDbSchemaMapper.INSTANCE;
        for (VocabularyRelatedVocabulary vrv : vrvList) {
            rvrList.add(vrvdbMapper.sourceToTarget(vrv));
        }

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
