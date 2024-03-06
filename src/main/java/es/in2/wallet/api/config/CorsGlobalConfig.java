package es.in2.wallet.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import static es.in2.wallet.api.util.MessageUtils.ALLOWED_METHODS;
import static es.in2.wallet.api.util.MessageUtils.GLOBAL_ENDPOINTS_API;

@Configuration
@EnableWebFlux
@RequiredArgsConstructor
public class CorsGlobalConfig implements WebFluxConfigurer {
    private final AppConfig appConfig;
    @Override
    public void addCorsMappings(CorsRegistry corsRegistry) {
        corsRegistry.addMapping(GLOBAL_ENDPOINTS_API)
                .allowedOrigins(appConfig.getWalletDrivingUrl(), "http://localhost:4200")
                .allowedMethods(ALLOWED_METHODS)
                .maxAge(3600);
    }
}
