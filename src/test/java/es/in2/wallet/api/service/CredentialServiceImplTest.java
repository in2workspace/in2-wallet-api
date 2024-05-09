package es.in2.wallet.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.domain.exception.FailedCommunicationException;
import es.in2.wallet.domain.exception.FailedDeserializingException;
import es.in2.wallet.domain.exception.FailedSerializingException;
import es.in2.wallet.domain.model.*;
import es.in2.wallet.domain.service.impl.CredentialServiceImpl;
import es.in2.wallet.domain.util.ApplicationUtils;
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

import static es.in2.wallet.domain.util.ApplicationUtils.postRequest;
import static es.in2.wallet.domain.util.MessageUtils.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialServiceImplTest {
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private CredentialServiceImpl credentialService;

    @Test
    void getCredentialTest() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){

            String jwt = "ey34324";

            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();

            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();

            CredentialResponse mockCredentialResponse = CredentialResponse.builder().credential("credential").c_nonce("fresh_nonce").c_nonce_expires_in(600).format("jwt").build();


            List<Map.Entry<String, String>> headersForIssuer = new ArrayList<>();
            headersForIssuer.add(new AbstractMap.SimpleEntry<>(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON));
            headersForIssuer.add(new AbstractMap.SimpleEntry<>(HEADER_AUTHORIZATION, BEARER + tokenResponse.accessToken()));


            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");
            when(objectMapper.readValue(anyString(), eq(CredentialResponse.class))).thenReturn(mockCredentialResponse);

            when(postRequest(credentialIssuerMetadata.credentialEndpoint(), headersForIssuer, "credentialRequest")).thenReturn(Mono.just("credential"));

            StepVerifier.create(credentialService.getCredential(jwt,tokenResponse, credentialIssuerMetadata,JWT_VC, List.of("VerifiableCredential","LEARCredential")))
                    .expectNext(mockCredentialResponse)
                    .verifyComplete();
        }
    }
    @Test
    void getCredentialTestWithoutTypes() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){

            String jwt = "ey34324";

            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();

            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();

            CredentialResponse mockCredentialResponse = CredentialResponse.builder().credential("credential").c_nonce("fresh_nonce").c_nonce_expires_in(600).format("jwt").build();


            List<Map.Entry<String, String>> headersForIssuer = new ArrayList<>();
            headersForIssuer.add(new AbstractMap.SimpleEntry<>(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON));
            headersForIssuer.add(new AbstractMap.SimpleEntry<>(HEADER_AUTHORIZATION, BEARER + tokenResponse.accessToken()));


            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");
            when(objectMapper.readValue(anyString(), eq(CredentialResponse.class))).thenReturn(mockCredentialResponse);

            when(postRequest(credentialIssuerMetadata.credentialEndpoint(), headersForIssuer, "credentialRequest")).thenReturn(Mono.just("credential"));

            StepVerifier.create(credentialService.getCredential(jwt,tokenResponse, credentialIssuerMetadata,JWT_VC, null))
                    .expectNext(mockCredentialResponse)
                    .verifyComplete();
        }
    }

    @Test
    void getCredentialTestForFiware() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){

            String jwt = "ey34324";

            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();

            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();

            CredentialResponse mockCredentialResponse = CredentialResponse.builder().credential("credential").c_nonce("fresh_nonce").c_nonce_expires_in(600).format("jwt").build();


            List<Map.Entry<String, String>> headersForIssuer = new ArrayList<>();
            headersForIssuer.add(new AbstractMap.SimpleEntry<>(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON));
            headersForIssuer.add(new AbstractMap.SimpleEntry<>(HEADER_AUTHORIZATION, BEARER + tokenResponse.accessToken()));


            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");
            when(objectMapper.readValue(anyString(), eq(CredentialResponse.class))).thenReturn(mockCredentialResponse);

            when(postRequest(credentialIssuerMetadata.credentialEndpoint(), headersForIssuer, "credentialRequest")).thenReturn(Mono.just("credential"));

            StepVerifier.create(credentialService.getCredential(jwt,tokenResponse, credentialIssuerMetadata,JWT_VC, List.of("LEARCredential")))
                    .expectNext(mockCredentialResponse)
                    .verifyComplete();
        }
    }
    @Test
    void getCredentialFailedCommunicationErrorTest() throws JsonProcessingException{
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){

            String jwt = "ey34324";

            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();

            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();

            List<Map.Entry<String, String>> headersForIssuer = new ArrayList<>();
            headersForIssuer.add(new AbstractMap.SimpleEntry<>(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON));
            headersForIssuer.add(new AbstractMap.SimpleEntry<>(HEADER_AUTHORIZATION, BEARER + tokenResponse.accessToken()));

            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");

            when(postRequest(credentialIssuerMetadata.credentialEndpoint(), headersForIssuer, "credentialRequest"))
                    .thenReturn(Mono.error(new RuntimeException("Communication error")));

            StepVerifier.create(credentialService.getCredential(jwt,tokenResponse, credentialIssuerMetadata ,JWT_VC, List.of("LEARCredential")))
                    .expectError(FailedCommunicationException.class)
                    .verify();
        }
    }
    @Test
    void getCredentialFailedDeserializingErrorTest() throws JsonProcessingException{
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){

            String jwt = "ey34324";

            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();

            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();

            List<Map.Entry<String, String>> headersForIssuer = new ArrayList<>();
            headersForIssuer.add(new AbstractMap.SimpleEntry<>(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON));
            headersForIssuer.add(new AbstractMap.SimpleEntry<>(HEADER_AUTHORIZATION, BEARER + tokenResponse.accessToken()));


            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");
            when(objectMapper.readValue(anyString(), eq(CredentialResponse.class)))
                    .thenThrow(new JsonProcessingException("Deserialization error") {});

            when(postRequest(credentialIssuerMetadata.credentialEndpoint(), headersForIssuer, "credentialRequest")).thenReturn(Mono.just("credential"));

            StepVerifier.create(credentialService.getCredential(jwt,tokenResponse, credentialIssuerMetadata ,JWT_VC, List.of("LEARCredential")))
                    .expectError(FailedDeserializingException.class)
                    .verify();
        }
    }

    @Test
    void getCredentialFailedSerializingExceptionTest() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){

            String jwt = "ey34324";

            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();

            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();

            when(objectMapper.writeValueAsString(any()))
                    .thenThrow(new JsonProcessingException("Serialization error") {});

            StepVerifier.create(credentialService.getCredential(jwt,tokenResponse, credentialIssuerMetadata ,JWT_VC, List.of("LEARCredential")))
                    .expectError(FailedSerializingException.class)
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
    void getCredentialDeferredSuccessTest() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
            String jwt = "ey34324";
            CredentialOffer.Credential credential = CredentialOffer.Credential.builder().types(List.of("LEARCredential")).format("jwt_vc").build();
            List<CredentialOffer.Credential> credentials = List.of(credential);

            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();

            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();


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

            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");

            when(postRequest(credentialIssuerMetadata.credentialEndpoint(), headersForCredentialRequest, "credentialRequest"))
                    .thenReturn(Mono.just("deferredResponse"));

            when(postRequest(credentialIssuerMetadata.deferredCredentialEndpoint(), headersForDeferredCredentialRequest, ""))
                    .thenReturn(Mono.just("deferredResponseRecursive"));

            when(postRequest(credentialIssuerMetadata.deferredCredentialEndpoint(), headersForDeferredCredentialRequestRecursive, ""))
                    .thenReturn(Mono.just("finalCredentialResponse"));

            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");
            when(objectMapper.readValue("deferredResponse", CredentialResponse.class)).thenReturn(mockDeferredResponse1);

            when(objectMapper.readValue("deferredResponse", CredentialResponse.class))
                    .thenReturn(mockDeferredResponse1);
            when(objectMapper.readValue("deferredResponseRecursive", CredentialResponse.class))
                    .thenReturn(mockDeferredResponse2);
            when(objectMapper.readValue("finalCredentialResponse", CredentialResponse.class))
                    .thenReturn(mockFinalCredentialResponse);

            StepVerifier.withVirtualTime(() -> credentialService.getCredential(jwt,tokenResponse, credentialIssuerMetadata, credentials.get(0).format(), credentials.get(0).types()))
                    .thenAwait(Duration.ofSeconds(10))
                    .expectNext(mockFinalCredentialResponse)
                    .verifyComplete();
        }
    }

    @Test
    void getCredentialDeferredErrorTest() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
            String jwt = "ey34324";
            CredentialOffer.Credential credential = CredentialOffer.Credential.builder().types(List.of("LEARCredential")).format("jwt_vc").build();
            List<CredentialOffer.Credential> credentials = List.of(credential);

            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();

            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();


            CredentialResponse mockDeferredResponse1 = CredentialResponse.builder()
                    .acceptanceToken("deferredToken")
                    .build();


            List<Map.Entry<String, String>> headersForCredentialRequest = new ArrayList<>();
            headersForCredentialRequest.add(new AbstractMap.SimpleEntry<>(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON));
            headersForCredentialRequest.add(new AbstractMap.SimpleEntry<>(HEADER_AUTHORIZATION, BEARER + tokenResponse.accessToken()));

            List<Map.Entry<String, String>> headersForDeferredCredentialRequest = new ArrayList<>();
            headersForDeferredCredentialRequest.add(new AbstractMap.SimpleEntry<>(HEADER_AUTHORIZATION, BEARER + "deferredToken"));


            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");

            when(postRequest(credentialIssuerMetadata.credentialEndpoint(), headersForCredentialRequest, "credentialRequest"))
                    .thenReturn(Mono.just("deferredResponse"));

            when(postRequest(credentialIssuerMetadata.deferredCredentialEndpoint(), headersForDeferredCredentialRequest, ""))
                    .thenReturn(Mono.just("deferredResponseRecursive"));


            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");
            when(objectMapper.readValue("deferredResponse", CredentialResponse.class)).thenReturn(mockDeferredResponse1);

            when(objectMapper.readValue("deferredResponseRecursive", CredentialResponse.class))
                    .thenThrow(new IllegalStateException("No credential or new acceptance token received") {});

            StepVerifier.withVirtualTime(() -> credentialService.getCredential(jwt,tokenResponse, credentialIssuerMetadata, credentials.get(0).format(), credentials.get(0).types()))
                    .thenAwait(Duration.ofSeconds(10))
                    .expectError(FailedDeserializingException.class)
                    .verify();
        }
    }
    @Test
    void getCredentialDomeDeferredCaseTest() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
            String transactionId = "trans123";
            String accessToken = "access-token";
            String deferredEndpoint = "/deferred/endpoint";

            // Expected CredentialResponse to be returned
            CredentialResponse expectedCredentialResponse = CredentialResponse.builder().credential("credentialData").build();

            List<Map.Entry<String, String>> headersForCredentialRequest  = new ArrayList<>();
            headersForCredentialRequest.add(new AbstractMap.SimpleEntry<>(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON));
            headersForCredentialRequest.add(new AbstractMap.SimpleEntry<>(HEADER_AUTHORIZATION, BEARER + accessToken));

            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");
            // Mock the response of the postCredential method
            when(postRequest(deferredEndpoint, headersForCredentialRequest, "credentialRequest"))
                    .thenReturn(Mono.just("response from server"));

            // Configure ObjectMapper to parse the mocked response
            when(objectMapper.readValue("response from server", CredentialResponse.class)).thenReturn(expectedCredentialResponse);

            // Execute the method and verify the results
            StepVerifier.create(credentialService.getCredentialDomeDeferredCase(transactionId, accessToken, deferredEndpoint))
                    .expectNext(expectedCredentialResponse)
                    .verifyComplete();

        }
    }
}
