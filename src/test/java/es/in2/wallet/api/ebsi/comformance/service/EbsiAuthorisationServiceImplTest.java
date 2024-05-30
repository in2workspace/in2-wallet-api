package es.in2.wallet.api.ebsi.comformance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.domain.exception.FailedCommunicationException;
import es.in2.wallet.domain.model.AuthorisationServerMetadata;
import es.in2.wallet.domain.model.CredentialIssuerMetadata;
import es.in2.wallet.domain.model.CredentialOffer;
import es.in2.wallet.domain.model.TokenResponse;
import es.in2.wallet.domain.service.impl.EbsiAuthorisationServiceImpl;
import es.in2.wallet.domain.util.ApplicationUtils;
import es.in2.wallet.infrastructure.core.config.WebClientConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static es.in2.wallet.domain.util.ApplicationConstants.*;
import static es.in2.wallet.domain.util.ApplicationUtils.extractAllQueryParams;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EbsiAuthorisationServiceImplTest {
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private WebClientConfig webClientConfig;
    @InjectMocks
    private EbsiAuthorisationServiceImpl ebsiAuthorisationService;

    @Test
    void getRequestWithOurGeneratedCodeVerifierTest() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
            String processId = "processId";
            CredentialOffer.Credential credential = CredentialOffer.Credential.builder().format("jwt_vc").types(List.of("VC")).build();
            CredentialOffer.Grant grant = CredentialOffer.Grant.builder().authorizationCodeGrant(CredentialOffer.Grant.AuthorizationCodeGrant.builder().issuerState("state").build()).build();
            CredentialOffer credentialOffer = CredentialOffer.builder().credentials(List.of(credential)).grant(grant).build();
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().authorizationEndpoint("https://example").build();
            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").build();
            String did = "did:key:example";

            String json = "{\"request\":\"auth request\"}";
            ObjectMapper objectMapper2 = new ObjectMapper();
            JsonNode jsonNode = objectMapper2.readTree(json);

            when(objectMapper.valueToTree(any())).thenReturn(jsonNode);
            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.FOUND)
                    .header("Location", "redirect response")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);


            Map<String, String> map = new HashMap<>();
            map.put("request","jwt");
            when(extractAllQueryParams("redirect response")).thenReturn(Mono.just(map));

            StepVerifier.create(ebsiAuthorisationService.getRequestWithOurGeneratedCodeVerifier(processId, credentialOffer, authorisationServerMetadata, credentialIssuerMetadata, did))
                    .assertNext(tuple -> {
                        assertEquals("jwt", tuple.getT1());

                        assertTrue(tuple.getT2().length() >= 43 && tuple.getT2().length() <= 128);
                    })
                    .verifyComplete();


        }
    }

    @Test
    void getRequestWithOurGeneratedCodeVerifierTestWithRequestUri() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
            String processId = "processId";
            CredentialOffer.Credential credential = CredentialOffer.Credential.builder().format("jwt_vc").types(List.of("VC")).build();
            CredentialOffer.Grant grant = CredentialOffer.Grant.builder().authorizationCodeGrant(CredentialOffer.Grant.AuthorizationCodeGrant.builder().issuerState("state").build()).build();
            CredentialOffer credentialOffer = CredentialOffer.builder().credentials(List.of(credential)).grant(grant).build();
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().authorizationEndpoint("https://example").build();
            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").build();
            String did = "did:key:example";

            String json = "{\"request\":\"auth request\"}";
            ObjectMapper objectMapper2 = new ObjectMapper();
            JsonNode jsonNode = objectMapper2.readTree(json);

            when(objectMapper.valueToTree(any())).thenReturn(jsonNode);

            WebClient webClient = WebClient.builder().exchangeFunction(request -> {
                String url = request.url().toString();
                ClientResponse.Builder responseBuilder = ClientResponse.create(HttpStatus.OK)
                        .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON);

                if (url.contains(authorisationServerMetadata.authorizationEndpoint())) {
                    return Mono.just(responseBuilder.statusCode(HttpStatus.FOUND).header("Location", "redirect response").build());
                } else {
                    return Mono.just(responseBuilder.body("jwt").build());
                }
            }).build();
            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

            Map<String, String> map = new HashMap<>();
            map.put("request_uri","https://resource");
            when(extractAllQueryParams("redirect response")).thenReturn(Mono.just(map));


            StepVerifier.create(ebsiAuthorisationService.getRequestWithOurGeneratedCodeVerifier(processId, credentialOffer, authorisationServerMetadata, credentialIssuerMetadata, did))
                    .assertNext(tuple -> {
                        assertEquals("jwt", tuple.getT1());

                        assertTrue(tuple.getT2().length() >= 43 && tuple.getT2().length() <= 128);
                    })
                    .verifyComplete();


        }
    }
    @Test
    void getRequestWithOurGeneratedCodeVerifierFailedCommunicationExceptionTest() throws JsonProcessingException {
            String processId = "processId";
            CredentialOffer.Credential credential = CredentialOffer.Credential.builder().format("jwt_vc").types(List.of("VC")).build();
            CredentialOffer.Grant grant = CredentialOffer.Grant.builder().authorizationCodeGrant(CredentialOffer.Grant.AuthorizationCodeGrant.builder().issuerState("state").build()).build();
            CredentialOffer credentialOffer = CredentialOffer.builder().credentials(List.of(credential)).grant(grant).build();
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().authorizationEndpoint("https://example").build();
            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").build();
            String did = "did:key:example";

            String json = "{\"request\":\"auth request\"}";
            ObjectMapper objectMapper2 = new ObjectMapper();
            JsonNode jsonNode = objectMapper2.readTree(json);

            when(objectMapper.valueToTree(any())).thenReturn(jsonNode);

            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.BAD_REQUEST)
                    .body("Error")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);


            StepVerifier.create(ebsiAuthorisationService.getRequestWithOurGeneratedCodeVerifier(processId, credentialOffer, authorisationServerMetadata, credentialIssuerMetadata, did))
                    .expectError(FailedCommunicationException.class)
                    .verify();
    }

    @Test
    void getRequestWithOurGeneratedCodeVerifierIllegalArgumentExceptionTest() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
            String processId = "processId";
            CredentialOffer.Credential credential = CredentialOffer.Credential.builder().format("jwt_vc").types(List.of("VC")).build();
            CredentialOffer.Grant grant = CredentialOffer.Grant.builder().authorizationCodeGrant(CredentialOffer.Grant.AuthorizationCodeGrant.builder().issuerState("state").build()).build();
            CredentialOffer credentialOffer = CredentialOffer.builder().credentials(List.of(credential)).grant(grant).build();
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().authorizationEndpoint("https://example").build();
            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").build();
            String did = "did:key:example";

            String json = "{\"request\":\"auth request\"}";
            ObjectMapper objectMapper2 = new ObjectMapper();
            JsonNode jsonNode = objectMapper2.readTree(json);

            when(objectMapper.valueToTree(any())).thenReturn(jsonNode);

            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.FOUND)
                    .header("Location", "redirect response")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

            Map<String, String> map = new HashMap<>();
            map.put("not known property","example");
            when(extractAllQueryParams("redirect response")).thenReturn(Mono.just(map));

            StepVerifier.create(ebsiAuthorisationService.getRequestWithOurGeneratedCodeVerifier(processId, credentialOffer, authorisationServerMetadata, credentialIssuerMetadata, did))
                    .expectError(IllegalArgumentException.class)
                    .verify();


        }
    }

    @Test
    void sendTokenRequest_SuccessfulFlow() throws JsonProcessingException {
            // Setup
            String codeVerifier = "codeVerifier";
            String did = "did:key:example";
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().tokenEndpoint("https://example/token").build();
            Map<String, String> params = Map.of("state", GLOBAL_STATE, "code", "authCode");

            TokenResponse expectedTokenResponse = TokenResponse.builder().accessToken("token").build();
            when(objectMapper.readValue(anyString(), eq(TokenResponse.class))).thenReturn(expectedTokenResponse);

            // Mock postRequest to simulate successful token response

            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                    .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                    .body("token response")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

            // Execute & Verify
            StepVerifier.create(ebsiAuthorisationService.sendTokenRequest(codeVerifier, did, authorisationServerMetadata, params))
                    .expectNext(expectedTokenResponse)
                    .verifyComplete();

    }
    @Test
    void sendTokenRequest_FailedCommunicationException() {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
            // Setup
            String codeVerifier = "codeVerifier";
            String did = "did:key:example";
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().tokenEndpoint("https://example/token").build();
            Map<String, String> params = Map.of("state", GLOBAL_STATE, "code", "authCode");

            // Mock postRequest to simulate a failure in communication

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

            // Execute & Verify
            StepVerifier.create(ebsiAuthorisationService.sendTokenRequest(codeVerifier, did, authorisationServerMetadata, params))
                    .expectError(FailedCommunicationException.class)
                    .verify();
        }
    }
}
