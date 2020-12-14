/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.utils.language;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.utils.RegistryConfig;
import au.org.ands.vocabs.registry.utils.RegistryFileUtils;
import au.org.ands.vocabs.registry.utils.language.lsr.Entry;

/** Support for the IANA language subtag registry. */
public final class LanguageSubtagRegistry {

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Singleton instance of this class. */
    private static final LanguageSubtagRegistry INSTANCE =
            new LanguageSubtagRegistry();

    /** The parsed language subtag registry. */
    private LanguageSubtagRegistryParser lsrParser;

    /** The map of languages in the registry. */
    private Map<String, Entry> languagesMap;

    /** The map of scripts in the registry. */
    private Map<String, Entry> scriptsMap;

    /** The map of regions in the registry. */
    private Map<String, Entry> regionsMap;

    /** The map of grandfathered tags in the registry. */
    private Map<String, Entry> grandfatheredsMap;

    /** The map of redundant tags in the registry. */
    private Map<String, Entry> redundantsMap;

    /** The map of variants in the registry. */
    private Map<String, Entry> variantsMap;

    /** The map of extlangs in the registry. */
    private Map<String, Entry> extlangsMap;

    /** Default constructor, used to create the singleton instance. */
    private LanguageSubtagRegistry() {
        // Source:
        String registryFilename = RegistryConfig.LSR_FILE_PATH;
        // Destination:
        Path lsrDirectory = Paths.get(RegistryConfig.LSR_OUTPUT_FILES_PATH);
        RegistryFileUtils.requireDirectory(lsrDirectory.toString());
        try {
            lsrParser = new LanguageSubtagRegistryParser(registryFilename,
                    lsrDirectory);
            languagesMap = lsrParser.getLanguagesMap();
            scriptsMap = lsrParser.getScriptsMap();
            regionsMap = lsrParser.getRegionsMap();
            grandfatheredsMap = lsrParser.getGrandfatheredsMap();
            redundantsMap = lsrParser.getRedundantsMap();
            variantsMap = lsrParser.getVariantsMap();
            extlangsMap = lsrParser.getExtlangsMap();
        } catch (IOException e) {
            logger.error("Fatal error parsing language subtag registry", e);
        } catch (JAXBException e) {
            logger.error("Fatal error processing language subtag registry", e);
        }
    }

    /** Return the singleton instance of the language subtag registry.
     * @return The singleton instance of the language subtag registry.
     */
    public static LanguageSubtagRegistry getLSR() {
        return INSTANCE;
    }

    /** Get the absolute path to the generated <code>lsr.xml</code> file.
     * @return The absolute path to the generated <code>lsr.xml</code> file.
     */
    public String getLsrXmlFilename() {
        return lsrParser.getLsrXmlFilename();
    }

    /** Get the absolute path to the generated <code>lsr.xml.gz</code> file.
     * @return The absolute path to the generated <code>lsr.xml.gz</code> file.
     */
    public String getLsrXmlGzFilename() {
        return lsrParser.getLsrXmlGzFilename();
    }

    /** Get the absolute path to the generated <code>lsr.json</code> file.
     * @return The absolute path to the generated <code>lsr.json</code> file.
     */
    public String getLsrJsonFilename() {
        return lsrParser.getLsrJsonFilename();
    }

    /** Get the absolute path to the generated <code>lsr.json.gz</code> file.
     * @return The absolute path to the generated <code>lsr.json.gz</code> file.
     */
    public String getLsrJsonGzFilename() {
        return lsrParser.getLsrJsonGzFilename();
    }

    /** Error message used if the tag is null. */
    public static final String ERROR_NULL_TAG = "The tag may not be null.";

    /** Error message used if the tag is an empty string. */
    public static final String ERROR_EMPTY_TAG = "The tag may not be empty.";

    /** Error message used if the tag contains an empty subtag. */
    public static final String ERROR_EMPTY_SUBTAG =
            "The tag contains an empty subtag.";

