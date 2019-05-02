package rocks.inspectit.ocelot.core.exporter;

import com.expedia.open.tracing.Span;
import com.expedia.open.tracing.agent.api.DispatchResult;
import com.expedia.open.tracing.agent.api.SpanAgentGrpc;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.samplers.Samplers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.core.SpringTestBase;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;

@TestPropertySource(properties = {
        "inspectit.exporters.tracing.haystack.enabled=true",
        "inspectit.exporters.tracing.haystack.host=localhost"
})
@DirtiesContext
public class HaystackExporterServiceIntTest extends SpringTestBase {

    public static final String HOST = "localhost";
    public static final int PORT = 35000;
    public static final String SPAN_NAME = "haystackspan";
    private static Server agent;
    private static FakeGRPCSpanAgentImpl fakeGRPCSpanAgent = new FakeGRPCSpanAgentImpl();

    @BeforeAll
    public static void setUp() {
        agent = getServer("localhost:35000" ,fakeGRPCSpanAgent);
    }

    @AfterAll
    public static void tearDown() {
        agent.shutdown();
    }

    /**
     * To test the client, a fake GRPC server servers the fake class implementation {@link FakeGRPCSpanAgentImpl}.
     */
    @Test
    public void testGrpcRequest() {
        try {
            agent.start();

            Tracing.getTracer().spanBuilder(SPAN_NAME)
                    .setSampler(Samplers.alwaysSample())
                    .startSpanAndRun(() -> {
                    });

            Thread.sleep(15000);
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
        assertEquals(1, fakeGRPCSpanAgent.getSpans().size());
        assertEquals(SPAN_NAME, fakeGRPCSpanAgent.getSpans().get(0).getOperationName());
    }

    private static Server getServer(String endPoint, BindableService service) {
        ServerBuilder<?> builder = NettyServerBuilder.forAddress(new InetSocketAddress(HOST, PORT));
        Executor executor = MoreExecutors.directExecutor();
        builder.executor(executor);
        return builder.addService(service).build();
    }

    static class FakeGRPCSpanAgentImpl extends SpanAgentGrpc.SpanAgentImplBase{

        /**
         * List of received spans
         */
        private ArrayList<Span> spans = new ArrayList<Span>();

        @Override
        public void dispatch(Span request, StreamObserver<DispatchResult> responseObserver) {
            spans.add(request);
        }

        public ArrayList<Span> getSpans(){
            return spans;
        }
    }

}
