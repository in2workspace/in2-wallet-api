package es.in2.wallet.api.crypto.service;

import reactor.core.publisher.Mono;

import java.security.KeyPair;
import java.util.Map;

public interface DidKeyGeneratorService {
    Mono<Map<String, String>> generateDidKeyJwkJcsPubWithFromKeyPair(KeyPair keyPair);
    Mono<Map<String, String>> generateDidKeyFromKeyPair(KeyPair keyPair);
}
