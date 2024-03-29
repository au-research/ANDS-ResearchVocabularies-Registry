/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.solr;

import static au.org.ands.vocabs.registry.solr.FieldConstants.ACCESS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.ACRONYM;
import static au.org.ands.vocabs.registry.solr.FieldConstants.CONCEPT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.DESCRIPTION;
import static au.org.ands.vocabs.registry.solr.FieldConstants.FORMAT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.ID;
import static au.org.ands.vocabs.registry.solr.FieldConstants.LANGUAGE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.LAST_UPDATED;
import static au.org.ands.vocabs.registry.solr.FieldConstants.LICENCE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.NOTE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.OWNER;
import static au.org.ands.vocabs.registry.solr.FieldConstants.POOLPARTY_ID;
import static au.org.ands.vocabs.registry.solr.FieldConstants.PUBLISHER;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SISSVOC_ENDPOINT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SLUG;
import static au.org.ands.vocabs.registry.solr.FieldConstants.STATUS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SUBJECT_IRIS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SUBJECT_LABELS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SUBJECT_NOTATIONS;
import static au.org.ands.vocabs.registry.solr.FieldConstants.SUBJECT_SOURCES;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TITLE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.TOP_CONCEPT;
import static au.org.ands.vocabs.registry.solr.FieldConstants.VERSION_ID;
import static au.org.ands.vocabs.registry.solr.FieldConstants.VERSION_RELEASE_DATE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.VERSION_TITLE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.VOCABULARY_ID;
import static au.org.ands.vocabs.registry.solr.FieldConstants.VOCABULARY_TITLE;
import static au.org.ands.vocabs.registry.solr.FieldConstants.WIDGETABLE;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.ContentStreamBase;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import au.org.ands.vocabs.registry.db.context.DBContext;
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.dao.AccessPointDAO;
import au.org.ands.vocabs.registry.db.dao.RelatedEntityDAO;
import au.org.ands.vocabs.registry.db.dao.VersionArtefactDAO;
import au.org.ands.vocabs.registry.db.dao.VersionDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyRelatedEntityDAO;
import au.org.ands.vocabs.registry.db.entity.AccessPoint;
import au.org.ands.vocabs.registry.db.entity.RelatedEntity;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.VersionArtefact;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.db.entity.VocabularyRelatedEntity;
import au.org.ands.vocabs.registry.db.internal.ApFile;
import au.org.ands.vocabs.registry.db.internal.ApSissvoc;
import au.org.ands.vocabs.registry.db.internal.VaConceptList;
import au.org.ands.vocabs.registry.db.internal.VaResourceDocs;
import au.org.ands.vocabs.registry.db.internal.VersionJson;
import au.org.ands.vocabs.registry.db.internal.VocabularyJson;
import au.org.ands.vocabs.registry.db.internal.VocabularyJson.PoolpartyProject;
import au.org.ands.vocabs.registry.db.internal.VocabularyJson.Subjects;
import au.org.ands.vocabs.registry.enums.AccessPointType;
import au.org.ands.vocabs.registry.enums.RelatedEntityRelation;
import au.org.ands.vocabs.registry.enums.VersionArtefactType;
import au.org.ands.vocabs.registry.enums.VersionStatus;
import au.org.ands.vocabs.registry.utils.fileformat.FileFormatUtils;
import au.org.ands.vocabs.registry.utils.language.Languages;
import au.org.ands.vocabs.registry.utils.language.ParsedLanguage;
import au.org.ands.vocabs.registry.workflow.provider.transform.JsonListTransformProvider;

/** Methods to support Solr indexing, including creating a Solr document
 * for a registry entity, and for adding and deleting index entries.
 */
public final class EntityIndexer {

    /** Private constructor for a utility class. */
    private EntityIndexer() {
    }

    /** Optimized access to the shared SolrClient for the registry collection.
     */
    private static final SolrClient SOLR_CLIENT_REGISTRY =
            SolrUtils.getSolrClientRegistry();

    /** Optimized access to the shared SolrClient for the resources collection.
     */
    private static final SolrClient SOLR_CLIENT_RESOURCES =
            SolrUtils.getSolrClientResources();

    /** The Solr endpoint for sending updates. */
    private static final String UPDATE_ENDPOINT = "/update";

    /** Logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /* Original comment from portal (PHP) code:
    // workaround for storing "groupings" of licence identifiers
    // Long term solution should use a vocabulary service (such as ANDS's)
    */

