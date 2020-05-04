package rocks.inspectit.ocelot.security.userdetails;

import com.google.common.annotations.VisibleForTesting;
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
import rocks.inspectit.ocelot.config.model.LdapRoleResolveSettings;
import rocks.inspectit.ocelot.security.config.UserRoleConfiguration;

import java.util.Collection;
import java.util.List;

/**
 * The user details service used for authentication against the configured LDAP system.
 */
@Component
@Order(1)
@ConditionalOnLdap
public class CustomLdapUserDetailsService extends LdapUserDetailsService {

    @Autowired
    @VisibleForTesting
    InspectitServerSettings settings;

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
        String[] permissions = resolvePermissionSet(user);
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(permissions)
                .build();
    }

    /**
     * Maps the in the ldap section of the server config defined ldap roles to a role-set internally used for access
     * control. Always returns the role-set with highest access level if user contains multiple matching authorities.
     *
     * @param user The LDAP-User object the roles should be resolved of.
     * @return The highest level of access role the user's authorities could be resolved to.
     */
    @VisibleForTesting
    String[] resolvePermissionSet(UserDetails user) {
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        LdapRoleResolveSettings role_settings = settings.getSecurity().getLdap().getRoles();
        if (containsAuthority(authorities, role_settings.getAdmin()) || hasAdminGroup(authorities)) {
            return UserRoleConfiguration.ADMIN_ROLE_PERMISSION_SET;
        }
        if (containsAuthority(authorities, role_settings.getCommit())) {
            return UserRoleConfiguration.COMMIT_ROLE_PERMISSION_SET;
        }
        if (containsAuthority(authorities, role_settings.getWrite())) {
            return UserRoleConfiguration.WRITE_ROLE_PERMISSION_SET;
        }
        return UserRoleConfiguration.READ_ROLE_PERMISSION_SET;
    }

    /**
     * Checks if at least one entry of a Collection of authorities is contained in a List of Strings.
     *
     * @param authorities A Collection containing GrantedAuthority objects.
     * @param roleList    The List of Strings the authorities are checked with.
     * @return Returns true if at least one element of authorities is contained in roleList or vice versa.
     */
    private boolean containsAuthority(Collection<? extends GrantedAuthority> authorities, List<String> roleList) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .distinct()
                .anyMatch(authority -> roleList.contains(authority.substring("ROLE_".length())));
    }

    /**
     * Checks if a collection of GrantedAuthorities contains the admin group defined in application.yml. This method
     * ensures backwards compatibility for the old configuration standard.
     *
     * @param authorities A collection of authorities the admin group should be contained in.
     * @return True if the given admin group is contained in the authorities. Otherwise false.
     */
    private boolean hasAdminGroup(Collection<? extends GrantedAuthority> authorities) {
        String ldapAdminGroup = settings.getSecurity().getLdap().getAdminGroup();
        return authorities.stream()
                .anyMatch(
                        authority -> authority.getAuthority()
                                .substring("ROLE_".length())
                                .equals(ldapAdminGroup)
                );
    }
}
