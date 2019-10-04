/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.workflow.provider.transform;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.io.FileUtils;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.SKOS;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.UnsupportedRDFormatException;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.dao.AccessPointDAO;
import au.org.ands.vocabs.registry.db.dao.RelatedEntityDAO;
import au.org.ands.vocabs.registry.db.dao.VersionArtefactDAO;
import au.org.ands.vocabs.registry.db.dao.VocabularyRelatedEntityDAO;
import au.org.ands.vocabs.registry.db.entity.AccessPoint;
import au.org.ands.vocabs.registry.db.entity.RelatedEntity;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.VersionArtefact;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.db.entity.VocabularyRelatedEntity;
import au.org.ands.vocabs.registry.db.internal.ApSissvoc;
import au.org.ands.vocabs.registry.db.internal.VersionJson;
import au.org.ands.vocabs.registry.db.internal.VocabularyJson;
import au.org.ands.vocabs.registry.db.internal.VocabularyJson.Subjects;
import au.org.ands.vocabs.registry.enums.AccessPointType;
import au.org.ands.vocabs.registry.enums.RelatedEntityRelation;
import au.org.ands.vocabs.registry.enums.SubtaskOperationType;
import au.org.ands.vocabs.registry.enums.TaskStatus;
import au.org.ands.vocabs.registry.enums.VersionArtefactType;
import au.org.ands.vocabs.registry.enums.VersionStatus;
import au.org.ands.vocabs.registry.solr.EntityIndexer;
import au.org.ands.vocabs.registry.solr.FieldConstants;
import au.org.ands.vocabs.registry.workflow.provider.DefaultPriorities;
import au.org.ands.vocabs.registry.workflow.provider.WorkflowProvider;
import au.org.ands.vocabs.registry.workflow.provider.transform.utils.PredicateInfo;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;
import au.org.ands.vocabs.registry.workflow.tasks.TaskRunner;
import au.org.ands.vocabs.registry.workflow.tasks.TaskUtils;
import au.org.ands.vocabs.registry.workflow.tasks.VersionArtefactUtils;

/** Transform provider for generating a version artefact which is a
 * file in JSON format, which is an array of Solr documents, each
 * of which represents an RDF resource. (If the version has status
 * current, any vocabulary-level top concepts are also included
 * in the array.)
 * For now, this assumes a vocabulary encoded using SKOS. */
public class ResourceDocsTransformProvider implements WorkflowProvider {

    /** Logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Prefix for keys used for results that say that a file could
     * not be parsed. */
    public static final String PARSE_PREFIX = "parse-";

    /** Special value to use for the "rdf_type" field to indicate
     * that there is no RDF type: this is a vocabulary-level,
     * manually-entered top concept.
     */
    private static final String NO_RDF_TYPE = "NONE";

    /** Filename to use for the files in which the generated
     * Solr documents are stored. */
    private static final String RESOURCE_DOCS_JSON = "resource_docs.json";

    /** Array of resource types of interest. */
    private static Set<String> resourceTypes = new HashSet<>();

    static {
        resourceTypes.add(NO_RDF_TYPE);
        resourceTypes.add(SKOS.CONCEPT.stringValue());
        resourceTypes.add(SKOS.CONCEPT_SCHEME.stringValue());
        resourceTypes.add(SKOS.COLLECTION.stringValue());
        resourceTypes.add(SKOS.ORDERED_COLLECTION.stringValue());
    };

    /** A map of predicates to take note of, i.e., for which we will
     * include a field in the generated Solr document.
     * Each key is a predicate IRI; the corresponding value is the
     * information we need about that predicate in order to determine
     * how that predicate should be treated. */
    private static HashMap<URI, PredicateInfo> predicatesInfo =
            new HashMap<>();

    /* For predicates that have mayHaveMultipleObjects,
     * the field in the generated Solr document will be an array.
     * (So the Solr schema had better define the field as multiValued!)
     * For predicates that have mayHaveLanguageSpecificObjects,
     * a generated Solr document may have a language-specific
     * field. E.g., since skos:prefLabel has this setting, we may
     * generate a Solr document with fields "skos_prefLabel-en",
     * "skos_prefLabel-fr", etc. */

