/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.roles.db.entity;

import javax.xml.bind.annotation.XmlEnumValue;

/** Authentication Service Id. */
public enum AuthenticationServiceId {

    /** Built-in. */
    @XmlEnumValue("AUTHENTICATION_BUILT_IN")
    AUTHENTICATION_BUILT_IN,

    /** LDAP. */
    @XmlEnumValue("AUTHENTICATION_LDAP")
    AUTHENTICATION_LDAP,

    /** Shibboleth; Rapid Connect. */
    @XmlEnumValue("AUTHENTICATION_SHIBBOLETH")
    AUTHENTICATION_SHIBBOLETH,

    /** Facebook. */
    @XmlEnumValue("AUTHENTICATION_SOCIAL_FACEBOOK")
    AUTHENTICATION_SOCIAL_FACEBOOK,

    /** Google. */
    @XmlEnumValue("AUTHENTICATION_SOCIAL_GOOGLE")
    AUTHENTICATION_SOCIAL_GOOGLE,

    /** Twitter. */
    @XmlEnumValue("AUTHENTICATION_SOCIAL_TWITTER")
    AUTHENTICATION_SOCIAL_TWITTER

}
