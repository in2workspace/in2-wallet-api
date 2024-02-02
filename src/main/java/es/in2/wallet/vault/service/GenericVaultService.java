package es.in2.wallet.vault.service;

import reactor.core.publisher.Mono;

public interface GenericVaultService {
    Mono<Void> saveSecret(String key, String secret);

    Mono<String> getSecretByKey(String key);

    Mono<Void> deleteSecretByKey(String key);
}
