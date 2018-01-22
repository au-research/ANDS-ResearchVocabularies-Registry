/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.tasks;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.dao.VersionArtefactDAO;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.VersionArtefact;
import au.org.ands.vocabs.registry.db.internal.VaCommon;
import au.org.ands.vocabs.registry.db.internal.VaConceptList;
import au.org.ands.vocabs.registry.db.internal.VaConceptTree;
import au.org.ands.vocabs.registry.db.internal.VaHarvestPoolparty;
import au.org.ands.vocabs.registry.enums.VersionArtefactStatus;
import au.org.ands.vocabs.registry.enums.VersionArtefactType;

/** Utilities for working with version artefacts. */
public final class VersionArtefactUtils {

    /** Private constructor for a utility class. */
    private VersionArtefactUtils() {
    }

    /** Logger for this class. */
    private static Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Create a database entity for a system-generated PoolParty harvest
     * version artefact for a version.
     * Don't duplicate it, if it already exists.
     * @param <T> The class of the version artefact, as a subclass of VaCommon.
     * @param em The EntityManager to use.
     * @param modifiedBy The value to use for "modifiedBy" when adding/updating
     *      rows of the database.
     * @param nowTime The date/time being used for this operation.
     * @param version The version for which the access point is to be created.
     * @param vaClass The class instance of the version artefact type.
     * @param vaType The version artefact type.
     * @param comparePredicate Predicate used to compare against an existing
     *      version artefact of the same type.
     * @param fieldSetter Consumer used to set type-specific field(s) of a
     *      new version artefact.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    private static <T extends VaCommon> void createVersionArtefact(
            final EntityManager em,
            final String modifiedBy,
            final LocalDateTime nowTime,
            final Version version,
            final Class<T> vaClass,
            final VersionArtefactType vaType,
            final Predicate<T> comparePredicate,
            final Consumer<T> fieldSetter) {
        Integer versionId = version.getVersionId();
        List<VersionArtefact> vas = VersionArtefactDAO.
                getCurrentVersionArtefactListForVersionByType(
                        versionId, vaType, em);
        for (VersionArtefact va : vas) {
            T vaT = JSONSerialization.deserializeStringAsJson(
                    va.getData(), vaClass);
            if (comparePredicate.test(vaT)) {
                // Already exists, and good to go.
                return;
            }
            // So we've got a currently-valid one with a
            // different path. Retire this one.
            va.setModifiedBy(modifiedBy);
            TemporalUtils.makeHistorical(va, nowTime);
            VersionArtefactDAO.updateVersionArtefact(em, va);
        }
        // No existing access point with the correct path,
        // so create a new one.
        VersionArtefact va = new VersionArtefact();
        TemporalUtils.makeCurrentlyValid(va, nowTime);
        va.setVersionId(version.getId());
        va.setModifiedBy(modifiedBy);
        va.setStatus(VersionArtefactStatus.CURRENT);
        va.setType(vaType);

        T vaT;
        try {
            vaT = vaClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error("Error creating instance of class: " + vaClass, e);
            return;
        }
        fieldSetter.accept(vaT);
        va.setData(JSONSerialization.serializeObjectAsJsonString(
                vaT));

        VersionArtefactDAO.saveVersionArtefactWithId(em, va);
    }

    /** Create a database entity for a system-generated concept list
     * version artefact for a version.
     * Don't duplicate it, if it already exists.
     * @param em The EntityManager to use.
     * @param modifiedBy The value to use for "modifiedBy" when adding/updating
     *      rows of the database.
     * @param nowTime The date/time being used for this operation.
     * @param version The version for which the access point is to be created.
     * @param path The path to put into the database entity.
     */
    public static void createConceptListVersionArtefact(
            final EntityManager em,
            final String modifiedBy,
            final LocalDateTime nowTime,
            final Version version,
            final String path) {
        createVersionArtefact(em, modifiedBy, nowTime, version,
                VaConceptList.class,
                VersionArtefactType.CONCEPT_LIST,
                vaT -> path.equals(vaT.getPath()),
                vaT -> vaT.setPath(path));
    }

    /** Create a database entity for a system-generated concept tree
     * version artefact for a version.
     * Don't duplicate it, if it already exists.
     * @param em The EntityManager to use.
     * @param modifiedBy The value to use for "modifiedBy" when adding/updating
     *      rows of the database.
     * @param nowTime The date/time being used for this operation.
     * @param version The version for which the access point is to be created.
     * @param path The path to put into the database entity.
     */
    public static void createConceptTreeVersionArtefact(
            final EntityManager em,
            final String modifiedBy,
            final LocalDateTime nowTime,
            final Version version,
            final String path) {
        createVersionArtefact(em, modifiedBy, nowTime, version,
                VaConceptTree.class,
                VersionArtefactType.CONCEPT_TREE,
                vaT -> path.equals(vaT.getPath()),
                vaT -> vaT.setPath(path));
    }

    /** Create a database entity for a system-generated PoolParty harvest
     * version artefact for a version.
     * Don't duplicate it, if it already exists.
     * @param em The EntityManager to use.
     * @param modifiedBy The value to use for "modifiedBy" when adding/updating
     *      rows of the database.
     * @param nowTime The date/time being used for this operation.
     * @param version The version for which the access point is to be created.
     * @param path The path to put into the database entity.
     */
    public static void createPoolpartyHarvestVersionArtefact(
            final EntityManager em,
            final String modifiedBy,
            final LocalDateTime nowTime,
            final Version version,
            final String path) {
        createVersionArtefact(em, modifiedBy, nowTime, version,
                VaHarvestPoolparty.class,
                VersionArtefactType.HARVEST_POOLPARTY,
                vaT -> path.equals(vaT.getPath()),
                vaT -> vaT.setPath(path));
    }

}
