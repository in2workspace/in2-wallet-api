package es.in2.wallet.api.crypto.service.impl;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import es.in2.wallet.api.crypto.service.DidKeyGeneratorService;
import io.ipfs.multibase.Base58;
import org.erdtman.jcs.JsonCanonicalizer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;


@Service
public class DidKeyGeneratorServiceImpl implements DidKeyGeneratorService {

    @Override
    public Mono<String> generateDidFromKeyPair(KeyPair keyPair) {
        return Mono.just(generateDidFromKeyPairSync(keyPair));
    }

    private String generateDidFromKeyPairSync(KeyPair keyPair){

        byte[] jwkPubKeyBytes = getJwkPubKeyRequiredMembersBytes((ECPublicKey) keyPair.getPublic());
        int jwkJcsPubMultiCodecKeyCode = 0xeb51;
        String multiBase58Btc = convertRawKeyToMultiBase58Btc(jwkPubKeyBytes,jwkJcsPubMultiCodecKeyCode);
        return "did:key:z" + multiBase58Btc;
    }

    private byte[] getJwkPubKeyRequiredMembersBytes(ECPublicKey publicKey){
        ECKey jwk = new ECKey.Builder(Curve.P_256, publicKey).build();

        String jwkJsonString = jwk.toJSONString();

        JsonCanonicalizer jsonCanonicalizer = null;
        try {
            jsonCanonicalizer = new JsonCanonicalizer(jwkJsonString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            return jsonCanonicalizer.getEncodedUTF8();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public String convertRawKeyToMultiBase58Btc(byte[] publicKey, int code) {
        UVarInt codeVarInt = new UVarInt(code);
        byte[] multicodecAndRawKey = new byte[publicKey.length + codeVarInt.getLength()];
        System.arraycopy(codeVarInt.getBytes(), 0, multicodecAndRawKey, 0, codeVarInt.getLength());
        System.arraycopy(publicKey, 0, multicodecAndRawKey, codeVarInt.getLength(), publicKey.length);
        return Base58.encode(multicodecAndRawKey);
    }
}
