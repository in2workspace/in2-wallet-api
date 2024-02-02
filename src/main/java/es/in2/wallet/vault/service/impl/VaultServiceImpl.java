package es.in2.wallet.vault.service.impl;

import es.in2.wallet.vault.service.GenericVaultService;
import es.in2.wallet.vault.service.VaultService;
import es.in2.wallet.vault.util.VaultFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class VaultServiceImpl implements VaultService {

    private final GenericVaultService vaultService;

    public VaultServiceImpl(VaultFactory vaultFactory){
        this.vaultService = vaultFactory.getVaultAdapter();
    }

    public Mono<Void> saveSecret(Map<String, String> secrets) {
        return vaultService.saveSecret(secrets);
    }


    public Mono<String> getSecretByKey(String key, String type) {
        return vaultService.getSecretByKey(key,type);
    }


    public Mono<Void> deleteSecretByKey(String key) {
        return vaultService.deleteSecretByKey(key);
    }
}
