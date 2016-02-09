package de.zalando.mass.ratwrap;

import de.zalando.mass.ratwrap.config.EnableRatpack;
import de.zalando.mass.ratwrap.controller.AnotherTestRegistryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import ratpack.registry.Registry;
import ratpack.registry.RegistryBuilder;

@SpringBootApplication
@ComponentScan
@EnableRatpack
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

    @Bean
    public AnotherTestRegistryBean anotherTestRegistryBean() {
        return new AnotherTestRegistryBean();
    }

    @Bean
    @Autowired
    public Registry registry(RegistryBuilder builder) {
        builder.add(anotherTestRegistryBean());
        return builder.build();
    }

}
