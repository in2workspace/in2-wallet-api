package es.in2.wallet.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.domain.exception.ParseErrorException;
import es.in2.wallet.application.port.VaultService;
import es.in2.wallet.domain.service.impl.SignerServiceImpl;
import es.in2.wallet.infrastructure.vault.model.KeyVaultSecret;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignerServiceImplTest {

    @Mock
    private ObjectMapper mockedObjectMapper;

    @Mock
    private VaultService vaultService;
    @InjectMocks
    private SignerServiceImpl signerService;

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @ValueSource(strings = {"proof", "vp", "jwt"})
    void testSignDocumentWithDifferentTypesAndDidPK(String documentType) throws JsonProcessingException {
        String json = "{\"document\":\"sign this document\"}";
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(json);
        String privateKey = "{\"kty\":\"EC\",\"d\":\"MDtaBGOjN0SY0NtX2hFvv4uJNLrUGUWHvquqNZHwi5s\",\"use\":\"sig\",\"crv\":\"P-256\",\"kid\":\"75bb28ac9f4247248c73348f890e050c\",\"x\":\"E9pfJi7I29gtdofnJJBvC_DK3KH1eTialAMOoX6CfZw\",\"y\":\"hDfdnEyabkB-9Hf1PFYaYomSdYVwJ0NSM5CzxhOUIr0\",\"alg\":\"ES256\"}";
        String did = "did:example:1234";

        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("someKey", "someValue");


        KeyVaultSecret keyVaultSecret = KeyVaultSecret.builder()
                .value(privateKey)
                .build();

        when(vaultService.getSecretByKey(did)).thenReturn(Mono.just(keyVaultSecret));
        when(mockedObjectMapper.convertValue(any(JsonNode.class), any(TypeReference.class))).thenReturn(claimsMap);

        StepVerifier.create(signerService.buildJWTSFromJsonNode(jsonNode, did, documentType))
                .assertNext(signedDocument -> {
                    assert signedDocument != null;
                })
                .verifyComplete();
    }

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @ValueSource(strings = {"proof", "vp", "jwt"})
    void testSignDocumentWithDifferentTypesWithoutDidPK(String documentType) throws JsonProcessingException {
        String json = "{\"document\":\"sign this document\"}";
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(json);
        String privateKey = "{\"kty\":\"EC\",\"d\":\"MDtaBGOjN0SY0NtX2hFvv4uJNLrUGUWHvquqNZHwi5s\",\"use\":\"sig\",\"crv\":\"P-256\",\"kid\":\"75bb28ac9f4247248c73348f890e050c\",\"x\":\"E9pfJi7I29gtdofnJJBvC_DK3KH1eTialAMOoX6CfZw\",\"y\":\"hDfdnEyabkB-9Hf1PFYaYomSdYVwJ0NSM5CzxhOUIr0\",\"alg\":\"ES256\"}";
        String did = "key123";

        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("someKey", "someValue");

        KeyVaultSecret keyVaultSecret = KeyVaultSecret.builder()
                .value(privateKey)
                .build();

        when(vaultService.getSecretByKey(did)).thenReturn(Mono.just(keyVaultSecret));
        when(mockedObjectMapper.convertValue(any(JsonNode.class), any(TypeReference.class))).thenReturn(claimsMap);

        StepVerifier.create(signerService.buildJWTSFromJsonNode(jsonNode, did, documentType))
                .assertNext(signedDocument -> {
                    assert signedDocument != null;
                })
                .verifyComplete();
    }
    @ParameterizedTest
    @ValueSource(strings = {"proof", "vp", "jwt"})
    void testSignDocumentFailure(String documentType) throws JsonProcessingException {
        String json = "{\"document\":\"sign this document\"}";
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(json);
        String privateKey = "invalid private key";
        String did = "did:example";

        KeyVaultSecret keyVaultSecret = KeyVaultSecret.builder()
                .value(privateKey)
                .build();

        when(vaultService.getSecretByKey(did)).thenReturn(Mono.just(keyVaultSecret));

        StepVerifier.create(signerService.buildJWTSFromJsonNode(jsonNode, did, documentType))
                .expectError(ParseErrorException.class)
                .verify();
    }

}
