/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.context;

import java.time.Clock;
import java.time.LocalDateTime;

import javax.persistence.Query;
import javax.persistence.TypedQuery;

/** Support for temporal data.
 */
public final class TemporalUtils {

    /** Private constructor for a utility class. */
    private TemporalUtils() {
    }

    /** The name of the date/time parameter to use in queries. */
    private static final String DATETIME_PARAMETER = "datetime";

    /** Entity name E1, for use in JPQL queries. */
    public static final String E1 = "e1";

    /** Entity name E2, for use in JPQL queries. */
    public static final String E2 = "e2";

    /** Entity name E3, for use in JPQL queries. */
    public static final String E3 = "e3";

    /** The name of a JPQL parameter to use for the constant
     * {@link TemporalConstants#CURRENTLY_VALID_END_DATE}. */
    private static final String CURRENTLY_VALID_END_DATE =
            "currently_valid_end_date";

    /** The name of a JPQL parameter to use for the constant
     * {@link TemporalConstants#DRAFT_START_DATE}. */
    private static final String DRAFT_START_DATE =
            "draft_start_date";

    /** Start date field name. Must match the property name in the entity
     * class. */
    public static final String START_DATE = "startDate";

    /** End date field name. Must match the property name in the entity
     * class. */
    public static final String END_DATE = "endDate";

    /** Clause for JPQL queries to select only currently-valid rows. */
    public static final String TEMPORAL_QUERY_VALID_CLAUSE =
            " " + END_DATE + " = :" + CURRENTLY_VALID_END_DATE;

    /** Clause for JPQL queries to select only currently-valid rows
     * of entity e1. */
    public static final String TEMPORAL_QUERY_VALID_CLAUSE_E1 =
            " " + E1 + "." + END_DATE + " = :" + CURRENTLY_VALID_END_DATE;

    /** Clause for JPQL queries to select only currently-valid rows
     * of entity e2. */
    public static final String TEMPORAL_QUERY_VALID_CLAUSE_E2 =
            " " + E2 + "." + END_DATE + " = :" + CURRENTLY_VALID_END_DATE;

    /** Suffix for JPQL queries to select only currently-valid rows.
     * This version of the suffix is for queries that already
     * have a WHERE clause. */
    public static final String AND_TEMPORAL_QUERY_VALID_SUFFIX =
            " AND " + TEMPORAL_QUERY_VALID_CLAUSE;

    /** Suffix for JPQL queries to select only currently-valid rows
     * of entity e1.
     * This version of the suffix is for queries that already
     * have a WHERE clause. */
    public static final String AND_TEMPORAL_QUERY_VALID_SUFFIX_E1 =
            " AND " + TEMPORAL_QUERY_VALID_CLAUSE_E1;

    /** Suffix for JPQL queries to select only currently-valid rows
     * of entity e2.
     * This version of the suffix is for queries that already
     * have a WHERE clause. */
    public static final String AND_TEMPORAL_QUERY_VALID_SUFFIX_E2 =
            " AND " + TEMPORAL_QUERY_VALID_CLAUSE_E2;

    // Original version follows. Less efficient than the previous.
//    public static final String AND_TEMPORAL_QUERY_VALID_SUFFIX =
//            " AND start_date <= :" + DATETIME_PARAMETER
//            + " AND :" + DATETIME_PARAMETER + " < end_date";

    /** Suffix for JPQL queries to select only currently-valid rows.
     * This version of the suffix is for queries that do not already
     * have a WHERE clause. */
    public static final String WHERE_TEMPORAL_QUERY_VALID_SUFFIX =
            " WHERE " + TEMPORAL_QUERY_VALID_CLAUSE;
    // Original version follows. Less efficient than the previous.
//    public static final String WHERE_TEMPORAL_QUERY_VALID_SUFFIX =
//            " WHERE start_date <= :" + DATETIME_PARAMETER
//            + " AND :" + DATETIME_PARAMETER + " < end_date";

    /** Clause for JPQL queries to select only draft rows. */
    public static final String TEMPORAL_QUERY_ALL_DRAFT_CLAUSE =
            " " + START_DATE + " > :" + CURRENTLY_VALID_END_DATE;

    /** Suffix for JPQL queries to select only draft rows.
     * This version of the suffix is for queries that already
     * have a WHERE clause. */
    public static final String AND_TEMPORAL_QUERY_ALL_DRAFT_SUFFIX =
            " AND " + TEMPORAL_QUERY_ALL_DRAFT_CLAUSE;

