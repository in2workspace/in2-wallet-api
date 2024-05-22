package es.in2.wallet.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.domain.exception.FailedDeserializingException;
import es.in2.wallet.domain.exception.FailedSerializingException;
import es.in2.wallet.domain.model.CredentialIssuerMetadata;
import es.in2.wallet.domain.model.CredentialOffer;
import es.in2.wallet.domain.model.CredentialResponse;
import es.in2.wallet.domain.model.TokenResponse;
import es.in2.wallet.domain.service.impl.CredentialServiceImpl;
import es.in2.wallet.infrastructure.core.config.WebClientConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static es.in2.wallet.domain.util.ApplicationConstants.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialServiceImplTest {
    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WebClientConfig webClientConfig;
    @InjectMocks
    private CredentialServiceImpl credentialService;

    @Test
    void getCredentialTest() throws JsonProcessingException {

            String jwt = "ey34324";

            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();

            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();

            CredentialResponse mockCredentialResponse = CredentialResponse.builder().credential("credential").c_nonce("fresh_nonce").c_nonce_expires_in(600).format("jwt").build();


            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");
            when(objectMapper.readValue(anyString(), eq(CredentialResponse.class))).thenReturn(mockCredentialResponse);

            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                    .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                    .body("credential")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

            StepVerifier.create(credentialService.getCredential(jwt,tokenResponse, credentialIssuerMetadata,JWT_VC, List.of("VerifiableCredential","LEARCredential")))
                    .expectNext(mockCredentialResponse)
                    .verifyComplete();
    }
    @Test
    void getCredentialTestRuntimeException() throws JsonProcessingException {

        String jwt = "ey34324";

        TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();

        CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();


        when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");

        ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

        // Create a mock ClientResponse for a successful response
        ClientResponse clientResponse = ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                .body("error")
                .build();

        // Stub the exchange function to return the mock ClientResponse
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

        WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

        StepVerifier.create(credentialService.getCredential(jwt,tokenResponse, credentialIssuerMetadata,JWT_VC, List.of("VerifiableCredential","LEARCredential")))
                .expectError(RuntimeException.class)
                .verify();
    }
    @Test
    void getCredentialTestWithoutTypes() throws JsonProcessingException {

            String jwt = "ey34324";

            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();

            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();

            CredentialResponse mockCredentialResponse = CredentialResponse.builder().credential("credential").c_nonce("fresh_nonce").c_nonce_expires_in(600).format("jwt").build();

            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");
            when(objectMapper.readValue(anyString(), eq(CredentialResponse.class))).thenReturn(mockCredentialResponse);

            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                    .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                    .body("credential")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

            StepVerifier.create(credentialService.getCredential(jwt,tokenResponse, credentialIssuerMetadata,JWT_VC, null))
                    .expectNext(mockCredentialResponse)
                    .verifyComplete();
    }

    @Test
    void getCredentialTestForFiware() throws JsonProcessingException {

            String jwt = "ey34324";

            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();

            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();

            CredentialResponse mockCredentialResponse = CredentialResponse.builder().credential("credential").c_nonce("fresh_nonce").c_nonce_expires_in(600).format("jwt").build();

            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");
            when(objectMapper.readValue(anyString(), eq(CredentialResponse.class))).thenReturn(mockCredentialResponse);

            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                    .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                    .body("credential")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

            StepVerifier.create(credentialService.getCredential(jwt,tokenResponse, credentialIssuerMetadata,JWT_VC, List.of("LEARCredential")))
                    .expectNext(mockCredentialResponse)
                    .verifyComplete();
    }
    @Test
    void getCredentialFailedCommunicationErrorTest() throws JsonProcessingException{

            String jwt = "ey34324";

            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();

            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();

            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");

            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.BAD_REQUEST)
                    .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                    .body("error")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

            StepVerifier.create(credentialService.getCredential(jwt,tokenResponse, credentialIssuerMetadata ,JWT_VC, List.of("LEARCredential")))
                    .expectError(RuntimeException.class)
                    .verify();
    }
    @Test
    void getCredentialFailedDeserializingErrorTest() throws JsonProcessingException{

            String jwt = "ey34324";

            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();

            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();

            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");
            when(objectMapper.readValue(anyString(), eq(CredentialResponse.class)))
                    .thenThrow(new JsonProcessingException("Deserialization error") {});

            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                    .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                    .body("credential")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

            StepVerifier.create(credentialService.getCredential(jwt,tokenResponse, credentialIssuerMetadata ,JWT_VC, List.of("LEARCredential")))
                    .expectError(FailedDeserializingException.class)
                    .verify();
    }

    @Test
    void getCredentialFailedSerializingExceptionTest() throws JsonProcessingException {
            String jwt = "ey34324";

            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();

            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").build();

            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);
            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

            when(objectMapper.writeValueAsString(any()))
                    .thenThrow(new JsonProcessingException("Serialization error") {});

            StepVerifier.create(credentialService.getCredential(jwt,tokenResponse, credentialIssuerMetadata ,JWT_VC, List.of("LEARCredential")))
                    .expectError(FailedSerializingException.class)
                    .verify();
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
            String jwt = "ey34324";
            CredentialOffer.Credential credential = CredentialOffer.Credential.builder().types(List.of("LEARCredential")).format("jwt_vc").build();
            List<CredentialOffer.Credential> credentials = List.of(credential);

            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();

            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").deferredCredentialEndpoint("deferredEndpoint").build();


            CredentialResponse mockDeferredResponse1 = CredentialResponse.builder()
                    .acceptanceToken("deferredToken")
                    .build();
            CredentialResponse mockDeferredResponse2 = CredentialResponse.builder()
                    .acceptanceToken("deferredTokenRecursive")
                    .build();
            CredentialResponse mockFinalCredentialResponse = CredentialResponse.builder()
                    .credential("finalCredential")
                    .build();

            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");


            WebClient webClient = WebClient.builder().exchangeFunction(request -> {
                String url = request.url().toString();
                String header = request.headers().getFirst(HttpHeaders.AUTHORIZATION);
                ClientResponse.Builder responseBuilder = ClientResponse.create(HttpStatus.OK)
                        .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON);

                if (url.equals(credentialIssuerMetadata.credentialEndpoint())) {
                    return Mono.just(responseBuilder.body("deferredResponse").build());
                } else if (url.equals(credentialIssuerMetadata.deferredCredentialEndpoint())) {
                    assert header != null;
                    if (header.equals(BEARER + "deferredToken")) {
                        return Mono.just(responseBuilder.body("deferredResponseRecursive").build());
                    }
                    return Mono.just(responseBuilder.body("finalCredentialResponse").build());
                }
                return Mono.just(responseBuilder.build());
            }).build();
            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

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

    @Test
    void getCredentialDeferredErrorTest() throws JsonProcessingException {
            String jwt = "ey34324";
            CredentialOffer.Credential credential = CredentialOffer.Credential.builder().types(List.of("LEARCredential")).format("jwt_vc").build();
            List<CredentialOffer.Credential> credentials = List.of(credential);

            TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();

            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").deferredCredentialEndpoint("deferredEndpoint").build();


            CredentialResponse mockDeferredResponse1 = CredentialResponse.builder()
                    .acceptanceToken("deferredToken")
                    .build();


            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");

            WebClient webClient = WebClient.builder().exchangeFunction(request -> {
                String url = request.url().toString();
                ClientResponse.Builder responseBuilder = ClientResponse.create(HttpStatus.OK)
                        .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON);

                if (url.equals(credentialIssuerMetadata.credentialEndpoint())) {
                    return Mono.just(responseBuilder.body("deferredResponse").build());
                } else if (url.equals(credentialIssuerMetadata.deferredCredentialEndpoint())) {
                    return Mono.just(responseBuilder.body("deferredResponseRecursive").build());

                }
                return Mono.just(responseBuilder.build());
            }).build();
            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);


            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");
            when(objectMapper.readValue("deferredResponse", CredentialResponse.class)).thenReturn(mockDeferredResponse1);

            when(objectMapper.readValue("deferredResponseRecursive", CredentialResponse.class))
                    .thenThrow(new IllegalStateException("No credential or new acceptance token received") {});

            StepVerifier.withVirtualTime(() -> credentialService.getCredential(jwt,tokenResponse, credentialIssuerMetadata, credentials.get(0).format(), credentials.get(0).types()))
                    .thenAwait(Duration.ofSeconds(10))
                    .expectError(FailedDeserializingException.class)
                    .verify();

    }

    @Test
    void getCredentialDeferredErrorDuringSecondRequestTest() throws JsonProcessingException {
        String jwt = "ey34324";
        CredentialOffer.Credential credential = CredentialOffer.Credential.builder().types(List.of("LEARCredential")).format("jwt_vc").build();
        List<CredentialOffer.Credential> credentials = List.of(credential);

        TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").cNonce("nonce").build();

        CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").credentialEndpoint("endpoint").deferredCredentialEndpoint("deferredEndpoint").build();


        CredentialResponse mockDeferredResponse1 = CredentialResponse.builder()
                .acceptanceToken("deferredToken")
                .build();


        when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");

        WebClient webClient = WebClient.builder().exchangeFunction(request -> {
            String url = request.url().toString();
            ClientResponse.Builder responseBuilder = ClientResponse.create(HttpStatus.OK)
                    .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON);

            if (url.equals(credentialIssuerMetadata.credentialEndpoint())) {
                return Mono.just(responseBuilder.body("deferredResponse").build());
            } else if (url.equals(credentialIssuerMetadata.deferredCredentialEndpoint())) {
                return Mono.just(responseBuilder.statusCode(HttpStatus.BAD_REQUEST).build());
            }
            return Mono.just(responseBuilder.build());
        }).build();
        when(webClientConfig.centralizedWebClient()).thenReturn(webClient);


        when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");
        when(objectMapper.readValue("deferredResponse", CredentialResponse.class)).thenReturn(mockDeferredResponse1);


        StepVerifier.withVirtualTime(() -> credentialService.getCredential(jwt,tokenResponse, credentialIssuerMetadata, credentials.get(0).format(), credentials.get(0).types()))
                .thenAwait(Duration.ofSeconds(10))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void getCredentialDomeDeferredCaseTest() throws JsonProcessingException {
            String transactionId = "trans123";
            String accessToken = "access-token";
            String deferredEndpoint = "/deferred/endpoint";

            // Expected CredentialResponse to be returned
            CredentialResponse expectedCredentialResponse = CredentialResponse.builder().credential("credentialData").build();

            when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");

            // Mock the response of the postCredential method
            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                    .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                    .body("credential")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);
            // Configure ObjectMapper to parse the mocked response
            when(objectMapper.readValue("credential", CredentialResponse.class)).thenReturn(expectedCredentialResponse);

            // Execute the method and verify the results
            StepVerifier.create(credentialService.getCredentialDomeDeferredCase(transactionId, accessToken, deferredEndpoint))
                    .expectNext(expectedCredentialResponse)
                    .verifyComplete();
    }
    @Test
    void getCredentialDomeDeferredCaseTestFailedDeserializingException() throws JsonProcessingException {
        String transactionId = "trans123";
        String accessToken = "access-token";
        String deferredEndpoint = "/deferred/endpoint";

        when(objectMapper.writeValueAsString(any())).thenReturn("credentialRequest");

        // Mock the response of the postCredential method
        ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

        // Create a mock ClientResponse for a successful response
        ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                .body("invalid body")
                .build();

        // Stub the exchange function to return the mock ClientResponse
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

        WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        when(webClientConfig.centralizedWebClient()).thenReturn(webClient);
        // Configure ObjectMapper to parse the mocked response
        when(objectMapper.readValue("invalid body", CredentialResponse.class))
                .thenThrow(new IllegalStateException("The response have a invalid format") {});

        // Execute the method and verify the results
        StepVerifier.create(credentialService.getCredentialDomeDeferredCase(transactionId, accessToken, deferredEndpoint))
                .expectError(FailedDeserializingException.class)
                .verify();
    }
}
