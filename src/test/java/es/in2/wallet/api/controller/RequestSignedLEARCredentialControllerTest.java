package es.in2.wallet.api.controller;

import es.in2.wallet.application.workflow.issuance.DeferredCredentialDomeProfileWorkflow;
import es.in2.wallet.infrastructure.core.controller.RequestSignedLEARCredentialController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestSignedLEARCredentialControllerTest {

    @Mock
    private DeferredCredentialDomeProfileWorkflow deferredCredentialDomeProfileWorkflow;

    @InjectMocks
    private RequestSignedLEARCredentialController requestSignedLEARCredentialController;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(requestSignedLEARCredentialController).build();
    }

    @Test
    void getSignedCredential_Success() {
        String credentialId = "cred123";
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        when(deferredCredentialDomeProfileWorkflow.requestDeferredCredential(anyString(), eq("1234567890"), eq(credentialId)))
                .thenReturn(Mono.empty());

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/request-signed-credential")
                        .queryParam("credentialId", credentialId)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " +  token)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getSignedCredential_ErrorHandling() {
        String credentialId = "cred123";
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        when(deferredCredentialDomeProfileWorkflow.requestDeferredCredential(anyString(), eq("1234567890"), eq(credentialId)))
                .thenReturn(Mono.error(new RuntimeException("Internal Server Error")));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/request-signed-credential")
                        .queryParam("credentialId", credentialId)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().is5xxServerError();
    }
}

