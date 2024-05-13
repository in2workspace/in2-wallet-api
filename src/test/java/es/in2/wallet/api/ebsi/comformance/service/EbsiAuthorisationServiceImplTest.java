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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static es.in2.wallet.domain.util.ApplicationConstants.GLOBAL_STATE;
import static es.in2.wallet.domain.util.ApplicationUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EbsiAuthorisationServiceImplTest {
    @Mock
    private ObjectMapper objectMapper;
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
            List<Map.Entry<String, String>> headers = new ArrayList<>();

            when(getRequest(anyString(), eq(headers))).thenReturn(Mono.just("redirect response"));
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
    void getRequestWithOurGeneratedCodeVerifierFailedCommunicationExceptionTest() throws JsonProcessingException {
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
            List<Map.Entry<String, String>> headers = new ArrayList<>();

            when(getRequest(anyString(), eq(headers))).thenReturn(Mono.error(new RuntimeException("Error during request")));

            StepVerifier.create(ebsiAuthorisationService.getRequestWithOurGeneratedCodeVerifier(processId, credentialOffer, authorisationServerMetadata, credentialIssuerMetadata, did))
                    .expectError(FailedCommunicationException.class)
                    .verify();
        }
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
            List<Map.Entry<String, String>> headers = new ArrayList<>();

            when(getRequest(anyString(), eq(headers))).thenReturn(Mono.just("redirect response"));
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
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
            // Setup
            String codeVerifier = "codeVerifier";
            String did = "did:key:example";
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().tokenEndpoint("https://example/token").build();
            Map<String, String> params = Map.of("state", GLOBAL_STATE, "code", "authCode");

            TokenResponse expectedTokenResponse = TokenResponse.builder().accessToken("token").build();
            when(objectMapper.readValue(anyString(), eq(TokenResponse.class))).thenReturn(expectedTokenResponse);

            // Mock postRequest to simulate successful token response
            when(postRequest(eq(authorisationServerMetadata.tokenEndpoint()), anyList(), anyString())).thenReturn(Mono.just("token response"));

            // Execute & Verify
            StepVerifier.create(ebsiAuthorisationService.sendTokenRequest(codeVerifier, did, authorisationServerMetadata, params))
                    .expectNext(expectedTokenResponse)
                    .verifyComplete();
        }
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
            when(postRequest(eq(authorisationServerMetadata.tokenEndpoint()), anyList(), anyString()))
                    .thenReturn(Mono.error(new RuntimeException("Error during request")));

            // Execute & Verify
            StepVerifier.create(ebsiAuthorisationService.sendTokenRequest(codeVerifier, did, authorisationServerMetadata, params))
                    .expectError(FailedCommunicationException.class)
                    .verify();
        }
    }
}
