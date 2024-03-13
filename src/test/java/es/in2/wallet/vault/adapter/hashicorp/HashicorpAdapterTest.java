package es.in2.wallet.vault.adapter.hashicorp;

import es.in2.wallet.infrastructure.vault.adapter.hashicorp.HashicorpAdapter;
import es.in2.wallet.infrastructure.vault.adapter.hashicorp.config.HashicorpConfig;
import es.in2.wallet.infrastructure.vault.adapter.hashicorp.model.HashicorpSecretRequest;
import es.in2.wallet.infrastructure.vault.model.KeyVaultSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.vault.core.ReactiveVaultOperations;
import org.springframework.vault.support.VaultResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HashicorpAdapterTest {
    @Mock
    private HashicorpConfig hashicorpConfig;
    @Mock
    private ReactiveVaultOperations vaultOperations;
    @InjectMocks
    private HashicorpAdapter hashicorpAdapter;

    @BeforeEach
    void setUp() {
        // Mock the behavior of broker properties to return predefined paths
        when(hashicorpConfig.getSecretPath()).thenReturn("/paths");

        // Create an instance of HashicorpAdapter with the mocked dependencies
        hashicorpAdapter = new HashicorpAdapter(vaultOperations, hashicorpConfig);
    }

    @Test
    void saveSecretTestSuccess() {
        String key = "key";
        KeyVaultSecret keyVaultSecret = KeyVaultSecret.builder().value("value").build();
        // Mock the write operation of ReactiveVaultOperations
        when(vaultOperations.write(any(), any(HashicorpSecretRequest.class)))
                .thenReturn(Mono.empty());

        // Test the saveSecret method
        StepVerifier.create(hashicorpAdapter.saveSecret(key, keyVaultSecret))
                .expectComplete()
                .verify();
    }

    @Test
    void saveSecretTestFailure() {
        String key = "key";
        KeyVaultSecret keyVaultSecret = KeyVaultSecret.builder().value("value").build();
        // Mock the write operation of ReactiveVaultOperations
        when(vaultOperations.write(any(), any(HashicorpSecretRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Simulated error")));

        // Test the saveSecret method
        StepVerifier.create(hashicorpAdapter.saveSecret(key, keyVaultSecret))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void getSecretTestSuccess() {
        String key = "key";
        KeyVaultSecret keyVaultSecret = KeyVaultSecret.builder().value(key).build();
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("data", data);
        VaultResponse vaultResponse = new VaultResponse();
        vaultResponse.setData(responseData);
        // Mock the write operation of ReactiveVaultOperations
        when(vaultOperations.read(any()))
                .thenReturn(Mono.just(vaultResponse));

        // Test the saveSecret method
        StepVerifier.create(hashicorpAdapter.getSecret(key))
                .expectNext(keyVaultSecret)
                .expectComplete()
                .verify();
    }

    @Test
    void getSecretTestFailure() {
        String key = "key";
        // Mock the write operation of ReactiveVaultOperations
        when(vaultOperations.read(any()))
                .thenReturn(Mono.error(new RuntimeException("Simulated error")));

        // Test the saveSecret method
        StepVerifier.create(hashicorpAdapter.getSecret(key))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void deleteSecretTestSuccess() {
        String key = "key";
        // Mock the write operation of ReactiveVaultOperations
        when(vaultOperations.delete(any()))
                .thenReturn(Mono.empty());

        // Test the saveSecret method
        StepVerifier.create(hashicorpAdapter.deleteSecret(key))
                .expectComplete()
                .verify();
    }

    @Test
    void deleteSecretTestFailure() {
        String key = "key";
        // Mock the write operation of ReactiveVaultOperations
        when(vaultOperations.delete(any()))
                .thenReturn(Mono.error(new RuntimeException("Simulated error")));

        // Test the saveSecret method
        StepVerifier.create(hashicorpAdapter.deleteSecret(key))
                .expectError(RuntimeException.class)
                .verify();
    }
}
