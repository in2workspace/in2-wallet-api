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
                                   @NestedConfigurationProperty UrlProperties tokenUrl,
                                   String jwtDecoderPath ) {

    @ConstructorBinding
    public AuthServerProperties(UrlProperties externalUrl, UrlProperties internalUrl, UrlProperties tokenUrl, String jwtDecoderPath) {
        this.externalUrl = Optional.ofNullable(externalUrl).orElse(new UrlProperties(null, null, 0, null));
        this.internalUrl = Optional.ofNullable(internalUrl).orElse(new UrlProperties(null, null, 0, null));
        this.tokenUrl = Optional.ofNullable(tokenUrl).orElse(new UrlProperties(null, null, 0, null));
        this.jwtDecoderPath = Optional.ofNullable(jwtDecoderPath).orElse("/protocol/openid-connect/certs");

    }

}
