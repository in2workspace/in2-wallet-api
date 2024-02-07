package es.in2.wallet.vault.properties.azure;


import com.azure.data.appconfiguration.ConfigurationClient;
import com.azure.data.appconfiguration.ConfigurationClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AzureVaultProperties {

    private final AzureVaultConfigProperties azureAppConfigProperties;
    private final AzureVaultKeyProperties azureKeyVaultProperties;

    @Bean
    @ConditionalOnProperty(name = "vault.secret-provider.name", havingValue = "azure")
    public ConfigurationClient configurationClient() {
        return new ConfigurationClientBuilder()
                .endpoint(azureAppConfigProperties.endpoint())
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }

    @Bean
    @ConditionalOnProperty(name = "vault.secret-provider.name", havingValue = "azure")
    public SecretClient secretClient() {
        return new SecretClientBuilder()
                .vaultUrl(azureKeyVaultProperties.secret().endpoint())
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }

}