    /** Map of licence keys to a generic description.
     * Defined as a {@link HashMap} rather than as a {@link Map},
     * as the code relies on the ability to pass in the value
     * {@code null} to the {@link HashMap#get(Object)} method
     * without getting a {@code NullPointerException}. */
    private static HashMap<String, String> licenceGroups = new HashMap<>();

    /** Fallback value to use for licence, if the vocabulary does
     * not specify a licence, or it specifies a licence value,
     * but that value is not one of the keys of
     * {@code licenceGroups}. */
    private static final String LICENCE_UNKNOWN = "Unknown/Other";

    static {
        licenceGroups.put("GPL", "Open Licence");
        licenceGroups.put("CC-BY-SA", "Open Licence");
        licenceGroups.put("CC-BY-ND", "Non-Derivative Licence");
        licenceGroups.put("CC-BY-NC-SA", "Non-Commercial Licence");
        licenceGroups.put("CC-BY-NC-ND", "Non-Derivative Licence");
        licenceGroups.put("CC-BY-NC", "Non-Commercial Licence");
        licenceGroups.put("CC-BY", "Open Licence");
        licenceGroups.put("CC0", "Open Licence");
        licenceGroups.put("ODC-By", "Open Licence");
        licenceGroups.put("CSIRODataLicence", "Non-Commercial Licence");
        licenceGroups.put("AusGoalRestrictive", "Restrictive Licence");
        licenceGroups.put("NoLicence", "No Licence");
        // Allow users to specify LICENCE_UNKNOWN itself!
        licenceGroups.put(LICENCE_UNKNOWN, LICENCE_UNKNOWN);
    }

    /** Map of access point types to a human-readable description. */
    private static Map<AccessPointType, String> accessPointName =
            new HashMap<>();

    static {
        accessPointName.put(AccessPointType.API_SPARQL, "API/SPARQL");
        // No longer distinguish FILE and SESAME_DOWNLOAD access points.
        // accessPointName.put(AccessPointType.FILE, "Direct Download");
        accessPointName.put(AccessPointType.FILE, "Download");
        // accessPointName.put(AccessPointType.SESAME_DOWNLOAD,
        //         "Repository Download");
        accessPointName.put(AccessPointType.SESAME_DOWNLOAD, "Download");
        accessPointName.put(AccessPointType.SISSVOC, "Linked Data API");
        accessPointName.put(AccessPointType.WEB_PAGE, "Online");
    }

    /** Add a key/value pair to the Solr document, if the value
     * is non-null and non-empty. The value is specified as a String.
     * @param document The Solr document.
     * @param key The key.
     * @param value The value.
     */
    private static void addDataToDocument(final SolrInputDocument document,
            final String key, final String value) {
        // The basic Java way here would be:
        //      if (value != null && !value.isEmpty()) {
        // But Apache Commons Lang3 StringUtils.isNotBlank()
        // not only does that, but also filters out Strings that
        // are _sequences_ of whitespace.
        if (StringUtils.isNotBlank(value)) {
            document.addField(key, value);
        }
    }

    /** Convert a {@link LocalDateTime} to a String, in the format
     * expected by Solr.
     * @param timestamp The LocalDateTime value to be converted.
     * @return The timestamp converted into a String.
     */
    public static String localDateTimeToString(final LocalDateTime timestamp) {
        return timestamp.atZone(ZoneOffset.UTC).
            format(DateTimeFormatter.ISO_INSTANT);
    }

    /** Add a key/value pair to the Solr document, if the value
     * is non-null and non-empty. The value is specified as a LocalDateTime.
     * @param document The Solr document.
     * @param key The key.
     * @param value The value.
     */
    private static void addDataToDocument(final SolrInputDocument document,
            final String key, final LocalDateTime value) {
        if (value != null) {
            document.addField(key, localDateTimeToString(value));
        }
    }

