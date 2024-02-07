package es.in2.wallet.vault.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.api.exception.ParseErrorException;
import es.in2.wallet.vault.model.VaultSecretData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.vault.core.ReactiveVaultOperations;
import org.springframework.vault.support.VaultResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.security.auth.login.CredentialNotFoundException;
import java.util.HashMap;
import java.util.Map;

import static es.in2.wallet.api.util.MessageUtils.PRIVATE_KEY_TYPE;
import static es.in2.wallet.api.util.MessageUtils.PUBLIC_KEY_TYPE;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HashicorpVaultAdapterTest {
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ReactiveVaultOperations vaultOperations;

    @InjectMocks
    private HashicorpAdapter hashicorpAdapter;

    @Test
    void saveSecretSuccess() {
        Map<String, String> result = new HashMap<>();
        result.put("did", "example");
        when(vaultOperations.write(anyString(), any())).thenReturn(Mono.just(new VaultResponse()));

        StepVerifier.create(hashicorpAdapter.saveSecret(result))
                .verifyComplete();
    }

    @Test
    void saveSecretFailure() {
        when(vaultOperations.write(anyString(), any())).thenReturn(Mono.error(new RuntimeException("Error")));

        Map<String, String> result = new HashMap<>();
        result.put("did", "example");
        StepVerifier.create(hashicorpAdapter.saveSecret(result))
                .expectError(RuntimeException.class)
                .verify();
    }
    @Test
    void getPrivateKeyByKeySuccess() throws JsonProcessingException {
        String key = "did:key:1234";

        Map<String, Object> data = new HashMap<>();
        VaultSecretData vaultSecretData = VaultSecretData.builder()
                .privateKey("key1")
                .publicKey("key2").build();
        data.put(key,vaultSecretData);
        VaultResponse vaultResponse = new VaultResponse();
        vaultResponse.setData(data);

        when(vaultOperations.read(anyString())).thenReturn(Mono.just(vaultResponse));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"privateKey\":\"key1\", \"publicKey\":\"key2\"}");
        when(objectMapper.readValue(anyString(), eq(VaultSecretData.class))).thenReturn(vaultSecretData);

        StepVerifier.create(hashicorpAdapter.getSecretByKey(key, PRIVATE_KEY_TYPE))
                .expectNext("key1")
                .verifyComplete();
    }

    @Test
    void getPublicKeyByKeySuccess() throws JsonProcessingException {
        String key = "did:key:1234";

        Map<String, Object> data = new HashMap<>();
        VaultSecretData vaultSecretData = VaultSecretData.builder()
                .privateKey("key1")
                .publicKey("key2").build();
        data.put(key,vaultSecretData);
        VaultResponse vaultResponse = new VaultResponse();
        vaultResponse.setData(data);

        when(vaultOperations.read(anyString())).thenReturn(Mono.just(vaultResponse));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"privateKey\":\"key1\", \"publicKey\":\"key2\"}");
        when(objectMapper.readValue(anyString(), eq(VaultSecretData.class))).thenReturn(vaultSecretData);

        StepVerifier.create(hashicorpAdapter.getSecretByKey(key, PUBLIC_KEY_TYPE))
                .expectNext("key2")
                .verifyComplete();
    }

    @Test
    void getSecretByKeyNotFound() {

        VaultResponse vaultResponse = new VaultResponse();
        vaultResponse.setData(null);

        when(vaultOperations.read(anyString())).thenReturn(Mono.just(vaultResponse));

        StepVerifier.create(hashicorpAdapter.getSecretByKey("did:key:123", PUBLIC_KEY_TYPE))
                .expectError(CredentialNotFoundException.class)
                .verify();
    }

    @Test
    void getSecretByKeyParseError() throws JsonProcessingException {

        String key = "did:key:1234";

        Map<String, Object> data = new HashMap<>();
        VaultSecretData vaultSecretData = VaultSecretData.builder()
                .privateKey("key1")
                .publicKey("key2").build();
        data.put(key,vaultSecretData);
        VaultResponse vaultResponse = new VaultResponse();
        vaultResponse.setData(data);

        when(vaultOperations.read(anyString())).thenReturn(Mono.just(vaultResponse));
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Error") {});

        StepVerifier.create(hashicorpAdapter.getSecretByKey(key, PUBLIC_KEY_TYPE))
                .expectError(ParseErrorException.class)
                .verify();
    }

    @Test
    void deleteSecretByKeySuccess() {
        when(vaultOperations.delete(anyString())).thenReturn(Mono.empty());
        StepVerifier.create(hashicorpAdapter.deleteSecretByKey("testKey"))
                .verifyComplete();
    }

    @Test
    void deleteSecretByKeyNotFound() {
        when(vaultOperations.delete(anyString())).thenReturn(Mono.error(new RuntimeException("Error")));

        StepVerifier.create(hashicorpAdapter.deleteSecretByKey("testKey"))
                .expectError(RuntimeException.class)
                .verify();
    }
}
