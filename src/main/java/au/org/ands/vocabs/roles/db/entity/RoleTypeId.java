/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.roles.db.entity;

import javax.xml.bind.annotation.XmlEnumValue;

/** Role Type Id. */
public enum RoleTypeId {

    /** User. */
    @XmlEnumValue("ROLE_USER")
    ROLE_USER,

    /** Organizational. */
    @XmlEnumValue("ROLE_ORGANISATIONAL")
    ROLE_ORGANISATIONAL,

    /** Functional. */
    @XmlEnumValue("ROLE_FUNCTIONAL")
    ROLE_FUNCTIONAL,

    /** DOI. */
    @XmlEnumValue("ROLE_DOI_APPID")
    ROLE_DOI_APPID

}
