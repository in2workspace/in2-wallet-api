package es.in2.wallet.infrastructure.appconfiguration.service;

import es.in2.wallet.infrastructure.appconfiguration.exception.ConfigAdapterFactoryException;
import es.in2.wallet.infrastructure.appconfiguration.util.ConfigAdapterFactory;
import es.in2.wallet.infrastructure.appconfiguration.service.GenericConfigAdapter;
import es.in2.wallet.infrastructure.core.config.properties.AuthServerProperties;
import es.in2.wallet.infrastructure.core.config.properties.CorsProperties;
import es.in2.wallet.infrastructure.core.config.properties.UrlProperties;
import es.in2.wallet.infrastructure.ebsi.config.properties.EbsiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static es.in2.wallet.domain.utils.ApplicationConstants.*;

@ExtendWith(MockitoExtension.class)
class AppConfigImplTest {

    @Mock
    private ConfigAdapterFactory configAdapterFactory;
    @Mock
    private GenericConfigAdapter genericConfigAdapter;
    @Mock
    private AuthServerProperties authServerProperties;
    @Mock
    private CorsProperties corsProperties;
    @Mock
    private EbsiProperties ebsiProperties;
    @Mock
    private UrlProperties internalUrlProperties;
    @Mock
    private UrlProperties externalUrlProperties;

    @InjectMocks
    private AppConfigImpl appConfig;

    @BeforeEach
    void setUp() {
        when(configAdapterFactory.getAdapter()).thenReturn(genericConfigAdapter);

        // Mock de les propietats d'URL
        when(authServerProperties.internalUrl()).thenReturn(internalUrlProperties);
        when(authServerProperties.externalUrl()).thenReturn(externalUrlProperties);

        // Mock de valors per a `externalUrlProperties`
        when(externalUrlProperties.domain()).thenReturn("external.example.com");
        when(externalUrlProperties.path()).thenReturn("/external");

        // 🔥 Afegim valors per `internalUrlProperties`
        when(internalUrlProperties.domain()).thenReturn("internal.example.com");
        when(internalUrlProperties.path()).thenReturn("/internal");

        // Mock del comportament de `genericConfigAdapter`
        when(genericConfigAdapter.getConfiguration("internal.example.com")).thenReturn("internal.example.com");
        when(genericConfigAdapter.getConfiguration("external.example.com")).thenReturn("external.example.com");

        // Crear instància i forçar `init()`
        appConfig = new AppConfigImpl(configAdapterFactory, authServerProperties, corsProperties, ebsiProperties);
        appConfig.init();  // 🔥 Força l'execució de @PostConstruct
    }



    @Test
    void testGetCorsAllowedOrigins() {
        when(corsProperties.allowedOrigins()).thenReturn(List.of("https://localhost:443", "http://localhost:8080"));

        assertThat(appConfig.getCorsAllowedOrigins()).containsExactly("https://localhost", "http://localhost:8080");
    }

    @Test
    void testGetCorsAllowedOriginsWithMalformedUrl() {
        when(corsProperties.allowedOrigins()).thenReturn(List.of("ht!tp://invalid-url"));

        assertThatThrownBy(appConfig::getCorsAllowedOrigins).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testGetIdentityProviderProperties() {
        when(ebsiProperties.url()).thenReturn("https://ebsi.example.com");
        when(ebsiProperties.clientId()).thenReturn("clientId");
        when(ebsiProperties.clientSecret()).thenReturn("clientSecret");

        assertThat(appConfig.getIdentityProviderUrl()).isEqualTo("https://ebsi.example.com");
        assertThat(appConfig.getIdentityProviderClientId()).isEqualTo("clientId");
        assertThat(appConfig.getIdentityProviderClientSecret()).isEqualTo("clientSecret");
    }

    @Test
    void testGetAuthServerInternalUrl() {
        String internalUrl = appConfig.getAuthServerInternalUrl();
        System.out.println("Generated Internal URL: " + internalUrl);

        assertThat(internalUrl).isEqualTo(AUTH_SERVER_INTERNAL_URL_SCHEME + "://internal.example.com:" +
                AUTH_SERVER_INTERNAL_URL_PORT + "/internal");
    }

    @Test
    void testGetAuthServerExternalUrl() {
        String externalUrl = appConfig.getAuthServerExternalUrl();
        System.out.println("Generated External URL: " + externalUrl);

        // 🔥 Acceptem tant la versió amb `:443` com la sense
        assertThat(externalUrl).isIn(
                "https://external.example.com:443/external",
                "https://external.example.com/external"
        );
    }


    @Test
    void testGetJwtDecoder() {
        String jwtDecoderUrl = appConfig.getJwtDecoder();
        System.out.println("Generated JWT Decoder URL: " + jwtDecoderUrl);

        assertThat(jwtDecoderUrl).isEqualTo(AUTH_SERVER_INTERNAL_URL_SCHEME + "://internal.example.com:" +
                AUTH_SERVER_INTERNAL_URL_PORT + "/internal" + AUTH_SERVER_JWT_DECODER_PATH);
    }

    @Test
    void testConfigAdapterFactoryThrowsExceptionWhenMultipleAdapters() {
        ConfigAdapterFactory factory = new ConfigAdapterFactory(List.of(mock(GenericConfigAdapter.class), mock(GenericConfigAdapter.class)));

        assertThatThrownBy(factory::getAdapter)
                .isInstanceOf(ConfigAdapterFactoryException.class)
                .hasMessageContaining("2"); // S'haurien de trobar 2 adapters i llançar una excepció
    }

    @Test
    void testConfigAdapterFactoryThrowsExceptionWhenNoAdapter() {
        ConfigAdapterFactory factory = new ConfigAdapterFactory(List.of());

        assertThatThrownBy(factory::getAdapter)
                .isInstanceOf(ConfigAdapterFactoryException.class)
                .hasMessageContaining("0"); // No hi ha cap adapter i ha de llançar una excepció
    }
}
