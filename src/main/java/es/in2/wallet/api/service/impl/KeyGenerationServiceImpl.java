package es.in2.wallet.api.service.impl;

import es.in2.wallet.api.service.KeyGenerationService;
import es.in2.wallet.api.exception.KeyPairGenerationError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;

@Service
@Slf4j
public class KeyGenerationServiceImpl implements KeyGenerationService {
    @Override
    public Mono<KeyPair> generateES256r1ECKeyPair() {
        return Mono.fromCallable(() -> {
            try {
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
                ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
                keyPairGenerator.initialize(ecSpec, new SecureRandom());
                return keyPairGenerator.generateKeyPair();
            } catch (Exception e) {
                throw new KeyPairGenerationError("Error generating EC key pair: " + e);
            }
        });
    }
}
