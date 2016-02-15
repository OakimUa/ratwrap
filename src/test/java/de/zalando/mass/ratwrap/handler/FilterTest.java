package de.zalando.mass.ratwrap.handler;

import de.zalando.mass.ratwrap.TestApplication;
import de.zalando.mass.ratwrap.util.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ratpack.http.client.ReceivedResponse;
import ratpack.server.RatpackServer;
import ratpack.test.ApplicationUnderTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
public class FilterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilterTest.class);

    @Autowired
    private RatpackServer server;

    @Before
    public void setUp() throws Exception {
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testFilterSuccess() throws Exception {
        ApplicationUnderTest.of(server)
                .test(testHttpClient -> {
                    final ReceivedResponse response = testHttpClient
                            .requestSpec(requestSpec -> {})
                            .get("/filtered/test");
                    TestUtils.logResponse(LOGGER, response, "/filtered/test");
                    assertEquals(HttpStatus.OK.value(), response.getStatusCode());
                    assertEquals("OK", response.getBody().getText());
                    assertEquals("done", response.getHeaders().get("x-request-filter"));
                    assertEquals("done", response.getHeaders().get("x-common-filter"));
                });
    }

    @Test
    public void testFilterForbidCommon() throws Exception {
        ApplicationUnderTest.of(server)
                .test(testHttpClient -> {
                    final ReceivedResponse response = testHttpClient
                            .requestSpec(requestSpec -> requestSpec.getHeaders().add("x-block-in-common-filter", "block"))
                            .get("/filtered/test");
                    TestUtils.logResponse(LOGGER, response, "/filtered/test");
                    assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode());
                    assertEquals("done", response.getHeaders().get("x-common-filter"));
                    assertNull(response.getHeaders().get("x-request-filter"));
                });
    }

    @Test
    public void testFilterForbidRequest() throws Exception {
        ApplicationUnderTest.of(server)
                .test(testHttpClient -> {
                    final ReceivedResponse response = testHttpClient
                            .requestSpec(requestSpec -> requestSpec.getHeaders().add("x-block-in-request-filter", "block"))
                            .get("/filtered/test");
                    TestUtils.logResponse(LOGGER, response, "/filtered/test");
                    assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode());
                    assertEquals("done", response.getHeaders().get("x-common-filter"));
                    assertEquals("done", response.getHeaders().get("x-request-filter"));
                });
    }
}
