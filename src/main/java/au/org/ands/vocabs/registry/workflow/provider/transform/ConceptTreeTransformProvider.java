/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.workflow.provider.transform;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.db.dao.VersionArtefactDAO;
import au.org.ands.vocabs.registry.db.entity.BrowseFlagsParsed;
import au.org.ands.vocabs.registry.db.entity.Version;
import au.org.ands.vocabs.registry.db.entity.VersionArtefact;
import au.org.ands.vocabs.registry.db.entity.Vocabulary;
import au.org.ands.vocabs.registry.db.internal.VersionJson;
import au.org.ands.vocabs.registry.db.internal.VocabularyJson;
import au.org.ands.vocabs.registry.enums.BrowseFlag;
import au.org.ands.vocabs.registry.enums.SubtaskOperationType;
import au.org.ands.vocabs.registry.enums.TaskStatus;
import au.org.ands.vocabs.registry.enums.VersionArtefactType;
import au.org.ands.vocabs.registry.workflow.provider.DefaultPriorities;
import au.org.ands.vocabs.registry.workflow.provider.WorkflowProvider;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;
import au.org.ands.vocabs.registry.workflow.tasks.TaskRunner;
import au.org.ands.vocabs.registry.workflow.tasks.TaskUtils;
import au.org.ands.vocabs.registry.workflow.tasks.VersionArtefactUtils;

/*
 *
 * THIS IS THE NEW IMPLEMENTATION of the back end of the
 * browse tree, replacing JsonTreeTransformProvider.
 *
 */



/** Transform provider for generating a forest-like representation of the
 * concepts as JSON.
 *
 * This class replaces {@link JsonTreeTransformProvider}.
 *
 * The transform assumes a vocabulary encoded using SKOS.
 * The resulting output is sorted at each level. The sort key is
 * user-determined: either by prefLabel, case-insensitively, or by
 * notation. In some cases (as explained below),
 * an alternative sort order is also represented within the result.
 * When sorting by prefLabel, concepts without prefLabels are
 * gathered at the end, sorted by IRI.
 *
 * For the following vocabulary data:
 * <pre>{@literal @}prefix skos: &lt;http://www.w3.org/2004/02/skos/core#&gt; .
 *
 * &lt;http://url1&gt; a skos:Concept ;
 *   skos:prefLabel "Label 1" ;
 *   skos:definition "Definition 1" ;
 *   skos:notation "1" .
 *
 * &lt;http://uri1/narrower1&gt; a skos:Concept ;
 *   skos:prefLabel "Label 1.1" ;
 *   skos:definition "Definition 1.1" ;
 *   skos:notation "1.1" .
 *
 * &lt;http://uri1/narrower1/narrower1&gt; a skos:Concept ;
 *   skos:prefLabel "Label 1.1.1" ;
 *   skos:definition "Definition 1.1.1" ;
 *   skos:notation "1.1.1" .
 *
 * &lt;http://url2&gt; a skos:Concept ;
 *   skos:prefLabel "Label 2" ;
 *   skos:definition "Definition 2" ;
 *   skos:notation "2" .
 *
 * &lt;http://noPrefLabel1&gt; a skos:Concept ;
 *   skos:definition "Concepts without preflabels go at the end ..." .
 *
 * &lt;http://noPrefLabel2&gt; a skos:Concept ;
 *   skos:definition "... sorted by IRI" .
 *
 * &lt;http://url1&gt; skos:narrower &lt;http://uri1/narrower1&gt; .
 *
 * &lt;http://uri1/narrower1&gt; skos:narrower
 *   &lt;http://uri1/narrower1/narrower1&gt; .
 *
 * &lt;http://url2&gt; skos:narrower &lt;http://uri1/narrower1/narrower1&gt; .
 * </pre>
 *
 * here is the structure of the generated JSON:
 * <pre>
 * {
 *   "language": "en",
 *   "maySortByNotation": true,
 *   "notationFormat": "notationDotted",
 *   "defaultSortByNotation": false,
 *   "forest": [
 *     {
 *       "type": "concept",
 *       "iri": "http://url1",
 *       "prefLabel": "Label 1",
 *       "definition": "Definition 1",
 *       "notation": "1",
 *       "notationSortOrder": 0,
 *       "narrower": [
 *         {
 *           "type": "concept",
 *           "iri": "http://uri1/narrower1",
 *           "prefLabel": "Label 1.1",
 *           "definition": "Definition 1.1",
 *           "notation": "1.1",
 *           "notationSortOrder": 0,
 *           "narrower": [
 *             {
 *               "type": "concept",
 *               "iri": "http://uri1/narrower1/narrower1",
 *               "prefLabel": "Label 1.1.1",
 *               "definition": "Definition 1.1.1",
 *               "notation": "1.1.1",
 *               "notationSortOrder": 0
 *             }
 *           ]
 *         }
 *       ]
 *     },
 *     {
 *       "type": "concept",
 *       "iri": "http://url2",
 *       "prefLabel": "Label 2",
 *       "definition": "Definition 2",
 *       "notation": "2",
 *       "notationSortOrder": 1,
 *       "narrower": [
 *         {
 *           "type": "concept_ref",
 *           "iri": "http://uri1/narrower1/narrower1",
 *           "prefLabel": "Label 1.1.1",
 *           "notation": "1.1.1",
 *           "notationSortOrder": 0
 *         }
 *       ]
 *     },
 *     {
 *       "type": "concept",
 *       "iri": "http://noPrefLabel1",
 *       "definition": "Concepts without preflabels go at the end ...",
 *       "notationSortOrder": 2
 *     },
 *     {
 *       "type": "concept",
 *       "iri": "http://noPrefLabel2",
 *       "definition": "... sorted by IRI",
 *       "notationSortOrder": 3
 *     }
 *   ]
 * }
 * </pre>
 *
 * <p>There is a new key/value pair for each concept. The key is {@code
 * type}, and the value is either {@code "concept"} or {@code
 * "concept_ref"}. If there are no polyhierarchies in the hierarchy, all
 * concepts will have {@code "type": "concept"}. Otherwise, the presence of
 * a concept with {@code "type": "concept_ref"} indicates
 * that this concept was visited more than once during depth-first search
 * (indicating a cross edge), and so this is a
 * "cross-reference" to a concept whose full definition is given elsewhere
 * in the hierarchy. A concept with {@code "type": "concept_ref"}
 * does <em>not</em> include a {@code narrower} key/value pair. In order to
 * find out if the concept has narrower concepts, it is necessary to locate
 * the corresponding concept with the same {@code iri} but with {@code
 * "type": "concept"}, and to see if <em>it</em> has a {@code narrower}
 * key/value pair.</p>
 *
 * <p>There are additional top-level key/value pairs, known (for the
 * time being) as "browse flags". The keys are:</p>
 *
 * <ul>
 *   <li>{@code maySortByNotation}
 *     <ul><li>Boolean</li></ul>
 *   </li>
 *   <li>{@code notationFormat}
 *     <ul>
 *       <li>String; one
 *         of {@code "notationAlpha"}, {@code "notationFloat"},
 *         {@code "notationDotted"}
 *       </li>
 *     </ul>
 *   </li>
 *   <li><code>defaultSortByNotation</code>
 *     <ul><li>Boolean</li></ul>
 *   </li>
 * </ul>
 *
 * <p>If {@code maySortByNotation is true},
 * there will be an additional key/value pair for each concept. The
 * key depends on the value of {@code defaultSortByNotation}.</p>
 *
 * <ul>
 * <li>If the {@code maySortByNotation} key/value pair is present, and set
 * to {@code true}, but the {@code defaultSortByNotation} key/value pair
 * is missing, or is set to {@code false}:
 * <ul>
 * <li>Each node will also have a key/value pair, where the key is
 * {@code notationSortOrder}.</li>
 * </ul>
 * </li>
 * <li>If the {@code defaultSortByNotation} key/value pair is present, and
 * set to {@code true}:
 * <ul>
 * <li>Each node will also have a key/value pair, where the key is
 * {@code prefLabelSortOrder}.</li>
 * </ul></li></ul>
 *
 * The input vocabulary can have its hierarchy specified using either
 * {@code skos:narrower} or {@code skos:broader};
 * missing properties are inferred.
 *
 * Because the SKOS model explicitly allows polyhierarchies and cycles,
 * a depth-first search is performed to compute a depth-first spanning forest.
 *
 * The concepts that have no broader concept are privileged: they are
 * roots of the forest. Then a depth-first search is performed on all roots,
 * identifying the tree edges. When this is complete, do any nodes
 * remain unvisited? If so, one is chosen and added to the set of roots,
 * and a DFS is performed on it. This process is repeated until there
 * are no more unvisited nodes.
 *
 * In the following pseudo code, the elements of Roots are the roots
 * of the spanning forest, and the "children" sets of
 * each node form the edges of the spanning forest. The flag "Cycle" is True
 * if there is a cycle. The flag "OnlyTreeEdges" is True if there are only
 * tree edges: there are neither cycles nor polyhierarchies.
 *
 * Rather than adding "visited" and "active" properties to each node,
 * we use AllNodesNotVisited and NodesActive sets.
 * <pre>
 * Roots = All concepts that have no broader concept
 * AllNodesNotVisited = AllConcepts
 * NodesActive = empty set
 * Cycle = false
 * OnlyTreeEdges = true
 * for each node in AllNodes:
 *   set node.children = empty set
 * for each root in Roots:
 *   DFS(root)
 * if AllNodesNotVisited is empty:
 *   Cycle = true
 * while AllNodesNotVisited is not empty:
 *   select node from AllNodesNotVisited
 *     add node to Roots
 *     DFS(node)
 *
 * DFS(node):
 *   remove node from AllNodesNotVisited
 *   add node to NodesActive
 *   foreach narrower in narrower(node):
 *     if narrower is not in AllNodesNotVisited:
 *       add narrower to node.children (as a Concept)
 *       DFS(narrower)
 *     else:
 *       OnlyTreeEdges = False
 *       if narrower is in NodesActive:
 *         Cycle = true
 *       add narrower to node.children as a reference (ConceptRef)
 *   remove node from NodesActive
 * </pre>
 *
 * This class has a number of nested classes; see also the documentation
 * for those classes.
 */
