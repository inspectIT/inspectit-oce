package rocks.inspectit.ocelot.core.selfmonitoring;

import io.opencensus.stats.AggregationData;
import io.opencensus.stats.View;
import io.opencensus.stats.ViewData;
import io.opencensus.stats.ViewManager;
import io.opencensus.tags.TagValue;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import rocks.inspectit.ocelot.core.SpringTestBase;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;


public class LogMetricsAppenderIntTest extends SpringTestBase {

    @Autowired
    ViewManager viewManager;

    Logger log = LoggerFactory.getLogger(LogMetricsAppenderIntTest.class);



    @Test
    void logInfoMessage() throws Exception {
        log.info("Info Message");

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            ViewData logCounterView = viewManager.getView(View.Name.create("inspectit/self/logs"));
            assertThat(logCounterView).isNotNull();
            Map<List<TagValue>, AggregationData> aggregationMap = logCounterView.getAggregationMap();
            assertThat(aggregationMap).isNotNull().isNotEmpty();
            assertThat(aggregationMap.keySet()).anyMatch(tagValueList -> tagValueList.contains(TagValue.create("INFO")));
        });
    }

    @Test
    void logErrorMessage() throws Exception {
        log.error("Error Message");

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            ViewData logCounterView = viewManager.getView(View.Name.create("inspectit/self/logs"));
            assertThat(logCounterView).isNotNull();
            Map<List<TagValue>, AggregationData> aggregationMap = logCounterView.getAggregationMap();
            assertThat(aggregationMap).isNotNull().isNotEmpty();
            assertThat(aggregationMap.keySet()).anyMatch(tagValueList -> tagValueList.contains(TagValue.create("ERROR")));
        });
    }

    @Test
    void logWarnMessage() throws Exception {
        log.warn("Warning Message");

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            ViewData logCounterView = viewManager.getView(View.Name.create("inspectit/self/logs"));
            assertThat(logCounterView).isNotNull();
            Map<List<TagValue>, AggregationData> aggregationMap = logCounterView.getAggregationMap();
            assertThat(aggregationMap).isNotNull().isNotEmpty();
            assertThat(aggregationMap.keySet()).anyMatch(tagValueList -> tagValueList.contains(TagValue.create("WARN")));
        });
    }

    @Test
    void logWarnAndErrorMessage() throws Exception {
        log.warn("Warning Message");
        log.error("Error Message");

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            ViewData logCounterView = viewManager.getView(View.Name.create("inspectit/self/logs"));
            assertThat(logCounterView).isNotNull();
            Map<List<TagValue>, AggregationData> aggregationMap = logCounterView.getAggregationMap();
            assertThat(aggregationMap).isNotNull().isNotEmpty();
            assertThat(aggregationMap.keySet()).anyMatch(tagValueList -> tagValueList.contains(TagValue.create("WARN")));
            assertThat(aggregationMap.keySet()).anyMatch(tagValueList -> tagValueList.contains(TagValue.create("ERROR")));
        });
    }
}