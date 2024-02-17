package es.in2.wallet.api.ebsi.comformance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.api.ebsi.comformance.service.impl.AuthorisationRequestServiceImpl;
import es.in2.wallet.api.exception.FailedCommunicationException;
import es.in2.wallet.api.model.AuthorisationServerMetadata;
import es.in2.wallet.api.model.CredentialIssuerMetadata;
import es.in2.wallet.api.model.CredentialOffer;
import es.in2.wallet.api.util.MessageUtils;
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

import static es.in2.wallet.api.util.MessageUtils.extractAllQueryParams;
import static es.in2.wallet.api.util.MessageUtils.getRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorisationRequestServiceImplTest {
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private AuthorisationRequestServiceImpl authorisationRequestService;

    @Test
    void getRequestWithOurGeneratedCodeVerifierTest() throws JsonProcessingException {
        try (MockedStatic<MessageUtils> ignored = Mockito.mockStatic(MessageUtils.class)){
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

            StepVerifier.create(authorisationRequestService.getRequestWithOurGeneratedCodeVerifier(processId, credentialOffer, authorisationServerMetadata, credentialIssuerMetadata, did))
                    .assertNext(tuple -> {
                        assertEquals("jwt", tuple.getT1());

                        assertTrue(tuple.getT2().length() >= 43 && tuple.getT2().length() <= 128);
                    })
                    .verifyComplete();


        }
    }
    @Test
    void getRequestWithOurGeneratedCodeVerifierFailedCommunicationExceptionTest() throws JsonProcessingException {
        try (MockedStatic<MessageUtils> ignored = Mockito.mockStatic(MessageUtils.class)){
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

            StepVerifier.create(authorisationRequestService.getRequestWithOurGeneratedCodeVerifier(processId, credentialOffer, authorisationServerMetadata, credentialIssuerMetadata, did))
                    .expectError(FailedCommunicationException.class)
                    .verify();
        }
    }

    @Test
    void getRequestWithOurGeneratedCodeVerifierIllegalArgumentExceptionTest() throws JsonProcessingException {
        try (MockedStatic<MessageUtils> ignored = Mockito.mockStatic(MessageUtils.class)){
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

            StepVerifier.create(authorisationRequestService.getRequestWithOurGeneratedCodeVerifier(processId, credentialOffer, authorisationServerMetadata, credentialIssuerMetadata, did))
                    .expectError(IllegalArgumentException.class)
                    .verify();


        }
    }
}
