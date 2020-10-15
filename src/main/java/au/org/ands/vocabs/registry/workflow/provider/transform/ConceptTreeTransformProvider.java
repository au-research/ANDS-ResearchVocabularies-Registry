/** See the file "LICENSE" for the full license governing this code. */
package au.org.ands.vocabs.registry.workflow.provider.transform;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.UnsupportedRDFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import au.org.ands.vocabs.registry.workflow.provider.transform.conceptTree.ConceptResult;
import au.org.ands.vocabs.registry.workflow.provider.transform.conceptTree.ResourceOrRef;
import au.org.ands.vocabs.registry.workflow.provider.transform.conceptTree.StatementHandler;
import au.org.ands.vocabs.registry.workflow.tasks.Subtask;
import au.org.ands.vocabs.registry.workflow.tasks.TaskInfo;
import au.org.ands.vocabs.registry.workflow.tasks.TaskRunner;
import au.org.ands.vocabs.registry.workflow.tasks.TaskUtils;
import au.org.ands.vocabs.registry.workflow.tasks.VersionArtefactUtils;

/** Transform provider for generating a forest-like representation of the
 * SKOS concepts, collections, and concept schemes as JSON.
 *
 * This class replaces {@link JsonTreeTransformProvider}.
 *
 * The transform assumes a vocabulary encoded using SKOS.
 * The resulting output is sorted at each level. The sort key is
 * user-determined: either by label, case-insensitively, or by
 * notation. In some cases (as explained below),
 * an alternative sort order is also represented within the result.
 * When sorting by label, resources without labels are
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
 *   "format": "3",
 *   "language": "en",
 *   "maySortByNotation": true,
 *   "mayResolveResources": false,
 *   "notationFormat": "notationDotted",
 *   "defaultDisplayNotation": false,
 *   "defaultSortByNotation": false,
 *   "forest": [
 *     {
 *       "type": "concept",
 *       "iri": "http://url1",
 *       "label": "Label 1",
 *       "definition": "Definition 1",
 *       "notation": "1",
 *       "notationSortOrder": 0,
 *       "children": [
 *         {
 *           "type": "concept",
 *           "iri": "http://uri1/narrower1",
 *           "label": "Label 1.1",
 *           "definition": "Definition 1.1",
 *           "notation": "1.1",
 *           "notationSortOrder": 0,
 *           "children": [
 *             {
 *               "type": "concept",
 *               "iri": "http://uri1/narrower1/narrower1",
 *               "label": "Label 1.1.1",
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
 *       "label": "Label 2",
 *       "definition": "Definition 2",
 *       "notation": "2",
 *       "notationSortOrder": 1,
 *       "children": [
 *         {
 *           "type": "concept_ref",
 *           "iri": "http://uri1/narrower1/narrower1",
 *           "label": "Label 1.1.1",
 *           "definition": "Definition 1.1.1",
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
 * <p>Each resource is "typed" using the key {@code
 * type}. For a concept, the value of the {@code type} key is either
 * {@code "concept"} or {@code "concept_ref"}.
 * If there are no polyhierarchies in the hierarchy, all
 * concepts will have {@code "type": "concept"}. Otherwise, the presence of
 * a concept with {@code "type": "concept_ref"} indicates
 * that this concept was visited more than once during depth-first search
 * (indicating a cross edge), and so this is a
 * "cross-reference" to a concept whose full definition is given elsewhere
 * in the hierarchy. A concept with {@code "type": "concept_ref"}
 * does <em>not</em> include a {@code children} key/value pair. In order to
 * find out if the concept has narrower concepts, it is necessary to locate
 * the corresponding concept with the same {@code iri} but with {@code
 * "type": "concept"}, and to see if <em>it</em> has a {@code narrower}
 * key/value pair.</p>
 *
 * <p>At the top level, there are these properties:</p>
 * <ul>
 *   <li>format: the version of the format of the JSON data</li>
 *   <li>language: the language of the labels</li>
 *   <li>maySortByNotation, notationFormat, etc.:
 *     the values of browse flags; see below.</li>
 * </ul>
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
 *   <li><code>defaultDisplayNotation</code>
 *     <ul><li>Boolean</li></ul>
 *   </li>
 *   <li>{@code includeConceptSchemes}
 *     <ul><li>Boolean</li></ul>
 *   </li>
 *   <li>{@code includeCollections}
 *     <ul><li>Boolean</li></ul>
 *   </li>
 *   <li>{@code mayResolveResources}
 *     <ul><li>Boolean</li></ul>
 *   </li>
 * </ul>
 *
 * <p>If {@code maySortByNotation is true},
 * there will be an additional key/value pair for each concept. The
 * key depends on the value of {@code defaultSortByNotation}.</p>
 *
 * <ul>
 *   <li>If the {@code maySortByNotation} key/value pair is present, and set
 *   to {@code true}, but the {@code defaultSortByNotation} key/value pair
 *   is missing, or is set to {@code false}:
 *     <ul>
 *       <li>Each node will also have a key/value pair, where the key is
 *       {@code notationSortOrder}.</li>
 *     </ul>
 *   </li>
 *   <li>If the {@code defaultSortByNotation} key/value pair is present, and
 *   set to {@code true}:
 *     <ul>
 *       <li>Each node will also have a key/value pair, where the key is
 *       {@code labelSortOrder}.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>The input vocabulary can have its broader/narrower hierarchy specified
 * using either {@code skos:narrower} or {@code skos:broader};
 * missing properties are inferred.</p>
 *
 * <p>Because the SKOS model explicitly allows polyhierarchies and cycles,
 * a depth-first search is performed to compute a depth-first spanning
 * forest.</p>
 *
 * <p>Things in the SKOS model to pay attention to:</p>
 * <ul>
 *   <li>A concept may belong to any number of concept schemes
 *     (SKOS Reference 4.6.1)</li>
 *   <li>A concept marked as the top concept of a concept scheme may
 *     nevertheless have a broader concept in the same concept scheme
 *     (SKOS Reference 4.6.3)</li>
 *   <li>The narrower/broader relations do not entail containment within
 *     the same concept scheme. If concept A is inScheme CS, and B is
 *     narrower then A, then B is not automatically also inScheme CS.
 *     (SKOS Reference 4.6.4)</li>
 * </ul>
 *
 * <p>The concepts that have no broader concept are privileged: they are
 * roots of the forest. Then a depth-first search is performed on all roots,
 * identifying the tree edges. When this is complete, do any nodes
 * remain unvisited? If so, one is chosen and added to the set of roots,
 * and a DFS is performed on it. This process is repeated until there
 * are no more unvisited nodes.</p>
 *
 * <p>In the following pseudo code, the elements of Roots are the roots
 * of the spanning forest, and the "children" sets of
 * each node form the edges of the spanning forest. The flag "Cycle" is True
 * if there is a cycle. The flag "OnlyTreeEdges" is True if there are only
 * tree edges: there are neither cycles nor polyhierarchies.</p>
 *
 * <p>Rather than adding "visited" and "active" properties to each node,
 * we use AllNodesNotVisited and NodesActive sets.</p>
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
 * if AllNodesNotVisited is not empty:
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
 *       add narrower to node.children as a reference (ResourceRef)
 *   remove node from NodesActive
 * </pre>
 *
 * See also the documentation for the classes in the child package
 * {@link au.org.ands.vocabs.registry.workflow.provider.transform.conceptTree}.
 */
