package es.in2.wallet.infrastructure.vault.service;

import es.in2.wallet.infrastructure.vault.model.KeyVaultSecret;
import reactor.core.publisher.Mono;


public interface GenericVaultService {
    Mono<Void> saveSecret(String key, KeyVaultSecret secret);
    Mono<KeyVaultSecret> getSecret(String key);
    Mono<Void> deleteSecret(String key);

}
