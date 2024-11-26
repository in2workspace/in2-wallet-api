package es.in2.wallet.infrastructure.core.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.List;
import java.util.Optional;

/**
 * WalletDataProperties
 *
 * @param allowedOrigins - list of wallet driving application url
 */
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(List<String> allowedOrigins) {

    @ConstructorBinding
    public CorsProperties(List<String> allowedOrigins) {
        this.allowedOrigins = Optional.ofNullable(allowedOrigins).orElse(List.of());
    }
}
