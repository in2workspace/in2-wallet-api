package es.in2.wallet.infrastructure.core.config;

import es.in2.wallet.application.ports.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.CorsRegistration;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorsGlobalConfigTest {

    @Mock
    private AppConfig appConfig;

    @Mock
    private CorsRegistry corsRegistry;

    @Mock
    private CorsRegistration corsRegistration;

    private CorsGlobalConfig corsGlobalConfig;

    @BeforeEach
    void setUp() {
        corsGlobalConfig = new CorsGlobalConfig(appConfig);
    }

    @Test
    void addCorsMappings_shouldConfigureCorsCorrectly() {
        // Arrange
        List<String> allowedOrigins = List.of("https://example.com", "http://localhost:3000");
        when(appConfig.getCorsAllowedOrigins()).thenReturn(allowedOrigins);

        when(corsRegistry.addMapping("/**")).thenReturn(corsRegistration);
        when(corsRegistration.allowedOrigins(any(String[].class))).thenReturn(corsRegistration);
        when(corsRegistration.allowedMethods(anyString())).thenReturn(corsRegistration);
        when(corsRegistration.allowedHeaders(anyString())).thenReturn(corsRegistration);
        when(corsRegistration.allowCredentials(anyBoolean())).thenReturn(corsRegistration);
        when(corsRegistration.maxAge(anyLong())).thenReturn(corsRegistration);

        // Act
        corsGlobalConfig.addCorsMappings(corsRegistry);

        // Assert
        verify(corsRegistry).addMapping("/**");
        verify(corsRegistration).allowedOrigins(allowedOrigins.toArray(String[]::new));
        verify(corsRegistration).allowedMethods("/*");
        verify(corsRegistration).allowedHeaders("*");
        verify(corsRegistration).allowCredentials(true);
        verify(corsRegistration).maxAge(3600);
    }
}