    /** Error message used if the tag contains an internal space. */
    public static final String ERROR_INTERNAL_SPACE =
            "The tag contains an internal space.";

    /** Error message used if the tag either begins or ends with a hyphen. */
    public static final String ERROR_BEGIN_END_HYPHEN =
            "The tag either begins or ends with hyphen.";

    /** Error message used if the tag contains an invalid character. */
    public static final String ERROR_INVALID_CHARACTER =
            "The tag contains an invalid character.";

    /** Error message used if the tag is grandfathered, but there is
     * no preferred value. */
    public static final String ERROR_GRANDFATHERED_DEPRECATED =
            "The tag is grandfathered, but there is no preferred value.";

    /** Error message used if the tag begins with a private use component. */
    public static final String ERROR_BEGINS_PRIVATE_USE =
            "The tag begins with a private use tag.";

    /** Error message used if the tag contains more than one private use
     *      subtag. */
    public static final String ERROR_MULTIPLE_PRIVATE_USE =
            "The tag contains more than one private use subtag.";

    /** Error message used if the private use subtag contains a component
     *      that's too long. */
    public static final String ERROR_PRIVATE_USE_COMPONENT_LENGTH =
            "The tag contains a private use subtag with a component that "
            + "is too long.";

    /** Error message used if there is a duplicated extension singleton. */
    public static final String ERROR_DUPLICATE_SINGLETON =
            "The tag contains a duplicate extension singleton.";

    /** Error message used if there is an invalid extension singleton. */
    public static final String ERROR_INVALID_SINGLETON =
            "The tag contains an invalid extension singleton.";

    /** Error message used if an extension subtag is too long. */
    public static final String ERROR_EXTENSION_COMPONENT_LENGTH =
            "The tag contains an extension subtag that is too long.";

    /** Error message used if the tag begins with an extension. */
    public static final String ERROR_BEGINS_EXTENSION =
            "The tag begins with an extension.";

    /** Error message used if the tag begins with an unknown language subtag. */
    public static final String ERROR_PRIVATE_USE_LANGUAGE_UNSUPPORTED =
            "Private use language subtags are not supported.";

    /** Error message used if the tag begins with an unknown language subtag. */
    public static final String ERROR_UNKNOWN_LANGUAGE_SUBTAG =
            "The tag begins with an unknown language subtag.";

    /** Error message used if the tag begins with a disallowed
     *      language subtag. */
    public static final String ERROR_DISALLOWED_LANGUAGE_SUBTAG =
            "The tag begins with a disallowed language subtag.";

    /** Error message used if a subtag (not an extension or private use)
     *      is too long. */
    public static final String ERROR_SUBTAG_TOO_LONG =
            "The tag contains a subtag that is too long.";

    /** Error message used if the tag contains a misplaced extlang subtag. */
    public static final String ERROR_EXTLANG_NOT_ALLOWED =
            "The tag contains a misplaced extlang subtag.";

    /** Error message used if the tag contains a misplaced script subtag. */
    public static final String ERROR_SCRIPT_NOT_ALLOWED =
            "The tag contains a misplaced script subtag.";

    /** Error message used if the tag contains a misplaced region subtag. */
    public static final String ERROR_REGION_NOT_ALLOWED =
            "The tag contains a misplaced region subtag.";

    /** Error message used if the tag contains an unknown extlang subtag. */
    public static final String ERROR_UNKNOWN_EXTLANG_SUBTAG =
            "The tag contains an unknown extlang subtag.";

    /** Error message used if the tag contains an unknown script subtag. */
    public static final String ERROR_UNKNOWN_SCRIPT_SUBTAG =
            "The tag contains an unknown script subtag.";

    /** Error message used if the tag contains an unknown region subtag. */
    public static final String ERROR_UNKNOWN_REGION_SUBTAG =
            "The tag contains an unknown region subtag.";

