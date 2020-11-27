/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.utils.language;

import java.util.ArrayList;

import au.org.ands.vocabs.registry.utils.language.lsr.Entry;

/** The result of parsing a language tag. */
public class ParsedLanguage {

//    /** Logger for this class. */
//    private Logger logger = LoggerFactory.getLogger(
//            MethodHandles.lookup().lookupClass());

    /** Is the tag valid, according to the rules of BCP 47? */
    private boolean valid;

    /** If there are parse errors, a list of the errors, otherwise, null. */
    private ArrayList<String> errors;

    /** The canonical form of the tag. */
    private String canonicalForm;

    /** The textual description of the tag. */
    private String description;

    /** The language subtag. */
    private Entry language;

    /** The extlang subtag, if there is one. */
    private Entry extlang;

    /** The script subtag, if there is one. */
    private Entry script;

    /** The region subtag, if there is one. */
    private Entry region;

    /** Variant subtags. */
    private ArrayList<Entry> variants;

    /** Extension subtags. */
    private ArrayList<String> extensions;

    /** Private use subtag, if there is one. */
    private String privateUse;

    /** Set the validity of the tag.
     * @param aValid Whether the tag is to be considered valid.
     */
    public void setValid(final boolean aValid) {
        valid = aValid;
    }

    /** Get whether the tag is valid.
     * @return The validity of the tag.
     */
    public boolean isValid() {
        return valid;
    }

    /** Add a parse error. Set valid to false.
     * @param anError A parse error to add.
     */
    public void addError(final String anError) {
        valid = false;
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(anError);
    }

    /** Get the list of parse errors.
     * @return The list of parse errors; null, if there were no errors.
     */
    public ArrayList<String> getErrors() {
        return errors;
    }

//    /** Set the canonical form of the tag.
//     * @param aCanonicalForm The canonical form to set.
//     */
//    void setCanonicalForm(final String aCanonicalForm) {
//        canonicalForm = aCanonicalForm;
//    }

    /** Get the canonical form of the tag. The canonical form is
     * computed and cached, if it has not already been computed. Therefore,
     * you should only call this method once parsing is complete;
     * after this method is invoked for the first time, setters will not
     * update the canonical form.
     * @return The canonical form of the tag, if the tag is valid,
     *      otherwise, null.
     */
    public synchronized String getCanonicalForm() {
        if (!valid) {
            return null;
        }
        if (canonicalForm != null) {
            // We already computed it.
            return canonicalForm;
        }
        canonicalForm = language.getSubtag();

        // If we were going to do something about extlang values,
        // we would do it here. But for now, we aren't.

        if (script != null) {
            String scriptSubtag = script.getSubtag();
            String suppressScript = language.getSuppressScript();
            if (!(suppressScript != null
                    && suppressScript.equals(scriptSubtag))) {
                canonicalForm = canonicalForm + "-" + scriptSubtag;
            }
        }

        if (region != null) {
            canonicalForm = canonicalForm + "-" + region.getSubtag();
        }

        if (variants != null) {
            for (Entry variant : variants) {
                canonicalForm = canonicalForm + "-" + variant.getSubtag();
            }
        }

        if (extensions != null) {
            for (String extension : extensions) {
                canonicalForm = canonicalForm + "-" + extension;
            }
        }

        if (privateUse != null) {
            // We don't currently support tags that are _just_ a private
            // tag, so the following test is unnecessary; canonicalForm
            // will not be null. But do it anyway, to allow for future
            // expansion.
            if (canonicalForm != null) {
                canonicalForm = canonicalForm + "-";
            }
            canonicalForm = canonicalForm + privateUse;
        }
        return canonicalForm;
    }

    /** Get a brief textual description of the tag. This could be
     * used for indexing, for example. For now, the description of
     * the language subtag is returned.
     * @return A brief description of the tag, or null, if there
     *      is no language subtag.
     */
    public String getBriefDescription() {
        if (language == null) {
            return null;
        }
        return language.getDescription().get(0);
    }

