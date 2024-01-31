package es.in2.wallet.api.vault.properties.azure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.Optional;

@ConfigurationProperties("spring.cloud.azure.keyvault")
public record AzureVaultKeyProperties(@NestedConfigurationProperty Secret secret) {

    @ConstructorBinding
    public AzureVaultKeyProperties(Secret secret) {
        this.secret = Optional.ofNullable(secret).orElse(new Secret(null));
    }

}
