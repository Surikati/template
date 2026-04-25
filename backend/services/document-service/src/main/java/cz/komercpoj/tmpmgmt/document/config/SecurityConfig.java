package cz.komercpoj.tmpmgmt.document.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("!test")
public class SecurityConfig {

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/actuator/health/**",
                        "/actuator/info",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/actuator/prometheus")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtAuthConverter())))
        .build();
  }

  private Converter<Jwt, AbstractAuthenticationToken> jwtAuthConverter() {
    JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
    JwtGrantedAuthoritiesConverter defaults = new JwtGrantedAuthoritiesConverter();
    conv.setJwtGrantedAuthoritiesConverter(
        jwt -> {
          Collection<GrantedAuthority> all = new ArrayList<>(defaults.convert(jwt));
          all.addAll(keycloakRealmRoles(jwt));
          return all;
        });
    return conv;
  }

  @SuppressWarnings("unchecked")
  private Collection<GrantedAuthority> keycloakRealmRoles(Jwt jwt) {
    Map<String, Object> realmAccess = jwt.getClaim("realm_access");
    if (realmAccess == null) return List.of();
    List<String> roles = (List<String>) realmAccess.get("roles");
    if (roles == null) return List.of();
    return roles.stream()
        .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
        .toList();
  }
}
