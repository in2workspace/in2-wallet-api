package es.in2.wallet.domain.service;

import reactor.core.publisher.Mono;

public interface QrCodeProcessorService {
    Mono<Object> processQrContent(String processId, String authorizationToken, String qrContent);
}

