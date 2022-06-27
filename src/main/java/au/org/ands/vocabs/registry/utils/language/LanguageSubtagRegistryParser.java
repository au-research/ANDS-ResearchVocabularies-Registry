/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.utils.language;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.converter.JSONSerialization;
import au.org.ands.vocabs.registry.utils.RegistryConfig;
import au.org.ands.vocabs.registry.utils.RegistryFileUtils;
import au.org.ands.vocabs.registry.utils.language.lsr.Entry;
import au.org.ands.vocabs.registry.utils.language.lsr.Lsr;

/** Parse the IANA language subtag registry and transform the contents
 * into resources in XML and JSON formats.
 * Notes:
 * <ul>
 * <li>Only run this once per application startup! It creates shared files
 *   which should not subsequently be overwritten during operation.</li>
 * <li>For now, we don't support "Private use" subtags.
 *   See various places in the code where this decision is implemented.</li>
 * <li>We treat "mi" as a special case, and make "Māori" the first
 *   description value for that subtag.</li>
 * </ul>
 */
public final class LanguageSubtagRegistryParser {

    /* This is a complete list of the keys that appear in the registry file:
       Added
       Comments
       Deprecated
       Description
       File-Date
       Macrolanguage
       Preferred-Value
       Prefix
       Scope
       Subtag
       Suppress-Script
       Tag
       Type
     */

    /* There are ranges in subtag values, e.g., "Subtag: qaa..qtz".
       They occur as languages, scripts, and regions.
       This class has code to expand those ranges.
       However, we note that all current ranges are "Private use" values.
       And we make the broader decision, not to support "Private use"
       values for now. */

    /** Logger for this class. */
    private Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** The language subtag registry. */
    private Lsr lsr;

    /** The list of languages in the registry. */
    private List<Entry> languages;

    /** The list of scripts in the registry. */
    private List<Entry> scripts;

    /** The list of regions in the registry. */
    private List<Entry> regions;

    /** The list of grandfathered tags in the registry. */
    private List<Entry> grandfathereds;

    /** The list of redundant tags in the registry. */
    private List<Entry> redundants;

    /** The list of variants in the registry. */
    private List<Entry> variants;

    /** The list of extlangs in the registry. */
    private List<Entry> extlangs;


    /** The map of languages in the registry. */
    private Map<String, Entry> languagesMap = new HashMap<>();

    /** The map of scripts in the registry. */
    private Map<String, Entry> scriptsMap = new HashMap<>();

    /** The map of regions in the registry. */
    private Map<String, Entry> regionsMap = new HashMap<>();

    /** The map of grandfathered tags in the registry. */
    private Map<String, Entry> grandfatheredsMap = new HashMap<>();

    /** The map of redundant tags in the registry. */
    private Map<String, Entry> redundantsMap = new HashMap<>();

    /** The map of variants in the registry. */
    private Map<String, Entry> variantsMap = new HashMap<>();

    /** The map of extlangs in the registry. */
    private Map<String, Entry> extlangsMap = new HashMap<>();

    /** Get the list of language subtags.
     * @return The list of language subtags.
     */
    List<Entry> getLanguages() {
        return languages;
    }

    /** Get the list of script subtags.
     * @return The list of script subtags.
     */
    List<Entry> getScripts() {
        return scripts;
    }

    /** Get the list of region subtags.
     * @return The list of region subtags.
     */
    List<Entry> getRegions() {
        return regions;
    }

    /** Get the list of grandfathered tags.
     * @return The list of grandfathered tags.
     */
    List<Entry> getGrandfathereds() {
        return grandfathereds;
    }

    /** Get the list of redundant tags.
     * @return The list of redundant tags.
     */
    List<Entry> getRedundants() {
        return redundants;
    }

    /** Get the list of variant subtags.
     * @return The list of variant subtags.
     */
    List<Entry> getVariants() {
        return variants;
    }

    /** Get the list of extlang subtags.
     * @return The list of extlang subtags.
     */
    List<Entry> getExtlangs() {
        return extlangs;
    }

    /** Get the map of language subtags.
     * @return The map of language subtags. The keys are lowercase.
     */
    Map<String, Entry> getLanguagesMap() {
        return languagesMap;
    }