    /** Create the Solr document for the current version of a vocabulary.
     * @param em The EntityManager to use.
     * @param vocabulary The vocabulary for which the Solr document is
     *      to be created.
     * @return The Solr document.
     */
    public static SolrInputDocument createSolrDocument(
            final EntityManager em,
            final Vocabulary vocabulary) {
        Integer vocabularyId = vocabulary.getVocabularyId();
        SolrInputDocument document = new SolrInputDocument();
        VocabularyJson vocabularyData =
                JSONSerialization.deserializeStringAsJson(vocabulary.getData(),
                        VocabularyJson.class);

        // The order of the fields matches:
        // https://ardc-services.atlassian.net/wiki/spaces/PROJ/pages/
        //     3376942/Vocabulary+Solr+documents+and+queries+New+Generation
        addDataToDocument(document, LAST_UPDATED, vocabulary.getStartDate());
        addDataToDocument(document, ID, Integer.toString(vocabularyId));
        addDataToDocument(document, TITLE, vocabularyData.getTitle());
        addDataToDocument(document, SLUG, vocabulary.getSlug());
        PoolpartyProject ppProject = vocabularyData.getPoolpartyProject();
        if (ppProject != null) {
            addDataToDocument(document, POOLPARTY_ID,
                    vocabularyData.getPoolpartyProject().getProjectId());
        }
        addDataToDocument(document, STATUS,
                vocabulary.getStatus().toString());
        // Licence values are grouped together into categories.
        // Map the licence value to its category for indexing.
        String licence = vocabularyData.getLicence();
        String licenceGroup = licenceGroups.get(licence);
        if (licenceGroup != null) {
            addDataToDocument(document, LICENCE, licenceGroup);
        } else {
            // No licence specified, or a non-blank licence,
            // but one we don't recognize; use the fallback value.
            addDataToDocument(document, LICENCE, LICENCE_UNKNOWN);
        }
        addDataToDocument(document, ACRONYM, vocabularyData.getAcronym());
        // Strip HTML tags, and convert HTML elements into their
        // corresponding Unicode characters.
        addDataToDocument(document, DESCRIPTION,
                Jsoup.parse(vocabularyData.getDescription()).text());
        // Note is like description, but it is optional.
        String note = vocabularyData.getNote();
        if (note != null) {
            // Strip HTML tags, and convert HTML elements into their
            // corresponding Unicode characters.
            addDataToDocument(document, NOTE, Jsoup.parse(note).text());
        }
        // languages
        //   NB: as a function, the resolveLanguage() method is not currently
        //   one-to-one. (E.g., "en" -> "English", and "en-CA" -> "English".)
        //   So, if languages were defined as an ArrayList, it would be
        //   quite possible for it to contain duplicate values.
        //   This would not affect subsequent Solr search result scores or
        //   facet counts, so there's no pressing need to remove these
        //   duplicates.
        //   Nevertheless, remove duplicates, while otherwise preserving
        //   the sequence of values, by using a LinkedHashSet.
        LinkedHashSet<String> languages = new LinkedHashSet<>();
        languages.add(resolveLanguage(vocabularyData.getPrimaryLanguage()));
        for (String otherLanguage : vocabularyData.getOtherLanguages()) {
            languages.add(resolveLanguage(otherLanguage));
        }
        // Use addField() directly, as we know that languages is non-empty.
        document.addField(LANGUAGE, languages);
        // subjects
        List<Subjects> subjects = vocabularyData.getSubjects();
        if (!subjects.isEmpty()) {
            ArrayList<String> subjectTypes = new ArrayList<>();
            ArrayList<String> subjectLabels = new ArrayList<>();
            ArrayList<String> subjectNotations = new ArrayList<>();
            ArrayList<String> subjectIris = new ArrayList<>();
            for (Subjects subject : subjects) {
                // Source and label are required ...
                subjectTypes.add(subject.getSource());
                subjectLabels.add(subject.getLabel());
                // ... but notation and IRI are optional.
                subjectNotations.add(StringUtils.defaultString(
                        subject.getNotation()));
                subjectIris.add(StringUtils.defaultString(subject.getIri()));
            }
            document.addField(SUBJECT_SOURCES, subjectTypes);
            document.addField(SUBJECT_LABELS, subjectLabels);
            document.addField(SUBJECT_NOTATIONS, subjectNotations);
            document.addField(SUBJECT_IRIS, subjectIris);
        }
        // top concepts
        List<String> topConcepts = vocabularyData.getTopConcepts();
        if (!topConcepts.isEmpty()) {
            document.addField(TOP_CONCEPT, topConcepts);
        }
        addDataToDocument(document, OWNER, vocabulary.getOwner());
        // publishers
        // First get VocabularyRelatedEntity objects.
        MultivaluedMap<Integer, VocabularyRelatedEntity> vreMap =
                VocabularyRelatedEntityDAO.
                getCurrentVocabularyRelatedEntitiesForVocabulary(
                        em, vocabularyId);
        // Now fetch the related entities that are publishers of this
        // vocabulary.
        ArrayList<RelatedEntity> relatedEntities = new ArrayList<>();
        for (Map.Entry<Integer, List<VocabularyRelatedEntity>>
            vreMapElement : vreMap.entrySet()) {
            for (VocabularyRelatedEntity vre : vreMapElement.getValue()) {
                if (RelatedEntityRelation.PUBLISHED_BY.equals(
                        vre.getRelation())) {
                    relatedEntities.add(RelatedEntityDAO.
                            getCurrentRelatedEntityByRelatedEntityId(
                                    em, vre.getRelatedEntityId()));
                }
            }
        }
        if (!relatedEntities.isEmpty()) {
            ArrayList<String> publishers = new ArrayList<>();
            for (RelatedEntity re : relatedEntities) {
                publishers.add(re.getTitle());
            }
            document.addField(PUBLISHER, publishers);
        }

        // The values of the remaining fields are determined by what
        // is the "most suitable version" of the vocabulary, if it has one.
        addFieldsForMostSuitableVersion(em, vocabularyId, document);
        return document;
    }

