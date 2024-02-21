package es.in2.wallet.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.api.model.CredentialOffer;
import es.in2.wallet.api.service.impl.CredentialOfferServiceImpl;
import es.in2.wallet.api.util.ApplicationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static es.in2.wallet.api.util.ApplicationUtils.getRequest;
import static es.in2.wallet.api.util.MessageUtils.CONTENT_TYPE;
import static es.in2.wallet.api.util.MessageUtils.CONTENT_TYPE_APPLICATION_JSON;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialOfferServiceImplTest {
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private CredentialOfferServiceImpl credentialOfferService;

    @Test
    void getCredentialOfferWithHardcodedCredentialListTest() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "123";
            String qrContent = "openid-credential-offer://?credential_offer_uri=https%3A%2F%2Fapi-conformance.ebsi.eu%2Fconformance%2Fv3%2Fissuer-mock%2Foffers%2F00067a17-681f-4b41-9794-cb7c98570a7a";
            List<Map.Entry<String, String>> headers = List.of(Map.entry(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON));
            CredentialOffer.Credential credential = CredentialOffer.Credential.builder().types(List.of("LEARCredential")).build();

            CredentialOffer credentialOffer = CredentialOffer.builder().credentialIssuer("https://example.com").build();
            CredentialOffer expectedCredentialOffer = CredentialOffer.builder().credentialIssuer("https://example.com").credentials(List.of(credential)).build();

            String json = "{\"credentials\":[\"LEARCredential\"], \"credential_issuer\":\"https://example.com\"}";
            String json2 = "{\"credential_issuer\":\"https://example.com\"}";
            ObjectMapper objectMapper2 = new ObjectMapper();
            JsonNode jsonNode = objectMapper2.readTree(json);
            JsonNode jsonNode2 = objectMapper2.readTree(json2);

            when(getRequest("https://api-conformance.ebsi.eu/conformance/v3/issuer-mock/offers/00067a17-681f-4b41-9794-cb7c98570a7a",headers)).thenReturn(Mono.just("response"));

            when(objectMapper.readTree("response")).thenReturn(jsonNode);
            when(objectMapper.treeToValue(jsonNode2, CredentialOffer.class)).thenReturn(credentialOffer);

            StepVerifier.create(credentialOfferService.getCredentialOfferFromCredentialOfferUri(processId,qrContent))
                    .expectNext(expectedCredentialOffer)
                    .verifyComplete();

        }
    }
    @Test
    void getCredentialOfferWithoutHardcodedCredentialListTest() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "123";
            String qrContent = "openid-credential-offer://?credential_offer_uri=https%3A%2F%2Fapi-conformance.ebsi.eu%2Fconformance%2Fv3%2Fissuer-mock%2Foffers%2F00067a17-681f-4b41-9794-cb7c98570a7a";
            List<Map.Entry<String, String>> headers = List.of(Map.entry(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON));
            CredentialOffer.Credential credential = CredentialOffer.Credential.builder().format("jwt_vc").build();
            CredentialOffer expectedCredentialOffer = CredentialOffer.builder().credentialIssuer("https://example.com").credentials(List.of(credential)).build();

            String json = "{\"credentials\":[{\"format\":\"jwt_vc\"}], \"credential_issuer\":\"https://example.com\"}";
            ObjectMapper objectMapper2 = new ObjectMapper();
            JsonNode jsonNode = objectMapper2.readTree(json);

            when(getRequest("https://api-conformance.ebsi.eu/conformance/v3/issuer-mock/offers/00067a17-681f-4b41-9794-cb7c98570a7a",headers)).thenReturn(Mono.just("response"));

            when(objectMapper.readTree("response")).thenReturn(jsonNode);
            when(objectMapper.readValue("response", CredentialOffer.class)).thenReturn(expectedCredentialOffer);

            StepVerifier.create(credentialOfferService.getCredentialOfferFromCredentialOfferUri(processId,qrContent))
                    .expectNext(expectedCredentialOffer)
                    .verifyComplete();

        }
    }


}
