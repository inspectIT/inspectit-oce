package rocks.inspectit.ocelot.core.metrics.percentiles;

import io.opencensus.common.Scope;
import io.opencensus.metrics.LabelKey;
import io.opencensus.metrics.LabelValue;
import io.opencensus.metrics.export.Metric;
import io.opencensus.metrics.export.MetricDescriptor;
import io.opencensus.metrics.export.TimeSeries;
import io.opencensus.metrics.export.Value;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;

public class PercentileViewManagerTest {

    private PercentileViewManager viewManager;

    private Supplier<Long> clock;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void init() {
        clock = Mockito.mock(Supplier.class);
        lenient().doReturn(0L).when(clock).get();
        viewManager = new PercentileViewManager(clock);
        viewManager.init();
    }

    @AfterEach
    void destroy() {
        viewManager.destroy();
    }

    private void awaitMetricsProcessing() {
        await().until(() ->
                viewManager.worker.recordsQueue.isEmpty()
                        && viewManager.worker.worker.getState() == Thread.State.WAITING);
    }

    private void assertTotalSeriesCount(Collection<Metric> metrics, long expectedSeriesCount) {
        long count = metrics
                .stream()
                .flatMap(metric -> metric.getTimeSeriesList().stream())
                .count();
        assertThat(count).isEqualTo(expectedSeriesCount);
    }

    private void assertContainsMetric(Collection<Metric> metrics, String name, double value, String... tagKeyValuePairs) {
        assertThat(metrics)
                .anySatisfy(m -> assertThat(m.getMetricDescriptor().getName()).isEqualTo(name));
        Metric metric = metrics.stream()
                .filter(m -> m.getMetricDescriptor().getName().equals(name))
                .findFirst()
                .get();

        List<LabelKey> keys = metric.getMetricDescriptor().getLabelKeys();
        assertThat(keys).hasSize(tagKeyValuePairs.length / 2);
        List<LabelValue> values = new ArrayList<>();
        keys.forEach(label -> values.add(null));

        for (int i = 0; i < tagKeyValuePairs.length; i += 2) {
            LabelKey tagKey = LabelKey.create(tagKeyValuePairs[i], "");
            LabelValue tagValue = LabelValue.create(tagKeyValuePairs[i + 1]);
            assertThat(keys).contains(tagKey);
            values.set(keys.indexOf(tagKey), tagValue);
        }

        assertThat(metric.getTimeSeriesList())
                .anySatisfy(ts -> assertThat(ts.getLabelValues()).isEqualTo(values));
        TimeSeries ts = metric.getTimeSeriesList().stream()
                .filter(series -> series.getLabelValues().equals(values))
                .findFirst().get();

        assertThat(ts.getPoints()).hasSize(1);
        assertThat(ts.getPoints().get(0).getValue()).isEqualTo(Value.doubleValue(value));

    }

    @Nested
    class ComputeMetrics {

        @Test
        void testNoData() {
            viewManager.createOrUpdateView("my/measure", "my/view", "ms", "foo",
                    true, true, Arrays.asList(0.5, 0.95), 15000, Collections.emptyList());

            Collection<Metric> result = viewManager.computeMetrics();

            assertThat(result).hasSize(1);
            assertTotalSeriesCount(result, 0);
        }

        @Test
        void testWithData() {
            viewManager.createOrUpdateView("my/measure", "my/view", "ms", "foo",
                    true, true, Arrays.asList(0.5, 0.95), 15000, Collections.emptyList());

            for (int i = 1; i < 100; i++) {
                doReturn((long) i).when(clock).get();
                viewManager.recordMeasurement("my/measure", i);
            }
            awaitMetricsProcessing();

            doReturn(10000L).when(clock).get();
            Collection<Metric> result = viewManager.computeMetrics();

            assertThat(result).hasSize(1);
            assertTotalSeriesCount(result, 4);
            assertContainsMetric(result, "my/view", 1, "p", "min");
            assertContainsMetric(result, "my/view", 99, "p", "max");
            assertContainsMetric(result, "my/view", 50, "p", "0.5");
            assertContainsMetric(result, "my/view", 95, "p", "0.95");
        }

        @Test
        void testMultiSeriesData() {
            viewManager.createOrUpdateView("my/measure", "my/view", "ms", "foo",
                    true, true, Arrays.asList(0.5, 0.95), 15000, Arrays.asList("tag1", "tag2"));

            for (int i = 1; i < 100; i++) {
                doReturn((long) i).when(clock).get();
                viewManager.recordMeasurement("my/measure", i);
                try (Scope s = Tags.getTagger().emptyBuilder()
                        .putLocal(TagKey.create("tag1"), TagValue.create("foo"))
                        .putLocal(TagKey.create("tag2"), TagValue.create("bar")).buildScoped()) {
                    viewManager.recordMeasurement("my/measure", 1000 + i);
                }
            }
            awaitMetricsProcessing();

            doReturn(10000L).when(clock).get();
            Collection<Metric> result = viewManager.computeMetrics();

            assertThat(result).hasSize(1);
            assertTotalSeriesCount(result, 8);
            assertContainsMetric(result, "my/view", 1, "tag1", "", "tag2", "", "p", "min");
            assertContainsMetric(result, "my/view", 99, "tag1", "", "tag2", "", "p", "max");
            assertContainsMetric(result, "my/view", 50, "tag1", "", "tag2", "", "p", "0.5");
            assertContainsMetric(result, "my/view", 95, "tag1", "", "tag2", "", "p", "0.95");
            assertContainsMetric(result, "my/view", 1001, "tag1", "foo", "tag2", "bar", "p", "min");
            assertContainsMetric(result, "my/view", 1099, "tag1", "foo", "tag2", "bar", "p", "max");
            assertContainsMetric(result, "my/view", 1050, "tag1", "foo", "tag2", "bar", "p", "0.5");
            assertContainsMetric(result, "my/view", 1095, "tag1", "foo", "tag2", "bar", "p", "0.95");
        }

