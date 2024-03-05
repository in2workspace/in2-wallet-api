package es.in2.wallet.api.config;


import es.in2.wallet.api.config.properties.AuthServerProperties;
import es.in2.wallet.api.config.properties.WalletDrivingApplicationProperties;
import es.in2.wallet.api.ebsi.comformance.config.properties.IdentityProviderProperties;
import es.in2.wallet.configuration.service.GenericConfigAdapter;
import es.in2.wallet.configuration.util.ConfigAdapterFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    private final GenericConfigAdapter genericConfigAdapter;
    private final AuthServerProperties authServerProperties;
    private final WalletDrivingApplicationProperties walletDrivingApplicationProperties;
    private final IdentityProviderProperties identityProviderProperties;


    // Variable for caching the configuration
    private String authServerInternalUrl;
    private String authServerExternalUrl;
    private String authServerTokenEndpoint;

    @PostConstruct
    public void init() {
        authServerInternalUrl = initAuthServerInternalUrl();
        authServerExternalUrl = initAuthServerExternalUrl();
        authServerTokenEndpoint = initAuthServerTokenEndpoint();
    }


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
        //TODO: Change to get from config when azure app configuration variable is created
        return walletDrivingApplicationProperties.url();
    }

    public String getAuthServerInternalUrl() {
        return authServerInternalUrl;
    }

    private String initAuthServerInternalUrl() {
        if (authServerProperties.internalUrl().port() == 443) {
            return String.format("%s://%s%s",
                    authServerProperties.internalUrl().scheme(),
                    genericConfigAdapter.getConfiguration(authServerProperties.internalUrl().domain()),
                    authServerProperties.internalUrl().path());
        }

        return  String.format("%s://%s:%d%s",
                authServerProperties.internalUrl().scheme(),
                genericConfigAdapter.getConfiguration(authServerProperties.internalUrl().domain()),
                authServerProperties.internalUrl().port(),
                authServerProperties.internalUrl().path());
    }

    public String getAuthServerExternalUrl() {
        return authServerExternalUrl;
    }

    private String initAuthServerExternalUrl() {
        if (authServerProperties.externalUrl().port() == 443) {
            return String.format("%s://%s%s",
                    authServerProperties.externalUrl().scheme(),
                    genericConfigAdapter.getConfiguration(authServerProperties.externalUrl().domain()),
                    authServerProperties.externalUrl().path());
        }

        return  String.format("%s://%s:%d%s",
                authServerProperties.externalUrl().scheme(),
                genericConfigAdapter.getConfiguration(authServerProperties.externalUrl().domain()),
                authServerProperties.externalUrl().port(),
                authServerProperties.externalUrl().path());
    }

    public String getAuthServerTokenEndpoint() {
        return authServerTokenEndpoint;
    }

    private String initAuthServerTokenEndpoint() {
        if (authServerProperties.tokenUrl().port() == 443) {
            return String.format("%s://%s%s",
                    authServerProperties.tokenUrl().scheme(),
                    genericConfigAdapter.getConfiguration(authServerProperties.tokenUrl().domain()),
                    authServerProperties.tokenUrl().path());
        }

        return String.format("%s://%s:%d%s",
                authServerProperties.tokenUrl().scheme(),
                genericConfigAdapter.getConfiguration(authServerProperties.tokenUrl().domain()),
                authServerProperties.tokenUrl().port(),
                authServerProperties.tokenUrl().path());
    }

    public String getIdentityProviderUrl() {
        //TODO: Change to get from config when azure app configuration variable is created
        return identityProviderProperties.url();
    }

    public String getIdentityProviderUsername() {
        //TODO: Change to get from config when azure app configuration variable is created
        return identityProviderProperties.username();
    }

    public String getIdentityProviderPassword() {
        //TODO: Change to get from config when azure app configuration variable is created
        return identityProviderProperties.password();
    }

    public String getIdentityProviderClientId() {
        //TODO: Change to get from config when azure app configuration variable is created
        return identityProviderProperties.clientId();
    }

    public String getIdentityProviderClientSecret() {
        //TODO: Change to get from config when azure app configuration variable is created
        return identityProviderProperties.clientSecret();
    }
}