public class ConceptTreeTransformProvider implements WorkflowProvider {

    /** Logger for this class. */
    private final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Short form of the concept type name. Used both in
     * {@link #typesToLookFor} and
     * {@link ConceptHandler#populateRoots()}. */
    private static final String CONCEPT_SHORT_FORM = "Concept";

    /** A map of SKOS types to take note of. */
    private static HashMap<URI, String> typesToLookFor =
            new HashMap<>();

    static {
        typesToLookFor.put(SKOS.CONCEPT_SCHEME, "ConceptScheme");
        typesToLookFor.put(SKOS.CONCEPT, CONCEPT_SHORT_FORM);
        typesToLookFor.put(SKOS.COLLECTION, "Collection");
        typesToLookFor.put(SKOS.ORDERED_COLLECTION, "OrderedCollection");
    }

    /** Filename to use for the output of the transform.
     * The use of "2" in the name is to make explicit that this is the
     * second version of the format of the JSON data. */
    private static final String CONCEPTS_TREE_FILENAME = "concepts_tree2.json";

    /** Prefix for keys used for results that say that a file could
     * not be parsed. */
    public static final String PARSE_PREFIX = "parse-";

    /** Key to use for a result that says that the concepts tree was
     * not generated. */
    public static final String CONCEPTS_TREE_NOT_PROVIDED =
            "concepts-tree-not-provided";

    /** Key to use for a result that says that the concepts tree does
     * not contain information about notations, because of an exception.
     */
    private static final String CONCEPTS_TREE_NO_NOTATIONS =
            "concepts-tree-no-notations";

    /** Create/update the ConceptTree version artefact for the version.
     * @param taskInfo The top-level TaskInfo for the subtask.
     * @param subtask The subtask to be performed.
     */
    @SuppressWarnings("checkstyle:MethodLength")
    public final void transform(final TaskInfo taskInfo,
            final Subtask subtask) {
        // Reset any existing subtask status (i.e., in case this
        // is in a task that's being re-run).
        subtask.setStatus(null);

        Vocabulary vocabulary = taskInfo.getVocabulary();
        VocabularyJson vocabularyJson =
                JSONSerialization.deserializeStringAsJson(vocabulary.getData(),
                        VocabularyJson.class);
        String primaryLanguage = vocabularyJson.getPrimaryLanguage();
        Version version = taskInfo.getVersion();
        VersionJson versionJson =
                JSONSerialization.deserializeStringAsJson(version.getData(),
                        VersionJson.class);
        List<BrowseFlag> browseFlags = versionJson.getBrowseFlag();

        BrowseFlagsParsed bfParsed = new BrowseFlagsParsed(browseFlags);

        ConceptHandler conceptHandler = new ConceptHandler(primaryLanguage,
                bfParsed.isDefaultSortByNotation(),
                bfParsed.isMaySortByNotation(),
                bfParsed.getNotationFormat());
        // Parse all input files in the harvest directory, loading
        // the content into conceptHandler.
        List<Path> pathsToProcess =
                TaskUtils.getPathsToProcessForVersion(taskInfo);
        for (Path entry: pathsToProcess) {
            try {
                RDFFormat format = Rio.getParserFormatForFileName(
                        entry.toString());
                RDFParser rdfParser = Rio.createParser(format);
                rdfParser.setRDFHandler(conceptHandler);
                FileInputStream is = new FileInputStream(entry.toString());
                rdfParser.parse(is, entry.toString());
                logger.debug("Reading RDF: " + entry.toString());
            } catch (DirectoryIteratorException
                    | IOException
                    | RDFParseException
                    | RDFHandlerException
                    | UnsupportedRDFormatException ex) {
                subtask.addResult(PARSE_PREFIX + entry.getFileName(),
                        "Exception in ConceptTreeTransform while Parsing RDF");
                logger.error("Exception in ConceptTreeTransform "
                        + "while Parsing RDF:", ex);
            }
        }

        // Extract the result, save in results Set and store in the
        // file system.

        Set<ConceptOrRef> conceptTree = conceptHandler.buildForest();

        if (!conceptTree.isEmpty()) {
            String resultFileNameTree = TaskUtils.getTaskOutputPath(taskInfo,
                    true, CONCEPTS_TREE_FILENAME);
            try {
                // The returned JSON is an object in which the concept forest
                // is the value of one key/value pair, and the
                // other key/value pairs are other metadata.
                // Possible future work: add diagnostic information
                // to the result, e.g., if something went wrong.
                // For now, generate a JSON tree _only_ if there are no
                // cycles.
                // Well, and we now also return alert information if
                // there was an error parsing a notation value.
                if (!conceptHandler.isCycle()) {
                    // Serialize the forest and write to the file system.
                    // Jackson will serialize TreeSets in sorted order of values
                    // (i.e., the Concept objects' prefLabels).
                    File out = new File(resultFileNameTree);
                    ConceptResult conceptResult = new ConceptResult();
                    conceptResult.setForest(conceptTree);
                    conceptResult.setLanguage(primaryLanguage);
                    if (conceptHandler.getNotationException() == null) {
                        // No problem to set flags as specified in the version.
                        conceptResult.setMaySortByNotation(
                                bfParsed.isMaySortByNotation());
                        if (bfParsed.isMaySortByNotation()) {
                            conceptResult.setDefaultSortByNotation(
                                    bfParsed.isDefaultSortByNotation());
                            conceptResult.setNotationFormat(
                                    bfParsed.getNotationFormat());
                        }
                    } else {
                        // There was an exception when parsing a notation.
                        conceptResult.setMaySortByNotation(false);
                        subtask.setStatus(TaskStatus.PARTIAL);
                        subtask.addResult(CONCEPTS_TREE_NO_NOTATIONS,
                                "No notation information because of a "
                                + "parse error.");
                        subtask.addResult(TaskRunner.ALERT_HTML,
                                conceptHandler.getNotationException().
                                getAlertHTML());
                    }
                    FileUtils.writeStringToFile(out,
                            JSONSerialization.serializeObjectAsJsonString(
                                    conceptResult),
                            StandardCharsets.UTF_8);
                    VersionArtefactUtils.createConceptTreeVersionArtefact(
                            taskInfo, resultFileNameTree);
                } else {
                    // We aren't going to provide a concept tree.
                    // However, there might be an existing one left over,
                    // which we must now remove. Call untransform() _here_
                    // (i.e., rather than later), because it sets
                    // the task status, and we want to override that below.
                    untransform(taskInfo, subtask);

                    String reason = "there is a cycle";
                    subtask.setStatus(TaskStatus.PARTIAL);
                    subtask.addResult(CONCEPTS_TREE_NOT_PROVIDED,
                            "No concepts tree "
                            + "provided, because " + reason + ".");
                    subtask.addResult(TaskRunner.ALERT_HTML,
                            "Alert: A cycle was detected "
                            + "in the vocabulary data.<br />The concept browse "
                            + "tree will not be visible for this version ("
                            + version.getSlug() + ")."
                            + "<br />For more information, please see "
                            + "<a target=\"_blank\" "
                            + "href=\"https://documentation."
                            + "ardc.edu.au/display/DOC/Support+for+concept+"
                            + "browsing+within+the+portal\">Portal concept "
                            + "browsing</a>.");
                    logger.error("ConceptTreeTransform: not providing a "
                            + "concept tree because " + reason + ".");
                    // Future work:
                    // write something else, e.g., a JSON string.
                    //    FileUtils.writeStringToFile(out, "something");
                    return;
                }
            } catch (IOException ex) {
                subtask.setStatus(TaskStatus.ERROR);
                subtask.addResult(TaskRunner.ERROR,
                        "IO exception in ConceptTreeTransform while "
                        + "generating result");
                logger.error("IO exception in ConceptTreeTransform "
                        + "generating result:", ex);
                return;
            } catch (Exception ex) {
                // Any other possible cause?
                subtask.setStatus(TaskStatus.ERROR);
                subtask.addResult(TaskRunner.ERROR,
                        "Exception in ConceptTreeTransform while "
                        + "generating result");
                logger.error("Exception in ConceptTreeTransform "
                        + "generating result:", ex);
                return;
            }
        } else {
            // Clear out any existing VA, because we did in fact succeed,
            // and we don't want to leave around any previous non-empty VA.
            untransform(taskInfo, subtask);
        }

        // We may already have set the status to the value we want
        // (i.e., to PARTIAL); if not so, set it to SUCCESS.
        if (subtask.getStatus() == null) {
            subtask.setStatus(TaskStatus.SUCCESS);
        }
    }

    /** Inner class for representing concepts, to be used as
     * values of Sets that store concepts. An instance
     * stores an IRI, its narrower Concepts, and (optional) metadata
     * of the concept: a prefLabel, definition, and notation.
     * Equality, and the value of {@link #toString()}, is based
     * on the IRI.
     * The purpose of this class is to facilitate sorting of
     * the result of this transform based on prefLabels.
     * The class itself is labelled {@code private} to prevent inadvertent
     * instance creation. Various getter methods of this class are
     * therefore also annotated with {@code @SuppressWarnings("unused")},
     * because otherwise a warning is generated for them. These getter
     * methods <i>are</i> used: they are invoked during JSON generation.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class Concept
//        implements Comparable<Concept>, ConceptOrRef {
        extends ConceptOrRef {

        /** The IRI of the concept. */
        private String iri;

        /** The prefLabel of the concept. */
        private String prefLabel;

        /** The definition of the concept. */
        private String definition;

        /** The notation of the concept. */
        private String notation;

        /** The narrower concepts of the concept. */
        private TreeSet<ConceptOrRef> narrower;

        /** Constructor with an IRI specified.
         * @param anIRI The IRI of the concept. Must be non-null.
         * @throws IllegalArgumentException Thrown if {@code anIRI == null}.
         */
        Concept(final String anIRI) {
            if (anIRI == null) {
                throw new IllegalArgumentException("Won't make a Concept "
                        + "with a null IRI");
            }
            iri = anIRI;
        }

        /** {@inheritDoc} */
        @Override
        public String getType() {
            return "concept";
        }

        /** Get the IRI.
         * @return The value of the IRI.
         */
        @Override
        public String getIri() {
            return iri;
        }

        /** The language tag associated with the prefLabel, if there
         * is one; null, otherwise.
         */
        private String prefLabelLanguage;

        /** Set the prefLabel. Call this setter if the prefLabel has no
         * language tag.
         * @param aPrefLabel The value of the prefLabel.
         */
        public void setPrefLabel(final String aPrefLabel) {
            // Always give priority to a prefLabel without a language tag.
            prefLabel = aPrefLabel;
            // Reset this, in case we have already seen a prefLabel
            // with a language tag.
            prefLabelLanguage = null;
        }

        /** Set the prefLabel. Call this setter if the prefLabel has a
         * language tag.
         * @param aPrefLabel The value of the prefLabel.
         * @param aLanguage The language tag of the prefLabel.
         * @param primaryLanguage The primary language of the vocabulary.
         */
        public void setPrefLabel(final String aPrefLabel,
                final String aLanguage,
                final String primaryLanguage) {
            // Give preference to labels in the primary language.
            // That means:
            // 1. If this method is called when there is not already a
            //    prefLabel recorded, then use aPrefLabel/aLanguage.
            // 2. If this method is called when there _is_ already a
            //    prefLabel recorded, but there _is_ a language
            //    recorded, and it is not the primary language.
            //    (This gives "last one wins" behaviour.)
            // 3. Otherwise, leave the existing prefLabel/prefLabelLanguage
            //    values unchanged.

            // Please note the clarification of what is allowed for multiple
            // prefLabels at
            // https://www.w3.org/2006/07/SWD/SKOS/reference/20090811-errata#S14
            // "A resource has no more than one value of skos:prefLabel
            // per language tag, and no more than one value of skos:prefLabel
            // without language tag.".
            // So we make no attempt to specify the behaviour in the case of
            // multiple prefLabels with language tag that is the primary
            // language.
            if (prefLabel == null
                    ||
                    (prefLabelLanguage != null
                        && !primaryLanguage.equals(prefLabelLanguage))
                    ) {
                prefLabel = aPrefLabel;
                prefLabelLanguage = aLanguage;
            }
            // Otherwise, leave the existing prefLabel unchanged.
        }

        /** Get the prefLabel.
         * @return The value of the prefLabel.
         */
        @Override
        @SuppressWarnings("unused")
        public String getPrefLabel() {
            return prefLabel;
        }

        /** Set the definition.
         * @param aDefinition The value of the definition.
         */
        public void setDefinition(final String aDefinition) {
            definition = aDefinition;
        }

        /** Get the definition.
         * @return The value of the definition.
         */
        @SuppressWarnings("unused")
        public String getDefinition() {
            return definition;
        }

        /** Set the notation.
         * @param aNotation The value of the notation.
         */
        public void setNotation(final String aNotation) {
            notation = aNotation;
        }

        /** Get the notation.
         * @return The value of the notation.
         */
        @Override
        @SuppressWarnings("unused")
        public String getNotation() {
            return notation;
        }

        /** Add a narrower concept.
         * @param aNarrower A narrower concept to be added to the set
         *      of narrower concepts.
         */
        public void addNarrower(final ConceptOrRef aNarrower) {
            if (narrower == null) {
                narrower = new TreeSet<>();
            }
            narrower.add(aNarrower);
        }

        /** Get the Set of narrower concepts. Invoked during
         * serialization into JSON.
         * @return The Set of narrower concepts.
         */
        @SuppressWarnings("unused")
        public TreeSet<ConceptOrRef> getNarrower() {
            return narrower;
        }

        /** Set the Set of narrower concepts. Invoked by assignSortOrders
         * if the narrower concepts are being resorted by notation.
         * @param aNarrower The Set of narrower concepts.
         */
        public void setNarrower(final TreeSet<ConceptOrRef> aNarrower) {
            narrower = aNarrower;
        }

        /** The {@link ConceptRef} instance corresponding to this
         * instance, if one has been requested, otherwise null. */
        private ConceptRef conceptRef;

        /** Get the (singleton) {@link ConceptRef} instance correspoding
         * to this instance. If one doesn't already exist, it is created.
         * @return The {@link ConceptRef} instance correspoding
         *      to this instance.
         */
        @JsonIgnore
        public synchronized ConceptRef getConceptRef() {
            if (conceptRef == null) {
                conceptRef = new ConceptRef(this);
            }
            return conceptRef;
        }

        /** {@inheritDoc}
         * Equality test based on IRI. But there should be only one
         * instance of an IRI in a Set or Map of Concepts.
         */
        @Override
        public boolean equals(final Object other) {
            if (other == null || !(other instanceof Concept)) {
                return false;
            }
            Concept otherConcept = (Concept) other;
            return iri.equals(otherConcept.iri);
        }

        /** {@inheritDoc}
         * The hash code returned is that of the IRI.
         */
        @Override
        public int hashCode() {
            return iri.hashCode();
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return iri;
        }

    }

    /** Inner class for representing a reference to a Concept, to be used as
     * values of Sets that store concepts-or-references. Every instance
     * of this class is linked to an existing Concept instance;
     * indeed, this class's only constructor
     * requires that a non-null Concept be passed in as a parameter.
     * The class provides getter methods for IRI, type, prefLabel, and
     * notation (i.e., enough to support sorting),
     * but <i>not</i> for narrower Concepts. The point is to enable
     * serialization into JSON without getting into infinite loops.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class ConceptRef extends ConceptOrRef {

        /** The IRI of the concept. */
        private Concept concept;

        /** Constructor with an IRI specified.
         * @param aConcept The Concept, of which this is a reference.
         *      Must be non-null.
         * @throws IllegalArgumentException Thrown if {@code aConcept == null}.
         */
        ConceptRef(final Concept aConcept) {
            if (aConcept == null) {
                throw new IllegalArgumentException("Won't make a ConceptRef "
                        + "with a null Concept");
            }
            concept = aConcept;
        }

        /** Get the IRI.
         * @return The value of the IRI.
         */
        @Override
        public String getIri() {
            return concept.getIri();
        }

        /* {@inheritDoc} */
        @Override
        public String getType() {
            return "concept_ref";
        }

        /** {@inheritDoc}
         * Equality test based on IRI. But there should be only one
         * instance of an IRI in a Set or Map of Concepts.
         */
        @Override
        public boolean equals(final Object other) {
            if (other == null || !(other instanceof ConceptRef)) {
                return false;
            }
            ConceptRef otherConcept = (ConceptRef) other;
            return getIri().equals(otherConcept.getIri());
        }

        /** {@inheritDoc}
         * The hash code returned is that of the IRI.
         */
        @Override
        public int hashCode() {
            return getIri().hashCode();
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return getIri();
        }

        /** {@inheritDoc} */
        @Override
        public String getPrefLabel() {
            return concept.getPrefLabel();
        }

        /** {@inheritDoc} */
        @Override
        public String getNotation() {
            return concept.getNotation();
        }

    }

