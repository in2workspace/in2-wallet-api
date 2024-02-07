package es.in2.wallet.vault.config.properties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.Optional;

@Slf4j
@ConfigurationProperties(prefix = "vault")
public record VaultProperties(@NestedConfigurationProperty VaultProvider provider) {

    @ConstructorBinding
    public VaultProperties(VaultProvider provider) {
        this.provider = Optional.ofNullable(provider).orElse(new VaultProvider(null));
    }

}
