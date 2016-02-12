package de.zalando.mass.ratwrap.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.zalando.mass.ratwrap.TestApplication;
import de.zalando.mass.ratwrap.data.InputData;
import de.zalando.mass.ratwrap.sse.client.HttpEvent;
import de.zalando.mass.ratwrap.sse.client.SSESubscriber;
import io.netty.buffer.ByteBuf;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ratpack.func.Action;
import ratpack.http.client.HttpClient;
import ratpack.http.client.RequestSpec;
import ratpack.server.RatpackServer;
import ratpack.stream.TransformablePublisher;
import ratpack.test.embed.EmbeddedApp;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
public class SSEStreamTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SSEStreamTest.class);

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
    public void testStream() throws Exception {
        doTestStream("regular", requestSpec -> {
        }, -1, "0123456789");
        final Action<RequestSpec> requestSpecAction = requestSpec -> requestSpec.getHeaders().set("Last-Event-ID", "5");
        doTestStream("regular", requestSpecAction, 5, "6789101112131415");
    }

    @Test
    public void testQueueStream() throws Exception {
        doTestStream("queued", requestSpec -> {
        }, -1, "0123456789");
        final Action<RequestSpec> requestSpecAction = requestSpec -> requestSpec.getHeaders().set("Last-Event-ID", "5");
        doTestStream("queued", requestSpecAction, 5, "6789101112131415");
    }

    private void doTestStream(String uri, Action<RequestSpec> requestSpecAction, int start, String expected) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        final SSESubscriber<InputData> subscriber = new SSESubscriber<>(objectMapper, InputData.class);

        EmbeddedApp.fromHandler(ctx -> {
                    ctx.get(HttpClient.class)
                            .requestStream(URI.create("http://localhost:" + server.getBindPort() + "/streams/" + uri), requestSpecAction)
                            .then(streamedResponse -> {
                                final Set<String> names = streamedResponse.getHeaders().getNames();
                                names.stream().forEach(name -> LOGGER.debug(name + " : " + streamedResponse.getHeaders().get(name)));
                                final TransformablePublisher<ByteBuf> publisher = streamedResponse.getBody();
                                publisher.subscribe(subscriber);
                            });
                    ctx.render("Done");
                }
        ).getHttpClient().get();

        List<InputData> dataList = new LinkedList<>();
        HttpEvent<InputData> event;
        while (!(event = subscriber.next()).isPoison()) {
            LOGGER.debug("--> client: " + event.toString());
            assertEquals("" + (dataList.size() + start + 1), event.getId());
            assertEquals(Integer.valueOf(dataList.size() + start + 1), event.getData().getField2());
            dataList.add(event.getData());
        }

        LOGGER.debug(dataList.toString());
        assertEquals(10, dataList.size());
        assertEquals(expected, dataList.stream().map(InputData::getField2).sorted().map(Object::toString).collect(Collectors.joining()));
        LOGGER.debug("--> FIN!");
    }

}