    /** An abstract class that is extended by both {@link Concept} and
     * {@link ConceptRef}. The narrower "concepts" of a {@link Concept}
     * are stored in {@link Concept#narrower}, a collection of
     * {@link ConceptOrRef} instances.
     * The natural order of instances
     * ({@link #compareTo(ConceptTreeTransformProvider.ConceptOrRef)})
     * is based on a case-insensitive comparison of the prefLabels.
     */
    private abstract static class ConceptOrRef
    implements Comparable<ConceptOrRef> {

        /** Get the IRI.
         * @return The value of the IRI.
         */
        abstract String getIri();

        /** Get the type of this element, either a concept,
         * or a reference to a concept.
         * @return The value of the type.
         */
        abstract String getType();

        /** Get the prefLabel.
         * @return The value of the prefLabel.
         */
        abstract String getPrefLabel();

        /** Get the notation.
         * @return The value of the notation.
         */
        abstract String getNotation();

        /** The sort order to use, when sorting by prefLabel.
         * The value will only be non-null if sorting by notation
         * is enabled. */
        private Integer prefLabelSortOrder;

        /** Set the sort order to use for this concept-or-reference,
         * when sorting by prefLabel.
         * @param aPrefLabelSortOrder The sort order for this
         *      concept-or-reference.
         */
        public void setPrefLabelSortOrder(final Integer aPrefLabelSortOrder) {
            prefLabelSortOrder = aPrefLabelSortOrder;
        }

        /** Get the sort order to use for this concept-or-reference,
         * when sorting by prefLabel.
         * @return The sort order for this concept-or-reference.
         */
        @SuppressWarnings("unused")
        public Integer getPrefLabelSortOrder() {
            return prefLabelSortOrder;
        }

        /** The sort order to use, when sorting by notation.
         * The value will only be non-null if sorting by notation
         * is enabled. */
        private Integer notationSortOrder;

        /** Set the sort order to use for this concept-or-reference,
         * when sorting by notation.
         * @param aNotationSortOrder The sort order for this
         *      concept-or-reference.
         */
        public void setNotationSortOrder(final Integer aNotationSortOrder) {
            notationSortOrder = aNotationSortOrder;
        }

        /** Get the sort order to use for this concept-or-reference,
         * when sorting by notation.
         * @return The sort order for this concept-or-reference.
         */
        public Integer getNotationSortOrder() {
            return notationSortOrder;
        }

        /** {@inheritDoc}
         * Comparison based first (case-insensitively) on prefLabels,
         * then on IRIs.
         * All Concepts with null prefLabels are sorted at the end
         * (by their IRIs).
         */
        @Override
        public int compareTo(final ConceptOrRef otherConcept) {
            if (otherConcept == null) {
                // NPE required by the contract specified in
                // the Javadocs of Comparable<T>.
                throw new NullPointerException();
            }
            if (getPrefLabel() == null) {
                // This concept has no prefLabel. It will be sorted
                // after all concepts that _do_ have prefLabels.
                if (otherConcept.getPrefLabel() == null) {
                    // Both concepts have null prefLabels, so
                    // sort by their IRIs.
                    return getIri().compareTo(otherConcept.getIri());
                }
                // The other concept has a prefLabel. This concept
                // is sorted after it.
                return 1;
            }
            // This concept has a prefLabel.
            if (otherConcept.getPrefLabel() == null) {
                // The other concept doesn't have a prefLabel. It is
                // sorted after this concept.
                return -1;
            }
            // Both this and otherConcept have prefLabels.
            // Use String case-insensitive comparison on them.
            int prefLabelComparison =
                    getPrefLabel().compareToIgnoreCase(
                            otherConcept.getPrefLabel());
            if (prefLabelComparison != 0) {
                return prefLabelComparison;
            }
            // Identical prefLabels. Fall back to comparing their IRIs.
            return getIri().compareTo(otherConcept.getIri());
        }

    }

