# RatWrap
Ratpack spring integration with spring-mvc-style request handling

#### Links:

- [Ratpack home](https://ratpack.io/)
- [Ratpack current manual](https://ratpack.io/manual/current/)
- [Ratpack API](https://ratpack.io/manual/current/api/)

## Config

## Handlers

## SSE Subscribing

## ToDo's:

- Security integration
  + Integration with [STUPS OAUTH2 support](https://github.com/zalando-stups/stups-spring-oauth2-support)
  + Integration with [STUPS tokens](https://github.com/zalando-stups/tokens) or [Spring access tokens](https://github.com/zalando-stups/spring-boot-zalando-stups-tokens)
- Use bean post processing in for handler dispatch
- Extend content type support
- Solve blocking thread issue in SSESubscriber
- Remove spring-web dependency