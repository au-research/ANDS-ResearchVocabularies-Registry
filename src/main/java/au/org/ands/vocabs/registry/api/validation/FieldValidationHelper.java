/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.validation;

import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Email;

/** A utility class with fields of varying types, annotated with
 * validation annotations. In combination with
 * {@link ValidationUtils#getValidator()}, these fields
 * can be used to validate dynamic values.
 * For an example, see how {@link
 * CheckRelatedEntityImpl#isValid(au.org.ands.vocabs.registry.schema.vocabulary201701.RelatedEntity,javax.validation.ConstraintValidatorContext)}
 * validates an email address.
 */
public class FieldValidationHelper {

    /** The name of the email property. */
    public static final String EMAIL_FIELDNAME = "email";

    /** A property annotated with the {@link Email} validation annotation.
     * @return null
     */
    @Email
    public String getEmail() {
        return null;
    }

    // NB: the regexp attribute of @Pattern annotations is used
    // to match against the entire value. So "^" and "$" are of
    // no use here, and you might have to end with ".*".

    /** The name of the AU-ANL:PEAU property. */
    public static final String AU_ANL_PEAU_FIELDNAME = "auAnlPeau";

    /** A property annotated with a validation annotation that validates
     * AU-ANL:PEAU identifiers.
     * @return null
     */
    @Pattern(regexp = "nla\\.party-\\d+")
    public String getAuAnlPeau() {
        return null;
    }

    /** The name of the doi property. */
    public static final String DOI_FIELDNAME = "doi";

    /** A property annotated with a validation annotation that validates
     * DOI identifiers.
     * @return null
     */
    @Pattern(regexp = "10\\..+")
    public String getDoi() {
        return null;
    }

    /** The name of the handle property. */
    public static final String HANDLE_FIELDNAME = "handle";

    /** A property annotated with a validation annotation that validates
     * Handle identifiers.
     * @return null
     */
    @Pattern(regexp = "\\d.*/.+")
    public String getHandle() {
        return null;
    }

    /** The name of the INFO URI property. */
    public static final String INFOURI_FIELDNAME = "infouri";

    /** A property annotated with a validation annotation that validates
     * INFO URI identifiers. See <a href=
     * "http://info-uri.info/registry/docs/misc/faq.html">http://info-uri.info/registry/docs/misc/faq.html</a>.
     * @return null
     */
    @Pattern(regexp = "info:.+")
    public String getInfouri() {
        return null;
    }

    /** The name of the ISIL property. */
    public static final String ISIL_FIELDNAME = "isil";

    /** A property annotated with a validation annotation that validates
     * ISIL identifiers.
     * See <a
     * href="http://biblstandard.dk/isil/structure.htm">http://biblstandard.dk/isil/structure.htm</a>,
     * <a
     * href="https://www.wikidata.org/wiki/Property:P791">https://www.wikidata.org/wiki/Property:P791</a>.
     * @return null
     */
    // NB: for presentation, value must be preceded by "ISIL ".
    @Pattern(regexp =
            "([A-Z]{2}|[A-Za-z]|[A-Za-z]{3,4})-[A-Za-z0-9:/\\-]{1,11}")
    public String getIsil() {
        return null;
    }

    /** The name of the ISNI property. */
    public static final String ISNI_FIELDNAME = "isni";

    /** A property annotated with a validation annotation that validates
     * ISNI identifiers. See <a href=
     * "https://www.wikidata.org/wiki/Property:P213">https://www.wikidata.org/wiki/Property:P213</a>.
     * @return null
     */
    // If this were for matching display purposes, we might use:
//    @Pattern(regexp = "\\d{4} \\d{4} \\d{4} \\d{3}[\\dX]")
    // or
    //  @Pattern(regexp = "\\d{4} ?\\d{4} ?\\d{4} ?\\d{3}[\\dX]")
    @Pattern(regexp = "\\d{15}[\\dX]")
    public String getIsni() {
        return null;
    }

    /** The name of the ORCID property. */
    public static final String ORCID_FIELDNAME = "orcid";

    /** A property annotated with a validation annotation that validates
     * ORCID identifiers. Note that only the numeric value is used; these
     * are not complete URLs. See <a href=
     * "https://support.orcid.org/knowledgebase/articles/116780-structure-of-the-orcid-identifier">https://support.orcid.org/knowledgebase/articles/116780-structure-of-the-orcid-identifier</a>.
     * @return null
     */
    @Pattern(regexp = "\\d{4}-\\d{4}-\\d{4}-\\d{3}[\\dX]")
    public String getOrcid() {
        return null;
    }

    /** The name of the PURL property. */
    public static final String PURL_FIELDNAME = "purl";

    /** A property annotated with a validation annotation that validates
     * PURL identifiers.
     * @return null
     */
    @Pattern(regexp = "https?://purl.org/.+")
    public String getPurl() {
        return null;
    }

    /** The name of the Researcher ID property. */
    public static final String RESEARCHER_ID_FIELDNAME = "researcherId";

    /** A property annotated with a validation annotation that validates
     * Researcher ID identifiers.
     * @return null
     */
    @Pattern(regexp = "[A-Z]{1,3}-[1-9][0-9]{3}-[0-9]{4}")
    public String getResearcherId() {
        return null;
    }

    /** The name of the VIAF property. */
    public static final String VIAF_FIELDNAME = "viaf";

    /** A property annotated with a validation annotation that validates
     * VIAF identifiers. See <a href=
     * "https://www.wikidata.org/wiki/Property:P214">https://www.wikidata.org/wiki/Property:P214</a>.
     * @return null
     */
    @Pattern(regexp = "[1-9]\\d(\\d{0,7}|\\d{17,20})")
    public String getViaf() {
        return null;
    }

//    /** The name of the  property. */
//    public static final String _FIELDNAME = "";
//
//    /** A property annotated with a validation annotation that validates
//     *  identifiers.
//     * @return null
//     */
//    @Pattern(regexp = "")
//    public String get() {
//        return null;
//    }

}
