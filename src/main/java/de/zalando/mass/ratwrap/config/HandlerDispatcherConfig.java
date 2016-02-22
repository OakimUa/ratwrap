package de.zalando.mass.ratwrap.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import de.zalando.mass.ratwrap.annotation.ServerRegistry;
import de.zalando.mass.ratwrap.handler.FilterDispatcher;
import de.zalando.mass.ratwrap.handler.HandlerDispatcher;
import de.zalando.mass.ratwrap.handler.ProblemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.problem.ProblemModule;
import ratpack.error.ServerErrorHandler;
import ratpack.registry.Registry;
import ratpack.registry.RegistryBuilder;
import ratpack.server.RatpackServer;
import ratpack.server.ServerConfig;

import java.util.Map;

@Configuration
@EnableConfigurationProperties({RatpackProperties.class})
public class HandlerDispatcherConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(HandlerDispatcherConfig.class);

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection")
    private RatpackProperties ratpack;

    @Autowired
    private ApplicationContext context;

    @Bean
    public HandlerDispatcher handlerDispatcher() {
        return new HandlerDispatcher();
    }

    @Bean
    public FilterDispatcher filterDispatcher() {
        return new FilterDispatcher();
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new Jdk8Module())
                .registerModule(new ProblemModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    }

    @Bean
    @Autowired
    public RatpackServer server(Registry registry) throws Exception {
        return RatpackServer.start(server -> server
                        .serverConfig(serverConfig())
                        .registry(registry)
                        .handlers(chain -> chain
                                .all(filterDispatcher())
                                .all(handlerDispatcher())));
    }

    @Bean
    public RegistryBuilder serverRegistryBuilder() {
        LOGGER.debug("Registry building");
        final Map<String, Object> registryBeans = context.getBeansWithAnnotation(ServerRegistry.class);
        final RegistryBuilder builder = Registry.builder();
        registryBeans.entrySet().forEach(entry -> {
            LOGGER.debug("rb -> " + entry.getKey());
            builder.add(entry.getValue());
        });
        builder.add(objectMapper());
        return builder;
    }

    @Bean
    @ConditionalOnMissingBean(Registry.class)
    @Autowired
    public Registry registry(RegistryBuilder builder) {
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean(ServerConfig.class)
    public ServerConfig serverConfig() {
        return ServerConfig.builder()
                .port(ratpack.getPort())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(ServerErrorHandler.class)
    public ServerErrorHandler errorHandler() {
        return new ProblemHandler();
    }

}
