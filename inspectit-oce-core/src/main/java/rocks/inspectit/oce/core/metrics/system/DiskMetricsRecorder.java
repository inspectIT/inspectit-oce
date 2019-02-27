package rocks.inspectit.oce.core.metrics.system;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import rocks.inspectit.oce.core.config.model.metrics.MetricsSettings;

import java.io.File;
import java.time.Duration;

@Service
@Slf4j
public class DiskMetricsRecorder extends AbstractPollingMetricsRecorder {


    private static final String METRIC_NAME_PREFIX = "disk/";
    private static final String FREE_METRIC_NAME = "free";
    private static final String TOTAL_METRIC_NAME = "total";

    public DiskMetricsRecorder() {
        super("metrics.disk");
    }

    @Override
    protected boolean checkEnabledForConfig(MetricsSettings conf) {
        return conf.getDisk().getEnabled().containsValue(true);
    }

    @Override
    protected Duration getFrequency(MetricsSettings config) {
        return config.getDisk().getFrequency();
    }

    @Override
    protected void takeMeasurement(MetricsSettings config) {
        val mm = recorder.newMeasureMap();
        val disk = config.getDisk();
        if (disk.getEnabled().getOrDefault(FREE_METRIC_NAME, false)) {
            measureManager.getMeasureLong(METRIC_NAME_PREFIX + FREE_METRIC_NAME)
                    .ifPresent(measure -> mm.put(measure, new File("/").getFreeSpace()));
        }
        if (disk.getEnabled().getOrDefault(TOTAL_METRIC_NAME, false)) {
            measureManager.getMeasureLong(METRIC_NAME_PREFIX + TOTAL_METRIC_NAME)
                    .ifPresent(measure -> mm.put(measure, new File("/").getTotalSpace()));
        }
        mm.record();
    }
}