        @Test
        void testMultipleViewsForSameMeasure() {
            viewManager.createOrUpdateView("my/measure", "viewA", "ms", "foo",
                    true, false, Collections.emptyList(), 1, Collections.emptyList());
            viewManager.createOrUpdateView("my/measure", "viewB", "ms", "foo",
                    true, false, Collections.emptyList(), 1, Arrays.asList("tag1"));

            viewManager.recordMeasurement("my/measure", 1);
            awaitMetricsProcessing();

            Collection<Metric> result = viewManager.computeMetrics();

            assertThat(result).hasSize(2);
            assertTotalSeriesCount(result, 2);
            assertContainsMetric(result, "viewA", 1, "p", "min");
            assertContainsMetric(result, "viewB", 1, "tag1", "", "p", "min");
        }

        @Test
        void testWithStaleData() {
            viewManager.createOrUpdateView("my/measure", "my/view", "ms", "foo",
                    true, true, Arrays.asList(0.5, 0.95), 15000, Collections.emptyList());

            for (int i = 1; i < 100; i++) {
                doReturn((long) i).when(clock).get();
                viewManager.recordMeasurement("my/measure", i);
            }
            awaitMetricsProcessing();
            doReturn(20000L).when(clock).get();

            Collection<Metric> result = viewManager.computeMetrics();

            assertThat(result).hasSize(1);
            assertTotalSeriesCount(result, 0);
        }
    }

    @Nested
    class CreateOrUpdateView {

        @Test
        void updateMetricDescription() {
            viewManager.createOrUpdateView("my/measure", "my/view", "ms", "foo",
                    true, false, Collections.emptyList(), 1, Collections.emptyList());
            viewManager.createOrUpdateView("my/measure", "my/view", "s", "bar",
                    true, false, Collections.emptyList(), 1, Collections.emptyList());

            viewManager.recordMeasurement("my/measure", 42);
            awaitMetricsProcessing();

            Collection<Metric> result = viewManager.computeMetrics();
            assertThat(result).hasSize(1);
            MetricDescriptor md = result.iterator().next().getMetricDescriptor();

            assertThat(md.getName()).isEqualTo("my/view");
            assertThat(md.getUnit()).isEqualTo("s");
            assertThat(md.getDescription()).isEqualTo("bar");
        }

        @Test
        void updateMinMaxPercentiles() {
            viewManager.createOrUpdateView("my/measure", "my/view", "ms", "foo",
                    true, false, Collections.emptyList(), 1, Collections.emptyList());
            viewManager.createOrUpdateView("my/measure", "my/view", "s", "bar",
                    false, true, Arrays.asList(0.5), 1, Collections.emptyList());

            viewManager.recordMeasurement("my/measure", 42);
            awaitMetricsProcessing();

            Collection<Metric> result = viewManager.computeMetrics();
            assertThat(result).hasSize(1);
            assertContainsMetric(result, "my/view", 42, "p", "max");
            assertContainsMetric(result, "my/view", 42, "p", "0.5");
        }

        @Test
        void updateTimeWindow() {
            viewManager.createOrUpdateView("my/measure", "my/view", "ms", "foo",
                    true, false, Collections.emptyList(), 1, Collections.emptyList());
            viewManager.createOrUpdateView("my/measure", "my/view", "ms", "foo",
                    true, false, Collections.emptyList(), 100, Collections.emptyList());

            doReturn(0L).when(clock).get();
            viewManager.recordMeasurement("my/measure", 42);
            awaitMetricsProcessing();

            doReturn(99L).when(clock).get();
            Collection<Metric> result = viewManager.computeMetrics();
            assertThat(result).hasSize(1);
            assertContainsMetric(result, "my/view", 42, "p", "min");
        }

        @Test
        void updateTags() {
            viewManager.createOrUpdateView("my/measure", "my/view", "ms", "foo",
                    true, false, Collections.emptyList(), 1, Collections.emptyList());
            viewManager.createOrUpdateView("my/measure", "my/view", "ms", "foo",
                    true, false, Collections.emptyList(), 1, Arrays.asList("tag1", "tag2"));

            try (Scope s = Tags.getTagger().emptyBuilder()
                    .putLocal(TagKey.create("tag1"), TagValue.create("foo"))
                    .putLocal(TagKey.create("tag2"), TagValue.create("bar")).buildScoped()) {
                viewManager.recordMeasurement("my/measure", 42);
            }
            awaitMetricsProcessing();

            Collection<Metric> result = viewManager.computeMetrics();
            assertThat(result).hasSize(1);
            assertContainsMetric(result, "my/view", 42, "tag1", "foo", "tag2", "bar", "p", "min");
        }
    }

    @Nested
    class RemoveView {

        @Test
        void checkViewRemoved() {
            viewManager.createOrUpdateView("my/measure", "my/view", "ms", "foo",
                    true, false, Collections.emptyList(), 100, Collections.emptyList());

            viewManager.recordMeasurement("my/measure", 42);
            awaitMetricsProcessing();

            boolean removed = viewManager.removeView("my/measure", "my/view");

            Collection<Metric> result = viewManager.computeMetrics();
            assertThat(viewManager.areAnyViewsRegisteredForMeasure("my/measure")).isFalse();
            assertThat(removed).isTrue();
            assertThat(result).isEmpty();

        }

        @Test
        void removeNonExistingView() {
            boolean removed = viewManager.removeView("my/measure", "my/view");
            assertThat(removed).isFalse();
        }

    }
}
