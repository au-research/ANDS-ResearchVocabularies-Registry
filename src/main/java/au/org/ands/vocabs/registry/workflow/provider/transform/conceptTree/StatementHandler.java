// CHECKSTYLE:OFF: FileLength
/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.provider.transform.conceptTree;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.SKOS;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.entity.BrowseFlagsParsed;
import au.org.ands.vocabs.registry.enums.BrowseFlag;
import au.org.ands.vocabs.registry.workflow.provider.transform.ConceptTreeTransformProvider;

/** <p>RDF Handler to extract prefLabels, notation, definition,
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
 * all the RDF data has been parsed, so that all {@link Resource}
 * instances have their label data set, so that insertion
 * of the subsequently generated {@link Resource} instances
 * into a TreeSet (which is based on the
 * {@link ResourceOrRef#compareTo(ResourceOrRef)} method)
 * will work correctly.</p>
 *
 * <p>The interface to this class is as follows:</p>
 * <ol>
 * <li>Use the constructor to initialize an instance of the handler
 * with the information it needs.</li>
 * <li>Use Rio to create a parser, and set the handler to the
 * instance just created.</li>
 * <li>Parse the data. The {@link #handleStatement(Statement)} method
 * will be called back on every statement.</li>
 * <li>Invoke {@link #buildForest()} to construct the forest and
 * get the final result.</li>
 * <li>Also check the value of {@link #getNotationException()} to see
 * if there was an error in parsing a notation value.</li>
 * </ol>
 */
public class StatementHandler extends RDFHandlerBase {

    /** Logger for this class. */
    private final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** A map of SKOS types to take note of. */
    private static HashMap<URI, ResourceType> typesToLookFor = new HashMap<>();

    static {
        typesToLookFor.put(SKOS.CONCEPT_SCHEME, ResourceType.CONCEPT_SCHEME);
        typesToLookFor.put(SKOS.CONCEPT, ResourceType.CONCEPT);
        typesToLookFor.put(SKOS.COLLECTION, ResourceType.UNORDERED_COLLECTION);
        typesToLookFor.put(SKOS.ORDERED_COLLECTION,
                ResourceType.ORDERED_COLLECTION);
    }

    /** Map of Strings to canonical known IRIs. */
    private static Map<String, KnownIRI> knownIRIMap = new HashMap<>();

    /** Enumerated type of canonical instances of known IRIs.
     * Used as cases in a switch statement in the body of
     * {@link StatementHandler#handleStatement(Statement)}. */
    private enum KnownIRI {

        // Note that for now, this enum is used only to indicate
        // the _predicates_ we're interested in.
        // So although we are "interested" in the values rdf:List and rdf:nil,
        // we're not interested in those values as _predicates_.
        // Revisit when necessary.

        /** rdf:type. */
        RDF_TYPE(RDF.TYPE),
//        /** rdf:List. */
//        RDF_LIST(RDF.LIST),

        /** rdf:first. */
        RDF_FIRST(RDF.FIRST),
        /** rdf:rest. */
        RDF_REST(RDF.REST),
//        /** rdf:nil. */
//        RDF_NIL(RDF.NIL),

        /** skos:prefLabel. */
        SKOS_PREF_LABEL(SKOS.PREF_LABEL),
        /** skos:altLabel. */
        SKOS_ALT_LABEL(SKOS.ALT_LABEL),
        /** skos:notation. */
        SKOS_NOTATION(SKOS.NOTATION),
        /** skos:definition. */
        SKOS_DEFINITION(SKOS.DEFINITION),
        /** skos:broader. */
        SKOS_BROADER(SKOS.BROADER),
        /** skos:narrower. */
        SKOS_NARROWER(SKOS.NARROWER),
        /** skos:inScheme. */
        SKOS_IN_SCHEME(SKOS.IN_SCHEME),
        /** skos:topConceptOf. */
        SKOS_TOP_CONCEPT_OF(SKOS.TOP_CONCEPT_OF),
        /** skos:hasTopConcept. */
        SKOS_HAS_TOP_CONCEPT(SKOS.HAS_TOP_CONCEPT),
        /** skos:member. */
        SKOS_MEMBER(SKOS.MEMBER),
        /** skos:memberList. */
        SKOS_MEMBER_LIST(SKOS.MEMBER_LIST),

        /** dcterms:title. */
        DCTERMS_TITLE(DCTERMS.TITLE),
        /** dcterms:description. */
        DCTERMS_DESCRIPTION(DCTERMS.DESCRIPTION),

        /** rdfs:label. */
        RDFS_LABEL(RDFS.LABEL);

        /** Constructor, which takes a URI parameter.
         * @param aURI The URI
         */
        KnownIRI(final URI aURI) {
            knownIRIMap.put(aURI.stringValue(), this);
        }

        /** Get the KnownIRI for a given URI.
         * @param aURI the URI to be looked up.
         * @return The KnownIRI instance for the URI, or null,
         *      if the URI is unknown.
         */
        public static KnownIRI getKnownIRI(final URI aURI) {
            return knownIRIMap.get(aURI.stringValue());
        }
    }

    /** The primary language of the vocabulary. */
    private String primaryLanguage;

    /** The parsed browse flags. */
    private BrowseFlagsParsed bfParsed;

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

    /** Whether concept schemes are to be considered. */
    private boolean includeConceptSchemes;

    /** Whether collections are to be considered. */
    private boolean includeCollections;

    /** Whether resource IRIs may be resolved. */
    private boolean mayResolveResources;

    /** If an exception related to parsing of notations is generated,
     * it is stored in this field. Otherwise, it remains null. */
    private NotationException notationException;

    /** Constructor.
     * @param aPrimaryLanguage The primary language of the vocabulary.
     * @param aBfParsed The parsed browse flags.
     */
    public StatementHandler(final String aPrimaryLanguage,
            final BrowseFlagsParsed aBfParsed) {
        super();
        bfParsed = aBfParsed;
        primaryLanguage = aPrimaryLanguage;
        defaultSortByNotation = bfParsed.isDefaultSortByNotation();
        maySortByNotation = bfParsed.isMaySortByNotation();
        notationFormat = bfParsed.getNotationFormat();
        includeConceptSchemes = bfParsed.isIncludeConceptSchemes();
        includeCollections = bfParsed.isIncludeCollections();
        mayResolveResources = bfParsed.isMayResolveResources();
    }

    /** Map from Concept Scheme IRI to Concept Scheme object,
     * used as a cache of all Concept Scheme objects.
     * This is only used if the {@link #includeConceptSchemes} flag
     * is true.
     */
    private Map<String, Resource> conceptSchemeMap = new HashMap<>();

    /** Map from each concept scheme resources to the set of
     * its master members. */
    private Map<Resource, Set<Resource>> conceptSchemeMasterMembers =
            new HashMap<>();

    /** Map from each concept scheme resources to the set of
     * its top concepts. */
    private Map<Resource, Set<Resource>> conceptSchemeTopConcepts =
            new HashMap<>();

    /** Map from RDF List IRI to RDFList object, used as a cache of all
     * RDF Lists that have been specified as the object of the
     * skos:memberList property. */
    private Map<String, RDFList> collectionMemberListMap = new HashMap<>();

    /** MAP From RDF List to collection Resource, used by
     * {@link #extractRDFListElements(RDFList)} to get a collection's
     * IRI in case of an error.
     */
    private Map<RDFList, Resource> rdfListMapToCollection = new HashMap<>();

    /** Map from Collection IRI to Collection object,
     * used as a cache of all Collection objects.
     * This is only used if the {@link #includeCollections} flag
     * is true.
     */
    private Map<String, Resource> collectionMap = new HashMap<>();

    /** Map from Ordered Collection resources to lists of their
     * members. */
    private Map<Resource, List<Resource>> orderedCollectionMembers =
            new HashMap<>();

    /** Map from resource IRI to master Resource object,
     * used as a cache of all master Resource objects. This Map
     * is maintained by {@link #getMasterResource(String)}, whose
     * body contains the only invocation of the constructor of the
     * Resource class.
     */
    private Map<String, Resource> iriMasterResourceMap = new HashMap<>();

    /** Look up an IRI in the master Resource map. If it is present,
     * return the Resource. Otherwise, return null.
     * @param iri The IRI to be looked up in the master Resource map.
     * @return The Resource, if one exists, or null, if not.
     */
    private Resource getMasterResourceOrNull(final String iri) {
        return iriMasterResourceMap.get(iri);
    }

    /** Get the master Resource object for an IRI from the
     * {@link #iriMasterResourceMap}
     * cache. Create such an object and add it to the cache,
     * if it is not already there.
     * This method mustn't be invoked once {@link #freezeResources()}
     * has been invoked, as that method removes entries from
     * {@link #iriMasterResourceMap}.
     * @param iri The IRI to look up.
     * @return The Resource for this IRI.
     */
    private Resource getMasterResource(final String iri) {
        Resource resource = iriMasterResourceMap.get(iri);
        if (resource == null) {
            try {
                resource = new Resource(iri, mayResolveResources);
            } catch (IllegalArgumentException e) {
                // Pass on the exception's message.
                String error = e.getMessage();
                addRdfError(error);
            }
            iriMasterResourceMap.put(iri, resource);
        }
        return resource;
    }

