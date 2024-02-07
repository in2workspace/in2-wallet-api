package es.in2.wallet.vault.config.properties.azure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.Optional;

@ConfigurationProperties("spring.cloud.azure.keyvault")
public record AzKeyVaultProperties(@NestedConfigurationProperty AzSecret secret) {

    @ConstructorBinding
    public AzKeyVaultProperties(AzSecret secret) {
        this.secret = Optional.ofNullable(secret).orElse(new AzSecret(null));
    }

}
