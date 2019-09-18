/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.solr;

/** Constants for Solr fields. */
public final class FieldConstants {

    /** Private constructor for a utility class. */
    private FieldConstants() {
    }

    /* Names of Solr fields. Listed in alphabetical order! */

    /** The name of the "access" Solr field. */
    public static final String ACCESS = "access";
    /** The name of the "acronym" Solr field. */
    public static final String ACRONYM = "acronym";
    /** The name of the "concept" Solr field. */
    public static final String CONCEPT = "concept";
    /** The name of the "concept_phrase" Solr field. */
    public static final String CONCEPT_PHRASE = "concept_phrase";
    /** The name of the "concept_search" Solr field. */
    public static final String CONCEPT_SEARCH = "concept_search";
    /** The name of the "description" Solr field. */
    public static final String DESCRIPTION = "description";
    /** The name of the "description_phrase" Solr field. */
    public static final String DESCRIPTION_PHRASE = "description_phrase";
    /** The name of the "format" Solr field. */
    public static final String FORMAT = "format";
    /** The name of the "fulltext" Solr field. */
    public static final String FULLTEXT = "fulltext";
    /** The name of the "id" Solr field. */
    public static final String ID = "id";
    /** The name of the "language" Solr field. */
    public static final String LANGUAGE = "language";
    /** The name of the "last_updated" Solr field. */
    public static final String LAST_UPDATED = "last_updated";
    /** The name of the "licence" Solr field. */
    public static final String LICENCE = "licence";
    /** The name of the "note" Solr field, used for the top-level
     * vocabulary notes. */
    public static final String NOTE = "note";
    /** The name of the "note_phrase" Solr field, used for the top-level
     * vocabulary notes. */
    public static final String NOTE_PHRASE = "note_phrase";
    /** The name of the "owner" Solr field. */
    public static final String OWNER = "owner";
    /** The name of the "poolparty_id" Solr field. */
    public static final String POOLPARTY_ID = "poolparty_id";
    /** The name of the "publisher" Solr field. */
    public static final String PUBLISHER = "publisher";
    /** The name of the "publisher_phrase" Solr field. */
    public static final String PUBLISHER_PHRASE = "publisher_phrase";
    /** The name of the "publisher_search" Solr field. */
    public static final String PUBLISHER_SEARCH = "publisher_search";
    /** The name of the "schema_version" Solr user property. */
    public static final String SCHEMA_VERSION = "schema_version";
    /** The name of the "sissvoc_endpoint" Solr field. */
    public static final String SISSVOC_ENDPOINT = "sissvoc_endpoint";
    /** The name of the "slug" Solr field. */
    public static final String SLUG = "slug";
    /** The name of the "status" Solr field. */
    public static final String STATUS = "status";
    /** The name of the "subject_iris" Solr field. */
    public static final String SUBJECT_IRIS = "subject_iris";
    /** The name of the "subject_labels" Solr field. */
    public static final String SUBJECT_LABELS = "subject_labels";
    /** The name of the "subject_notations" Solr field. */
    public static final String SUBJECT_NOTATIONS = "subject_notations";
    /** The name of the "subject_phrase" Solr field. */
    public static final String SUBJECT_PHRASE = "subject_phrase";
    /** The name of the "subject_search" Solr field. */
    public static final String SUBJECT_SEARCH = "subject_search";
    /** The name of the "subject_sources" Solr field. This was formerly
     * known as "subject_types". */
    public static final String SUBJECT_SOURCES = "subject_sources";
    /** The name of the "title" Solr field. */
    public static final String TITLE = "title";
    /** The name of the "title_phrase" Solr field. */
    public static final String TITLE_PHRASE = "title_phrase";
    /** The name of the "title_search" Solr field. */
    public static final String TITLE_SEARCH = "title_search";
    /** The name of the "title_sort" Solr field. */
    public static final String TITLE_SORT = "title_sort";
    /** The name of the "top_concept" Solr field. */
    public static final String TOP_CONCEPT = "top_concept";
    /** The name of the "widgetable" Solr field. */
    public static final String WIDGETABLE = "widgetable";

    // Fields used only for the resources collection.

    /** The name of the "iri" Solr field. */
    public static final String IRI = "iri";
    /** The name of the Solr document field used for RDF types. */
    public static final String RDF_TYPE = "rdf_type";
    /** The name of the Solr document field used for RDFS labels. */
    public static final String RDFS_LABEL = "rdfs_label";
    /** The name of the Solr document field used for SKOS altLabels. */
    public static final String SKOS_ALTLABEL = "skos_altLabel";
    /** The name of the Solr document field used for SKOS hiddenLabels. */
    public static final String SKOS_HIDDENLABEL = "skos_hiddenLabel";
    /** The name of the Solr document field used for SKOS definitions. */
    public static final String SKOS_DEFINITION = "skos_definition";
    /** The name of the Solr document field used for SKOS notations. */
    public static final String SKOS_NOTATION = "skos_notation";
    /** The name of the Solr document field used for SKOS prefLabels. */
    public static final String SKOS_PREFLABEL = "skos_prefLabel";
    /** The name of the "top_concept_phrase" Solr field. */
    public static final String TOP_CONCEPT_PHRASE = "top_concept_phrase";
    /** The name of the "version_id" Solr field. */
    public static final String VERSION_ID = "version_id";
    /** The name of the "vocabulary_id" Solr field. */
    public static final String VOCABULARY_ID = "vocabulary_id";

    /* Names of Solr field types. Listed in alphabetical order! */

    /** The name of the custom "alphaOnlySort" Solr field type. */
    public static final String ALPHA_ONLY_SORT = "alphaOnlySort";
    /** The name of the "boolean" Solr field type. */
    public static final String BOOLEAN = "boolean";
    /** The name of the "pdate" Solr field type, based on the
     * "solr.DatePointField" class. */
    public static final String DATE_POINT = "pdate";
    /** The name of the custom "lower_exact_words" Solr field type. */
    public static final String LOWER_EXACT_WORDS = "lower_exact_words";
    /** The name of the "string" Solr field type. */
    public static final String STRING = "string";
    /** The name of the "text_en_splitting" Solr field type. */
    public static final String TEXT_EN_SPLITTING = "text_en_splitting";


}
