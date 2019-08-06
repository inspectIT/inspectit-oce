package rocks.inspectit.ocelot.user.userdetails;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import rocks.inspectit.ocelot.config.model.DefaultUserSettings;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.SecuritySettings;
import rocks.inspectit.ocelot.user.userdetails.LocalUserDetailsService;
import rocks.inspectit.ocelot.user.User;
import rocks.inspectit.ocelot.user.UserRepository;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LocalUserDetailsServiceTest {

    @InjectMocks
    LocalUserDetailsService detailsService;

    @Mock
    UserRepository repository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Nested
    class AddOrUpdateUser {

        @Test
        void verifyUsernameConvertedToLowerCase() {
            User result = new User();
            User input = User.builder()
                    .id(42L)
                    .password("test")
                    .username("CamelCase")
                    .build();
            when(repository.save(any())).thenReturn(result);
            when(passwordEncoder.encode(anyString())).thenReturn("password-hash");

            User actualResult = detailsService.addOrUpdateUser(input);

            ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
            verify(repository).save(argument.capture());
            assertThat(argument.getValue().getUsername()).isEqualTo("camelcase");
            assertThat(actualResult).isSameAs(result);
        }
    }


    @Nested
    class GetUserByName {

        @Test
        void verifyUsernameConvertedToLowerCase() {
            detailsService.getUserByName("CamelCase");

            ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
            verify(repository).findByUsername(argument.capture());
            assertThat(argument.getValue()).isEqualTo("camelcase");
        }
    }

    @Nested
    class AddDefaultUserIfRequired {

        private Method testMethod = ReflectionUtils.findMethod(LocalUserDetailsService.class, "addDefaultUserIfRequired").get();

        @Test
        public void addDefaultUser() {
            SecuritySettings securitySettings = SecuritySettings.builder().ldapAuthentication(false).build();
            DefaultUserSettings userSettings = DefaultUserSettings.builder().name("username").password("passwd").build();
            InspectitServerSettings settings = InspectitServerSettings.builder().security(securitySettings).defaultUser(userSettings).build();
            detailsService.settings = settings;
            when(repository.count()).thenReturn(0L);
            when(passwordEncoder.encode(anyString())).thenReturn("password-hash");

            ReflectionUtils.invokeMethod(testMethod, detailsService);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(repository).save(userCaptor.capture());
            verify(repository).count();
            verifyNoMoreInteractions(repository);
            assertThat(userCaptor.getValue()).isNotNull();
            assertThat(userCaptor.getValue().getUsername()).isEqualTo("username");
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("passwd");
            assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("password-hash");
        }

        @Test
        public void doNothing() {
            SecuritySettings securitySettings = SecuritySettings.builder().ldapAuthentication(false).build();
            InspectitServerSettings settings = InspectitServerSettings.builder().security(securitySettings).build();
            detailsService.settings = settings;
            when(repository.count()).thenReturn(1L);

            ReflectionUtils.invokeMethod(testMethod, detailsService);

            verify(repository).count();
            verifyNoMoreInteractions(repository);
        }

        @Test
        public void ldapAuthentication() {
            SecuritySettings securitySettings = SecuritySettings.builder().ldapAuthentication(true).build();
            InspectitServerSettings settings = InspectitServerSettings.builder().security(securitySettings).build();
            detailsService.settings = settings;

            ReflectionUtils.invokeMethod(testMethod, detailsService);

            verifyZeroInteractions(repository);
        }
    }
}
