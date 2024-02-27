package es.in2.wallet.vault.config;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretAsyncClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import es.in2.wallet.vault.config.properties.azure.AzKeyVaultProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AzKeyVaultConfig {

    private final AzKeyVaultProperties azureKeyVaultProperties;

    @Bean
    @ConditionalOnProperty(name = "vault.provider.name", havingValue = "azure")
    public SecretAsyncClient secretAsyncClient() {
        return new SecretClientBuilder()
                .vaultUrl(azureKeyVaultProperties.secret().endpoint())
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildAsyncClient();
    }

}
