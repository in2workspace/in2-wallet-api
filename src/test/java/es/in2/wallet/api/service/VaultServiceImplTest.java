package es.in2.wallet.api.service;

import es.in2.wallet.infrastructure.vault.model.KeyVaultSecret;
import es.in2.wallet.infrastructure.vault.service.impl.VaultServiceImpl;
import es.in2.wallet.infrastructure.vault.service.GenericVaultService;
import es.in2.wallet.infrastructure.vault.util.VaultFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VaultServiceImplTest {
    @Mock
    private GenericVaultService genericVaultService;
    @Mock
    private VaultFactory vaultFactory;
    @InjectMocks
    private VaultServiceImpl vaultService;

    @BeforeEach
    public void setup() {
        when(vaultFactory.getVaultService()).thenReturn(genericVaultService);
        vaultService = new VaultServiceImpl(vaultFactory);
    }

    @Test
    void testSaveSecret() {
        String key = "key";
        KeyVaultSecret secret = KeyVaultSecret.builder().build();

        when(genericVaultService.saveSecret(key, secret))
                .thenReturn(Mono.empty());

        StepVerifier.create(vaultService.saveSecret(key, secret))
                .expectComplete()
                .verify();
    }

    @Test
    void testGetSecretByKey() {
        String key = "key";
        KeyVaultSecret secret = KeyVaultSecret.builder().build();

        when(genericVaultService.getSecret(key))
                .thenReturn(Mono.just(secret));

        StepVerifier.create(vaultService.getSecretByKey(key))
                .expectNext(secret)
                .expectComplete()
                .verify();
    }

    @Test
    void testDeleteSecretByKey() {
        String key = "key";

        when(genericVaultService.deleteSecret(key))
                .thenReturn(Mono.empty());

        StepVerifier.create(vaultService.deleteSecretByKey(key))
                .expectComplete()
                .verify();
    }
}
