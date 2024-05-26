package es.in2.wallet.infrastructure.core.config;

import es.in2.wallet.application.port.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static es.in2.wallet.domain.util.ApplicationConstants.GLOBAL_ENDPOINTS_API;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
@Slf4j
public class WebSecurityConfig {
    private final AppConfig appConfig;
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder
                .withJwkSetUri(appConfig.getJwtDecoder())
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(appConfig.getAuthServerExternalUrl()));
        log.debug(appConfig.getJwtDecoder());
        log.debug(appConfig.getAuthServerExternalUrl());
        return jwtDecoder;
    }
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange(exchanges -> exchanges
                                .pathMatchers(HttpMethod.GET, GLOBAL_ENDPOINTS_API).authenticated()
                                .pathMatchers(HttpMethod.POST, GLOBAL_ENDPOINTS_API).authenticated()
                                .pathMatchers(HttpMethod.DELETE, GLOBAL_ENDPOINTS_API).authenticated()
                                .anyExchange().permitAll()
                ).csrf(ServerHttpSecurity.CsrfSpec::disable)
                // Disables Cross-Site Request Forgery (CSRF) protection.
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .oauth2ResourceServer(oauth2ResourceServer ->
                        oauth2ResourceServer
                                .jwt(withDefaults()));
        return http.build();
    }
}