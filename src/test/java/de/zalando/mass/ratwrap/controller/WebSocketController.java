package de.zalando.mass.ratwrap.controller;

import com.google.common.primitives.Ints;
import de.zalando.mass.ratwrap.annotation.RequestHandler;
import de.zalando.mass.ratwrap.annotation.QueryParam;
import de.zalando.mass.ratwrap.annotation.RequestController;
import de.zalando.mass.ratwrap.data.InputData;
import org.reactivestreams.Publisher;
import ratpack.stream.Streams;
import ratpack.websocket.WebSocketHandler;
import ratpack.websocket.internal.BuiltWebSocketHandler;

import java.util.Optional;

import static de.zalando.mass.ratwrap.enums.RequestMethod.GET;

@RequestController(uri = "sockets/")
public class WebSocketController {

    @RequestHandler(method = GET, uri = "regular", eventName = "scheduled_event", eventIdMethod = "getField2")
    public WebSocketHandler<Boolean> regular() {
        return new BuiltWebSocketHandler<>(
                webSocket -> {
                    System.out.println("|--> server onOpen: ");
                    return true;
                },
                webSocketClose -> {
                    System.out.println("|--> server onClose: " +
                            "fromClient = " + webSocketClose.isFromClient() +
                            ", fromServer = " + webSocketClose.isFromServer() +
                            ", openResult = " + webSocketClose.getOpenResult().toString());
                },
                webSocketMessage -> {
                    System.out.println("|--> server onMessage: " +
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


    @RequestHandler(method = GET, uri = "broadcast", webSocketBroadcasting = true)
    public Publisher<InputData> broadcast(
            @QueryParam("startWith") final String lastEventID) {
        int start = Ints.tryParse(Optional.ofNullable(lastEventID).orElse("-1"));
        return Streams.yield(yieldRequest -> {
            if (yieldRequest.getRequestNum() > 9)
                return null;
            System.out.println("server -> " + yieldRequest.toString());
            return new InputData("" + yieldRequest.getRequestNum() + ":" + yieldRequest.getSubscriberNum(),
                    (int) yieldRequest.getRequestNum() + start + 1);
        });
    }
}
