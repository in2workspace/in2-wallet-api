package es.in2.wallet.infrastructure.appconfiguration.service;

import es.in2.wallet.infrastructure.appconfiguration.exception.ConfigAdapterFactoryException;
import es.in2.wallet.infrastructure.appconfiguration.util.ConfigAdapterFactory;
import es.in2.wallet.infrastructure.core.config.properties.AuthServerProperties;
import es.in2.wallet.infrastructure.core.config.properties.CorsProperties;
import es.in2.wallet.infrastructure.ebsi.config.properties.EbsiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private AppConfigImpl appConfig;

    @BeforeEach
    void setUp() {
        when(configAdapterFactory.getAdapter()).thenReturn(genericConfigAdapter);
        when(authServerProperties.internalUrl()).thenReturn("https://internal.example.com/internal");
        when(authServerProperties.externalUrl()).thenReturn("https://external.example.com/external");

        appConfig = new AppConfigImpl(configAdapterFactory, authServerProperties, corsProperties, ebsiProperties);
        appConfig.init();
    }

    @Test
    void testGetCorsAllowedOrigins() {
        when(corsProperties.allowedOrigins()).thenReturn(List.of("https://localhost:443", "http://localhost:8080"));

        List<String> origins = appConfig.getCorsAllowedOrigins();

        assertThat(origins).containsExactly("https://localhost", "http://localhost:8080");
    }

    @Test
    void testGetCorsAllowedOriginsWithMalformedUrl() {
        when(corsProperties.allowedOrigins()).thenReturn(List.of("ht!tp://invalid-url"));

        assertThatThrownBy(appConfig::getCorsAllowedOrigins)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid CORS URL");
    }

    @Test
    void testGetIdentityProviderProperties() {
        when(ebsiProperties.url()).thenReturn("https://ebsi.example.com");
        when(ebsiProperties.clientId()).thenReturn("clientId");
        when(ebsiProperties.clientSecret()).thenReturn("clientSecret");
        when(ebsiProperties.username()).thenReturn("username");
        when(ebsiProperties.password()).thenReturn("password");

        assertThat(appConfig.getIdentityProviderUrl()).isEqualTo("https://ebsi.example.com");
        assertThat(appConfig.getIdentityProviderClientId()).isEqualTo("clientId");
        assertThat(appConfig.getIdentityProviderClientSecret()).isEqualTo("clientSecret");
        assertThat(appConfig.getIdentityProviderUsername()).isEqualTo("username");
        assertThat(appConfig.getIdentityProviderPassword()).isEqualTo("password");
    }

    @Test
    void testGetAuthServerInternalUrl() {
        String internalUrl = appConfig.getAuthServerInternalUrl();

        assertThat(internalUrl).isEqualTo("https://internal.example.com/internal");
    }

    @Test
    void testGetAuthServerExternalUrl() {
        String externalUrl = appConfig.getAuthServerExternalUrl();

        assertThat(externalUrl).isEqualTo("https://external.example.com/external");
    }

    @Test
    void testGetJwtDecoder() {
        String jwtDecoderUrl = appConfig.getJwtDecoder();

        assertThat(jwtDecoderUrl).isEqualTo("https://internal.example.com/internal" + AUTH_SERVER_JWT_DECODER_PATH);
    }

    @Test
    void testConfigAdapterFactoryThrowsExceptionWhenMultipleAdapters() {
        ConfigAdapterFactory factory = new ConfigAdapterFactory(List.of(mock(GenericConfigAdapter.class), mock(GenericConfigAdapter.class)));

        assertThatThrownBy(factory::getAdapter)
                .isInstanceOf(ConfigAdapterFactoryException.class)
                .hasMessageContaining("2");
    }

    @Test
    void testConfigAdapterFactoryThrowsExceptionWhenNoAdapter() {
        ConfigAdapterFactory factory = new ConfigAdapterFactory(List.of());

        assertThatThrownBy(factory::getAdapter)
                .isInstanceOf(ConfigAdapterFactoryException.class)
                .hasMessageContaining("0");
    }
}
