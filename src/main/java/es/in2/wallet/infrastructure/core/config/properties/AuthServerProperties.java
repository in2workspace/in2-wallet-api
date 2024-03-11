package es.in2.wallet.infrastructure.core.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.Optional;

/**
 * AuthServerProperties
 *
 * @param externalUrl - externalUrl auth-server url
 * @param internalUrl - internalUrl auth-server url
 * @param tokenUrl - token Endpoint
 */
@ConfigurationProperties(prefix = "auth-server")
public record AuthServerProperties(UrlProperties externalUrl, UrlProperties internalUrl, UrlProperties tokenUrl) {

    @ConstructorBinding
    public AuthServerProperties(UrlProperties externalUrl, UrlProperties internalUrl, UrlProperties tokenUrl) {
        this.externalUrl = Optional.ofNullable(externalUrl).orElse(new UrlProperties("http", "externalissuerkeycloak.demo.in2.es", 8080, "EAAProvider"));
        this.internalUrl = Optional.ofNullable(internalUrl).orElse(new UrlProperties("http", "issuerkeycloak.demo.in2.es", 8080, "EAAProvider"));
        this.tokenUrl = Optional.ofNullable(tokenUrl).orElse(new UrlProperties("http", "issuerkeycloak.demo.in2.es", 8080, "/realms/EAAProvider/verifiable-credential/did:key:z6MkqmaCT2JqdUtLeKah7tEVfNXtDXtQyj4yxEgV11Y5CqUa/token"));
    }

}