public class ConceptTreeTransformProvider implements WorkflowProvider {

    /** Logger for this class. */
    private final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** A line break in HTML. Used for constructing the alert HTML.*/
    public static final String BR = "<br />";

    /** URL of a page that gives user-level documentation for
     * the browse visualization. */
    public static final String DOCUMENTATION_URL =
            "https://documentation.ardc.edu.au/display/DOC/"
            + "Support+for+concept+browsing+within+the+portal";

    /** The prelude of the alert HTML used when an RDF error is found. */
    public static final String ALERT_HTML_PRELUDE =
            "Alert: An error was detected "
                    + "in the vocabulary data RDF." + BR
                    + "The concept browse "
                    + "tree will not be visible for this version (";

    /** The interlude of the alert HTML used when an RDF error is found. */
    public static final String ALERT_HTML_INTERLUDE =
            ")." + BR;

    /** The postlude of the alert HTML used when an RDF error is found. */
    public static final String ALERT_HTML_POSTLUDE =
            "For more information, please see "
                    + "<a target=\"_blank\" "
                    + "href=\"" + DOCUMENTATION_URL + "\">Portal concept "
                    + "browsing</a>.";

    /** The version of the format of the JSON data stored in the
     * version artefact. */
    public static final String CONCEPTS_TREE_FORMAT = "3";

