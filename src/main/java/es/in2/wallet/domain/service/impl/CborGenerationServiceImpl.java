package es.in2.wallet.domain.service.impl;

import COSE.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObject;
import com.nimbusds.jose.shaded.gson.JsonArray;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import com.upokecenter.cbor.CBORObject;
import es.in2.wallet.domain.service.CborGenerationService;
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
    private final ObjectMapper objectMapper;

    @Override
    public Mono<String> generateCbor(String processId, String content) throws ParseException {
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
        return modifyPayload(content)
                .flatMap(modifiedPayload -> Mono.just((CBORObject.FromJSONString(modifiedPayload)).EncodeToBytes()));
    }

    private Mono<String> modifyPayload(String token) throws ParseException {
        String vcPayload = JOSEObject.parse(token).getPayload().toString();

        // Parse the original VP JSON
        JsonObject vpJsonObject = JsonParser.parseString(vcPayload).getAsJsonObject();

        // Select the VC from the VP
        JsonObject vpContent = vpJsonObject.getAsJsonObject("vp");

        JsonArray verifiableCredentialArray = vpContent.getAsJsonArray("verifiableCredential");

        if (!verifiableCredentialArray.isEmpty()) {
            // Get the first element as a string
            String firstCredential = verifiableCredentialArray.get(0).getAsString();

            // Replace "verifiableCredential" in vpContent with the first credential
            vpContent.addProperty("verifiableCredential", firstCredential);
        }

        return Mono.just(vpJsonObject.toString());
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
