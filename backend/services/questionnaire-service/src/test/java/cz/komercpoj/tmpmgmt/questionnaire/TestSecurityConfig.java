package cz.komercpoj.tmpmgmt.questionnaire;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration(proxyBeanMethods = false)
@EnableMethodSecurity
@Profile("test")
public class TestSecurityConfig {

  @Bean
  SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(c -> c.disable())
        .authorizeHttpRequests(a -> a.anyRequest().permitAll())
        .build();
  }
}
