package de.zalando.mass.ratwrap.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.zalando.mass.ratwrap.TestApplication;
import de.zalando.mass.ratwrap.util.TestUtils;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.zalando.problem.Problem;
import org.zalando.problem.ThrowableProblem;
import ratpack.http.client.ReceivedResponse;
import ratpack.server.RatpackServer;
import ratpack.test.ApplicationUnderTest;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
public class ExceptionHandlingTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandlingTest.class);

    @Autowired
    private RatpackServer server;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    private void testForCode(int code) throws Exception {
        ApplicationUnderTest.of(server)
                .test(testHttpClient -> {
                    final ReceivedResponse response = testHttpClient.get("/errors/"+code);
                    TestUtils.logResponse(LOGGER, response, "/errors/"+code);
                    assertEquals(code, response.getStatusCode());
                    assertEquals("application/problem+json", response.getHeaders().get(HttpHeaderNames.CONTENT_TYPE));
                    final Problem problem = objectMapper.readValue(response.getBody().getInputStream(), Problem.class);
                    final ThrowableProblem expectedProblem = Problem.valueOf(Response.Status.fromStatusCode(code));
                    assertEquals(expectedProblem.getStatus(), problem.getStatus());

                });
    }

    @Test
    public void test404() throws Exception {
        testForCode(404);
    }

    @Test
    public void test500() throws Exception {
        testForCode(500);
    }

}