    /** Get the textual description form of the tag. The description is
     * computed and cached, if it has not already been computed. Therefore,
     * you should only call this method once parsing is complete;
     * after this method is invoked for the first time, setters will not
     * update the description.
     * @return The textual description form of the tag, if the tag is valid,
     *      otherwise, null.
     */
    public synchronized String getDescription() {
        if (!valid) {
            return null;
        }
        if (description != null) {
            // We already computed it.
            return description;
        }
        description = language.getDescription().get(0);
        boolean parenthesisOpened = false;
        boolean parenthesisOpenedEarlier = false;

        // If we were going to do something about extlang values,
        // we would do it here. But for now, we aren't.

        if (script != null) {
            // NB: we've already "suppressed" the script, if we were
            // supposed to; see setScript().
            if (!parenthesisOpened) {
                description = description + " (";
                parenthesisOpened = true;
                parenthesisOpenedEarlier = true;
            } else {
                description = description + ", ";
            }
            description = description + script.getDescription().get(0);
        }

        if (region != null) {
            if (!parenthesisOpened) {
                description = description + " (";
                parenthesisOpened = true;
                parenthesisOpenedEarlier = true;
            } else {
                description = description + ", ";
            }
            description = description + region.getDescription().get(0);
        }

        if (variants != null) {
            if (!parenthesisOpened) {
                description = description + " (";
                parenthesisOpened = true;
            }
            for (Entry variant : variants) {
                if (parenthesisOpenedEarlier) {
                    description = description + ", ";
                } else {
                    parenthesisOpenedEarlier = true;
                }
                description = description + variant.getDescription().get(0);
            }
        }

        // It would be nice to give a human-readable description
        // of the extensions. To do that, we would need to make use
        // of the CLDR data, and it would be a lot of work.
        // That's way beyond what we need for now. So instead, we just
        // include the subtags "as is".
        if (extensions != null) {
            if (!parenthesisOpened) {
                description = description + " (";
                parenthesisOpened = true;
            }
            for (String extension : extensions) {
                if (parenthesisOpenedEarlier) {
                    description = description + ", ";
                } else {
                    parenthesisOpenedEarlier = true;
                }
                description = description + extension;
            }
        }

        if (privateUse != null) {
            // We don't currently support tags that are _just_ a private
            // tag, so the following test is unnecessary; description
            // will not be null. But do it anyway, to allow for future
            // expansion.
            if (description != null) {
                if (!parenthesisOpened) {
                    description = description + " (";
                    parenthesisOpened = true;
                } else {
                    description = description + ", ";
                }
            }
            description = description + privateUse;
        }
        if (parenthesisOpened) {
            description = description + ")";
        }
        return description;
    }

    /** Set the language subtag.
     * @param aLanguage The Entry for the language subtag to set.
     */
    public void setLanguage(final Entry aLanguage) {
        language = aLanguage;
    }

    /** Get the language subtag.
     * @return The language subtag.
     */
    public Entry getLanguage() {
        return language;
    }

    /** Set the extlang subtag.
     * @param anExtlang The Extry for the extlang subtag to set.
     */
    public void setExtlang(final Entry anExtlang) {
        extlang = anExtlang;
    }

    /** Get the extlang subtag.
     * @return The extlang subtag.
     */
    public Entry getExtlang() {
        return extlang;
    }

    /** Set the script subtag, but only if it is not the script
     * mentioned in the Suppress-Script attribute of the language.
     * Only invoke this method after {@link #setLanguage(Entry)}.
     * @param aScript The Entry for the script subtag to set.
     */
    public void setScript(final Entry aScript) {
        String scriptSubtag = aScript.getSubtag();
        String suppressScript = language.getSuppressScript();
        if (!(suppressScript != null
                && suppressScript.equals(scriptSubtag))) {
            script = aScript;
        }
    }

    /** Get the script subtag.
     * @return The script subtag.
     */
    public Entry getScript() {
        return script;
    }

    /** Set the region subtag.
     * @param aRegion The Entry for the region subtag to set.
     */
    public void setRegion(final Entry aRegion) {
        region = aRegion;
    }

    /** Get the region subtag.
     * @return The region subtag.
     */
    public Entry getRegion() {
        return region;
    }

    /** Add a variant.
     * @param aVariant The Entry for the variant to add.
     */
    public void addVariant(final Entry aVariant) {
        if (variants == null) {
            variants = new ArrayList<>();
        }
        variants.add(aVariant);
    }

    /** Get the list of variants.
     * @return The list of variants; null, if there are none.
     */
    public ArrayList<Entry> getVariants() {
        return variants;
    }

    /** Add an extension.
     * @param anExtension An extension to add.
     */
    public void addExtension(final String anExtension) {
        if (extensions == null) {
            extensions = new ArrayList<>();
        }
        extensions.add(anExtension);
        // Canonicalize by sorting by the singleton.
        extensions.sort(null);
    }

    /** Get the list of extensions.
     * @return The list of extensions; null, if there are none.
     */
    public ArrayList<String> getExtensions() {
        return extensions;
    }

    /** Set the private use component of the tag.
     * @param aPrivateUse The private use to set.
     */
    public void setPrivateUse(final String aPrivateUse) {
        privateUse = aPrivateUse;
    }

    /** Get the private use component of the tag, if there is one.
     * @return The private use component of the tag, if there is one,
     *      or null, if there isn't.
     */
    public String getPrivateUse() {
        return privateUse;
    }

}
