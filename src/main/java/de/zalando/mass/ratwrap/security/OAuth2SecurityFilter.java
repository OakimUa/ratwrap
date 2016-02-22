package de.zalando.mass.ratwrap.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.zalando.mass.ratwrap.filter.RequestFilter;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.zalando.problem.Problem;
import ratpack.handling.Context;
import ratpack.http.client.HttpClient;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toSet;

public class OAuth2SecurityFilter implements RequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2SecurityFilter.class);

    @Value("${ratwrap.security.url}")
    protected String authUrl;

    @Autowired
    private ObjectMapper objectMapper;

    // TODO: collection with ttl or cache
    private Map<String, TokenInfo> tokens = new ConcurrentHashMap<>();
    private Map<String, Long> expirations = new ConcurrentHashMap<>();

    @Override
    public void handle(Context ctx) throws Exception {
        LOGGER.debug("SecurityFilter");
        // Expire elements
        final long timestamp = System.currentTimeMillis();
        final Set<String> expired = expirations.entrySet().stream()
                .filter(entry -> entry.getValue() < timestamp)
                .map(Map.Entry::getKey)
                .collect(toSet());
        if (!expired.isEmpty()) {
            final String[] expiredKeys = expired.toArray(new String[expired.size()]);
            for (String expiredKey : expiredKeys) {
                tokens.remove(expiredKey);
                expirations.remove(expiredKey);
            }
        }

        final String token = Optional.ofNullable(ctx.getRequest().getHeaders().get(HttpHeaderNames.AUTHORIZATION))
                .map(hdr -> hdr.substring("Bearer ".length()))
                .orElseThrow(() -> Problem.valueOf(Response.Status.FORBIDDEN));
        if (tokens.containsKey(token)) {
            final TokenInfo tokenInfo = tokens.get(token);
            LOGGER.debug("Access granted from cache to " + tokenInfo.getCn());
            ctx.getRequest().add(tokenInfo);
            ctx.next();
        } else {
            final String path = authUrl + token;
            ctx.get(HttpClient.class)
                    .get(URI.create(path))
                    .then(response -> {
                        final TokenInfo tokenInfo = objectMapper.readValue(response.getBody().getInputStream(), TokenInfo.class);
                        if (response.getStatusCode() == 200) {
                            tokens.put(token, tokenInfo);
                            expirations.put(token, timestamp + tokenInfo.getExpiresIn() - TimeUnit.SECONDS.toMillis(1));
                            LOGGER.debug("Access granted to " + tokenInfo.getCn());
                            ctx.getRequest().add(tokenInfo);
                            ctx.next();
                        } else {
                            LOGGER.debug("Access denied.");
                            throw Problem.valueOf(Response.Status.fromStatusCode(HttpStatus.FORBIDDEN.value()), tokenInfo.getErrorDescription());
                        }
                    });
        }
    }
}
