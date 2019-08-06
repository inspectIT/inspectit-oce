package rocks.inspectit.ocelot.rest.users;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rocks.inspectit.ocelot.IntegrationTestBase;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.rest.ErrorInfo;
import rocks.inspectit.ocelot.user.userdetails.LocalUserDetailsService;
import rocks.inspectit.ocelot.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static rocks.inspectit.ocelot.rest.users.AccountController.PasswordChangeRequest;

public class AccountControllerIntTest extends IntegrationTestBase {

    @Autowired
    InspectitServerSettings settings;

    @Autowired
    LocalUserDetailsService userDetailsService;

    @AfterEach
    void cleanupUsers() {
        userDetailsService.getUsers().forEach(u -> {
            if (!u.getUsername().equalsIgnoreCase(settings.getDefaultUser().getName())) {
                userDetailsService.deleteUserById(u.getId());
            }
        });
    }

    @Nested
    class ChangePassword {

        @Test
        void testChange() {
            userDetailsService.addOrUpdateUser(User.builder()
                    .username("John")
                    .password("doe")
                    .build());

            PasswordChangeRequest req = new PasswordChangeRequest("Foo");

            ResponseEntity<String> result = rest
                    .withBasicAuth("John", "doe").exchange(
                            "/api/v1/account/password",
                            HttpMethod.PUT,
                            new HttpEntity<>(req),
                            new ParameterizedTypeReference<String>() {
                            });
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);

            String pwHash = userDetailsService
                    .getUserByName("John").get()
                    .getPasswordHash();
            assertThat(userDetailsService.getPasswordEncoder()
                    .matches("Foo", pwHash))
                    .isTrue();
        }

        @Test
        void emptyPassword() {
            PasswordChangeRequest req = new PasswordChangeRequest("");

            ResponseEntity<ErrorInfo> result = authRest.exchange(
                    "/api/v1/account/password",
                    HttpMethod.PUT,
                    new HttpEntity<>(req),
                    new ParameterizedTypeReference<ErrorInfo>() {
                    });
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(result.getBody().getError()).isEqualTo(ErrorInfo.Type.NO_PASSWORD);

        }

        @Test
        void nullPassword() {
            PasswordChangeRequest req = new PasswordChangeRequest(null);

            ResponseEntity<ErrorInfo> result = authRest.exchange(
                    "/api/v1/account/password",
                    HttpMethod.PUT,
                    new HttpEntity<>(req),
                    new ParameterizedTypeReference<ErrorInfo>() {
                    });
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(result.getBody().getError()).isEqualTo(ErrorInfo.Type.NO_PASSWORD);

        }

    }
}
