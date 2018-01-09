/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.dao.AccessPointDAO;
import au.org.ands.vocabs.registry.db.entity.AccessPoint;
import au.org.ands.vocabs.registry.db.entity.VersionArtefact;
import au.org.ands.vocabs.registry.db.internal.ApApiSparql;
import au.org.ands.vocabs.registry.db.internal.ApFile;
import au.org.ands.vocabs.registry.db.internal.ApSissvoc;
import au.org.ands.vocabs.registry.db.internal.ApWebPage;
import au.org.ands.vocabs.registry.enums.ApSource;
import au.org.ands.vocabs.registry.enums.SubtaskOperationType;
import au.org.ands.vocabs.registry.enums.SubtaskProviderType;
import au.org.ands.vocabs.registry.workflow.provider.importer.SesameImporterProvider;
import au.org.ands.vocabs.registry.workflow.provider.publish.SISSVocPublishProvider;
import au.org.ands.vocabs.registry.workflow.provider.transform.JsonListTransformProvider;
import au.org.ands.vocabs.registry.workflow.provider.transform.JsonTreeTransformProvider;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;

/** The interface provided by the workflow package. API and methods
 * in other parts of the registry should invoke the methods of this class,
 * rather than directly invoking the methods of the workflow classes
 * in this package.
 */
public final class WorkflowMethods {

    /** Logger for this class. */
    private static Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Private constructor for a utility class. */
    private WorkflowMethods() {
    }

    // TO DO: add method to run a task.

