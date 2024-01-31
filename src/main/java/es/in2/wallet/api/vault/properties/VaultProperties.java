package es.in2.wallet.api.vault.properties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.Optional;

@Slf4j
@ConfigurationProperties(prefix = "vault")
public record VaultProperties(@NestedConfigurationProperty SecretProvider secretProvider) {

    @ConstructorBinding
    public VaultProperties(SecretProvider secretProvider) {
        this.secretProvider = Optional.ofNullable(secretProvider).orElse(new SecretProvider(null));
    }
}
