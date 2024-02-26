package es.in2.wallet.api.service;

import reactor.core.publisher.Mono;

public interface CborGenerationService {
    Mono<String> generateCbor(String processId, String authorizationToken, String content);
}
