package es.in2.wallet.infrastructure.core.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.Optional;

/**
 * AuthServerProperties
 *
 * @param externalUrl - externalUrl auth-server url
 * @param internalUrl - internalUrl auth-server url
 * @param tokenUrl    - token Endpoint
 */
@ConfigurationProperties(prefix = "auth-server")
public record AuthServerProperties(@NestedConfigurationProperty UrlProperties externalUrl,
                                   @NestedConfigurationProperty UrlProperties internalUrl,
                                   @NestedConfigurationProperty UrlProperties tokenUrl) {

    @ConstructorBinding
    public AuthServerProperties(UrlProperties externalUrl, UrlProperties internalUrl, UrlProperties tokenUrl) {
        this.externalUrl = Optional.ofNullable(externalUrl).orElse(new UrlProperties(null, null, null, null));
        this.internalUrl = Optional.ofNullable(internalUrl).orElse(new UrlProperties(null, null, null, null));
        this.tokenUrl = Optional.ofNullable(tokenUrl).orElse(new UrlProperties(null, null, null, null));

    }

}
