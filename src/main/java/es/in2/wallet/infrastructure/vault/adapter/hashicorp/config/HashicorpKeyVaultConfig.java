package es.in2.wallet.infrastructure.vault.adapter.hashicorp.config;

import es.in2.wallet.infrastructure.vault.model.VaultProviderEnum;
import es.in2.wallet.infrastructure.vault.util.VaultProviderAnnotation;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(HashicorpKeyVaultConfig.class);

    @Override
    @NonNull
    public VaultEndpoint vaultEndpoint() {
        String host = hashicorpConfig.getVaultHost();
        int port = hashicorpConfig.getVaultPort();
        String scheme = hashicorpConfig.getVaultScheme();

//        todo remove
        logger.info("Initializing VaultEndpoint with host: {}, port: {}, scheme: {}", host, port, scheme);

        VaultEndpoint vaultEndpoint = new VaultEndpoint();
        vaultEndpoint.setHost(host);
        vaultEndpoint.setPort(port);
        vaultEndpoint.setScheme(scheme);

        return vaultEndpoint;
    }

    @Override
    @NonNull
    public ClientAuthentication clientAuthentication() {
        String token = hashicorpConfig.getVaultToken();
        // todo remove
        logger.info("Using Vault token: {}", token != null && !token.isEmpty() ? "[PROVIDED]" : "[MISSING]");
        return new TokenAuthentication(token);
    }

}
