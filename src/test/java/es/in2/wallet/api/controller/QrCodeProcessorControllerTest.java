package es.in2.wallet.api.controller;

import es.in2.wallet.application.dto.QrContent;
import es.in2.wallet.application.workflows.processor.QrCodeProcessorWorkflow;
import es.in2.wallet.infrastructure.core.controller.QrCodeProcessorController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrCodeProcessorControllerTest {

    @Mock
    private QrCodeProcessorWorkflow qrCodeProcessorWorkflow;

    @InjectMocks
    private QrCodeProcessorController qrCodeProcessorController;

    @Test
    void testExecuteQrContent() {
        // Arrange
        String authorizationHeader = "Bearer authToken";
        QrContent qrContent = QrContent.builder().content("qrCodeContent").build();

        when(qrCodeProcessorWorkflow.processQrContent(anyString(), anyString(), anyString())).thenReturn(Mono.empty());

        WebTestClient
                .bindToController(qrCodeProcessorController)
                .build()
                .post()
                .uri("/api/v1/execute-content")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .bodyValue(qrContent)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void testExecuteQrContentWithError() {
        // Arrange
        String authorizationHeader = "Bearer authToken";
        QrContent qrContent = QrContent.builder().content("qrCodeContent").build();

        when(qrCodeProcessorWorkflow.processQrContent(anyString(), anyString(), anyString())).thenReturn(Mono.error(new RuntimeException("Error processing QR content")));

        WebTestClient
                .bindToController(qrCodeProcessorController)
                .build()
                .post()
                .uri("/api/v1/execute-content")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .bodyValue(qrContent)
                .exchange()
                .expectStatus().is5xxServerError();
    }
}

