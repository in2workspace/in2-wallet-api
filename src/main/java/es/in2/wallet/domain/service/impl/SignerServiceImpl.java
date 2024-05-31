package es.in2.wallet.domain.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import es.in2.wallet.application.port.VaultService;
import es.in2.wallet.domain.exception.ParseErrorException;
import es.in2.wallet.domain.model.JWTSType;
import es.in2.wallet.domain.service.SignerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static es.in2.wallet.domain.model.JWTSType.*;
import static es.in2.wallet.domain.util.ApplicationConstants.JWT_PROOF_CLAIM;
import static es.in2.wallet.domain.util.ApplicationConstants.PROCESS_ID;
import static es.in2.wallet.domain.util.ApplicationRegexPattern.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignerServiceImpl implements SignerService {

    private final ObjectMapper objectMapper;
    private final VaultService vaultService;

    @Override
    public Mono<String> buildJWTSFromJsonNode(JsonNode document, String did, String documentType) {
        String processId = MDC.get(PROCESS_ID);

        return  vaultService.getSecretByKey(did)
                .flatMap(privateKey -> identifyDocumentType(documentType)
                    .flatMap(docType -> Mono.fromCallable(() -> {
                        try {
                            ECKey ecJWK = JWK.parse(privateKey.value().toString()).toECKey();
                            log.debug("ECKey: {}", ecJWK);

                            JWSAlgorithm jwsAlgorithm = mapToJWSAlgorithm(ecJWK.getAlgorithm());
                            JWSSigner signer = new ECDSASigner(ecJWK);

                            JOSEObjectType joseObjectType = docType.equals(PROOF_JWT) ?
                                    new JOSEObjectType(JWT_PROOF_CLAIM) : JOSEObjectType.JWT;

                            String didWithKey = extractAfterPattern(did);
                            JWSHeader header = new JWSHeader.Builder(jwsAlgorithm)
                                    .type(joseObjectType)
                                    .keyID(didWithKey)
                                    .build();

                            JWTClaimsSet payload = convertJsonNodeToJWTClaimsSet(document);
                            SignedJWT signedJWT = new SignedJWT(header, payload);
                            signedJWT.sign(signer);
                            log.debug("JWT signed successfully: ");
                            return signedJWT.serialize();
                        } catch (Exception e) {
                            log.error("Error while creating the Signed JWT", e);
                            throw new ParseErrorException("Error while encoding the JWT: " + e.getMessage());
                        }
                    }))
                )
                .doOnSuccess(jwt -> log.debug("ProcessID: {} - Created JWT: {}", processId, jwt))
                .doOnError(throwable -> log.error("ProcessID: {} - Error creating the jwt: {}", processId, throwable.getMessage()));
    }

    private JWSAlgorithm mapToJWSAlgorithm(Algorithm algorithm) {
        return JWSAlgorithm.parse(algorithm.getName());
    }

    private JWTClaimsSet convertJsonNodeToJWTClaimsSet(JsonNode jsonNode){
        Map<String, Object> claimsMap = objectMapper.convertValue(jsonNode, new TypeReference<>() {
        });
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
        for (Map.Entry<String, Object> entry : claimsMap.entrySet()) {
            builder.claim(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    private Mono<JWTSType> identifyDocumentType(String documentType) {
        return Mono.fromSupplier(() -> {
            if (PROOF_DOCUMENT_PATTERN.matcher(documentType).matches()) {
                return PROOF_JWT;
            } else if (JWT_TYPE.matcher(documentType).matches()) {
                return JWT;
            } else if (VP_DOCUMENT_PATTERN.matcher(documentType).matches()) {
                return VP_JWT;
            } else {
                log.warn("Unknown document type: {}", documentType);
                return UNKNOWN;
            }
        });
    }

    private String extractAfterPattern(String str) {
        Pattern pattern = Pattern.compile("did:.*:(.*)");
        Matcher matcher = pattern.matcher(str);

        if (matcher.find()) {
            return str+"#"+matcher.group(1);
        } else {
            return str;
        }
    }

}
