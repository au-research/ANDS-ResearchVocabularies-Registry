/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.tasks;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.dao.AccessPointDAO;
import au.org.ands.vocabs.registry.db.entity.AccessPoint;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.internal.ApApiSparql;
import au.org.ands.vocabs.registry.db.internal.ApCommon;
import au.org.ands.vocabs.registry.db.internal.ApSesameDownload;
import au.org.ands.vocabs.registry.db.internal.ApSissvoc;
import au.org.ands.vocabs.registry.enums.AccessPointType;
import au.org.ands.vocabs.registry.enums.ApSource;
import au.org.ands.vocabs.registry.utils.PropertyConstants;
import au.org.ands.vocabs.registry.utils.RegistryProperties;

/** Utilities for working with access points. */
public final class AccessPointUtils {

    /** Private constructor for a utility class. */
    private AccessPointUtils() {
    }

    /** Logger for this class. */
    private static Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** URL that is a prefix to download endpoints. */
    private static String downloadPrefixProperty =
            RegistryProperties.getProperty(
                    PropertyConstants.REGISTRY_DOWNLOADPREFIX);

    /** Compute the URL that gives access to a file access point.
     * @param id The access point Id of the access point.
     * @param baseFilename The base filename of the file.
     * @return The URL of the file access point.
     */
    public static String getDownloadUrlForFileAccessPoint(final Integer id,
            final String baseFilename) {
        return downloadPrefixProperty + id + "/" + baseFilename;
    }

    /** Create a database entity for a system-generated access point
     * for a version. Don't duplicate it, if it already exists.
     * @param <T> The class of the access point, as a subclass of ApCommon.
     * @param taskInfo The TaskInfo providing the context for
     *      the creation of the access point.
     * @param apClass The class instance of the access point type.
     * @param apType The access point type.
     * @param comparePredicate Predicate used to compare against an existing
     *      access point of the same type.
     * @param fieldSetter Consumer used to set type-specific field(s) of a
     *      new access point. NB: This consumer is called <i>after</i> the
     *      new access point has been persisted with a dummy setting
     *      for the data field; this means that an access point Id is
     *      available.
     */
    private static <T extends ApCommon> void createAccessPoint(
            final TaskInfo taskInfo,
            final Class<T> apClass,
            final AccessPointType apType,
            final Predicate<T> comparePredicate,
            final BiConsumer<AccessPoint, T> fieldSetter) {

        EntityManager em = taskInfo.getEm();
        String modifiedBy = taskInfo.getModifiedBy();
        LocalDateTime nowTime = taskInfo.getNowTime();
        Version version = taskInfo.getVersion();

        Integer versionId = version.getVersionId();
        List<AccessPoint> aps =
                AccessPointDAO.getCurrentAccessPointListForVersionByType(
                        versionId, apType, em);
        for (AccessPoint ap : aps) {
            if (ap.getSource() == ApSource.USER) {
                // We don't touch user-specified SISSVoc access points.
                continue;
            }
            T apT = JSONSerialization.deserializeStringAsJson(
                    ap.getData(), apClass);
            if (comparePredicate.test(apT)) {
                // Already exists, and good to go.
                return;
            }
            // So we've got a currently-valid system-generated one with a
            // different urlPrefix. Retire this one.
            ap.setModifiedBy(modifiedBy);
            TemporalUtils.makeHistorical(ap, nowTime);
            AccessPointDAO.updateAccessPoint(em, ap);
        }
        // No existing access point with the correct urlPrefix,
        // so create a new one.
        AccessPoint ap = new AccessPoint();
        TemporalUtils.makeCurrentlyValid(ap, nowTime);
        ap.setVersionId(version.getVersionId());
        ap.setModifiedBy(modifiedBy);
        ap.setType(apType);
        ap.setSource(ApSource.SYSTEM);

        T apT;
        try {
            apT = apClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error("Error creating instance of class: " + apClass, e);
            return;
        }
        // Dummy data setting.
        ap.setData("{}");
        // Persist for the first time.
        AccessPointDAO.saveAccessPointWithId(em, ap);
        // Now the access point has an Id, which can be accessed by
        // the consumer.
        if (fieldSetter != null) {
            fieldSetter.accept(ap, apT);
        }
        ap.setData(JSONSerialization.serializeObjectAsJsonString(apT));

        AccessPointDAO.updateAccessPoint(em, ap);
    }

    /** Create a database entity for a system-generated SPARQL access point
     * for a version. Don't duplicate it, if it already exists.
     * @param taskInfo The TaskInfo providing the context for
     *      the creation of the access point.
     * @param url The URL to put into the database entity.
     */
    public static void createApiSparqlAccessPoint(
            final TaskInfo taskInfo,
            final String url) {
        createAccessPoint(taskInfo,
                ApApiSparql.class, AccessPointType.API_SPARQL,
                apT -> url.equals(apT.getUrl()),
                (ap, apT) -> apT.setUrl(url));
    }

    /** Create a database entity for a system-generated Sesame download
     * access point for a version. Don't duplicate it, if it already exists.
     * @param taskInfo The TaskInfo providing the context for
     *      the creation of the access point.
     * @param repository The repository Id to put into the database entity.
     * @param serverBase The server base to put into the database entity.
     */
    public static void createSesameDownloadAccessPoint(
            final TaskInfo taskInfo,
            final String repository,
            final String serverBase) {
        createAccessPoint(taskInfo,
                ApSesameDownload.class, AccessPointType.SESAME_DOWNLOAD,
                apT -> repository.equals(apT.getRepository()),
                (ap, apT) -> {
                    apT.setRepository(repository);
                    apT.setServerBase(serverBase);
                    apT.setUrlPrefix(getDownloadUrlForFileAccessPoint(
                            ap.getAccessPointId(), repository + "."));
                });
    }

    /** Create a database entity for a system-generated SISSVoc access point
     * for a version. Don't duplicate it, if it already exists.
     * @param taskInfo The TaskInfo providing the context for
     *      the creation of the access point.
     * @param urlPrefix The urlPrefix to put into the database entity.
     */
    public static void createSissvocAccessPoint(
            final TaskInfo taskInfo,
            final String urlPrefix) {
        createAccessPoint(taskInfo,
                ApSissvoc.class, AccessPointType.SISSVOC,
                apT -> urlPrefix.equals(apT.getUrlPrefix()),
                (ap, apT) -> apT.setUrlPrefix(urlPrefix));
    }

}
