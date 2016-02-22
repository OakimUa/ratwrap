package de.zalando.mass.ratwrap.security;

import de.zalando.mass.ratwrap.TestApplication;
import de.zalando.mass.ratwrap.util.TestUtils;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.hamcrest.Matchers;
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
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
public class SecurityTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityTest.class);

    @Autowired
    private RatpackServer server;

    private RatpackServer securityServer;

    private Integer callCounter;

    @Before
    public void setUp() throws Exception {
        callCounter = 0;
        securityServer = RatpackServer.start(server -> server
                .serverConfig(scBuilder -> scBuilder.port(8099))
                .handlers(chain -> chain
                        .get("oauth2/tokeninfo", ctx -> {
                            callCounter++;
                            switch (ctx.getRequest().getQueryParams().get("access_token")) {
                                case "valid-token":
                                    ctx.render("{\"uid\":\"fsinatra\"," +
                                            "\"scope\":[\"uid\",\"cn\"]," +
                                            "\"grant_type\":\"password\"," +
                                            "\"cn\":\"Francis Albert Sinatra\"," +
                                            "\"realm\":\"/employees\"," +
                                            "\"token_type\":\"Bearer\"," +
                                            "\"expires_in\":3000," +
                                            "\"access_token\":\"valid-token\"}");
                                    break;
                                case "not-valid-token":
                                default:
                                    ctx.getResponse().status(400);
                                    ctx.render("{\"error\":\"invalid_request\",\"error_description\":\"Access Token not valid\"}");
                            }
                        })));
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
        if (securityServer!=null) {
            securityServer.stop();
        }
    }

    @Test
    public void testSuccess() throws Exception {
        final String token = "valid-token";
        ApplicationUnderTest.of(server)
                .test(testHttpClient -> {
                    final ReceivedResponse response = testHttpClient
                            .requestSpec(requestSpec -> requestSpec.getHeaders().add(HttpHeaderNames.AUTHORIZATION, "Bearer " + token))
                            .get("/secured/verify");
                    TestUtils.logResponse(LOGGER, response, "/secured/verify");
                    assertEquals(HttpStatus.OK.value(), response.getStatusCode());
                    final String resp = response.getBody().getText();
                    assertEquals(token, resp);
                    assertThat(callCounter, Matchers.is(1));

                    final ReceivedResponse response1 = testHttpClient
                            .requestSpec(requestSpec -> requestSpec.getHeaders().add(HttpHeaderNames.AUTHORIZATION, "Bearer " + token))
                            .get("/secured/verify");
                    TestUtils.logResponse(LOGGER, response1, "/secured/verify");
                    assertEquals(HttpStatus.OK.value(), response1.getStatusCode());
                    final String resp1 = response1.getBody().getText();
                    assertEquals(token, resp1);
                    assertThat(callCounter, Matchers.is(1));
                });
    }

    @Test
    public void testForbidden() throws Exception {
        final String token = "not-valid-token";
        ApplicationUnderTest.of(server)
                .test(testHttpClient -> {
                    final ReceivedResponse response = testHttpClient
                            .requestSpec(requestSpec -> requestSpec.getHeaders().add(HttpHeaderNames.AUTHORIZATION, "Bearer " + token))
                            .get("/secured/verify");
                    TestUtils.logResponse(LOGGER, response, "/secured/verify");
                    assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode());
                });
    }
}
