package rocks.inspectit.ocelot.autocomplete.autocompleterimpl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import rocks.inspectit.ocelot.autocomplete.util.ConfigurationQueryHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ScopeAutoCompleterTest {

    @InjectMocks
    ScopeAutoCompleter scopeAutoCompleter;

    @Mock
    ConfigurationQueryHelper configurationQueryHelper;

    @Nested
    public class GetSuggestions {
        @Test
        public void wrongPath() {
            List<String> path = Arrays.asList("inspectit", "metrics", "processor");

            List<String> output = scopeAutoCompleter.getSuggestions(path);

            assertThat(output).isEmpty();
        }

        @Test
        public void correctPath() {
            List<String> path = Arrays.asList("inspectit", "instrumentation", "scopes");
            List<String> mockOutput = Arrays.asList("my_scope", "another_scope");
            when(configurationQueryHelper.getKeysForPath(any())).thenReturn(mockOutput);

            List<String> output = scopeAutoCompleter.getSuggestions(path);

            assertThat(output).hasSize(2);
            assertThat(output).contains("my_scope");
            assertThat(output).contains("another_scope");
        }

        @Test
        public void correctPathNothingDefined() {
            List<String> path = Arrays.asList("inspectit", "instrumentation", "scopes");

            List<String> output = scopeAutoCompleter.getSuggestions(path);

            assertThat(output).isEmpty();
        }

        @Test
        public void testRulesPath() {
            List<String> rulesPath = Arrays.asList("inspectit", "instrumentation", "rules", "*", "scopes");
            List<String> scopesPath = Arrays.asList("inspectit", "instrumentation", "scopes");
            when(configurationQueryHelper.getKeysForPath(any())).thenAnswer((Answer<List<String>>) invocation -> {
                Object[] args = invocation.getArguments();
                if (args[0].equals(rulesPath)) {
                    return Collections.singletonList("rules_path_triggered");
                }
                if (args[0].equals(scopesPath)) {
                    return Collections.singletonList("scopes_path_triggered");
                }
                return Collections.singletonList("error!");
            });

            List<String> output = scopeAutoCompleter.getSuggestions(rulesPath);

            assertThat(output).hasSize(2);
            assertThat(output).contains("rules_path_triggered");
            assertThat(output).contains("scopes_path_triggered");
        }

        @Test
        public void testScopesPath() {
            List<String> scopesPath = Arrays.asList("inspectit", "instrumentation", "scopes");
            when(configurationQueryHelper.getKeysForPath(any())).thenAnswer((Answer<List<String>>) invocation -> {
                Object[] args = invocation.getArguments();
                if (args[0].equals(scopesPath)) {
                    return Collections.singletonList("scopes_path_triggered");
                }
                return Collections.singletonList("error!");
            });

            List<String> output = scopeAutoCompleter.getSuggestions(scopesPath);

            assertThat(output).hasSize(1);
            assertThat(output).contains("scopes_path_triggered");
        }
    }
}
