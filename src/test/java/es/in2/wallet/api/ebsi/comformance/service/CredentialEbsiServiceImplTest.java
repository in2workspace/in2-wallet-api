package es.in2.wallet.api.ebsi.comformance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.api.ebsi.comformance.configuration.EbsiConfig;
import es.in2.wallet.api.ebsi.comformance.service.impl.CredentialEbsiServiceImpl;
import es.in2.wallet.api.exception.FailedDeserializingException;
import es.in2.wallet.api.exception.ParseErrorException;
import es.in2.wallet.api.model.CredentialIssuerMetadata;
import es.in2.wallet.api.model.CredentialOffer;
import es.in2.wallet.api.model.CredentialResponse;
import es.in2.wallet.api.model.TokenResponse;
import es.in2.wallet.api.service.SignerService;
import es.in2.wallet.api.util.MessageUtils;
import es.in2.wallet.vault.service.VaultService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static es.in2.wallet.api.util.MessageUtils.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialEbsiServiceImplTest {
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private EbsiConfig ebsiConfig;
    @Mock
    private VaultService vaultService;

    @Mock
    private SignerService signerService;
    @InjectMocks
    private CredentialEbsiServiceImpl credentialEbsiService;

    @Test
    void getEbsiCredentialTest() throws JsonProcessingException {
        try (MockedStatic<MessageUtils> ignored = Mockito.mockStatic(MessageUtils.class)){
            String processId = "processId";
            String ebsiDid = "did:key:1234";
            CredentialOffer.Credential credential = CredentialOffer.Credential.builder().types(List.of("LEARCredential")).format("jwt_vc").build();
            List<CredentialOffer.Credential> credentials = List.of(credential);

            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();

            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();

            String authorizationToken = "authToken";

            CredentialResponse mockCredentialResponse = CredentialResponse.builder().credential("credential").c_nonce("fresh_nonce").c_nonce_expires_in(600).format("jwt").build();


            List<Map.Entry<String, String>> headersForIssuer = new ArrayList<>();
            headersForIssuer.add(new AbstractMap.SimpleEntry<>(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON));
            headersForIssuer.add(new AbstractMap.SimpleEntry<>(HEADER_AUTHORIZATION, BEARER + tokenResponse.accessToken()));

            String json = "{\"document\":\"sign this document\"}";

            ObjectMapper objectMapper2 = new ObjectMapper();

            JsonNode jsonNode = objectMapper2.readTree(json);

            when(ebsiConfig.getDid()).thenReturn(Mono.just(ebsiDid));
            when(vaultService.getSecretByKey(ebsiDid,PRIVATE_KEY_TYPE)).thenReturn(Mono.just("key"));
            when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
            when(signerService.buildJWTSFromJsonNode(jsonNode,ebsiDid,"proof","key")).thenReturn(Mono.just("signedJwt"));
            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");
            when(objectMapper.readValue(anyString(), eq(CredentialResponse.class))).thenReturn(mockCredentialResponse);


            when(postRequest(credentialIssuerMetadata.credentialEndpoint(), headersForIssuer, "credentialRequest")).thenReturn(Mono.just("credential"));

            StepVerifier.create(credentialEbsiService.getCredential(processId, tokenResponse, credentialIssuerMetadata, authorizationToken,credentials.get(0).format(),credentials.get(0).types()))
                    .expectNext(mockCredentialResponse)
                    .verifyComplete();
        }
    }

    @Test
    void getCredentialFailedDeserializingErrorTest() throws JsonProcessingException{
        try (MockedStatic<MessageUtils> ignored = Mockito.mockStatic(MessageUtils.class)){

            String processId = "processId";
            String ebsiDid = "did:key:1234";
            CredentialOffer.Credential credential = CredentialOffer.Credential.builder().types(List.of("LEARCredential")).format("jwt_vc").build();
            List<CredentialOffer.Credential> credentials = List.of(credential);

            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();

            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();

            String authorizationToken = "authToken";

            List<Map.Entry<String, String>> headersForIssuer = new ArrayList<>();
            headersForIssuer.add(new AbstractMap.SimpleEntry<>(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON));
            headersForIssuer.add(new AbstractMap.SimpleEntry<>(HEADER_AUTHORIZATION, BEARER + tokenResponse.accessToken()));

            String json = "{\"document\":\"sign this document\"}";

            ObjectMapper objectMapper2 = new ObjectMapper();

            JsonNode jsonNode = objectMapper2.readTree(json);

            when(ebsiConfig.getDid()).thenReturn(Mono.just(ebsiDid));
            when(vaultService.getSecretByKey(ebsiDid,PRIVATE_KEY_TYPE)).thenReturn(Mono.just("key"));
            when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
            when(signerService.buildJWTSFromJsonNode(jsonNode,ebsiDid,"proof","key")).thenReturn(Mono.just("signedJwt"));
            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");

            when(objectMapper.readValue(anyString(), eq(CredentialResponse.class)))
                    .thenThrow(new JsonProcessingException("Deserialization error") {});


            when(postRequest(credentialIssuerMetadata.credentialEndpoint(), headersForIssuer, "credentialRequest")).thenReturn(Mono.just("credential"));

            StepVerifier.create(credentialEbsiService.getCredential(processId, tokenResponse, credentialIssuerMetadata, authorizationToken,credentials.get(0).format(),credentials.get(0).types()))
                    .expectError(FailedDeserializingException.class)
                    .verify();
        }
    }

    @Test
    void getCredentialParseErrorTest() throws JsonProcessingException {
        try (MockedStatic<MessageUtils> ignored = Mockito.mockStatic(MessageUtils.class)){
            String processId = "processId";
            String ebsiDid = "did:key:1234";
            CredentialOffer.Credential credential = CredentialOffer.Credential.builder().types(List.of("LEARCredential")).format("jwt_vc").build();
            List<CredentialOffer.Credential> credentials = List.of(credential);

            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();

            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();

            String authorizationToken = "authToken";

            when(ebsiConfig.getDid()).thenReturn(Mono.just(ebsiDid));
            when(objectMapper.readTree(anyString())).thenThrow(new JsonProcessingException("Deserialization error") {});

            StepVerifier.create(credentialEbsiService.getCredential(processId, tokenResponse, credentialIssuerMetadata, authorizationToken,credentials.get(0).format(),credentials.get(0).types()))
                    .expectError(ParseErrorException.class)
                    .verify();
        }
    }

    /**
     * Utilizes StepVerifier.withVirtualTime for simulating the passage of time in tests.
     * This approach is crucial when testing reactive streams that incorporate delays,
     * like Mono.delay, as it allows us to virtually "skip" over these delay periods.
     * In the context of this test, we are dealing with an asynchronous operation that includes
     * a deliberate delay (Mono.delay(Duration.ofSeconds(10))) to synchronize with an external
     * process or service. Using virtual time, we can simulate this delay without actually
     * causing the test to wait for the real-time duration. This makes our tests more efficient
     * and avoids unnecessarily long-running tests, while still accurately testing the time-based
     * behavior of our reactive streams.
     * The thenAwait(Duration.ofSeconds(10)) call is used to advance the virtual clock by 10 seconds,
     * effectively simulating the delay introduced in our reactive flow, allowing us to test
     * the behavior post-delay without the real-world wait.
     */
    @Test
    void getEbsiCredentialDeferredSuccessTest() throws JsonProcessingException {
        try (MockedStatic<MessageUtils> ignored = Mockito.mockStatic(MessageUtils.class)){
            String processId = "processId";
            String ebsiDid = "did:key:1234";
            CredentialOffer.Credential credential = CredentialOffer.Credential.builder().types(List.of("LEARCredential")).format("jwt_vc").build();
            List<CredentialOffer.Credential> credentials = List.of(credential);

            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();

            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();

            String authorizationToken = "authToken";

            CredentialResponse mockDeferredResponse1 = CredentialResponse.builder()
                    .acceptanceToken("deferredToken")
                    .build();
            CredentialResponse mockDeferredResponse2 = CredentialResponse.builder()
                    .acceptanceToken("deferredTokenRecursive")
                    .build();
            CredentialResponse mockFinalCredentialResponse = CredentialResponse.builder()
                    .credential("finalCredential")
                    .build();


            List<Map.Entry<String, String>> headersForCredentialRequest = new ArrayList<>();
            headersForCredentialRequest.add(new AbstractMap.SimpleEntry<>(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON));
            headersForCredentialRequest.add(new AbstractMap.SimpleEntry<>(HEADER_AUTHORIZATION, BEARER + tokenResponse.accessToken()));

            List<Map.Entry<String, String>> headersForDeferredCredentialRequest = new ArrayList<>();
            headersForDeferredCredentialRequest.add(new AbstractMap.SimpleEntry<>(HEADER_AUTHORIZATION, BEARER + "deferredToken"));

            List<Map.Entry<String, String>> headersForDeferredCredentialRequestRecursive = new ArrayList<>();
            headersForDeferredCredentialRequestRecursive.add(new AbstractMap.SimpleEntry<>(HEADER_AUTHORIZATION, BEARER + "deferredTokenRecursive"));

            String json = "{\"document\":\"sign this document\"}";

            ObjectMapper objectMapper2 = new ObjectMapper();

            JsonNode jsonNode = objectMapper2.readTree(json);

            when(ebsiConfig.getDid()).thenReturn(Mono.just(ebsiDid));
            when(vaultService.getSecretByKey(ebsiDid,PRIVATE_KEY_TYPE)).thenReturn(Mono.just("key"));
            when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
            when(signerService.buildJWTSFromJsonNode(jsonNode,ebsiDid,"proof","key")).thenReturn(Mono.just("signedJwt"));
            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");

            when(postRequest(credentialIssuerMetadata.credentialEndpoint(), headersForCredentialRequest, "credentialRequest"))
                    .thenReturn(Mono.just("deferredResponse"));

            when(postRequest(credentialIssuerMetadata.deferredCredentialEndpoint(), headersForDeferredCredentialRequest, ""))
                    .thenReturn(Mono.just("deferredResponseRecursive"));

            when(postRequest(credentialIssuerMetadata.deferredCredentialEndpoint(), headersForDeferredCredentialRequestRecursive, ""))
                    .thenReturn(Mono.just("finalCredentialResponse"));

            when(ebsiConfig.getDid()).thenReturn(Mono.just(ebsiDid));
            when(vaultService.getSecretByKey(ebsiDid,PRIVATE_KEY_TYPE)).thenReturn(Mono.just("key"));
            when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
            when(signerService.buildJWTSFromJsonNode(jsonNode,ebsiDid,"proof","key")).thenReturn(Mono.just("signedJwt"));
            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");
            when(objectMapper.readValue("deferredResponse", CredentialResponse.class)).thenReturn(mockDeferredResponse1);

            when(objectMapper.readValue("deferredResponse", CredentialResponse.class))
                    .thenReturn(mockDeferredResponse1);
            when(objectMapper.readValue("deferredResponseRecursive", CredentialResponse.class))
                    .thenReturn(mockDeferredResponse2);
            when(objectMapper.readValue("finalCredentialResponse", CredentialResponse.class))
                    .thenReturn(mockFinalCredentialResponse);

            StepVerifier.withVirtualTime(() -> credentialEbsiService.getCredential(processId, tokenResponse, credentialIssuerMetadata, authorizationToken, credentials.get(0).format(), credentials.get(0).types()))
                    .thenAwait(Duration.ofSeconds(10))
                    .expectNext(mockFinalCredentialResponse)
                    .verifyComplete();
        }
    }



}
