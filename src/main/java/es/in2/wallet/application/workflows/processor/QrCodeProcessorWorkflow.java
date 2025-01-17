package es.in2.wallet.application.workflows.processor;

import reactor.core.publisher.Mono;

public interface QrCodeProcessorWorkflow {
    Mono<Object> processQrContent(String processId, String authorizationToken, String qrContent);
}

