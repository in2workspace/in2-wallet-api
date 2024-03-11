package es.in2.wallet.vault.adapter.azure.config;

import es.in2.wallet.configuration.service.GenericConfigAdapter;
import es.in2.wallet.configuration.util.ConfigAdapterFactory;
import es.in2.wallet.vault.adapter.azure.config.properties.AzureKeyVaultProperties;
import es.in2.wallet.vault.model.provider.VaultProviderEnum;
import es.in2.wallet.vault.util.VaultProviderAnnotation;
import org.springframework.stereotype.Component;

@Component
@VaultProviderAnnotation(provider = VaultProviderEnum.AZURE)
public class AzureConfig {
    private final GenericConfigAdapter genericConfigAdapter;
    private final AzureKeyVaultProperties azureKeyVaultProperties;

    public AzureConfig(ConfigAdapterFactory configAdapterFactory, AzureKeyVaultProperties azureKeyVaultProperties){
        this.genericConfigAdapter = configAdapterFactory.getAdapter();
        this.azureKeyVaultProperties = azureKeyVaultProperties;
    }

    public String getKeyVaultUrl() {
        return genericConfigAdapter.getConfiguration(azureKeyVaultProperties.endpoint());
    }

}