    /** Error message used if the tag contains an unknown variant subtag. */
    public static final String ERROR_UNKNOWN_VARIANT_SUBTAG =
            "The tag contains an unknown variant subtag.";

    /** Error message used if the tag contains a duplicate variant subtag. */
    public static final String ERROR_DUPLICATE_VARIANT_SUBTAG =
            "The tag contains a duplicate variant subtag.";

    /** Error message used if the tag contains an extlang subtag,
     *      but the extlang's prefix doesn' match the language subtag. */
    public static final String ERROR_EXTLANG_PREFIX_DIFFERS =
            "The tag contains an extlang whose prefix doesn't match "
            + "the language subtag.";

    /** Pattern that matches any invalid character in a tag. Note that
     * parsing is done on values that have already been lowercased, so
     * we don't bother to include uppercase characters in the
     * regular expression. Also, we don't consider a space as invalid
     * here, because the presence of internal spaces is checked separately. */
    private static final Pattern INVALID_CHARACTERS =
            Pattern.compile(".*[^a-z0-9- ].*");

    /** Pattern that matches a hyphen. Used to split a tag into subtags. */
    private static final Pattern HYPHEN = Pattern.compile("-");

    /** Length of a private use language subtag ("qaa".."qtz"). */
    private static final int PRIVATE_USE_LANGUAGE_SUBTAG_LENGTH = 3;

    /** Maximum length of each subtag. */
    private static final int SUBTAG_MAX_LENGTH = 8;

    /** Pattern that matches a three-digit region. */
    private static final Pattern REGION_DIGITS = Pattern.compile("[0-9]{3}");

    /** Get the "preferred" entry for a subtag. For most subtags, this
     * means the entry contained in the map for the subtag. But if
     * that entry has a Preferred-Value, then the entry for the
     * preferred value is returned.
     * @param subtag The subtag to be looked up in the map.
     * @param map The map of subtags to entries to be used for the lookup.
     * @return The "preferred" entry for subtag, or null, if there is
     *      no entry in map for subtag.
     */
    private Entry getPreferredEntry(final String subtag,
            final Map<String, Entry> map) {
        Entry entry = map.get(subtag);
        if (entry == null) {
            return null;
        }
        String preferredValue = entry.getPreferredValue();
        if (preferredValue == null) {
            return entry;
        }
        return map.get(preferredValue.toLowerCase(Locale.ROOT));
    }

