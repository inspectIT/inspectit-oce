package rocks.inspectit.ocelot.config.model;

import lombok.Data;

@Data
public class SecuritySettings {

    private boolean ldapEnabled = false;

    private LdapSettings ldap = new LdapSettings();
}
