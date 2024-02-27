package es.in2.wallet.api.service.impl;

import COSE.*;
import COSE.AlgorithmID;
import COSE.CoseException;
import COSE.OneKey;
import com.nimbusds.jose.JOSEObject;
import com.upokecenter.cbor.CBORObject;
import es.in2.wallet.api.service.CborGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.minvws.encoding.Base45;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class CborGenerationServiceImpl implements CborGenerationService {
    @Override
    public Mono<String> generateCbor(String processId, String authorizationToken, String content) throws ParseException {
        return generateCborFromJson(content)
                .doOnSuccess(cbor -> log.info("ProcessID: {} - Cbor generated correctly: {}", processId, cbor))
                .flatMap(cbor -> {
                    try {
                        return generateCOSEBytesFromCBOR(cbor);
                    } catch (CoseException e) {
                        return Mono.error(new RuntimeException());
                    }
                })
                .flatMap(this::compressAndConvertToBase45FromCOSE);
    }

    private Mono<byte[]> generateCborFromJson(String content) throws ParseException {
        return Mono.just((CBORObject.FromJSONString(JOSEObject.parse(content).getPayload().toString())).EncodeToBytes());
    }

    private Mono<byte[]> generateCOSEBytesFromCBOR(byte[] cbor) throws CoseException {
        OneKey oneKey = OneKey.generateKey(AlgorithmID.ECDSA_256);
        OneKey publicKey = oneKey.PublicKey();

        Sign1Message msg = new Sign1Message();
        msg.addAttribute(HeaderKeys.Algorithm, oneKey.get(KeyKeys.Algorithm), Attribute.PROTECTED);
        msg.addAttribute(HeaderKeys.KID, publicKey.AsCBOR(), Attribute.UNPROTECTED);
        msg.SetContent(cbor);
        msg.sign(oneKey);

        return Mono.just(msg.EncodeToBytes());
    }

    private Mono<String> compressAndConvertToBase45FromCOSE(byte[] cose) {
        ByteArrayInputStream bis = new ByteArrayInputStream(cose);
        DeflaterInputStream compressedInput = new DeflaterInputStream(bis, new Deflater(Deflater.BEST_COMPRESSION));

        byte[] coseCompressed;
        try {
            coseCompressed = compressedInput.readAllBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        return Mono.just(Base45.getEncoder().encodeToString(coseCompressed));
    }
}
