package de.zalando.mass.ratwrap.handler;

import de.zalando.mass.ratwrap.TestApplication;
import lombok.Data;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ratpack.server.RatpackServer;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
public class WebSocketTest {

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
    public void testSocket() throws Exception {
        CompletableFuture<WebSocketResult> future = new CompletableFuture<>();
        final int start = 0;

        WebSocketClient wsc = new WebSocketClient(
                URI.create("http://localhost:" + server.getBindPort() + "/sockets/regular"),
                new Draft_17(),
                null,
                5) {
            private int last = start;

            @Override
            public void onOpen(ServerHandshake handshakedata) {
                System.out.println("|--< client onOpen: " +
                        "status = " + handshakedata.getHttpStatus() +
                        ", message = " + handshakedata.getHttpStatusMessage());
                handshakedata.iterateHttpFields().forEachRemaining(field -> System.out.println("|--< client onOpen: " + field + " = " + handshakedata.getFieldValue(field)));
            }

            @Override
            public void onMessage(String message) {
                System.out.println("|--< client onMessage: " + message);
                if (message.startsWith("pong|")) {
                    final String[] split = message.split("\\|");
                    Integer i = Integer.parseInt(split[1]);
                    assertThat(i, is(last + 1));
                    last = i + 1;
                    if (i < 10) {
                        getConnection().send("ping|" + last);
                    } else {
                        getConnection().close();
                    }
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("|--< client onClose: code = " + code + ", reason = " + reason + ", remote = " + remote);
                future.complete(new WebSocketResult(code, "" + last));
            }

            @Override
            public void onError(Exception ex) {
                System.out.println("|--< client onError: " + ex.getMessage());
                ex.printStackTrace();
            }
        };

        assertTrue(wsc.connectBlocking());

        wsc.send("ping|" + start);

        final WebSocketResult webSocketResult = future.get(5, TimeUnit.SECONDS);
        assertThat(webSocketResult.getStatus(), is(1000));
        assertThat(webSocketResult.getData(), is("10"));
    }

    @Test
    public void testBroadcast() throws Exception {
        CompletableFuture<WebSocketResult> future = new CompletableFuture<>();

        WebSocketClient wsc = new WebSocketClient(
                URI.create("http://localhost:" + server.getBindPort() + "/sockets/broadcast"),
                new Draft_17(),
                null,
                5) {
            private String last = null;

            @Override
            public void onOpen(ServerHandshake handshakedata) {
                System.out.println("|--< client onOpen: " +
                        "status = " + handshakedata.getHttpStatus() +
                        ", message = " + handshakedata.getHttpStatusMessage());
                handshakedata.iterateHttpFields().forEachRemaining(field -> System.out.println("|--< client onOpen: " + field + " = " + handshakedata.getFieldValue(field)));
            }

            @Override
            public void onMessage(String message) {
                System.out.println("|--< client onMessage: " + message);
                last = message;
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("|--< client onClose: code = " + code + ", reason = " + reason + ", remote = " + remote);
                future.complete(new WebSocketResult(code, last));
            }

            @Override
            public void onError(Exception ex) {
                System.out.println("|--< client onError: " + ex.getMessage());
                ex.printStackTrace();
            }
        };

        assertTrue(wsc.connectBlocking());

        final WebSocketResult webSocketResult = future.get(5, TimeUnit.SECONDS);
        assertThat(webSocketResult.getStatus(), is(1000));
        assertThat(webSocketResult.getData(), is("{\"field1\":\"9:0\",\"field2\":9}"));
    }

    @Test
    public void testBroadcastWithParam() throws Exception {
        CompletableFuture<WebSocketResult> future = new CompletableFuture<>();

        WebSocketClient wsc = new WebSocketClient(
                URI.create("http://localhost:" + server.getBindPort() + "/sockets/broadcast?startWith=5"),
                new Draft_17(),
                null,
                5) {
            private String last = null;

            @Override
            public void onOpen(ServerHandshake handshakedata) {
                System.out.println("|--< client onOpen: " +
                        "status = " + handshakedata.getHttpStatus() +
                        ", message = " + handshakedata.getHttpStatusMessage());
                handshakedata.iterateHttpFields().forEachRemaining(field -> System.out.println("|--< client onOpen: " + field + " = " + handshakedata.getFieldValue(field)));
            }

            @Override
            public void onMessage(String message) {
                System.out.println("|--< client onMessage: " + message);
                last = message;
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("|--< client onClose: code = " + code + ", reason = " + reason + ", remote = " + remote);
                future.complete(new WebSocketResult(code, last));
            }

            @Override
            public void onError(Exception ex) {
                System.out.println("|--< client onError: " + ex.getMessage());
                ex.printStackTrace();
            }
        };

        assertTrue(wsc.connectBlocking());

        final WebSocketResult webSocketResult = future.get(5, TimeUnit.SECONDS);
        assertThat(webSocketResult.getStatus(), is(1000));
        assertThat(webSocketResult.getData(), is("{\"field1\":\"9:0\",\"field2\":15}"));
    }

    @Data
    private class WebSocketResult {
        private final Integer status;
        private final String data;
    }
}
