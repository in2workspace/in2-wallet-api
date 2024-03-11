package es.in2.wallet.vault.adapter.azure;

import com.azure.core.util.polling.PollerFlux;
import com.azure.security.keyvault.secrets.SecretAsyncClient;
import es.in2.wallet.infrastructure.vault.adapter.azure.AzureKeyVaultAdapter;
import es.in2.wallet.infrastructure.vault.model.KeyVaultSecret;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class AzureKeyVaultAdapterTest {
    @Mock
    private SecretAsyncClient secretClient;
    @InjectMocks
    private AzureKeyVaultAdapter azureKeyVaultAdapter;

    @Test
    void saveSecretTestSuccess() {
        String key = "key";
        KeyVaultSecret keyVaultSecret = KeyVaultSecret.builder().value("privateKey").build();
        when(secretClient.setSecret(key, keyVaultSecret.value().toString())).thenReturn(Mono.empty());

        StepVerifier.create(azureKeyVaultAdapter.saveSecret(key, keyVaultSecret))
                .verifyComplete();
    }

    @Test
    void saveSecretTestFailure() {
        String key = "key";
        KeyVaultSecret keyVaultSecret = KeyVaultSecret.builder().value("privateKey").build();
        when(secretClient.setSecret(key, keyVaultSecret.value().toString())).thenReturn(Mono.error(new RuntimeException("Simulated error")));

        StepVerifier.create(azureKeyVaultAdapter.saveSecret(key, keyVaultSecret))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void getSecretTestSuccess() {
        String key = "key";
        String secretValue = "secretValue";
        KeyVaultSecret keyVaultSecret = KeyVaultSecret.builder().value(secretValue).build();
        com.azure.security.keyvault.secrets.models.KeyVaultSecret secret =
                new com.azure.security.keyvault.secrets.models.KeyVaultSecret("exampleDid", secretValue);

        when(secretClient.getSecret(key)).thenReturn(Mono.just(secret));

        StepVerifier.create(azureKeyVaultAdapter.getSecret(key))
                .expectNext(keyVaultSecret)
                .verifyComplete();
    }

    @Test
    void deleteSecretTestFailure() {
        String key = "key";
        when(secretClient.beginDeleteSecret(key)).thenReturn(PollerFlux.error(new RuntimeException("Simulated error")));

        StepVerifier.create(azureKeyVaultAdapter.deleteSecret(key))
                .expectError(RuntimeException.class)
                .verify();
    }
}
