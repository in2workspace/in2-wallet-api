package es.in2.wallet.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.application.port.AppConfig;
import es.in2.wallet.domain.model.CredentialIssuerMetadata;
import es.in2.wallet.domain.model.CredentialOffer;
import es.in2.wallet.domain.service.impl.CredentialIssuerMetadataServiceImpl;
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

import static es.in2.wallet.domain.util.ApplicationConstants.CONTENT_TYPE;
import static es.in2.wallet.domain.util.ApplicationConstants.CONTENT_TYPE_APPLICATION_JSON;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialIssuerMetadataServiceImplTest {
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private AppConfig appConfig;
    @Mock
    private WebClientConfig webClientConfig;
    @InjectMocks
    private CredentialIssuerMetadataServiceImpl credentialIssuerMetadataService;

    @Test
    void getCredentialIssuerMetadataFromCredentialOfferWithCredentialEndpointHardcodedTest() throws JsonProcessingException {
            String processId = "123";
            CredentialOffer credentialOffer = CredentialOffer.builder().credentialIssuer("example").build();
            CredentialIssuerMetadata credentialIssuerMetadataWithoutTheHardcodedEndpoint = CredentialIssuerMetadata.builder().credentialIssuer("example").build();
            CredentialIssuerMetadata expectedCredentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("example").authorizationServer("https://example.com").build();

            String json = "{\"credential_token\":\"example\"}";
            ObjectMapper objectMapper2 = new ObjectMapper();
            JsonNode jsonNode = objectMapper2.readTree(json);

            when(appConfig.getAuthServerInternalUrl()).thenReturn("https://example.com");
            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                    .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                    .body("response")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

            when(objectMapper.readTree("response")).thenReturn(jsonNode);
            when(objectMapper.treeToValue(jsonNode, CredentialIssuerMetadata.class)).thenReturn(credentialIssuerMetadataWithoutTheHardcodedEndpoint);

            StepVerifier.create(credentialIssuerMetadataService.getCredentialIssuerMetadataFromCredentialOffer(processId, credentialOffer))
                    .expectNext(expectedCredentialIssuerMetadata)
                    .verifyComplete();
    }

    @Test
    void getCredentialIssuerMetadataFromCredentialOfferWithoutCredentialEndpointHardcodedTest() throws JsonProcessingException {
            String processId = "123";
            CredentialOffer credentialOffer = CredentialOffer.builder().credentialIssuer("example").build();
            CredentialIssuerMetadata expectedCredentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("example").build();

            String json = "{\"credential_endpoint\":\"example\"}";
            ObjectMapper objectMapper2 = new ObjectMapper();
            JsonNode jsonNode = objectMapper2.readTree(json);

            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                    .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                    .body("response")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();

            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);
            when(objectMapper.readTree("response")).thenReturn(jsonNode);
            when(objectMapper.readValue("response", CredentialIssuerMetadata.class)).thenReturn(expectedCredentialIssuerMetadata);

            StepVerifier.create(credentialIssuerMetadataService.getCredentialIssuerMetadataFromCredentialOffer(processId, credentialOffer))
                    .expectNext(expectedCredentialIssuerMetadata)
                    .verifyComplete();
    }

    @Test
    void getCredentialIssuerMetadataError() {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "123";
            CredentialOffer credentialOffer = CredentialOffer.builder().credentialIssuer("example").build();

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
            StepVerifier.create(credentialIssuerMetadataService.getCredentialIssuerMetadataFromCredentialOffer(processId, credentialOffer))
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }

    @Test
    void getCredentialIssuerMetadataFromCredentialOfferFailedDeserializingException() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "123";
            CredentialOffer credentialOffer = CredentialOffer.builder().credentialIssuer("example").build();
            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                    .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                    .body("response")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();

            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);
            when(objectMapper.readTree("response")).thenThrow(new JsonProcessingException("Deserialization error") {
            });

            StepVerifier.create(credentialIssuerMetadataService.getCredentialIssuerMetadataFromCredentialOffer(processId, credentialOffer))
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }


}