    /** Get the map of script subtags.
     * @return The map of script subtags. The keys are lowercase.
     */
    Map<String, Entry> getScriptsMap() {
        return scriptsMap;
    }

    /** Get the map of region subtags.
     * @return The map of region subtags. The keys are lowercase.
     */
    Map<String, Entry> getRegionsMap() {
        return regionsMap;
    }

    /** Get the map of grandfathered tags.
     * @return The map of grandfathered tags. The keys are lowercase.
     */
    Map<String, Entry> getGrandfatheredsMap() {
        return grandfatheredsMap;
    }

    /** Get the map of redundant tags.
     * @return The map of redundant tags. The keys are lowercase.
     */
    Map<String, Entry> getRedundantsMap() {
        return redundantsMap;
    }

    /** Get the map of variant subtags.
     * @return The map of variant subtags. The keys are lowercase.
     */
    Map<String, Entry> getVariantsMap() {
        return variantsMap;
    }

    /** Get the map of extlang subtags.
     * @return The map of extlang subtags. The keys are lowercase.
     */
    Map<String, Entry> getExtlangsMap() {
        return extlangsMap;
    }

    /** The absolute path to the generated <code>lsr.xml</code> file. */
    private String lsrXmlFilename;

    /** The absolute path to the generated <code>lsr.xml.gz</code> file. */
    private String lsrXmlGzFilename;

    /** The absolute path to the generated <code>lsr.json</code> file. */
    private String lsrJsonFilename;

    /** The absolute path to the generated <code>lsr.json.gz</code> file. */
    private String lsrJsonGzFilename;

    /** Get the absolute path to the generated <code>lsr.xml</code> file.
     * @return The absolute path to the generated <code>lsr.xml</code> file.
     */
    String getLsrXmlFilename() {
        return lsrXmlFilename;
    }

    /** Get the absolute path to the generated <code>lsr.xml.gz</code> file.
     * @return The absolute path to the generated <code>lsr.xml.gz</code> file.
     */
    String getLsrXmlGzFilename() {
        return lsrXmlGzFilename;
    }

    /** Get the absolute path to the generated <code>lsr.json</code> file.
     * @return The absolute path to the generated <code>lsr.json</code> file.
     */
    String getLsrJsonFilename() {
        return lsrJsonFilename;
    }

    /** Get the absolute path to the generated <code>lsr.json.gz</code> file.
     * @return The absolute path to the generated <code>lsr.json.gz</code> file.
     */
    String getLsrJsonGzFilename() {
        return lsrJsonGzFilename;
    }

    /** Load the language subtag registry data and store the parsed results
     * in the {@code lsr} subdirectory of the top-level Registry
     * output directory. This is a convenience method for use only
     * in testing; see {@link LanguageSubtagRegistry#LanguageSubtagRegistry()}
     * for the caller used in production.
     * @param args Command-line arguments. The only parameter is
     *   the filename of the language subtag registry data to be loaded.
     * @throws JAXBException If there is an error marshalling the registry
     *      into XML.
     * @throws IOException If there is an error creating one of the
     *      output files.
     */
    public static void main(final String[] args)
            throws IOException, JAXBException {
        String filename = args[0];

        Path lsrDirectory = Paths.get(RegistryConfig.ROOT_FILES_PATH).
                resolve("lsr");
        RegistryFileUtils.requireDirectory(lsrDirectory.toString());
        new LanguageSubtagRegistryParser(filename, lsrDirectory);
    }

