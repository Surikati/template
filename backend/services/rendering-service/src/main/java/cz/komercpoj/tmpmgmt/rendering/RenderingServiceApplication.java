package cz.komercpoj.tmpmgmt.rendering;

import cz.komercpoj.tmpmgmt.rendering.config.RenderingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(RenderingProperties.class)
@EnableFeignClients
@EnableScheduling
public class RenderingServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(RenderingServiceApplication.class, args);
  }
}
