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
import static au.org.ands.vocabs.registry.solr.FieldConstants.WIDGETABLE;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient.RemoteSolrException;
import org.apache.solr.common.SolrInputDocument;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ibm.icu.util.ULocale;

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
import au.org.ands.vocabs.registry.db.internal.VocabularyJson;
import au.org.ands.vocabs.registry.db.internal.VocabularyJson.PoolpartyProject;
import au.org.ands.vocabs.registry.db.internal.VocabularyJson.Subjects;
import au.org.ands.vocabs.registry.enums.AccessPointType;
import au.org.ands.vocabs.registry.enums.RelatedEntityRelation;
import au.org.ands.vocabs.registry.enums.VersionArtefactType;
import au.org.ands.vocabs.registry.enums.VersionStatus;
import au.org.ands.vocabs.registry.utils.fileformat.FileFormatUtils;
import au.org.ands.vocabs.registry.workflow.provider.transform.JsonListTransformProvider;

/** Methods to support Solr indexing, including creating a Solr document
 * for a registry entity, and for adding and deleting index entries.
 */
public final class EntityIndexer {

    /** Private constructor for a utility class. */
    private EntityIndexer() {
    }

    /** Optimized access to the shared SolrClient. */
    private static final SolrClient SOLR_CLIENT = SolrUtils.getSolrClient();

    /** Logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /* Original comment from portal (PHP) code:
    // workaround for storing "groupings" of licence identifiers
    // Long term solution should use a vocabulary service (such as ANDS's)
    */

    /** Map of licence keys to a generic description. */
    private static Map<String, String> licenceGroups = new HashMap<>();

    static {
        licenceGroups.put("GPL", "Open Licence");
        licenceGroups.put("CC-BY-SA", "Open Licence");
        licenceGroups.put("CC-BY-ND", "Non-Derivative Licence");
        licenceGroups.put("CC-BY-NC-SA", "Non-Commercial Licence");
        licenceGroups.put("CC-BY-NC-ND", "Non-Derivative Licence");
        licenceGroups.put("CC-BY-NC", "Non-Commercial Licence");
        licenceGroups.put("CC-BY", "Open Licence");
        licenceGroups.put("CSIRO Data Licence", "Non-Commercial Licence");
        licenceGroups.put("AusGoalRestrictive", "Restrictive Licence");
        licenceGroups.put("NoLicence", "No Licence");
    }

    /** Map of access point types to a human-readable description. */
    private static Map<AccessPointType, String> accessPointName =
            new HashMap<>();

    static {
        accessPointName.put(AccessPointType.API_SPARQL, "API/SPARQL");
        accessPointName.put(AccessPointType.FILE, "Direct Download");
        accessPointName.put(AccessPointType.SESAME_DOWNLOAD,
                "Repository Download");
        accessPointName.put(AccessPointType.SISSVOC, "Linked Data API");
        accessPointName.put(AccessPointType.WEB_PAGE, "Online");
    }

    /** Locale for conversion of ISO 639 codes into human-readable
     * forms used in the indexing process. Currently, we use the "en_NZ"
     * locale, as this gives the "right answers", i.e., "Māori" for "mi".
     */
    private static final ULocale LANGUAGE_LOCALE = new ULocale("en_NZ");

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

    /** Add a key/value pair to the Solr document, if the value
     * is non-null and non-empty. The value is specified as a LocalDateTime.
     * @param document The Solr document.
     * @param key The key.
     * @param value The value.
     */
    private static void addDataToDocument(final SolrInputDocument document,
            final String key, final LocalDateTime value) {
        if (value != null) {
            document.addField(key,
                    value.atZone(ZoneOffset.UTC).
                        format(DateTimeFormatter.ISO_INSTANT));
        }
    }

