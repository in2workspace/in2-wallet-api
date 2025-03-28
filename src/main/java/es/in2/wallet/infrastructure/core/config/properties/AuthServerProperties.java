package es.in2.wallet.infrastructure.core.config.properties;

import jakarta.validation.Valid;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * AuthServerProperties
 *
 * @param externalUrl - externalUrl auth-server url
 * @param internalUrl - internalUrl auth-server url
 */
@Validated
@ConfigurationProperties(prefix = "security.auth-server")
public record AuthServerProperties(
        @URL String externalUrl,
        @URL String internalUrl) {
}