    /** RDF Handler to extract prefLabels, notation, definition,
     * and use broader and narrow properties to construct a tree-like
     * structure.
     * Note the use of both HashMaps and TreeSets.
     * TreeSets are used when the sorting of keys is required
     * during serialization;
     * in this case, the values are Concepts.
     * HashMaps are used when the sorting of keys is not required;
     * in this case, the keys are Strings ("prefLabel", "notation",
     * etc.)
     * Note well that all TreeSets are constructed only <i>after</i>
     * all the RDF data has been parsed, so that all {@link Concept}
     * instances have their prefLabel data set, so that insertion
     * of the subsequently generated {@link Concept} instances
     * into a TreeSet (which is based on the
     * {@link ConceptOrRef#compareTo(ConceptTreeTransformProvider.ConceptOrRef)}
     * method) will work correctly. */
    class ConceptHandler extends RDFHandlerBase {

        /** The primary language of the vocabulary. */
        private String primaryLanguage;

        /** Whether the default sort order is by notation (true)
         * or by preferred label (false). */
        private boolean defaultSortByNotation;

        /** In case {@link #defaultSortByNotation} is true, whether
         * users will be offered the ability to sort by notation.
         * If false, we won't do any sorting of notations here. */
        private boolean maySortByNotation;

        /** If {@link maySortByNotation} is true, the format of
         * notation values. */
        private BrowseFlag notationFormat;

