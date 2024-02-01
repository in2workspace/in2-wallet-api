package es.in2.wallet.api.crypto.service;

import reactor.core.publisher.Mono;

import java.security.KeyPair;

public interface KeyGenerationService {
    Mono<KeyPair> generateECKeyPair();
}