    /** Get a Collection containing all master Resources.
     * @return A Collection containing all master Resources.
     */
    private Collection<Resource> getAllMasterResources() {
        return iriMasterResourceMap.values();
    }

    /** A Set to keep track of which master resources have at least
     * one deputy. We don't expect these master resources to be visited
     * during depth-first search.*/
    private Set<Resource> masterResourcesWhichHaveADeputy = new HashSet<>();

    /** The Resources selected to be the roots of the visualization.
     * If neither {@link #includeConceptSchemes} nor {@link #includeCollections}
     * is true, then a Resource is considered to be "top-most" if it is a
     * SKOS Concept and it does not specify any broader concepts.
     * If either flag is true, then concept schemes and/or collections
     * are also considered to be "top-most". (We also handle
     * nesting of collections.) */
    private TreeSet<ResourceOrRef> roots = new TreeSet<>();

    // Fields and methods to support "deputy" Resources used for
    // each concept scheme.

    /** Map from the master Resource of every concept scheme to
     * a Map of the resource IRIs used in that concept scheme to
     * the deputy instance of the resource.
     */
    private Map<Resource, Map<String, Resource>> csDeputiesIriResourceMaps =
            new HashMap<>();

    /** Map from the master Resource of every concept scheme to
     * a Set of the deputy instances of the resources that are the
     * concept scheme's members.
     */
    private Map<Resource, Set<Resource>> conceptSchemeDeputyMembers =
            new HashMap<>();

    // Fields and methods to support "deputy" Resources used for
    // each collection.

    /** Map from the master Resource of every collection to
     * a Map of the resource IRIs used in that collection to
     * the deputy instance of the resource.
     */
    private Map<Resource, Map<String, Resource>> collDeputiesIriResourceMaps =
            new HashMap<>();

    /** Map from the master Resource of every collection to
     * a Set of the deputy instances of the resources that are the
     * collection's members.
     */
    private Map<Resource, Set<Resource>> collectionDeputyMembers =
            new HashMap<>();

    // Fields and methods to represent lists

    /** A map whose keys are all IRIs or blank nodes
     * that we believe should be lists, and whose values are our
     * own representation of lists.
     */
    private Map<String, RDFList> rdfListElements = new HashMap<>();

    /** Get the RDFList object for an IRI from the rdfListElements
     * cache. Create such an object and add it to the cache,
     * if it is not already there.
     * @param iri The IRI to look up.
     * @return The RDFList for this IRI.
     */
    private RDFList getRDFList(final String iri) {
        RDFList rdfList = rdfListElements.get(iri);
        if (rdfList == null) {
            rdfList = new RDFList(iri);
            rdfListElements.put(iri, rdfList);
        }
        return rdfList;
    }

    /** Given a triple ?subject rdf:first ?object, record that
     * fact that ?object is the first element of the list ?subject.
     * @param subject The subject of the triple.
     * @param object The object of the triple.
     */
    private void setListFirst(final Value subject, final Value object) {
        String subjectString = subject.stringValue();
        RDFList head = getRDFList(subjectString);
        // Watch out for rdf:nil. We _shouldn't_ find it!
        if (RDF.NIL.equals(object)) {
            String error = RDF_ERROR_LIST_FIRST_NIL + subjectString;
            addRdfError(error);
            // This method doesn't return a value, so carry on. No need to do:
            //  throw new IllegalArgumentException(error);
        } else {
            try {
                head.setFirst(object);
            } catch (IllegalArgumentException e) {
                String error = RDF_ERROR_LIST_MULTIPLE_FIRST + subjectString;
                addRdfError(error);
                // This method doesn't return a value, so carry on.
                // No need to do:
                //  throw new IllegalArgumentException(error);
            }
        }
    }

    /** Given a triple ?subject rdf:rest ?object, record that
     * fact that ?object is the rest of the list ?subject.
     * @param subject The subject of the triple.
     * @param object The object of the triple.
     */
    private void setListRest(final Value subject, final Value object) {
        String subjectString = subject.stringValue();
        if (object instanceof Literal) {
            String error = RDF_ERROR_LIST_REST_LITERAL + object.stringValue();
            addRdfError(error);
            // This method doesn't return a value, so carry on. No need to do:
            //   throw new IllegalArgumentException(error);
            return;
        }
        RDFList head = getRDFList(subjectString);
        // Watch out for rdf:nil, and use our own special marker if we find it.
        try {
            if (RDF.NIL.equals(object)) {
                head.setRest(RDFList.NIL);
            } else {
                String objectString = object.stringValue();
                RDFList rest = getRDFList(objectString);
                head.setRest(rest);
            }
        } catch (IllegalArgumentException e) {
            String error = RDF_ERROR_LIST_MULTIPLE_REST + subjectString;
            addRdfError(error);
            // This method doesn't return a value, so carry on. No need to do:
            // throw new IllegalArgumentException(error);
        }
    }