        /** If an exception related to parsing of notations is generated,
         * it is stored in this field. Otherwise, it remains null. */
        private NotationException notationException;

        /** Constructor.
         * @param aPrimaryLanguage The primary language of the vocabulary.
         * @param aDefaultSortByNotation Whether or not sorting by
         *      notation is the default sorting option.
         * @param aMaySortByNotation Whether or not sorting by notation
         *      is supported for this version.
         * @param aNotationFormat The format of notation to sort by,
         *      if one has been specified, otherwise null.
         */
        ConceptHandler(final String aPrimaryLanguage,
                final boolean aDefaultSortByNotation,
                final boolean aMaySortByNotation,
                final BrowseFlag aNotationFormat) {
            super();
            primaryLanguage = aPrimaryLanguage;
            defaultSortByNotation = aDefaultSortByNotation;
            maySortByNotation = aMaySortByNotation;
            notationFormat = aNotationFormat;
        }

        /** Map from concept IRI to Concept object,
         * used as a cache of all Concept objects. This Map
         * is maintained by {@link #getConcept(String)}, whose
         * body contains the only invocation of the constructor of the
         * Concept class.
         */
        private Map<String, Concept> iriConceptMap =
                new HashMap<>();

        /** Map from concept IRI to a map that maps
         * property name to the property value(s).
         * Used during parsing to collect all of the concepts.
         * This can be a HashMap (e.g., rather than a TreeMap),
         * because we are not sorting at this
         * stage. Sorting happens during {@link #buildForest()}.
         * The values of the map are themselves maps.
         * Those maps have keys which are
         * Strings: either "type", "broader", or "narrower".
         * (The keys "prefLabel", "notation", "definition" are
         * not used for now; they may come back, if it is desired
         * to represent values for resources other than SKOS Concepts.)
         * The values depend on what the keys are
         * (hence, the formal type is Object). For key "type"
         * (and "prefLabel", "notation", and "definition", if supported),
         * the actual type will be String;
         * for keys "broader", "narrower", the actual type will be
         * {@code Set<Concept>}.
         * */
        private Map<Concept, HashMap<String, Object>> conceptMap =
                new HashMap<>();

        /** The top-most concepts of the vocabulary. This is based on
         * finding all concepts that do not have a broader concept.
         * This is used in the first stage of {@link #buildForest()}
         * to collect the top-most concepts. It can be a HashMap
         * (e.g., rather than a TreeMap), because its contents are iterated
         * over to produce the Set that is actually returned by
         * {@link #buildForest()}. */
        private Map<Concept, HashMap<String, Object>> topmostConcepts =
                new HashMap<>();

        /** Get the Concept object for an IRI from the iriConceptMap
         * cache. Create such an object and add it to the cache,
         * if it is not already there.
         * @param iri The IRI to look up.
         * @return The Concept for this IRI.
         */
        private Concept getConcept(final String iri) {
            Concept concept = iriConceptMap.get(iri);
            if (concept == null) {
                concept = new Concept(iri);
                iriConceptMap.put(iri, concept);
            }
            return concept;
        }

        // Fields and methods to compute the depth-first spanning forest.

        /** Flag for the presence of a cycle. Initialized to false,
         *  and subsequently set to true if a cycle is detected.
         */
        private boolean cycle;

        /** Was a cycle detected during depth-first search?
         * @return True, if a cycle was detected.
         */
        public boolean isCycle() {
            return cycle;
        }

        /** Flag for the presence of non-tree edges. Initialized to true,
         * and subsequently set to false if a non-tree edge is found
         * during depth-first search.
         */
        private boolean onlyTreeEdges = true;

        /** Were only tree edges found during depth-first search?
         * @return True, if only tree edges were found during depth-first
         *      search.
         */
        public boolean isOnlyTreeEdges() {
            return onlyTreeEdges;
        }

        /** A set into which all concepts will be added, and then
         * progressively removed during depth-first search.
         * When the set is empty again, construction of the spanning
         * forest is complete.
         */
        private Set<Concept> nodesNotVisited = new HashSet<>();

        /** A set into which concepts are added while they are the
         * subject of the depth-first search. A concept is added to this
         * set on entry to the call to DFS on the concept, and it is
         * removed from the set on exit. There is a cycle if and
         * only if, during DFS we are considering visiting a node,
         * and it is currently in this set.
         */
        private Set<Concept> nodesActive = new HashSet<>();

        /** When either a broader or narrower triple is encountered,
         * keep track of that relationship and infer its inverse.
         * Also, infer that both subject and object are Concepts.
         * See section 8.6.2 of the SKOS Reference.
         * @param parent The parent Concept.
         * @param child The child Concept.
         */
        private void addBroaderNarrower(final Concept parent,
                final Concept child) {
            HashMap<String, Object> parentProperties;
            HashMap<String, Object> childProperties;

            if (conceptMap.get(parent) == null) {
                parentProperties = conceptMap.put(parent,
                        new HashMap<String, Object>());
            }
            parentProperties = conceptMap.get(parent);

            if (conceptMap.get(child) == null) {
                childProperties = conceptMap.put(child,
                        new HashMap<String, Object>());
            }
            childProperties = conceptMap.get(child);

            if (parentProperties.get("narrower") == null) {
                parentProperties.put("narrower",
                        new HashSet<Concept>());
            }
            @SuppressWarnings("unchecked")
            HashSet<Concept> narrowerSet =
                    (HashSet<Concept>) parentProperties.get("narrower");
            narrowerSet.add(child);

            if (childProperties.get("broader") == null) {
                childProperties.put("broader",
                        new HashSet<Concept>());
            }
            @SuppressWarnings("unchecked")
            HashSet<Concept> broaderSet =
                    (HashSet<Concept>) childProperties.get("broader");
            broaderSet.add(parent);

            // Inference on the type of both parent and child,
            // as per the SKOS Reference.
            if (!parentProperties.containsKey("type")) {
                parentProperties.put("type", CONCEPT_SHORT_FORM);
            }
            if (!childProperties.containsKey("type")) {
                childProperties.put("type", CONCEPT_SHORT_FORM);
            }
        }

