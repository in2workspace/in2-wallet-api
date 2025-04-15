package es.in2.wallet.infrastructure.core.config;

import es.in2.wallet.application.ports.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

import static es.in2.wallet.domain.utils.ApplicationConstants.*;

@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final AppConfig appConfig;
    private final ReactiveJwtDecoder internalJwtDecoder;
    private final InternalCORSConfig internalCORSConfig;
    private final PublicCORSConfig publicCORSConfig;

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder
                .withJwkSetUri(appConfig.getJwtDecoder())
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(appConfig.getAuthServerExternalUrl()));
        log.debug("JWT Decoder URI: {}", appConfig.getJwtDecoder());
        log.debug("JWT Issuer: {}", appConfig.getAuthServerExternalUrl());
        return jwtDecoder;
    }

    // Public filter chain for public endpoints
    @Bean
    @Order(1)
    public SecurityWebFilterChain publicFilterChain(ServerHttpSecurity http) {
        http
                .securityMatcher(ServerWebExchangeMatchers.pathMatchers(
                        ENDPOINT_PIN,
                        ENDPOINT_HEALTH,
                        ENDPOINT_PROMETHEUS
                ))
                .cors(cors -> cors.configurationSource(publicCORSConfig.publicCorsConfigurationSource()))
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll()
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable);

        return http.build();
    }

    // Internal security configuration for internal endpoints
    @Bean
    @Order(2)
    public SecurityWebFilterChain internalFilterChain(ServerHttpSecurity http) {
        http
                .securityMatcher(ServerWebExchangeMatchers.anyExchange())
                .cors(cors -> internalCORSConfig.defaultCorsConfigurationSource())
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().authenticated()
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .oauth2ResourceServer(oauth2ResourceServer ->
                        oauth2ResourceServer
                                .jwt(jwtSpec -> jwtSpec
                                        .jwtDecoder(internalJwtDecoder))
                );

        return http.build();
    }
}