    /** Get the list elements of an {@link RDFList} that has been
     * specified as the value of a skos:memberList property of an
     * ordered collection.
     * Throws an IllegalArgumentException is we consider the list not
     * to be well-formed (for example, if there is a cycle).
     * @param rdfList The RDFList for which list elements are to be extracted.
     * @return A List of the Resources in the RDFList.
     */
    private List<Resource> extractRDFListElements(final RDFList rdfList) {
        Set<RDFList> rdfListValuesInList = new HashSet<>();
        Set<Resource> resourcesInList = new HashSet<>();
        List<Resource> resourceList = new ArrayList<>();
        RDFList remainingList = rdfList;
        while (remainingList != null && remainingList != RDFList.NIL) {
            rdfListValuesInList.add(remainingList);
            Value headValue = remainingList.getFirst();
            if (headValue instanceof Literal) {
                // Literals aren't allowed.
                String error = RDF_ERROR_MEMBERLIST_ELEMENT_LITERAL
                        + headValue.stringValue();
                addRdfError(error);
                // This method returns a value, so can't carry on.
                throw new IllegalArgumentException(error);
            }
            String headValueString = headValue.stringValue();
            Resource headResource = getMasterResourceOrNull(headValueString);
            if (headResource == null) {
                // We don't know anything about this.
                String error = RDF_ERROR_LIST_MEMBERLIST_ELEMENT_NOT_VALID
                        + headValueString;
                addRdfError(error);
                // This method returns a value, so can't carry on.
                throw new IllegalArgumentException(error);
            }
            // We don't do type-checking here; that's done by
            // getOrderedMembersForCollection().
            if (!resourcesInList.contains(headResource)) {
                resourcesInList.add(headResource);
                resourceList.add(headResource);
            }
            remainingList = remainingList.getRest();
            if (rdfListValuesInList.contains(remainingList)) {
                // Cycle!
                String error = RDF_ERROR_MEMBERLIST_CYCLE
                        + rdfListMapToCollection.get(rdfList).getIri();
                addRdfError(error);
                throw new IllegalArgumentException(error);
            }
        }
        return resourceList;
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
     * during depth-first search. But note that we don't currently
     * make use of this value, as we do allow polyhierarchies, and
     * cycle detection doesn't rely on it.
     */
    private boolean onlyTreeEdges = true;

    /** Were only tree edges found during depth-first search?
     * (NB: as present, we don't use this value, as we do allow
     * polyhierarchies, and cycle detection doesn't rely on it.)
     * @return True, if only tree edges were found during depth-first
     *      search.
     */
    public boolean isOnlyTreeEdges() {
        return onlyTreeEdges;
    }

    /** A set into which all resources will be added, and then
     * progressively removed during depth-first search.
     * When the set is empty again, construction of the spanning
     * forest is complete.
     */
    private Set<Resource> nodesNotVisited = new HashSet<>();

    /** A set into which resources are added while they are the
     * subject of the depth-first search. A resources is added to this
     * set on entry to the call to DFS on the resource, and it is
     * removed from the set on exit. There is a cycle if and
     * only if, during DFS we are considering visiting a node,
     * and it is currently in this set.
     */
    private Set<Resource> nodesActive = new HashSet<>();

    /** Register that a resource is in a concept scheme.
     * Apply the corresponding type constraints.
     * @param csValue The Value containing the IRI of the concept scheme.
     * @param resourceValue The Value containing the IRI of the resource.
     * @param isTopConcept True, if the resource is also to be recorded
     *      as being a top concept of the Concept Scheme.
     */
    private void addResourceToConceptScheme(final Value csValue,
            final Value resourceValue, final boolean isTopConcept) {
        // Treatment for concepts, whether or not also top concepts.
        requireResourceHaveType(csValue, ResourceType.CONCEPT_SCHEME);
        // The resource is not necessarily a concept when invoked for inScheme.
        // See SKOS Reference example 81. So we mustn't do this:
        //   requireResourceHaveType(resourceValue, ResourceType.CONCEPT);
        Resource csResource = getMasterResource(csValue.stringValue());
        Resource conceptResource = getMasterResource(
                resourceValue.stringValue());
        Set<Resource> csMembers = conceptSchemeMasterMembers.get(csResource);
        // csMembers won't be null, because we just called
        // requireResourceHaveType() above.
        csMembers.add(conceptResource);
        // This concept will _not_ appear as an "orphan" at the top level.
        conceptResource.addScaffoldInConceptSchemes(csResource);

        // Additional treatment only for top concepts.
        if (isTopConcept) {
            // We definitely know that resource is a concept.
            requireResourceHaveType(resourceValue, ResourceType.CONCEPT);
            Set<Resource> csTCs = conceptSchemeTopConcepts.get(csResource);
            if (csTCs == null) {
                csTCs = new HashSet<>();
                conceptSchemeTopConcepts.put(csResource, csTCs);
            }
            csTCs.add(conceptResource);
        }
    }

    /** When either a {@code skos:broader} or {@code skos:narrower} triple
     * is encountered, keep track of that relationship and infer its inverse.
     * Also, infer that both subject and object are Concepts.
     * See section 8.6.2 of the SKOS Reference.
     * @param parent The parent Concept.
     * @param child The child Concept.
     */
    private void addBroaderNarrowerConcepts(final Resource parent,
            final Resource child) {
        parent.addScaffoldNarrower(child);
        child.addScaffoldBroader(parent);

        // Inference on the type of both parent and child,
        // as per the SKOS Reference.
        ResourceType existingType = parent.getType();
        try {
            parent.setType(ResourceType.CONCEPT);
        } catch (IllegalArgumentException e) {
            String error = RDF_ERROR_INVALID_TYPE_CHANGE + parent.getIri()
            + "; from type " + existingType + " to " + ResourceType.CONCEPT;
            addRdfError(error);
        }
        existingType = child.getType();
        try {
            child.setType(ResourceType.CONCEPT);
        } catch (IllegalArgumentException e) {
            String error = RDF_ERROR_INVALID_TYPE_CHANGE + child.getIri()
            + "; from type " + existingType + " to " + ResourceType.CONCEPT;
            addRdfError(error);
        }
    }

    /** When a {@code skos:member} is encountered, keep track of that
     * relationship and infer its inverse.
     * See section 9.3 of the SKOS Reference.
     * This method does not do type inference or checking.
     * @param unorderedCollection The unordered collection Resource.
     * @param member The member Resource.
     */
    private void addMemberForUnorderedCollection(
            final Resource unorderedCollection,
            final Resource member) {
        unorderedCollection.addScaffoldMember(member);
        // This concept will _not_ appear as an "orphan" at the top level.
        member.addScaffoldInCollections(unorderedCollection);
    }

    /** When a {@code skos:memberList} is encountered, keep track of that
     * relationship and infer its inverse.
     * See section 9.3 of the SKOS Reference.
     * This method does not do type inference or checking.
     * @param parent The parent Resource.
     * @param child The child Resource.
     */
    private void addMemberListForCollection(final Resource parent,
            final Value child) {
        String memberList = parent.getScaffoldMemberList();
        String childString = child.stringValue();
        if (memberList == null) {
            parent.setScaffoldMemberList(childString);
            RDFList rdfList = getRDFList(childString);
            collectionMemberListMap.put(childString, rdfList);
            // And add to this map, in case we need to generate an error
            // during extractRDFListElements().
            rdfListMapToCollection.put(rdfList, parent);
        } else {
            // Could be a duplicate triple; only generate an error if not.
            if (!memberList.equals(childString)) {
                String error = RDF_ERROR_MULTIPLE_MEMBERLIST + parent.getIri();
                addRdfError(error);
                // This method doesn't return a value, so carry on.
                // No need to do:
                //   throw new IllegalArgumentException(error);
            }
        }
    }

    /** Require that a value be either an IRI, or a blank node, and that
     * that value be represented as a Resource with
     * a given type. If no such Resource instance exists, one is
     * created. An attempt is made to set the type of the resource
     * to the specified value; an IllegalArgumentException will be
     * thrown if this was not possible. Any other necessary bookkeeping is done.
     * @param value The Value being required to have a specified type.
     * @param requiredType The required type of the Resource.
     */
    private void requireResourceHaveType(final Value value,
            final ResourceType requiredType) {
        String iri = value.stringValue();
        Resource resource = getMasterResource(iri);
        if (resource == null) {
            // This happens if the Resource(String, boolean) constructor
            // throws an exception.
            throw new IllegalArgumentException(UNABLE_TO_CONTINUE);
        }

        ResourceType existingType = resource.getType();
        try {
            resource.setType(requiredType);
        } catch (IllegalArgumentException e) {
            String error = RDF_ERROR_INVALID_TYPE_CHANGE + resource.getIri()
            + "; from type " + existingType + " to " + requiredType;
            addRdfError(error);
        }
        switch (requiredType) {
        case CONCEPT:
            break;
        case ORDERED_COLLECTION:
        case UNORDERED_COLLECTION:
            collectionMap.put(iri, resource);
            break;
        case CONCEPT_SCHEME:
            if (conceptSchemeMap.get(iri) == null) {
                conceptSchemeMap.put(iri, resource);
                Set<Resource> csMembers = new HashSet<>();
                conceptSchemeMasterMembers.put(resource, csMembers);
            }
            break;
        case CONCEPT_REF:
        case UNORDERED_COLLECTION_REF:
        case ORDERED_COLLECTION_REF:
            // Can't happen!
            logger.error("Defect: somehow saw ..._REF");
            throw new IllegalArgumentException("requireResourceHaveType: "
                    + "broken typesToLookFor");
        default:
            logger.error("Defect: missing case in switch: " + requiredType);
            throw new IllegalArgumentException("requireResourceHaveType: "
                    + "missing case in switch");
            }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("checkstyle:MethodLength")
    @Override
    public void handleStatement(final Statement st) {
        // Uncomment as needed for debugging.
//        logger.info("Encountered triple: s: "
//                + st.getSubject().stringValue()
//                + " ; p: " + st.getPredicate().stringValue()
//                + " ; o: " + st.getObject().stringValue());
        KnownIRI predicateIRI = KnownIRI.getKnownIRI(st.getPredicate());
        if (predicateIRI == null) {
            // No problem; we're not interested.
            //  logger.info("Not interested in that triple");
            return;
        }
        Resource subjectResource =
                getMasterResource(st.getSubject().stringValue());
        if (subjectResource == null) {
            // This happens if the Resource(String, boolean) constructor
            // throws an exception.
            throw new IllegalArgumentException(UNABLE_TO_CONTINUE);
        }
        Value stObject;

        switch (predicateIRI) {
        case RDF_TYPE:
            Value typeIRI = st.getObject();
            if (typeIRI instanceof Literal) {
                String error = RDF_ERROR_TYPE_LITERAL + typeIRI.stringValue();
                addRdfError(error);
                // This method doesn't return a value, so carry on.
                // No need to do:
                //  throw new IllegalArgumentException(error);
                return;
            }
            if (typesToLookFor.containsKey(typeIRI)) {
                requireResourceHaveType(st.getSubject(),
                        typesToLookFor.get(typeIRI));
            }
            break;
        case SKOS_PREF_LABEL:
            stObject = st.getObject();
            if (stObject instanceof Literal
                    && ((Literal) stObject).getLanguage() != null) {
                subjectResource.setPrefLabel(stObject.stringValue(),
                        ((Literal) stObject).getLanguage(),
                        primaryLanguage);
            } else {
                subjectResource.setPrefLabel(stObject.stringValue());
            }
            break;
        case SKOS_ALT_LABEL:
            stObject = st.getObject();
            if (stObject instanceof Literal
                    && ((Literal) stObject).getLanguage() != null) {
                subjectResource.addAltLabel(stObject.stringValue(),
                        ((Literal) stObject).getLanguage(),
                        primaryLanguage);
            } else {
                subjectResource.addAltLabel(stObject.stringValue());
            }
            break;
        case SKOS_NOTATION:
            subjectResource.setNotation(st.getObject().stringValue());
            break;
        case SKOS_DEFINITION:
            subjectResource.setDefinition(st.getObject().stringValue());
            break;
        // The (absence of a) broader relationship is used to identify
        // top concepts.
        case SKOS_BROADER:
            addBroaderNarrowerConcepts(getMasterResource(
                    st.getObject().stringValue()), subjectResource);
            break;
        case SKOS_NARROWER:
            addBroaderNarrowerConcepts(subjectResource,
                    getMasterResource(st.getObject().stringValue()));
            break;
        case SKOS_IN_SCHEME:
            // Only care if we're paying attention to concept schemes.
            if (includeConceptSchemes) {
                // Note: the domain of inScheme is all resources, so this
                // _doesn't_ mean that the subject is a concept.
                addResourceToConceptScheme(st.getObject(), st.getSubject(),
                        false);
            }
            break;
        case SKOS_TOP_CONCEPT_OF:
            // Only care if we're paying attention to concept schemes.
            if (includeConceptSchemes) {
                addResourceToConceptScheme(st.getObject(), st.getSubject(),
                        true);
            }
            break;
        case SKOS_HAS_TOP_CONCEPT:
            // Only care if we're paying attention to concept schemes.
            if (includeConceptSchemes) {
                addResourceToConceptScheme(st.getSubject(), st.getObject(),
                        true);
            }
            break;
        case SKOS_MEMBER:
            // Only care if we're paying attention to collections.
            if (includeCollections) {
                // No problem to "require" unordered even if the collection
                // turns out to be ordered; see the implementation
                // of Resource#setType().
                requireResourceHaveType(st.getSubject(),
                        ResourceType.UNORDERED_COLLECTION);
                // freezeResources() will do a type check of all members.
                stObject = st.getObject();
                if (stObject instanceof Literal) {
                    String error =  RDF_ERROR_MEMBER_LITERAL
                            + stObject.stringValue();
                    addRdfError(error);
                    // This method doesn't return a value, so carry on.
                    // No need to do:
                    //   throw new IllegalArgumentException(error);
                    return;
                }
                addMemberForUnorderedCollection(subjectResource,
                        getMasterResource(stObject.stringValue()));
            }
            break;
        case SKOS_MEMBER_LIST:
            // Only care if we're paying attention to collections.
            if (includeCollections) {
                requireResourceHaveType(st.getSubject(),
                        ResourceType.ORDERED_COLLECTION);
                stObject = st.getObject();
                if (stObject instanceof Literal) {
                    String error =  RDF_ERROR_MEMBERLIST_LITERAL
                            + stObject.stringValue();
                    addRdfError(error);
                    // This method doesn't return a value, so carry on.
                    // No need to do:
                    //   throw new IllegalArgumentException(error);
                    return;
                }
                addMemberListForCollection(subjectResource, stObject);
            }
            break;
        // rdf:first and rdf:rest are used to build ordered collections.
        case RDF_FIRST:
            // Only care if we're paying attention to collections.
            if (includeCollections) {
                setListFirst(st.getSubject(), st.getObject());
            }
            break;
        case RDF_REST:
            // Only care if we're paying attention to collections.
            if (includeCollections) {
                setListRest(st.getSubject(), st.getObject());
            }
            break;
        case DCTERMS_TITLE:
            stObject = st.getObject();
            if (stObject instanceof Literal
                    && ((Literal) stObject).getLanguage() != null) {
                subjectResource.setDctermsTitle(stObject.stringValue(),
                        ((Literal) stObject).getLanguage(),
                        primaryLanguage);
            } else {
                subjectResource.setDctermsTitle(stObject.stringValue());
            }
            break;
        case DCTERMS_DESCRIPTION:
            stObject = st.getObject();
            if (stObject instanceof Literal
                    && ((Literal) stObject).getLanguage() != null) {
                subjectResource.setDctermsDescription(stObject.stringValue(),
                        ((Literal) stObject).getLanguage(),
                        primaryLanguage);
            } else {
                subjectResource.setDctermsDescription(stObject.stringValue());
            }
            break;
        case RDFS_LABEL:
            stObject = st.getObject();
            if (stObject instanceof Literal
                    && ((Literal) stObject).getLanguage() != null) {
                subjectResource.setRdfsLabel(stObject.stringValue(),
                        ((Literal) stObject).getLanguage(),
                        primaryLanguage);
            } else {
                subjectResource.setRdfsLabel(stObject.stringValue());
            }
            break;
        default:
            // If we get here, there's a missing case.
            logger.error("Defect: missing case in switch: " + predicateIRI);
            throw new IllegalArgumentException("handleStatement: "
                    + "missing case in switch");
        }

        // Future work: uncomment/modify the next six lines
        // when the portal is ready to receive it.
//            case SKOS_HIDDEN_LABEL:
//                set stObject and do Literal test etc. here ...
//                subjectResource.setHiddenLabel(stObject.stringValue());
//                break;

    }

    /** Go through all of the master Resources and eliminate those that
     * don't have a type of interest. Also, do "type checking" of
     * the members of concept schemes and collections.
     */
    private void freezeResources() {
        // Resources can be marked inScheme, even if not concepts.
        // We throw those away.
        if (!includeConceptSchemes && !includeCollections) {
            // This is "classic" mode. Almost anything goes.
            return;
        }
        List<String> masterIRIsToRemove = new ArrayList<>();
        // Not needed; left in case needed in the future.
//        Set<Resource> masterResourcesToRemove = new HashSet<>();
        for (Entry<String, Resource> masterResourceEntry
                : iriMasterResourceMap.entrySet()) {
            Resource masterResource = masterResourceEntry.getValue();
//            logger.info("master resource iri:"
//                    + masterResourceEntry.getKey()
//                    + " ; type: " + masterResource.getType());
            if (masterResource.getType() == null) {
                masterIRIsToRemove.add(masterResourceEntry.getKey());
//                masterResourcesToRemove.add(masterResource);
            }
        }
        for (String masterResourceIRI : masterIRIsToRemove) {
            iriMasterResourceMap.remove(masterResourceIRI);
        }
        // Note well: now that we've removed entries from
        // iriMasterResourceMap, we mustn't call
        // getMasterResource() again.

        // For further consideration: do we have to remove entries
        // from other data structures?

        // Check that all members of all concept schemes are concepts.
        for (Entry<Resource, Set<Resource>> csMemberEntry
                : conceptSchemeMasterMembers.entrySet()) {
            Set<Resource> memberSet = csMemberEntry.getValue();
            List<Resource> membersToRemove = new ArrayList<>();
            for (Resource member : memberSet) {
                ResourceType type = member.getType();
                if (type == null) {
                    membersToRemove.add(member);
                } else {
                    // An error if we find a member that's either a
                    // concept scheme or a collection.
                    if (type != ResourceType.CONCEPT) {
                        String error = RDF_ERROR_CS_MEMBER_NOT_CONCEPT
                                + member.getIri();
                        addRdfError(error);
                    }
                }
            }
            memberSet.removeAll(membersToRemove);
        }

        // Check that all members of all (unordered) collections are not
        // concept schemes.
        for (Resource collection : collectionMap.values()) {
            Set<Resource> memberSet = collection.getScaffoldMembers();
            if (memberSet != null) {
                for (Resource member : memberSet) {
                    ResourceType type = member.getType();
                    if (type == ResourceType.CONCEPT_SCHEME) {
                        // An error if we find a member that's a concept
                        // scheme, i.e., of a known type but neither a
                        // concept nor a collection.
                        String error = RDF_ERROR_COLL_MEMBER_NOT_VALID
                                + member.getIri();
                        addRdfError(error);
                    }
                }
            }
        }
        // We can't do ordered collections here, as the lists aren't
        // yet populated. The type checking of ordered collections is done
        // in getOrderedMembersForCollection.
    }

    /** Build the resource forest.
     *  @return The forest of resources, represented as a TreeSet.
     *  The values are Resources. Each value represents
     *  one resource and each of its "narrower" resources.
     */
    public TreeSet<ResourceOrRef> buildForest() {
        // First, "freeze" all resources that don't already have
        // types assigned.
        freezeResources();

        populateRoots();
        for (ResourceOrRef rootResourceOrRef : roots) {
            // If we ever add the inserted flag, do this here:
            //     root.setInsertedIntoTree(true);

            // Now, notice that roots is only ever added to
            // using the method addRoot(Resource). So we know
            // that rootResourceOrRef must be a Resource.
            Resource root = (Resource) rootResourceOrRef;

            switch (root.getType()) {
            case CONCEPT:
                depthFirstSearchConcept(root, true);
                break;
            case CONCEPT_SCHEME:
                depthFirstSearchConceptScheme(root);
                break;
            case ORDERED_COLLECTION:
            case UNORDERED_COLLECTION:
                depthFirstSearchCollection(root);
                break;
            case CONCEPT_REF:
            case UNORDERED_COLLECTION_REF:
            case ORDERED_COLLECTION_REF:
                // Can't happen!
                logger.error("Defect: somehow saw ..._REF");
                throw new IllegalArgumentException("buildForest: "
                        + "broken resource type");
            default:
                logger.error("Defect: missing case in switch: "
                        + root.getType());
                throw new IllegalArgumentException("buildForest: "
                        + "missing case in switch");
            }
        }
        // Remove any nodes that we don't expect to have visited,
        // i.e., deputies.
        // Uncomment as needed for debugging.
//        if (!nodesNotVisited.isEmpty()) {
//            logger.info("nodesNotVisited not empty before pruning: "
//                    + Arrays.toString(nodesNotVisited.toArray()));
//        }
        nodesNotVisited.removeAll(masterResourcesWhichHaveADeputy);
        if (!nodesNotVisited.isEmpty()) {
            // There are still some nodes that we haven't seen.
            // But in this case, there is definitely a cycle.
            // A cycle may or may not have been detected _already_, during
            // a depth-first-search of one of the top concepts.
            // But if we get here, there is also a cycle, e.g., of concepts
            // A -> B -> C -> A, in which there are no other nodes
            // that have either A, B, C, as a narrower node.
            // In this case, neither A, B, nor C is a top concept,
            // and we won't have visited any of them yet.
            // Or, it could be a cycle of collections.
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
                // Get an arbitrary resource that has not yet been
                // visited ...
                Resource newRoot = nodesNotVisited.iterator().next();
                // ... and make it a root, then do DFS on it.
                // Future work if we support returning a result
                // even if there's a cycle:
                // mark newRoot as _belonging_ to a cycle.

                // Subtle point: because roots is a TreeSet, before we add
                // newRoot to it we must ensure that newRoot is comparable
                // to all other values in the TreeSet (i.e., the other roots).
                // But newRoot may be a collection that is itself a member
                // of an ordered collection, and may have had an
                // orderedCollectionSortOrder assigned. Comparison of that
                // with other nodes will then give an NPE. To prevent that,
                // remove any existing orderedCollectionSortOrder before
                // adding newRoot to the TreeSet.
                newRoot.setOrderedCollectionSortOrder(null);
                addRoot(newRoot);
                // We have a principle of recording an error message
                // as soon as we discover each error in the data.
                // So we "should" add an error here. But newRoot may
                // not itself be in any cycle, so the error message
                // may be "collateral" and not-very-helpful for the user.
                // Decision: we trust ourselves that we will indeed
                // subsequently find the cycle, and add an error
                // message for the back edge.
                //    String error;
                if (newRoot.getType() == ResourceType.CONCEPT) {
                    /* Trust ourselves that we will find/report the back edge.
                    error = RDF_ERROR_CYCLE_CONCEPT_UNVISITED
                            + newRoot.getIri();
                    addRdfError(error);
                    */
                    // If we ever add the inserted flag, do this here:
                    //     newRoot.setInsertedIntoTree(true);
                  depthFirstSearchConcept(newRoot, true);
                } else {
                    // Not a concept, so must be a collection.
                    /* Trust ourselves that we will find/report the back edge.
                    error = RDF_ERROR_CYCLE_COLLECTION_UNVISITED
                            + newRoot.getIri();
                    addRdfError(error);
                    */
                    // If we ever add the inserted flag, do this here:
                    //     newRoot.setInsertedIntoTree(true);
                  depthFirstSearchCollection(newRoot);
                }
            } while (!nodesNotVisited.isEmpty());
        }
        try {
            // See the note above about avoiding comparisons of
            // nodes with orderedCollectionSortOrder values and nodes without.
            // Turns out that an NPE can also come out from the following
            // code, i.e., from NotationComparator.compare(), if
            // we have broken a cycle. So we can't do the resorting
            // if we found a cycle.
            if (!cycle && maySortByNotation && notationFormat != null) {
                logger.info("Will do notation sort by " + notationFormat);
                NotationComparator comparator =
                        new NotationComparator(notationFormat);
                assignSortOrders(roots, comparator);
                if (defaultSortByNotation) {
                    // Resort the children using the computed notation order.
                    TreeSet<ResourceOrRef> resortedRoots =
                            new TreeSet<>(new PrecomputedNotationComparator());
                    resortedRoots.addAll(roots);
                    // Now remove all the notation order values.
                    resortedRoots.forEach(n -> n.setNotationSortOrder(null));
                    roots = resortedRoots;
                }
            }
        } catch (NotationException ne) {
            setNotationException(ne);
        }
        return roots;
    }

    /** Perform a depth-first search starting at a concept.
     * @param resource The resource from which to start the search.
     * @param searchRootIsAConcept The root of the search is a concept.
     *      If this is true, and {@link #includeConceptSchemes} is also
     *      true, then the values of the narrower scaffolding are
     *      filtered down to the concepts not in any concept scheme.
     */
    private void depthFirstSearchConcept(final Resource resource,
            final boolean searchRootIsAConcept) {
        nodesNotVisited.remove(resource);
        nodesActive.add(resource);
        Set<Resource> narrowerSet = resource.getScaffoldNarrower();
        if (narrowerSet != null) {
            for (Resource narrower : narrowerSet) {
                if (includeConceptSchemes && searchRootIsAConcept
                        && narrower.getScaffoldInConceptSchemes() != null) {
                    // resource is a master resource; it is being traversed
                    // from the top level of the visualization, and narrower
                    // is a member of at least one concept scheme. We won't
                    // visit it.
                    continue;
                }
                if (nodesNotVisited.contains(narrower)) {
                    resource.addChild(narrower);
                    // If we ever add the inserted flag, do this here:
                    //     narrower.setInsertedIntoTree(true);
                    depthFirstSearchConcept(narrower, searchRootIsAConcept);
                } else {
                    // We have visited this narrower concept already,
                    // which means this edge is not a tree edge,
                    // and there is either a cycle or a polyhierarchy.
                    onlyTreeEdges = false;
                    // If the narrower concept is active, we have
                    // a back edge, which means there's a cycle.
                    if (nodesActive.contains(narrower)) {
                        cycle = true;
                        String error = RDF_ERROR_CYCLE_CONCEPT_BACK_EDGE
                                + resource.getIri() + " to "
                                + narrower.getIri();
                        addRdfError(error);
                        logger.debug("ConceptTreeTransformProvider: "
                                + "there's a cycle of concepts! "
                                + "Not following a back edge from "
                                + resource.getIri() + " to "
                                + narrower.getIri());
                    } else {
                        logger.debug("ConceptTreeTransformProvider: "
                                + "not following a forward or cross edge "
                                + "from concept "
                                + resource.getIri() + " to "
                                + narrower.getIri());
                    }
                    // Add as a reference. We make a new instance each
                    // time if we need one -- which we do, if we
                    // need to support sort-by-notation.
                    resource.addChild(narrower.getConceptResourceRef(
                            maySortByNotation));
                }
            }
        }
        nodesActive.remove(resource);
    }

    /** Perform a depth-first search starting at a concept scheme.
     * @param conceptScheme The concept scheme Resource from which
     *      to start the search.
     */
    private void depthFirstSearchConceptScheme(final Resource conceptScheme) {
        nodesNotVisited.remove(conceptScheme);
        nodesActive.add(conceptScheme);
        populateRootsOfConceptScheme(conceptScheme);
        nodesNotVisited.addAll(conceptSchemeDeputyMembers.get(conceptScheme));

        // now use conceptScheme.getChildren() to get the top-level concepts.
        Set<ResourceOrRef> children = conceptScheme.getChildren();
        if (children != null) {
            for (ResourceOrRef childResourceOrRef : children) {
                // We know that the top-level children are all deputy Resources.
                Resource child = (Resource) childResourceOrRef;
                if (nodesNotVisited.contains(child)) {
                    // If we ever add the inserted flag, do this here:
                    //     child.setInsertedIntoTree(true);
                    depthFirstSearchConcept(child, false);
                } else {
                    // Something wrong! We should not have visited this
                    // deputy before.
                    logger.debug("ConceptTreeTransformProvider defect: "
                                + "attempt to visit a previously-visited "
                                + "resource in concept scheme "
                                + conceptScheme.getIri() + " to "
                                + child.getIri());
                    throw new IllegalArgumentException(
                            "depthFirstSearchConceptScheme: "
                                    + "attempt to visit a previously-visited "
                                    + "resource in concept scheme "
                                    + conceptScheme.getIri() + " to "
                                    + child.getIri());
                }
            }
        }
        nodesActive.remove(conceptScheme);
    }

    /** Perform a depth-first search starting at a collection.
     * @param collection The collection Resource from which
     *      to start the search.
     */
    private void depthFirstSearchCollection(final Resource collection) {
        nodesNotVisited.remove(collection);
        nodesActive.add(collection);
        populateDeputiesOfCollection(collection);
        nodesNotVisited.addAll(collectionDeputyMembers.get(collection));
        if (collection.getType() == ResourceType.UNORDERED_COLLECTION) {
            depthFirstSearchUnorderedCollection(collection);
        } else {
            depthFirstSearchOrderedCollection(collection);
        }
        nodesActive.remove(collection);
    }

    /** Perform a depth-first search starting at an unordered collection.
     * @param collection The unordered collection Resource from which
     *      to start the search.
     */
    private void depthFirstSearchUnorderedCollection(
            final Resource collection) {
        Set<Resource> members = collection.getScaffoldMembers();
        if (members == null) {
            // Empty collection!
            return;
        }
        Map<String, Resource> deputyMap =
                collDeputiesIriResourceMaps.get(collection);
        for (Resource member : members) {
            ResourceType type = member.getType();
            // freezeResources() and populateDeputiesOfCollection() have
            // checked that the type is set to either concept or a
            // collection type, and flagged an error if it isn't.
            // But we do still keep going, so we must skip over any
            // wrong type value we find here.
            if (type == null || type == ResourceType.CONCEPT_SCHEME) {
                continue;
            }
            if (type == ResourceType.CONCEPT) {
                // No problem to add, and nothing deeper to search.
                Resource deputy = deputyMap.get(member.getIri());
                // If we ever add the inserted flag, do this here:
                //     deputy.setInsertedIntoTree(true);
                collection.addChild(deputy);
            } else {
                // A collection.
                if (nodesNotVisited.contains(member)) {
                    collection.addChild(member);
                    // If we ever add the inserted flag, do this here:
                    //     member.setInsertedIntoTree(true);
                    depthFirstSearchCollection(member);
                } else {
                    // We have visited this collection already,
                    // which means this edge is not a tree edge,
                    // and there is either a cycle or a polyhierarchy.
                    onlyTreeEdges = false;
                    // If the collection member is active, we have
                    // a back edge, which means there's a cycle.
                    if (nodesActive.contains(member)) {
                        // Cycle of nested collections.
                        cycle = true;
                        String error = RDF_ERROR_CYCLE_COLLECTION_BACK_EDGE
                                + collection.getIri() + " to "
                                + member.getIri();
                        addRdfError(error);
                        logger.debug("depthFirstSearchUnorderedCollection: "
                                + "there's a cycle of collections! "
                                + "Not following a back edge from "
                                + collection.getIri() + " to "
                                + member.getIri());
                    } else {
                        logger.debug("depthFirstSearchUnorderedCollection: "
                                + "not following a forward or cross edge "
                                + "from collection "
                                + collection.getIri() + " to "
                                + member.getIri());
                    }
                    // Add as a reference.
                    collection.addChild(member.getCollectionResourceRef());
                }
            }
        }
    }

    /** Perform a depth-first search starting at an ordered collection.
     * @param collection The unordered collection Resource from which
     *      to start the search.
     */
    private void depthFirstSearchOrderedCollection(final Resource collection) {
        // Ordered Collection; worry about the order!
        List<Resource> elementList =
                orderedCollectionMembers.get(collection);
        Map<String, Resource> deputyMap =
                collDeputiesIriResourceMaps.get(collection);
        int i = 0;
        for (Resource element : elementList) {
            ResourceType type = element.getType();
            // getOrderedMembersForCollection() and
            // populateDeputiesOfCollection() have
            // checked that the type is set to either concept or a
            // collection type, and flagged an error if it isn't.
            // But we do still keep going, so we must skip over any
            // wrong type value we find here.
            if (type == null || type == ResourceType.CONCEPT_SCHEME) {
                continue;
            }
            if (type == ResourceType.CONCEPT) {
                // No problem to add, and nothing deeper to search.
                Resource deputy = deputyMap.get(element.getIri());
                // If we ever add the inserted flag, do this here:
                //     deputy.setInsertedIntoTree(true);
                deputy.setOrderedCollectionSortOrder(i);
                collection.addChild(deputy);
            } else {
                // A collection.
                if (nodesNotVisited.contains(element)) {
                    // If we ever add the inserted flag, do this here:
                    //     element.setInsertedIntoTree(true);
                    element.setOrderedCollectionSortOrder(i);
                    collection.addChild(element);
                    depthFirstSearchCollection(element);
                } else {
                    // We have visited this collection already,
                    // which means this edge is not a tree edge,
                    // and there is either a cycle or a polyhierarchy.
                    onlyTreeEdges = false;
                    // If the collection member is active, we have
                    // a back edge, which means there's a cycle.
                    if (nodesActive.contains(element)) {
                        // Cycle of nested collections.
                        cycle = true;
                        String error = RDF_ERROR_CYCLE_COLLECTION_BACK_EDGE
                                + collection.getIri() + " to "
                                + element.getIri();
                        addRdfError(error);
                        logger.debug("depthFirstSearchOrderedCollection: "
                                + "there's a cycle of collections! "
                                + "Not following a back edge from "
                                + collection.getIri() + " to "
                                + element.getIri());
                    } else {
                        logger.debug("depthFirstSearchOrderedCollection: "
                                + "not following a forward or cross edge "
                                + "from collection "
                                + collection.getIri() + " to "
                                + element.getIri());
                    }
                    // Add as a reference.
                    ResourceOrRef ref = element.getCollectionResourceRef();
                    // If we ever add the inserted flag, do this here:
                    //     ref.setInsertedIntoTree(true);
                    ref.setOrderedCollectionSortOrder(i);
                    collection.addChild(ref);
                }
            }
            i++;
        }
    }


    /** Map of RDF List IRIs (as Strings) to Lists of their members. */
    private Map<String, List<Resource>> rdfListMembers = new HashMap<>();

    /** Populate the lists used as the values of the skos:memberList property.
     */
    private void populateLists() {
        for (Entry<String, RDFList> rdfListEntry
                : collectionMemberListMap.entrySet()) {
            String listIri = rdfListEntry.getKey();
            RDFList list = rdfListEntry.getValue();
            List<Resource> resourceList = extractRDFListElements(list);
            rdfListMembers.put(listIri, resourceList);
        }
    }

    /** Get the list of members of an Ordered Collection.
     * During construction of the list, each list member is marked
     * as having the collection as a "broader" resource.
     * If the collection has values of the scaffoldMembers property
     * that are not in the collection's memberList, an
     * IllegalArgumentException is thrown.
     * @param collection The Ordered Collection for which the list of
     *      members is to be fetched.
     * @return The list of members of the Ordered Collection.
     */
    private List<Resource> getOrderedMembersForCollection(
            final Resource collection) {
        // First, add all of the members of skos:memberList.
        List<Resource> orderedMembers = new ArrayList<>();
        String memberList = collection.getScaffoldMemberList();
        if (memberList != null) {
            for (Resource resource : rdfListMembers.get(memberList)) {
                // While we're here, typecheck the resource. It must
                // not be a concept scheme. See also freezeResources().
                if (resource.getType() == ResourceType.CONCEPT_SCHEME) {
                    String error = RDF_ERROR_COLL_MEMBER_NOT_VALID
                            + resource.getIri();
                    addRdfError(error);
                }
                orderedMembers.add(resource);
                // And record the fact that the resource is used in
                // a collection, so that it won't appear at the top level.
                resource.addScaffoldInCollections(collection);
            }
        }
        // Sigh, see if there are any skos:member stragglers.
        // For now, we don't allow them. (How would we?)
        Set<Resource> members = collection.getScaffoldMembers();
        if (members != null) {
            for (Resource member: members) {
                if (!orderedMembers.contains(member)) {
                    // Found a resource not already in the list.
                    String error = RDF_ERROR_MEMBER_NOT_IN_MEMBERLIST
                            + collection.getIri();
                    addRdfError(error);
                    // This method returns a value, so can't carry on.
                    throw new IllegalArgumentException(error);
                }
            }
        }
        return orderedMembers;
    }

    /** Populate {@link #orderedCollectionMembers} by finding all
     * Resources that are Ordered Collection, and computing the
     * list of elements by invoking
     * {@link #getOrderedMembersForCollection(Resource)}.
     * This method needs to be invoked before choosing the roots of
     * the forest, as this method is how we know which concepts are
     * in which collections, and therefore which concepts are not in
     * <i>any</i> collection.
     */
    private void populateOrderedCollectionMembers() {
        populateLists();
        // By now, collectionMap is complete, and we can use
        // collectionMap.values() reliably.
        for (Resource collection : collectionMap.values()) {
            if (collection.getType() == ResourceType.ORDERED_COLLECTION) {
                // This has the effect of setting up the inCollections
                // scaffolding for the member resources.
                orderedCollectionMembers.put(collection,
                        getOrderedMembersForCollection(collection));
            }
        }
    }

    /** Populate the roots of the forest, which are the top-most resources.
     * If neither {@link #includeConceptSchemes} nor {@link #includeCollections}
     * is true, then a resource is considered to be "top-most" if it is a
     * SKOS Concept and it does not specify any broader concepts.
     * If either flag is true, then concept schemes and/or collections
     * are also considered to be "top-most". (We also handle
     * nesting of collections.)
     * This (probably) catches both concepts explicitly
     * labelled as top concepts, and also any "dangling" concepts. */
    private void populateRoots() {
        populateOrderedCollectionMembers();
        for (Resource resource : getAllMasterResources()) {
//            logger.info("Considering resource: " + resource.getIri());
            ResourceType type = resource.getType();
            // We're always interested in Concepts, but we're only
            // interested in concept schemes and collections if the
            // corresponding browse flags are set.
            if (type == ResourceType.CONCEPT
                    || (includeConceptSchemes
                            && type == ResourceType.CONCEPT_SCHEME)
                    || (includeCollections
                            && (type == ResourceType.UNORDERED_COLLECTION
                                || type == ResourceType.ORDERED_COLLECTION))
                    ) {
                // Add all resources of interest to nodesNotVisited ...
                nodesNotVisited.add(resource);
                // Uncomment as needed for testing.
//                logger.info("Adding resource to nodesNotVisited: "
//                        + resource.getIri());

                // ... but certain resources to the set of topmost resources.

                if (!includeConceptSchemes && !includeCollections) {
                    // "Classic mode".
                    // (Unlike the other three cases, there's no need for a
                    // type check; we know that resource must be a concept.)
                    if (resource.getScaffoldBroader() == null) {
                        // A concept with nothing broader.
                        addRoot(resource);
                    }
                } else if (includeConceptSchemes && !includeCollections) {
                    // resource is either a concept scheme or a concept.
                    if ((resource.getType() == ResourceType.CONCEPT_SCHEME)
                            ||
                            (resource.getType() == ResourceType.CONCEPT
                            && resource.getScaffoldInConceptSchemes() == null
                            && !conceptHasBroaderResourceNotInAnyConceptScheme(
                                    resource))) {
                        addRoot(resource);
                    }
                } else if (!includeConceptSchemes && includeCollections) {
                    // resource is either a collection or a concept.
                    if (
                        // collection types
                            ((resource.getType()
                              == ResourceType.UNORDERED_COLLECTION
                            || resource.getType()
                              == ResourceType.ORDERED_COLLECTION)
                            && resource.getScaffoldInCollections() == null)
                            ||
                        // concept
                            (resource.getType() == ResourceType.CONCEPT
                            &&
                            (
                                (resource.getScaffoldNarrower() != null
                                    && resource.getScaffoldBroader() == null)
                                ||
                                (resource.getScaffoldNarrower() == null
                                && resource.getScaffoldBroader() == null
                                && resource.getScaffoldInCollections() == null)
                                    )
                            )) {
                        addRoot(resource);
                    }
                } else if (includeConceptSchemes && includeCollections) {
                    // resource is either a concept scheme, a collection,
                    // or a concept.
                    if (
                        // concept scheme
                            (resource.getType() == ResourceType.CONCEPT_SCHEME)
                            ||
                        // collection types
                            ((resource.getType()
                              == ResourceType.UNORDERED_COLLECTION
                            || resource.getType()
                              == ResourceType.ORDERED_COLLECTION)
                            && resource.getScaffoldInCollections() == null)
                            ||
                        // concept
                            (resource.getType() == ResourceType.CONCEPT
                            &&
                            resource.getScaffoldInConceptSchemes() == null
                            &&
                            (
                              (resource.getScaffoldInCollections() == null
                                &&
                                !conceptHasBroaderResourceNotInAnyConceptScheme(
                                        resource)
                                )
                              ||
                              (resource.getScaffoldInCollections() != null
                                &&
                                !conceptHasBroaderResourceNotInAnyConceptScheme(
                                        resource)
                                &&
                                conceptHasNarrowerResourceNotInAnyConceptScheme(
                                        resource)
                              )
                            )
                            )) {
                        addRoot(resource);
                    }
                }
            }
        }
    }

    /** Add a Resource as a root. It will appear at the
     * top level of the visualization.
     * @param resource The resource to be added at the top level.
     */
    public void addRoot(final Resource resource) {
        // Uncomment as needed for debugging.
//        logger.debug("Adding to roots: " + resource.getIri());
        roots.add(resource);
    }

    /** Test if a concept Resource has a "broader" resource
     * (from the scaffolding) that belongs to a specified
     * concept scheme.
     * @param concept The concept Resource to be tested.
     * @param conceptsInConceptSchemeMasters The Set of all master
     *      concepts in the concept scheme.
     * @return True, if the concept has a "broader" resource (from
     *      the scaffolding) that belongs to the concept scheme.
     */
    private boolean conceptHasBroaderResourceInConceptScheme(
            final Resource concept,
            final Set<Resource> conceptsInConceptSchemeMasters) {
        Set<Resource> broaderSet = concept.getScaffoldBroader();
        if (broaderSet == null) {
            // No broader concepts at all, so a root.
            return false;
        }
        for (Resource broader : broaderSet) {
            if (conceptsInConceptSchemeMasters.contains(broader)) {
                return true;
            }
        }
        return false;
    }

    /** Test if a concept Resource has a "broader" resource
     * (from the scaffolding) that does not belong to any concept scheme.
     * @param concept The concept Resource to be tested.
     * @return True, if the concept has a "broader" resource (from
     *      the scaffolding) that does not belong to any concept scheme.
     */
    private boolean conceptHasBroaderResourceNotInAnyConceptScheme(
            final Resource concept) {
        Set<Resource> broaderSet = concept.getScaffoldBroader();
        if (broaderSet == null) {
            // No broader concepts at all.
            return false;
        }
        for (Resource broader : broaderSet) {
            if (broader.getScaffoldInConceptSchemes() == null) {
                return true;
            }
        }
        return false;
    }

    /** Test if a concept Resource has a "narrower" resource
     * (from the scaffolding) that does not belong to any concept scheme.
     * @param concept The concept Resource to be tested.
     * @return True, if the concept has a "narrower" resource (from
     *      the scaffolding) that does not belong to any concept scheme.
     */
    private boolean conceptHasNarrowerResourceNotInAnyConceptScheme(
            final Resource concept) {
        Set<Resource> narrowerSet = concept.getScaffoldNarrower();
        if (narrowerSet == null) {
            // No narrower concepts at all.
            return false;
        }
        for (Resource narrower : narrowerSet) {
            if (narrower.getScaffoldInConceptSchemes() == null) {
                return true;
            }
        }
        return false;
    }

    /** Create a map instance in {@link #csDeputiesIriResourceMaps}
     * for a concept scheme, and add to it deputy instances for all
     * concepts belonging to the concept scheme.
     * @param conceptScheme The concept scheme for which deputies are
     *      to be created.
     */
    private void populateDeputiesOfConceptScheme(final Resource conceptScheme) {
        Set<Resource> deputies = new HashSet<>();
        Map<String, Resource> deputyMap = new HashMap<>();
        conceptSchemeDeputyMembers.put(conceptScheme, deputies);
        csDeputiesIriResourceMaps.put(conceptScheme, deputyMap);
        Set<Resource> conceptsInConceptScheme =
                conceptSchemeMasterMembers.get(conceptScheme);
        // For the set conceptSchemeMasterMembers, resources of unknown
        // type have already been filtered out by freezeResources().
        if (conceptsInConceptScheme != null) {
            for (Resource concept : conceptsInConceptScheme) {
                Resource deputy =
                        concept.makeDeputyForConceptScheme(conceptScheme);
                masterResourcesWhichHaveADeputy.add(concept);
                deputies.add(deputy);
                deputyMap.put(concept.getIri(), deputy);
            }
        }
        // Now go through all the deputies we just made, adjusting their
        // broader/narrower scaffolding to use the corresponding deputies.
        // (It would be nice not to have to do this in a separate step.
        // But it seems we can only do this once all the deputies have been
        // created. Is there a better way?)
        for (Resource deputy : deputies) {
            deputy.adjustScaffoldingForDeputyForConceptScheme(deputyMap);
        }
    }

    /** Add a child resource to a concept scheme for all of the concepts
     * in the concept scheme that are marked as top concepts of the
     * concept scheme, and all other concepts that are in the
     * concept scheme that do not have a "broader" resource in the same
     * concept scheme.
     * @param conceptScheme The concept scheme for which the
     *      child resources are to be added.
     */
    private void populateRootsOfConceptScheme(final Resource conceptScheme) {
        populateDeputiesOfConceptScheme(conceptScheme);
        Set<Resource> conceptsInConceptSchemeMasters =
                conceptSchemeMasterMembers.get(conceptScheme);

        Map<String, Resource> deputyIriMap =
                csDeputiesIriResourceMaps.get(conceptScheme);
        Set<Resource> conceptsInConceptSchemeDeputies =
                conceptSchemeDeputyMembers.get(conceptScheme);
        Set<Resource> conceptsToConsider =
                new HashSet<>(conceptsInConceptSchemeDeputies);
        // Top concepts are roots.
        // Map the master resources to their deputies.
        Set<Resource> topConceptsMasters =
                conceptSchemeTopConcepts.get(conceptScheme);
        if (topConceptsMasters != null) {
            for (Resource topConceptMaster : topConceptsMasters) {
                Resource topConceptDeputy = deputyIriMap.get(
                        topConceptMaster.getIri());
                if (conceptHasBroaderResourceInConceptScheme(topConceptMaster,
                        conceptsInConceptSchemeMasters)) {
                    // error
                    String error = RDF_ERROR_TOP_CONCEPT_BROADER
                            + topConceptMaster.getIri();
                    addRdfError(error);
                    // This method doesn't return a value, so carry on.
                    // No need to do:
                    //   throw new IllegalArgumentException(error);
                    return;
                }
                topConceptDeputy.setIsTopConceptOfContext(true);
                conceptScheme.addChild(topConceptDeputy);
                conceptsToConsider.remove(topConceptDeputy);
            }
        }
        // Other concepts in the concept scheme are roots if they
        // have no broader concept.
        for (Resource concept : conceptsToConsider) {
            if (!conceptHasBroaderResourceInConceptScheme(
                    concept, conceptsInConceptSchemeMasters)) {
                conceptScheme.addChild(concept);
            }
        }
        // Uncomment as needed for debugging.
//        if (conceptScheme.getChildren() == null) {
//            logger.debug("conceptScheme " + conceptScheme.getIri()
//            + " has no children.");
//        } else {
//            logger.debug("conceptScheme " + conceptScheme.getIri()
//            + " has children: "
//            + Arrays.toString(conceptScheme.getChildren().toArray()));
//        }
    }

    /** Create a map instance in {@link #collDeputiesIriResourceMaps}
     * for a collection, and add to it deputy instances for all
     * concepts belonging to the collection.
     * @param collection The collection for which deputies are
     *      to be created.
     */
    private void populateDeputiesOfCollection(final Resource collection) {
        Set<Resource> deputies = new HashSet<>();
        Map<String, Resource> deputyMap = new HashMap<>();
        collectionDeputyMembers.put(collection, deputies);
        collDeputiesIriResourceMaps.put(collection, deputyMap);
        Collection<Resource> resourcesInCollection;
        if (collection.getType() == ResourceType.UNORDERED_COLLECTION) {
            resourcesInCollection = collection.getScaffoldMembers();
        } else {
            resourcesInCollection = orderedCollectionMembers.get(collection);
        }
        if (resourcesInCollection != null) {
            for (Resource resource : resourcesInCollection) {
                // Only make deputies for concepts.
                ResourceType resourceType = resource.getType();
                if (resourceType == null) {
                    // It can happen!
                    String error = RDF_ERROR_MEMBER_UNKNOWN_TYPE
                            + collection.getIri()
                            + RDF_ERROR_MEMBER_UNKNOWN_TYPE_RESOURCE
                            + resource.getIri();
                    addRdfError(error);
                    // This method doesn't return a value, so carry on.
                    // No need to do:
                    //   throw new IllegalArgumentException(error);
                    continue;
                }
                if (resourceType == ResourceType.CONCEPT) {
                    Resource deputy = resource.makeDeputyForCollection();
                    masterResourcesWhichHaveADeputy.add(resource);
                    deputies.add(deputy);
                    deputyMap.put(resource.getIri(), deputy);
                }
            }
        }
    }

    /** Compute the sorted order of a set of concepts-or-references
     * according to a particular notation format. Each element of the
     * set will have a sort orders assigned, for the order that isn't
     * the one specified as the default sort order.
     * The same computation will be applied recursively to narrower
     * concepts-or-references.
     * @param resourceOrRefSet A TreeSet of concepts-or-references, already
     *      sorted by label, that is also to be sorted by notation.
     * @param comparator The notation-format-specific Comparator to be
     *      used to order the concepts.
     */
    private void assignSortOrders(final TreeSet<ResourceOrRef> resourceOrRefSet,
            final NotationComparator comparator) {
        if (resourceOrRefSet == null) {
            return;
        }
        int setSize = resourceOrRefSet.size();
        @SuppressWarnings("unchecked")
        Pair<ResourceOrRef, Integer>[] setSortedByNotations =
            new Pair[setSize];
        int i = 0;
        for (ResourceOrRef c : resourceOrRefSet) {
            // Take this opportunity to sort the children of this
            // child, if it has any.
            if (c instanceof Resource) {
                TreeSet<ResourceOrRef> narrowerTreeSet =
                        ((Resource) c).getChildren();
                if (narrowerTreeSet != null) {
                    assignSortOrders(narrowerTreeSet, comparator);
                    if (defaultSortByNotation) {
                        // Resort the children using the computed
                        // notation order.
                        TreeSet<ResourceOrRef> resortedNarrowerTreeSet =
                                new TreeSet<>(
                                        new PrecomputedNotationComparator());
                        resortedNarrowerTreeSet.addAll(narrowerTreeSet);
                        // Now remove all the notation order values.
                        resortedNarrowerTreeSet.forEach(
                                n -> n.setNotationSortOrder(null));
                        ((Resource) c).setChildren(resortedNarrowerTreeSet);
                    }
                }
            }
            setSortedByNotations[i] = Pair.of(c, i);
            i++;
        }
        Arrays.sort(setSortedByNotations, comparator);
        for (i = 0; i < setSize; i++) {
            Pair<ResourceOrRef, Integer> p = setSortedByNotations[i];
            ResourceOrRef c = p.getLeft();
            // Always set the notation sort order at first. If the
            // default sort order is by notation, these values will
            // be removed after we resort.
            c.setNotationSortOrder(i);
            if (defaultSortByNotation) {
                // We are going to resort the data by notation, so
                // set the label sort order values.
                c.setLabelSortOrder(p.getRight());
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
    public NotationException getNotationException() {
        return notationException;
    }

    /** Error message for a literal value of {@code rdf:type}. */
    public static final String RDF_ERROR_TYPE_LITERAL =
            "A type must be an IRI or blank node, but found a literal: ";

    /** Error message for changing the type of a resource to something
     * different and incompatible. */
    public static final String RDF_ERROR_INVALID_TYPE_CHANGE =
            "Attempt to change the type of a resource to something "
            + "incompatible: ";

    /** Error message for a literal value of {@code skos:member}. */
    public static final String RDF_ERROR_MEMBER_LITERAL =
            "The object of a skos:member triple was expected to be an IRI "
                    + "or blank node, but is a literal: ";

    /** Error message for a literal value of {@code skos:memberList}. */
    public static final String RDF_ERROR_MEMBERLIST_LITERAL =
            "The object of a skos:memberList triple was "
                    + "expected to be an IRI or blank node, but is "
                    + "a literal: ";

    /** Error message for multiple instances of {@code skos:memberList} for
     * a collection. */
    public static final String RDF_ERROR_MULTIPLE_MEMBERLIST =
            "There may only be one skos:memberList triple for a collection, "
                    + "but found more than one for: ";

    /** Error message for a literal value of a member of a
     * list specified by {@code skos:memberList}. */
    public static final String RDF_ERROR_MEMBERLIST_ELEMENT_LITERAL =
            "Every memberList element must be either an IRI or a blank node, "
                    + "but found a literal: ";

    /** Error message for a value of {@code skos:memberList} that's
     * a list that contains a cycle. */
    public static final String RDF_ERROR_MEMBERLIST_CYCLE =
            "A memberList list contains a cycle: ";

    /** Error message for a {@code skos:member} that's not in the value of
     * a collection's {@code skos:memberList}. */
    public static final String RDF_ERROR_MEMBER_NOT_IN_MEMBERLIST =
            "This OrderedCollection has at least one member not in "
                    + "its memberList: ";

    /** Error message for a top concept that has a broader concept in
     * the same concept scheme. */
    public static final String RDF_ERROR_TOP_CONCEPT_BROADER =
            "A top concept of a concept scheme has a broader concept "
                    + "in the same concept scheme: ";

    /** Error message for {@code rdf:nil} used as a list's {@code rdf:first}. */
    public static final String RDF_ERROR_LIST_FIRST_NIL =
            "List has rdf:first value that is rdf:nil: ";

    /** Error message for multiple values instances of {@code rdf:first}
     * for a list. */
    public static final String RDF_ERROR_LIST_MULTIPLE_FIRST =
            "Multiple, but different rdf:first values for RDF List: ";

    /** Error message for a literal value of {@code rdf:rest}. */
    public static final String RDF_ERROR_LIST_REST_LITERAL =
            "A List's rdf:rest can't be a literal: ";

    /** Error message for multiple {@code rdf:rest} values for a list. */
    public static final String RDF_ERROR_LIST_MULTIPLE_REST =
            "Multiple, but different rdf:rest values for RDF List: ";

    /** Error message for a value of a {@code skos:memberList} List
     * that's not known to be either a Concept or Collection.
     * (In fact, the error message is misleading, because this
     * particular check also allows concept schemes through. They're
     * caught later and reported using
     * {@link #RDF_ERROR_COLL_MEMBER_NOT_VALID}.) */
    public static final String RDF_ERROR_LIST_MEMBERLIST_ELEMENT_NOT_VALID =
            "Every memberList element must be defined "
                    + "as either a Concept or Collection, "
                    + "but found a value not defined as either type: ";

    /** Error message for concept scheme member that isn't a concept. */
    public static final String RDF_ERROR_CS_MEMBER_NOT_CONCEPT =
            "Found a concept scheme member that is not a concept: ";

    /** Error message for collection member that's a concept scheme. */
    public static final String RDF_ERROR_COLL_MEMBER_NOT_VALID =
            "Found a collection member that is a concept scheme: ";

    /** Error message for a value of a {@code skos:member}
     * that's not known to be either a Concept or Collection.
     * Part 1 of the error message. */
    public static final String RDF_ERROR_MEMBER_UNKNOWN_TYPE =
            "Every collection member must be defined "
            + "as either a Concept or Collection, "
            + "but found a value not defined as either type. Collection: ";
    /** Error message for a value of a {@code skos:member}
     * that's not known to be either a Concept or Collection.
     * Part 2 of the error message. */
    public static final String RDF_ERROR_MEMBER_UNKNOWN_TYPE_RESOURCE =
            "; resource: ";

    /* * Error message for a cycle, detected because there was a concept node
     * that wasn't visited during depth-first search. */
    /* Trust ourselves that we don't need to report this, because there
       will be an error logged for a back edge.
    public static final String RDF_ERROR_CYCLE_CONCEPT_UNVISITED =
            "There is a cycle, either in the broader/narrower hierarchy "
            + "or in the collection hierarchy; "
            + "this resource wasn't visited: ";
    */

    /* * Error message for a cycle, detected because there was a collection node
     * that wasn't visited during depth-first search. */
    /* Trust ourselves that we don't need to report this, because there
       will be an error logged for a back edge.
    public static final String RDF_ERROR_CYCLE_COLLECTION_UNVISITED =
            "There is a cycle in the collection hierarchy; "
            + "this resource wasn't visited: ";
    */

    /** Error message for a cycle of concepts, detected because we would
     * otherwise have to follow a back edge during depth-first search. */
    public static final String RDF_ERROR_CYCLE_CONCEPT_BACK_EDGE =
            "There is a cycle in the broader/narrower hierarchy; "
            + "there is a back edge from: ";

    /** Error message for a cycle of collections, detected because we would
     * otherwise have to follow a back edge during depth-first search. */
    public static final String RDF_ERROR_CYCLE_COLLECTION_BACK_EDGE =
            "There is a cycle in the collection hierarchy; "
            + "there is a back edge from: ";

    /** Error message returned in an {@link IllegalArgumentException}
     * when we can't continue. */
    public static final String UNABLE_TO_CONTINUE =
            "Unable to continue due to a previously-encountered error.";

    /** List of the errors accumulated during processing. Each element
     * is in HTML format.
     */
    private List<String> rdfErrors;

    /** HTML-escape the text of an error message.
     * This method is public so that it can be used in
     * {@link ConceptTreeTransformProvider} to escape exceptions
     * generated by the RDF parser.
     * @param error The error message to be escaped.
     * @return The error message, with HTML escaping applied.
     */
    public static String escapeRdfError(final String error) {
        return StringEscapeUtils.escapeHtml4(error);
    }

    /** Add an error found during processing. The value has HTML escaping
     * applied before being added to the list.
     * @param error The text of the error message, as plain text.
     */
    private void addRdfError(final String error) {
        if (rdfErrors == null) {
            rdfErrors = new ArrayList<>();
        }
        rdfErrors.add(escapeRdfError(error));
    }

    /** Get the list of errors found during processing. Each element is
     * in HTML format.
     * @return The list of errors, or null if there were no errors found.
     */
    public List<String> getRdfErrors() {
        return rdfErrors;
    }
}
