package de.zalando.mass.ratwrap.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ratpack", ignoreUnknownFields = false)
public class RatpackProperties {
  @Value("#{environment.getProperty('server.port')}")
  private Integer port = 8080;
}