    /** Suffix for JPQL queries to select only draft rows.
     * This version of the suffix is for queries that do not already
     * have a WHERE clause. */
    public static final String WHERE_TEMPORAL_QUERY_ALL_DRAFT_SUFFIX =
            " WHERE " + TEMPORAL_QUERY_ALL_DRAFT_CLAUSE;

    /** Suffix template for JPQL queries to select only currently-valid rows
     * of entity {@link #E1}. */
    public static final String
        TEMPORAL_QUERY_TEMPLATE_VALID_SUFFIX_E1 =
            " AND " + E1 + "." + END_DATE
            + " = :" + CURRENTLY_VALID_END_DATE;
            // This is what we would _like_ to do, but it doesn't currently
            // work with Hibernate. Hibernate serializes the Timestamp
            // literal using TimestampType.objectToSQLString(), which uses
            // Timestamp.toString(). It should be like the code in
            // LocalDateTimeType.objectToSQLString(), which generates
            // a JDBC escape "{ts '9999-12-01 00:00:00.0'}".
            /*
            " AND " + E1 + "." + END_DATE
            + " = au.org.ands.vocabs.registry.db.context.TemporalConstants."
            + "CURRENTLY_VALID_END_DATE";
            */
            // This is a more "formal" way, but not necessary.
            // We don't have any endDate values that are both
            // (a) in the future, (b) less than CURRENTLY_VALID_END_DATE.
            /*
            " AND " + E1 + "." + START_DATE
            + " <= :" + DATETIME_PARAMETER + " AND :"
            + DATETIME_PARAMETER + " < " + E1 + "." + END_DATE;
            */

    // When these are needed, uncomment and make match the
    // way we do TEMPORAL_QUERY_TEMPLATE_VALID_SUFFIX_E1.
    // (Definitions below are "legacy" from the Toolkit version of this class.


//    /** Suffix template for JPQL queries to select only currently-valid rows
//     * of entity {@link #E2}. */
//    public static final String
//        TEMPORAL_QUERY_TEMPLATE_VALID_SUFFIX_E2 =
//            " AND " + E2 + "." + START_DATE
//            + " <= :" + DATETIME_PARAMETER + " AND :"
//            + DATETIME_PARAMETER + " < " + E2 + "." + END_DATE;
//
//    /** Suffix template for JPQL queries to select only currently-valid rows
//     * of entity {@link #E3}. */
//    public static final String
//        TEMPORAL_QUERY_TEMPLATE_VALID_SUFFIX_E3 =
//            " AND " + E3 + "." + START_DATE
//            + " <= :" + DATETIME_PARAMETER + " AND :"
//            + DATETIME_PARAMETER + " < " + E3 + "." + END_DATE;

    /** Get the current time in UTC as a LocalDateTime.
     * @return The current time in UTC as a LocalDateTime value.
     */
    public static LocalDateTime nowUTC() {
        return LocalDateTime.now(Clock.systemUTC());
    }

    // The following are commented out for now, as not yet needed.

//    /** Set the datetime parameter of a query to the current
//     * date/time.
//     * @param q The query to be modified.
//     * @return The modified query.
//     */
//    public static Query setDatetimeParameterNow(final Query q) {
//        return q.setParameter(DATETIME_PARAMETER, nowUTC());
//    }
//
//    /** Set the datetime parameter of a query to the current
//     * date/time.
//     * @param <T> The return type of the TypedQuery.
//     * @param q The query to be modified.
//     * @return The modified query.
//     */
//    public static <T> TypedQuery<T> setDatetimeParameterNow(
//            final TypedQuery<T> q) {
//        return q.setParameter(DATETIME_PARAMETER, nowUTC());
//    }

    /** Set any datetime constant parameters of a query to the
     * correct values.
     * @param <T> The return type of the TypedQuery.
     * @param q The query to be modified.
     * @return The modified query.
     */
    public static <T> TypedQuery<T> setDatetimeConstantParameters(
            final TypedQuery<T> q) {
        try {
            q.setParameter(CURRENTLY_VALID_END_DATE,
                    TemporalConstants.CURRENTLY_VALID_END_DATE);
        } catch (IllegalArgumentException e) {
            // No problem.
        }
        try {
            q.setParameter(DRAFT_START_DATE,
                    TemporalConstants.DRAFT_START_DATE);
        } catch (IllegalArgumentException e) {
            // No problem.
        }
        return q;
    }

