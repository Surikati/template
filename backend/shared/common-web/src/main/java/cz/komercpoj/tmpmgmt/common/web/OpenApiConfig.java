package cz.komercpoj.tmpmgmt.common.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Value("${spring.application.name:template-management-service}")
  private String appName;

  @Value("${spring.application.version:0.1.0}")
  private String appVersion;

  @Bean
  OpenAPI openApi() {
    final String schemeName = "bearerAuth";
    return new OpenAPI()
        .info(
            new Info()
                .title(appName)
                .version(appVersion)
                .description("Template Management REST API"))
        .addSecurityItem(new SecurityRequirement().addList(schemeName))
        .components(
            new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes(
                    schemeName,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
