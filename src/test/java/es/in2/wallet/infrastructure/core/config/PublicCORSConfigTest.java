package es.in2.wallet.infrastructure.core.config;

import es.in2.wallet.application.ports.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static es.in2.wallet.domain.utils.ApplicationConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PublicCORSConfigTest {

    private AppConfig appConfig;
    private PublicCORSConfig publicCORSConfig;

    @BeforeEach
    void setUp() {
        appConfig = mock(AppConfig.class);
        when(appConfig.getCorsAllowedOrigins()).thenReturn(List.of("https://example.com"));
        publicCORSConfig = new PublicCORSConfig();
    }

    @Test
    void shouldCreateCorsSourceWithExpectedConfigurationForAllEndpoints() throws Exception {
        UrlBasedCorsConfigurationSource source = publicCORSConfig.publicCorsConfigSource(appConfig);

        Field field = UrlBasedCorsConfigurationSource.class.getDeclaredField("corsConfigurations");
        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<?, CorsConfiguration> configMap = (Map<?, CorsConfiguration>) field.get(source);

        List<String> expectedPaths = List.of(ENDPOINT_PIN, ENDPOINT_HEALTH, ENDPOINT_PROMETHEUS);

        for (String expectedPath : expectedPaths) {
            Object matchedKey = configMap.keySet().stream()
                    .filter(k -> k.toString().contains(expectedPath))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No s'ha trobat la clau per " + expectedPath));

            CorsConfiguration config = configMap.get(matchedKey);

            assertThat(config.getAllowedOrigins()).containsExactly("https://example.com");
            assertThat(config.getAllowedMethods()).containsExactly("GET", "POST", "OPTIONS");
            assertThat(config.getAllowedHeaders()).containsExactly("*");
            assertThat(config.getAllowCredentials()).isTrue();
            assertThat(config.getMaxAge()).isEqualTo(1800L);
        }
    }
}
