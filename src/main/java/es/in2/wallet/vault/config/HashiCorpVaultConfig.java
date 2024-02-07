package es.in2.wallet.vault.config;

import es.in2.wallet.vault.config.properties.hashicorp.HashiCorpVaultProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class HashiCorpVaultConfig {

    private final HashiCorpVaultProperties hashiCorpVaultProperties;

    @Bean
    @ConditionalOnProperty(name = "vault.provider.name", havingValue = "hashicorp")
    public VaultProperties vaultProperties() {
        VaultProperties vaultProperties = new VaultProperties();
        vaultProperties.setAuthentication(VaultProperties.AuthenticationMethod .TOKEN);
        vaultProperties.setToken(hashiCorpVaultProperties.token());
        vaultProperties.setHost(hashiCorpVaultProperties.host());
        vaultProperties.setPort(hashiCorpVaultProperties.port());
        vaultProperties.setScheme(hashiCorpVaultProperties.scheme());
        return vaultProperties;
    }

}
