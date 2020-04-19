package rocks.inspectit.ocelot.security.userdetails;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.conditional.ConditionalOnLdap;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

/**
 * The user details service used for authentication against the configured LDAP system.
 */
@Component
@Order(1)
@ConditionalOnLdap
public class CustomLdapUserDetailsService extends LdapUserDetailsService {

    @Autowired
    private InspectitServerSettings settings;

    @Autowired
    public CustomLdapUserDetailsService(LdapUserSearch ldapUserSearch, DefaultLdapAuthoritiesPopulator ldapAuthoritiesPopulator) {
        super(ldapUserSearch, ldapAuthoritiesPopulator);
    }

    /**
     * Loads {@link UserDetails} by a username. See {@link UserDetailsService#loadUserByUsername(String)}.
     * <p>
     * If LDAP authentication is disabled, this method will always throw a {@link UsernameNotFoundException}.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!settings.getSecurity().isLdapAuthentication()) {
            throw new UsernameNotFoundException(username);
        }
        UserDetails user = super.loadUserByUsername(username);
        String resolvedRole = resolveAccessRole(user);

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(resolvedRole)
                .build();
    }

    private String resolveAccessRole(UserDetails user) {
        String ldapAdminGroup = settings.getSecurity().getLdap().getAdminGroup();
        if (user.getAuthorities().stream().anyMatch(authority -> ((GrantedAuthority) authority).getAuthority().toLowerCase().equals("role_" + ldapAdminGroup.toLowerCase()))) {
            return "OCELOT_ADMIN";
        }
        return "OCELOT_BASIC";
    }
}
