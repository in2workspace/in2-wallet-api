package es.in2.wallet.api.crypto.service;

import reactor.core.publisher.Mono;

import java.io.IOException;
import java.security.KeyPair;

public interface DidKeyGeneratorService {
    Mono<String> generateDidFromKeyPair(KeyPair keyPair);
}
