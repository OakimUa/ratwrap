package de.zalando.mass.ratwrap.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.zalando.mass.ratwrap.TestApplication;
import de.zalando.mass.ratwrap.data.InputData;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ratpack.http.client.ReceivedResponse;
import ratpack.server.RatpackServer;
import ratpack.test.ApplicationUnderTest;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
public class RestTest {

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
    public void testContext() throws Exception {
        ApplicationUnderTest.of(server)
                .test(testHttpClient -> {
                    final ReceivedResponse response = testHttpClient.get("/test/data1/context");
                    assertEquals(HttpStatus.OK.value(), response.getStatusCode());
                    final String resp = response.getBody().getText();
                    assertEquals("HttpClient=true|ObjectMapper=true|Context=true", resp);
                    System.out.println(resp);
                });
    }

    @Test
    public void testRegistry() throws Exception {
        ApplicationUnderTest.of(server)
                .test(testHttpClient -> {
                    final ReceivedResponse response = testHttpClient.get("/test/data1/registry");
                    assertEquals(HttpStatus.OK.value(), response.getStatusCode());
                    assertTrue(Boolean.parseBoolean(response.getBody().getText()));
                });
    }

    @Test
    public void testGet() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ApplicationUnderTest.of(server)
                .test(testHttpClient -> {
                    final ReceivedResponse response = testHttpClient
                            .requestSpec(requestSpec -> requestSpec.getHeaders().add("param4", "headerValue"))
                            .get("/test/data1/path/15?param3=qwerty");
                    assertEquals(HttpStatus.OK.value(), response.getStatusCode());
                    assertEquals("application/json", response.getHeaders().get(HttpHeaderNames.CONTENT_TYPE));
                    final Map resp = objectMapper.readValue(response.getBody().getInputStream(), Map.class);
                    assertEquals("data1", resp.get("param1"));
                    assertEquals(15, resp.get("param2"));
                    assertEquals("qwerty", resp.get("param3"));
                    assertEquals("headerValue", resp.get("param4"));
                    System.out.println(resp);
                });
    }

    @Test
    public void testGet202() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ApplicationUnderTest.of(server)
                .test(testHttpClient -> {
                    final ReceivedResponse response = testHttpClient
                            .get("/test/data1/path202/15");
                    assertEquals(HttpStatus.ACCEPTED.value(), response.getStatusCode());
                    final Map resp = objectMapper.readValue(response.getBody().getInputStream(), Map.class);
                    assertEquals("data1", resp.get("param1"));
                    assertEquals(15, resp.get("param2"));
                    System.out.println(resp);
                });
    }

    @Test
    public void testPost() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ApplicationUnderTest.of(server)
                .test(testHttpClient -> {
                    final ReceivedResponse response = testHttpClient
                            .requestSpec(requestSpec -> requestSpec
                                    .body(body -> body
                                            .type("application/json")
                                            .bytes(objectMapper.writeValueAsBytes(new InputData("f1", 10)))))
                            .post("/test/data1/path/15");
                    assertEquals(HttpStatus.CREATED.value(), response.getStatusCode());
                    assertEquals("path/to/entity/data1/15/f1/10", response.getHeaders().get("Location"));
                });
    }

    @Test
    public void testTextResponse() throws Exception {
        ApplicationUnderTest.of(server)
                .test(testHttpClient -> {
                    final ReceivedResponse response = testHttpClient
                            .get("/test/data1/text");
                    assertEquals(HttpStatus.OK.value(), response.getStatusCode());
                    assertEquals("text/plain", response.getHeaders().get(HttpHeaderNames.CONTENT_TYPE));
                    final String resp = response.getBody().getText();
                    assertEquals("TestValue", resp);
                    System.out.println(resp);
                });
    }

    @Test
    public void testCustomResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ApplicationUnderTest.of(server)
                .test(testHttpClient -> {
                    final ReceivedResponse response = testHttpClient
                            .get("/test/data1/custom");
                    assertEquals(HttpStatus.OK.value(), response.getStatusCode());
                    assertEquals("application/x.custom+json", response.getHeaders().get(HttpHeaderNames.CONTENT_TYPE));
                    final InputData resp = objectMapper.readValue(response.getBody().getInputStream(), InputData.class);
                    assertEquals(new InputData("TestValue", 0), resp);
                    System.out.println(resp.toString());
                });
    }
}
