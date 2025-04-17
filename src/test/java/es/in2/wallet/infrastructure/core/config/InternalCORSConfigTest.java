package es.in2.wallet.infrastructure.core.config;

import es.in2.wallet.application.ports.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InternalCORSConfigTest {

    private AppConfig appConfig;
    private InternalCORSConfig internalCORSConfig;

    @BeforeEach
    void setUp() {
        appConfig = mock(AppConfig.class);
        when(appConfig.getCorsAllowedOrigins()).thenReturn(List.of("https://example.com"));
        internalCORSConfig = new InternalCORSConfig(appConfig);
    }

    @Test
    void shouldCreateCorsSourceWithExpectedConfiguration() throws Exception {
        UrlBasedCorsConfigurationSource source = internalCORSConfig.internalCorsConfigurationSource();

        Field field = UrlBasedCorsConfigurationSource.class.getDeclaredField("corsConfigurations");
        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<?, CorsConfiguration> configMap = (Map<?, CorsConfiguration>) field.get(source);

        Object matchedKey = configMap.keySet().stream()
                .filter(k -> k.toString().contains("/"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No s'ha trobat cap clau amb '/'"));

        CorsConfiguration config = configMap.get(matchedKey);

        assertThat(config.getAllowedOrigins()).containsExactly("https://example.com");
        assertThat(config.getAllowedMethods()).containsExactly("GET", "POST", "PUT", "DELETE", "OPTIONS");
        assertThat(config.getAllowedHeaders()).containsExactly("*");
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getMaxAge()).isEqualTo(3600L);
    }
}