        @Override
        public void handleStatement(final Statement st) {
            Concept subjectConcept = getConcept(st.getSubject().stringValue());
            if (conceptMap.get(subjectConcept) == null) {
                conceptMap.put(subjectConcept,
                        new HashMap<String, Object>());
            }
            HashMap<String, Object> concept =
                    conceptMap.get(subjectConcept);
            if (st.getPredicate().equals(RDF.TYPE)) {
                Value typeIRI = st.getObject();
                if (typesToLookFor.containsKey(typeIRI)) {
                    concept.put("type", typesToLookFor.get(typeIRI));
                }
            }
            if (st.getPredicate().equals(SKOS.PREF_LABEL)) {
                // Don't need need to do this, since for now
                // we are only processing SKOS Concepts. If we later
                // somehow wish to support prefLabels on things
                // other than Concepts, uncomment/modify as needed.
                // concept.put("prefLabel", st.getObject().stringValue());
                Value stObject = st.getObject();
                if (stObject instanceof Literal
                        && ((Literal) stObject).getLanguage() != null) {
                    subjectConcept.setPrefLabel(stObject.stringValue(),
                            ((Literal) stObject).getLanguage(),
                            primaryLanguage);
                } else {
                    subjectConcept.setPrefLabel(st.getObject().stringValue());
                }
            }
            // Future work: uncomment/modify the next six lines
            // when the portal is ready to receive it.
//            if (st.getPredicate().equals(SKOS.ALT_LABEL)) {
//                concept.put("altLabel", st.getObject().stringValue());
//            }
//            if (st.getPredicate().equals(SKOS.HIDDEN_LABEL)) {
//                concept.put("hiddenLabel", st.getObject().stringValue());
//            }
            if (st.getPredicate().equals(SKOS.NOTATION)) {
                // Don't need need to do this, since for now
                // we are only processing SKOS Concepts. If we later
                // somehow wish to support notations on things
                // other than Concepts, uncomment/modify as needed.
                // concept.put("notation", st.getObject().stringValue());
                subjectConcept.setNotation(st.getObject().stringValue());
            }
            if (st.getPredicate().equals(SKOS.DEFINITION)) {
                // Don't need need to do this, since for now
                // we are only processing SKOS Concepts. If we later
                // somehow wish to support definitions on things
                // other than Concepts, uncomment/modify as needed.
                // concept.put("definition", st.getObject().stringValue());
                subjectConcept.setDefinition(st.getObject().stringValue());
            }
            // The (absence of a) broader relationship is used to identify
            // top concepts.
            if (st.getPredicate().equals(SKOS.BROADER)) {
                addBroaderNarrower(getConcept(st.getObject().stringValue()),
                        subjectConcept);
            }
            if (st.getPredicate().equals(SKOS.NARROWER)) {
                addBroaderNarrower(subjectConcept,
                        getConcept(st.getObject().stringValue()));
            }
            // Future work: uncomment the next ten lines when work begins
            // on handling collections. NB: this code doesn't reflect
            // the "new" data structures; it will need to be updated.
//            if (st.getPredicate().equals(SKOS.MEMBER)) {
//                if (concept.get("member") == null) {
//                    concept.put("member",
//                            new ArrayList<String>());
//                }
//                @SuppressWarnings("unchecked")
//                ArrayList<String> memberList =
//                    (ArrayList<String>) concept.get("member");
//                memberList.add(st.getObject().stringValue());
//            }
        }

        /** Build the concepts forest.
         *  @return The forest of concepts, represented as a TreeSet.
         *  The values are Concepts. Each value represents
         *  one concept and each of its narrower concepts.
         */
        public TreeSet<ConceptOrRef> buildForest() {
            // This is a rearranged version of conceptMap, with
            // the concepts arranged in a forest structure based on
            // the broader/narrower relations.
            // More technically: the elements of roots are the roots
            // of a depth-first spanning forest.
            TreeSet<ConceptOrRef> roots = new TreeSet<>();
            populateRoots();
            for (Entry<Concept, HashMap<String, Object>> topmostConcept
                    : topmostConcepts.entrySet()) {
                roots.add(topmostConcept.getKey());
                depthFirstSearch(topmostConcept.getKey(),
                        topmostConcept.getValue());
            }
            if (!nodesNotVisited.isEmpty()) {
                // There are still some nodes that we haven't seen.
                // But in this case, there is definitely a cycle.
                // A cycle may or may not have been detected _already_, during
                // a depth-first-search of one of the top concepts.
                // But if we get here, there is also a cycle, e.g.,
                // A -> B -> C -> A, in which there are no other nodes
                // that have either A, B, C, as a narrower node.
                // In this case, neither A, B, nor C is a top concept,
                // and we won't have visited any of them yet.
                // We will visit one of those nodes now, in order
                // to "break" the cycle at one point.
                // We don't need to log it now, or note the presence
                // of the cycle now; it will be detected in due course
                // by the DFS. But log it anyway, and note the cycle
                // now anyway.
                logger.debug("ConceptTreeTransformProvider: "
                        + "there's a cycle! Non-empty nodesNotVisited.");
                cycle = true;
                do {
                    // Get an arbitrary concept that has not yet been
                    // visited ...
                    Concept newRoot = nodesNotVisited.iterator().next();
                    // ... and make it a root, then do DFS on it.
                    // Future work if we support returning a result
                    // even if there's a cycle:
                    // mark newRoot as _belonging_ to a cycle.
                    roots.add(newRoot);
                    depthFirstSearch(newRoot, conceptMap.get(newRoot));
                } while (!nodesNotVisited.isEmpty());
            }
            try {
                if (maySortByNotation && notationFormat != null) {
                    logger.info("Will do notation sort by " + notationFormat);
                    NotationComparator comparator =
                            new NotationComparator(notationFormat);
                    assignSortOrders(roots, comparator, defaultSortByNotation);
                    if (defaultSortByNotation) {
                        // Resort the children using the computed
                        // notation order.
                        TreeSet<ConceptOrRef> resortedRoots =
                                new TreeSet<>(
                                        new PrecomputedNotationComparator());
                        resortedRoots.addAll(roots);
                        // Now remove all the notation order values.
                        resortedRoots.forEach(n ->
                            n.setNotationSortOrder(null));
                        roots = resortedRoots;
                    }
                }
            } catch (NotationException ne) {
                setNotationException(ne);
            }
            return roots;
        }

        /** Perform a depth-first search starting at a concept.
         *
         * @param concept The concept from which to start the search.
         * @param map The HashMap of concept's properties.
         */
        @SuppressWarnings("unchecked")
        public void depthFirstSearch(final Concept concept,
                final HashMap<String, Object> map) {
            nodesNotVisited.remove(concept);
            nodesActive.add(concept);
            if (map.containsKey("narrower")) {
                Set<Concept> narrowerSet =
                        (Set<Concept>) map.get("narrower");
                for (Concept narrower : narrowerSet) {
                    if (nodesNotVisited.contains(narrower)) {
                        concept.addNarrower(narrower);
                        depthFirstSearch(narrower, conceptMap.get(narrower));
                    } else {
                        // We have visited this narrower concept already,
                        // which means this edge is not a tree edge,
                        // and there is either a cycle or a polyhierarchy.
                        onlyTreeEdges = false;
                        // If the narrower concept is active, we have
                        // a back edge, which means there's a cycle.
                        if (nodesActive.contains(narrower)) {
                            cycle = true;
                            logger.debug("ConceptTreeTransformProvider: "
                                    + "there's a cycle! Not following a back "
                                    + "edge from "
                                    + concept.getIri() + " to "
                                    + narrower.getIri());
                        } else {
                            logger.debug("ConceptTreeTransformProvider: "
                                    + "not following a forward or cross edge "
                                    + "from "
                                    + concept.getIri() + " to "
                                    + narrower.getIri());
                        }
                        // Add as a reference.
                        concept.addNarrower(narrower.getConceptRef());
                    }
                }
            }
            nodesActive.remove(concept);
        }