    static {
        predicatesInfo.put(RDF.TYPE,
                new PredicateInfo(FieldConstants.RDF_TYPE, true, false));
        predicatesInfo.put(RDFS.LABEL,
                new PredicateInfo(FieldConstants.RDFS_LABEL, false, true));
        predicatesInfo.put(DCTERMS.TITLE,
                new PredicateInfo(FieldConstants.DCTERMS_TITLE, false, true));
        predicatesInfo.put(DCTERMS.DESCRIPTION,
                new PredicateInfo(FieldConstants.DCTERMS_DESCRIPTION,
                        false, true));
        predicatesInfo.put(SKOS.PREF_LABEL,
                new PredicateInfo(FieldConstants.SKOS_PREFLABEL, false, true));
        predicatesInfo.put(SKOS.ALT_LABEL,
                new PredicateInfo(FieldConstants.SKOS_ALTLABEL, false, true));
        predicatesInfo.put(SKOS.HIDDEN_LABEL,
                new PredicateInfo(FieldConstants.SKOS_HIDDENLABEL,
                        false, true));
        predicatesInfo.put(SKOS.NOTATION,
                new PredicateInfo(FieldConstants.SKOS_NOTATION, false, false));
        predicatesInfo.put(SKOS.DEFINITION,
                new PredicateInfo(FieldConstants.SKOS_DEFINITION, false, true));
//        predicatesInfo.put(,
//                new PredicateInfo());
    }

    // Private convenience fields, initialized by transform(),
    // and used by methods that it invokes.

    /** The Vocabulary entity of the TaskInfo for the transform task. */
    private Vocabulary vocabulary;

    /** The vocabulary Id of the vocabulary being transformed. */
    private int vocabularyId;

    /** The vocabulary Id of the vocabulary being transformed, converted
     * to a String. */
    private String vocabularyIdString;

    /** The owner of the vocabulary being transformed. */
    private String owner;

    /** The timestamp of the last update of the vocabulary being
     * transformed, expressed in the format expected by Solr. */
    private String lastUpdated;

    /** The contents of the data field of the vocabulary being transformed. */
    private VocabularyJson vocabularyJson;

    /** The vocabulary title of the vocabulary being transformed. */
    private String vocabularyTitle;

    /** The subject labels defined in the vocabulary being transformed. */
    private List<String> subjectLabels = new ArrayList<>();

    /** The publishers of the vocabulary being transformed. */
    private ArrayList<String> publishers = new ArrayList<>();

    /** The primary language of the vocabulary being transformed. */
    private String primaryLanguage;

    /** The Version entity of the TaskInfo for the transform task. */
    private Version version;

    /** The version Id of the vocabulary being transformed. */
    private int versionId;

    /** The version Id of the vocabulary being transformed, converted
     * to a String. */
    private String versionIdString;

    /** The version status of the version being transformed. */
    private String versionStatus;

    /** The version release date of the version being transformed. */
    private String versionReleaseDate;

    /** The contents of the data field of the version being transformed. */
    private VersionJson versionJson;

    /** The version title of the version being transformed. */
    private String versionTitle;

    /** A list of keys to use to select a title for a resource.
     * Based on primary language.
     */
    private ArrayList<String> titleKeys = new ArrayList<>();

    /** The SISSVoc endpoint of the version being transformed, or null,
     * if there isn't one. */
    private String sissvocEndpoint = null;

