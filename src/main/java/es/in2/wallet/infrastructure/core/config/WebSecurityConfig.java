package es.in2.wallet.infrastructure.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static es.in2.wallet.domain.util.ApplicationConstants.GLOBAL_ENDPOINTS_API;

@Configuration
@EnableWebFluxSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        // Configures security for web applications in a reactive environment.
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // Disables Cross-Site Request Forgery (CSRF) protection.
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                // Disables HTTP Basic Authentication.
                .authorizeExchange(exchanges ->
                                exchanges
                                        .pathMatchers(HttpMethod.GET, GLOBAL_ENDPOINTS_API).authenticated()
                                        .pathMatchers(HttpMethod.POST, GLOBAL_ENDPOINTS_API).authenticated()
                                        .pathMatchers(HttpMethod.DELETE, GLOBAL_ENDPOINTS_API).authenticated()
                                        // Specifies authorization rules.
                                        // For GET, POST, and DELETE requests to paths matching "/api/v1/**", authentication is required.
                                        .anyExchange().permitAll()
                        // Allows all other requests without authentication, making the application accessible while protecting specific endpoints.
                )
                .oauth2ResourceServer(oauth2ResourceServer ->
                        oauth2ResourceServer.jwt(jwt ->
                                        jwt.jwtAuthenticationConverter(new ReactiveJwtAuthenticationConverter())
                                // Configures the OAuth2 resource server to use JWT authentication.
                                // The ReactiveJwtAuthenticationConverter is used to integrate with the reactive processing flow, converting JWTs into Spring Security Authentication objects.
                        )
                );

        return http.build();
        // Builds and returns the security configuration.
    }
}