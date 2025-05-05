package es.in2.wallet.infrastructure.vault.adapter.hashicorp.config;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractReactiveVaultConfiguration;

import java.net.MalformedURLException;
import java.net.URL;

@Component
@RequiredArgsConstructor
public class HashicorpKeyVaultConfig extends AbstractReactiveVaultConfiguration {

    private final HashicorpConfig hashicorpConfig;


    @Override
    @NonNull
    public VaultEndpoint vaultEndpoint() {
        String urlStr = hashicorpConfig.getVaultUrl();
        try {
            URL url = new URL(urlStr);

            VaultEndpoint vaultEndpoint = new VaultEndpoint();
            vaultEndpoint.setHost(url.getHost());

            int port = url.getPort();
            if (port == -1) {
                port = url.getProtocol().equals("https") ? 443 : 80;
            }
            vaultEndpoint.setPort(port);

            vaultEndpoint.setScheme(url.getProtocol());
            return vaultEndpoint;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid Vault URL: " + urlStr, e);
        }
    }

    @Override
    @NonNull
    public ClientAuthentication clientAuthentication() {
        String token = hashicorpConfig.getVaultToken();
        return new TokenAuthentication(token);
    }

}
