package rocks.inspectit.ocelot.agentstatus;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.agentconfiguration.AgentConfiguration;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(MockitoExtension.class)
public class AgentStatusManagerTest {

    @InjectMocks
    AgentStatusManager manager;

    @BeforeEach
    void init() {
        manager.config = InspectitServerSettings.builder()
                .maxAgents(100)
                .agentEvictionDelay(Duration.ofDays(1))
                .build();
        manager.reset();
    }

    @Nested
    class NotifyAgentConfigurationFetched {

        @Test
        void testWithAgentIdHeader() {
            AgentConfiguration config = new AgentConfiguration(
                    AgentMapping.builder()
                            .name("test-conf")
                            .build(), "");
            Map<String, String> attributes = ImmutableMap.of("service", "test");

            manager.notifyAgentConfigurationFetched(attributes, Collections.singletonMap("x-ocelot-agent-id", "aid"), config);

            assertThat(manager.getAgentStatuses())
                    .hasSize(1)
                    .anySatisfy(status -> {
                        assertThat(status.getAttributes()).isEqualTo(attributes);
                        assertThat(status.getMappingName()).isEqualTo("test-conf");
                        assertThat(status.getMetaInformation().getAgentId()).isEqualTo("aid");
                        assertThat(status.getLastConfigFetch()).isNotNull();
                    });
        }

        @Test
        void testNoMappingFound() {
            Map<String, String> attributes = ImmutableMap.of("service", "test");
            manager.notifyAgentConfigurationFetched(attributes, Collections.emptyMap(), null);

            assertThat(manager.getAgentStatuses())
                    .hasSize(1)
                    .anySatisfy(status -> {
                        assertThat(status.getAttributes()).isEqualTo(attributes);
                        assertThat(status.getMappingName()).isNull();
                        assertThat(status.getMetaInformation()).isNull();
                        assertThat(status.getLastConfigFetch()).isNotNull();
                    });
        }


        @Test
        void testMappingFound() {
            Map<String, String> attributes = ImmutableMap.of("service", "test");
            AgentConfiguration conf = new AgentConfiguration(
                    AgentMapping.builder()
                            .name("test-conf")
                            .build(), "");

            manager.notifyAgentConfigurationFetched(attributes, Collections.emptyMap(), conf);

            assertThat(manager.getAgentStatuses())
                    .hasSize(1)
                    .anySatisfy(status -> {
                        assertThat(status.getAttributes()).isEqualTo(attributes);
                        assertThat(status.getMappingName()).isEqualTo("test-conf");
                        assertThat(status.getLastConfigFetch()).isNotNull();
                    });
        }

        @Test
        void testOverriding() throws Exception {
            Map<String, String> attributes = ImmutableMap.of("service", "test");
            AgentConfiguration conf = new AgentConfiguration(
                    AgentMapping.builder()
                            .name("test-conf")
                            .build(), "");

            manager.notifyAgentConfigurationFetched(attributes, Collections.emptyMap(), null);

            assertThat(manager.getAgentStatuses())
                    .hasSize(1)
                    .anySatisfy(status -> {
                        assertThat(status.getAttributes()).isEqualTo(attributes);
                        assertThat(status.getMappingName()).isNull();
                        assertThat(status.getLastConfigFetch()).isNotNull();
                    });

            Date firstFetch = manager.getAgentStatuses().iterator().next().getLastConfigFetch();

            Thread.sleep(1);

            manager.notifyAgentConfigurationFetched(attributes, Collections.emptyMap(), conf);

            assertThat(manager.getAgentStatuses())
                    .hasSize(1)
                    .anySatisfy(status -> {
                        assertThat(status.getAttributes()).isEqualTo(attributes);
                        assertThat(status.getMappingName()).isEqualTo("test-conf");
                        assertThat(status.getLastConfigFetch()).isAfter(firstFetch);
                    });
        }
    }
}
