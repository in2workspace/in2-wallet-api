package es.in2.wallet.api.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.Optional;

/**
 * AuthServerProperties
 *
 * @param externalDomain - external auth-server url
 * @param internalDomain - internal auth-server url
 * @param tokenEndpoint - token Endpoint
 */
@ConfigurationProperties(prefix = "auth-server")
public record AuthServerProperties(String externalDomain, String internalDomain, String tokenEndpoint) {

    @ConstructorBinding
    public AuthServerProperties(String externalDomain, String internalDomain, String tokenEndpoint) {
        this.externalDomain = Optional.ofNullable(externalDomain).orElse("https://issuerkeycloak.demo.in2.es/realms/EAAProvider");
        this.internalDomain = Optional.ofNullable(internalDomain).orElse("https://issuerkeycloak.demo.in2.es/realms/EAAProvider");
        this.tokenEndpoint = Optional.ofNullable(tokenEndpoint).orElse("https://issuerkeycloak.demo.in2.es/realms/EAAProvider/verifiable-credential/did:key:z6MkqmaCT2JqdUtLeKah7tEVfNXtDXtQyj4yxEgV11Y5CqUa/token");
    }

}
