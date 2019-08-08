package rocks.inspectit.ocelot.security.config;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.LdapSettings;
import rocks.inspectit.ocelot.security.jwt.JwtTokenFilter;
import rocks.inspectit.ocelot.security.jwt.JwtTokenManager;
import rocks.inspectit.ocelot.utils.LdapUtils;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

import static rocks.inspectit.ocelot.user.userdetails.LocalUserDetailsService.DEFAULT_ACCESS_USER_ROLE;

/**
 * Spring security configuration enabling authentication on all except excluded endpoints.
 */
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtTokenManager tokenManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    @VisibleForTesting
    InspectitServerSettings serverSettings;

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers(
                "/v2/api-docs",
                "/configuration/**",
                "/csrf",
                "/",
                "/ui/**",
                "/swagger*/**",
                "/webjars/**",
                "/api/v1/agent/configuration");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)

                .and()
                .cors()

                .and()
                .authorizeRequests()
                .anyRequest().hasRole(getAccessRole())

                .and()
                // Custom authentication endpoint to prevent sending the "WWW-Authenticate" which causes Browsers to open the basic authentication dialog.
                // See the following post: https://stackoverflow.com/a/50023070/2478009
                .httpBasic().authenticationEntryPoint((req, resp, authException) -> resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage()))

                .and()
                //TODO: The "correct" way of selectively enabling token based would be to have multiple spring security configs.
                //However, previous attempts of doing so were unsuccessful, therefore we simply exclude them manually in the filter
                .addFilterBefore(
                        new JwtTokenFilter(tokenManager, Collections.singletonList("/api/v1/account/password")),
                        BasicAuthenticationFilter.class
                );
    }

    /**
     * Returns the role name which is required by users to get access to the secured API endpoints.
     * In case LDAP is not used, a constant role name is used, otherwise the configured role name of the LDAP settings is used.
     *
     * @return the role name to use
     */
    private String getAccessRole() {
        if (serverSettings.getSecurity().isLdapAuthentication()) {
            return serverSettings.getSecurity().getLdap().getAdminGroup();
        } else {
            return DEFAULT_ACCESS_USER_ROLE;
        }
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        if (serverSettings.getSecurity().isLdapAuthentication()) {
            configureLdapAuthentication(auth);
        } else {
            configureLocalAuthentication(auth);
        }
    }

    /**
     * Configures the user authentication to use LDAP user management and authentication
     */
    private void configureLdapAuthentication(AuthenticationManagerBuilder auth) throws Exception {
        LdapContextSource contextSource = LdapUtils.createLdapContextSource(serverSettings);
        LdapSettings ldapSettings = serverSettings.getSecurity().getLdap();

        auth
                .ldapAuthentication()
                .userSearchFilter(ldapSettings.getUserSearchFilter())
                .userSearchBase(ldapSettings.getUserSearchBase())
                .groupSearchFilter(ldapSettings.getGroupSearchFilter())
                .groupSearchBase(ldapSettings.getGroupSearchBase())
                .contextSource(contextSource);
    }

    /**
     * Configures the user authentication to use the local and embedded database for user management and authentication.
     */
    private void configureLocalAuthentication(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder);
    }
}
