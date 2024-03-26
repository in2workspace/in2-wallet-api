package es.in2.wallet.infrastructure.ebsi.config.properties;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Slf4j
@ConfigurationProperties(prefix = "ebsi.test")
public record EbsiProperties(@NotNull String url, @NotNull String clientSecret, @NotNull String clientId,
                             @NotNull String username, @NotNull String password) {

}
