package rocks.inspectit.ocelot.core.instrumentation.hook.actions;

import io.opencensus.stats.MeasureMap;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.tags.*;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.model.MetricAccessor;
import rocks.inspectit.ocelot.core.metrics.MeasuresAndViewsManager;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Hook action responsible for recording measurements at the exit of an instrumented method
 */
@Value
@Slf4j
public class MetricsRecorder implements IHookAction {

    /**
     * A list of metric accessors which will be used to find the value and tags for the metric.
     */
    private final List<MetricAccessor> metrics;

    /**
     * A map of variable accessors which are used to resolve tag values. The key represents the name of the data key
     * used by the variable accessor.
     */
    private Map<String, VariableAccessor> tagAccessors;

    /**
     * Common tags manager needed for gathering common tags when recording metrics.
     */
    private CommonTagsManager commonTagsManager;

    /**
     * The manager to acquire the actual OpenCensus metrics from
     */
    private MeasuresAndViewsManager metricsManager;

    /**
     * The manager to acquire the actual OpenCensus metrics from
     */
    private StatsRecorder statsRecorder;

    @Override
    public void execute(ExecutionContext context) {
        // then iterate all metrics and enter new scope for metric collection
        for (MetricAccessor metricAccessor : metrics) {
            Object value = metricAccessor.getVariableAccessor().get(context);
            if (value != null) {
                if (value instanceof Number) {
                    // only record metrics where a value is present
                    // this allows to disable the recording of a metric depending on the results of action executions
                    MeasureMap measureMap = statsRecorder.newMeasureMap();
                    metricsManager.tryRecordingMeasurement(metricAccessor.getName(), measureMap, (Number) value);
                    TagContext tagContext = getTagContext(context, metricAccessor);
                    measureMap.record(tagContext);
                }
            }
        }
    }

    private TagContext getTagContext(ExecutionContext context, MetricAccessor metricAccessor) {
        InspectitContextImpl inspectitContext = context.getInspectitContext();

        // create builder
        TagContextBuilder builder = Tags.getTagger().emptyBuilder();

        // first common tags to allow overwrite by constant or data tags
        commonTagsManager.getCommonTagKeys()
                .forEach(commonTagKey -> Optional.ofNullable(inspectitContext.getData(commonTagKey.getName()))
                        .ifPresent(value -> builder.putLocal(commonTagKey, TagValue.create(value.toString())))
                );

        // then constant tags to allow overwrite by data
        metricAccessor.getConstantTags()
                .forEach((key, value) -> builder.putLocal(TagKey.create(key), TagValue.create(value)));

        // go over data tags and match the value to the key from the contextTags (if available)
        metricAccessor.getDataTags().entrySet().stream()
                .filter(entry -> tagAccessors.containsKey(entry.getValue()))
                .forEach(entry -> getTagValue(context, entry.getValue())
                        .ifPresent(value -> builder.putLocal(TagKey.create(entry.getKey()), TagValue.create(value.toString())))
                );

        // build and return
        return builder.build();
    }

    private Optional<Object> getTagValue(ExecutionContext context, String dataKey) {
        return Optional.ofNullable(tagAccessors.get(dataKey).get(context));
    }

    @Override
    public String getName() {
        return "Metrics Recorder";
    }
}
