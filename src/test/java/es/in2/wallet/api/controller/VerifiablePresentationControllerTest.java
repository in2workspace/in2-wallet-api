package es.in2.wallet.api.controller;

import es.in2.wallet.application.service.CommonAttestationExchangeWorkflow;
import es.in2.wallet.application.service.DomeAttestationExchangeWorkflow;
import es.in2.wallet.application.service.TurnstileAttestationExchangeWorkflow;
import es.in2.wallet.domain.model.CredentialsBasicInfo;
import es.in2.wallet.domain.model.VcSelectorResponse;
import es.in2.wallet.infrastructure.core.controller.VerifiablePresentationController;
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
    private TurnstileAttestationExchangeWorkflow turnstileAttestationExchangeWorkflow;
    @Mock
    private CommonAttestationExchangeWorkflow commonAttestationExchangeWorkflow;
    @Mock
    private DomeAttestationExchangeWorkflow domeAttestationExchangeWorkflow;
    @InjectMocks
    private VerifiablePresentationController verifiablePresentationController;

    @Test
    void testCreateVerifiablePresentation() {
        // Arrange
        String authorizationToken = "authToken";
        VcSelectorResponse vcSelectorResponse = VcSelectorResponse.builder().redirectUri("https://redirect.uri.com").build();

        when(commonAttestationExchangeWorkflow.buildVerifiablePresentationWithSelectedVCs(anyString(), eq(authorizationToken), eq(vcSelectorResponse)))
                .thenReturn(Mono.empty());

        WebTestClient
                .bindToController(verifiablePresentationController)
                .build()
                .post()
                .uri("/api/v1/vp")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorizationToken)
                .bodyValue(vcSelectorResponse)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void testCreateVerifiablePresentationDomeCase() {
        // Arrange
        String authorizationToken = "authToken";
        VcSelectorResponse vcSelectorResponse = VcSelectorResponse.builder().redirectUri("https://dome-marketplace.org").build();

        when(domeAttestationExchangeWorkflow.buildAndSendVerifiablePresentationWithSelectedVCsForDome(anyString(), eq(authorizationToken), eq(vcSelectorResponse)))
                .thenReturn(Mono.empty());

        WebTestClient
                .bindToController(verifiablePresentationController)
                .build()
                .post()
                .uri("/api/v1/vp")
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

        when(turnstileAttestationExchangeWorkflow.createVerifiablePresentationForTurnstile(anyString(), eq(authorizationToken), any()))
                .thenReturn(Mono.just("cbor"));

        WebTestClient
                .bindToController(verifiablePresentationController)
                .build()
                .post()
                .uri("/api/v1/vp/cbor")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorizationToken)
                .bodyValue(credentialsBasicInfo)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .isEqualTo(expectedResponse);
    }

}
