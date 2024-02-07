package es.in2.wallet.api.crypto.service;

import es.in2.wallet.api.crypto.service.impl.DidKeyGeneratorServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;


import java.security.*;
import java.security.spec.ECGenParameterSpec;

import static es.in2.wallet.api.util.MessageUtils.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class DidKeyGeneratorServiceImplTest {
    @InjectMocks
    private DidKeyGeneratorServiceImpl didKeyGeneratorService;

    @Test
    void generateDidKeyJwkJcsPubWithFromKeyPairTest() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
        keyPairGenerator.initialize(ecSpec, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        StepVerifier.create(didKeyGeneratorService.generateDidKeyJwkJcsPubWithFromKeyPair(keyPair))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertTrue(result.containsKey(DID));
                    assertTrue(result.containsKey(PUBLIC_KEY_TYPE));
                    assertTrue(result.containsKey(PRIVATE_KEY_TYPE));

                    String did = result.get(DID);
                    assertNotNull(did);
                    assertTrue(did.startsWith("did:key:z"));

                })
                .verifyComplete();
    }

    @Test
    void generateDidKeyFromKeyPairTest() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException{

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
        keyPairGenerator.initialize(ecSpec, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        StepVerifier.create(didKeyGeneratorService.generateDidKeyFromKeyPair(keyPair))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertTrue(result.containsKey(DID));
                    assertTrue(result.containsKey(PUBLIC_KEY_TYPE));
                    assertTrue(result.containsKey(PRIVATE_KEY_TYPE));

                    String did = result.get(DID);
                    assertNotNull(did);
                    assertTrue(did.startsWith("did:key:z"));

                })
                .verifyComplete();
    }
}