    /** Load the language subtag registry data from a file, and store
     * the parsed results in the file system.
     * @param filename The filename of the language subtag registry data
     *   to be loaded.
     * @param lsrDirectory The name of the directory into which the
     *   parsed results are to be cached.
     * @throws JAXBException If there is an error marshalling the registry
     *      into XML.
     * @throws IOException If there is an error creating one of the
     *      output files.
     */
    LanguageSubtagRegistryParser(final String filename,
            final Path lsrDirectory) throws IOException, JAXBException {
        logger.info("lsrDirectory: " + lsrDirectory);
        lsr = new Lsr();
        lsr.setLanguages(new Lsr.Languages());
        languages = lsr.getLanguages().getEntry();

        lsr.setScripts(new Lsr.Scripts());
        scripts = lsr.getScripts().getEntry();

        lsr.setRegions(new Lsr.Regions());
        regions = lsr.getRegions().getEntry();

        lsr.setGrandfathereds(new Lsr.Grandfathereds());
        grandfathereds = lsr.getGrandfathereds().getEntry();

        lsr.setRedundants(new Lsr.Redundants());
        redundants = lsr.getRedundants().getEntry();

        lsr.setVariants(new Lsr.Variants());
        variants = lsr.getVariants().getEntry();

        lsr.setExtlangs(new Lsr.Extlangs());
        extlangs = lsr.getExtlangs().getEntry();

        parseFile(filename);

        logger.info("languages: " + languages.size());
        logger.info("scripts: " + scripts.size());
        logger.info("regions: " + regions.size());
        logger.info("grandfathereds: " + grandfathereds.size());
        logger.info("redundants: " + redundants.size());
        logger.info("variants: " + variants.size());
        logger.info("extlangs: " + extlangs.size());

        writeLsr(lsrDirectory.toAbsolutePath());
    }

    /** Open the registry file and extract its contents.
     * @param filename The filename of the file to be parsed.
     */
    public void parseFile(final String filename) {
        File file = new File(filename);
        Scanner scanner;
        try {
            scanner = new Scanner(file, StandardCharsets.UTF_8.name());
            scanner.useDelimiter("%%");
            while (scanner.hasNext()) {
                parseEntry(scanner.next());
            }
        } catch (FileNotFoundException e) {
            logger.error("File not found", e);
        }
    }

    /** Pattern that matches a newline character. */
    private static final Pattern NEWLINE_PATTERN = Pattern.compile("\n");

    /** Pattern that matches a colon. */
    private static final Pattern COLON_PATTERN = Pattern.compile(":");

    /** Pattern that matches two periods, as used in a Subtag value
     * that is a range. */
    private static final Pattern RANGE_PATTERN = Pattern.compile("\\.\\.");

