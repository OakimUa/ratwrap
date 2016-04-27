package de.zalando.mass.ratwrap.controller;

import com.google.common.primitives.Ints;
import de.zalando.mass.ratwrap.Utils;
import de.zalando.mass.ratwrap.annotation.QueryParam;
import de.zalando.mass.ratwrap.annotation.RequestController;
import de.zalando.mass.ratwrap.annotation.RequestHandler;
import de.zalando.mass.ratwrap.data.InputData;
import de.zalando.mass.ratwrap.enums.LongResponseType;
import de.zalando.mass.ratwrap.sse.ClosableBlockingQueue;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.websocket.WebSocketHandler;
import ratpack.websocket.internal.BuiltWebSocketHandler;

import java.util.Optional;

import static de.zalando.mass.ratwrap.enums.RequestMethod.GET;

@RequestController(uri = "sockets/")
public class WebSocketController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketController.class);

    @RequestHandler(method = GET, uri = "regular")
    public WebSocketHandler<Boolean> regular() {
        return new BuiltWebSocketHandler<>(
                webSocket -> {
                    LOGGER.debug("|--> server onOpen: ");
                    return true;
                },
                webSocketClose -> {
                    LOGGER.debug("|--> server onClose: " +
                            "fromClient = " + webSocketClose.isFromClient() +
                            ", fromServer = " + webSocketClose.isFromServer() +
                            ", openResult = " + webSocketClose.getOpenResult().toString());
                },
                webSocketMessage -> {
                    LOGGER.debug("|--> server onMessage: " +
                            "text = " + webSocketMessage.getText() +
                            ", openResult = " + webSocketMessage.getOpenResult());
                    final String text = webSocketMessage.getText();
                    if (text.startsWith("ping|")) {
                        final String[] split = text.split("\\|");
                        Integer i = Integer.parseInt(split[1]);
                        if (i < 10) {
                            webSocketMessage.getConnection().send("pong|" + (i + 1));
                        } else {
                            webSocketMessage.getConnection().close();
                        }
                    }
                }
        );
    }

    @RequestHandler(method = GET, uri = "broadcast", longResponseType = LongResponseType.WEBSOCKET_BROADCAST)
    public Publisher<InputData> broadcast(
            @QueryParam("startWith") final String lastEventID) {
        int start = Ints.tryParse(Optional.ofNullable(lastEventID).orElse("-1"));
        return Utils.testPublisher(start, LOGGER);
    }
    @RequestHandler(method = GET, uri = "queued", longResponseType = LongResponseType.WEBSOCKET_BROADCAST)
    public ClosableBlockingQueue<InputData> queued(
            @QueryParam("startWith") final String lastEventID) {
        int start = Ints.tryParse(Optional.ofNullable(lastEventID).orElse("-1"));
        return Utils.testQueue(start);
    }
}