    /** Parse a tag.
     * @param tag A supposed language tag.
     * @return The parsed tag. An instance of {@link ParsedLanguage}.
     *      Use the getters of the result to see if it is valid and
     *      to get the components of the parsed value.
     *      The tag is invalid, if it contains a deprecated subtag for
     *      which there is no preferred value.
     *      Because the parsed language subtag registry does not contain
     *      subtags marked as "private use", if the tag contains such
     *      a subtag, it will be invalid.
     *      If the tag contains a subtag for which there is a preferred value,
     *      the subtag is parsed as that preferred value.
     *      If the tag is grandfathered, and there is a preferred value,
     *      the tag is parsed as that preferred value, otherwise, it is
     *      rejected as invalid (because it is deprecated).
     *      If the tag is redundant, and there is a preferred value,
     *      it is parsed as the preferred value, otherwise, it is parsed
     *      using the individual subtags.
     *      A tag that consists only of extensions and private use subtags
     *      is considered invalid, even if it is well-formed.
     *      Because satisfying RFC 5646 section 4.1, point 6, on the
     *      validity and ordering of variants, is hard work, we don't yet
     *      do all of those strictly-necessary validity checks.
     */
    // Sorry for the method length. We very closely follow Richard Ishida's
    // implementation.
    @SuppressWarnings("checkstyle:MethodLength")
    public ParsedLanguage parseTag(final String tag) {
        ParsedLanguage parsedLanguage = new ParsedLanguage();
        // Start by assuming that the tag is valid.
        // FYI: addError() sets valid to false.
        parsedLanguage.setValid(true);
        if (tag == null) {
            // null is invalid.
            parsedLanguage.addError(ERROR_NULL_TAG);
            // Nothing more we can do.
            return parsedLanguage;
        }
        String tagNormalized = tag.trim().toLowerCase(Locale.ROOT);
        // From now on, work with tagNormalized instead of tag.
        // From here on, we follow (with some exceptions) the structure
        // of the parseTag() function in Richard Ishida's app-subtag code
        // (functions.js).
        if (tagNormalized.isEmpty()) {
            // Empty string is invalid.
            parsedLanguage.addError(ERROR_EMPTY_TAG);
            // Nothing more we can do.
            return parsedLanguage;
        }
        // Empty subtags
        if (tagNormalized.contains("--")) {
            parsedLanguage.addError(ERROR_EMPTY_SUBTAG);
        }
        // Internal spaces
        if (tagNormalized.contains(" ")) {
            parsedLanguage.addError(ERROR_INTERNAL_SPACE);
        }
        // Beginning or ending with a hyphen.
        if (tagNormalized.startsWith("-") || tagNormalized.endsWith("-")) {
            parsedLanguage.addError(ERROR_BEGIN_END_HYPHEN);
        }
        // Disallowed separators
        if (INVALID_CHARACTERS.matcher(tagNormalized).matches()) {
            parsedLanguage.addError(ERROR_INVALID_CHARACTER);
        }
        // Bail out if we got any of the above errors.
        if (!parsedLanguage.isValid()) {
            return parsedLanguage;
        }

        // Start doing lookups. Use entry to store the results of lookups.
        Entry entry;

        // Grandfathered.
        entry = grandfatheredsMap.get(tagNormalized);
        if (entry != null) {
            // We _require_ a preferred value, so look it up ourselves.
            String preferredValue = entry.getPreferredValue();
            if (preferredValue != null) {
                // Canonicalize to the preferred value!
                tagNormalized = preferredValue;
            } else {
                parsedLanguage.addError(ERROR_GRANDFATHERED_DEPRECATED);
                return parsedLanguage;
            }
        }

        // Redundant.
        entry = redundantsMap.get(tagNormalized);
        if (entry != null) {
            // Preferred value is optional, but handle it if there is one.
            String preferredValue = entry.getPreferredValue();
            if (preferredValue != null) {
                // Canonicalize to the preferred value!
                tagNormalized = preferredValue;
            }
            // No problem if there is no preferred value; just keep going.
        }

        // Extensions and private use
        tagNormalized = stripExtensionsPrivateUse(tagNormalized,
                parsedLanguage);

        if (tagNormalized.isEmpty()) {
            // Nothing left after removal of extensions and private use.
            // Although such a tag is well-formed, _we_ don't allow it.
            // The errors will have been added in stripExtensionsPrivateUse();
            return parsedLanguage;
        }

        // Split into subtags.
        // Need to wrap with an ArrayList, as Arrays.asList() returns
        // a List which doesn't support remove().
        ArrayList<String> subtags =
                new ArrayList<>(Arrays.asList(HYPHEN.split(tagNormalized)));
        String language = subtags.remove(0);
        Entry languageEntry = languagesMap.get(language);
        if (languageEntry == null) {
            // As noted in the method comment, private use language subtags
            // are not in the parsed registry, and will be marked as invalid
            // here. But we we do give a special error message in that case.
            if (language.charAt(0) == 'q'
                    && language.length() == PRIVATE_USE_LANGUAGE_SUBTAG_LENGTH
                    && language.charAt(1) < 'u') {
                parsedLanguage.addError(ERROR_PRIVATE_USE_LANGUAGE_UNSUPPORTED);
            } else {
                parsedLanguage.addError(ERROR_UNKNOWN_LANGUAGE_SUBTAG);
            }
        } else {
            // Is there a preferred value?
            String preferredValue = languageEntry.getPreferredValue();
            if (preferredValue != null) {
                // There is; we replace what we already have, with
                // the entry for the preferred value.
                languageEntry = languagesMap.get(preferredValue);
                // Update language with the canonical value, so we can use
                // it later to compare with an extlang prefix.
                language = languageEntry.getSubtag();
            }
            parsedLanguage.setLanguage(languageEntry);
            // But as per RFC 5646, page 57, we don't allow "mis".
            if (language.equals("mis")) {
                parsedLanguage.addError(ERROR_DISALLOWED_LANGUAGE_SUBTAG);
            }
        }

        // What follows comes in this sequence:
        // extlang, script, region, variants.
        // Here are some booleans, which we progressively set to false
        // as we work our way through. [Variants are always allowed,
        // as they come last. (We already removed extensions and private use
        // components.)]
        boolean extlangAllowed = true;
        boolean scriptAllowed = true;
        boolean regionAllowed = true;
        Set<String> variantsFound = new HashSet<>();
        Entry regionEntry;
        Entry variantEntry;
        for (String subtag : subtags) {
            if (subtag.length() > SUBTAG_MAX_LENGTH) {
                parsedLanguage.addError(ERROR_SUBTAG_TOO_LONG);
                continue;
            }
            // Length now must be 2-8. (Can't be 1, as that would
            // have been considered by stripExtensionsPrivateUse().
            switch (subtag.length()) {
            case 2:
                // It's a two-character region. Only variants may follow.
                extlangAllowed = false;
                scriptAllowed = false;
                if (!regionAllowed) {
                    parsedLanguage.addError(ERROR_REGION_NOT_ALLOWED);
                    continue;
                }
                regionAllowed = false;
                regionEntry = getPreferredEntry(subtag, regionsMap);
                if (regionEntry == null) {
                    parsedLanguage.addError(ERROR_UNKNOWN_REGION_SUBTAG);
                    continue;
                }
                parsedLanguage.setRegion(regionEntry);
                break;
            // CHECKSTYLE:OFF: MagicNumber
            case 3:
            // CHECKSTYLE:ON: MagicNumber
                // Either a three-digit region, or an extlang.
                if (REGION_DIGITS.matcher(subtag).matches()) {
                    // It's a three-digit region.
                    if (!regionAllowed) {
                        parsedLanguage.addError(ERROR_REGION_NOT_ALLOWED);
                        continue;
                    }
                    //Only variants may follow.
                    extlangAllowed = false;
                    scriptAllowed = false;
                    regionAllowed = false;
                    regionEntry = getPreferredEntry(subtag, regionsMap);
                    if (regionEntry == null) {
                        parsedLanguage.addError(ERROR_UNKNOWN_REGION_SUBTAG);
                        continue;
                    }
                    parsedLanguage.setRegion(regionEntry);
                } else {
                    // Try extlangs.
                    if (!extlangAllowed) {
                        parsedLanguage.addError(ERROR_EXTLANG_NOT_ALLOWED);
                        continue;
                    }
                    // No further extlang allowed.
                    extlangAllowed = false;
                    Entry extlangEntry = extlangsMap.get(subtag);
                    if (extlangEntry == null) {
                        parsedLanguage.addError(ERROR_UNKNOWN_EXTLANG_SUBTAG);
                        continue;
                    }
                    String prefix = extlangEntry.getPrefix().get(0);
                    if (prefix.equals(language)) {
                        languageEntry = languagesMap.get(
                                extlangEntry.getPreferredValue());
                        parsedLanguage.setLanguage(languageEntry);
                        language = languageEntry.getSubtag();
                    } else {
                        parsedLanguage.addError(ERROR_EXTLANG_PREFIX_DIFFERS);
                    }
                    // Store it, but we won't use it.
                    parsedLanguage.setExtlang(extlangEntry);
                }
                break;
            // CHECKSTYLE:OFF: MagicNumber
            case 4:
            // CHECKSTYLE:ON: MagicNumber
                // Either a four-character variant (beginning with a digit),
                // or a script.
                if (Character.isDigit(subtag.charAt(0))) {
                    // It's a four-character variant.
                    // Only more variants may follow.
                    extlangAllowed = false;
                    scriptAllowed = false;
                    regionAllowed = false;
                    if (variantsFound.contains(subtag)) {
                        parsedLanguage.addError(ERROR_DUPLICATE_VARIANT_SUBTAG);
                        continue;
                    }
                    variantsFound.add(subtag);
                    variantEntry = variantsMap.get(subtag);
                    if (variantEntry == null) {
                        parsedLanguage.addError(ERROR_UNKNOWN_VARIANT_SUBTAG);
                        continue;
                    }
                    parsedLanguage.addVariant(variantEntry);
                } else {
                    // A script.
                    if (!scriptAllowed) {
                        parsedLanguage.addError(ERROR_SCRIPT_NOT_ALLOWED);
                        continue;
                    }
                    // Only regions and variants may follow.
                    extlangAllowed = false;
                    scriptAllowed = false;
                    // Hmm, it seems that, at least for now, no script
                    // entry has a preferred value. Allow for one anyway.
                    Entry scriptEntry = getPreferredEntry(subtag, scriptsMap);
                    if (scriptEntry == null) {
                        parsedLanguage.addError(ERROR_UNKNOWN_SCRIPT_SUBTAG);
                        continue;
                    }
                    parsedLanguage.setScript(scriptEntry);
                }
                break;
            default:
                // From 5 to 8 characters: it must be a variant, and only
                // more variants may follow.
                extlangAllowed = false;
                scriptAllowed = false;
                regionAllowed = false;
                variantEntry = getPreferredEntry(subtag, variantsMap);
                if (variantEntry == null) {
                    parsedLanguage.addError(ERROR_UNKNOWN_VARIANT_SUBTAG);
                    continue;
                }
                if (variantsFound.contains(variantEntry.getSubtag())) {
                    parsedLanguage.addError(ERROR_DUPLICATE_VARIANT_SUBTAG);
                    continue;
                }
                variantsFound.add(variantEntry.getSubtag());
                parsedLanguage.addVariant(variantEntry);
                break;
            }
        }

        // Sigh, satisfying RFC 5646 section 4.1, point 6, on the
        // validity and ordering of variants, is hard work.
        // For now, we won't do that work.

        return parsedLanguage;
    }

