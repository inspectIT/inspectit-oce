package rocks.inspectit.ocelot.autocomplete;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.FileManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AutoCompleterSemanticTest {
    @Mock
    private FileManager manager;

    @InjectMocks
    private AutoCompleterSemantic autoCompleter;

    @Nested
    public class GetSuggestions {

        @Test
        public void getScopeSuggestion() throws IOException {
            ArrayList<String> propertyPath = new ArrayList<>(Arrays.asList("inspectit", "instrumentation", "scopes"));
            ArrayList<String> output = new ArrayList<>(Arrays.asList("my_scope1", "another_scope1"));
            AutoCompleterSemantic autoCompleter1 = Mockito.spy(autoCompleter);
            String mockInput = "inspectit:\n" +
                    "  instrumentation:\n" +
                    "    scopes: {my_scope1: null, another_scope1: null}\n" +
                    "traits: [ONE_HAND, ONE_EYE]\n" +
                    "sources: /common\n" +
                    "name: Example Mapping 1\n";
            ArrayList<String> mockPaths = new ArrayList<>(Arrays.asList("mockpath"));
            when(manager.readFile(any())).thenReturn(mockInput);
            Mockito.doReturn(mockPaths).when(autoCompleter1).getAllPaths();

            assertThat(autoCompleter1.getSuggestions(propertyPath)).isEqualTo(output);
        }

        @Test
        public void getNonScopeSuggestion() throws IOException {
            ArrayList<String> propertyPath = new ArrayList<>(Arrays.asList("inspectit", "metrics"));
            ArrayList<String> output = new ArrayList<>();
            AutoCompleterSemantic autoCompleter1 = Mockito.spy(autoCompleter);
            String mockInput = "inspectit:\n" +
                    "  instrumentation:\n" +
                    "    scopes: {my_scope1: null, another_scope1: null}\n" +
                    "traits: [ONE_HAND, ONE_EYE]\n" +
                    "sources: /common\n" +
                    "name: Example Mapping 1\n";
            ArrayList<String> mockPaths = new ArrayList<>(Arrays.asList("mockpath"));
            when(manager.readFile(any())).thenReturn(mockInput);
            Mockito.doReturn(mockPaths).when(autoCompleter1).getAllPaths();

            assertThat(autoCompleter1.getSuggestions(propertyPath)).isEqualTo(output);
        }

        @Test
        public void noScopeDefined() throws IOException {
            ArrayList<String> propertyPath = new ArrayList<>(Arrays.asList("inspectit", "instrumentation", "scopes"));
            ArrayList<String> output = new ArrayList<>();
            AutoCompleterSemantic autoCompleter1 = Mockito.spy(autoCompleter);
            String mockInput = "inspectit:\n" +
                    "  instrumentation:\n" +
                    "traits: [ONE_HAND, ONE_EYE]\n" +
                    "sources: /common\n" +
                    "name: Example Mapping 1\n";
            ArrayList<String> mockPaths = new ArrayList<>(Arrays.asList("mockpath"));
            when(manager.readFile(any())).thenReturn(mockInput);
            Mockito.doReturn(mockPaths).when(autoCompleter1).getAllPaths();

            assertThat(autoCompleter1.getSuggestions(propertyPath)).isEqualTo(output);

        }
    }
}
