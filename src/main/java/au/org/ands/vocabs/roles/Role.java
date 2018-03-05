/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.roles;

import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.builder.EqualsBuilder;

import au.org.ands.vocabs.roles.db.entity.RoleTypeId;

/** Representation of one role. */
@XmlType
public class Role {

    /** id. */
    private String id;
    /** typeId. */
    private RoleTypeId typeId;
    /** fullName. */
    private String fullName;

    /** Default constructor. Currently used during automated testing
     * when parsing XML generated from instances of this class. */
    public Role() {
    }

    /** Constructor that takes all properties.
     * @param anId The value of id.
     * @param aTypeId The value of typeId.
     * @param aFullName The value of fullName.
     */
    public Role(final String anId, final RoleTypeId aTypeId,
            final String aFullName) {
        id = anId;
        typeId = aTypeId;
        fullName = aFullName;
    }

    /** Get the value of id.
     * @return The value of id.
     */
    public String getId() {
        return this.id;
    }

    /** Set the value of id.
     * @param anId The value of id
     *      to set.
     */
    public void setId(
            final String anId) {
        id = anId;
    }

    /** Get the value of typeId.
     * @return The value of typeId.
     */
    public RoleTypeId getTypeId() {
        return this.typeId;
    }

    /** Set the value of typeId.
     * @param aTypeId The value of typeId
     *      to set.
     */
    public void setTypeId(
            final RoleTypeId aTypeId) {
        typeId = aTypeId;
    }

    /** Get the value of fullName.
     * @return The value of fullName.
     */
    public String getFullName() {
        return this.fullName;
    }

    /** Set the value of fullName.
     * @param aFullName The value of fullName
     *      to set.
     */
    public void setFullName(
            final String aFullName) {
        fullName = aFullName;
    }

    /** {@inheritDoc}
     * Equality test based on all properties.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == null
                || !(other instanceof Role)) {
            return false;
        }
        Role otherRole = (Role) other;
        return new EqualsBuilder().
                append(id, otherRole.getId()).
                append(typeId, otherRole.getTypeId()).
                append(fullName, otherRole.getFullName()).
                isEquals();
    }

    /** {@inheritDoc}
     * The hash code returned is that of the id.
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