    /** Filename to use for the output of the transform.
     * The filename includes the format to make the version
     * of the format of the JSON data explicit.
     * The use of "3" in the name is to make explicit that this is the
     * second version of the format of the JSON data. */
    private static final String CONCEPTS_TREE_FILENAME = "concepts_tree"
            + CONCEPTS_TREE_FORMAT + ".json";

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

        StatementHandler statementHandler = new StatementHandler(
                primaryLanguage, bfParsed);
        // Parse all input files in the harvest directory, loading
        // the content into conceptHandler.
        boolean isRdfError = false;
        // As well as our own error messages generated by StatementHandler,
        // we want to catch errors messages generated by the parser itself.
        // If we get an exception, store the exception's message in
        // rdfParseError, and provide it back to the user.
        String rdfParseError = null;

        List<Path> pathsToProcess =
                TaskUtils.getPathsToProcessForVersion(taskInfo);
        for (Path entry: pathsToProcess) {
            try {
                RDFFormat format = Rio.getParserFormatForFileName(
                        entry.toString());
                RDFParser rdfParser = Rio.createParser(format);
                rdfParser.setRDFHandler(statementHandler);
                FileInputStream is = new FileInputStream(entry.toString());
                rdfParser.parse(is, entry.toString());
                logger.debug("Reading RDF: " + entry.toString());
            } catch (RDFParseException ex) {
                logger.error("Exception in ConceptTreeTransform "
                        + "while Parsing RDF", ex);
                isRdfError = true;
                // We're in a loop, so we may already have accumulated an
                // error. If so, supply a br tag as separator.
                if (rdfParseError == null) {
                    rdfParseError = "";
                } else {
                    rdfParseError = rdfParseError + BR;
                }
                String exMessage = ex.getMessage();
                // But exMessage may give away too much info, e.g.,
                // a full path to a file inside the system. Strip away
                // such info.
                exMessage = exMessage.replaceFirst(
                        "^([^/]+ )/.*/([^/]*)$", "$1$2");
                rdfParseError = rdfParseError + StatementHandler.escapeRdfError(
                        exMessage);
            } catch (DirectoryIteratorException
                    | IOException
                    | RDFHandlerException
                    | UnsupportedRDFormatException ex) {
                subtask.addResult(PARSE_PREFIX + entry.getFileName(),
                        "Exception in ConceptTreeTransform while Parsing RDF");
                logger.error("Exception in ConceptTreeTransform "
                        + "while Parsing RDF:", ex);
            } catch (IllegalArgumentException ex) {
                logger.error("IllegalArgumentException leaked to "
                        + "ConceptTreeTransform", ex);
                isRdfError = true;
                // We're in a loop, so we may already have accumulated an
                // error. If so, supply a br tag as separator.
                if (rdfParseError == null) {
                    rdfParseError = "";
                } else {
                    rdfParseError = rdfParseError + BR;
                }
                rdfParseError = rdfParseError + StatementHandler.escapeRdfError(
                        ex.getMessage());
            }
        }

        // There can be errors during parsing.
        if (statementHandler.getRdfErrors() != null) {
            isRdfError = true;
        }

        // Try to build the spanning forest ...
        TreeSet<ResourceOrRef> conceptTree = null;
        // ... but not if we already found errors.
        if (!isRdfError) {
            try {
                conceptTree = statementHandler.buildForest();
            } catch (IllegalArgumentException ex) {
                isRdfError = true;
            }
        }

