package es.in2.wallet.api.ebsi.comformance.controller;

import es.in2.wallet.application.workflows.issuance.Oid4vciWorkflow;
import es.in2.wallet.application.workflows.issuance.CredentialIssuanceEbsiWorkflow;
import es.in2.wallet.infrastructure.ebsi.controller.OpenidCredentialOfferController;
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
class OpenidCredentialOfferControllerTest {

    @Mock
    private CredentialIssuanceEbsiWorkflow ebsiCredentialIssuanceServiceFacade;

    @Mock
    private Oid4vciWorkflow commonCredentialIssuanceServiceFacade;

    @InjectMocks
    private OpenidCredentialOfferController openidCredentialOfferController;

    @Test
    void testRequestOpenidCredentialOfferWithEbsiUri() {
        // Arrange
        String authorizationHeader = "Bearer test-token";
        String credentialOfferUri = "https://example.com/ebsi-offer";

        when(ebsiCredentialIssuanceServiceFacade.execute(anyString(), eq("test-token"), eq("https://example.com/ebsi-offer")))
                .thenReturn(Mono.empty());

        // Act & Assert
        WebTestClient
                .bindToController(openidCredentialOfferController)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/openid-credential-offer")
                        .queryParam("credentialOfferUri", credentialOfferUri)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .exchange()
                .expectStatus().isOk();

        verify(ebsiCredentialIssuanceServiceFacade, times(1)).execute(anyString(), eq("test-token"), eq("https://example.com/ebsi-offer"));
        verifyNoInteractions(commonCredentialIssuanceServiceFacade);
    }

    @Test
    void testRequestOpenidCredentialOfferWithCommonUri() {
        // Arrange
        String authorizationHeader = "Bearer test-token";
        String credentialOfferUri = "https://example.com/common-offer";
        when(commonCredentialIssuanceServiceFacade.execute(anyString(), eq("test-token"), eq("https://example.com/common-offer")))
                .thenReturn(Mono.empty());

        // Act & Assert
        WebTestClient
                .bindToController(openidCredentialOfferController)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/openid-credential-offer")
                        .queryParam("credentialOfferUri", credentialOfferUri)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .exchange()
                .expectStatus().isOk();

        verify(commonCredentialIssuanceServiceFacade, times(1)).execute(anyString(), eq("test-token"), eq("https://example.com/common-offer"));
        verifyNoInteractions(ebsiCredentialIssuanceServiceFacade);
    }
}
