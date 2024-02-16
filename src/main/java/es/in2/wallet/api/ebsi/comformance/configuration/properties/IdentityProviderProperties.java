package es.in2.wallet.api.ebsi.comformance.configuration.properties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.Optional;

@Slf4j
@ConfigurationProperties(prefix = "identity-provider")
public record IdentityProviderProperties(String url,
                                         String clientSecret,
                                         String clientId, String username,
                                         String password) {
    @ConstructorBinding
    public IdentityProviderProperties(String url, String clientSecret, String clientId, String username, String password) {
        this.url = Optional.ofNullable(url)
                .orElse("http://localhost:9099/realms/wallet/protocol/openid-connect/token");
        this.clientSecret = Optional.ofNullable(clientSecret)
                .orElse("fV51P8jFBo8VnFKMMuP3imw3H3i5mNck");
        this.clientId = Optional.ofNullable(clientId)
                .orElse("user-registry-client");
        this.username = Optional.ofNullable(username)
                .orElse("userWallet");
        this.password = Optional.ofNullable(password)
                .orElse("userPass");
    }
}