        /** Populate the roots of the forest, which are the top-most concepts.
         * A concept is considered to be "top-most" if it is a SKOS Concept
         * and it does not specify any broader concepts.
         * This (probably) catches both concepts explicitly
         * labelled as top concepts, and also any "dangling"
         * concepts.
         */
        private void populateRoots() {
            for (Entry<Concept, HashMap<String, Object>>
            concept : conceptMap.entrySet()) {
                HashMap<String, Object> propertyMap = concept.getValue();
                if (!propertyMap.isEmpty()
                        && CONCEPT_SHORT_FORM.equals(propertyMap.get("type"))) {
                    // Add all concepts to nodesNotVisited ...
                    nodesNotVisited.add(concept.getKey());
                    // ... but only those without a broader concept to
                    // the set of topmost concepts.
                    if (propertyMap.get("broader") == null) {
                        // logger.debug("Adding to topmost concepts: "
                        //         + concept.getKey());
                        topmostConcepts.put(concept.getKey(), propertyMap);
                    }
                }
            }
        }

        /** Set the value of {@link #notationException}.
         * @param ne The value of notationException to set.
         */
        private void setNotationException(final NotationException ne) {
            notationException = ne;
        }

        /** Get the value of {@link #notationException}.
         * @return The value of notationException.
         */
        NotationException getNotationException() {
            return notationException;
        }

    }

    /** Class for representing the result of the transform. An
     * instance of this class is serialized for storage into the
     * file for the version artefact.
     */
    private static class ConceptResult {

        /** The language of the preferred labels. */
        private String language;

        /** Set the value of language.
         * @param aLanguage The value of language to set.
         */
        public void setLanguage(final String aLanguage) {
            language = aLanguage;
        }

        /** Whether users will be offered the ability to sort by notation. */
        private boolean maySortByNotation = false;

        /** Set the value of maySortByNotation.
         * @param aMaySortByNotation The value of maySortByNotation to set.
         */
        public void setMaySortByNotation(final boolean aMaySortByNotation) {
            maySortByNotation = aMaySortByNotation;
        }

        /** Get the value of maySortByNotation. Invoked during
         * serialization into JSON.
         * @return The value of maySortByNotation.
         */
        @SuppressWarnings("unused")
        public boolean getMaySortByNotation() {
            return maySortByNotation;
        }

        /** In case {@link #maySortByNotation} is true,
         * whether the default sort order is by notation (true)
         * or by preferred label (false). */
        private Boolean defaultSortByNotation;

        /** Set the value of defaultSortByNotation.
         * @param aDefaultSortByNotation The value of defaultSortByNotation
         *      to set.
         */
        public void setDefaultSortByNotation(
                final boolean aDefaultSortByNotation) {
            defaultSortByNotation = aDefaultSortByNotation;
        }

        /** Get the value of defaultSortByNotation. Invoked during
         * serialization into JSON.
         * @return The value of defaultSortByNotation.
         */
        @SuppressWarnings("unused")
        public Boolean getDefaultSortByNotation() {
            return defaultSortByNotation;
        }

        /** If {@link maySortByNotation} is true, the format of
         * notation values. */
        private BrowseFlag notationFormat;

        /** Set the value of notationFormat.
         * @param aNotationFormat The value of notationFormat to set.
         */
        public void setNotationFormat(final BrowseFlag aNotationFormat) {
            notationFormat = aNotationFormat;
        }

        /** Get the value of notationFormat. Invoked during
         * serialization into JSON.
         * @return The value of notationFormat.
         */
        @SuppressWarnings("unused")
        public BrowseFlag getNotationFormat() {
            return notationFormat;
        }

        /** Get the value of language. Invoked during
         * serialization into JSON.
         * @return The value of language.
         */
        @SuppressWarnings("unused")
        public String getLanguage() {
            return language;
        }

        /** The roots of the forest of concepts. */
        private Set<ConceptOrRef> forest;

        /** Set the value of forest.
         * @param aForest The value of forest to set.
         */
        public void setForest(final Set<ConceptOrRef> aForest) {
            forest = aForest;
        }

        /** Get the value of forest. Invoked during
         * serialization into JSON.
         * @return The value of forest.
         */
        @SuppressWarnings("unused")
        public Set<ConceptOrRef> getForest() {
            return forest;
        }

    }

    /** A {@link Comparator} to use for sorting the narrower
     * concepts-or-references of a {@link Concept}, by notation,
     * where the format of the notation is specified as the parameter
     * to the constructor.
     */
    private static class NotationComparator
    implements Comparator<Pair<ConceptOrRef, Integer>> {

        /** Logger for this class. */
        private final Logger logger = LoggerFactory.getLogger(
                MethodHandles.lookup().lookupClass());

        /** The format of the notation values; one of the NOTATION_...
         * values. */
        private BrowseFlag notationFormat;

        /** Constructor.
         * @param aNotationFormat The format of the notation values to use
         *      during comparison of values.
         */
        NotationComparator(final BrowseFlag aNotationFormat) {
            notationFormat = aNotationFormat;
        }

        /** Plain text alert message to indicate that because of an
         * error with a notation value, sorting by notation will not
         * be offered on the view page. */
        private static final String SORT_BY_NOTATION_NOT_OFFERED =
                "Sorting by notation will not be offered.";

        /** HTML alert message to indicate that there was an
         * error in one of the notation values, because a floating-point
         * value was expected, but there was more than one decimal point. */
        private static final String FLOAT_MULTIPLE_POINTS_HTML =
                "The notation format was specified to "
                        + "be floating-point numbers, but one of "
                        + "the notation values contains more than "
                        + "one decimal point. (Hint: is the format "
                        + "in fact multi-level hierarchical?) "
                        + SORT_BY_NOTATION_NOT_OFFERED;

        /** HTML alert message to indicate an error in parsing
         * a floating-point value. */
        private static final String FLOAT_OTHER_ERROR_HTML =
                "The notation format was specified to "
                        + "be floating-point numbers, but one of "
                        + "the notation values is not a "
                        + "floating-point number. "
                        + SORT_BY_NOTATION_NOT_OFFERED;

        /** HTML alert message to indicate an error in parsing
         * an Integer value as a component of a dotted notation. */
        private static final String DOTTED_ERROR_HTML =
                "The notation format was specified to "
                        + "be numeric hierarchical, but one of "
                        + "the notation values contains a component that "
                        + "is not a number. "
                        + SORT_BY_NOTATION_NOT_OFFERED;

        /** Create and return a {@link NotationException} based on
         * the contents of a {@link NumberFormatException} that has
         * been thrown when attempting to parse a floating-point value.
         * @param nfe The NumberFormatException that was thrown
         * @param n The String that was being parsed as a floating-point value.
         * @return a NotationException that incorporates an appropriate
         *      alert text.
         */
        private NotationException floatNotationException(
                final NumberFormatException nfe, final String n) {
            logger.error("Exception while parsing float value: " + n
                    + "; message: " + nfe.getMessage());
            NotationException ne = new NotationException();
            if ("multiple points".equals(nfe.getMessage())) {
                ne.setAlertHTML(FLOAT_MULTIPLE_POINTS_HTML);
            } else {
                ne.setAlertHTML(FLOAT_OTHER_ERROR_HTML);
            }
            return ne;
        }

        /** Create and return a {@link NotationException} based on
         * the contents of a {@link NumberFormatException} that has
         * been thrown when attempting to parse a numeric value as an Integer.
         * @param nfe The NumberFormatException that was thrown
         * @param n The String that was being parsed as an Integer value.
         * @return a NotationException that incorporates an appropriate
         *      alert text.
         */
        private NotationException dottedNotationException(
                final NumberFormatException nfe, final String n) {
            logger.error("Exception while parsing integer: " + n
                    + "; message: " + nfe.getMessage());
            NotationException ne = new NotationException();
            ne.setAlertHTML(DOTTED_ERROR_HTML);
            return ne;
        }

        /* {@inheritDoc} */
        @Override
        public int compare(final Pair<ConceptOrRef, Integer> o1,
                final Pair<ConceptOrRef, Integer> o2) {
            String n1 = o1.getLeft().getNotation();
            String n2 = o2.getLeft().getNotation();
            if (n1 == null || n1.isEmpty()) {
                // o1 has no notation. It will be sorted
                // after all concepts that _do_ have notations.
                if (n2 == null || n2.isEmpty()) {
                    // Both concepts have null notations, so
                    // fall back to the ordering produced by the original
                    // prefLabel/IRI sort.
                    return o1.getRight() - o2.getRight();
                }
                // o2 has a notation. o1 is sorted after it.
                return 1;
            }
            // o1 has a notation.
            if (n2 == null || n2.isEmpty()) {
                // o2 doesn't have a notation. It is sorted after o1.
                return -1;
            }
            // Both o1 and o2 have notations.
            int notationComparison;
            switch (notationFormat) {
            case NOTATION_ALPHA:
                notationComparison = n1.compareToIgnoreCase(n2);
                if (notationComparison != 0) {
                    return notationComparison;
                }
                break;
            case NOTATION_DOTTED:
                // Adapted from:
                // https://stackoverflow.com/questions/198431/
                //         how-do-you-compare-two-version-strings-in-java
                String[] n1Parts = n1.split("\\.");
                String[] n2Parts = n2.split("\\.");
                int partsLength = Math.max(n1Parts.length, n2Parts.length);
                for (int i = 0; i < partsLength; i++) {
                    int n1Part, n2Part;
                    if (i < n1Parts.length) {
                        try {
                            n1Part = Integer.parseInt(n1Parts[i]);
                        } catch (NumberFormatException nfe) {
                            throw dottedNotationException(nfe, n1Parts[i]);
                        }
                    } else {
                        n1Part = 0;
                    }
                    if (i < n2Parts.length) {
                        try {
                        n2Part = Integer.parseInt(n2Parts[i]);
                        } catch (NumberFormatException nfe) {
                            throw dottedNotationException(nfe, n2Parts[i]);
                        }
                    } else {
                        n2Part = 0;
                    }
                    if (n1Part < n2Part) {
                        return -1;
                    }
                    if (n1Part > n2Part) {
                        return 1;
                    }
                }
                break;
            case NOTATION_FLOAT:
                // Empty strings have been catered for above, so we won't
                // get an exception for that here.
                float f1;
                float f2;
                try {
                    f1 = Float.parseFloat(n1);
                } catch (NumberFormatException nfe) {
                    throw floatNotationException(nfe, n1);
                }
                try {
                    f2 = Float.parseFloat(n2);
                } catch (NumberFormatException nfe) {
                    throw floatNotationException(nfe, n2);
                }
                notationComparison = Float.compare(f1, f2);
                if (notationComparison != 0) {
                    return notationComparison;
                }
                break;
            default:
                // Unknown notation format.
                logger.error("Illegal value for notation format: "
                        + notationFormat);
                throw new IllegalArgumentException(
                        "Illegal value for notation format: "
                                + notationFormat);
            }
            // Identical notations. Fall back to the ordering produced
            // by the original prefLabel/IRI sort.
            return o1.getRight() - o2.getRight();
        }
    }

