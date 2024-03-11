package es.in2.wallet.infrastructure.vault.adapter.azure.config;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretAsyncClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import es.in2.wallet.infrastructure.vault.model.VaultProviderEnum;
import es.in2.wallet.infrastructure.vault.util.VaultProviderAnnotation;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@VaultProviderAnnotation(provider = VaultProviderEnum.AZURE)
public class AzureKeyVaultConfig {

    private final AzureConfig azureConfig;

    @Bean
    public SecretAsyncClient secretClient() {
        return new SecretClientBuilder()
                .vaultUrl(azureConfig.getKeyVaultUrl())
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildAsyncClient();
    }

}
