package es.in2.wallet.infrastructure.core.config;

import es.in2.wallet.application.ports.AppConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

import static es.in2.wallet.domain.utils.ApplicationConstants.GLOBAL_ENDPOINTS_API;
import static es.in2.wallet.domain.utils.ApplicationConstants.ALLOWED_METHODS;

@Configuration
@RequiredArgsConstructor
public class InternalCORSConfig {

    private final AppConfig appConfig;

    @Bean
    public UrlBasedCorsConfigurationSource defaultCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(appConfig.getCorsAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        GLOBAL_ENDPOINTS instead of /**?
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
