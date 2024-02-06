package es.in2.wallet.vault.adapter;

import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static es.in2.wallet.api.util.MessageUtils.PRIVATE_KEY_TYPE;
import static es.in2.wallet.api.util.MessageUtils.PUBLIC_KEY_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureAdapterTest {
    @Mock
    private SecretClient secretClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AzureAdapter azureAdapter;

    @Test
    void saveSecretSuccessTest() throws Exception {
        Map<String, String> secrets = new HashMap<>();
        secrets.put("did", "exampleDid");

        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("{\"did\":\"exampleDid\"}");

        when(secretClient.setSecret(any(KeyVaultSecret.class))).thenReturn(null);

        StepVerifier.create(azureAdapter.saveSecret(secrets))
                .verifyComplete();
    }

    @Test
    void getSecretByKeySuccessTest() throws Exception {
        String secretValue = "{\"privateKeyType\":\"examplePrivateKey\",\"publicKeyType\":\"examplePublicKey\"}";
        KeyVaultSecret secret = new KeyVaultSecret("exampleDid", secretValue)
                .setProperties(new SecretProperties().setContentType("application/json"));

        when(secretClient.getSecret(anyString())).thenReturn(secret);

        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(new HashMap<String, String>() {{
            put(PRIVATE_KEY_TYPE, "examplePrivateKey");
            put(PUBLIC_KEY_TYPE, "examplePublicKey");
        }});

        StepVerifier.create(azureAdapter.getSecretByKey("exampleDid", PRIVATE_KEY_TYPE))
                .expectNext("examplePrivateKey")
                .verifyComplete();
    }

    @Test
    void deleteSecretByKeySuccessTest() {
        when(secretClient.beginDeleteSecret(anyString())).thenReturn(null);

        StepVerifier.create(azureAdapter.deleteSecretByKey("exampleDid"))
                .verifyComplete();
    }
}
