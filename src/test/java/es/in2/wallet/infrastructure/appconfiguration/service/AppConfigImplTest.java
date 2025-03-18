package es.in2.wallet.infrastructure.appconfiguration.service;

import es.in2.wallet.infrastructure.appconfiguration.util.ConfigAdapterFactory;
import es.in2.wallet.infrastructure.core.config.properties.AuthServerProperties;
import es.in2.wallet.infrastructure.core.config.properties.CorsProperties;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppConfigImplTest {
    @Mock
    private ConfigAdapterFactory configAdapterFactory;
    @Mock
    private AuthServerProperties authServerProperties;
    @Mock
    private CorsProperties corsProperties;
    @Mock
    private EbsiProperties ebsiProperties;

    @InjectMocks
    private AppConfigImpl appConfig;

    @BeforeEach
    void setUp() {
        when(configAdapterFactory.getAdapter()).thenReturn(url -> url);

        appConfig = new AppConfigImpl(configAdapterFactory, authServerProperties, corsProperties, ebsiProperties);
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
}
