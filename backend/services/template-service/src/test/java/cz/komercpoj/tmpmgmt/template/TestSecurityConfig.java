package cz.komercpoj.tmpmgmt.template;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Replaces the production SecurityConfig under the {@code test} profile. Permits all requests so
 * integration tests can exercise service logic without a running Keycloak. OAuth2 resource-server
 * autoconfiguration is disabled in {@code application-test.yml}.
 */
@TestConfiguration(proxyBeanMethods = false)
@EnableMethodSecurity
@Profile("test")
public class TestSecurityConfig {

    @Bean
    SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(c -> c.disable())
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .build();
    }
}
