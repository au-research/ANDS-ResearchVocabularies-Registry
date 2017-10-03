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

    /* NB: See the note in the interface about moving to MapStruct 1.2.
     * When we switch to MapStruct 1.2, remove the following two methods
     * and rename the third method to match "the" name of the united
     * mapper. */

    /** Decorator method that extends the default mapping behaviour
     * with extraction of the JSON data.
     * Include related entity references and related vocabulary
     * references.
     * (Don't make this method final; it is extended by the implementation
     * class.)
     */
    @Override
    @SuppressWarnings("checkstyle:DesignForExtension")
    public au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
    sourceToTarget(final au.org.ands.vocabs.registry.db.entity.Vocabulary
            source) {
        return sourceToTargetHelper(source, true);
    }

    /** Decorator method that extends the default mapping behaviour
     * with extraction of the JSON data.
     * Do not include related entity references and related vocabulary
     * references.
     * (Don't make this method final; it is extended by the implementation
     * class.)
     */
    @Override
    @SuppressWarnings("checkstyle:DesignForExtension")
    public au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
    sourceToTargetWithoutRelated(
            final au.org.ands.vocabs.registry.db.entity.Vocabulary
            source) {
        return sourceToTargetHelper(source, false);
    }

    /** Helper method that implements the requested mapping behaviour.
     * JSON data is always extracted; related entity references and
     * related vocabulary references are inserted if and only if requested.
     * @param source The database entity to be mapped.
     * @param insertRelated true, if related entity references and
     *      related vocabulary references are to be inserted in the result.
     * @return The result of the mapping.
     */
    @SuppressWarnings("checkstyle:DesignForExtension")
    public au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
    sourceToTargetHelper(final au.org.ands.vocabs.registry.db.entity.Vocabulary
            source, final boolean insertRelated) {
        if (source == null) {
            return null;
        }

        // Basic fields, handled for us by MapStruct.
        au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary
            target = delegate.sourceToTarget(source);

        if (insertRelated) {
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
            MultivaluedMap<Integer, VocabularyRelatedVocabulary> vrvMap =
                    VocabularyRelatedVocabularyDAO.
                    getCurrentVocabularyRelatedVocabulariesForVocabulary(
                            source.getVocabularyId());
            List<RelatedVocabularyRef> rvrList =
                    target.getRelatedVocabularyRef();
            VocabularyRelatedVocabularyDbSchemaMapper vrvdbMapper =
                    VocabularyRelatedVocabularyDbSchemaMapper.INSTANCE;
            for (Map.Entry<Integer, List<VocabularyRelatedVocabulary>>
                vrvMapElement : vrvMap.entrySet()) {
                RelatedVocabularyRef rvRef = null;
                for (VocabularyRelatedVocabulary vrv
                        : vrvMapElement.getValue()) {
                    if (rvRef == null) {
                        rvRef = vrvdbMapper.sourceToTarget(vrv);
                    }
                    rvRef.getRelation().add(vrv.getRelation());
                }
                rvrList.add(rvRef);
            }
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
