package rocks.inspectit.oce.eum.server.beacon.processor;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import rocks.inspectit.oce.eum.server.beacon.Beacon;

class CompositeBeaconProcessorTest {

    BeaconProcessor p1 = beacon -> beacon.merge(ImmutableMap.of("key1", "value2"));
    BeaconProcessor p2 = beacon -> beacon.merge(ImmutableMap.of("key2", "value2"));

    @Test
    public void test() {
        Beacon beacon = Beacon.of(ImmutableMap.of("key1", "value1"));

        CompositeBeaconProcessor processor = new CompositeBeaconProcessor(Lists.list(p1, p2));

        Beacon processedBeacon = processor.process(beacon);

        // Ensure value got properly overwritten
        Assertions.assertThat(processedBeacon.get("key1")).isEqualTo("value2");
        // Ensure new key was properly added
        Assertions.assertThat(processedBeacon.get("key2")).isEqualTo("value2");
    }

}