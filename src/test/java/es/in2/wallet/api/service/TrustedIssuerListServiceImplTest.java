package es.in2.wallet.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.domain.exception.FailedCommunicationException;
import es.in2.wallet.domain.exception.IssuerNotAuthorizedException;
import es.in2.wallet.domain.exception.JsonReadingException;
import es.in2.wallet.domain.model.IssuerAttribute;
import es.in2.wallet.domain.model.IssuerCredentialsCapabilities;
import es.in2.wallet.domain.model.IssuerResponse;
import es.in2.wallet.domain.model.TimeRange;
import es.in2.wallet.domain.service.impl.TrustedIssuerListServiceImpl;
import es.in2.wallet.infrastructure.core.config.WebClientConfig;
import es.in2.wallet.infrastructure.core.config.properties.TrustedIssuerListProperties;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrustedIssuerListServiceImplTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WebClientConfig webClientConfig;

    @Mock
    private TrustedIssuerListProperties trustedIssuerListProperties;

    @InjectMocks
    private TrustedIssuerListServiceImpl trustedIssuerListService;

    @Test
    void testGetTrustedIssuerListData_Success() throws Exception {
        // Arrange
        String id = "issuer123";
        String responseBody = "{\"attributes\":[{\"body\":\"eyJjcmVkZW50aWFsc1R5cGUiOiAiQ3JlZGVudGlhbFR5cGUifQ==\"}]}";
        IssuerResponse issuerResponse = IssuerResponse.builder()
                .did("did:example:issuer123")
                .attributes(List.of(IssuerAttribute.builder()
                        .body("eyJ2YWxpZEZvciI6IHsic3RhcnREYXRlIjogIjIwMjMtMDEtMDEiLCAiZW5kRGF0ZSI6ICIyMDI1LTAxLTAxIn0sICJjcmVkZW50aWFsc1R5cGUiOiAiQ3JlZGVudGlhbFR5cGUifQ==")
                        .build()))
                .build();

        IssuerCredentialsCapabilities expectedCapability = IssuerCredentialsCapabilities.builder()
                .credentialsType("CredentialType")
                .validFor(TimeRange.builder().to("2023-01-01").from("2025-01-01").build())
                .build();

        // WebClient setup
        ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);
        ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(responseBody)
                .build();
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

        WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        when(webClientConfig.centralizedWebClient()).thenReturn(webClient);
        when(trustedIssuerListProperties.uri()).thenReturn("https://example.com/issuers/");

        // ObjectMapper setup
        when(objectMapper.readValue(responseBody, IssuerResponse.class)).thenReturn(issuerResponse);
        when(objectMapper.readValue(anyString(), eq(IssuerCredentialsCapabilities.class)))
                .thenReturn(expectedCapability);

        // Act
        Mono<List<IssuerCredentialsCapabilities>> result = trustedIssuerListService.getTrustedIssuerListData(id);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(list -> list.size() == 1 && list.get(0).credentialsType().equals("CredentialType"))
                .verifyComplete();
    }

    @Test
    void testGetTrustedIssuerListData_404Error() {
        // Arrange
        String id = "nonExistentIssuer";
        ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);
        ClientResponse clientResponse = ClientResponse.create(HttpStatus.NOT_FOUND).build();
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

        WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        when(webClientConfig.centralizedWebClient()).thenReturn(webClient);
        when(trustedIssuerListProperties.uri()).thenReturn("https://example.com/issuers/");

        // Act
        Mono<List<IssuerCredentialsCapabilities>> result = trustedIssuerListService.getTrustedIssuerListData(id);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof IssuerNotAuthorizedException &&
                        throwable.getMessage().equals("Issuer with id: nonExistentIssuer not found."))
                .verify();
    }

    @Test
    void testGetTrustedIssuerListData_5xxError() {
        // Arrange
        String id = "issuer123";
        ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);
        ClientResponse clientResponse = ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build();
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

        WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        when(webClientConfig.centralizedWebClient()).thenReturn(webClient);
        when(trustedIssuerListProperties.uri()).thenReturn("https://example.com/issuers/");

        // Act
        Mono<List<IssuerCredentialsCapabilities>> result = trustedIssuerListService.getTrustedIssuerListData(id);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof FailedCommunicationException &&
                        throwable.getMessage().contains("Client error while fetching issuer data"))
                .verify();
    }

    @Test
    void testGetTrustedIssuerListData_JsonParsingError() throws Exception {
        // Arrange
        String id = "issuer123";
        String responseBody = "{\"invalidJson\":}"; // Malformed JSON to trigger error
        ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);
        ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(responseBody)
                .build();
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

        WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        when(webClientConfig.centralizedWebClient()).thenReturn(webClient);
        when(trustedIssuerListProperties.uri()).thenReturn("https://example.com/issuers/");

        // Simulate JSON parsing error
        when(objectMapper.readValue(responseBody, IssuerResponse.class)).thenThrow(new JsonReadingException("Error parsing JSON"));

        // Act
        Mono<List<IssuerCredentialsCapabilities>> result = trustedIssuerListService.getTrustedIssuerListData(id);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof JsonReadingException &&
                        throwable.getMessage().contains("Error parsing JSON"))
                .verify();
    }
}

