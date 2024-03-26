package es.in2.wallet.domain.service.impl;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import es.in2.wallet.domain.exception.KeyPairGenerationError;
import es.in2.wallet.domain.exception.ParseErrorException;
import es.in2.wallet.domain.model.UVarInt;
import es.in2.wallet.domain.service.DidKeyGeneratorService;
import es.in2.wallet.application.port.VaultService;
import es.in2.wallet.infrastructure.vault.model.KeyVaultSecret;
import io.ipfs.multibase.Base58;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.erdtman.jcs.JsonCanonicalizer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;


@Service
@RequiredArgsConstructor
public class DidKeyGeneratorServiceImpl implements DidKeyGeneratorService {

    private final VaultService vaultService;
    @Override
    public Mono<String> generateDidKeyJwkJcsPub() {
        return generateES256r1ECKeyPair().flatMap(keyPair -> {
            String did = generateDidKeyJwkJcsPub(keyPair);
            String privateKey = getPrivateKeyJwkString(keyPair);
            KeyVaultSecret secret = KeyVaultSecret.builder()
                    .value(privateKey)
                    .build();

            return vaultService.saveSecret(did, secret)
                    .thenReturn(did);
        });
    }

    @Override
    public Mono<String> generateDidKey() {
        return generateES256r1ECKeyPair().flatMap(keyPair -> {
            String did = generateDidKey(keyPair);
            String privateKey = getPrivateKeyJwkString(keyPair);
            KeyVaultSecret secret = KeyVaultSecret.builder()
                    .value(privateKey)
                    .build();

            return vaultService.saveSecret(did, secret)
                    .thenReturn(did);
        });
    }

    // Generates a DID Key using JWK with JCS Public format
    private String generateDidKeyJwkJcsPub(KeyPair keyPair){

        byte[] jwkPubKeyBytes = getJwkPubKeyRequiredMembersBytes(getPublicKeyJwkString((ECPublicKey) keyPair.getPublic()));
        int jwkJcsPubMultiCodecKeyCode = 0xeb51;
        String multiBase58Btc = convertRawKeyToMultiBase58Btc(jwkPubKeyBytes,jwkJcsPubMultiCodecKeyCode);
        return "did:key:z" + multiBase58Btc;
    }

    // Generates a standard DID Key
    private String generateDidKey(KeyPair keyPair){
        byte[] pubKeyBytes = getPublicKeyBytesForDidKey(keyPair);
        int multiCodecKeyCodeForSecp256r1 = 0x1200;
        String multiBase58Btc = convertRawKeyToMultiBase58Btc(pubKeyBytes,multiCodecKeyCodeForSecp256r1);
        return "did:key:z" + multiBase58Btc;
    }

    private String getPublicKeyJwkString(ECPublicKey publicKey) {
        ECKey jwk = new ECKey.Builder(Curve.P_256, publicKey).build();
        return jwk.toJSONString();
    }

    private String getPrivateKeyJwkString(KeyPair keyPair) {
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();

        ECKey jwk = new ECKey.Builder(Curve.P_256, publicKey)
                .privateKey(privateKey)
                .algorithm(Algorithm.parse("ES256"))
                .build();

        return jwk.toJSONString();
    }



    // Obtains required bytes of the public key for JWK
    private byte[] getJwkPubKeyRequiredMembersBytes(String jwkJsonString){
        try {
            JsonCanonicalizer jsonCanonicalizer = new JsonCanonicalizer(jwkJsonString);
            return jsonCanonicalizer.getEncodedUTF8();
        } catch (IOException e) {
            throw new ParseErrorException("Error while getting jwkPubKeyRequiredMembers " + e);
        }
    }

    // Obtains the bytes of the public key
    private byte[] getPublicKeyBytesForDidKey(KeyPair keyPair) {
        BCECPublicKey ecPublicKey = (BCECPublicKey) keyPair.getPublic();
        return ecPublicKey.getQ().getEncoded(true);
    }

    // Converts raw public key bytes into a multibase58 string
   private String convertRawKeyToMultiBase58Btc(byte[] publicKey, int code) {
        UVarInt codeVarInt = new UVarInt(code);

       // Calculate the total length of the resulting byte array
       int totalLength = publicKey.length + codeVarInt.getLength();

       // Create a byte array to hold the multicodec and raw key
       byte[] multicodecAndRawKey = new byte[totalLength];

       // Copy the UVarInt bytes to the beginning of the byte array
       System.arraycopy(codeVarInt.getBytes(), 0, multicodecAndRawKey, 0, codeVarInt.getLength());

       // Copy the raw public key bytes after the UVarInt bytes
       System.arraycopy(publicKey, 0, multicodecAndRawKey, codeVarInt.getLength(), publicKey.length);

       // Encode the combined byte array to Base58
       return Base58.encode(multicodecAndRawKey);
    }

    private Mono<KeyPair> generateES256r1ECKeyPair() {
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
