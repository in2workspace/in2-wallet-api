package es.in2.wallet.api.config;


import es.in2.wallet.api.config.properties.AuthServerProperties;
import es.in2.wallet.api.config.properties.WalletDrivingApplicationProperties;
import es.in2.wallet.api.ebsi.comformance.config.properties.IdentityProviderProperties;
import es.in2.wallet.configuration.service.GenericConfigAdapter;
import es.in2.wallet.configuration.util.ConfigAdapterFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.List;

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

    public List<String> getWalletDrivingUrls() {
        return walletDrivingApplicationProperties.urls().stream()
                .map(urlProperties -> {
                    String domain = "localhost".equalsIgnoreCase(urlProperties.domain()) ?
                            urlProperties.domain() :
                            genericConfigAdapter.getConfiguration(urlProperties.domain());
                    return formatUrl(urlProperties.scheme(), domain, urlProperties.port());
                })
                .toList();
    }

    public String getAuthServerInternalUrl() {
        return authServerInternalUrl;
    }


    private String initAuthServerInternalUrl() {
        return formatUrl(authServerProperties.internalUrl().scheme(),
                genericConfigAdapter.getConfiguration(authServerProperties.internalUrl().domain()),
                authServerProperties.internalUrl().port(),
                authServerProperties.internalUrl().path());
    }

    public String getAuthServerExternalUrl() {
        return authServerExternalUrl;
    }

    private String initAuthServerExternalUrl() {
        return formatUrl(authServerProperties.externalUrl().scheme(),
                genericConfigAdapter.getConfiguration(authServerProperties.externalUrl().domain()),
                authServerProperties.externalUrl().port(),
                authServerProperties.externalUrl().path());
    }

    public String getAuthServerTokenEndpoint() {
        return authServerTokenEndpoint;
    }

    private String initAuthServerTokenEndpoint() {
        return formatUrl(authServerProperties.tokenUrl().scheme(),
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

    private String formatUrl(String scheme, String domain, int port) {
        if (port == 443) {
            return String.format("%s://%s", scheme, domain);
        }

        return String.format("%s://%s:%d", scheme, domain, port);
    }

    private String formatUrl(String scheme, String domain, int port, String path) {
        if (port == 443) {
            return String.format("%s://%s%s", scheme, domain, path);
        }

        return String.format("%s://%s:%d%s", scheme, domain, port, path);
    }
}
