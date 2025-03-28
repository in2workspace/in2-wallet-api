package es.in2.wallet.vault.adapter.hashicorp.config;

import es.in2.wallet.infrastructure.appconfiguration.service.GenericConfigAdapter;
import es.in2.wallet.infrastructure.appconfiguration.util.ConfigAdapterFactory;
import es.in2.wallet.infrastructure.vault.adapter.hashicorp.config.HashicorpConfig;
import es.in2.wallet.infrastructure.vault.adapter.hashicorp.config.properties.HashicorpProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static es.in2.wallet.domain.utils.ApplicationConstants.VAULT_HASHICORP_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class HashicorpConfigTest {

    private ConfigAdapterFactory configAdapterFactory;
    private GenericConfigAdapter genericConfigAdapter;
    private HashicorpProperties hashicorpProperties;
    private HashicorpConfig hashicorpConfig;

    @BeforeEach
    void setUp() {
        configAdapterFactory = mock(ConfigAdapterFactory.class);
        genericConfigAdapter = mock(GenericConfigAdapter.class);
        hashicorpProperties = mock(HashicorpProperties.class);

        when(configAdapterFactory.getAdapter()).thenReturn(genericConfigAdapter);

        hashicorpConfig = new HashicorpConfig(configAdapterFactory, hashicorpProperties);
    }

    @Test
    void getSecretPath_shouldReturnValueFromGenericAdapter() {
        when(genericConfigAdapter.getConfiguration(VAULT_HASHICORP_PATH)).thenReturn("secret/path");

        String result = hashicorpConfig.getSecretPath();

        assertEquals("secret/path", result);
    }

    @Test
    void getVaultHost_shouldReturnValueFromGenericAdapter() {
        when(hashicorpProperties.host()).thenReturn("host.key");
        when(genericConfigAdapter.getConfiguration("host.key")).thenReturn("localhost");

        String result = hashicorpConfig.getVaultHost();

        assertEquals("localhost", result);
    }

    @Test
    void getVaultPort_shouldReturnParsedIntegerFromGenericAdapter() {
        when(hashicorpProperties.port()).thenReturn("port.key");
        when(genericConfigAdapter.getConfiguration("port.key")).thenReturn("8200");

        int result = hashicorpConfig.getVaultPort();

        assertEquals(8200, result);
    }

    @Test
    void getVaultScheme_shouldReturnValueFromGenericAdapter() {
        when(hashicorpProperties.scheme()).thenReturn("scheme.key");
        when(genericConfigAdapter.getConfiguration("scheme.key")).thenReturn("https");

        String result = hashicorpConfig.getVaultScheme();

        assertEquals("https", result);
    }

    @Test
    void getVaultToken_shouldDecodeBase64Token() {
        String rawToken = Base64.getEncoder().encodeToString("my-secret-token".getBytes());
        when(hashicorpProperties.token()).thenReturn(rawToken);

        String result = hashicorpConfig.getVaultToken();

        assertEquals("my-secret-token", result);
    }

    @Test
    void getVaultToken_shouldReturnPlainTokenIfNotBase64() {
        String rawToken = "plain-token";
        when(hashicorpProperties.token()).thenReturn(rawToken);

        String result = hashicorpConfig.getVaultToken();

        assertEquals("plain-token", result);
    }
}