    /** Compute the sorted order of a set of concepts-or-references
     * according to a particular notation format. Each element of the
     * set will have a sort orders assigned, for the order that isn't
     * the one specified as the default sort order.
     * The same computation will be applied recursively to narrower
     * concepts-or-references.
     * @param conceptOrRefSet A TreeSet of concepts-or-references, already
     *      sorted by prefLabel, that is also to be sorted by notation.
     * @param comparator The notation-format-specific Comparator to be
     *      used to order the concepts.
     * @param defaultSortByNotation Whether or not the default sorting
     *      is by notation.
     */
    private void assignSortOrders(final TreeSet<ConceptOrRef> conceptOrRefSet,
            final NotationComparator comparator,
            final boolean defaultSortByNotation) {
        if (conceptOrRefSet == null) {
            return;
        }
        int setSize = conceptOrRefSet.size();
        @SuppressWarnings("unchecked")
        Pair<ConceptOrRef, Integer>[] setSortedByNotations =
            new Pair[setSize];
        int i = 0;
        for (ConceptOrRef c : conceptOrRefSet) {
            // Take this opportunity to sort the children of this
            // child, if it has any.
            if (c instanceof Concept) {
                TreeSet<ConceptOrRef> narrowerTreeSet =
                        ((Concept) c).getNarrower();
                if (narrowerTreeSet != null) {
                    assignSortOrders(narrowerTreeSet, comparator,
                            defaultSortByNotation);
                    if (defaultSortByNotation) {
                        // Resort the children using the computed
                        // notation order.
                        TreeSet<ConceptOrRef> resortedNarrowerTreeSet =
                                new TreeSet<>(
                                        new PrecomputedNotationComparator());
                        resortedNarrowerTreeSet.addAll(narrowerTreeSet);
                        // Now remove all the notation order values.
                        resortedNarrowerTreeSet.forEach(
                                n -> n.setNotationSortOrder(null));
                        ((Concept) c).setNarrower(resortedNarrowerTreeSet);
                    }
                }
            }
            setSortedByNotations[i] = Pair.of(c, i);
            i++;
        }
        Arrays.sort(setSortedByNotations, comparator);
        for (i = 0; i < setSize; i++) {
            Pair<ConceptOrRef, Integer> p = setSortedByNotations[i];
            ConceptOrRef c = p.getLeft();
            // Always set the notation sort order at first. If the
            // default sort order is by notation, these values will
            // be removed after we resort.
            c.setNotationSortOrder(i);
            if (defaultSortByNotation) {
                // We are going to resort the data by notation, so
                // set the prefLabel sort order values.
                c.setPrefLabelSortOrder(p.getRight());
            }
        }
    }

    /** A {@link Comparator} to use for sorting the narrower
     * concepts-or-references of a {@link Concept}, based on
     * an already-assigned notation sort order.
     */
    private static class PrecomputedNotationComparator
    implements Comparator<ConceptOrRef> {

        /* {@inheritDoc} */
        @Override
        public int compare(final ConceptOrRef o1, final ConceptOrRef o2) {
            return o1.getNotationSortOrder() - o2.getNotationSortOrder();
        }
    }

    /** An exception class to use when an error is found in one of
     * the notation values.
     */
    private static class NotationException extends RuntimeException {

        /** Serial version UID for serialization. */
        private static final long serialVersionUID = 622153466578384982L;

        /** The HTML text of the alert to be provided to the user. */
        private String alertHTML;

        /** Set the HTML text of the alert to be provided to the user.
         * @param anAlertHTML The HTML text of the alert.
         */
        public void setAlertHTML(final String anAlertHTML) {
            alertHTML = anAlertHTML;
        }

        /** Set the HTML text of the alert to be provided to the user.
         * @return The HTML text of the alert.
         */
        public String getAlertHTML() {
            return alertHTML;
        }
    }

    /** Remove the ConceptTree version artefact for the version.
     * NB: This method will also be invoked by
     * {@link #transform(TaskInfo, Subtask)}, in the case of
     * success that nevertheless produces an empty result.
     * @param taskInfo The top-level TaskInfo for the subtask.
     * @param subtask The subtask to be performed.
     */
    public final void untransform(final TaskInfo taskInfo,
            final Subtask subtask) {
        // Remove the ConceptTree version artefact.
        List<VersionArtefact> vas = VersionArtefactDAO.
                getCurrentVersionArtefactListForVersionByType(
                        taskInfo.getVersion().getVersionId(),
                        VersionArtefactType.CONCEPT_TREE,
                        taskInfo.getEm());
        for (VersionArtefact va : vas) {
            // We _don't_ delete the file. But if we did:
            /*
            VaConceptTree vaConceptTree =
                    JSONSerialization.deserializeStringAsJson(
                            va.getData(), VaConceptTree.class);
            Files.deleteIfExists(Paths.get(vaConceptTree.getPath()));
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
            logger.error("Unknown operation!");
        }
    }

}