    /** Create/update the ResourceDocs version artefact for the version.
     * @param taskInfo The top-level TaskInfo for the subtask.
     * @param subtask The subtask to be performed.
     */
    public final void transform(final TaskInfo taskInfo,
            final Subtask subtask) {
        // First, initialize all of our private convenience fields.
        initializeConvenienceFields(taskInfo);
        ResourceHandler resourceHandler = new ResourceHandler();

        List<Path> pathsToProcess =
                TaskUtils.getPathsToProcessForVersion(taskInfo);
        for (Path entry: pathsToProcess) {
            try {
                RDFFormat format = Rio.getParserFormatForFileName(
                        entry.toString());
                RDFParser rdfParser = Rio.createParser(format);
                rdfParser.setRDFHandler(resourceHandler);
                LOGGER.debug("Reading RDF:" + entry.toString());
                FileInputStream is = new FileInputStream(entry.toString());
                rdfParser.parse(is, entry.toString());
            } catch (DirectoryIteratorException
                    | IOException
                    | RDFParseException
                    | RDFHandlerException
                    | UnsupportedRDFormatException ex) {
                // Hmm, don't register an error, but keep going.
                //    subtask.setStatus(TaskStatus.ERROR);
                // But do log the parse error for this file.
                subtask.addResult(PARSE_PREFIX + entry.getFileName(),
                        "Exception in ResourceDocsTransform while Parsing RDF");
                LOGGER.error("Exception in ResourceDocsTransform "
                        + "while Parsing RDF:", ex);
            }
        }

        HashMap<String, HashSetValuedHashMap<String, Object>> resourceMap =
                resourceHandler.getResourceMap();

        addTopConcepts(resourceMap);

        if (!resourceMap.isEmpty()) {
            String resultFileName = TaskUtils.getTaskOutputPath(taskInfo, true,
                    RESOURCE_DOCS_JSON);
            try {
                // Jackson won't serialize a HashSetValuedHashMap,
                // so transform it into something it _will_.
                ArrayList<HashMap<String, Object>> resources =
                        transformResourceMap(resourceMap);
                File out = new File(resultFileName);
                FileUtils.writeStringToFile(out,
                        JSONSerialization.serializeObjectAsJsonString(
                                resources),
                        StandardCharsets.UTF_8);
                VersionArtefactUtils.createResourceDocsVersionArtefact(
                        taskInfo, resultFileName);
            } catch (IOException ex) {
                subtask.setStatus(TaskStatus.ERROR);
                subtask.addResult(TaskRunner.ERROR,
                        "Exception in ResourceDocsTransform while Parsing RDF");
                LOGGER.error("Exception in ResourceDocsTransform "
                        + "generating result:", ex);
                return;
            }
        } else {
            // Clear out any existing VA, because we did in fact succeed,
            // and we don't want to leave around any previous non-empty VA.
            untransform(taskInfo, subtask);
        }
        subtask.setStatus(TaskStatus.SUCCESS);
    }

