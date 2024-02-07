package es.in2.wallet.api.service;

import es.in2.wallet.api.service.impl.KeyGenerationServiceImpl;
import es.in2.wallet.api.exception.KeyPairGenerationError;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class KeyGenerationServiceImplTest {
    @InjectMocks
    private KeyGenerationServiceImpl keyGenerationService;

    @Test
    void generateES256r1ECKeyPairTest() {
        Mono<KeyPair> keyPairMono = keyGenerationService.generateES256r1ECKeyPair();

        StepVerifier.create(keyPairMono)
                .assertNext(keyPair -> {
                    assertNotNull(keyPair);
                    assertNotNull(keyPair.getPublic());
                    assertInstanceOf(ECPublicKey.class, keyPair.getPublic());
                    assertNotNull(keyPair.getPrivate());
                    assertInstanceOf(ECPrivateKey.class, keyPair.getPrivate());

                })
                .verifyComplete();
    }

    @Test
    void generateES256r1ECKeyPairThrowsExceptionTest() {
        KeyGenerationServiceImpl keyGenerationService = new KeyGenerationServiceImpl() {
            @Override
            public Mono<KeyPair> generateES256r1ECKeyPair() {
                // Override the method to simulate an error during the generation of the key pair
                return Mono.fromCallable(() -> {
                    throw new KeyPairGenerationError("Simulated error generating EC key pair");
                });
            }
        };
        Mono<KeyPair> keyPairMono = keyGenerationService.generateES256r1ECKeyPair();

        StepVerifier.create(keyPairMono)
                .expectErrorMatches(throwable -> throwable instanceof KeyPairGenerationError
                        && throwable.getMessage().contains("Simulated error generating EC key pair"))
                .verify();
    }
}
