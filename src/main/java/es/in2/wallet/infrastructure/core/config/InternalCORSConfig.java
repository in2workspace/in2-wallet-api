package es.in2.wallet.infrastructure.core.config;

import es.in2.wallet.application.ports.AppConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

import static es.in2.wallet.domain.utils.ApplicationConstants.GLOBAL_ENDPOINTS_API;

@Configuration
public class InternalCORSConfig {

    @Bean("internalCorsSource")
    public UrlBasedCorsConfigurationSource defaultCorsConfigurationSource(AppConfig appConfig) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(appConfig.getCorsAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(GLOBAL_ENDPOINTS_API, configuration);
        return source;
    }
}
