/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.validation;

import java.util.HashSet;

/** Determine the validity of languages specified in vocabulary metadata.
 */
public final class Languages {

    /** Private constructor for a utility class. */
    private Languages() {
    }

    /** Set of valid ISO 639-1 language subtags as specified in
     * <a
     *  href="http://www.iana.org/assignments/language-subtag-registry/language-subtag-registry">http://www.iana.org/assignments/language-subtag-registry/language-subtag-registry</a>,
     * of 2017-03-16. */
    private static final HashSet<String> VALID_TWO_CHARACTER_SUBTAGS =
            new HashSet<>();

    static {
        VALID_TWO_CHARACTER_SUBTAGS.add("aa");
        VALID_TWO_CHARACTER_SUBTAGS.add("ab");
        VALID_TWO_CHARACTER_SUBTAGS.add("ae");
        VALID_TWO_CHARACTER_SUBTAGS.add("af");
        VALID_TWO_CHARACTER_SUBTAGS.add("ak");
        VALID_TWO_CHARACTER_SUBTAGS.add("am");
        VALID_TWO_CHARACTER_SUBTAGS.add("an");
        VALID_TWO_CHARACTER_SUBTAGS.add("ar");
        VALID_TWO_CHARACTER_SUBTAGS.add("as");
        VALID_TWO_CHARACTER_SUBTAGS.add("av");
        VALID_TWO_CHARACTER_SUBTAGS.add("ay");
        VALID_TWO_CHARACTER_SUBTAGS.add("az");
        VALID_TWO_CHARACTER_SUBTAGS.add("ba");
        VALID_TWO_CHARACTER_SUBTAGS.add("be");
        VALID_TWO_CHARACTER_SUBTAGS.add("bg");
        VALID_TWO_CHARACTER_SUBTAGS.add("bh");
        VALID_TWO_CHARACTER_SUBTAGS.add("bi");
        VALID_TWO_CHARACTER_SUBTAGS.add("bm");
        VALID_TWO_CHARACTER_SUBTAGS.add("bn");
        VALID_TWO_CHARACTER_SUBTAGS.add("bo");
        VALID_TWO_CHARACTER_SUBTAGS.add("br");
        VALID_TWO_CHARACTER_SUBTAGS.add("bs");
        VALID_TWO_CHARACTER_SUBTAGS.add("ca");
        VALID_TWO_CHARACTER_SUBTAGS.add("ce");
        VALID_TWO_CHARACTER_SUBTAGS.add("ch");
        VALID_TWO_CHARACTER_SUBTAGS.add("co");
        VALID_TWO_CHARACTER_SUBTAGS.add("cr");
        VALID_TWO_CHARACTER_SUBTAGS.add("cs");
        VALID_TWO_CHARACTER_SUBTAGS.add("cu");
        VALID_TWO_CHARACTER_SUBTAGS.add("cv");
        VALID_TWO_CHARACTER_SUBTAGS.add("cy");
        VALID_TWO_CHARACTER_SUBTAGS.add("da");
        VALID_TWO_CHARACTER_SUBTAGS.add("de");
        VALID_TWO_CHARACTER_SUBTAGS.add("dv");
        VALID_TWO_CHARACTER_SUBTAGS.add("dz");
        VALID_TWO_CHARACTER_SUBTAGS.add("ee");
        VALID_TWO_CHARACTER_SUBTAGS.add("el");
        VALID_TWO_CHARACTER_SUBTAGS.add("en");
        VALID_TWO_CHARACTER_SUBTAGS.add("eo");
        VALID_TWO_CHARACTER_SUBTAGS.add("es");
        VALID_TWO_CHARACTER_SUBTAGS.add("et");
        VALID_TWO_CHARACTER_SUBTAGS.add("eu");
        VALID_TWO_CHARACTER_SUBTAGS.add("fa");
        VALID_TWO_CHARACTER_SUBTAGS.add("ff");
        VALID_TWO_CHARACTER_SUBTAGS.add("fi");
        VALID_TWO_CHARACTER_SUBTAGS.add("fj");
        VALID_TWO_CHARACTER_SUBTAGS.add("fo");
        VALID_TWO_CHARACTER_SUBTAGS.add("fr");
        VALID_TWO_CHARACTER_SUBTAGS.add("fy");
        VALID_TWO_CHARACTER_SUBTAGS.add("ga");
        VALID_TWO_CHARACTER_SUBTAGS.add("gd");
        VALID_TWO_CHARACTER_SUBTAGS.add("gl");
        VALID_TWO_CHARACTER_SUBTAGS.add("gn");
        VALID_TWO_CHARACTER_SUBTAGS.add("gu");
        VALID_TWO_CHARACTER_SUBTAGS.add("gv");
        VALID_TWO_CHARACTER_SUBTAGS.add("ha");
        VALID_TWO_CHARACTER_SUBTAGS.add("he");
        VALID_TWO_CHARACTER_SUBTAGS.add("hi");
        VALID_TWO_CHARACTER_SUBTAGS.add("ho");
        VALID_TWO_CHARACTER_SUBTAGS.add("hr");
        VALID_TWO_CHARACTER_SUBTAGS.add("ht");
        VALID_TWO_CHARACTER_SUBTAGS.add("hu");
        VALID_TWO_CHARACTER_SUBTAGS.add("hy");
        VALID_TWO_CHARACTER_SUBTAGS.add("hz");
        VALID_TWO_CHARACTER_SUBTAGS.add("ia");
        VALID_TWO_CHARACTER_SUBTAGS.add("id");
        VALID_TWO_CHARACTER_SUBTAGS.add("ie");
        VALID_TWO_CHARACTER_SUBTAGS.add("ig");
        VALID_TWO_CHARACTER_SUBTAGS.add("ii");
        VALID_TWO_CHARACTER_SUBTAGS.add("ik");
        VALID_TWO_CHARACTER_SUBTAGS.add("in");
        VALID_TWO_CHARACTER_SUBTAGS.add("io");
        VALID_TWO_CHARACTER_SUBTAGS.add("is");
        VALID_TWO_CHARACTER_SUBTAGS.add("it");
        VALID_TWO_CHARACTER_SUBTAGS.add("iu");
        VALID_TWO_CHARACTER_SUBTAGS.add("iw");
        VALID_TWO_CHARACTER_SUBTAGS.add("ja");
        VALID_TWO_CHARACTER_SUBTAGS.add("ji");
        VALID_TWO_CHARACTER_SUBTAGS.add("jv");
        VALID_TWO_CHARACTER_SUBTAGS.add("jw");
        VALID_TWO_CHARACTER_SUBTAGS.add("ka");
        VALID_TWO_CHARACTER_SUBTAGS.add("kg");
        VALID_TWO_CHARACTER_SUBTAGS.add("ki");
        VALID_TWO_CHARACTER_SUBTAGS.add("kj");
        VALID_TWO_CHARACTER_SUBTAGS.add("kk");
        VALID_TWO_CHARACTER_SUBTAGS.add("kl");
        VALID_TWO_CHARACTER_SUBTAGS.add("km");
        VALID_TWO_CHARACTER_SUBTAGS.add("kn");
        VALID_TWO_CHARACTER_SUBTAGS.add("ko");
        VALID_TWO_CHARACTER_SUBTAGS.add("kr");
        VALID_TWO_CHARACTER_SUBTAGS.add("ks");
        VALID_TWO_CHARACTER_SUBTAGS.add("ku");
        VALID_TWO_CHARACTER_SUBTAGS.add("kv");
        VALID_TWO_CHARACTER_SUBTAGS.add("kw");
        VALID_TWO_CHARACTER_SUBTAGS.add("ky");
        VALID_TWO_CHARACTER_SUBTAGS.add("la");
        VALID_TWO_CHARACTER_SUBTAGS.add("lb");
        VALID_TWO_CHARACTER_SUBTAGS.add("lg");
        VALID_TWO_CHARACTER_SUBTAGS.add("li");
        VALID_TWO_CHARACTER_SUBTAGS.add("ln");
        VALID_TWO_CHARACTER_SUBTAGS.add("lo");
        VALID_TWO_CHARACTER_SUBTAGS.add("lt");
        VALID_TWO_CHARACTER_SUBTAGS.add("lu");
        VALID_TWO_CHARACTER_SUBTAGS.add("lv");
        VALID_TWO_CHARACTER_SUBTAGS.add("mg");
        VALID_TWO_CHARACTER_SUBTAGS.add("mh");
        VALID_TWO_CHARACTER_SUBTAGS.add("mi");
        VALID_TWO_CHARACTER_SUBTAGS.add("mk");
        VALID_TWO_CHARACTER_SUBTAGS.add("ml");
        VALID_TWO_CHARACTER_SUBTAGS.add("mn");
        VALID_TWO_CHARACTER_SUBTAGS.add("mo");
        VALID_TWO_CHARACTER_SUBTAGS.add("mr");
        VALID_TWO_CHARACTER_SUBTAGS.add("ms");
        VALID_TWO_CHARACTER_SUBTAGS.add("mt");
        VALID_TWO_CHARACTER_SUBTAGS.add("my");
        VALID_TWO_CHARACTER_SUBTAGS.add("na");
        VALID_TWO_CHARACTER_SUBTAGS.add("nb");
        VALID_TWO_CHARACTER_SUBTAGS.add("nd");
        VALID_TWO_CHARACTER_SUBTAGS.add("ne");
        VALID_TWO_CHARACTER_SUBTAGS.add("ng");
        VALID_TWO_CHARACTER_SUBTAGS.add("nl");
        VALID_TWO_CHARACTER_SUBTAGS.add("nn");
        VALID_TWO_CHARACTER_SUBTAGS.add("no");
        VALID_TWO_CHARACTER_SUBTAGS.add("nr");
        VALID_TWO_CHARACTER_SUBTAGS.add("nv");
        VALID_TWO_CHARACTER_SUBTAGS.add("ny");
        VALID_TWO_CHARACTER_SUBTAGS.add("oc");
        VALID_TWO_CHARACTER_SUBTAGS.add("oj");
        VALID_TWO_CHARACTER_SUBTAGS.add("om");
        VALID_TWO_CHARACTER_SUBTAGS.add("or");
        VALID_TWO_CHARACTER_SUBTAGS.add("os");
        VALID_TWO_CHARACTER_SUBTAGS.add("pa");
        VALID_TWO_CHARACTER_SUBTAGS.add("pi");
        VALID_TWO_CHARACTER_SUBTAGS.add("pl");
        VALID_TWO_CHARACTER_SUBTAGS.add("ps");
        VALID_TWO_CHARACTER_SUBTAGS.add("pt");
        VALID_TWO_CHARACTER_SUBTAGS.add("qu");
        VALID_TWO_CHARACTER_SUBTAGS.add("rm");
        VALID_TWO_CHARACTER_SUBTAGS.add("rn");
        VALID_TWO_CHARACTER_SUBTAGS.add("ro");
        VALID_TWO_CHARACTER_SUBTAGS.add("ru");
        VALID_TWO_CHARACTER_SUBTAGS.add("rw");
        VALID_TWO_CHARACTER_SUBTAGS.add("sa");
        VALID_TWO_CHARACTER_SUBTAGS.add("sc");
        VALID_TWO_CHARACTER_SUBTAGS.add("sd");
        VALID_TWO_CHARACTER_SUBTAGS.add("se");
        VALID_TWO_CHARACTER_SUBTAGS.add("sg");
        VALID_TWO_CHARACTER_SUBTAGS.add("sh");
        VALID_TWO_CHARACTER_SUBTAGS.add("si");
        VALID_TWO_CHARACTER_SUBTAGS.add("sk");
        VALID_TWO_CHARACTER_SUBTAGS.add("sl");
        VALID_TWO_CHARACTER_SUBTAGS.add("sm");
        VALID_TWO_CHARACTER_SUBTAGS.add("sn");
        VALID_TWO_CHARACTER_SUBTAGS.add("so");
        VALID_TWO_CHARACTER_SUBTAGS.add("sq");
        VALID_TWO_CHARACTER_SUBTAGS.add("sr");
        VALID_TWO_CHARACTER_SUBTAGS.add("ss");
        VALID_TWO_CHARACTER_SUBTAGS.add("st");
        VALID_TWO_CHARACTER_SUBTAGS.add("su");
        VALID_TWO_CHARACTER_SUBTAGS.add("sv");
        VALID_TWO_CHARACTER_SUBTAGS.add("sw");
        VALID_TWO_CHARACTER_SUBTAGS.add("ta");
        VALID_TWO_CHARACTER_SUBTAGS.add("te");
        VALID_TWO_CHARACTER_SUBTAGS.add("tg");
        VALID_TWO_CHARACTER_SUBTAGS.add("th");
        VALID_TWO_CHARACTER_SUBTAGS.add("ti");
        VALID_TWO_CHARACTER_SUBTAGS.add("tk");
        VALID_TWO_CHARACTER_SUBTAGS.add("tl");
        VALID_TWO_CHARACTER_SUBTAGS.add("tn");
        VALID_TWO_CHARACTER_SUBTAGS.add("to");
        VALID_TWO_CHARACTER_SUBTAGS.add("tr");
        VALID_TWO_CHARACTER_SUBTAGS.add("ts");
        VALID_TWO_CHARACTER_SUBTAGS.add("tt");
        VALID_TWO_CHARACTER_SUBTAGS.add("tw");
        VALID_TWO_CHARACTER_SUBTAGS.add("ty");
        VALID_TWO_CHARACTER_SUBTAGS.add("ug");
        VALID_TWO_CHARACTER_SUBTAGS.add("uk");
        VALID_TWO_CHARACTER_SUBTAGS.add("ur");
        VALID_TWO_CHARACTER_SUBTAGS.add("uz");
        VALID_TWO_CHARACTER_SUBTAGS.add("ve");
        VALID_TWO_CHARACTER_SUBTAGS.add("vi");
        VALID_TWO_CHARACTER_SUBTAGS.add("vo");
        VALID_TWO_CHARACTER_SUBTAGS.add("wa");
        VALID_TWO_CHARACTER_SUBTAGS.add("wo");
        VALID_TWO_CHARACTER_SUBTAGS.add("xh");
        VALID_TWO_CHARACTER_SUBTAGS.add("yi");
        VALID_TWO_CHARACTER_SUBTAGS.add("yo");
        VALID_TWO_CHARACTER_SUBTAGS.add("za");
        VALID_TWO_CHARACTER_SUBTAGS.add("zh");
        VALID_TWO_CHARACTER_SUBTAGS.add("zu");
    }

    /** Decide if a String is a valid language tag.
     * @param testString The String to be validated.
     * @return true, if testString represents a valid language tag.
     */
    public static boolean isValidLanguage(final String testString) {
        return VALID_TWO_CHARACTER_SUBTAGS.contains(testString);
    }

    /** Decide if a String is a valid language tag, that is not
     * the same as the primary language of a vocabulary.
     * @param primaryLanguage The primary language of the vocabulary.
     *      It may be null.
     * @param testString The String to be validated. It must not be null.
     * @return true, if testString represents a valid language tag.
     */
    public static boolean isValidLanguageNotPrimary(
            final String primaryLanguage,
            final String testString) {
        return VALID_TWO_CHARACTER_SUBTAGS.contains(testString)
                && !testString.equals(primaryLanguage);
    }


}
