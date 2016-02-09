package de.zalando.mass.ratwrap.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.zalando.mass.ratwrap.TestApplication;
import de.zalando.mass.ratwrap.data.InputData;
import de.zalando.mass.ratwrap.sse.HttpEvent;
import de.zalando.mass.ratwrap.sse.SSESubscriber;
import io.netty.buffer.ByteBuf;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ratpack.http.client.HttpClient;
import ratpack.server.RatpackServer;
import ratpack.stream.TransformablePublisher;
import ratpack.test.embed.EmbeddedApp;
import ratpack.test.http.TestHttpClient;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
public class StreamTest {

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
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println();
        System.out.println();

        final SSESubscriber<InputData> subscriber = new SSESubscriber<>(objectMapper, InputData.class);

        final TestHttpClient testHttpClient = EmbeddedApp.fromHandler(ctx -> {
                    ctx.get(HttpClient.class)
                            .requestStream(URI.create("http://localhost:" + server.getBindPort() + "/streams/regular"), requestSpec -> {
                            })
                            .then(streamedResponse -> {
                                final Set<String> names = streamedResponse.getHeaders().getNames();
                                names.stream().forEach(name -> System.out.println(name + " : " + streamedResponse.getHeaders().get(name)));
                                final TransformablePublisher<ByteBuf> publisher = streamedResponse.getBody();
                                publisher.subscribe(subscriber);
                            });
                    ctx.render("Done");
                }
        ).getHttpClient();
        testHttpClient.get();

        List<InputData> dataList = new LinkedList<>();
        HttpEvent<InputData> event;
        while (!(event = subscriber.next()).isPoison()) {
            System.out.println("--> client: " + event.toString());
            assertEquals("" + dataList.size(), event.getId());
            assertEquals(Integer.valueOf(dataList.size()), event.getData().getField2());
            dataList.add(event.getData());
        }

        System.out.println(dataList);
        assertEquals(10, dataList.size());
        assertEquals("0123456789", dataList.stream().map(InputData::getField2).sorted().map(Object::toString).collect(Collectors.joining()));
        System.out.println("--> FIN!");
    }

    @Test
    public void testQueueStream() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println();
        System.out.println();

        final SSESubscriber<InputData> subscriber = new SSESubscriber<>(objectMapper, InputData.class);

        final TestHttpClient testHttpClient = EmbeddedApp.fromHandler(ctx -> {
                    ctx.get(HttpClient.class)
                            .requestStream(URI.create("http://localhost:" + server.getBindPort() + "/streams/queued"), requestSpec -> {
                            })
                            .then(streamedResponse -> {
                                final Set<String> names = streamedResponse.getHeaders().getNames();
                                names.stream().forEach(name -> System.out.println(name + " : " + streamedResponse.getHeaders().get(name)));
                                final TransformablePublisher<ByteBuf> publisher = streamedResponse.getBody();
                                publisher.subscribe(subscriber);
                            });
                    ctx.render("Done");
                }
        ).getHttpClient();
        testHttpClient.get();

        List<InputData> dataList = new LinkedList<>();
        HttpEvent<InputData> event;
        while (!(event = subscriber.next()).isPoison()) {
            System.out.println("--> client: " + event.toString());
            assertEquals("" + dataList.size(), event.getId());
            assertEquals(Integer.valueOf(dataList.size()), event.getData().getField2());
            dataList.add(event.getData());
        }

        System.out.println(dataList);
        assertEquals(10, dataList.size());
        assertEquals("0123456789", dataList.stream().map(InputData::getField2).sorted().map(Object::toString).collect(Collectors.joining()));
        System.out.println("--> FIN!");
    }

    @Test
    public void testStreamWithLastID() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println();
        System.out.println();
        int start = 5;

        final SSESubscriber<InputData> subscriber = new SSESubscriber<>(objectMapper, InputData.class);

        final TestHttpClient testHttpClient = EmbeddedApp.fromHandler(ctx -> {
                    ctx.get(HttpClient.class)
                            .requestStream(URI.create("http://localhost:" + server.getBindPort() + "/streams/regular"), requestSpec -> {
                                requestSpec.getHeaders().set("Last-Event-ID", "" + start);
                            })
                            .then(streamedResponse -> {
                                final Set<String> names = streamedResponse.getHeaders().getNames();
                                names.stream().forEach(name -> System.out.println(name + " : " + streamedResponse.getHeaders().get(name)));
                                final TransformablePublisher<ByteBuf> publisher = streamedResponse.getBody();
                                publisher.subscribe(subscriber);
                            });
                    ctx.render("Done");
                }
        ).getHttpClient();
        testHttpClient.get();

        List<InputData> dataList = new LinkedList<>();
        HttpEvent<InputData> event;
        while (!(event = subscriber.next()).isPoison()) {
            System.out.println("--> client: " + event.toString());
            assertEquals("" + (dataList.size() + start + 1), event.getId());
            assertEquals(Integer.valueOf(dataList.size() + start + 1), event.getData().getField2());
            dataList.add(event.getData());
        }

        System.out.println(dataList);
        assertEquals(10, dataList.size());
        assertEquals("6789101112131415", dataList.stream().map(InputData::getField2).sorted().map(Object::toString).collect(Collectors.joining()));
        System.out.println("--> FIN!");
    }

}
