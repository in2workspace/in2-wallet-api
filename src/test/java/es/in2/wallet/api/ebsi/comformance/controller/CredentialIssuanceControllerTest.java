package es.in2.wallet.api.ebsi.comformance.controller;

import es.in2.wallet.application.workflow.issuance.CredentialIssuanceCommonWorkflow;
import es.in2.wallet.application.workflow.issuance.CredentialIssuanceEbsiWorkflow;
import es.in2.wallet.domain.model.CredentialOfferRequest;
import es.in2.wallet.infrastructure.ebsi.controller.CredentialIssuanceController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CredentialIssuanceControllerTest {

    @Mock
    private CredentialIssuanceEbsiWorkflow ebsiCredentialIssuanceServiceFacade;

    @Mock
    private CredentialIssuanceCommonWorkflow commonCredentialIssuanceServiceFacade;

    @InjectMocks
    private CredentialIssuanceController credentialIssuanceController;

    @Test
    void testRequestVerifiableCredentialWithEbsiUri() {
        // Arrange
        String authorizationHeader = "Bearer test-token";
        CredentialOfferRequest credentialOfferRequest = new CredentialOfferRequest("https://example.com/ebsi-offer");
        when(ebsiCredentialIssuanceServiceFacade.identifyAuthMethod(anyString(), eq("test-token"), eq("https://example.com/ebsi-offer")))
                .thenReturn(Mono.empty());

        // Act & Assert
        WebTestClient
                .bindToController(credentialIssuanceController)
                .build()
                .post()
                .uri("/api/v1/request-credential")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .bodyValue(credentialOfferRequest)
                .exchange()
                .expectStatus().isCreated();

        verify(ebsiCredentialIssuanceServiceFacade, times(1)).identifyAuthMethod(anyString(), eq("test-token"), eq("https://example.com/ebsi-offer"));
        verifyNoInteractions(commonCredentialIssuanceServiceFacade);
    }

    @Test
    void testRequestVerifiableCredentialWithCommonUri() {
        // Arrange
        String authorizationHeader = "Bearer test-token";
        CredentialOfferRequest credentialOfferRequest = new CredentialOfferRequest("https://example.com/common-offer");
        when(commonCredentialIssuanceServiceFacade.identifyAuthMethod(anyString(), eq("test-token"), eq("https://example.com/common-offer")))
                .thenReturn(Mono.empty());

        // Act & Assert
        WebTestClient
                .bindToController(credentialIssuanceController)
                .build()
                .post()
                .uri("/api/v1/request-credential")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .bodyValue(credentialOfferRequest)
                .exchange()
                .expectStatus().isCreated();

        verify(commonCredentialIssuanceServiceFacade, times(1)).identifyAuthMethod(anyString(), eq("test-token"), eq("https://example.com/common-offer"));
        verifyNoInteractions(ebsiCredentialIssuanceServiceFacade);
    }
}
