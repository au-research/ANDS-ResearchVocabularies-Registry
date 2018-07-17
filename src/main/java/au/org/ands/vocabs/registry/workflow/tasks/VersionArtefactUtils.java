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

    /** Create a database entity for a system-generated
     * version artefact for a version.
     * Don't duplicate it, if it already exists.
     * @param <T> The class of the version artefact, as a subclass of VaCommon.
     * @param taskInfo The TaskInfo providing the context for
     *      the creation of the version artefact.
     * @param vaClass The class instance of the version artefact type.
     * @param vaType The version artefact type.
     * @param multipleOfTheSameType Specify true, if there can be more
     *      than one instance of this version artefact type, for the
     *      same version.
     * @param comparePredicate Predicate used to compare against an existing
     *      version artefact of the same type.
     * @param fieldSetter Consumer used to set type-specific field(s) of a
     *      new version artefact.
     */
    private static <T extends VaCommon> void createVersionArtefact(
            final TaskInfo taskInfo,
            final Class<T> vaClass,
            final VersionArtefactType vaType,
            final boolean multipleOfTheSameType,
            final Predicate<T> comparePredicate,
            final Consumer<T> fieldSetter) {

        EntityManager em = taskInfo.getEm();
        String modifiedBy = taskInfo.getModifiedBy();
        LocalDateTime nowTime = taskInfo.getNowTime();
        Version version = taskInfo.getVersion();

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
            // different path. Retire this one, if there can only be one.
            if (!multipleOfTheSameType) {
                va.setModifiedBy(modifiedBy);
                TemporalUtils.makeHistorical(va, nowTime);
                VersionArtefactDAO.updateVersionArtefact(em, va);
            }
        }
        // No existing access point with the correct path,
        // so create a new one.
        VersionArtefact va = new VersionArtefact();
        TemporalUtils.makeCurrentlyValid(va, nowTime);
        va.setVersionId(version.getVersionId());
        va.setModifiedBy(modifiedBy);
        va.setStatus(VersionArtefactStatus.CURRENT);
        va.setType(vaType);

        T vaT;
        try {
            vaT = vaClass.newInstance();
            // errorprone says this would be better:
//            vaT = vaClass.getConstructor().newInstance();
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
     * @param taskInfo The TaskInfo providing the context for
     *      the creation of the version artefact.
     * @param path The path to put into the database entity.
     */
    public static void createConceptListVersionArtefact(
            final TaskInfo taskInfo,
            final String path) {
        createVersionArtefact(taskInfo,
                VaConceptList.class,
                VersionArtefactType.CONCEPT_LIST,
                false,
                vaT -> path.equals(vaT.getPath()),
                vaT -> vaT.setPath(path));
    }

    /** Create a database entity for a system-generated concept tree
     * version artefact for a version.
     * Don't duplicate it, if it already exists.
     * @param taskInfo The TaskInfo providing the context for
     *      the creation of the version artefact.
     * @param path The path to put into the database entity.
     */
    public static void createConceptTreeVersionArtefact(
            final TaskInfo taskInfo,
            final String path) {
        createVersionArtefact(taskInfo,
                VaConceptTree.class,
                VersionArtefactType.CONCEPT_TREE,
                false,
                vaT -> path.equals(vaT.getPath()),
                vaT -> vaT.setPath(path));
    }

    /** Create a database entity for a system-generated PoolParty harvest
     * version artefact for a version.
     * Don't duplicate it, if it already exists.
     * @param taskInfo The TaskInfo providing the context for
     *      the creation of the version artefact.
     * @param path The path to put into the database entity.
     */
    public static void createPoolpartyHarvestVersionArtefact(
            final TaskInfo taskInfo,
            final String path) {
        createVersionArtefact(taskInfo,
                VaHarvestPoolparty.class,
                VersionArtefactType.HARVEST_POOLPARTY,
                true,
                vaT -> path.equals(vaT.getPath()),
                vaT -> vaT.setPath(path));
    }

}