    /** Create the Solr document for the current version of a vocabulary.
     * @param vocabulary The vocabulary for which the Solr document is
     *      to be created.
     * @return The Solr document.
     */
    public static SolrInputDocument createSolrDocument(
            final Vocabulary vocabulary) {
        Integer vocabularyId = vocabulary.getVocabularyId();
        SolrInputDocument document = new SolrInputDocument();
        VocabularyJson vocabularyData =
                JSONSerialization.deserializeStringAsJson(vocabulary.getData(),
                        VocabularyJson.class);

        // The order of the fields matches:
        // https://intranet.ands.org.au/display/PROJ/
        //         Vocabulary+Solr+documents+and+queries
        addDataToDocument(document, LAST_UPDATED,
                vocabulary.getStartDate());
        addDataToDocument(document, ID,
                Integer.toString(vocabularyId));
        addDataToDocument(document, TITLE, vocabularyData.getTitle());
        addDataToDocument(document, SLUG, vocabulary.getSlug());
        PoolpartyProject ppProject = vocabularyData.getPoolpartyProject();
        if (ppProject != null) {
            addDataToDocument(document, POOLPARTY_ID,
                    vocabularyData.getPoolpartyProject().getProjectId());
        }
        addDataToDocument(document, STATUS,
                vocabulary.getStatus().toString());
        addDataToDocument(document, LICENCE,
                licenceGroups.get(vocabularyData.getLicence()));
        addDataToDocument(document, ACRONYM, vocabularyData.getAcronym());
        // Strip HTML tags, and convert HTML elements into their
        // corresponding Unicode characters.
        addDataToDocument(document, DESCRIPTION,
                Jsoup.parse(vocabularyData.getDescription()).text());
        // languages
        ArrayList<String> languages = new ArrayList<>();
        ULocale loc = new ULocale(vocabularyData.getPrimaryLanguage());
        languages.add(loc.getDisplayName(LANGUAGE_LOCALE));
        for (String otherLanguage : vocabularyData.getOtherLanguages()) {
            loc = new ULocale(otherLanguage);
            languages.add(loc.getDisplayName(LANGUAGE_LOCALE));
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
                        vocabularyId);
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
                                    vre.getRelatedEntityId()));
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
        // is the "current version" of the vocabulary, if it has one.
        addFieldsForCurrentVersion(vocabularyId, document);
        return document;
    }

    /** Add the fields for the current version of the vocabulary.
     * @param vocabularyId The vocabulary ID of the vocabulary. Used to
     *      fetch other database entities.
     * @param document The Solr document being generated. This method
     *      adds fields to it.
     */
    private static void addFieldsForCurrentVersion(final Integer vocabularyId,
            final SolrInputDocument document) {
        List<Version> versions =
                VersionDAO.getCurrentVersionListForVocabulary(vocabularyId);
        // The widgetable flag is always added to the Solr document.
        // Assume false unless/until proven otherwise.
        boolean widgetable = false;
        ArrayList<String> concepts = new ArrayList<>();
        // Find the current version, if any.
        Version currentVersion = null;
        for (Version version : versions) {
            if (version.getStatus() == VersionStatus.CURRENT) {
                currentVersion = version;
                break;
            }
        }
        if (currentVersion != null) {
            // Now add the fields that depend on this.
            Integer currentVersionId = currentVersion.getVersionId();
            ArrayList<String> accessList = new ArrayList<>();
            ArrayList<String> formatList = new ArrayList<>();
            List<VersionArtefact> conceptLists =
                    VersionArtefactDAO.
                    getCurrentVersionArtefactListForVersionByType(
                            currentVersionId,
                            VersionArtefactType.CONCEPT_LIST);
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
                    getCurrentAccessPointListForVersion(currentVersionId);
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

    /* Methods for communicating with Solr.
     * Note: for single additions/deletions, there are no explicit commits.
     * We rely on the updateHandler.autoSoftCommit.maxTime setting
     * made by CreateSchema.
     */

    /** Index a current vocabulary in Solr.
     * @param vocabularyId The vocabulary ID of the vocabulary to be
     *      added to the Solr index.
     * @throws IOException If the Solr API generated an IOException.
     * @throws SolrServerException If the Solr API generated a
     *      SolrServerException.
     * @throws RemoteSolrException If there is a problem communicating with
     *      Zookeeper.
     */
    public static void indexVocabulary(final int vocabularyId)
            throws IOException, SolrServerException, RemoteSolrException {
        Vocabulary vocabulary =
                VocabularyDAO.getCurrentVocabularyByVocabularyId(vocabularyId);
        if (vocabulary == null) {
            // For now, do nothing. Maybe revisit this decision later,
            // e.g., to throw an exception in this case.
            return;
        }
        SolrInputDocument document = createSolrDocument(vocabulary);
        try {
            SOLR_CLIENT.add(document);
        } catch (IOException | SolrServerException | RemoteSolrException e) {
            LOGGER.error("Exception when adding document to Solr index", e);
            throw e;
        }
    }

    /** Index all current vocabularies of a vocabulary in Solr.
     * @throws IOException If the Solr API generated an IOException.
     * @throws SolrServerException If the Solr API generated a
     *      SolrServerException.
     * @throws RemoteSolrException If there is a problem communicating with
     *      Zookeeper.
     */
    public static void indexAllVocabularies()
            throws IOException, SolrServerException, RemoteSolrException {
        List<Vocabulary> allVocabularies =
                VocabularyDAO.getAllCurrentVocabulary();
        List<SolrInputDocument> documents = new ArrayList<>();
        for (Vocabulary vocabulary : allVocabularies) {
            documents.add(createSolrDocument(vocabulary));
        }
        try {
            // In this case, we do do a commit immediately (by specifying 0
            // as the second parameter).
            SOLR_CLIENT.add(documents, 0);
        } catch (IOException | SolrServerException | RemoteSolrException e) {
            LOGGER.error("Exception when adding document to Solr index", e);
            throw e;
        }
    }


    /** Remove the current version of a vocabulary from the Solr index.
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
            SOLR_CLIENT.deleteById(Integer.toString(vocabularyId));
        } catch (IOException | SolrServerException | RemoteSolrException e) {
            LOGGER.error("Exception when removing document from Solr index", e);
            throw e;
        }
    }

    /** Remove all documents from the Solr index.
     * @throws IOException If the Solr API generated an IOException.
     * @throws SolrServerException If the Solr API generated a
     *      SolrServerException.
     * @throws RemoteSolrException If there is a problem communicating with
     *      Zookeeper.
     */
    public static void unindexAllVocabularies()
            throws IOException, SolrServerException, RemoteSolrException {
        try {
            // Delete by matching all documents.
            // In this case, we do do a commit immediately (by specifying 0
            // as the second parameter).
            SOLR_CLIENT.deleteByQuery("*:*", 0);
        } catch (IOException | SolrServerException | RemoteSolrException e) {
            LOGGER.error("Exception when removing all documents "
                    + "from Solr index", e);
            throw e;
        }
    }

}
