/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.db.entity;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

/** Validation of Version entities for persistence purposes.
 * The methods of this class prevent invalid Version entities from
 * being persisted. */
public class VersionListener {

    // Uncomment and use as needed during development.
//    /** Logger for this class. */
//    private Logger logger = LoggerFactory.getLogger(
//            MethodHandles.lookup().lookupClass());

    /** Check the validity of a Version proposed to be persisted or updated.
     * @param version Version to be checked.
     * @throws PersistenceValidationException If this Version entity
     *      must not be persisted for validation reasons.
     */
    @PrePersist
    @PreUpdate
    public void prePersistVersion(final Version version)
            throws PersistenceValidationException {
//        logger.debug("In VersionListener.prePersistVersion");
        if (version.getStartDate().isAfter(version.getEndDate())) {
            throw new PersistenceValidationException(
                    "Version start date after end date");
        }
    }
}
