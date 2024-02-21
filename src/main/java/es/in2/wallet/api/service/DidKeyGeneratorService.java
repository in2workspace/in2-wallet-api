package es.in2.wallet.api.service;

import reactor.core.publisher.Mono;

public interface DidKeyGeneratorService {
    Mono<String> generateDidKeyJwkJcsPub();
    Mono<String> generateDidKey();
}
