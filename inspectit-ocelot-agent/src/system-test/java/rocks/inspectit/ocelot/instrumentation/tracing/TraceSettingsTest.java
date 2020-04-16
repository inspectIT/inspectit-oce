package rocks.inspectit.ocelot.instrumentation.tracing;

import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Status;
import io.opencensus.trace.export.SpanData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TraceSettingsTest extends TraceTestBase {


    String attributesSetter() {
        return "Hello A!";
    }

    @Test
    void testAttributeWritingToParentSpan() {

        TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, 15, TimeUnit.SECONDS);
        attributesSetter();

        assertTraceExported((spans) ->
                assertThat(spans)
                        .hasSize(1)
                        .anySatisfy((sp) -> {
                            assertThat(sp.getName()).endsWith("TraceSettingsTest.attributesSetter");
                            assertThat(sp.getAttributes().getAttributeMap())
                                    .hasSize(7)
                                    .containsEntry("entry", AttributeValue.stringAttributeValue("const"))
                                    .containsEntry("exit", AttributeValue.stringAttributeValue("Hello A!"))
                                    .containsEntry("toObfuscate", AttributeValue.stringAttributeValue("***"))
                                    .containsEntry("anything", AttributeValue.stringAttributeValue("***"))
                                    // plus include all common tags (service + key validation only)
                                    .containsEntry("service", AttributeValue.stringAttributeValue("systemtest"))
                                    .containsKeys("host", "host_address");
                        })

        );

    }

    String attributesSetterWithConditions(boolean captureAttributes) {
        return "Hello B!";
    }

    @Test
    void testConditionalAttributeWriting() {

        TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, 15, TimeUnit.SECONDS);
        attributesSetterWithConditions(false);
        attributesSetterWithConditions(true);

        assertTraceExported((spans) ->
                assertThat(spans)
                        .hasSize(1)
                        .anySatisfy((sp) -> {
                            assertThat(sp.getName()).endsWith("TraceSettingsTest.attributesSetterWithConditions");
                            assertThat(sp.getAttributes().getAttributeMap())
                                    .hasSize(3)
                                    .containsKeys("service", "host", "host_address");
                        })

        );

        assertTraceExported((spans) ->
                assertThat(spans)
                        .hasSize(1)
                        .anySatisfy((sp) -> {
                            assertThat(sp.getName()).endsWith("TraceSettingsTest.attributesSetterWithConditions");
                            assertThat(sp.getAttributes().getAttributeMap())
                                    .hasSize(5)
                                    .containsEntry("entry", AttributeValue.stringAttributeValue("const"))
                                    .containsEntry("exit", AttributeValue.stringAttributeValue("Hello B!"))
                                    .containsKeys("service", "host", "host_address");
                        })

        );

    }


    void conditionalRoot(boolean startSpan) {
        nestedC();
    }

    void nestedC() {
    }

    @Test
    void testConditionalSpanCreation() {

        TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, 15, TimeUnit.SECONDS);
        conditionalRoot(false);
        conditionalRoot(true);

        assertTraceExported((spans) ->
                assertThat(spans)
                        .hasSize(1)
                        .anySatisfy((sp) -> {
                            assertThat(sp.getName()).endsWith("TraceSettingsTest.nestedC");
                            assertThat(sp.getParentSpanId()).isNull();
                        })

        );
        assertTraceExported((spans) ->
                assertThat(spans)
                        .hasSize(2)
                        .anySatisfy((sp) -> {
                            assertThat(sp.getName()).endsWith("TraceSettingsTest.conditionalRoot");
                            assertThat(sp.getParentSpanId()).isNull();
                        })
                        .anySatisfy((sp) -> {
                            assertThat(sp.getName()).endsWith("TraceSettingsTest.nestedC");
                            assertThat(sp.getParentSpanId()).isNotNull();
                        })

        );

    }


    void namedA(String name) {
        namedB("second");
    }

    void namedB(String name) {
    }

    @Test
    void testSpanNameCustomization() {

        TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, 15, TimeUnit.SECONDS);
        namedA("first");

        assertTraceExported((spans) ->
                assertThat(spans)
                        .hasSize(2)
                        .anySatisfy((sp) -> {
                            assertThat(sp.getName()).isEqualTo("first");
                            assertThat(sp.getParentSpanId()).isNull();
                        })
                        .anySatisfy((sp) -> {
                            assertThat(sp.getName()).isEqualTo("second");
                            assertThat(sp.getParentSpanId()).isNotNull();
                        })

        );

    }

    @Test
    void testNoCommonTagsOnChild() {
        TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, 15, TimeUnit.SECONDS);

        namedA("whatever");

        assertTraceExported((spans) ->
                assertThat(spans)
                        .hasSize(2)
                        .anySatisfy((sp) -> {
                            assertThat(sp.getParentSpanId()).isNull();
                            assertThat(sp.getAttributes().getAttributeMap()).hasSize(3);
                        })
                        .anySatisfy((sp) -> {
                            assertThat(sp.getParentSpanId()).isNotNull();
                            assertThat(sp.getAttributes().getAttributeMap()).hasSize(0);
                        })

        );

    }

    static class AsyncTask {
        void doAsync(String att1, String att2, String att3, boolean isFinished) {
        }
    }

    @Test
    void testInterleavedAsyncSpans() throws Exception {

        TestUtils.waitForClassInstrumentation(AsyncTask.class, 15, TimeUnit.SECONDS);

        //all method calls of each task will result in a single span
        AsyncTask first = new AsyncTask();
        AsyncTask second = new AsyncTask();

        //interleave the asynchronous tasks and check that the
        first.doAsync("a1", null, null, false);
        Thread.sleep(10);
        second.doAsync("b1", null, null, false);
        second.doAsync(null, "b2", null, false);
        Thread.sleep(10);
        first.doAsync(null, "a2", null, true);
        Thread.sleep(10);
        second.doAsync(null, null, "b3", true);

        assertSpansExported(spans -> {
            List<SpanData> asyncSpans = spans.stream()
                    .filter(s -> s.getName().equals("AsyncTask.doAsync"))
                    .collect(Collectors.toList());
            assertThat(asyncSpans).hasSize(2);

            SpanData firstSpan = asyncSpans.get(0);
            SpanData secondSpan = asyncSpans.get(1);

            //order the spans by time
            if (secondSpan.getStartTimestamp().compareTo(secondSpan.getStartTimestamp()) < 0) {
                SpanData temp = firstSpan;
                firstSpan = secondSpan;
                secondSpan = temp;
            }

            //ensure that all method invocations have been combined to single spans
            assertThat(firstSpan.getAttributes().getAttributeMap())
                    .hasSize(5)
                    .containsEntry("1", AttributeValue.stringAttributeValue("a1"))
                    .containsEntry("2", AttributeValue.stringAttributeValue("a2"))
                    .containsKeys("service", "host", "host_address");
            assertThat(secondSpan.getAttributes().getAttributeMap())
                    .hasSize(6)
                    .containsEntry("1", AttributeValue.stringAttributeValue("b1"))
                    .containsEntry("2", AttributeValue.stringAttributeValue("b2"))
                    .containsEntry("3", AttributeValue.stringAttributeValue("b3"))
                    .containsKeys("service", "host", "host_address");

            //ensure that the timings are valid
            assertThat(firstSpan.getEndTimestamp()).isLessThan(secondSpan.getEndTimestamp());
            assertThat(secondSpan.getStartTimestamp()).isLessThan(firstSpan.getEndTimestamp());
        });
    }

    void samplingTestEndMarker(String id) {
    }

    void fixedSamplingRateTest(String id) {
    }

    void dynamicSamplingRateTest(String id, Object rate) {
    }

    void nestedSamplingTestRoot(Double rootProbability, Double nestedProbability) {
        nestedSamplingTestNested(nestedProbability);
        nestedSamplingTestNestedDefault();
    }

    void nestedSamplingTestNested(Double nestedProbability) {
    }

    /**
     * Runs with the default sample probability
     */
    void nestedSamplingTestNestedDefault() {
    }

    @Nested
    class Sampling {

        @Test
        void testFixedSpanSamplingRate() {
            TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, 15, TimeUnit.SECONDS);
            for (int i = 0; i < 10000; i++) {
                fixedSamplingRateTest("fixed");
            }
            samplingTestEndMarker("fixed_end");

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) ->
                    assertThat(spans)
                            .anySatisfy((sp) -> {
                                assertThat(sp.getName()).isEqualTo("fixed_end");
                            })
            );

            long numSpans = exportedSpans.stream().filter(sp -> sp.getName().equals("fixed")).count();
            //the number of spans lies with a probability greater than 99.999% +-300 around the mean of 0.5 * 10000
            assertThat(numSpans).isGreaterThan(4700).isLessThan(5300);
        }


        @Test
        void dynamicSampleRate_low() {
            TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, 15, TimeUnit.SECONDS);
            for (int i = 0; i < 10000; i++) {
                dynamicSamplingRateTest("dynamic_0.2", 0.2);
            }
            samplingTestEndMarker("dynamic_end");

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) ->
                    assertThat(spans)
                            .anySatisfy((sp) -> {
                                assertThat(sp.getName()).isEqualTo("dynamic_end");
                            })
            );

            //the number of spans lies with a probability greater than 99.999% +-300 around the mean of 0.2 * 10000
            long numSpans02 = exportedSpans.stream().filter(sp -> sp.getName().equals("dynamic_0.2")).count();
            assertThat(numSpans02).isGreaterThan(1700).isLessThan(2300);
        }

        @Test
        void dynamicSampleRate_high() {
            TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, 15, TimeUnit.SECONDS);
            for (int i = 0; i < 10000; i++) {
                dynamicSamplingRateTest("dynamic_0.7", 0.7);
            }
            samplingTestEndMarker("dynamic_end");

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) ->
                    assertThat(spans)
                            .anySatisfy((sp) -> {
                                assertThat(sp.getName()).isEqualTo("dynamic_end");
                            })
            );

            //the number of spans lies with a probability greater than 99.999% +-300 around the mean of 0.7 * 10000
            long numSpans07 = exportedSpans.stream().filter(sp -> sp.getName().equals("dynamic_0.7")).count();
            assertThat(numSpans07).isGreaterThan(6700).isLessThan(7300);
        }


        @Test
        void dynamicSampleRate_invalidRate() {
            TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, 15, TimeUnit.SECONDS);
            for (int i = 0; i < 10000; i++) {
                dynamicSamplingRateTest("invalid", "not a number! haha!");
            }
            samplingTestEndMarker("dynamic_end");

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) ->
                    assertThat(spans)
                            .anySatisfy((sp) -> {
                                assertThat(sp.getName()).isEqualTo("dynamic_end");
                            })
            );

            //ensure that an invalid probability is equal to "never sample"
            long numSpansInvalid = exportedSpans.stream().filter(sp -> sp.getName().equals("invalid")).count();
            assertThat(numSpansInvalid).isEqualTo(10000L);
        }


        @Test
        void dynamicSampleRate_null() {
            TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, 15, TimeUnit.SECONDS);
            for (int i = 0; i < 10000; i++) {
                dynamicSamplingRateTest("null", null);
            }
            samplingTestEndMarker("dynamic_end");

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) ->
                    assertThat(spans)
                            .anySatisfy((sp) -> {
                                assertThat(sp.getName()).isEqualTo("dynamic_end");
                            })
            );

            //ensure that an invalid probability is equal to "never sample"
            long numSpansNull = exportedSpans.stream().filter(sp -> sp.getName().equals("null")).count();
            assertThat(numSpansNull).isEqualTo(10000L);
        }

        @Test
        void testNestedZeroSamplingProbability() {
            TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, 15, TimeUnit.SECONDS);

            nestedSamplingTestRoot(1.0, 0.0);

            samplingTestEndMarker("nested_zero_end");

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) ->
                    assertThat(spans)
                            .anySatisfy((sp) -> {
                                assertThat(sp.getName()).isEqualTo("nested_zero_end");
                            })
            );

            assertTraceExported((spans) ->
                    assertThat(spans)
                            .hasSize(3)
                            .anySatisfy((sp) -> {
                                assertThat(sp.getName()).isEqualTo("TraceSettingsTest.nestedSamplingTestRoot");
                                assertThat(sp.getParentSpanId()).isNull();
                            })
                            .anySatisfy((sp) -> {
                                assertThat(sp.getName()).isEqualTo("TraceSettingsTest.nestedSamplingTestNested");
                                assertThat(sp.getParentSpanId()).isNotNull();
                            })
                            .anySatisfy((sp) -> {
                                assertThat(sp.getName()).isEqualTo("TraceSettingsTest.nestedSamplingTestNestedDefault");
                                assertThat(sp.getParentSpanId()).isNotNull();
                            })

            );
        }

        @Test
        void testNestedOneSamplingProbability() {
            TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, 15, TimeUnit.SECONDS);

            nestedSamplingTestRoot(0.0, 1.0);

            samplingTestEndMarker("nested_one_end");

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) ->
                    assertThat(spans)
                            .anySatisfy((sp) -> {
                                assertThat(sp.getName()).isEqualTo("nested_one_end");
                            })
            );

            assertThat(exportedSpans)
                    .noneSatisfy(sp -> assertThat(sp.getName()).isEqualTo("TraceSettingsTest.nestedSamplingTestRoot"))
                    .noneSatisfy(sp -> assertThat(sp.getName()).isEqualTo("TraceSettingsTest.nestedSamplingTestNestedDefault"))
                    .anySatisfy(sp -> assertThat(sp.getName()).isEqualTo("TraceSettingsTest.nestedSamplingTestNested"));
        }


        @Test
        void testNestedNullSamplingProbability() {
            TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, 15, TimeUnit.SECONDS);

            nestedSamplingTestRoot(0.0, null);

            samplingTestEndMarker("nested_null_end");

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) ->
                    assertThat(spans)
                            .anySatisfy((sp) -> {
                                assertThat(sp.getName()).isEqualTo("nested_null_end");
                            })
            );

            assertThat(exportedSpans)
                    .noneSatisfy(sp -> assertThat(sp.getName()).isEqualTo("TraceSettingsTest.nestedSamplingTestRoot"))
                    .noneSatisfy(sp -> assertThat(sp.getName()).isEqualTo("TraceSettingsTest.nestedSamplingTestNestedDefault"))
                    .noneSatisfy(sp -> assertThat(sp.getName()).isEqualTo("TraceSettingsTest.nestedSamplingTestNested"));
        }

    }

    void withErrorStatus(Object status) {
    }

    void withoutErrorStatus() {
    }

    @Nested
    class ErrorStatus {

        @BeforeEach
        void waitForInstrumentation() {
            TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, 15, TimeUnit.SECONDS);
        }

        @Test
        void testWithoutErrorStatus() {
            withoutErrorStatus();

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) ->
                    assertThat(spans)
                            .hasSize(1)
                            .anySatisfy((sp) -> {
                                assertThat(sp.getName()).isEqualTo("TraceSettingsTest.withoutErrorStatus");
                                assertThat(sp.getStatus()).isEqualTo(Status.OK);
                            })
            );
        }

        @Test
        void testNullErrorStatus() {
            withErrorStatus(null);

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) ->
                    assertThat(spans)
                            .hasSize(1)
                            .anySatisfy((sp) -> {
                                assertThat(sp.getName()).isEqualTo("TraceSettingsTest.withErrorStatus");
                                assertThat(sp.getStatus()).isEqualTo(Status.OK);
                            })
            );
        }

        @Test
        void testFalseErrorStatus() {
            withErrorStatus(false);

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) ->
                    assertThat(spans)
                            .hasSize(1)
                            .anySatisfy((sp) -> {
                                assertThat(sp.getName()).isEqualTo("TraceSettingsTest.withErrorStatus");
                                assertThat(sp.getStatus()).isEqualTo(Status.OK);
                            })
            );
        }


        @Test
        void testNonNullErrorStatus() {
            withErrorStatus("foo");

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) ->
                    assertThat(spans)
                            .hasSize(1)
                            .anySatisfy((sp) -> {
                                assertThat(sp.getName()).isEqualTo("TraceSettingsTest.withErrorStatus");
                                assertThat(sp.getStatus()).isEqualTo(Status.UNKNOWN);
                            })
            );
        }
    }

}

