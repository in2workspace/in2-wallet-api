package es.in2.wallet.infrastructure.appconfiguration.service;

import es.in2.wallet.infrastructure.appconfiguration.util.ConfigAdapterFactory;
import es.in2.wallet.infrastructure.core.config.properties.AuthServerProperties;
import es.in2.wallet.infrastructure.core.config.properties.UrlProperties;
import es.in2.wallet.infrastructure.core.config.properties.CorsProperties;
import es.in2.wallet.infrastructure.ebsi.config.properties.EbsiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppConfigImplTest {
    @Mock
    private GenericConfigAdapter genericConfigAdapter;
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
        MockitoAnnotations.openMocks(this);

        // Mock config properties
        when(authServerProperties.internalUrl()).thenReturn(new UrlProperties("http", "localhost", 8080, "/internal"));
        when(authServerProperties.externalUrl()).thenReturn(new UrlProperties("http", "localhost", 80, "/external"));
        when(authServerProperties.tokenUrl()).thenReturn(new UrlProperties("http", "localhost", 8080, "/token"));

        when(configAdapterFactory.getAdapter()).thenReturn(genericConfigAdapter);
        when(genericConfigAdapter.getConfiguration("localhost")).thenReturn("localhost");


        // Initialize AppConfigImpl
        appConfig = new AppConfigImpl(configAdapterFactory, authServerProperties, corsProperties, ebsiProperties);
        appConfig.init();
    }

    @Test
    void testGetCorsAllowedOrigins() {
        when(corsProperties.allowedOrigins()).thenReturn(List.of(
                "https://localhost:443/external",
                "http://localhost:8080/external"));

        List<String> expectedUrls = Arrays.asList("https://localhost", "http://localhost:8080");
        assertThat(appConfig.getCorsAllowedOrigins()).isEqualTo(expectedUrls);
    }

    @Test
    void testGetCorsAllowedOriginsWithDefaultPort() {
        when(corsProperties.allowedOrigins()).thenReturn(List.of("http://localhost/external"));

        List<String> expectedUrls = List.of("http://localhost");
        assertThat(appConfig.getCorsAllowedOrigins()).isEqualTo(expectedUrls);
    }

    @Test
    void testGetCorsAllowedOriginsWithResolvedDomain() {
        when(corsProperties.allowedOrigins()).thenReturn(List.of("https://example.com:8443/external"));
        when(genericConfigAdapter.getConfiguration("example.com")).thenReturn("resolved-example.com");

        List<String> expectedUrls = List.of("https://resolved-example.com:8443");
        assertThat(appConfig.getCorsAllowedOrigins()).isEqualTo(expectedUrls);
    }

    @Test
    void testGetCorsAllowedOriginsWithEmptyList() {
        when(corsProperties.allowedOrigins()).thenReturn(List.of());

        List<String> expectedUrls = List.of();
        assertThat(appConfig.getCorsAllowedOrigins()).isEqualTo(expectedUrls);
    }

    @Test
    void testGetCorsAllowedOriginsWithMalformedUrl() {
        when(corsProperties.allowedOrigins()).thenReturn(List.of("ht!tp://invalid-url"));

        assertThatThrownBy(() -> appConfig.getCorsAllowedOrigins())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid CORS URL")
                .hasCauseInstanceOf(URISyntaxException.class);
    }


    @Test
    void testGetAuthServerInternalUrl() {
        String expectedEndpoint = "http://localhost:8080/internal";
        assertEquals(expectedEndpoint, appConfig.getAuthServerInternalUrl());
    }

    @Test
    void testGetAuthServerExternalUrl() {
        String expectedEndpoint = "http://localhost:80/external";
        assertEquals(expectedEndpoint, appConfig.getAuthServerExternalUrl());
    }

    @Test
    void testGetAuthServerTokenEndpoint() {
        String expectedEndpoint = "http://localhost:8080/token";
        assertEquals(expectedEndpoint, appConfig.getAuthServerTokenEndpoint());
    }

    @Test
    void testGetIdentityProviderUrl() {
        when(ebsiProperties.url()).thenReturn("https://ebsi.example.com");
        String expectedUrl = "https://ebsi.example.com";
        assertEquals(expectedUrl, appConfig.getIdentityProviderUrl());
    }

    @Test
    void testGetIdentityProviderUsername() {
        when(ebsiProperties.username()).thenReturn("username");
        String expectedUsername = "username";
        assertEquals(expectedUsername, appConfig.getIdentityProviderUsername());
    }

    @Test
    void testGetIdentityProviderPassword() {
        when(ebsiProperties.password()).thenReturn("password");
        String expectedPassword = "password";
        assertEquals(expectedPassword, appConfig.getIdentityProviderPassword());
    }

    @Test
    void testGetIdentityProviderClientId() {
        when(ebsiProperties.clientId()).thenReturn("clientId");
        String expectedClientId = "clientId";
        assertEquals(expectedClientId, appConfig.getIdentityProviderClientId());
    }

    @Test
    void testGetIdentityProviderClientSecret() {
        when(ebsiProperties.clientSecret()).thenReturn("clientSecret");
        String expectedClientSecret = "clientSecret";
        assertEquals(expectedClientSecret, appConfig.getIdentityProviderClientSecret());
    }

}
