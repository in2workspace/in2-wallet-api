package es.in2.wallet.infrastructure.appconfiguration.config;

import com.azure.core.credential.TokenCredential;
import com.azure.data.appconfiguration.ConfigurationClient;
import com.azure.data.appconfiguration.ConfigurationClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import es.in2.wallet.infrastructure.appconfiguration.model.ConfigProviderName;
import es.in2.wallet.infrastructure.appconfiguration.util.ConfigSourceNameAnnotation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConfigSourceNameAnnotation(name = ConfigProviderName.AZURE)
public class AzureAppConfigurationConfig {

    private final AzureAppConfigurationProperties azureAppConfigurationProperties;

    @Bean
    public TokenCredential azureTokenCredential() {
        TokenCredential credential = new DefaultAzureCredentialBuilder().build();
        log.debug("AZ Token Credential: {}", credential);
        return credential;
    }

    @Bean
    public ConfigurationClient azureConfigurationClient(TokenCredential azureTokenCredential) {
        log.debug("AZ AppConfiguration endpoint: {}", azureAppConfigurationProperties.endpoint());
        return new ConfigurationClientBuilder().credential(azureTokenCredential)
                .endpoint(azureAppConfigurationProperties.endpoint()).buildClient();
    }

}
