package es.in2.wallet.domain.service;

import reactor.core.publisher.Mono;

public interface CborGenerationService {
    Mono<String> generateCbor(String processId, String content);
}