    /** Add the fields for the "most suitable" version of the vocabulary.
     * Here, "most suitable" means the current version, if there is one.
     * Otherwise, it means the most recent superseded version (if there
     * is one), as returned by {@link
     * VersionDAO#getCurrentVersionListForVocabularyByReleaseDateSlug(Integer)}.
     * This implementation of "most suitable" matches the Portal
     * implementation in {@code vocab.blade.php}.
     * @param em The EntityManager to use.
     * @param vocabularyId The vocabulary ID of the vocabulary. Used to
     *      fetch other database entities.
     * @param document The Solr document being generated. This method
     *      adds fields to it.
     */
    private static void addFieldsForMostSuitableVersion(
            final EntityManager em,
            final Integer vocabularyId,
            final SolrInputDocument document) {
        List<Version> versions =
                VersionDAO.getCurrentVersionListForVocabularyByReleaseDateSlug(
                        vocabularyId, em);
        // The widgetable flag is always added to the Solr document.
        // Assume false unless/until proven otherwise.
        boolean widgetable = false;
        ArrayList<String> concepts = new ArrayList<>();
        // Find the current version, if any.
        Version mostSuitableVersion = null;
        Version currentVersion = null;
        for (Version version : versions) {
            if (version.getStatus() == VersionStatus.CURRENT) {
                currentVersion = version;
                break;
            }
        }
        if (currentVersion != null) {
            mostSuitableVersion = currentVersion;
        } else if (versions.size() > 0) {
            // There's no current version, but there are versions,
            // so the first one in the list is the "most suitable"
            // superseded version.
            // NB: if we ever introduce another version status value that
            // would be "less suitable" than superseded, change this code
            // to include a test of the status!
            mostSuitableVersion = versions.get(0);
        }
        if (mostSuitableVersion != null) {
            // Now add the fields that depend on this.
            Integer mostSuitableVersionId = mostSuitableVersion.getVersionId();
            ArrayList<String> accessList = new ArrayList<>();
            ArrayList<String> formatList = new ArrayList<>();
            List<VersionArtefact> conceptLists =
                    VersionArtefactDAO.
                    getCurrentVersionArtefactListForVersionByType(
                            mostSuitableVersionId,
                            VersionArtefactType.CONCEPT_LIST, em);
            if (conceptLists != null && conceptLists.size() == 1) {
                String artefactData = conceptLists.get(0).getData();
                VaConceptList vaConceptList =
                        JSONSerialization.deserializeStringAsJson(
                                artefactData, VaConceptList.class);
                File conceptsFile = new File(vaConceptList.getPath());
                try {
                    Map<String, Map<String, Object>> conceptsData =
                            JSONSerialization.deserializeStringAsJson(
                                    conceptsFile,
                                    new TypeReference<Map<String,
                                    Map<String, Object>>>() { });
                    for (Map<String, Object> concept : conceptsData.values()) {
                        if (concept.containsKey(JsonListTransformProvider.
                                PREF_LABEL)) {
                            concepts.add((String) concept.get(
                                    JsonListTransformProvider.PREF_LABEL));
                        }
                    }
                } catch (IllegalStateException ise) {
                    // This happens when Jackson's own hash implementation
                    // thinks it has detected a DoS:
                    // java.lang.IllegalStateException: Spill-over slots
                    //   in symbol table with 7128 entries,
                    //   hash area of 16384 slots is now full
                    //   (all 2048 slots -- suspect a DoS attack based on
                    //   hash collisions. You can disable the check via
                    //   `JsonFactory.Feature.FAIL_ON_SYMBOL_HASH_OVERFLOW`
                    LOGGER.error("Not indexing concepts, because Jackson says "
                            + "it looks like a DoS", ise);
                }
            }
            List<AccessPoint> accessPoints = AccessPointDAO.
                    getCurrentAccessPointListForVersion(em,
                            mostSuitableVersionId);
            for (AccessPoint accessPoint : accessPoints) {
                accessList.add(accessPointName.get(accessPoint.getType()));
                switch (accessPoint.getType()) {
                case API_SPARQL:
                    break;
                case FILE:
                    ApFile apFile = JSONSerialization.deserializeStringAsJson(
                            accessPoint.getData(), ApFile.class);
                    formatList.add(apFile.getFormat());
                    break;
                case SESAME_DOWNLOAD:
                    // CC-2455 Add all Sesame download types as formats.
                    formatList.addAll(
                            FileFormatUtils.getAllSesameDownloadFormatNames());
                    break;
                case SISSVOC:
                    // Be careful not to try to add a second SISSVOC_ENDPOINT,
                    // since there can only be one. So sneakily use the
                    // widgetable flag to determine that we've been here
                    // before.
                    if (!widgetable) {
                        widgetable = true;
                        ApSissvoc apSissvoc = JSONSerialization.
                                deserializeStringAsJson(
                                        accessPoint.getData(), ApSissvoc.class);
                        document.addField(SISSVOC_ENDPOINT,
                                apSissvoc.getUrlPrefix());
                    }
                    break;
                case WEB_PAGE:
                    break;
                default:
                    // Oops.
                }
            }
            document.addField(ACCESS, accessList);
            document.addField(FORMAT, formatList);
        }
        document.addField(CONCEPT, concepts);
        document.addField(WIDGETABLE, widgetable);
    }

