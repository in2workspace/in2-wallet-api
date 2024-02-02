package es.in2.wallet.vault.util;

import es.in2.wallet.vault.adapter.AzureAdapter;
import es.in2.wallet.vault.adapter.HashicorpAdapter;
import es.in2.wallet.vault.properties.VaultProperties;
import es.in2.wallet.vault.service.GenericVaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VaultFactory {
    private final VaultProperties vaultProperties;
    private final ObjectProvider<HashicorpAdapter> hashicorpAdapterProvider;
    private final ObjectProvider<AzureAdapter> azureAdapterProvider;

    public GenericVaultService getVaultAdapter() {
        switch (vaultProperties.secretProvider().name()) {
            case "hashicorp":
                HashicorpAdapter hashicorpAdapter = hashicorpAdapterProvider.getIfAvailable();
                if (hashicorpAdapter == null) {
                    throw new IllegalStateException("HashicorpAdapter is not available.");
                }
                return hashicorpAdapter;
            case "azure":
                AzureAdapter azureAdapter = azureAdapterProvider.getIfAvailable();
                if (azureAdapter == null) {
                    throw new IllegalStateException("AzureAdapter is not available.");
                }
                return azureAdapter;
            default:
                throw new IllegalArgumentException("Invalid Vault provider: " + vaultProperties.secretProvider().name());
        }
    }
}


