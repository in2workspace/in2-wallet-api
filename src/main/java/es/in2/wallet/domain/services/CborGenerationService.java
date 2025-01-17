package es.in2.wallet.domain.services;

import reactor.core.publisher.Mono;

public interface CborGenerationService {
    Mono<String> generateCbor(String processId, String content);
}
