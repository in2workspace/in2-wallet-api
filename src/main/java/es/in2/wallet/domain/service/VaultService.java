package es.in2.wallet.domain.service;

import es.in2.wallet.infrastructure.vault.model.KeyVaultSecret;
import reactor.core.publisher.Mono;

public interface VaultService {

    Mono<Void> saveSecret(String key, KeyVaultSecret secret);
    Mono<KeyVaultSecret> getSecretByKey(String key);
    Mono<Void> deleteSecretByKey(String key);
}
