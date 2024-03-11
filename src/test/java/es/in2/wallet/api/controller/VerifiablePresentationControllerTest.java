package es.in2.wallet.api.controller;

import es.in2.wallet.api.facade.AttestationExchangeServiceFacade;
import es.in2.wallet.api.facade.CredentialPresentationForTurnstileServiceFacade;
import es.in2.wallet.api.model.CredentialsBasicInfo;
import es.in2.wallet.api.model.VcSelectorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.shaded.com.google.common.net.HttpHeaders;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerifiablePresentationControllerTest {
    @Mock
    private CredentialPresentationForTurnstileServiceFacade credentialPresentationForTurnstileServiceFacade;
    @Mock
    private AttestationExchangeServiceFacade attestationExchangeServiceFacade;
    @InjectMocks
    private VerifiablePresentationController verifiablePresentationController;
    @Test
    void testCreateVerifiablePresentation() {
        // Arrange
        String authorizationToken = "authToken";
        VcSelectorResponse vcSelectorResponse = VcSelectorResponse.builder().build();

        when(attestationExchangeServiceFacade.buildVerifiablePresentationWithSelectedVCs(anyString(), eq(authorizationToken), eq(vcSelectorResponse)))
                .thenReturn(Mono.empty());

        WebTestClient
                .bindToController(verifiablePresentationController)
                .build()
                .post()
                .uri("/api/vp")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorizationToken)
                .bodyValue(vcSelectorResponse)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void testCreateVerifiablePresentationInCborFormat() {
        String authorizationToken = "authToken";
        CredentialsBasicInfo credentialsBasicInfo = CredentialsBasicInfo.builder().build();
        String expectedResponse = "cbor";

        when(credentialPresentationForTurnstileServiceFacade.createVerifiablePresentationForTurnstile(anyString(), eq(authorizationToken), any()))
                .thenReturn(Mono.just("cbor"));

        WebTestClient
                .bindToController(verifiablePresentationController)
                .build()
                .post()
                .uri("/api/vp/cbor")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorizationToken)
                .bodyValue(credentialsBasicInfo)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo(expectedResponse);
    }

}
