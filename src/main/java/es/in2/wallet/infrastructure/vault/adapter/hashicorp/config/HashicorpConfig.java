package es.in2.wallet.infrastructure.vault.adapter.hashicorp.config;

import es.in2.wallet.infrastructure.appconfiguration.service.GenericConfigAdapter;
import es.in2.wallet.infrastructure.appconfiguration.util.ConfigAdapterFactory;
import es.in2.wallet.infrastructure.vault.adapter.hashicorp.config.properties.HashicorpProperties;
import es.in2.wallet.infrastructure.vault.model.VaultProviderEnum;
import es.in2.wallet.infrastructure.vault.util.VaultProviderAnnotation;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static es.in2.wallet.domain.utils.ApplicationConstants.VAULT_HASHICORP_PATH;

import java.util.Base64;

@Component
@VaultProviderAnnotation(provider = VaultProviderEnum.HASHICORP)
public class HashicorpConfig {
    private final GenericConfigAdapter genericConfigAdapter;
    private final HashicorpProperties hashicorpProperties;
    //todo
    private static final Logger logger = LoggerFactory.getLogger(HashicorpConfig.class);

    public HashicorpConfig(ConfigAdapterFactory configAdapterFactory, HashicorpProperties hashicorpProperties) {
        this.genericConfigAdapter = configAdapterFactory.getAdapter();
        this.hashicorpProperties = hashicorpProperties;
    }


    public String getSecretPath() {
        String secretPath = genericConfigAdapter.getConfiguration(VAULT_HASHICORP_PATH);
        logger.info("Secret Path: {}", secretPath);
        return secretPath;
    }

    public String getVaultHost() {
        String host = genericConfigAdapter.getConfiguration(hashicorpProperties.host());
        logger.info("Vault Host: {}", host);
        return host;
    }

    public int getVaultPort() {
        String portStr = genericConfigAdapter.getConfiguration(hashicorpProperties.port());
        logger.info("Vault Port (raw): {}", portStr);
        return Integer.parseInt(portStr);
    }

    public String getVaultScheme() {
        String scheme = genericConfigAdapter.getConfiguration(hashicorpProperties.scheme());
        logger.info("Vault Scheme: {}", scheme);
        return scheme;
    }

    public String getVaultToken() {
        String rawToken = hashicorpProperties.token();
        String decodedToken = decodeIfBase64(rawToken);
        logger.info("Vault Token (decoded): {}", decodedToken);
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
