package es.in2.wallet.api.ebsi.comformance.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import es.in2.wallet.api.ebsi.comformance.model.CredentialRequestEbsi;
import es.in2.wallet.api.ebsi.comformance.service.CredentialEbsiService;
import es.in2.wallet.api.exception.FailedCommunicationException;
import es.in2.wallet.api.exception.FailedDeserializingException;
import es.in2.wallet.api.exception.FailedSerializingException;
import es.in2.wallet.api.exception.ParseErrorException;
import es.in2.wallet.api.model.CredentialIssuerMetadata;
import es.in2.wallet.api.model.CredentialResponse;
import es.in2.wallet.api.model.TokenResponse;
import es.in2.wallet.api.service.SignerService;
import es.in2.wallet.vault.service.VaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static es.in2.wallet.api.util.ApplicationUtils.postRequest;
import static es.in2.wallet.api.util.MessageUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialEbsiServiceImpl implements CredentialEbsiService {

    private final ObjectMapper objectMapper;
    private final VaultService vaultService;
    private final SignerService signerService;

    @Override
    public Mono<CredentialResponse> getCredential(String processId, String did, TokenResponse tokenResponse, CredentialIssuerMetadata credentialIssuerMetadata, String format, List<String> types) {
        // build CredentialRequest
        return buildCredentialRequest(tokenResponse.cNonce(), credentialIssuerMetadata.credentialIssuer(),did,format,types)
                .doOnSuccess(credentialRequest -> log.info("ProcessID: {} - CredentialRequest: {}", processId, credentialRequest))
                // post CredentialRequest
                .flatMap(credentialRequest -> postCredential(tokenResponse, credentialIssuerMetadata, credentialRequest))
                .doOnSuccess(response -> log.info("ProcessID: {} - Credential Post Response: {}", processId, response))
                // handle CredentialResponse or deferred response
                .flatMap(response -> handleCredentialResponse(response, credentialIssuerMetadata))
                .doOnSuccess(credentialResponse -> log.info("ProcessID: {} - CredentialResponse: {}", processId, credentialResponse));
    }

    /**
     * Handles the deferred credential request. This method manages the logic for requesting
     * a deferred credential using the provided acceptanceToken.
     * A delay is introduced before sending the request to align with the issuer's processing time.
     * According to the issuer's specification, the deferred credential is forced to go through the
     * deferred flow and will only be available after a delay of 5 seconds from the first Credential Request.
     * Therefore, a delay of 10 seconds is added here to ensure that the issuer has sufficient time
     * to process the request and make the credential available.
     */
    private Mono<CredentialResponse> handleCredentialResponse(String response, CredentialIssuerMetadata credentialIssuerMetadata) {
        try {
            CredentialResponse credentialResponse = objectMapper.readValue(response, CredentialResponse.class);
            if (credentialResponse.acceptanceToken() != null) {
                return Mono.delay(Duration.ofSeconds(10))
                        .then(handleDeferredCredential(credentialResponse.acceptanceToken(), credentialIssuerMetadata));
            } else {
                return Mono.just(credentialResponse);
            }
        } catch (Exception e) {
            log.error("Error while processing CredentialResponse", e);
            return Mono.error(new FailedDeserializingException("Error processing CredentialResponse: " + response));
        }
    }

    private Mono<CredentialResponse> handleDeferredCredential(String acceptanceToken, CredentialIssuerMetadata credentialIssuerMetadata) {
        List<Map.Entry<String, String>> headers = List.of(Map.entry(HEADER_AUTHORIZATION, BEARER + acceptanceToken));

        // Logic to handle the deferred credential request using acceptanceToken
        return postRequest(credentialIssuerMetadata.deferredCredentialEndpoint(),headers,"")
                .flatMap(response -> {
                    try {
                        log.debug(response);
                        CredentialResponse credentialResponse = objectMapper.readValue(response, CredentialResponse.class);
                        if (credentialResponse.acceptanceToken() != null && !credentialResponse.acceptanceToken().equals(acceptanceToken)) {
                            // New acceptance token received, call recursively
                            return handleDeferredCredential(credentialResponse.acceptanceToken(), credentialIssuerMetadata);
                        } else if (credentialResponse.credential() != null) {
                            // Credential received, return the response
                            return Mono.just(credentialResponse);
                        } else {
                            // No credential and no new token, throw an error
                            return Mono.error(new IllegalStateException("No credential or new acceptance token received"));
                        }
                    } catch (Exception e) {
                        log.error("Error while processing deferred CredentialResponse", e);
                        return Mono.error(new FailedDeserializingException("Error processing deferred CredentialResponse: " + response));
                    }
                });
    }


    private Mono<String> postCredential(TokenResponse tokenResponse,
                                        CredentialIssuerMetadata credentialIssuerMetadata,
                                        CredentialRequestEbsi credentialRequest) {
        try {
            List<Map.Entry<String, String>> headers = List.of(Map.entry(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON),
                    Map.entry(HEADER_AUTHORIZATION, BEARER + tokenResponse.accessToken()));
            return postRequest(credentialIssuerMetadata.credentialEndpoint(), headers, objectMapper.writeValueAsString(credentialRequest))
                    .onErrorResume(e -> {
                        log.error("Error while fetching Credential from Issuer: {}", e.getMessage());
                        return Mono.error(new FailedCommunicationException("Error while fetching  Credential from Issuer"));
                    });
        } catch (Exception e) {
            log.error("Error while serializing CredentialRequest: {}", e.getMessage());
            return Mono.error(new FailedSerializingException("Error while serializing Credential Request"));
        }
    }

    private Mono<CredentialRequestEbsi> buildCredentialRequest(String nonce, String issuer, String did,String format,List<String> types){
        Instant issueTime = Instant.now();
        Instant expirationTime = issueTime.plus(10, ChronoUnit.MINUTES);
        JWTClaimsSet payload = new JWTClaimsSet.Builder()
                .issuer(did)
                .audience(issuer)
                .issueTime(java.util.Date.from(issueTime))
                .expirationTime(java.util.Date.from(expirationTime))
                .claim("nonce", nonce)
                .build();
        try {
            JsonNode documentNode = objectMapper.readTree(payload.toString());

            return vaultService.getSecretByKey(did,PRIVATE_KEY_TYPE)
                    .flatMap(privateKey -> signerService.buildJWTSFromJsonNode(documentNode,did,"proof",privateKey))
                    .flatMap(jwt -> Mono.just(CredentialRequestEbsi.builder()
                            .format(format)
                            .types(types)
                            .proof(CredentialRequestEbsi.Proof.builder().proofType("jwt").jwt(jwt).build())
                            .build()))
                    .doOnNext(requestBody -> log.debug("Credential Request Body: {}", requestBody))
                    .onErrorResume(e -> {
                        log.error("Error creating CredentialRequestBodyDTO", e);
                        return Mono.error(new RuntimeException("Error creating CredentialRequestBodyDTO", e));
                    });
        }catch (JsonProcessingException e){
            log.error("Error while parsing the JWT payload", e);
            return Mono.error(new ParseErrorException("Error while parsing the JWT payload: " + e.getMessage()));
        }

    }

}