    /** Initialize the convenience fields.
     * @param taskInfo The top-level TaskInfo for the subtask.
     */
    public void initializeConvenienceFields(final TaskInfo taskInfo) {
        EntityManager em = taskInfo.getEm();
        vocabulary = taskInfo.getVocabulary();
        vocabularyId = vocabulary.getVocabularyId();
        vocabularyIdString = Integer.toString(vocabularyId);
        owner = vocabulary.getOwner();
        lastUpdated = EntityIndexer.localDateTimeToString(
                vocabulary.getStartDate());
        vocabularyJson = JSONSerialization.deserializeStringAsJson(
                vocabulary.getData(), VocabularyJson.class);
        vocabularyTitle = vocabularyJson.getTitle();
        // Subjects
        List<Subjects> subjects = vocabularyJson.getSubjects();
        if (!subjects.isEmpty()) {
            for (Subjects subject : subjects) {
                subjectLabels.add(subject.getLabel());
            }
        }
        // Publishers
        // First get VocabularyRelatedEntity objects.
        MultivaluedMap<Integer, VocabularyRelatedEntity> vreMap =
                VocabularyRelatedEntityDAO.
                getCurrentVocabularyRelatedEntitiesForVocabulary(em,
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
                            getCurrentRelatedEntityByRelatedEntityId(em,
                                    vre.getRelatedEntityId()));
                }
            }
        }
        if (!relatedEntities.isEmpty()) {
            for (RelatedEntity re : relatedEntities) {
                publishers.add(re.getTitle());
            }
        }
        primaryLanguage = vocabularyJson.getPrimaryLanguage();
        version = taskInfo.getVersion();
        versionId = version.getVersionId();
        versionIdString = Integer.toString(versionId);
        versionStatus = version.getStatus().toString();
        versionReleaseDate = version.getReleaseDate();
        versionJson = JSONSerialization.deserializeStringAsJson(
                version.getData(), VersionJson.class);
        versionTitle = versionJson.getTitle();
        // Create list of keys to use from which to pick out
        // the value to be used as the title for each resource.
        // The values of the list are used in sequence. If a value
        // is found for the key, stop, and use that value as the title.
        // We fall back to TOP_CONCEPT and IRI: every resource is
        // guaranteed to have exactly one of those keys!
        titleKeys.add(FieldConstants.SKOS_PREFLABEL + "-" + primaryLanguage);
        titleKeys.add(FieldConstants.SKOS_ALTLABEL + "-" + primaryLanguage);
        titleKeys.add(FieldConstants.SKOS_PREFLABEL);
        titleKeys.add(FieldConstants.SKOS_ALTLABEL);
        titleKeys.add(FieldConstants.SKOS_PREFLABEL + "-en");
        titleKeys.add(FieldConstants.SKOS_ALTLABEL + "-");
        // Possible future work: at this point, can we handle a fallback to
        // skos_prefLabel-*, etc., i.e., a value for _any_ language?
        titleKeys.add(FieldConstants.RDFS_LABEL + "-" + primaryLanguage);
        titleKeys.add(FieldConstants.RDFS_LABEL);
        titleKeys.add(FieldConstants.RDFS_LABEL + "-en");
        titleKeys.add(FieldConstants.DCTERMS_TITLE + "-" + primaryLanguage);
        titleKeys.add(FieldConstants.DCTERMS_TITLE);
        titleKeys.add(FieldConstants.DCTERMS_TITLE + "-en");
        titleKeys.add(FieldConstants.TOP_CONCEPT);
        titleKeys.add(FieldConstants.IRI);

        // Now get the value we want to use for the sissvoc_endpoint field.
        List<AccessPoint> accessPoints = AccessPointDAO.
                getCurrentAccessPointListForVersionByType(versionId,
                        AccessPointType.SISSVOC, em);
        if (accessPoints != null && accessPoints.size() > 0) {
            ApSissvoc apSissvoc = JSONSerialization.
                    deserializeStringAsJson(
                            accessPoints.get(0).getData(), ApSissvoc.class);
            sissvocEndpoint = apSissvoc.getUrlPrefix();
        }
    }

    /** Add entries into the resource map for the vocabulary's
     * top concept metadata. But only do this if this version
     * has status "current".
     * @param resourceMap The existing resource map, into which
     *      entries are to be added for the top concepts.
     */
    private void addTopConcepts(
            final HashMap<String, HashSetValuedHashMap<String, Object>>
            resourceMap) {
        if (version.getStatus() != VersionStatus.CURRENT) {
            return;
        }
        List<String> topConcepts = vocabularyJson.getTopConcepts();
        if (topConcepts != null) {
            for (String topConcept : topConcepts) {
                HashSetValuedHashMap<String, Object> concept =
                        new HashSetValuedHashMap<>();
                resourceMap.put("TC_" + topConcept, concept);
                // Use double-underscore to distinguish the format
                // of IDs from the IRI-based IDs.
                concept.put(FieldConstants.ID,
                        versionIdString + "__" + topConcept);
                concept.put(FieldConstants.TOP_CONCEPT, topConcept);
                concept.put(FieldConstants.RDF_TYPE, NO_RDF_TYPE);
                // NB: use integer value for version_id, to allow sorting.
                concept.put(FieldConstants.VERSION_ID, versionId);
                concept.put(FieldConstants.VERSION_TITLE, versionTitle);
                concept.put(FieldConstants.VERSION_RELEASE_DATE,
                        versionReleaseDate);
                concept.put(FieldConstants.VOCABULARY_ID, vocabularyIdString);
                concept.put(FieldConstants.VOCABULARY_TITLE, vocabularyTitle);
                concept.put(FieldConstants.OWNER, owner);
                concept.put(FieldConstants.LAST_UPDATED, lastUpdated);
                concept.put(FieldConstants.SUBJECT_LABELS, subjectLabels);
                concept.put(FieldConstants.PUBLISHER, publishers);
                concept.put(FieldConstants.STATUS, versionStatus);
                // No SISSVoc endpoint in this case.
            }
        }
    }

    /** Transform the computed resource map into a format that can
     * be serialized. Filtering is applied, so that the result only
     * includes resources of the types in which we are interested.
     * @param resourceMap The resource map, as computed.
     * @return A filtered resource map that contains only resources
     *      of the types in which we are interested, in a format
     *      that can be serialized.
     */
    private ArrayList<HashMap<String, Object>> transformResourceMap(
            final HashMap<String, HashSetValuedHashMap<String, Object>>
            resourceMap) {
        ArrayList<HashMap<String, Object>> resources = new ArrayList<>();
        for (HashSetValuedHashMap<String, Object> resource
                : resourceMap.values()) {
            Set<Object> types = resource.get(FieldConstants.RDF_TYPE);
            if (types != null
                    && CollectionUtils.containsAny(types, resourceTypes)) {
                // Intersect the types we got with the types we care about.
                // This strips out, e.g., rdfs:Resource.
                types.retainAll(resourceTypes);
                HashMap<String, Object> mappedResource = new HashMap<>();
                for (String key : resource.keySet()) {
                    Object[] values = resource.get(key).toArray();
                    if (values.length == 1) {
                        mappedResource.put(key, values[0]);
                    } else {
                        mappedResource.put(key, values);
                    }
                }
                // Now assign a value for TITLE, the "one true" title
                // of the concept, so that we can sort on it,
                // and which we present as the "header" of search results.
                // Take into account SKOS_PREFLABEL, SKOS_ALTLABEL,
                // RDFS_LABEL, TOP_CONCEPT. For each multilingual field,
                // try the vocabulary's primary language, then fall back
                // to a value without a language tag, then to English.
                // Fall back to IRI in the last instance.
                // We initialize title to null to keep Java happy,
                // but we have constructed titleKeys to ensure that
                // there always _will_ be a value assigned to title
                // in the loop.
                String title = null;
                // The value of titleKeys
                for (String titleKey : titleKeys) {
                    String value = (String) mappedResource.get(titleKey);
                    if (value != null) {
                        title = value;
                        break;
                    }
                }
                mappedResource.put(FieldConstants.TITLE, title);
                resources.add(mappedResource);
            }
        }
        return resources;
    }

    /** RDF Handler to extract prefLabels, notation, and use broader
     * and narrow properties to construct a list-like structure. */
    class ResourceHandler extends RDFHandlerBase {

        /** Map from resource IRI to a map that maps
         * property name to the property value(s). */
        private HashMap<String, HashSetValuedHashMap<String, Object>>
        resourceMap = new HashMap<>();

        @Override
        public void handleStatement(final Statement st) {
            Resource subject = st.getSubject();
            URI predicate = st.getPredicate();
            Value object = st.getObject();
            HashSetValuedHashMap<String, Object> concept =
                    resourceMap.get(subject.stringValue());
            if (concept == null) {
                concept = new HashSetValuedHashMap<>();
                resourceMap.put(subject.stringValue(), concept);
                concept.put(FieldConstants.ID,
                        versionIdString + "_"
                        + subject.stringValue());
                concept.put(FieldConstants.LAST_UPDATED, lastUpdated);
                // NB: use integer value for version_id, to allow sorting.
                concept.put(FieldConstants.VERSION_ID, versionId);
                concept.put(FieldConstants.VERSION_TITLE, versionTitle);
                concept.put(FieldConstants.VERSION_RELEASE_DATE,
                        versionReleaseDate);
                concept.put(FieldConstants.IRI, subject.stringValue());
                concept.put(FieldConstants.VOCABULARY_ID, vocabularyIdString);
                concept.put(FieldConstants.VOCABULARY_TITLE, vocabularyTitle);
                concept.put(FieldConstants.OWNER, owner);
                concept.put(FieldConstants.SUBJECT_LABELS, subjectLabels);
                concept.put(FieldConstants.PUBLISHER, publishers);
                concept.put(FieldConstants.STATUS, versionStatus);
                if (sissvocEndpoint != null) {
                    concept.put(FieldConstants.SISSVOC_ENDPOINT,
                            sissvocEndpoint);
                }
            }
            PredicateInfo predicateInfo = predicatesInfo.get(predicate);
            if (predicateInfo == null) {
                // We're not interested in this predicate at all.
                return;
            }
            String field = predicateInfo.getFieldName();
            if (predicateInfo.isMayHaveMultipleObjects()) {
                // Add the object; there may already be one for this field.
                concept.put(field, object.stringValue());
                // And we're done.
                return;
            }
            if (predicateInfo.isMayHaveLanguageSpecificObjects()) {
                if (object instanceof Literal) {
                    Literal objectLiteral = (Literal) object;
                    String lang = objectLiteral.getLanguage();
                    if (lang == null) {
                        // No language tag.
                        // Remove any existing object.
                        concept.remove(field);
                        concept.put(field, object.stringValue());
                    } else {
                        // There is a language tag. Separate using
                        // "-". NB: very important that "-" not otherwise
                        // occur in the field names, so that you can
                        // use wildcards in field names without worrying
                        // about inadvertently matching fields you don't
                        // want. E.g., "prefLabel-*" won't match
                        // against "prefLabel_phrase-en".
                        String fieldWithLang = field + "-" + lang;
                        // Remove any existing object.
                        concept.remove(fieldWithLang);
                        concept.put(fieldWithLang, object.stringValue());
                    }
                } else {
                    LOGGER.info("Predicate of interest has "
                            + "non-literal object: "
                            + "s: " + subject.stringValue()
                            + "; p: " + predicate.stringValue()
                            + "; o: " + object.stringValue());
                }
                // And we're done.
                return;
            }
            // Fall-through: there may be only one.
            // Remove any existing object.
            concept.remove(field);
            concept.put(field, object.stringValue());
        }

        /** Getter for resource list.
         * @return The completed resource map. */
        public HashMap<String, HashSetValuedHashMap<String, Object>>
        getResourceMap() {
            return resourceMap;
        }
    }

    /** Remove the ResourceDocs version artefact for the version.
     * NB: This method will also be invoked by
     * {@link #transform(TaskInfo, Subtask)}, in the case of
     * success that nevertheless produces an empty result.
     * @param taskInfo The top-level TaskInfo for the subtask.
     * @param subtask The subtask to be performed.
     */
    public final void untransform(final TaskInfo taskInfo,
            final Subtask subtask) {
        // Remove the ResourceDocs version artefact.
        List<VersionArtefact> vas = VersionArtefactDAO.
                getCurrentVersionArtefactListForVersionByType(
                        taskInfo.getVersion().getVersionId(),
                        VersionArtefactType.RESOURCE_DOCS,
                        taskInfo.getEm());
        for (VersionArtefact va : vas) {
            // We _don't_ delete the file. But if we did:
            /*
            VaResourceDocs vaResourceDocs =
                    JSONSerialization.deserializeStringAsJson(
                            va.getData(), VaResourceDocs.class);
            Files.deleteIfExists(Paths.get(vaResourceDocs.getPath()));
            */
            TemporalUtils.makeHistorical(va, taskInfo.getNowTime());
            va.setModifiedBy(taskInfo.getModifiedBy());
            VersionArtefactDAO.updateVersionArtefact(taskInfo.getEm(), va);
        }
        subtask.setStatus(TaskStatus.SUCCESS);
    }

    /** {@inheritDoc} */
    @Override
    public Integer defaultPriority(final SubtaskOperationType operationType) {
        switch (operationType) {
        case INSERT:
        case PERFORM:
            return DefaultPriorities.
                    DEFAULT_TRANSFORM_BEFORE_IMPORTER_INSERT_PRIORITY;
        case DELETE:
            return DefaultPriorities.
                    DEFAULT_TRANSFORM_BEFORE_IMPORTER_DELETE_PRIORITY;
        default:
            // Unknown operation type!
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void doSubtask(final TaskInfo taskInfo, final Subtask subtask) {
        switch (subtask.getOperation()) {
        case INSERT:
        case PERFORM:
            transform(taskInfo, subtask);
            break;
        case DELETE:
            untransform(taskInfo, subtask);
            break;
        default:
            LOGGER.error("Unknown operation!");
            break;
        }
    }

}