    /** Pattern that matches the beginning of a private use subtag,
     * but not one that occurs at the beginning. */
    private static final Pattern PRIVATE_USE_MIDDLE =
            Pattern.compile("-x-");

    /** Pattern that matches the beginning of an extension subtag,
     * but not one that occurs at the beginning. */
    private static final Pattern EXTENSION_MIDDLE =
            Pattern.compile("-.-");

    /** Strip out any extensions and private use components of a tag.
     * @param tag The tag from which extensions and private use components
     *      are to be stripped. It should already have been trimmed,
     *      converted to lower case, and validated to ensure it only
     *      contains valid characters.
     * @param parsedLanguage The parsed representation of the tag. It
     *      is updated to contain any errors found, and any extensions
     *      and private use subtags found.
     * @return An updated tag, from which the extensions and private use
     *      components have been stripped.
     */
    private String stripExtensionsPrivateUse(final String tag,
            final ParsedLanguage parsedLanguage) {
        // This method is inspired by the "removeExtensions" function
        // of Richard Ishida's JavaScript code. Because we don't allow
        // some values for tag, we can do some simplification.
        // First simplification: forbid tags that begin with a private
        // use tag.
        if (tag.startsWith("x-")) {
            parsedLanguage.addError(ERROR_BEGINS_PRIVATE_USE);
            // Remove the entire lot!
            return "";
        }

        // String to keep hold of "what's left" after we remove the various
        // components. It is the value of this local variable that will
        // be returned, on success.
        String remainder = tag;
        // Index of the first match of one of the patterns we use here.
        int start;

        Matcher matcher = PRIVATE_USE_MIDDLE.matcher(tag);
        if (matcher.find()) {
            // Found a private use tag.
            boolean privateUseOK = true;
            start = matcher.start();
            // We say "start + 1", because the match begins with
            // "-x-", and we strip off the starting hyphen.
            String privateUse = tag.substring(start + 1);
            remainder = tag.substring(0, start);
            String[] privateUseComponents = privateUse.split("-");
            for (String component : privateUseComponents) {
                if (component.length() > SUBTAG_MAX_LENGTH) {
                    // At least one component is too long.
                    privateUseOK = false;
                    parsedLanguage.addError(ERROR_PRIVATE_USE_COMPONENT_LENGTH);
                    break;
                }
            }
            if (matcher.find()) {
                // Oops! More than one private use subtag.
                privateUseOK = false;
                parsedLanguage.addError(ERROR_MULTIPLE_PRIVATE_USE);
            }
            if (privateUseOK) {
                parsedLanguage.setPrivateUse(privateUse);
            }
        }

        // Now, extensions.
        // We do really "cheat" here in the same way that Richard Ishida's
        // code does, i.e., we don't use the official IANA list of
        // extensions, at
        // https://www.iana.org/assignments/
        //   language-tag-extensions-registry/language-tag-extensions-registry
        // but instead hard-code just "t" and "u".
        // First, make sure the remainder doesn't _begin_ with an extension.
        if (remainder.length() > 1 && remainder.charAt(1) == '-') {
            // Oops, nothing left! A tag may not begin with an extension.
            parsedLanguage.addError(ERROR_BEGINS_EXTENSION);
            // As with private use above, remove the entire lot.
            // (It would be tricky to keep going, because EXTENSION_MIDDLE
            // won't match this extension.)
            return "";
        }

        matcher = EXTENSION_MIDDLE.matcher(remainder);
        Set<Character> singletonsFound = new HashSet<>();
        if (matcher.find()) {
            // Found at least one extension.
            start = matcher.start();
            // We say "firstStart + 1", because the match begins with
            // "-.-", and we strip off the starting hyphen.
            String extensions = remainder.substring(start + 1);
            remainder = remainder.substring(0, start);
            // Shrink extensions until there's nothing left.
            while (extensions.length() > 0) {
                // Start matching aagain, with extensions. This makes
                // working with substring indexes more convenient.
                matcher = EXTENSION_MIDDLE.matcher(extensions);
                // Separate the first extension from any subsequent one(s).
                String extension;
                String extensionsRemainder;
                if (matcher.find()) {
                    // There's another extension. The extension we're looking
                    // at goes from start + 1 to the new start.
                    start = matcher.start();
                    extensionsRemainder = extensions.substring(start + 1);
                    extension = extensions.substring(0, start);
                } else {
                    extensionsRemainder = "";
                    extension = extensions;
                }
                // Check we haven't seen the singleton before.
                char singleton = extension.charAt(0);
                if (singletonsFound.contains(singleton)) {
                    parsedLanguage.addError(ERROR_DUPLICATE_SINGLETON);
                }
                singletonsFound.add(singleton);
                // But the singleton must be either "t" or "u".
                if (singleton != 't' && singleton != 'u') {
                    parsedLanguage.addError(ERROR_INVALID_SINGLETON);
                }
                // Now check that each component has up to eight charaters.
                boolean extensionOK = true;
                String[] extensionComponents = extension.split("-");
                for (String component : extensionComponents) {
                    if (component.length() > SUBTAG_MAX_LENGTH) {
                        // At least one component is too long.
                        extensionOK = false;
                        parsedLanguage.addError(
                                ERROR_EXTENSION_COMPONENT_LENGTH);
                        break;
                    }
                }
                if (extensionOK) {
                    parsedLanguage.addExtension(extension);
                }
                extensions = extensionsRemainder;
            }
        }
        return remainder;
    }

}
