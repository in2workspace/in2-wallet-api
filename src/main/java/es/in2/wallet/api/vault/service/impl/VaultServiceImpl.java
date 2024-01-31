package es.in2.wallet.api.vault.service.impl;

import es.in2.wallet.api.vault.service.GenericVaultService;
import es.in2.wallet.api.vault.service.VaultService;
import es.in2.wallet.api.vault.util.VaultFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class VaultServiceImpl implements VaultService {

    private final GenericVaultService vaultService;

    public VaultServiceImpl(VaultFactory vaultFactory){
        this.vaultService = vaultFactory.getVaultAdapter();
    }

    public Mono<Void> saveSecret(String key, String secret) {
        return vaultService.saveSecret(key,secret);
    }


    public Mono<String> getSecretByKey(String key) {
        return vaultService.getSecretByKey(key);
    }


    public Mono<Void> deleteSecretByKey(String key) {
        return vaultService.deleteSecretByKey(key);
    }
}
