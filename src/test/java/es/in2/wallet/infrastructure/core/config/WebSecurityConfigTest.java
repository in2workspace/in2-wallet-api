package es.in2.wallet.infrastructure.core.config;

import es.in2.wallet.application.ports.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringJUnitConfig
class WebSecurityConfigTest {

    @MockBean
    AppConfig appConfig;

    @MockBean
    InternalCORSConfig internalCORSConfig;

    @MockBean
    PublicCORSConfig publicCORSConfig;

    @MockBean
    @Qualifier("internalCorsSource")
    UrlBasedCorsConfigurationSource internalCorsSource;

    @MockBean
    @Qualifier("publicCorsSource")
    UrlBasedCorsConfigurationSource publicCorsSource;

    WebSecurityConfig webSecurityConfig;

    @BeforeEach
    void setUp() {
        when(appConfig.getJwtDecoder()).thenReturn("https://example.com/.well-known/jwks.json");
        when(appConfig.getAuthServerExternalUrl()).thenReturn("https://example.com");

        webSecurityConfig = new WebSecurityConfig(appConfig, internalCORSConfig, publicCORSConfig);
    }

    @Test
    void jwtDecoder_shouldCreateDecoderWithConfiguredValues() {
        ReactiveJwtDecoder decoder = webSecurityConfig.jwtDecoder();

        assertThat(decoder).isNotNull();
    }

    @Test
    void publicFilterChain_shouldCreateSecurityWebFilterChain() {
        ServerHttpSecurity http = ServerHttpSecurity.http();
        SecurityWebFilterChain chain = webSecurityConfig.publicFilterChain(http, publicCorsSource);

        assertThat(chain).isNotNull();
        assertThat(chain).isInstanceOf(SecurityWebFilterChain.class);
    }

    @Test
    void internalFilterChain_shouldCreateSecurityWebFilterChain() {
        ServerHttpSecurity http = ServerHttpSecurity.http();
        SecurityWebFilterChain chain = webSecurityConfig.internalFilterChain(http, internalCorsSource);

        assertThat(chain).isNotNull();
        assertThat(chain).isInstanceOf(SecurityWebFilterChain.class);
    }
}
