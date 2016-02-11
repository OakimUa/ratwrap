# RatWrap
Ratpack spring integration with spring-mvc-style request handling

#### Links:

- [Ratpack home](https://ratpack.io/)
- [Ratpack current manual](https://ratpack.io/manual/current/)
- [Ratpack API](https://ratpack.io/manual/current/api/)
- [Retpack GitHub](https://github.com/ratpack/ratpack)
- [Java-WebSocket](https://github.com/TooTallNate/Java-WebSocket)

## Config

`@EnableRatpack` annotation will start RatpackServer with defined server configuration and registry.
ATM spring configuration contains only `ratpack.port` with default value 8080.
Alternative configuration could be defined with bean `ServerConfig`.

All beans annotated with `@ServerRegistry` will be added to Ratpack server registry. It not works with beans created 
in configuration files. Alternative possibility to add something to the registry - definition of bean `Registry`.
For correct registry processing bean `RegistryBuilder` should be taken - it already contains all `@ServerRegistry` 
beans and `ObjectMapper`.

```java
@Bean
@Autowired
public Registry registry(RegistryBuilder builder) {
    builder.add(anotherTestRegistryBean());
    return builder.build();
}
```

## Request Handling

All beans annotated as `@RequestController` will be used for the request handling. Each method annotated with 
`@RequestHandler` is a request handler. `RequestController` annotation defines base path for all included handlers.
`RequestHandler` defines request uri, request method, response status, produced content type and other handler parameters.

For correct handling, defined request URIs should be unique.

### Request parameters

Each handler method could retrieve parameters:
- `@PathVariable("entity-id") final Integer id` - parameter from request URI. Should have name as defined in URI: `@RequestHandler(uri = "entities/{entity-id}")`;
- `@QueryParam("filter-val") final String filter` - query parameter: `HttpClient.get("/entities?filter-val=somevalue")`;
- `@HeaderParam("etag") final String etag` - injects header value;
- `@ContextParam final HttpClient httpClient` - will be taken from ratpack context by class. Also context itself could be injected.

## Response

### Response type

Handler response type defined with `@RequestHandler(produce = "...")`. Currently only `text*` and `*json`
types are supported.

Handler can return any Object (besides `WebSocketHandler`, `ClosableBlockingQueue` and `Publisher`) or ResponseEntity
that will be converted to json or text (depends on produce).
With ResponseEntity you could also define response status and headers.

By default, operation is `Blocking`, that means server will do response in a different thread to not block server.
`@RequestHandler(blocking = false)` you could turn this option off.

### Server Sent Events

If handler returns `ClosableBlockingQueue` or `Publisher` (with `@RequestHandler(webSocketBroadcasting = false)`) then
response will be converted to [Server Sent Events](https://ratpack.io/manual/current/streams.html#server_sent_events).
In this case you can define event name and event id with `@RequestHandler(eventName = "...", eventIdMethod="...")`.
By default, event name is method name and event id method is `toString()`.

#### Client SSE subscribing

For processing SSE on java client side you can use `SSESubscriber`. But consuming events from subscriber
(`event = subscriber.next()`) should be executed in different stream with subscription:

```java
/* Thread #1 */
ctx.get(HttpClient.class)
        .requestStream(uri), requestSpec -> {})
        .then(streamedResponse -> {
            final TransformablePublisher<ByteBuf> publisher = streamedResponse.getBody();
            publisher.subscribe(subscriber);
        });
/* Thread #2 */
HttpEvent<InputData> event;
while (!(event = subscriber.next()).isPoison()) {
    // process event
}
```

### Websocket communication

If handler returns `WebSocketHandler` or `Publisher` (with `@RequestHandler(webSocketBroadcasting = true)`) then server
will try to open WebSocket channel with client. `WebSocketHandler` allows you bi-directional communication and
`Publisher` with `@RequestHandler(webSocketBroadcasting = true)` makes message broadcasting via WebSocket.

## ToDo's:

- `[x]` Exception and error handling
- `[ ]` Javadoc
- `[x]` Logging
- `[ ]` Security integration
  + `[ ]` Integration with [STUPS OAUTH2 support](https://github.com/zalando-stups/stups-spring-oauth2-support)
  + `[ ]` Integration with [STUPS tokens](https://github.com/zalando-stups/tokens) or [Spring access tokens](https://github.com/zalando-stups/spring-boot-zalando-stups-tokens)
- `[ ]` Use bean post processing in for handler dispatch
- `[ ]` Extend content type support
- `[ ]` Solve blocking thread issue in SSESubscriber
- `[ ]` Add client SSE emulation for the long polling. For instance for integration with [Nakadi](https://github.com/zalando/nakadi/)
- `[ ]` Remove spring-web dependency
