package es.in2.wallet.infrastructure.core.config.properties;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public record AuthServerProperties(
        @URL String externalUrl,
        @URL String internalUrl) {

//    todo remove test logs
    @PostConstruct
    public void logUrls() {
        log.debug("AuthServerProperties - externalUrl: {}", externalUrl);
        log.debug("AuthServerProperties - internalUrl: {}", internalUrl);
    }
}
