package rocks.inspectit.ocelot.core.selfmonitoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Logback recorder which exposes the the counts to the {@link SelfMonitoringService}
 */
@Component
public class LogMetricsRecorder {
    @Autowired
    SelfMonitoringService selfMonitoringService;

    /**
     * Records the increment of the number of metrics.
     * @param increment
     */
    public void increment(String logLevel, long increment) {
        Map<String, String> customTags = new HashMap<>();
        customTags.put("level", logLevel);
        selfMonitoringService.recordMeasurement("logs",increment, customTags);
    }

    @PostConstruct
    private void subscribe(){
        LogMetricsAppender.registerRecorder(this);
    }
}