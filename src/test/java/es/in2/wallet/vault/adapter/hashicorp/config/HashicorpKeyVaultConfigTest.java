package es.in2.wallet.infrastructure.vault.adapter.hashicorp.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void testVaultEndpoint() {
        // Given
        when(hashicorpConfig.getVaultHost()).thenReturn("localhost");
        when(hashicorpConfig.getVaultPort()).thenReturn(8200);
        when(hashicorpConfig.getVaultScheme()).thenReturn("https");

        // When
        VaultEndpoint endpoint = hashicorpKeyVaultConfig.vaultEndpoint();

        // Then
        assertEquals("localhost", endpoint.getHost());
        assertEquals(8200, endpoint.getPort());
        assertEquals("https", endpoint.getScheme());
    }

    @Test
    void testClientAuthentication() {
        // Given
        when(hashicorpConfig.getVaultToken()).thenReturn("my-token");

        // When
        ClientAuthentication auth = hashicorpKeyVaultConfig.clientAuthentication();

        // Then
        assertEquals(TokenAuthentication.class, auth.getClass());

    }
}
