package cz.komercpoj.tmpmgmt.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tmpmgmt.keycloak")
public record KeycloakProperties(
    String serverUrl, String realm, String adminClientId, String adminClientSecret) {}