    /** Apply workflow insertion to an access point specified in registry
     * schema format.
     * It may be that the access point can be created immediately as
     * a database entity. In this case, the AccessPoint component of
     * the returned Pair is a newly-created, and persisted AccessPoint object.
     * If the access point requires workflow processing, the right-hand
     * component of the returned Pair is a list of workflow subtasks
     * that must be performed.
     * @param em The EntityManager to use to persist any newly-created
     *      AccessPoint entity.
     * @param versionId The version Id.
     * @param isDraft True, if this a request for insertion of a draft
     *      instance.
     * @param modifiedBy The value to use for "modifiedBy" when adding
     *      rows of the database.
     * @param nowTime The date/time being used for this insertion.
     * @param schemaAP The registry schema description of the access point
     *      to be added.
     * @return A Pair of values. The left element is a non-null AccessPoint,
     *      if this is should be persisted directly. The right element
     *      is a non-empty list of subtasks, if workflow processing
     *      is required.
     */
    public static Pair<AccessPoint, List<Subtask>> insertAccessPoint(
            final EntityManager em,
            final Integer versionId,
            final boolean isDraft,
            final String modifiedBy,
            final LocalDateTime nowTime,
            final au.org.ands.vocabs.registry.schema.vocabulary201701.
            AccessPoint schemaAP) {
        if (schemaAP.getSource() == ApSource.SYSTEM) {
            // Not needed/supported.
            return Pair.of(null, null);
        }
        AccessPoint ap;
        switch (schemaAP.getDiscriminator()) {
        case API_SPARQL:
            ap = new AccessPoint();
            ap.setVersionId(versionId);
            ap.setSource(ApSource.USER);
            ApApiSparql apApiSparql = new ApApiSparql();
            apApiSparql.setUrl(schemaAP.getApApiSparql().getUrl());
            if (isDraft) {
                TemporalUtils.makeDraft(ap);
                apApiSparql.setDraftCreatedDate(nowTime.toString());
                apApiSparql.setDraftModifiedDate(nowTime.toString());
            } else {
                TemporalUtils.makeCurrentlyValid(ap, nowTime);
            }
            ap.setData(JSONSerialization.serializeObjectAsJsonString(
                    apApiSparql));
            ap.setModifiedBy(modifiedBy);
            AccessPointDAO.saveAccessPoint(em, ap);
            return Pair.of(ap, null);
        case FILE:
            // In this case, there is something to be done apart from adding
            // to the database.
            ap = new AccessPoint();
            ap.setVersionId(versionId);
            ap.setSource(ApSource.USER);
            ApFile apFile = new ApFile();
            apFile.setFormat(schemaAP.getApFile().getFormat());
            apFile.setUploadId(schemaAP.getApFile().getUploadId());
            if (isDraft) {
                TemporalUtils.makeDraft(ap);
                apFile.setDraftCreatedDate(nowTime.toString());
                apFile.setDraftModifiedDate(nowTime.toString());
                ap.setData(JSONSerialization.serializeObjectAsJsonString(
                        apFile));
                ap.setModifiedBy(modifiedBy);
                AccessPointDAO.saveAccessPoint(em, ap);
            } else {
                TemporalUtils.makeCurrentlyValid(ap, nowTime);
                // We make the access point visible, by "harvesting"
                // the upload.
                // Temporarily set the data to an empty value, so that
                // the entity can be persisted.
                ap.setData("{}");
                ap.setModifiedBy(modifiedBy);
                AccessPointDAO.saveAccessPoint(em, ap);

                // TO DO: copy/"harvest" the previously-uploaded file.
//                FileUtils.copyFileToDirectory(srcFile, destDir);
//                String url = "";

                apFile.setPath("TODO");
                apFile.setUrl("TODO");
                ap.setData(JSONSerialization.serializeObjectAsJsonString(
                        apFile));
                AccessPointDAO.updateAccessPoint(em, ap);
            }
            return Pair.of(ap, null);
        case SESAME_DOWNLOAD:
            // Nuh, you can't do this ... yet.
            logger.error("Attempt to add sesameDownload with source=USER");
            return Pair.of(null, null);
        case SISSVOC:
            ap = new AccessPoint();
            ap.setVersionId(versionId);
            ap.setSource(ApSource.USER);
            ApSissvoc apSissvoc = new ApSissvoc();
            apSissvoc.setUrlPrefix(schemaAP.getApSissvoc().getUrlPrefix());
            if (isDraft) {
                TemporalUtils.makeDraft(ap);
                apSissvoc.setDraftCreatedDate(nowTime.toString());
                apSissvoc.setDraftModifiedDate(nowTime.toString());
            } else {
                TemporalUtils.makeCurrentlyValid(ap, nowTime);
            }
            ap.setData(JSONSerialization.serializeObjectAsJsonString(
                    apSissvoc));
            ap.setModifiedBy(modifiedBy);
            AccessPointDAO.saveAccessPoint(em, ap);
            return Pair.of(ap, null);
        case WEB_PAGE:
            ap = new AccessPoint();
            ap.setVersionId(versionId);
            ap.setSource(ApSource.USER);
            ApWebPage apWebPage = new ApWebPage();
            apWebPage.setUrl(schemaAP.getApWebPage().getUrl());
            if (isDraft) {
                TemporalUtils.makeDraft(ap);
                apWebPage.setDraftCreatedDate(nowTime.toString());
                apWebPage.setDraftModifiedDate(nowTime.toString());
            } else {
                TemporalUtils.makeCurrentlyValid(ap, nowTime);
            }
            ap.setData(JSONSerialization.serializeObjectAsJsonString(
                    apWebPage));
            ap.setModifiedBy(modifiedBy);
            AccessPointDAO.saveAccessPoint(em, ap);
            return Pair.of(ap, null);
        default:
            break;
        }
        return null;
    }

