package es.in2.wallet.api.config;


import es.in2.wallet.api.config.properties.AuthServerProperties;
import es.in2.wallet.api.config.properties.WalletDrivingApplicationProperties;
import es.in2.wallet.api.ebsi.comformance.config.properties.IdentityProviderProperties;
import es.in2.wallet.configuration.service.GenericConfigAdapter;
import es.in2.wallet.configuration.util.ConfigAdapterFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    private final GenericConfigAdapter genericConfigAdapter;
    private final AuthServerProperties authServerProperties;
    private final WalletDrivingApplicationProperties walletDrivingApplicationProperties;
    private final IdentityProviderProperties identityProviderProperties;


    public AppConfig(ConfigAdapterFactory configAdapterFactory,
                     AuthServerProperties authServerProperties,
                     WalletDrivingApplicationProperties walletDrivingApplicationProperties,
                     IdentityProviderProperties identityProviderProperties) {
        this.genericConfigAdapter = configAdapterFactory.getAdapter();
        this.authServerProperties = authServerProperties;
        this.walletDrivingApplicationProperties = walletDrivingApplicationProperties;
        this.identityProviderProperties = identityProviderProperties;
    }

    public String getWalletDrivingUrl() {
        return genericConfigAdapter.getConfiguration(walletDrivingApplicationProperties.url());
    }

    public String getAuthServerInternalDomain() {
        return genericConfigAdapter.getConfiguration(authServerProperties.internalDomain());
    }

    public String getAuthServerExternalDomain() {
        return genericConfigAdapter.getConfiguration(authServerProperties.externalDomain());
    }

    public String getAuthServerTokenEndpoint() {
        return genericConfigAdapter.getConfiguration(authServerProperties.tokenEndpoint());
    }

    public String getIdentityProviderUrl() {
        return genericConfigAdapter.getConfiguration(identityProviderProperties.url());
    }

    public String getIdentityProviderUsername() {
        return genericConfigAdapter.getConfiguration(identityProviderProperties.username());
    }

    public String getIdentityProviderPassword() {
        return genericConfigAdapter.getConfiguration(identityProviderProperties.password());
    }

    public String getIdentityProviderClientId() {
        return genericConfigAdapter.getConfiguration(identityProviderProperties.clientId());
    }

    public String getIdentityProviderClientSecret() {
        return genericConfigAdapter.getConfiguration(identityProviderProperties.clientSecret());
    }
}