        List<String> rdfErrors = statementHandler.getRdfErrors();
        // isRdfError should be true iff rdfErrors is non-null,
        // but check both "just in case".
        if (isRdfError || rdfErrors != null) {
            // We aren't going to provide a concept tree.
            // However, there might be an existing one left over,
            // which we must now remove. Call untransform() _here_
            // (i.e., rather than later), because it sets
            // the task status, and we want to override that below.
            untransform(taskInfo, subtask);

            String reason;
            if (statementHandler.isCycle()) {
                reason = "there is a cycle";
            } else {
                reason = "there is at least one error in the "
                        + "vocabulary data RDF";
            }
            String rdfErrorsString;
            if (rdfErrors == null && rdfParseError == null) {
                rdfErrorsString = "No further details of the error "
                        + "are available." + BR;
            } else {
                rdfErrorsString = "";
                if (rdfErrors != null) {
                    for (String rdfError : rdfErrors) {
                        rdfErrorsString = rdfErrorsString + rdfError + BR;
                        // Did we just add rdfParseError? If so, clear it.
                        if (rdfError.equals(rdfParseError)) {
                            rdfParseError = null;
                        }
                    }
                }
                // Add rdfParseError if we didn't already add it.
                if (rdfParseError != null) {
                    rdfErrorsString = rdfErrorsString + rdfParseError + BR;
                }
            }
            logger.error("Returning rdfErrorsString: " + rdfErrorsString);
            subtask.setStatus(TaskStatus.PARTIAL);
            subtask.addResult(CONCEPTS_TREE_NOT_PROVIDED,
                    "No concepts tree "
                    + "provided, because " + reason + ".");
            subtask.addResult(TaskRunner.ALERT_HTML,
                    ALERT_HTML_PRELUDE
                    + version.getSlug()
                    + ALERT_HTML_INTERLUDE
                    + rdfErrorsString
                    + ALERT_HTML_POSTLUDE);
            logger.error("ConceptTreeTransform: not providing a "
                    + "concept tree because " + reason + ".");
            // Future work:
            // write something else, e.g., a JSON string.
            //    FileUtils.writeStringToFile(out, "something");
            return;
        }

        // Extract the result, save in results Set and store in the
        // file system.

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
                if (!statementHandler.isCycle()) {
                    // Serialize the forest and write to the file system.
                    // Jackson will serialize TreeSets in sorted order of values
                    // (i.e., the Concept objects' prefLabels).
                    File out = new File(resultFileNameTree);
                    ConceptResult conceptResult = new ConceptResult();
                    conceptResult.setMayResolveResources(
                            bfParsed.isMayResolveResources());
                    conceptResult.setForest(conceptTree);
                    conceptResult.setLanguage(primaryLanguage);
                    if (statementHandler.getNotationException() == null) {
                        // No problem to set flags as specified in the version.
                        conceptResult.setMaySortByNotation(
                                bfParsed.isMaySortByNotation());
                        if (bfParsed.isMaySortByNotation()) {
                            conceptResult.setDefaultSortByNotation(
                                    bfParsed.isDefaultSortByNotation());
                            conceptResult.setNotationFormat(
                                    bfParsed.getNotationFormat());
                            conceptResult.setDefaultDisplayNotation(
                                    bfParsed.isDefaultDisplayNotation());
                        }
                    } else {
                        // There was an exception when parsing a notation.
                        conceptResult.setMaySortByNotation(false);
                        subtask.setStatus(TaskStatus.PARTIAL);
                        subtask.addResult(CONCEPTS_TREE_NO_NOTATIONS,
                                "No notation information because of a "
                                + "parse error.");
                        subtask.addResult(TaskRunner.ALERT_HTML,
                                statementHandler.getNotationException().
                                getAlertHTML());
                    }
                    FileUtils.writeStringToFile(out,
                            JSONSerialization.serializeObjectAsJsonString(
                                    conceptResult),
                            StandardCharsets.UTF_8);
                    VersionArtefactUtils.createConceptTreeVersionArtefact(
                            taskInfo, resultFileNameTree);
                } else {
                    // Note: as of format version 3, this code is now dead.
                    // For a cycle, we now return an alert in the same way
                    // as above.

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
                            + "in the vocabulary data." + BR
                            + "The concept browse "
                            + "tree will not be visible for this version ("
                            + version.getSlug() + ")." + BR
                            + "For more information, please see "
                            + "<a target=\"_blank\" "
                            + "href=\"" + DOCUMENTATION_URL
                            + "\">Portal concept browsing</a>.");
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