    /** Apply workflow deletion to an access point.
     * One of two situations applies: either, the access point can be, and is,
     * deleted immediately, or, a list of workflow subtasks must be performed.
     * NB: in either case, this method does <i>not</i> delete (or mark as
     * historical) the database row for the access point.
     * @param ap The access point to be deleted.
     * @return null, if the deletion has been completed, or a non-empty
     *      list of required workflow subtasks that need to be performed.
     */
    public static List<Subtask> deleteAccessPoint(final AccessPoint ap) {
        List<Subtask> subtaskList = new ArrayList<>();
        Subtask subtask = new Subtask();
        subtaskList.add(subtask);
        switch (ap.getType()) {
        case API_SPARQL:
            if (ap.getSource() == ApSource.USER) {
                // No further action required.
                return null;
            }
            // Handle source=SYSTEM.
            // Note well: we rely on the fact that SesameImporterProvider
            // is responsible for _both_ the API_SPARQL and SESAME_DOWNLOAD
            // access points, and where there is one, there will also be
            // the other. We choose to do something for API_SPARQL and
            // "ignore" the corresponding SESAME_DOWNLOAD.
            subtask.setSubtaskProviderType(SubtaskProviderType.IMPORTER);
            subtask.setProvider(SesameImporterProvider.class);
            subtask.setOperation(SubtaskOperationType.DELETE);
            subtask.determinePriority();
            break;
        case FILE:
            ApFile apFile = JSONSerialization.deserializeStringAsJson(
                    ap.getData(), ApFile.class);
            try {
                Files.delete(Paths.get(apFile.getPath()));
            } catch (IOException e) {
                logger.error("Error deleting file: " + apFile.getPath(), e);
            }
            // No subtask required.
            return null;
        case SESAME_DOWNLOAD:
            // Note well: we rely on the fact that SesameImporterProvider
            // is responsible for _both_ the API_SPARQL and SESAME_DOWNLOAD
            // access points, and where there is one, there will also be
            // the other. So we do nothing here.
            // And since there's nothing to be done if source=USER,
            // we do nothing for that case either. Below is the sort of
            // code that would be required if this assumption should change.
            /*
            if (ap.getSource() == ApSource.USER) {
                // No further action required.
                return null;
            }
            subtask.setSubtaskProviderType(SubtaskProviderType.IMPORTER);
            subtask.setProvider(SesameImporterProvider.class);
            subtask.setOperation(SubtaskOperationType.DELETE);
            subtask.determinePriority();
            */
            break;
        case SISSVOC:
            if (ap.getSource() == ApSource.USER) {
                // No further action required.
                return null;
            }
            // Handle source=SYSTEM.
            subtask.setSubtaskProviderType(SubtaskProviderType.PUBLISH);
            subtask.setProvider(SISSVocPublishProvider.class);
            subtask.setOperation(SubtaskOperationType.DELETE);
            subtask.determinePriority();
            break;
        case WEB_PAGE:
            // These are all source=USER, and no further action is required.
            return null;
        default:
            // Oops, a type we don't know about.
            throw new IllegalArgumentException(
                    "Unknown access point type: " + ap.getType());
        }
        return subtaskList;
    }

    /** Apply workflow deletion to a version artefact.
     * One of two situations applies: either, the version artefact can be,
     * and is, deleted immediately, or, a list of workflow subtasks must
     * be performed.
     * NB: in either case, this method does <i>not</i> delete (or mark
     * as historical) the database row for the version artefact.
     * @param va The version artefact to be deleted.
     * @return null, if the deletion has been completed, or a non-empty
     *      list of required workflow subtasks that need to be performed.
     */
    public static List<Subtask> deleteVersionArtefact(
            final VersionArtefact va) {
        List<Subtask> subtaskList = new ArrayList<>();
        Subtask subtask = new Subtask();
        subtaskList.add(subtask);
        switch (va.getType()) {
        case CONCEPT_LIST:
            subtask.setSubtaskProviderType(SubtaskProviderType.TRANSFORM);
            subtask.setProvider(JsonListTransformProvider.class);
            subtask.setOperation(SubtaskOperationType.DELETE);
            subtask.determinePriority();
            break;
        case CONCEPT_TREE:
            subtask.setSubtaskProviderType(SubtaskProviderType.TRANSFORM);
            subtask.setProvider(JsonTreeTransformProvider.class);
            subtask.setOperation(SubtaskOperationType.DELETE);
            subtask.determinePriority();
            break;
        default:
            // Oops, a type we don't know about.
            throw new IllegalArgumentException(
                    "Unknown version artefact type: " + va.getType());
        }
        return subtaskList;
    }

}
