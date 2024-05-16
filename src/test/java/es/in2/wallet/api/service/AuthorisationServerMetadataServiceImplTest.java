package es.in2.wallet.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.application.port.AppConfig;
import es.in2.wallet.domain.model.AuthorisationServerMetadata;
import es.in2.wallet.domain.model.CredentialIssuerMetadata;
import es.in2.wallet.domain.service.impl.AuthorisationServerMetadataServiceImpl;
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
class AuthorisationServerMetadataServiceImplTest {
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private AppConfig appConfig;
    @Mock
    private WebClientConfig webClientConfig;
    @InjectMocks
    private AuthorisationServerMetadataServiceImpl authorisationServerMetadataService;

    @Test
    void getAuthorizationServerMetadataFromCredentialIssuerMetadataWithTokenEndpointHardcodedTest() throws JsonProcessingException {
            String processId = "123";
            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().authorizationServer("example").build();
            AuthorisationServerMetadata authorizationServerMetadata = AuthorisationServerMetadata.builder().tokenEndpoint("https://example.com/token").build();
            AuthorisationServerMetadata expectedAuthorizationServerMetadataWithTokenEndpointHardcodedTest = AuthorisationServerMetadata.builder().tokenEndpoint("https://example.com/example/token").build();


            when(appConfig.getAuthServerExternalUrl()).thenReturn("https://example.com");
            when(appConfig.getAuthServerTokenEndpoint()).thenReturn("https://example.com/example/token");


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

            when(objectMapper.readValue("response", AuthorisationServerMetadata.class)).thenReturn(authorizationServerMetadata);

            StepVerifier.create(authorisationServerMetadataService.getAuthorizationServerMetadataFromCredentialIssuerMetadata(processId, credentialIssuerMetadata))
                    .expectNext(expectedAuthorizationServerMetadataWithTokenEndpointHardcodedTest)
                    .verifyComplete();

    }

    @Test
    void getCredentialIssuerMetadataError() {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "123";
            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().authorizationServer("example").build();

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

            StepVerifier.create(authorisationServerMetadataService.getAuthorizationServerMetadataFromCredentialIssuerMetadata(processId, credentialIssuerMetadata))
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }

}

