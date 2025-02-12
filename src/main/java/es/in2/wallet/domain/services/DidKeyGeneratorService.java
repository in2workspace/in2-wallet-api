package es.in2.wallet.domain.services;

import reactor.core.publisher.Mono;

public interface DidKeyGeneratorService {
    Mono<String> generateDidKeyJwkJcsPub();
    Mono<String> generateDidKey();
}
