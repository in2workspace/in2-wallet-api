package es.in2.wallet.infrastructure.vault.adapter.hashicorp.config;

import es.in2.wallet.infrastructure.vault.model.VaultProviderEnum;
import es.in2.wallet.infrastructure.vault.util.VaultProviderAnnotation;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractReactiveVaultConfiguration;

@Component
@RequiredArgsConstructor
@VaultProviderAnnotation(provider = VaultProviderEnum.HASHICORP)
public class HashicorpKeyVaultConfig extends AbstractReactiveVaultConfiguration {

    private final HashicorpConfig hashicorpConfig;

    @Override
    @NonNull
    public VaultEndpoint vaultEndpoint() {
        VaultEndpoint vaultEndpoint = new VaultEndpoint();

        vaultEndpoint.setHost(hashicorpConfig.getVaultHost());
        vaultEndpoint.setPort(hashicorpConfig.getVaultPort());
        vaultEndpoint.setScheme(hashicorpConfig.getVaultScheme());

        return vaultEndpoint;
    }

    @Override
    @NonNull
    public ClientAuthentication clientAuthentication() {
        return new TokenAuthentication(hashicorpConfig.getVaultToken());
    }

}
