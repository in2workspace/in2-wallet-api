package es.in2.wallet.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.domain.model.CredentialOffer;
import es.in2.wallet.domain.service.impl.CredentialOfferServiceImpl;
import es.in2.wallet.infrastructure.core.config.WebClientConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialOfferServiceImplTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WebClientConfig webClientConfig;

    @InjectMocks
    private CredentialOfferServiceImpl credentialOfferService;


    @Test
    void testGetCredentialOfferFromCredentialOfferUriWhenTheresNoCredentialsNode_Success() throws Exception {
        String processId = "123";
        String credentialOfferUri = "openid://?credential_offer=https://example.com/offer";
        CredentialOffer expectedCredentialOffer = CredentialOffer
                .builder()
                .credentialConfigurationsIds(List.of("UniversityDegreeCredential"))
                .credentialIssuer("https://credential-issuer.example.com")
                .grant(CredentialOffer.Grant.builder()
                        .preAuthorizedCodeGrant(CredentialOffer.Grant.PreAuthorizedCodeGrant.builder()
                                .preAuthorizedCode("oaKazRN8I0IbtZ0C7JuMn5")
                                .txCode(CredentialOffer.Grant.PreAuthorizedCodeGrant.TxCode.builder()
                                        .length(4)
                                        .inputMode("numeric")
                                        .description("Please provide the one-time code that was sent via e-mail")
                                        .build())
                                .build())
                        .build())
                .build();

        ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);
        String credentialOfferJsonNode = """
                {
                   "credential_issuer": "https://credential-issuer.example.com",
                   "credential_configuration_ids": [
                      "UniversityDegreeCredential"
                   ],
                   "grants": {
                      "urn:ietf:params:oauth:grant-type:pre-authorized_code": {
                         "pre-authorized_code": "oaKazRN8I0IbtZ0C7JuMn5",
                         "tx_code": {
                            "length": 4,
                            "input_mode": "numeric",
                            "description": "Please provide the one-time code that was sent via e-mail"
                         }
                      }
                   }
                }
                """;

        // Create a mock ClientResponse for a successful response
        ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body("credential offer")
                .build();

        // Stub the exchange function to return the mock ClientResponse
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

        WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

        ObjectMapper objectMapper1 = new ObjectMapper();
        JsonNode jsonNode = objectMapper1.readTree(credentialOfferJsonNode);
        when(objectMapper.readTree("credential offer")).thenReturn(jsonNode);
        when(objectMapper.treeToValue(any(JsonNode.class), any(Class.class))).thenReturn(expectedCredentialOffer);

        StepVerifier.create(credentialOfferService.getCredentialOfferFromCredentialOfferUri(processId, credentialOfferUri))
                .expectNext(expectedCredentialOffer)
                .verifyComplete();
    }

    @Test
    void testGetCredentialOfferFromCredentialOfferUriWhenCredentialOfferUriIsParsed_Success() throws JsonProcessingException {
        String processId = "123";
        String credentialOfferUri = "https://example.com/offer";
        CredentialOffer expectedCredentialOffer = CredentialOffer
                .builder()
                .credentialConfigurationsIds(List.of("UniversityDegreeCredential"))
                .credentialIssuer("https://credential-issuer.example.com")
                .grant(CredentialOffer.Grant.builder()
                        .preAuthorizedCodeGrant(CredentialOffer.Grant.PreAuthorizedCodeGrant.builder()
                                .preAuthorizedCode("oaKazRN8I0IbtZ0C7JuMn5")
                                .txCode(CredentialOffer.Grant.PreAuthorizedCodeGrant.TxCode.builder()
                                        .length(4)
                                        .inputMode("numeric")
                                        .description("Please provide the one-time code that was sent via e-mail")
                                        .build())
                                .build())
                        .build())
                .build();

        ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);
        String credentialOfferJsonNode = """
                {
                   "credential_issuer": "https://credential-issuer.example.com",
                   "credential_configuration_ids": [
                      "UniversityDegreeCredential"
                   ],
                   "grants": {
                      "urn:ietf:params:oauth:grant-type:pre-authorized_code": {
                         "pre-authorized_code": "oaKazRN8I0IbtZ0C7JuMn5",
                         "tx_code": {
                            "length": 4,
                            "input_mode": "numeric",
                            "description": "Please provide the one-time code that was sent via e-mail"
                         }
                      }
                   }
                }
                """;

        // Create a mock ClientResponse for a successful response
        ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body("credential offer")
                .build();

        // Stub the exchange function to return the mock ClientResponse
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

        WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

        ObjectMapper objectMapper1 = new ObjectMapper();
        JsonNode jsonNode = objectMapper1.readTree(credentialOfferJsonNode);
        when(objectMapper.readTree("credential offer")).thenReturn(jsonNode);
        when(objectMapper.treeToValue(any(JsonNode.class), any(Class.class))).thenReturn(expectedCredentialOffer);

        StepVerifier.create(credentialOfferService.getCredentialOfferFromCredentialOfferUri(processId, credentialOfferUri))
                .expectNext(expectedCredentialOffer)
                .verifyComplete();
    }

}
