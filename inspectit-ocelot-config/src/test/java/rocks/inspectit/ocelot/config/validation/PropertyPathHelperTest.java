package rocks.inspectit.ocelot.config.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.metrics.PrometheusExporterSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.scope.MatcherMode;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertyPathHelperTest {

    PropertyPathHelper propertyPathHelper;

    @BeforeEach
    void buildHelper() {
        propertyPathHelper = new PropertyPathHelper();
    }

    @Nested
    public class Parse {
        @Test
        void kebabCaseTest() {
            ArrayList<String> output = new ArrayList<>(Arrays.asList("inspectit", "iCan-parse-kebab", "case", "even-in-brackets\\wow", "thisIs-awesome"));

            assertThat(propertyPathHelper.parse("inspectit.iCan-parse-kebab.case[even-in-brackets\\wow].thisIs-awesome")).isEqualTo(output);
        }

        @Test
        void emptyString() {
            ArrayList<String> output = new ArrayList<>();

            assertThat(propertyPathHelper.parse("")).isEqualTo(output);
        }

        @Test
        void nullString() {
            ArrayList<String> output = new ArrayList<>();

            assertThat(propertyPathHelper.parse(null)).isEqualTo(output);

        }

        @Test
        void bracketAfterBracket() {
            ArrayList<String> output = new ArrayList<>(Arrays.asList("inspectit", "property", "first", "second"));

            assertThat(propertyPathHelper.parse("inspectit.property[first][second]")).isEqualTo(output);
        }

        @Test
        void dotInBrackets() {
            ArrayList<String> output = new ArrayList<>(Arrays.asList("inspectit", "property", "first.second"));

            assertThat(propertyPathHelper.parse("inspectit.property[first.second]")).isEqualTo(output);
        }

        @Test
        void throwsException() {
            try {
                propertyPathHelper.parse("inspectit.property[first.second");
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage()).isEqualTo("invalid property path");
            }
        }

    }

    @Nested
    public class CheckPropertyExists {
        @Test
        void termminalTest() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList("config", "file-based", "path"));
            Type output = String.class;

            assertThat(propertyPathHelper.getPathEndType(list, InspectitConfig.class)).isEqualTo(output);
        }

        @Test
        void nonTermminalTest() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList("exporters", "metrics", "prometheus"));
            Type output = PrometheusExporterSettings.class;

            assertThat(propertyPathHelper.getPathEndType(list, InspectitConfig.class)).isEqualTo(output);
        }

        @Test
        void emptyString() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList(""));
            Type output = null;

            assertThat(propertyPathHelper.getPathEndType(list, InspectitConfig.class)).isEqualTo(output);
        }

        @Test
        void existingList() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList("instrumentation", "scopes", "jdbc_statement_execute", "interfaces", "0", "matcher-mode"));
            Type output = MatcherMode.class;

            assertThat(propertyPathHelper.getPathEndType(list, InspectitConfig.class)).isEqualTo(output);
        }

        @Test
        void existingMap() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList("metrics", "definitions", "jvm/gc/concurrent/phase/time", "description"));
            Type output = String.class;

            assertThat(propertyPathHelper.getPathEndType(list, InspectitConfig.class)).isEqualTo(output);
        }

        @Test
        void readMethodIsNull() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList("instrumentation", "data", "method_duration", "is-tag"));
            Type output = boolean.class;

            assertThat(propertyPathHelper.getPathEndType(list, InspectitConfig.class)).isEqualTo(output);
        }

        @Test
        void endsInWildcardType() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList("instrumentation", "actions", "string_replace_all", "input", "regex"));
            Type output = String.class;

            assertThat(propertyPathHelper.getPathEndType(list, InspectitConfig.class)).isEqualTo(output);
        }
    }

    @Nested
    public class CheckPropertyExistsInMap {
        @Test
        void nonTerminalMapTest() {
            ArrayList<String> list = new ArrayList<>(Arrays.asList("matcher-mode"));

            assertThat(propertyPathHelper.getPathEndType(list, Map.class)).isEqualTo(null);

        }
    }

    @Nested
    public class CheckPropertyExistsInList {
        @Test
        void nonTerminalListTest() {
            ArrayList<String> list = new ArrayList(Arrays.asList("instrumentation", "scopes", "jdbc_statement_execute", "interfaces", "0", "matcher-mode"));
            Type output = MatcherMode.class;

            assertThat(propertyPathHelper.getPathEndType(list, InspectitConfig.class)).isEqualTo(output);
        }
    }
}
