package es.in2.wallet.infrastructure.core.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trusted-issuer-list")
public record TrustedIssuerListProperties(
        String uri

){
}
