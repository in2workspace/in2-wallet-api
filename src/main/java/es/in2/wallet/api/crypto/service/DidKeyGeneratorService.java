package es.in2.wallet.api.crypto.service;

import reactor.core.publisher.Mono;

import java.security.KeyPair;

public interface DidKeyGeneratorService {
    Mono<String> generateDidKeyJwkJcsPubWithFromKeyPair(KeyPair keyPair);
    Mono<String> generateDidKeyFromKeyPair(KeyPair keyPair);
}
