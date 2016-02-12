package de.zalando.mass.ratwrap.enums;

public enum LongResponseType {
    /**
     * should send server sent events
     * @return
     */
    SERVER_SENT_EVENTS,

    /**
     * should open WebSocket connection with broadcasting
     * @return
     */
    WEBSOCKET_BROADCAST,

    /**
     * should send json objects separated by new line
     * @return
     */
    JSON_LONG_POLLING;
}
