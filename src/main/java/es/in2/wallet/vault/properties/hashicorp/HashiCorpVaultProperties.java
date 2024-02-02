package es.in2.wallet.vault.properties.hashicorp;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class HashiCorpVaultProperties {

    private final HashiCorpVaultConfigProperties hashiCorpVaultConfigProperties;

    @Bean
    @ConditionalOnProperty(name = "vault.secret-provider.name", havingValue = "hashicorp")
    public VaultProperties vaultProperties() {
        VaultProperties vaultProperties = new VaultProperties();
        vaultProperties.setAuthentication(VaultProperties.AuthenticationMethod .TOKEN);
        vaultProperties.setToken(hashiCorpVaultConfigProperties.token());
        vaultProperties.setHost(hashiCorpVaultConfigProperties.host());
        vaultProperties.setPort(hashiCorpVaultConfigProperties.port());
        vaultProperties.setScheme(hashiCorpVaultConfigProperties.scheme());
        return vaultProperties;
    }

}