    /** Parse one entry of the registry.
     * @param entryText The text of the entry to be parsed.
     */
    @SuppressWarnings("checkstyle:MethodLength")
    private void parseEntry(final String entryText) {
        String[] subtagLines = NEWLINE_PATTERN.split(entryText);

        Entry entry = new Entry();
        String property = null;
        String propertyValue = null;

        // Much easier to handle the list if we fix up the continuation
        // lines now, rather than on-the-fly.
        ArrayList<String> subtagLinesList = new ArrayList<>();
        String lastLine = "";
        for (String line: subtagLines) {
            if (line.startsWith("  ")) {
                lastLine = lastLine + " " + line.trim();
                subtagLinesList.set(subtagLinesList.size() - 1, lastLine);
            } else {
                subtagLinesList.add(line);
                lastLine = line;
            }
        }

        // We don't store "Type" values, but we do need them later on.
        String type = null;
        // Convenience copy of the "Subtag" value, as we may need it
        // for processing ranges.
        String subtag = null;

        for (String line: subtagLinesList) {
            if (line.contains(":")) {
                String[] propertyAndValue = COLON_PATTERN.split(line, 2);
                property = propertyAndValue[0];
                propertyValue = propertyAndValue[1].trim();
                switch (property) {
                case "Added":
                    // We don't store "Added" values.
                    // entry.setAdded(propertyValue);
                    break;
                case "Comments":
                    entry.getComments().add(propertyValue);
                    break;
                case "Deprecated":
                    entry.setDeprecated(propertyValue);
                    break;
                case "Description":
                    // For now, we don't support "Private use" subtags.
                    if (propertyValue.equals("Private use")) {
                        return;
                    }
                    entry.getDescription().add(propertyValue);
                    break;
                case "File-Date":
                    lsr.setFileDate(propertyValue);
                    // We're done for this method. Don't fall through
                    // to the following switch, where we would get
                    // an error due to the missing "Type".
                    return;
                case "Macrolanguage":
                    entry.setMacrolanguage(propertyValue);
                    break;
                case "Preferred-Value":
                    entry.setPreferredValue(propertyValue);
                    break;
                case "Prefix":
                    entry.getPrefix().add(propertyValue);
                    break;
                case "Scope":
                    entry.setScope(propertyValue);
                    break;
                case "Subtag":
                    entry.setSubtag(propertyValue);
                    // Special case for "mi". We assume the lines
                    // of the registry are in the sequence
                    // Type, Subtag, Description, i.e., at this point,
                    // we have the Type, and we don't yet have
                    // any Descriptions.
                    if (type.equals("language") && propertyValue.equals("mi")) {
                        entry.getDescription().add("Māori");
                    }
                    break;
                case "Suppress-Script":
                    entry.setSuppressScript(propertyValue);
                    break;
                case "Tag":
                    entry.setTag(propertyValue);
                    break;
                case "Type":
                    // We don't store "Type" values.
                    // entry.setType(propertyValue);
                    type = propertyValue;
                    break;
                default:
                    logger.error("Unknown property: " + property);
                    break;
                }
            }
        }
        if (type != null) {
            String[] range;
            switch (type) {
            case "language":
                // Handle ranges
                subtag = entry.getSubtag();
                range = RANGE_PATTERN.split(subtag);
                if (range.length == 1) {
                    // Not a range.
                    languages.add(entry);
                    languagesMap.put(subtag.toLowerCase(Locale.ROOT), entry);
                } /* else {
                    // It's a range.
                    // Decision for now: we ignore all ranges.
                    // We note that all existing ranges are "Private use".
                } */
                break;
            case "script":
                // Handle ranges
                subtag = entry.getSubtag();
                range = RANGE_PATTERN.split(subtag);
                if (range.length == 1) {
                    // Not a range.
                    scripts.add(entry);
                    scriptsMap.put(subtag.toLowerCase(Locale.ROOT), entry);
                } /* else {
                    // It's a range.
                    // Decision for now: we ignore all ranges.
                    // We note that all existing ranges are "Private use".
                } */
                break;
            case "region":
                // Handle ranges
                subtag = entry.getSubtag();
                range = RANGE_PATTERN.split(subtag);
                if (range.length == 1) {
                    // Not a range.
                    regions.add(entry);
                    regionsMap.put(subtag.toLowerCase(Locale.ROOT), entry);
                } /* else {
                    // It's a range.
                    // Decision for now: we ignore all ranges.
                    // We note that all existing ranges are "Private use".
                } */
                break;
            case "grandfathered":
                grandfathereds.add(entry);
                // grandfathered entries have a "Tag" instead of a "Subtag".
                grandfatheredsMap.put(
                        entry.getTag().toLowerCase(Locale.ROOT), entry);
                break;
            case "redundant":
                redundants.add(entry);
                // redundant entries have a "Tag" instead of a "Subtag".
                redundantsMap.put(entry.getTag().toLowerCase(Locale.ROOT),
                        entry);
                break;
            case "variant":
                variants.add(entry);
                variantsMap.put(entry.getSubtag().toLowerCase(Locale.ROOT),
                        entry);
                break;
            case "extlang":
                extlangs.add(entry);
                extlangsMap.put(entry.getSubtag().toLowerCase(Locale.ROOT),
                        entry);
                break;
            default:
                logger.error("Unknown type: " + type);
            }
        } else {
            logger.error("No type specified!");
        }
    }

    /** The number of letters in the Latin alphabet. Used by
     * the {@link LanguageSubtagRegistryParser#stringRange(String, String)}
     * method as a base for the values of each character position.
     */
    private static final int LETTERS_IN_THE_ALPHABET = 26;

