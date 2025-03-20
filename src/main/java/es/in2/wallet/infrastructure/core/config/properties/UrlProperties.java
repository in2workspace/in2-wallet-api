package es.in2.wallet.infrastructure.core.config.properties;

import jakarta.validation.constraints.NotNull;

public record UrlProperties(
        @NotNull String scheme,
        @NotNull String domain,
        @NotNull Integer port,
        @NotNull String path) {
}
