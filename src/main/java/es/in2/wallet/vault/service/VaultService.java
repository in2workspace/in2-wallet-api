package es.in2.wallet.vault.service;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface VaultService {
    Mono<Void> saveSecret(Map<String, String> secrets);

    Mono<String> getSecretByKey(String key, String type);

    Mono<Void> deleteSecretByKey(String key);
}
