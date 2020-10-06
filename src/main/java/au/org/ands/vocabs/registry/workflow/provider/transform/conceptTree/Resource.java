/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.provider.transform.conceptTree;

import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Parent class of the various "real" resource types. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Resource extends ResourceOrRef {

    /** Logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** The IRI of the resource. */
    @SuppressWarnings("checkstyle:VisibilityModifier")
    protected String iri;

    /** The URL of the resource, if different from the IRI. */
    @SuppressWarnings("checkstyle:VisibilityModifier")
    protected String url;

    /** The type of the resource. */
    @SuppressWarnings("checkstyle:VisibilityModifier")
    protected ResourceType type;

    /** The prefLabel of the resource. */
    @SuppressWarnings("checkstyle:VisibilityModifier")
    protected String prefLabel;

    /** The dcterms:title of the resource. */
    @SuppressWarnings("checkstyle:VisibilityModifier")
    protected String dctermsTitle;

    /** The rdfs:label of the resource. */
    @SuppressWarnings("checkstyle:VisibilityModifier")
    protected String rdfsLabel;

    /** The definition of the resource. */
    @SuppressWarnings("checkstyle:VisibilityModifier")
    protected String definition;

    /** The notation of the resource. */
    @SuppressWarnings("checkstyle:VisibilityModifier")
    protected String notation;

    /** The DC Terms description of the resource. */
    @SuppressWarnings("checkstyle:VisibilityModifier")
    protected String dctermsDescription;

    /** The "children" resources of the resource. The meaning of
     * "children" depends on the type. */
    private TreeSet<ResourceOrRef> children;

    /** Constructor with an IRI specified.
     * @param anIRI The IRI of the concept. Must be non-null.
     * @throws IllegalArgumentException Thrown if {@code anIRI == null}.
     */
    private Resource(final String anIRI) {
        if (anIRI == null) {
            // We shouldn't get this, because the parser should already
            // have found this problem. If we ever see it, it's almost
            // certainly a defect in our code.
            throw new IllegalArgumentException("Won't make a Resource "
                    + "with a null IRI");
        }
        iri = anIRI;
    }

    /** Error message returned in an {@link IllegalArgumentException}
     * when unable to normalize an IRI into a URL. */
    public static final String UNABLE_TO_NORMALIZE =
            "Unable to normalize an IRI into a URL, because the result "
            + "would be too long: ";

    /** Constructor with both an IRI and the value of the
     * {@code mayResolveResources} browse flag specified.
     * @param anIRI The IRI of the concept. Must be non-null.
     * @param mayResolveResources The value of the {@code mayResolveResources}
     *      browse flag.
     * @throws IllegalArgumentException Thrown if {@code anIRI == null}.
     */
    Resource(final String anIRI, final boolean mayResolveResources) {
        this(anIRI);
        if (mayResolveResources) {
            try {
                String canonicalURL =
                        new ParsedIRI(anIRI).normalize().toASCIIString();
                // Only store a value for url if it's different from iri.
                if (!anIRI.equals(canonicalURL)) {
                    url = canonicalURL;
                }
            } catch (URISyntaxException e) {
                // Really, we shouldn't get this, because the parser
                // should already have found this problem.
                LOGGER.error("Got URISyntaxException parsing: " + anIRI, e);
            } catch (IllegalArgumentException e) {
                // The call to toASCIIString() above leads to an invocation
                // of java.net.IDN.toASCIIInternal(), which can give an IAE
                // with message "The label in the input is too long".
                // You get this if the result is longer than 63 characters.
                LOGGER.error("Got IllegalArgumentException exception parsing: "
                        + anIRI, e);
                throw new IllegalArgumentException(UNABLE_TO_NORMALIZE + anIRI);
            }
        }
    }

    /** Get the IRI.
     * @return The value of the IRI.
     */
    @Override
    public String getIri() {
        return iri;
    }

    /** Get the URL of the resource, if there is one. There will be such
     * a value if the {@code mayResolveResources} browse flag was set, and
     * the IRI contains non-ASCII characters.
     * @return The value of the URL of the resource, or null, if
     *      there isn't one.
     */
    public String getUrl() {
        return url;
    }

    /** {@inheritDoc}
     * Set the type of the resource. Throws an IllegalArgumentException
     * if that would not be allowed, for example, if the type has already
     * been set, and the new type is incompatible.
     */
    public void setType(final ResourceType aType) {
        // LOGGER.info("Setting type for iri:" + iri + " to: " + aType);
        if (aType == ResourceType.CONCEPT_REF
                || aType == ResourceType.UNORDERED_COLLECTION_REF
                || aType == ResourceType.ORDERED_COLLECTION_REF) {
            LOGGER.error("Not allowed to set type to ..._REF");
            throw new IllegalArgumentException("Not allowed to set type "
                    + "to ..._REF");
        }
        if (type == null) {
            // No problem to set the type.
            type = aType;
        } else {
            // We may or may not allow changing the type.
            switch (type) {
            case CONCEPT:
                // Not allowed to change CONCEPT to anything else.
                if (aType != ResourceType.CONCEPT) {
                    LOGGER.error("Not allowed to change "
                            + "type from CONCEPT to anything else");
                    throw new IllegalArgumentException("Not allowed to change "
                            + "type from CONCEPT to anything else");
                }
                break;
            case CONCEPT_REF:
            case UNORDERED_COLLECTION_REF:
            case ORDERED_COLLECTION_REF:
                // Can't happen, as you can see above.
                break;
            case CONCEPT_SCHEME:
                // Not allowed to change CONCEPT_SCHEME to anything else.
                if (aType != ResourceType.CONCEPT_SCHEME) {
                    LOGGER.error("Not allowed to change "
                            + "type from CONCEPT_SCHEME to anything else");
                    throw new IllegalArgumentException("Not allowed to change "
                            + "type from CONCEPT_SCHEME to anything else");
                }
                break;
            case ORDERED_COLLECTION:
                // A change to UNORDERED_COLLECTION is ignored; no
                // other change is allowed.
                if (aType == ResourceType.UNORDERED_COLLECTION) {
                    // No problem; ignored.
                    break;
                }
                if (aType != ResourceType.ORDERED_COLLECTION) {
                    LOGGER.error("Not allowed to change "
                            + "type from ORDERED_COLLECTION to anything else");
                    throw new IllegalArgumentException("Not allowed to change "
                            + "type from ORDERED_COLLECTION to anything else");
                }
                break;
            case UNORDERED_COLLECTION:
                // An "upgrade" to ORDERED_COLLECTION is allowed; no
                // other change is allowed.
                if (aType == ResourceType.ORDERED_COLLECTION) {
                    // Allow this change.
                    type = aType;
                    break;
                }
                if (aType != ResourceType.UNORDERED_COLLECTION) {
                    LOGGER.error("Not allowed to change "
                            + "type from UNORDERED_COLLECTION to anything "
                            + "except ORDERED_COLLECTION");
                    throw new IllegalArgumentException("Not allowed to change "
                            + "type from UNORDERED_COLLECTION to anything "
                            + "except ORDERED_COLLECTION");
                }
                break;
            default:
                LOGGER.error("Defect: missing case in switch: " + type);
                throw new IllegalArgumentException("setType: "
                        + "missing case in switch");
            }
        }
    }

    /** {@inheritDoc}
     * Returns null if the type is unknown.
     */
    @Override
    public ResourceType getType() {
        return type;
    }

    /** {@inheritDoc} */
    @Override
    public String getLabel() {
        if (prefLabel != null) {
            return prefLabel;
        }
        if (dctermsTitle != null) {
            return dctermsTitle;
        }
        return rdfsLabel;
    }

    /** The language tag associated with the prefLabel, if there
     * is one; null, otherwise. */
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

    /** The language tag associated with the dctermsTitle, if there
     * is one; null, otherwise. */
    private String dctermsTitleLanguage;

    /** Set the dctermsTitle. Call this setter if the dctermsTitle has no
     * language tag.
     * @param aDctermsTitle The value of the dctermsTitle.
     */
    public void setDctermsTitle(final String aDctermsTitle) {
        // Always give priority to a dctermsTitle without a language tag.
        dctermsTitle = aDctermsTitle;
        // Reset this, in case we have already seen a dctermsTitle
        // with a language tag.
        dctermsTitleLanguage = null;
    }

    /** Set the dctermsTitle. Call this setter if the dctermsTitle has a
     * language tag.
     * @param aDctermsTitle The value of the dctermsTitle.
     * @param aLanguage The language tag of the dctermsTitle.
     * @param primaryLanguage The primary language of the vocabulary.
     */
    public void setDctermsTitle(final String aDctermsTitle,
            final String aLanguage,
            final String primaryLanguage) {
        // Give preference to labels in the primary language.
        // That means:
        // 1. If this method is called when there is not already a
        //    dctermsTitle recorded, then use aDctermsTitle/aLanguage.
        // 2. If this method is called when there _is_ already a
        //    dctermsTitle recorded, but there _is_ a language
        //    recorded, and it is not the primary language.
        //    (This gives "last one wins" behaviour.)
        // 3. Otherwise, leave the existing dctermsTitle/dctermsTitleLanguage
        //    values unchanged.

        // We apply the same clarification for prefLabels as noted
        // in setPrefLabel().
        if (dctermsTitle == null
                ||
                (dctermsTitleLanguage != null
                && !primaryLanguage.equals(dctermsTitleLanguage))
                ) {
            dctermsTitle = aDctermsTitle;
            dctermsTitleLanguage = aLanguage;
        }
        // Otherwise, leave the existing dctermsTitle unchanged.
    }

    /** The language tag associated with the rdfsLabel, if there
     * is one; null, otherwise. */
    private String rdfsLabelLanguage;

    /** Set the rdfsLabel. Call this setter if the rdfsLabel has no
     * language tag.
     * @param aRdfsLabel The value of the rdfsLabel.
     */
    public void setRdfsLabel(final String aRdfsLabel) {
        // Always give priority to an rdfsLabel without a language tag.
        rdfsLabel = aRdfsLabel;
        // Reset this, in case we have already seen an rdfsLabel
        // with a language tag.
        rdfsLabelLanguage = null;
    }

    /** Set the rdfsLabel. Call this setter if the rdfsLabel has a
     * language tag.
     * @param aRdfsLabel The value of the rdfsLabel.
     * @param aLanguage The language tag of the rdfsLabel.
     * @param primaryLanguage The primary language of the vocabulary.
     */
    public void setRdfsLabel(final String aRdfsLabel,
            final String aLanguage,
            final String primaryLanguage) {
        // Give preference to labels in the primary language.
        // That means:
        // 1. If this method is called when there is not already a
        //    rdfsLabel recorded, then use aRdfsLabel/aLanguage.
        // 2. If this method is called when there _is_ already a
        //    rdfsLabel recorded, but there _is_ a language
        //    recorded, and it is not the primary language.
        //    (This gives "last one wins" behaviour.)
        // 3. Otherwise, leave the existing rdfsLabel/rdfsLabelLanguage
        //    values unchanged.

        // We apply the same clarification for prefLabels as noted
        // in setPrefLabel().
        if (rdfsLabel == null
                ||
                (rdfsLabelLanguage != null
                && !primaryLanguage.equals(rdfsLabelLanguage))
                ) {
            rdfsLabel = aRdfsLabel;
            rdfsLabelLanguage = aLanguage;
        }
        // Otherwise, leave the existing rdfsLabel unchanged.
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
    @Override
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
    public String getNotation() {
        return notation;
    }

    /** The language tag associated with the rdfsLabel, if there
     * is one; null, otherwise. */
    private String dctermsDescriptionLanguage;

    /** Set the DC Terms description. Call this setter if the
     * DC Terms description has no language tag.
     * @param aDctermsDescription The value of the DC Terms description.
     */
    public void setDctermsDescription(final String aDctermsDescription) {
        // Always give priority to a value without a language tag.
        dctermsDescription = aDctermsDescription;
        // Reset this, in case we have already seen an value
        // with a language tag.
        dctermsDescriptionLanguage = null;
    }

    /** Set the DC Terms description. Call this setter if the value has a
     * language tag.
     * @param aDctermsDescription The value of the DC Terms description.
     * @param aLanguage The language tag of the DC Terms description.
     * @param primaryLanguage The primary language of the vocabulary.
     */
    public void setDctermsDescription(final String aDctermsDescription,
            final String aLanguage,
            final String primaryLanguage) {
        // Give preference to values in the primary language.
        // That means:
        // 1. If this method is called when there is not already a
        //    value recorded, then use aDctermsDescription/aLanguage.
        // 2. If this method is called when there _is_ already a
        //    value recorded, but there _is_ a language
        //    recorded, and it is not the primary language.
        //    (This gives "last one wins" behaviour.)
        // 3. Otherwise, leave the existing dctermsDescription/
        //    dctermsDescriptionLanguage values unchanged.

        // We apply the same clarification for prefLabels as noted
        // in setPrefLabel().
        if (dctermsDescription == null
                ||
                (dctermsDescriptionLanguage != null
                && !primaryLanguage.equals(dctermsDescriptionLanguage))
                ) {
            dctermsDescription = aDctermsDescription;
            dctermsDescriptionLanguage = aLanguage;
        }
        // Otherwise, leave the existing rdfsLabel unchanged.
    }

    /** Get the DC Terms description.
     * @return The value of the notation.
     */
    @Override
    public String getDctermsDescription() {
        return dctermsDescription;
    }

    /** Add a child resource-or-reference.
     * @param aChild A child resource-or-reference to be added
     *      to the set of children resources-or-references.
     */
    public void addChild(final ResourceOrRef aChild) {
        if (children == null) {
            children = new TreeSet<>();
        }
        children.add(aChild);
    }

    /** Get the Set of children resources-or-references. Invoked during
     * serialization into JSON.
     * @return The Set of children resources-or-references.
     */
    public TreeSet<ResourceOrRef> getChildren() {
        return children;
    }

    /** Set the Set of children resources-or-references.
     * Invoked by assignSortOrders
     * if the children concepts are being resorted by notation.
     * @param aChildren The Set of children resources-or-references.
     */
    public void setChildren(final TreeSet<ResourceOrRef> aChildren) {
        children = aChildren;
    }

    /** The {@link ResourceRef} instance corresponding to this
     * instance, if one has been requested, otherwise null. */
    private ResourceRef resourceRef;

    /** For a Resource of type concept, get a {@link ResourceRef} instance
     * corresponding to this Resource instance.
     * If one doesn't already exist, it is created.
     * @param newInstance If true, always make a new instance. Otherwise,
     *      a cached instance may be returned.
     * @return A {@link ResourceRef} instance correspoding
     *      to this instance.
     */
    @JsonIgnore
    public synchronized ResourceRef getConceptResourceRef(
            final boolean newInstance) {
        if (type != ResourceType.CONCEPT) {
            LOGGER.error("Attempt to make singleton ResourceRef "
                    + "of something other than a concept: " + iri
                    + "; type: " + type);
            throw new IllegalArgumentException("Attempt to make singleton "
                    + "ResourceRef of something other than a concept: " + iri);
        }
        if (newInstance) {
            // We were asked for a new instance, so provide one.
            return new ResourceRef(this);
        }
        // We weren't asked for a new instance, so make one only if needed.
        if (resourceRef == null) {
            resourceRef = new ResourceRef(this);
        }
        return resourceRef;
    }

    /** For a resource of a collection type, get a new {@link ResourceRef}
     * instance corresponding to this instance.
     * This method always creates a new instance.
     * @return The {@link ResourceRef} instance correspoding
     *      to this instance.
     */
    @JsonIgnore
    public synchronized ResourceRef getCollectionResourceRef() {
        if (!((type == ResourceType.UNORDERED_COLLECTION)
                || (type == ResourceType.ORDERED_COLLECTION))) {
            LOGGER.error("Attempt to make non-singleton ResourceRef "
                    + "of something other than a collection: " + iri
                    + "; type: " + type);
            throw new IllegalArgumentException("Attempt to make non-singleton "
                    + "ResourceRef of something other than a collection: "
                    + iri);
        }
        return new ResourceRef(this);
    }

    /** {@inheritDoc}
     * Equality test based on IRI. But there should be only one
     * instance of an IRI in a Set or Map of Resources.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == null || !(other instanceof Resource)) {
            return false;
        }
        Resource otherResource = (Resource) other;
        return iri.equals(otherResource.iri);
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

    // "Scaffold properties", used to store data during parsing,
    // and then during contruction of the tree-structure,
    // but not themselves output during serialization.
    // Indeed, the @JsonIgnore annotations are strictly necessary
    // to prevent infinite loops of traversal of the children/broader
    // relations.

    /** The "broader" resources of the resource,
     *  collected during scaffolding. */
    private Set<Resource> scaffoldBroader;

    /** The "children" resources of the resource,
     *  collected during scaffolding. */
    private Set<Resource> scaffoldNarrower;

    /** The "member" resources of the resource,
     *  collected during scaffolding. */
    private Set<Resource> scaffoldMembers;

    /** The "memberList" resource of the resource,
     *  collected during scaffolding. */
    private String scaffoldMemberList;

    /** The concept schemes of which the resource is a member,
     *  collected during scaffolding. */
    private Set<Resource> scaffoldInConceptSchemes;

    /** The collections of which the resource is a member,
     *  collected during scaffolding. */
    private Set<Resource> scaffoldInCollections;

    /** Add a broader resource to the scaffolding.
     * @param aBroader The broader resource to be added.
     */
    public void addScaffoldBroader(final Resource aBroader) {
        if (scaffoldBroader == null) {
            scaffoldBroader = new HashSet<>();
        }
        scaffoldBroader.add(aBroader);
    }

    /** Get the scaffolding set of broader resources.
     * @return The value of scaffoldBroader.
     */
    @JsonIgnore
    public Set<Resource> getScaffoldBroader() {
        return scaffoldBroader;
    }

    /** Add a children resource to the scaffolding.
     * @param aNarrower The children resource to be added.
     */
    public void addScaffoldNarrower(final Resource aNarrower) {
        if (scaffoldNarrower == null) {
            scaffoldNarrower = new HashSet<>();
        }
        scaffoldNarrower.add(aNarrower);
    }

    /** Get the scaffolding set of children resources.
     * @return The value of scaffoldNarrower.
     */
    @JsonIgnore
    public Set<Resource> getScaffoldNarrower() {
        return scaffoldNarrower;
    }

    /** Add a collection member to the scaffolding.
     * @param aMember The collection member to be added.
     */
    public void addScaffoldMember(final Resource aMember) {
        if (scaffoldMembers == null) {
            scaffoldMembers = new HashSet<>();
        }
        scaffoldMembers.add(aMember);
    }

    /** Get the scaffolding set of collection members of this resource.
     * @return The value of scaffoldMembers.
     */
    @JsonIgnore
    public Set<Resource> getScaffoldMembers() {
        return scaffoldMembers;
    }

    /** Set the scaffolding memberList property of this resource.
     * @param aMemberList The value to be set for scaffoldMemberList.
     */
    public void setScaffoldMemberList(final String aMemberList) {
        scaffoldMemberList = aMemberList;
    }

    /** Get the scaffolding memberList property of this resource.
     * @return The value of scaffoldMemberList.
     */
    @JsonIgnore
    public String getScaffoldMemberList() {
        return scaffoldMemberList;
    }

    /** Add membership of a concept scheme to the scaffolding.
     * @param aConceptScheme The concept scheme to be added.
     */
    public void addScaffoldInConceptSchemes(final Resource aConceptScheme) {
        if (scaffoldInConceptSchemes == null) {
            scaffoldInConceptSchemes = new HashSet<>();
        }
        scaffoldInConceptSchemes.add(aConceptScheme);
    }

    /** Get the scaffolding set of concept schemes of which this resource
     * is a member.
     * @return The value of scaffoldInConceptSchemes.
     */
    @JsonIgnore
    public Set<Resource> getScaffoldInConceptSchemes() {
        return scaffoldInConceptSchemes;
    }

    /** Add membership of a collection to the scaffolding.
     * @param aCollection The collection to be added.
     */
    public void addScaffoldInCollections(final Resource aCollection) {
        if (scaffoldInCollections == null) {
            scaffoldInCollections = new HashSet<>();
        }
        scaffoldInCollections.add(aCollection);
    }

    /** Get the scaffolding set of collections of which this resource
     * is a member.
     * @return The value of scaffoldInCollections.
     */
    @JsonIgnore
    public Set<Resource> getScaffoldInCollections() {
        return scaffoldInCollections;
    }

    /** Make a deputy instance for use in a concept scheme.
     * @param conceptScheme The concept scheme Resource for which this
     *      deputy instance is made.
     * @return A deputy instance of this Resource. The broader and narrower
     *      scaffolding is filtered to the concept scheme.
     */
    public Resource makeDeputyForConceptScheme(final Resource conceptScheme) {
        Resource deputy = new Resource(iri);
        // Copy across all the fields that could be of interest during
        // depth-first search.
        // The fields are listed below in alphabetical order for convenience
        // of checking that they're all there.
        deputy.dctermsTitle = dctermsTitle;
        deputy.dctermsTitleLanguage = dctermsTitleLanguage;
        deputy.definition = definition;
        deputy.notation = notation;
        deputy.prefLabel = prefLabel;
        deputy.prefLabelLanguage = prefLabelLanguage;
        deputy.rdfsLabel = rdfsLabel;
        deputy.rdfsLabelLanguage = rdfsLabelLanguage;
        if (scaffoldBroader != null) {
            Set<Resource> sb = new HashSet<>();
            for (Resource b : scaffoldBroader) {
                if ((b.scaffoldInConceptSchemes != null)
                        && b.scaffoldInConceptSchemes.contains(conceptScheme)) {
                    sb.add(b);
                }
            }
            if (!sb.isEmpty()) {
                deputy.scaffoldBroader = sb;
            }
        }
        deputy.scaffoldInConceptSchemes = scaffoldInConceptSchemes;
        if (scaffoldNarrower != null) {
            Set<Resource> sn = new HashSet<>();
            for (Resource n : scaffoldNarrower) {
                if ((n.scaffoldInConceptSchemes != null)
                        && n.scaffoldInConceptSchemes.contains(conceptScheme)) {
                    sn.add(n);
                }
            }
            if (!sn.isEmpty()) {
                deputy.scaffoldNarrower = sn;
            }
        }
        deputy.type = type;
        deputy.url = url;
        return deputy;
    }

    /** Adjust the broader and narrower scaffolding of a deputy belonging
     * to a concept scheme, rewriting the resources to use the deputies
     * of created for that concept scheme.
     * @param deputyMap The concept scheme Resource for which this
     *      deputy instance is made.
     */
    public void adjustScaffoldingForDeputyForConceptScheme(
            final Map<String, Resource> deputyMap) {
        if (scaffoldBroader != null) {
            Set<Resource> newSb = new HashSet<>();
            for (Resource b : scaffoldBroader) {
                newSb.add(deputyMap.get(b.getIri()));
            }
            scaffoldBroader = newSb;
        }
        if (scaffoldNarrower != null) {
            Set<Resource> newSn = new HashSet<>();
            for (Resource n : scaffoldNarrower) {
                newSn.add(deputyMap.get(n.getIri()));
            }
            scaffoldNarrower = newSn;
        }
    }


    /** Make a deputy instance for use in a collection.
     * @return A deputy instance of this Resource. The broader and narrower
     *      scaffolding is removed.
     */
    public Resource makeDeputyForCollection() {
        Resource deputy = new Resource(iri);
        // Copy across all the fields that could be of interest during
        // depth-first search.
        // The fields are listed below in alphabetical order for convenience
        // of checking that they're all there.
        deputy.dctermsTitle = dctermsTitle;
        deputy.dctermsTitleLanguage = dctermsTitleLanguage;
        deputy.definition = definition;
        deputy.notation = notation;
        deputy.prefLabel = prefLabel;
        deputy.prefLabelLanguage = prefLabelLanguage;
        deputy.rdfsLabel = rdfsLabel;
        deputy.rdfsLabelLanguage = rdfsLabelLanguage;
        deputy.type = type;
        deputy.url = url;
        return deputy;
    }

}
