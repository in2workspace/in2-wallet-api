package es.in2.wallet.infrastructure.appconfiguration.service;


import es.in2.wallet.application.ports.AppConfig;
import es.in2.wallet.infrastructure.appconfiguration.util.ConfigAdapterFactory;
import es.in2.wallet.infrastructure.core.config.properties.AuthServerProperties;
import es.in2.wallet.infrastructure.core.config.properties.CorsProperties;
import es.in2.wallet.infrastructure.ebsi.config.properties.EbsiProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static es.in2.wallet.domain.utils.ApplicationConstants.*;
import static es.in2.wallet.domain.utils.ApplicationUtils.formatUrl;

@Configuration
@Slf4j
public class AppConfigImpl implements AppConfig {

    private final GenericConfigAdapter genericConfigAdapter;
    private final AuthServerProperties authServerProperties;
    private final CorsProperties corsProperties;
    private final EbsiProperties ebsiProperties;

    private String authServerInternalUrl;
    private String authServerExternalUrl;
    private String authServerTokenEndpoint;

    @PostConstruct
    public void init() {
        authServerInternalUrl = initAuthServerInternalUrl();
        authServerExternalUrl = initAuthServerExternalUrl();
        authServerTokenEndpoint = initAuthServerTokenEndpoint();
        log.debug(authServerExternalUrl);
        log.debug(authServerInternalUrl);
    }

    public AppConfigImpl(ConfigAdapterFactory configAdapterFactory,
                         AuthServerProperties authServerProperties,
                         CorsProperties corsProperties,
                         EbsiProperties ebsiProperties) {
        this.genericConfigAdapter = configAdapterFactory.getAdapter();
        this.authServerProperties = authServerProperties;
        this.corsProperties = corsProperties;
        this.ebsiProperties = ebsiProperties;
        log.debug(ebsiProperties.url());
    }


    @Override
    public List<String> getCorsAllowedOrigins() {
        return corsProperties.allowedOrigins().stream()
                .map(url -> {
                    try {
                        log.debug("CORS URL: {}", url);
                        URI uri = new URI(url);
                        String domain = "localhost".equalsIgnoreCase(uri.getHost()) ?
                                uri.getHost() :
                                genericConfigAdapter.getConfiguration(uri.getHost());
                        return formatUrl(uri.getScheme(), domain, uri.getPort(), null);
                    } catch (URISyntaxException e) {
                        log.error("Error processing CORS URL: {}", url, e);
                        throw new IllegalArgumentException("Invalid CORS URL: " + url, e);
                    }
                })
                .toList();
    }

    @Override
    public String getAuthServerInternalUrl() {
        return authServerInternalUrl;
    }


    private String initAuthServerInternalUrl() {
        return formatUrl(AUTH_SERVER_INTERNAL_URL_SCHEME,
                genericConfigAdapter.getConfiguration(authServerProperties.internalUrl().domain()),
                AUTH_SERVER_INTERNAL_URL_PORT,
                authServerProperties.internalUrl().path());
    }

    @Override
    public String getAuthServerExternalUrl() {
        return authServerExternalUrl;
    }

    private String initAuthServerExternalUrl() {
        return formatUrl(AUTH_SERVER_EXTERNAL_URL_SCHEME,
                genericConfigAdapter.getConfiguration(authServerProperties.externalUrl().domain()),
                AUTH_SERVER_EXTERNAL_URL_PORT,
                authServerProperties.externalUrl().path());
    }

    @Override
    public String getAuthServerTokenEndpoint() {
        return authServerTokenEndpoint;
    }

    private String initAuthServerTokenEndpoint() {
        return formatUrl(authServerProperties.tokenUrl().scheme(),
                genericConfigAdapter.getConfiguration(authServerProperties.tokenUrl().domain()),
                authServerProperties.tokenUrl().port(),
                authServerProperties.tokenUrl().path());
    }

    @Override
    public String getIdentityProviderUrl() {
        return ebsiProperties.url();
    }

    @Override
    public String getIdentityProviderUsername() {
        return ebsiProperties.username();
    }

    @Override
    public String getIdentityProviderPassword() {
        return ebsiProperties.password();
    }

    @Override
    public String getIdentityProviderClientId() {
        return ebsiProperties.clientId();
    }

    @Override
    public String getIdentityProviderClientSecret() {
        return ebsiProperties.clientSecret();
    }

    private String getAuthServerJwtDecoderPath() {
        return authServerProperties.jwtDecoderPath();
    }
    @Override
    public String getJwtDecoder() {
        return getAuthServerInternalUrl() + getAuthServerJwtDecoderPath();
    }

}
