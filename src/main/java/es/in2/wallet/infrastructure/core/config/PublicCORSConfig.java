package es.in2.wallet.infrastructure.core.config;

import es.in2.wallet.application.ports.AppConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import static es.in2.wallet.domain.utils.ApplicationConstants.*;

import java.util.List;

@Configuration
public class PublicCORSConfig {

    /**
     * Public CORS configuration source for restricted access within the cluster.
     */
    @Bean
    public UrlBasedCorsConfigurationSource publicCorsConfigurationSource(AppConfig appConfig) {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(appConfig.getCorsAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(1800L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(ENDPOINT_PIN, configuration);
        source.registerCorsConfiguration(ENDPOINT_HEALTH, configuration);
        source.registerCorsConfiguration(ENDPOINT_PROMETHEUS, configuration);

        return source;
    }
}
