package es.in2.wallet.api.vault.service;

import reactor.core.publisher.Mono;

public interface VaultService {
    Mono<Void> saveSecret(String key, String secret);

    Mono<String> getSecretByKey(String key);

    Mono<Void> deleteSecretByKey(String key);
}
