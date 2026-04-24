package cz.komercpoj.tmpmgmt.rendering;

import cz.komercpoj.tmpmgmt.rendering.config.RenderingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RenderingProperties.class)
public class RenderingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RenderingServiceApplication.class, args);
    }
}