    /** Resolve a language tag into its brief description.
     * The tag must be valid according to BCP 47.
     * @param lang The language tag to be resolved.
     * @return The resolved language name. Only the brief description is
     *      returned. For example, "en-GB" is returned as "English".
     */
    public static String resolveLanguage(final String lang) {
        ParsedLanguage parsedLanguage = Languages.getParsedLanguage(lang);
        if (parsedLanguage.isValid()) {
            return parsedLanguage.getBriefDescription();
        } else {
            // Should never happen, if our validation is correct.
            LOGGER.error("Tried to resolved language that should be valid, "
                    + "but it wasn't: " + lang);
            return "";
        }
    }

    /* Methods for communicating with Solr.
     * Note: for single additions/deletions, there are no explicit commits.
     * We rely on the updateHandler.autoSoftCommit.maxTime setting
     * made by CreateSolrSchemaRegistry.
     */

    /** Index a current vocabulary in Solr.
     * @param vocabularyId The vocabulary ID of the vocabulary to be
     *      added to the Solr indexes.
     * @throws IOException If the Solr API generated an IOException.
     * @throws SolrServerException If the Solr API generated a
     *      SolrServerException.
     * @throws RemoteSolrException If there is a problem communicating with
     *      Zookeeper.
     */
    public static void indexVocabulary(final int vocabularyId)
            throws IOException, SolrServerException, RemoteSolrException {
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            Vocabulary vocabulary =
                    VocabularyDAO.getCurrentVocabularyByVocabularyId(em,
                            vocabularyId);
            if (vocabulary == null) {
                // For now, do nothing. Maybe revisit this decision later,
                // e.g., to throw an exception in this case.
                return;
            }
            SolrInputDocument document = createSolrDocument(em, vocabulary);
            try {
                SOLR_CLIENT_REGISTRY.add(document);
            } catch (IOException | SolrServerException
                    | RemoteSolrException e) {
                LOGGER.error("Exception when adding document to Solr index", e);
                throw e;
            }
            indexResourceDocsForVocabulary(em,
                    vocabularyId, vocabulary, document);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /** Index all current vocabularies into Solr.
     * @throws IOException If the Solr API generated an IOException.
     * @throws SolrServerException If the Solr API generated a
     *      SolrServerException.
     * @throws RemoteSolrException If there is a problem communicating with
     *      Zookeeper.
     */
    public static void indexAllVocabularies()
            throws IOException, SolrServerException, RemoteSolrException {
        // At present, there isn't a method getAllCurrentVocabulary(em),
        // so for now, use the method that makes its own EntityManager.
        // If we later have another reason to add that method, come
        // back here and use it.
        List<Vocabulary> allVocabularies =
                VocabularyDAO.getAllCurrentVocabulary();
        EntityManager em = null;
        try {
            em = DBContext.getEntityManager();
            List<SolrInputDocument> documents = new ArrayList<>();
            // Populate vocabularyIdList with vocabulary Ids, so we can
            // invoke indexResourceDocsForVocabulary on each one.
            List<Integer> vocabularyIdList = new ArrayList<>();
            for (Vocabulary vocabulary : allVocabularies) {
                vocabularyIdList.add(vocabulary.getVocabularyId());
                documents.add(createSolrDocument(em, vocabulary));
            }
            try {
                // First, delete all existing documents.
                SOLR_CLIENT_REGISTRY.deleteByQuery("*:*");
                // In this case, we do do a commit immediately (by specifying 0
                // as the second parameter).
                SOLR_CLIENT_REGISTRY.add(documents, 0);
                // Clear out the entire resources index here, as there
                // "might" be deleted vocabularies for which resources
                // still need to be cleaned out. (Of course, that
                // "shouldn't happen", but if you're using this method,
                // you may well be dealing with an exceptional circumstance.)
                SOLR_CLIENT_RESOURCES.deleteByQuery("*:*");
            } catch (IOException | SolrServerException
                    | RemoteSolrException e) {
                LOGGER.error("Exception when adding documents to Solr index",
                        e);
                throw e;
            }
            // Now put all of the resource docs into the
            // resource Solr collection.
            for (int i = 0; i < vocabularyIdList.size(); i++) {
                indexResourceDocsForVocabulary(em, vocabularyIdList.get(i),
                        allVocabularies.get(i), documents.get(i));
            }
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /** Remove a current vocabulary from the Solr registry index,
     *      and remove its resource docs from the resources Solr index.
     * @param vocabularyId The vocabulary ID of the vocabulary to be
     *      removed from the Solr index.
     * @throws IOException If the Solr API generated an IOException.
     * @throws SolrServerException If the Solr API generated a
     *      SolrServerException.
     * @throws RemoteSolrException If there is a problem communicating with
     *      Zookeeper.
     */
    public static void unindexVocabulary(final int vocabularyId)
            throws IOException, SolrServerException, RemoteSolrException {
        try {
            SOLR_CLIENT_REGISTRY.deleteById(Integer.toString(vocabularyId));
            SOLR_CLIENT_RESOURCES.deleteByQuery("vocabulary_id:"
                    + vocabularyId);
        } catch (IOException | SolrServerException | RemoteSolrException e) {
            LOGGER.error("Exception when removing documents from Solr indexes",
                    e);
            throw e;
        }
    }

    // In testing with a very large number (> 9000) of vocabularies,
    // creating the Solr documents for all vocabularies can take over
    // a minute. It used to be that
    // AdminRestMethods.indexAll() invoked first unindexAllVocabularies()
    // and then indexAllVocabularies(). That meant that you could be
    // without an index for over a minute. That would be bad.
    // So I've copied the call to SOLR_CLIENT_REGISTRY.deleteByQuery() into
    // indexAllVocabularies(), and (for now) we no longer need this method.
    // If, in future, we decide to add an AdminRestMethods.unindexAll() method,
    // uncomment this again.
//    /** Remove all documents from the Solr index.
//     * @throws IOException If the Solr API generated an IOException.
//     * @throws SolrServerException If the Solr API generated a
//     *      SolrServerException.
//     * @throws RemoteSolrException If there is a problem communicating with
//     *      Zookeeper.
//     */
//    public static void unindexAllVocabularies()
//            throws IOException, SolrServerException, RemoteSolrException {
//        try {
//            // Delete by matching all documents.
//            // In this case, we do do a commit immediately (by specifying 0
//            // as the second parameter).
//            SOLR_CLIENT_REGISTRY.deleteByQuery("*:*", 0);
//            SOLR_CLIENT_RESOURCES.deleteByQuery("*:*", 0);
//        } catch (IOException | SolrServerException | RemoteSolrException e) {
//            LOGGER.error("Exception when removing all documents "
//                    + "from Solr index", e);
//            throw e;
//        }
//    }

    /** Add all of the current resource docs for a vocabulary to the resources
     * Solr collection.
     * Here, "current" means non-historical, non-draft.
     * We index all such versions, irrespective of
     * whether they have version status "current" or "superseded".
     * Note: the Resource Docs version artefacts contain Solr documents
     * that don't have values for the fields for vocabulary and version
     * metadata, e.g., vocabulary title and owner, and version status.
     * This method fills in all of the missing data before sending it
     * to Solr for indexing.
     * @param em The EntityManager to use.
     * @param vocabularyId The vocabulary ID of the vocabulary for which
     *      resource docs are to be added to the Solr index.
     * @param vocabulary The current Vocabulary instance of the vocabulary
     *      for which resource docs are to be added to the Solr index.
     * @param document The Solr document already constructed from the
     *      vocabulary metadata, that went into the registry collection.
     *      Used as a quick way to get some of the vocabulary-level metadata,
     *      rather than re-computing it.
     */
    private static void indexResourceDocsForVocabulary(
            final EntityManager em,
            final int vocabularyId,
            final Vocabulary vocabulary,
            final SolrInputDocument document) {
        // Extract the fields that we need to add to each Solr document,
        // that come from the vocabulary-level metadata.
        VocabularyJson vocabularyJson =
                JSONSerialization.deserializeStringAsJson(
                        vocabulary.getData(), VocabularyJson.class);

        String vocabularyTitle = vocabularyJson.getTitle();
        String vocabularyIdString = Integer.toString(vocabularyId);
        String owner = vocabulary.getOwner();
        String lastUpdated = localDateTimeToString(vocabulary.getStartDate());
        // Don't re-compute subject labels and publishers, but reuse
        // the values we already have.
        @SuppressWarnings("unchecked")
        ArrayList<String> subjectLabels = (ArrayList<String>)
                document.getField(SUBJECT_LABELS).getValue();
        @SuppressWarnings("unchecked")
        ArrayList<String> publishers = (ArrayList<String>)
                document.getField(PUBLISHER).getValue();

        // Get all "current" versions, which here means non-historical,
        // non-draft. We index all such versions, irrespective of
        // whether they have version status "current" or "superseded".
        List<Version> versions =
                VersionDAO.getCurrentVersionListForVocabulary(em, vocabularyId);
        try {
            SOLR_CLIENT_RESOURCES.deleteByQuery("vocabulary_id:"
                    + vocabularyId);
        } catch (SolrServerException | IOException e) {
            LOGGER.error("Unable to delete existing resource docs "
                    + "from Solr collection", e);
        }
        ObjectMapper mapper = new ObjectMapper();
        for (Version version : versions) {
            // Extract the fields that we need to add to each Solr document,
            // that come from the version-level metadata.
            Integer versionId = version.getVersionId();
            VersionJson versionJson = JSONSerialization.deserializeStringAsJson(
                    version.getData(), VersionJson.class);
            String versionTitle = versionJson.getTitle();
            String versionReleaseDate = version.getReleaseDate();
            String versionStatus = version.getStatus().toString();
            String sissvocEndpoint = getSissvocEndpoint(em, versionId);

            List<VersionArtefact> resourceDocs = VersionArtefactDAO.
                    getCurrentVersionArtefactListForVersionByType(
                            versionId, VersionArtefactType.RESOURCE_DOCS, em);
            if (resourceDocs != null && resourceDocs.size() == 1) {
                String vaData = resourceDocs.get(0).getData();
                VaResourceDocs vaResourceDocs =
                        JSONSerialization.deserializeStringAsJson(
                                vaData, VaResourceDocs.class);
                File resourceDocsFile = new File(vaResourceDocs.getPath());
                JsonNode jsonNode = jsonFileToJsonNode(mapper,
                        resourceDocsFile);
                if (jsonNode == null) {
                    LOGGER.error("JSON file parsed as null; skipping");
                    continue;
                }
                // jsonNode is an array of objects. Iterate over it, adding
                // the basic fields to each object.
                for (JsonNode node : jsonNode) {
                    ObjectNode objectNode = (ObjectNode) node;
                    objectNode.put(VERSION_ID, versionId);
                    objectNode.put(VERSION_TITLE, versionTitle);
                    objectNode.put(VERSION_RELEASE_DATE, versionReleaseDate);
                    objectNode.put(VOCABULARY_ID, vocabularyIdString);
                    objectNode.put(VOCABULARY_TITLE, vocabularyTitle);
                    objectNode.put(OWNER, owner);
                    objectNode.put(LAST_UPDATED, lastUpdated);
                    if (sissvocEndpoint != null) {
                        objectNode.put(SISSVOC_ENDPOINT, sissvocEndpoint);
                    }
                    objectNode.putPOJO(SUBJECT_LABELS, subjectLabels);
                    objectNode.putPOJO(PUBLISHER, publishers);
                    objectNode.put(STATUS, versionStatus);
                }
                try {
                    ContentStreamUpdateRequest request =
                            new ContentStreamUpdateRequest(UPDATE_ENDPOINT);
                    ContentStreamBase cs =
                            new ContentStreamBase.StringStream(
                                    jsonNodeToString(mapper, jsonNode),
                                    MediaType.APPLICATION_JSON);
                    request.addContentStream(cs);
                    request.process(SOLR_CLIENT_RESOURCES);
                } catch (IOException | SolrServerException e) {
                    LOGGER.error("Unable to process update to add resource "
                            + "docs for vocabulary: " + vocabularyId, e);
                }
            }
        }
    }

    /** Get the value to use for the {@code sissvoc_endpoint} field
     * of the resources of a specified version, if the version
     * has a currently-valid access point of type {@code sissvoc},
     * or null, if it doesn't.
     * @param em The EntityManager to use.
     * @param versionId The version Id of the version.
     * @return The {@code url-prefix} of the version's (first) access point
     *      of type {@code sissvoc}, if such an access point exists,
     *      or null, if it doesn't.
     */
    private static String getSissvocEndpoint(final EntityManager em,
            final Integer versionId) {
        List<AccessPoint> accessPoints = AccessPointDAO.
                getCurrentAccessPointListForVersionByType(versionId,
                        AccessPointType.SISSVOC, em);
        if (accessPoints != null && accessPoints.size() > 0) {
            ApSissvoc apSissvoc = JSONSerialization.
                    deserializeStringAsJson(
                            accessPoints.get(0).getData(), ApSissvoc.class);
            return apSissvoc.getUrlPrefix();
        }
        // No currently-valid access point of type "sissvoc" for this version.
        return null;
    }

    /** Parse a file containing JSON into a JsonNode.
     * @param mapper The Jackson ObjectMapper to use.
     * @param jsonFile The File in JSON format to be converted.
     * @return The resulting JSON structure, or null, if there was
     *      an Exception.
     */
    private static JsonNode jsonFileToJsonNode(final ObjectMapper mapper,
            final File jsonFile) {
        try {
            return mapper.readTree(jsonFile);
        } catch (IOException e) {
            LOGGER.error("Exception in jsonFileToJsonNode", e);
            return null;
        }
    }

    /** Convert a JsonNode into a String representation.
     * @param mapper The Jackson ObjectMapper to use.
     * @param jsonNode The JsonNode to be converted to a String.
     * @return The resulting String, or null, if there was
     *      an Exception.
     */
    private static String jsonNodeToString(final ObjectMapper mapper,
            final JsonNode jsonNode) {
        try {
            return mapper.writeValueAsString(jsonNode);
        } catch (IOException e) {
            LOGGER.error("Exception in jsonNodeToString", e);
            return null;
        }
    }

    /** Force a soft commit of pending changes to the Solr collections.
     * @throws IOException If the Solr API generated an IOException.
     * @throws SolrServerException If the Solr API generated a
     *      SolrServerException.
     * @throws RemoteSolrException If there is a problem communicating with
     *      Zookeeper.
     */
    public static void commit()
            throws IOException, SolrServerException, RemoteSolrException {
        try {
            // We need the four-parameter version in order to get
            // soft commit. The zero-parameter commit() method
            // does a hard commit.
            SOLR_CLIENT_REGISTRY.commit(null, false, true, true);
            SOLR_CLIENT_RESOURCES.commit(null, false, true, true);
        } catch (IOException | SolrServerException | RemoteSolrException e) {
            LOGGER.error("Exception during commit of Solr collections", e);
            throw e;
        }
    }

}
