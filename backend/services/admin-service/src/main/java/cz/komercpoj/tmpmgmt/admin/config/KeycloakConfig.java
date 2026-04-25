package cz.komercpoj.tmpmgmt.admin.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
public class KeycloakConfig {

  @Bean
  Keycloak keycloakAdminClient(KeycloakProperties props) {
    return KeycloakBuilder.builder()
        .serverUrl(props.serverUrl())
        .realm(props.realm())
        .grantType("client_credentials")
        .clientId(props.adminClientId())
        .clientSecret(props.adminClientSecret())
        .build();
  }
}
