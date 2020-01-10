package rocks.inspectit.ocelot.autocomplete.autocompleterimpl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.autocomplete.util.YamlFileHelper;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DataInputAutoCompleterTest {

    @InjectMocks
    DataInputAutoCompleter actionInputAutoCompleter;

    @Mock
    YamlFileHelper yamlFileHelper;

    @Nested
    public class GetSuggestions {
        @Test
        public void wrongPath() {
            List<String> path = Arrays.asList("inspectit", "metrics", "processor");
            List<String> output = Arrays.asList();

            assertThat(actionInputAutoCompleter.getSuggestions(path)).isEqualTo(output);
        }

        @Test
        public void correctPath() {
            List<String> path = Arrays.asList("inspectit", "instrumentation", "rules", "*", "entry", "*", "data-input");
            List<String> output = Arrays.asList("myDataInput");
            List<String> mockOutput = Arrays.asList("myDataInput");
            when(yamlFileHelper.extractKeysFromYamlFiles(any())).thenReturn(mockOutput);

            assertThat(actionInputAutoCompleter.getSuggestions(path)).isEqualTo(output);
        }

        @Test
        public void correctPathNothingDefined() {
            List<String> path = Arrays.asList("inspectit", "instrumentation", "rules", "*", "entry", "*", "data-input");
            List<String> output = Arrays.asList();

            assertThat(actionInputAutoCompleter.getSuggestions(path)).isEqualTo(output);
        }
    }

}