package es.in2.wallet.api.service;

import reactor.core.publisher.Mono;

import java.security.KeyPair;

public interface KeyGenerationService {
    Mono<KeyPair> generateES256r1ECKeyPair();
}
