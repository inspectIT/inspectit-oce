package rocks.inspectit.oce.eum.server.beacon.processor;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import rocks.inspectit.oce.eum.server.beacon.Beacon;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class URLNormalizeBeaconProcessorTest {

    private BeaconProcessor processor = new URLNormalizeBeaconProcessor();
    private Beacon beacon = Beacon.of(ImmutableMap.of(
            "u", "http://localhost:8080/a/b?param1=value1&param2=value2",
            "pgu", "http://localhost:8080/a/b?param1=value1&param2=value2")
    );

    @Test
    void testRemoveQueryParameters() {
        Beacon b = processor.process(beacon);
        assertThat(b.contains(URLNormalizeBeaconProcessor.TAG_PGU_NO_QUERY)).isTrue();
        assertThat(b.contains(URLNormalizeBeaconProcessor.TAG_U_NO_QUERY)).isTrue();

        assertThat(b.get(URLNormalizeBeaconProcessor.TAG_U_NO_QUERY)).isEqualTo("http://localhost:8080/a/b");
        assertThat(b.get(URLNormalizeBeaconProcessor.TAG_PGU_NO_QUERY)).isEqualTo("http://localhost:8080/a/b");
    }

    @Test
    void addEmptyURIValuesOnMissingSourceValues() {
        Beacon b = processor.process(Beacon.of(new HashMap<>()));
        assertThat(b.get(URLNormalizeBeaconProcessor.TAG_PGU_NO_QUERY)).isEqualTo("");
        assertThat(b.get(URLNormalizeBeaconProcessor.TAG_U_NO_QUERY)).isEqualTo("");
    }


}