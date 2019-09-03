package rocks.inspectit.ocelot.autocomplete;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.instrumentation.scope.InstrumentationScopeSettings;
import rocks.inspectit.ocelot.config.validation.PropertyPathHelper;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AutoCompleterModelTest {

    @InjectMocks
    AutoCompleterModel completer;

    @Mock
    PropertyPathHelper propertyPathHelper;

    @Nested
    public class CheckPropertyExists {
        @Test
        void checkFirstLevel() {
            List<String> input = Arrays.asList("inspectit", "instrumentation");
            List<String> output = Arrays.asList("actions",
                    "class",
                    "data",
                    "exclude-lambdas",
                    "ignored-bootstrap-packages",
                    "ignored-packages",
                    "internal",
                    "rules",
                    "scopes",
                    "special");
            Optional<PropertyDescriptor> mapDescriptor = Arrays.stream(BeanUtils.getPropertyDescriptors(InspectitConfig.class)).filter(descriptor -> descriptor.getName().equals("instrumentation"))
                    .findFirst();
            Type typeOfMap = mapDescriptor.get().getPropertyType();
            AutoCompleterModel completer1 = Mockito.spy(completer);
            when(propertyPathHelper.getPathEndType(any(), any())).thenReturn(typeOfMap);
            when(propertyPathHelper.isTerminal(any())).thenReturn(false);
            when(propertyPathHelper.isListOfTerminalTypes(any())).thenReturn(false);


            assertThat(completer1.getSuggestions(input)).isEqualTo(output);
        }

        @Test
        void pastMap() {
            List<String> input = Arrays.asList("inspectit", "instrumentation", "scopes", "my-key");
            List<String> output = Arrays.asList("advanced",
                    "class",
                    "interfaces",
                    "methods",
                    "narrow-scope",
                    "superclass",
                    "type");
            Type t = InstrumentationScopeSettings.class;
            AutoCompleterModel completer1 = Mockito.spy(completer);
            when(propertyPathHelper.getPathEndType(any(), any())).thenReturn(t);

            assertThat(completer1.getSuggestions(input)).isEqualTo(output);
        }

        @Test
        void pastList() {
            List<String> input = Arrays.asList("inspectit.");
            ArrayList<String> output = new ArrayList<>(Arrays.asList("class",
                    "config",
                    "exporters",
                    "instrumentation",
                    "logging",
                    "metrics",
                    "self-monitoring",
                    "service-name",
                    "tags",
                    "thread-pool-size",
                    "tracing"));
            Type t = InspectitConfig.class;
            AutoCompleterModel completer1 = Mockito.spy(completer);
            when(propertyPathHelper.getPathEndType(any(), any())).thenReturn(t);

            assertThat(completer1.getSuggestions(input)).isEqualTo(output);
        }

        @Test
        void endsInWildcard() {
            List<String> input = Arrays.asList("inspectit", "instrumentation", "actions", "string_replace_all", "input", "regex");
            ArrayList<String> output = new ArrayList<>();
            Type t = String.class;
            when(propertyPathHelper.getPathEndType(any(), any())).thenReturn(t);
            when(propertyPathHelper.isTerminal(any())).thenReturn(true);
            AutoCompleterModel completer1 = Mockito.spy(completer);

            assertThat(completer1.getSuggestions(input)).isEqualTo(output);
        }

        @Test
        void propertyIsPresentAndReadMethodIsNull() {
            List<String> input = Arrays.asList("inspectit", "instrumentation", "data", "method_duration", "is-tag");
            ArrayList<String> output = new ArrayList<>();
            Type t = boolean.class;
            AutoCompleterModel completer1 = Mockito.spy(completer);
            when(propertyPathHelper.getPathEndType(any(), any())).thenReturn(t);

            assertThat(completer1.getSuggestions(input)).isEqualTo(output);
        }
    }

    @Nested
    public class GetProperties {
        void getPropertiesInspectit() {
            List<String> output = Arrays.asList("class",
                    "config",
                    "exporters",
                    "instrumentation",
                    "logging",
                    "metrics",
                    "self-monitoring",
                    "service-name",
                    "tags",
                    "thread-pool-size",
                    "tracing");

            assertThat(completer.getProperties(InspectitConfig.class)).isEqualTo(output);
        }
    }
}
