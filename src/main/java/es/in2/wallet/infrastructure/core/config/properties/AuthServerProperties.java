package es.in2.wallet.infrastructure.core.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
        @NotNull @Valid UrlProperties externalUrl,
        @NotNull @Valid UrlProperties internalUrl) {
}
