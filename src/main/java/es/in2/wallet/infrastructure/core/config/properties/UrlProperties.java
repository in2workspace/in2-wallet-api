package es.in2.wallet.infrastructure.core.config.properties;

import jakarta.validation.constraints.NotNull;

public record UrlProperties(
        @NotNull String domain,
        @NotNull String path) {
}
