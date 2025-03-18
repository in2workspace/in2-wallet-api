package es.in2.wallet.application.ports;

import java.util.List;

public interface AppConfig {

    List<String> getCorsAllowedOrigins();

    String getJwtDecoder();

    String getAuthServerInternalUrl();

    String getAuthServerExternalUrl();

    String getAuthServerTokenEndpoint();

    String getIdentityProviderUrl();

    String getIdentityProviderUsername();

    String getIdentityProviderPassword();

    String getIdentityProviderClientId();

    String getIdentityProviderClientSecret();

}