    /** Set any datetime constant parameters of a query to the
     * correct values.
     * @param q The query to be modified.
     * @return The modified query.
     */
    public static Query setDatetimeConstantParameters(
            final Query q) {
        try {
            q.setParameter(CURRENTLY_VALID_END_DATE,
                    TemporalConstants.CURRENTLY_VALID_END_DATE);
        } catch (IllegalArgumentException e) {
            // No problem.
        }
        try {
            q.setParameter(DRAFT_START_DATE,
                    TemporalConstants.DRAFT_START_DATE);
        } catch (IllegalArgumentException e) {
            // No problem.
        }
        return q;
    }

    /** Set the datetime parameter of a query to a specified
     * date/time.
     * @param q The query to be modified.
     * @param dateTime The date/time value to be used in the query.
     * @return The modified query.
     */
    public static Query setDatetimeParameter(final Query q,
            final LocalDateTime dateTime) {
        return q.setParameter(DATETIME_PARAMETER, dateTime);
    }

    /** Set the datetime parameter of a query to a specified
     * date/time.
     * @param <T> The return type of the TypedQuery.
     * @param q The query to be modified.
     * @param dateTime The date/time value to be used in the query.
     * @return The modified query.
     */
    public static <T> TypedQuery<T> setCurrentDatetimeParameter(
            final TypedQuery<T> q, final LocalDateTime dateTime) {
        return q.setParameter(DATETIME_PARAMETER, dateTime);
    }

    /* Methods that set and describe properties of entities that have
     * temporal columns.*/

    /** Make an entity represent no-longer-current data, by setting
     * its end date to the specified value for the current time.
     * @param entity The entity to be made no-longer-current.
     * @param nowValue The value to be used for the end date.
     */
    public static void makeHistorical(final TemporalColumns entity,
            final LocalDateTime nowValue) {
        entity.setEndDate(nowValue);
    }

    /** Does an entity represent a historical data?
     * Prerequisite: the end date must be set.
     * @param entity The entity to be tested.
     * @return True, iff the entity represents historical data.
     */
    public static boolean isHistorical(final TemporalColumns entity) {
        // Test if now >= end_date.
        // Since there is no "isAfterOrEqual" method,
        // use !isBefore(), i.e., test if !(now < end_date).
        return !(nowUTC().isBefore(entity.getEndDate()));
    }

    /** Make an entity represent currently-valid data. This is done by
     * setting the end date only. It is the responsibility of the caller
     * to set the start date.
     * @param entity The entity to be made to represent currently-valid data.
     */
    public static void makeCurrentlyValid(final TemporalColumns entity) {
        entity.setEndDate(TemporalConstants.CURRENTLY_VALID_END_DATE);
    }

    /** Make an entity represent currently-valid data. This is done by
     * setting the start date to startDate and setting
     * the end date to mark it as currently valid.
     * @param entity The entity to be made to represent currently-valid data.
     * @param startDate The start date to be used.
     */
    public static void makeCurrentlyValid(final TemporalColumns entity,
            final LocalDateTime startDate) {
        entity.setStartDate(startDate);
        entity.setEndDate(TemporalConstants.CURRENTLY_VALID_END_DATE);
    }

    /** Does an entity represent currently-valid data?
     * Prerequisite: the end date must be set.
     * @param entity The entity to be tested.
     * @return True, iff the entity represents currently-valid data.
     */
    public static boolean isCurrent(final TemporalColumns entity) {
        return TemporalConstants.CURRENTLY_VALID_END_DATE.equals(
                entity.getEndDate());
    }

    /** Does an entity represent draft data of some sort?
     * Prerequisite: the start date must be set.
     * @param entity The entity to be tested.
     * @return True, iff the entity represents draft data of some sort,
     *      whether an addition, modification, or deletion.
     */
    public static boolean isDraft(final TemporalColumns entity) {
        return TemporalConstants.CURRENTLY_VALID_END_DATE.isBefore(
                entity.getStartDate());
    }

    /** Make an entity represent a draft.
     * @param entity The entity to be made to represent a draft.
     */
    public static void makeDraft(final TemporalColumns entity) {
        entity.setStartDate(TemporalConstants.DRAFT_START_DATE);
        entity.setEndDate(TemporalConstants.DRAFT_END_DATE);
    }

    /** Determine the temporal meaning of a temporal entity.
     * Prerequisite: the start and end dates must be set.
     * @param entity The temporal entity to be tested.
     * @return The temporal meaning of the entity.
     */
    public static TemporalMeaning getTemporalDescription(
            final TemporalColumns entity) {
        if (isHistorical(entity)) {
            return TemporalMeaning.HISTORICAL;
        }
        if (isCurrent(entity)) {
            return TemporalMeaning.CURRENT;
        }
        if (isDraft(entity)) {
            return TemporalMeaning.DRAFT;
        }
        return TemporalMeaning.UNKNOWN;
    }

}
