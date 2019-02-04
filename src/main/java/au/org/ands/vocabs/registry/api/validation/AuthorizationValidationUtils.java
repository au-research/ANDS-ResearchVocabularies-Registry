/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.api.validation;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.api.auth.AuthUtils;
import au.org.ands.vocabs.registry.api.user.ValidationError;
import au.org.ands.vocabs.registry.db.dao.UploadDAO;
import au.org.ands.vocabs.registry.db.entity.Upload;
import au.org.ands.vocabs.registry.enums.AccessPointType;
import au.org.ands.vocabs.registry.schema.vocabulary201701.AccessPoint;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Version;
import au.org.ands.vocabs.registry.schema.vocabulary201701.Vocabulary;

/** Utility methods to support validation, where authorization checks
 * are required. */
public final class AuthorizationValidationUtils {

    // Leave logger here, though it is unused. We might want to use
    // it later.
    /** Logger for this class. */
    @SuppressWarnings("unused")
    private static Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Private constructor for a utility class. */
    private AuthorizationValidationUtils() {
    }

    /** Check if the caller is authorized with regards to the content
     * of a vocabulary specified in registry schema format.
     * @param profile The caller's security profile.
     * @param vocabulary The vocabulary to be checked.
     * @param mode The validation mode.
     * @return A list of validation errors, if there are any. If there
     *      are no errors, an empty list is returned (i.e., not null).
     */
    public static List<ValidationError> checkAuthorizationForContent(
            final CommonProfile profile,
            final Vocabulary vocabulary,
            final ValidationMode mode) {
        List<ValidationError> validationErrors = new ArrayList<>();

        // Things to be tested:
        // * if mode == UPDATE: vocabulary owner
        // * uploadId of file access points

        // Vocabulary owner
        // If the vocabulary is being updated, the user must also have
        // the role specified in the owner field of the updated vocabulary.
        // So, in practice, the value
        // of the owner field may be changed, as long as both the
        // original and final values are in the user's purview.
        // Note the test "mode == ValidationMode.UPDATE": the
        // createVocabulary() method does the same check of the owner field,
        // but issues a different error in that case.
        if (mode == ValidationMode.UPDATE
                && !AuthUtils.ownerIsAuthorizedByOrganisationOrUsername(
                        profile, vocabulary.getOwner())) {
            ValidationError ve = new ValidationError();
            ve.setMessage("Not authorised to change the owner to this value.");
            ve.setPath("owner");
            validationErrors.add(ve);
        }

        // For file access points, that the uploadId corresponds
        // to an existing upload, that we own.
        // We rely on the CheckVocabulary annotation having already
        // required that the uploadId be specified.
        for (Version version : vocabulary.getVersion()) {
            for (AccessPoint ap : version.getAccessPoint()) {
                if (ap.getDiscriminator() == AccessPointType.FILE) {
                    Integer uploadId = ap.getApFile().getUploadId();
                    Upload upload = UploadDAO.getUploadById(uploadId);
                    if (upload == null
                            || !AuthUtils.
                            ownerIsAuthorizedByOrganisationOrUsername(profile,
                                    upload.getOwner())) {
                        ValidationError ve = new ValidationError();
                        ve.setMessage("Upload Id does not correspond to an "
                                + "upload owned by this user.");
                        ve.setPath(uploadId.toString());
                        validationErrors.add(ve);
                    }
                }
            }
        }
        return validationErrors;
    }

}
