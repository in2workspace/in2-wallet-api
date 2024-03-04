package es.in2.wallet.api.service;

import es.in2.wallet.vault.model.secret.KeyVaultSecret;
import reactor.core.publisher.Mono;

public interface VaultService {

    Mono<Void> saveSecret(String key, KeyVaultSecret secret);
    Mono<KeyVaultSecret> getSecretByKey(String key);
    Mono<Void> deleteSecretByKey(String key);
}
