package es.in2.wallet.vault.adapter.hashicorp.config;

import es.in2.wallet.infrastructure.vault.adapter.hashicorp.config.HashicorpConfig;
import es.in2.wallet.infrastructure.vault.adapter.hashicorp.config.HashicorpKeyVaultConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HashicorpKeyVaultConfigTest {

    private HashicorpConfig hashicorpConfig;
    private HashicorpKeyVaultConfig hashicorpKeyVaultConfig;

    @BeforeEach
    void setUp() {
        hashicorpConfig = mock(HashicorpConfig.class);
        hashicorpKeyVaultConfig = new HashicorpKeyVaultConfig(hashicorpConfig);
    }

    @Test
    void vaultEndpoint_shouldReturnProperlyParsedVaultEndpoint() {
        // Given a complete Vault URL
        when(hashicorpConfig.getVaultUrl()).thenReturn("https://vault.example.com:8200");

        VaultEndpoint endpoint = hashicorpKeyVaultConfig.vaultEndpoint();

        assertEquals("vault.example.com", endpoint.getHost());
        assertEquals(8200, endpoint.getPort());
        assertEquals("https", endpoint.getScheme());
    }

    @Test
    void vaultEndpoint_shouldFallbackToDefaultPortIfNotSpecified() {
        // URL without explicit port → should fallback to 443 (https)
        when(hashicorpConfig.getVaultUrl()).thenReturn("https://vault.example.com");

        VaultEndpoint endpoint = hashicorpKeyVaultConfig.vaultEndpoint();

        assertEquals("vault.example.com", endpoint.getHost());
        assertEquals(443, endpoint.getPort()); // default for https
        assertEquals("https", endpoint.getScheme());
    }

    @Test
    void vaultEndpoint_shouldThrowExceptionOnInvalidUrl() {
        when(hashicorpConfig.getVaultUrl()).thenReturn("not-a-valid-url");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                hashicorpKeyVaultConfig.vaultEndpoint());

        assertTrue(exception.getMessage().startsWith("Invalid Vault URL: not-a-valid-url"));
    }

    @Test
    void clientAuthentication_shouldReturnTokenAuthentication() {
        when(hashicorpConfig.getVaultToken()).thenReturn("my-token");

        ClientAuthentication authentication = hashicorpKeyVaultConfig.clientAuthentication();

        assertEquals(TokenAuthentication.class, authentication.getClass());
    }
}
