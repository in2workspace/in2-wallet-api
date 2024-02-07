package es.in2.wallet.vault.util;

import es.in2.wallet.vault.adapter.AzKeyVaultAdapter;
import es.in2.wallet.vault.adapter.HashicorpAdapter;
import es.in2.wallet.vault.config.properties.VaultProperties;
import es.in2.wallet.vault.service.GenericVaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VaultFactory {

    private final VaultProperties vaultProperties;
    private final ObjectProvider<HashicorpAdapter> hashicorpAdapterProvider;
    private final ObjectProvider<AzKeyVaultAdapter> azureAdapterProvider;

    public GenericVaultService getVaultAdapter() {
        switch (vaultProperties.provider().name()) {
            case "hashicorp":
                HashicorpAdapter hashicorpAdapter = hashicorpAdapterProvider.getIfAvailable();
                if (hashicorpAdapter == null) {
                    throw new IllegalStateException("HashicorpAdapter is not available.");
                }
                return hashicorpAdapter;
            case "azure":
                AzKeyVaultAdapter azKeyVaultAdapter = azureAdapterProvider.getIfAvailable();
                if (azKeyVaultAdapter == null) {
                    throw new IllegalStateException("AzureAdapter is not available.");
                }
                return azKeyVaultAdapter;
            default:
                throw new IllegalArgumentException("Invalid Vault provider: " + vaultProperties.provider().name());
        }
    }

}


