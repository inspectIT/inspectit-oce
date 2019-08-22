package rocks.inspectit.ocelot.core.selfmonitoring;

import io.opencensus.stats.*;
import io.opencensus.tags.TagValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.logging.logback.LogbackInitializer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class LogMetricsAppenderIntTest extends SpringTestBase {

    @Autowired
    ViewManager viewManager;

    Logger log = LoggerFactory.getLogger(LogMetricsAppenderIntTest.class);

    @Autowired
    InspectitEnvironment environment;


    @BeforeEach
    private void reset(){
        LogbackInitializer.initLogging(environment.getCurrentConfig());
        Stats.setState(StatsCollectionState.DISABLED);
        Stats.setState(StatsCollectionState.ENABLED);
    }

    @Test
    void logInfoMessage() throws Exception {
        log.info("Info Message");

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            ViewData logCounterView = viewManager.getView(View.Name.create("inspectit/self/logs"));
            assertThat(logCounterView).isNotNull();
            Map<List<TagValue>, AggregationData> aggregationMap = logCounterView.getAggregationMap();
            assertThat(aggregationMap).isNotNull().isNotEmpty();
            assertThat(aggregationMap.keySet()).anyMatch(tagValueList -> tagValueList.contains(TagValue.create("INFO")));
            assertThat(getCountForLogLevel("WARN", aggregationMap)).isEqualTo(0);
            assertThat(getCountForLogLevel("ERROR", aggregationMap)).isEqualTo(0);
            assertThat(getCountForLogLevel("INFO", aggregationMap)).isEqualTo(1);
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
            assertThat(getCountForLogLevel("WARN", aggregationMap)).isEqualTo(0);
            assertThat(getCountForLogLevel("ERROR", aggregationMap)).isEqualTo(1);
            assertThat(getCountForLogLevel("INFO", aggregationMap)).isEqualTo(0);
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
            assertThat(getCountForLogLevel("WARN", aggregationMap)).isEqualTo(1);
            assertThat(getCountForLogLevel("ERROR", aggregationMap)).isEqualTo(0);
            assertThat(getCountForLogLevel("INFO", aggregationMap)).isEqualTo(0);
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
            assertThat(getCountForLogLevel("WARN", aggregationMap)).isEqualTo(1);
            assertThat(getCountForLogLevel("ERROR", aggregationMap)).isEqualTo(1);
            assertThat(getCountForLogLevel("INFO", aggregationMap)).isEqualTo(0);
        });
    }

    /**
     * Returns log count of given log level
     * @param logLevel
     * @param aggregationDataMap
     * @return the count of the log level
     */
    private long getCountForLogLevel(String logLevel, Map<List<TagValue>, AggregationData> aggregationDataMap){
        for(Map.Entry<List<TagValue>, AggregationData> entry: aggregationDataMap.entrySet()){
            if(entry.getKey().contains(TagValue.create(logLevel))){
                return ((AggregationData.SumDataLong) entry.getValue()).getSum();
            }
        }
        return 0;
    }
}