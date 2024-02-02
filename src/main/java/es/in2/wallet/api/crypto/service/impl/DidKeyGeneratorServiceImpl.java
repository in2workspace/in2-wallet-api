package es.in2.wallet.api.crypto.service.impl;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import es.in2.wallet.api.crypto.service.DidKeyGeneratorService;
import es.in2.wallet.api.exception.ParseErrorException;
import es.in2.wallet.api.util.UVarInt;
import io.ipfs.multibase.Base58;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.erdtman.jcs.JsonCanonicalizer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;


@Service
@RequiredArgsConstructor
public class DidKeyGeneratorServiceImpl implements DidKeyGeneratorService {

    @Override
    public Mono<String> generateDidKeyJwkJcsPubWithFromKeyPair(KeyPair keyPair) {
        return Mono.just(generateDidKeyJwkJcsPub(keyPair));
    }

    @Override
    public Mono<String> generateDidKeyFromKeyPair(KeyPair keyPair) {
        return Mono.just(generateDidKey(keyPair));
    }

    // Generates a DID Key using JWK with JCS Public format
    private String generateDidKeyJwkJcsPub(KeyPair keyPair){

        byte[] jwkPubKeyBytes = getJwkPubKeyRequiredMembersBytes((ECPublicKey) keyPair.getPublic());
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


    // Obtains required bytes of the public key for JWK
    private byte[] getJwkPubKeyRequiredMembersBytes(ECPublicKey publicKey){
        ECKey jwk = new ECKey.Builder(Curve.P_256, publicKey).build();

        String jwkJsonString = jwk.toJSONString();
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
}
