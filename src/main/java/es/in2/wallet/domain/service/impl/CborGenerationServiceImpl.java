package es.in2.wallet.domain.service.impl;

import COSE.*;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObject;
import com.nimbusds.jose.shaded.gson.JsonArray;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import com.upokecenter.cbor.CBORObject;
import es.in2.wallet.domain.exception.ParseErrorException;
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

    @Override
    public Mono<String> generateCbor(String processId, String content) {
        return generateCborFromJson(content)
                .doOnSuccess(cbor -> log.info("ProcessID: {} - Cbor generated correctly: {}", processId, cbor))
                .flatMap(this::generateCOSEBytesFromCBOR)
                .flatMap(this::compressAndConvertToBase45FromCOSE);
    }

    private Mono<byte[]> generateCborFromJson(String content) {
        return modifyPayload(content)
                .flatMap(modifiedPayload -> Mono.just((CBORObject.FromJSONString(modifiedPayload)).EncodeToBytes()));
    }

    private Mono<String> modifyPayload(String token) {
        String vcPayload;
        try {
            vcPayload = JOSEObject.parse(token).getPayload().toString();
        } catch (ParseException e) {
            log.warn("ParseException -- modifyPayload -- Failed to parse token payload. Error: {}", e.getMessage());
            return Mono.error(new ParseErrorException("Failed to parse token payload"));
        }

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

    private Mono<byte[]> generateCOSEBytesFromCBOR(byte[] cbor) {
        try {
            OneKey oneKey = OneKey.generateKey(AlgorithmID.ECDSA_256);
            OneKey publicKey = oneKey.PublicKey();

            Sign1Message msg = new Sign1Message();
            msg.addAttribute(HeaderKeys.Algorithm, oneKey.get(KeyKeys.Algorithm), Attribute.PROTECTED);
            msg.addAttribute(HeaderKeys.KID, publicKey.AsCBOR(), Attribute.UNPROTECTED);
            msg.SetContent(cbor);
            msg.sign(oneKey);

            return Mono.just(msg.EncodeToBytes());
        } catch (CoseException e) {
            log.warn("CoseException -- generateCOSEBytesFromCBOR -- Error generating COSE bytes from CBOR: {}", e.getMessage());
            return Mono.error(new CoseException("Error generating COSE bytes from CBOR"));
        }

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
