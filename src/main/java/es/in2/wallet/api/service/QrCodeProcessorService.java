package es.in2.wallet.api.service;

import reactor.core.publisher.Mono;

public interface QrCodeProcessorService {
    Mono<Object> processQrContent(String processId, String authorizationToken, String qrContent);
}

