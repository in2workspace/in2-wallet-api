package es.in2.wallet.infrastructure.vault.adapter.azure.config;

import es.in2.wallet.infrastructure.appconfiguration.service.GenericConfigAdapter;
import es.in2.wallet.infrastructure.appconfiguration.util.ConfigAdapterFactory;
import es.in2.wallet.infrastructure.vault.adapter.azure.config.properties.AzureKeyVaultProperties;
import es.in2.wallet.infrastructure.vault.model.VaultProviderEnum;
import es.in2.wallet.infrastructure.vault.util.VaultProviderAnnotation;
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
