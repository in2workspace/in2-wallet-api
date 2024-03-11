package es.in2.wallet.infrastructure.config.adapter.azure.config;

import com.azure.core.credential.TokenCredential;
import com.azure.data.appconfiguration.ConfigurationClient;
import com.azure.data.appconfiguration.ConfigurationClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import es.in2.wallet.infrastructure.config.adapter.azure.config.properties.AzureProperties;
import es.in2.wallet.infrastructure.config.model.ConfigProviderName;
import es.in2.wallet.infrastructure.config.util.ConfigSourceNameAnnotation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
@ConfigSourceNameAnnotation(name = ConfigProviderName.AZURE)
public class AzureAppConfigurationConfig {

    private final AzureProperties azureProperties;

    @Bean
    public TokenCredential azureTokenCredential() {
        TokenCredential credential = new DefaultAzureCredentialBuilder().build();
        log.info("Token Credential: {}", credential);
        return credential;
    }

    @Bean
    public ConfigurationClient azureConfigurationClient(TokenCredential azureTokenCredential) {
        log.info("ENDPOINT --> {}", azureProperties.endpoint());

        return new ConfigurationClientBuilder()
                .credential(azureTokenCredential)
                .endpoint(azureProperties.endpoint())
                .buildClient();
    }
}
