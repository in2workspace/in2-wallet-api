package es.in2.wallet.infrastructure.core.config.properties;

import java.util.Optional;

public record UrlProperties(String scheme, String domain, Integer port, String path) {
}
