package es.in2.wallet.infrastructure.core.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.List;
import java.util.Optional;

/**
 * WalletDataProperties
 *
 * @param urls - list of wallet driving application url
 */
@ConfigurationProperties(prefix = "wallet-wda")
public record WalletDrivingApplicationProperties(List<UrlProperties> urls) {

    @ConstructorBinding
    public WalletDrivingApplicationProperties(List<UrlProperties> urls) {
        this.urls = Optional.ofNullable(urls).orElse(List.of());
    }

}
