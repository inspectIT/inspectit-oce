package rocks.inspectit.ocelot.instrumentation.servicegraph;

import io.opencensus.stats.AggregationData;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.context.IInspectitContext;
import rocks.inspectit.ocelot.utils.TestUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * uses global-propagation-tests.yml
 */
public class ServiceInMetricTest {

    public static final int PORT = 9999;
    public static final String TEST_PATH = "/test";
    public static final String TEST_URL = "http://localhost:" + PORT + TEST_PATH;

    public static final String SERVICE_NAME = "systemtest";

    private Server server;

    void fireRequest(String originService) {

        try {
            IInspectitContext context = Instances.contextManager.enterNewContext();
            context.setData("prop_origin_service", originService);
            context.makeActive();

            HttpURLConnection urlConnection = (HttpURLConnection) new URL(TEST_URL).openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.getResponseCode();
            urlConnection.disconnect();

            context.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class TestServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setStatus(200);
        }

    }

    @Nested
    class ServletAPI {

        @Test
        void testInternalCallRecording() throws Exception {

            server = new Server(PORT);
            ServletHandler servletHandler = new ServletHandler();
            server.setHandler(servletHandler);
            servletHandler.addServletWithMapping(TestServlet.class, "/*");
            server.start();
            HttpURLConnection.class.getDeclaredMethods();
            TestUtils.waitForInstrumentationToComplete();

            fireRequest("servlet_origin");
            server.stop();


            Map<String, String> tags = new HashMap<>();
            tags.put("protocol", "http");
            tags.put("service", SERVICE_NAME);
            tags.put("origin_service", "servlet_origin");

            long cnt = ((AggregationData.CountData) TestUtils.getDataForView("service/in/count", tags)).getCount();
            double respSum = ((AggregationData.SumDataDouble) TestUtils.getDataForView("service/in/responsetime/sum", tags)).getSum();

            assertThat(cnt).isEqualTo(1);
            assertThat(respSum).isGreaterThan(0);
        }

    }

}