    /** Generate a range of strings between a start and end value.
     * For example, if start="Qaaa" and end="Qabx", the result is
     * the list ["Qaaa", "Qaab", ..., "Qaaz", "Qaba", ..., "Qabx"].
     * We require that start and end have the same length, and that
     * start is lexicographically less than or equal to end.
     * We require that corresponding characters in start and end be
     * of the same case. Therefore, it is not allowed to have
     * start="Qaaa" but end="qabx", since the first characters of
     * start and end have different case.
     * @param start The start string.
     * @param end The end string.
     * @return An ArrayList of the range of strings.
     */
    // Remove the following annotation, if/when we decide to support
    // ranges.
    @SuppressWarnings("unused")
    private ArrayList<String> stringRange(final String start,
            final String end) {
        // The result.
        ArrayList<String> range = new ArrayList<>();

        int subtagLength = start.length();
        boolean[] isLower = new boolean[subtagLength];
        int startBase26 = 0;
        int endBase26 = 0;
        for (int i = 0; i < start.length(); i++) {
            isLower[i] = Character.isLowerCase(start.charAt(i));
            startBase26 = startBase26 * LETTERS_IN_THE_ALPHABET
                    + Character.toLowerCase(start.charAt(i)) - 'a';
            endBase26 = endBase26 * LETTERS_IN_THE_ALPHABET
                    + Character.toLowerCase(end.charAt(i)) - 'a';
        }

        int[] indexes = new int[subtagLength];
        for (int i = startBase26; i <= endBase26; i++) {
            int position = subtagLength - 1;
            int j = i;
            while (j > 0) {
                indexes[position] = j % LETTERS_IN_THE_ALPHABET;
                j = j / LETTERS_IN_THE_ALPHABET;
                position--;
            }
            StringBuilder sb = new StringBuilder(subtagLength);
            for (int k = 0; k < subtagLength; k++) {
                if (isLower[k]) {
                    sb.append((char) (indexes[k] + 'a'));
                } else {
                    sb.append((char) (indexes[k] + 'A'));
                }
            }
            range.add(sb.toString());
        }

        return range;
    }

    /** Write out the registry in the various supported formats and encodings.
     * @param lsrDirectory The name of the directory into which the
     *   parsed results are to be cached.
     * @throws JAXBException If there is an error marshalling the registry
     *      into XML.
     * @throws IOException If there is an error creating one of the
     *      output files.
     */
    private void writeLsr(final Path lsrDirectory)
            throws IOException, JAXBException {
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(Lsr.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            // Generate DOCTYPE, if needed.
//            jaxbMarshaller.setProperty("com.sun.xml.bind.xmlHeaders",
//                    " <!DOCTYPE lsr>\n");
            // Make it pretty, so we can use newlines in our test data.
//          jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            // First, uncompressed XML.
            lsrXmlFilename = lsrDirectory.resolve("lsr.xml").toString();
            OutputStream writer;
            try {
                writer = new FileOutputStream(lsrXmlFilename);
                jaxbMarshaller.marshal(lsr, writer);
                writer.close();
            } catch (FileNotFoundException e) {
                logger.error("Can't create XML output file for LSR.", e);
                throw e;
            } catch (IOException e) {
                logger.error("Error closing XML output file for LSR.", e);
                throw e;
            }

            // Second, gzipped XML.
            lsrXmlGzFilename = lsrDirectory.resolve("lsr.xml.gz").toString();
            try {
                writer = new GZIPOutputStream(new FileOutputStream(
                        lsrXmlGzFilename));
                jaxbMarshaller.marshal(lsr, writer);
                writer.close();
            } catch (FileNotFoundException e) {
                logger.error("Can't create XML output file for LSR.", e);
                throw e;
            } catch (IOException e) {
                logger.error("Can't gzip XML output file for LSR.", e);
                throw e;
            }
        } catch (JAXBException e) {
            logger.error("Got a JAXBException", e);
            throw e;
        }

        // JSON.
        byte[] jsonBytes = JSONSerialization.serializeObjectAsJsonString(lsr).
                getBytes(StandardCharsets.UTF_8);

        // Third, uncompressed JSON.
        lsrJsonFilename = lsrDirectory.resolve("lsr.json").toString();
        OutputStream writer;
        try {
            writer = new FileOutputStream(lsrJsonFilename);
            writer.write(jsonBytes);
            writer.close();
        } catch (FileNotFoundException e) {
            logger.error("Can't create JSON output file for LSR.", e);
            throw e;
        } catch (IOException e) {
            logger.error("Can't write JSON output file for LSR.", e);
            throw e;
        }

        // Fourth, gzipped JSON.
        lsrJsonGzFilename = lsrDirectory.resolve("lsr.json.gz").toString();
        try {
            writer = new GZIPOutputStream(new FileOutputStream(
                    lsrJsonGzFilename));
            writer.write(jsonBytes);
            writer.close();
        } catch (FileNotFoundException e) {
            logger.error("Can't create JSON output file for LSR.", e);
            throw e;
        } catch (IOException e) {
            logger.error("Can't gzip JSON output file for LSR.", e);
            throw e;
        }
    }

}
