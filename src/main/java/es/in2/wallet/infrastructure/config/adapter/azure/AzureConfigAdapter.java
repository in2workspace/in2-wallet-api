package es.in2.wallet.infrastructure.config.adapter.azure;

import com.azure.data.appconfiguration.ConfigurationClient;
import es.in2.wallet.infrastructure.config.adapter.azure.config.properties.AzureProperties;
import es.in2.wallet.infrastructure.config.model.ConfigProviderName;
import es.in2.wallet.infrastructure.config.service.GenericConfigAdapter;
import es.in2.wallet.infrastructure.config.util.ConfigSourceNameAnnotation;
import org.springframework.stereotype.Component;

@Component
@ConfigSourceNameAnnotation(name = ConfigProviderName.AZURE)
public class AzureConfigAdapter implements GenericConfigAdapter {
    private final ConfigurationClient configurationClient;
    private final AzureProperties azureProperties;

    public AzureConfigAdapter(ConfigurationClient configurationClient, AzureProperties azureProperties) {
        this.configurationClient = configurationClient;
        this.azureProperties = azureProperties;
    }

    @Override
    public String getConfiguration(String key){
        return getConfigurationValue(key);
    }

    private String getConfigurationValue(String key) {
        return configurationClient.getConfigurationSetting(key, azureProperties.label().global()).getValue();
    }
}
