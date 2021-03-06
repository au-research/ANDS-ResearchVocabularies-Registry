/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
import au.org.ands.vocabs.registry.db.dao.UploadDAO;
import au.org.ands.vocabs.registry.db.entity.AccessPoint;
import au.org.ands.vocabs.registry.db.entity.Upload;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.VersionArtefact;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.db.internal.ApApiSparql;
import au.org.ands.vocabs.registry.db.internal.ApFile;
import au.org.ands.vocabs.registry.db.internal.ApSissvoc;
import au.org.ands.vocabs.registry.db.internal.ApWebPage;
import au.org.ands.vocabs.registry.db.internal.VocabularyJson;
import au.org.ands.vocabs.registry.db.internal.VocabularyJson.PoolpartyProject;
import au.org.ands.vocabs.registry.enums.ApSource;
import au.org.ands.vocabs.registry.enums.SubtaskOperationType;
import au.org.ands.vocabs.registry.enums.SubtaskProviderType;
import au.org.ands.vocabs.registry.utils.RegistryFileUtils;
import au.org.ands.vocabs.registry.utils.fileformat.FileFormatUtils;
import au.org.ands.vocabs.registry.workflow.provider.harvest.PoolPartyHarvestProvider;
import au.org.ands.vocabs.registry.workflow.provider.importer.SesameImporterProvider;
import au.org.ands.vocabs.registry.workflow.provider.publish.SISSVocPublishProvider;
import au.org.ands.vocabs.registry.workflow.provider.transform.ConceptTreeTransformProvider;
import au.org.ands.vocabs.registry.workflow.provider.transform.JsonListTransformProvider;
import au.org.ands.vocabs.registry.workflow.provider.transform.ResourceDocsTransformProvider;
import au.org.ands.vocabs.registry.workflow.provider.transform.SesameInsertMetadataTransformProvider;
import au.org.ands.vocabs.registry.workflow.tasks.AccessPointUtils;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;
import au.org.ands.vocabs.registry.workflow.tasks.TaskUtils;

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
     * @param existingAccessPoint Optionally, an existing AccessPoint
     *      database entity may be provided. If so, it is reused,
     *      rather than creating a new entity.
     * @param taskInfo A TaskInfo object that encapsulates the
     *      vocabulary and version entities, for which the access point
     *      is to be added.
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
            final AccessPoint existingAccessPoint,
            final TaskInfo taskInfo,
            final boolean isDraft,
            final String modifiedBy,
            final LocalDateTime nowTime,
            final au.org.ands.vocabs.registry.schema.vocabulary201701.
            AccessPoint schemaAP) {
        if (schemaAP.getSource() == ApSource.SYSTEM) {
            // Not needed/supported.
            return Pair.of(null, null);
        }
        Version version = taskInfo.getVersion();
        Integer versionId = version.getVersionId();
        AccessPoint ap;
        if (existingAccessPoint == null) {
            ap = new AccessPoint();
        } else {
            ap = existingAccessPoint;
        }
        ap.setType(schemaAP.getDiscriminator());
        switch (schemaAP.getDiscriminator()) {
        case API_SPARQL:
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
            if (existingAccessPoint == null) {
                AccessPointDAO.saveAccessPointWithId(em, ap);
            } else {
                AccessPointDAO.updateAccessPoint(em, ap);
            }
            return Pair.of(ap, null);
        case FILE:
            // In this case, there is something to be done apart from adding
            // to the database.
            return insertAccessPointFile(em, existingAccessPoint, taskInfo,
                    isDraft, modifiedBy, nowTime, schemaAP, versionId, ap);
        case SESAME_DOWNLOAD:
            // Nuh, you can't do this ... yet.
            // If this functionality is added, revisit the comment
            // for the SESAME_DOWNLOAD case in AccessPointElement.compareTo(),
            // and make sure there is a test case for that branch of the
            // code, i.e., adding multiple instances to the same version
            // at the same time.
            logger.error("Attempt to add sesameDownload with source=USER");
            return Pair.of(null, null);
        case SISSVOC:
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
            if (existingAccessPoint == null) {
                AccessPointDAO.saveAccessPointWithId(em, ap);
            } else {
                AccessPointDAO.updateAccessPoint(em, ap);
            }
            return Pair.of(ap, null);
        case WEB_PAGE:
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
            if (existingAccessPoint == null) {
                AccessPointDAO.saveAccessPointWithId(em, ap);
            } else {
                AccessPointDAO.updateAccessPoint(em, ap);
            }
            return Pair.of(ap, null);
        default:
            break;
        }
        return null;
    }

    /** The {@link #insertAccessPoint(EntityManager, AccessPoint, TaskInfo,
     * boolean, String, LocalDateTime,
     * au.org.ands.vocabs.registry.schema.vocabulary201701.AccessPoint)}
     * method defers to this method to
     * apply workflow insertion to a file access point specified in
     * registry schema format.
     * If the access point requires workflow processing (which is the
     * case if isDraft is false), the right-hand
     * component of the returned Pair is a list of workflow subtasks
     * that must be performed.
     * Sorry that this method has too many parameters.
     * @param em The EntityManager to use to persist any newly-created
     *      AccessPoint entity.
     * @param existingAccessPoint Optionally, an existing AccessPoint
     *      database entity may be provided. If so, it is reused,
     *      rather than creating a new entity.
     * @param taskInfo A TaskInfo object that encapsulates the
     *      vocabulary and version entities, for which the access point
     *      is to be added.
     * @param isDraft True, if this a request for insertion of a draft
     *      instance.
     * @param modifiedBy The value to use for "modifiedBy" when adding
     *      rows of the database.
     * @param nowTime The date/time being used for this insertion.
     * @param schemaAP The registry schema description of the access point
     *      to be added.
     * @param versionId The version Id of the version.
     * @param ap An already-created AccessPoint instance, which is to be
     *      filled in by this method.
     * @return A Pair of values. The left element is a non-null AccessPoint,
     *      if this is should be persisted directly. The right element
     *      is a non-empty list of subtasks, if workflow processing
     *      is required.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    private static Pair<AccessPoint, List<Subtask>> insertAccessPointFile(
            final EntityManager em, final AccessPoint existingAccessPoint,
            final TaskInfo taskInfo, final boolean isDraft,
            final String modifiedBy, final LocalDateTime nowTime,
            final
            au.org.ands.vocabs.registry.schema.vocabulary201701.AccessPoint
            schemaAP, final Integer versionId, final AccessPoint ap) {
        // In this case, there is something to be done apart from adding
        // to the database.
        List<Subtask> subtaskList = null;
        ap.setVersionId(versionId);
        ap.setSource(ApSource.USER);
        ApFile apFile = new ApFile();
        Integer uploadId = schemaAP.getApFile().getUploadId();
        Upload upload = UploadDAO.getUploadById(em, uploadId);
        // Override whatever format was specified by the user, and
        // use the upload format.
        apFile.setFormat(upload.getFormat());
        apFile.setUploadId(uploadId);
        if (isDraft) {
            TemporalUtils.makeDraft(ap);
            apFile.setDraftCreatedDate(nowTime.toString());
            apFile.setDraftModifiedDate(nowTime.toString());
            ap.setData(JSONSerialization.serializeObjectAsJsonString(
                    apFile));
            ap.setModifiedBy(modifiedBy);
            AccessPointDAO.saveAccessPointWithId(em, ap);
        } else {
            TemporalUtils.makeCurrentlyValid(ap, nowTime);
            // We make the access point visible, by "harvesting"
            // the upload.
            // Temporarily set the data to an empty value, so that
            // the entity can be persisted.
            ap.setData("{}");
            ap.setModifiedBy(modifiedBy);
            if (existingAccessPoint == null) {
                AccessPointDAO.saveAccessPointWithId(em, ap);
            } else {
                AccessPointDAO.updateAccessPoint(em, ap);
            }
            String harvestOutputPath =
                    TaskUtils.getTaskHarvestOutputPath(taskInfo, true);
            // We create our own filename, e.g., 17.ttl.
            // Here, we don't trust the extension of the original filename
            // (e.g., it could be in upper case).
            String filename = uploadId.toString() + "."
                    + FileFormatUtils.getFileFormatByName(
                            upload.getFormat()).getExtension();
            Path destPath = Paths.get(harvestOutputPath, filename);
            try {
                // We currently copy, but this could (maybe?) be
                // done as a symbolic link.
                Files.copy(RegistryFileUtils.getUploadPath(uploadId),
                        destPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                logger.error("Error attempting to copy uploaded file", e);
            }
            apFile.setPath(destPath.toString());
            // But we do use the original extension for the URL.
            apFile.setUrl(AccessPointUtils.
                    getDownloadUrlForFileAccessPoint(ap.getAccessPointId(),
                            upload.getFilename()));
            ap.setData(JSONSerialization.serializeObjectAsJsonString(
                    apFile));
            AccessPointDAO.updateAccessPoint(em, ap);
            subtaskList = new ArrayList<>();
            addConceptTransformSubtasks(subtaskList);
        }
        return Pair.of(ap, subtaskList);
    }

    /** Apply workflow deletion to an access point.
     * One of two situations applies: either, the access point can be, and is,
     * deleted immediately, or, a list of workflow subtasks must be performed.
     * NB: in either case, this method does <i>not</i> delete (or mark as
     * historical) the database row for the access point.
     * @param ap The access point to be deleted.
     * @param deletingVersion True, if the caller is deleting the entire
     *      version; false, if the caller is doing updates. This is used
     *      to determine whether or not it's "worth" adding subtasks in
     *      some cases.
     * @return A Pair. The left element of the pair is a Boolean, indicating
     *      if the caller should delete/make historical the access point.
     *      The right element of the pair is a list of workflow subtasks
     *      to be performed, or null, if there are no subtasks needed to
     *      be performed.
     */
    public static Pair<Boolean, List<Subtask>>
    deleteAccessPoint(final AccessPoint ap, final boolean deletingVersion) {
        // Take care to ensure that _every_ return statement returns
        // a Pair. _No_ instances of "return null"!

        // Default is that the caller does not have to delete (or mark
        // as historical) the row from the database.
        boolean doDatabaseDeletion = false;
        List<Subtask> subtaskList = new ArrayList<>();
        Subtask subtask;
        switch (ap.getType()) {
        case API_SPARQL:
        case SESAME_DOWNLOAD:
            if (ap.getSource() == ApSource.USER) {
                // These are source=USER, and no further action is required.
                return Pair.of(true, null);
            }
            // Handle source=SYSTEM.
            // Note well: SesameImporterProvider
            // is responsible for _both_ the API_SPARQL and SESAME_DOWNLOAD
            // access points. No problem to double-up; because of
            // the implementation of Subtask, there will be only one
            // instance of this added to the Task.
            subtask = new Subtask();
            subtask.setSubtaskProviderType(SubtaskProviderType.IMPORTER);
            subtask.setProvider(SesameImporterProvider.class);
            subtask.setOperation(SubtaskOperationType.DELETE);
            subtask.determinePriority();
            subtaskList.add(subtask);
            break;
        case FILE:
            ApFile apFile = JSONSerialization.deserializeStringAsJson(
                    ap.getData(), ApFile.class);
            try {
                Files.delete(Paths.get(apFile.getPath()));
            } catch (IOException e) {
                logger.error("Error deleting file: " + apFile.getPath(), e);
            }
            doDatabaseDeletion = true;
            // We deleted a file, therefore we need to run the
            // concept and resource docs tasks ... unless we're deleting
            // the entire vocabulary, in which case, we don't bother.
            if (!deletingVersion) {
                addConceptTransformSubtasks(subtaskList);
            }
            break;
        case SISSVOC:
            if (ap.getSource() == ApSource.USER) {
                // These are source=USER, and no further action is required.
                return Pair.of(true, null);
            }
            // Handle source=SYSTEM.
            subtask = new Subtask();
            subtask.setSubtaskProviderType(SubtaskProviderType.PUBLISH);
            subtask.setProvider(SISSVocPublishProvider.class);
            subtask.setOperation(SubtaskOperationType.DELETE);
            subtask.determinePriority();
            subtaskList.add(subtask);
            break;
        case WEB_PAGE:
            // These are all source=USER, and no further action is required.
            return Pair.of(true, null);
        default:
            // Oops, a type we don't know about.
            throw new IllegalArgumentException(
                    "Unknown access point type: " + ap.getType());
        }
        return Pair.of(doDatabaseDeletion, subtaskList);
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
            subtask.setProvider(ConceptTreeTransformProvider.class);
            subtask.setOperation(SubtaskOperationType.DELETE);
            subtask.determinePriority();
            break;
        case RESOURCE_DOCS:
            subtask.setSubtaskProviderType(SubtaskProviderType.TRANSFORM);
            subtask.setProvider(ResourceDocsTransformProvider.class);
            subtask.setOperation(SubtaskOperationType.DELETE);
            subtask.determinePriority();
            break;
        case HARVEST_POOLPARTY:
            subtask.setSubtaskProviderType(SubtaskProviderType.HARVEST);
            subtask.setProvider(PoolPartyHarvestProvider.class);
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

    /** To a given subtask list, add subtask transform operations
     * for the JsonList, ConceptTree, and ResourceDocs transform providers.
     * @param subtaskList An existing list of subtasks. It must not be null,
     *      but it may be empty.
     */
    public static void addConceptTransformSubtasks(
            final List<Subtask> subtaskList) {
        subtaskList.add(newConceptExtractionSubtask());
        subtaskList.add(newConceptBrowseSubtask());
        subtaskList.add(newResourceDocsSubtask());
    }

    /** Factory method to create a new subtask transform operation
     * for the JsonList transform provider.
     * @return The new Subtask instance.
     */
    private static Subtask newConceptExtractionSubtask() {
        Subtask subtask = new Subtask();
        subtask.setSubtaskProviderType(SubtaskProviderType.TRANSFORM);
        subtask.setProvider(JsonListTransformProvider.class);
        subtask.setOperation(SubtaskOperationType.INSERT);
        subtask.determinePriority();
        return subtask;
    }

    /** Factory method to create a subtask transform operation
     * for the ConceptTree transform provider.
     * @return The new Subtask instance.
     */
    public static Subtask newConceptBrowseSubtask() {
        Subtask subtask = new Subtask();
        subtask.setSubtaskProviderType(SubtaskProviderType.TRANSFORM);
        subtask.setProvider(ConceptTreeTransformProvider.class);
        subtask.setOperation(SubtaskOperationType.INSERT);
        subtask.determinePriority();
        return subtask;
    }

    /** Factory method to create a subtask transform operation
     * for the ResourceDocs transform provider.
     * @return The new Subtask instance.
     */
    public static Subtask newResourceDocsSubtask() {
        Subtask subtask = new Subtask();
        subtask.setSubtaskProviderType(SubtaskProviderType.TRANSFORM);
        subtask.setProvider(ResourceDocsTransformProvider.class);
        subtask.setOperation(SubtaskOperationType.INSERT);
        subtask.determinePriority();
        return subtask;
    }

    // Utility methods for "quickly" creating a subtask of a particular
    // provider type.

    /** Create a new Subtask to represent harvesting from PoolParty.
     * @param operation The operation to be performed; either INSERT or DELETE.
     * @param vocabulary The Vocabulary entity which holds the details
     *      of the PoolParty project, if operation is INSERT. If
     *      operation is DELETE, there does not need to be a PoolParty
     *      project specified.
     * @return The newly-created Subtask. If vocabulary does not specify a
     *      PoolParty project, null is returned.
     */
    public static Subtask createHarvestPoolPartySubtask(
            final SubtaskOperationType operation,
            final Vocabulary vocabulary) {
        Subtask subtask = new Subtask(SubtaskProviderType.HARVEST,
                operation, PoolPartyHarvestProvider.class);
        if (operation == SubtaskOperationType.DELETE) {
            // Nothing else required.
            return subtask;
        }
        VocabularyJson vocabularyJson =
                JSONSerialization.deserializeStringAsJson(
                        vocabulary.getData(), VocabularyJson.class);
        PoolpartyProject poolpartyProject =
                vocabularyJson.getPoolpartyProject();
        if (poolpartyProject == null) {
            throw new IllegalArgumentException("Harvest requested, but "
                    + "no project specified.");
        }
        subtask.addSubtaskProperty(PoolPartyHarvestProvider.SERVER_ID,
                poolpartyProject.getServerId().toString());
        subtask.addSubtaskProperty(PoolPartyHarvestProvider.PROJECT_ID,
                poolpartyProject.getProjectId());
        return subtask;
    }

    /** Create a new Subtask to represent importing into Sesame.
     * @param operation The operation to be performed; either INSERT or DELETE.
     * @return The newly-created Subtask.
     */
    public static Subtask createImporterSesameSubtask(
            final SubtaskOperationType operation) {
        Subtask subtask = new Subtask(SubtaskProviderType.IMPORTER,
                operation, SesameImporterProvider.class);
        return subtask;
    }

    /** Create a new Subtask to represent publishing into SISSVoc.
     * @param operation The operation to be performed; either INSERT or DELETE.
     * @return The newly-created Subtask.
     */
    public static Subtask createPublishSissvocSubtask(
            final SubtaskOperationType operation) {
        Subtask subtask = new Subtask(SubtaskProviderType.PUBLISH,
                operation, SISSVocPublishProvider.class);
        return subtask;
    }

    /** Create a new Subtask to represent inserting metadata into Sesame.
     * @param operation The operation to be performed; either PERFORM or DELETE.
     * @return The newly-created Subtask.
     */
    public static Subtask createSesameInsertMetadataSubtask(
            final SubtaskOperationType operation) {
        Subtask subtask = new Subtask(SubtaskProviderType.TRANSFORM,
                operation, SesameInsertMetadataTransformProvider.class);
        return subtask;
    }

}
