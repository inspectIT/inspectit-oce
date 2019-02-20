package rocks.inspectit.oce.instrumentation;

import org.junit.jupiter.api.Test;
import rocks.inspectit.oce.TestUtils;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DataProvidersTest extends InstrumentationSysTestBase {


    void argumentAccessTest(NamedElement elem, Runnable assertions) {
        assertions.run();
    }

    @Test
    void verifyArgumentsAccessible() {
        argumentAccessTest(() -> "test123", () -> {
            Map<String, String> tags = TestUtils.getCurrentTagsAsMap();
            assertThat(tags).containsEntry("name_via_arg0", "test123");
            assertThat(tags).containsEntry("name_via_args", "test123");
            assertThat(tags).containsEntry("name_reversed", "321tset");
            assertThat(tags).containsEntry("name_reversed_upper", "321TSET");
        });
    }

    void constantParsingTest(Duration dur, Runnable assertions) {
        assertions.run();
    }

    @Test
    void verifyConstantArgumentsParsedCorrectly() {
        constantParsingTest(Duration.ofMillis(1500), () -> {
            Map<String, String> tags = TestUtils.getCurrentTagsAsMap();
            assertThat(tags).containsEntry("result", "3500");
        });
    }

    @Test
    void testDefaultMethodInstrumented() {
        NamedElement n1 = () -> "blablub";
        n1.doSomething(() -> {
            Map<String, String> tags = TestUtils.getCurrentTagsAsMap();
            assertThat(tags).containsEntry("name", "blablub");
        });

        NamedElement n2 = new NamedElement() {
            private String name = "something";

            @Override
            public void doSomething(Runnable r) {
                name = "somethingelse";
                r.run();
            }

            @Override
            public String getName() {
                return name;
            }
        };
        waitForInstrumentation(); //wait because until here the class has most likely not been loaded yet
        n1.doSomething(() -> {
            Map<String, String> tags = TestUtils.getCurrentTagsAsMap();
            assertThat(tags).containsEntry("name", "blablub");
        });
        n2.doSomething(() -> {
            Map<String, String> tags = TestUtils.getCurrentTagsAsMap();
            assertThat(tags).containsEntry("name", "something");
        });
        n2.doSomething(() -> {
            Map<String, String> tags = TestUtils.getCurrentTagsAsMap();
            assertThat(tags).containsEntry("name", "somethingelse");
        });
    }

}

