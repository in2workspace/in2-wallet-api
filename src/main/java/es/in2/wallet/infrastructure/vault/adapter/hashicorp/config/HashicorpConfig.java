package es.in2.wallet.infrastructure.vault.adapter.hashicorp.config;

import es.in2.wallet.infrastructure.appconfiguration.service.GenericConfigAdapter;
import es.in2.wallet.infrastructure.appconfiguration.util.ConfigAdapterFactory;
import es.in2.wallet.infrastructure.vault.adapter.hashicorp.config.properties.HashicorpProperties;
import es.in2.wallet.infrastructure.vault.model.VaultProviderEnum;
import es.in2.wallet.infrastructure.vault.util.VaultProviderAnnotation;
import org.springframework.stereotype.Component;

import static es.in2.wallet.domain.utils.ApplicationConstants.VAULT_HASHICORP_PATH;

import java.util.Base64;

@Component
@VaultProviderAnnotation(provider = VaultProviderEnum.HASHICORP)
public class HashicorpConfig {
    private final GenericConfigAdapter genericConfigAdapter;
    private final HashicorpProperties hashicorpProperties;

    public HashicorpConfig(ConfigAdapterFactory configAdapterFactory, HashicorpProperties hashicorpProperties) {
        this.genericConfigAdapter = configAdapterFactory.getAdapter();
        this.hashicorpProperties = hashicorpProperties;
    }


    public String getSecretPath() {
        String secretPath = genericConfigAdapter.getConfiguration(VAULT_HASHICORP_PATH);
        return secretPath;
    }

    public String getVaultUrl() {
        String vaultUrl = genericConfigAdapter.getConfiguration(hashicorpProperties.url());
        return vaultUrl;
    }

    public String getVaultToken() {
        String rawToken = hashicorpProperties.token();
        String decodedToken = decodeIfBase64(rawToken);
        return decodedToken;
    }

    private String decodeIfBase64(String token) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(token);
            return new String(decodedBytes).trim();
        } catch (IllegalArgumentException ex) {
            return token.trim();
        }
    }
}
