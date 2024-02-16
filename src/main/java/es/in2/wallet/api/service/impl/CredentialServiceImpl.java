package es.in2.wallet.api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.api.exception.FailedCommunicationException;
import es.in2.wallet.api.exception.FailedDeserializingException;
import es.in2.wallet.api.exception.FailedSerializingException;
import es.in2.wallet.api.model.CredentialIssuerMetadata;
import es.in2.wallet.api.model.CredentialRequest;
import es.in2.wallet.api.model.CredentialResponse;
import es.in2.wallet.api.model.TokenResponse;
import es.in2.wallet.api.service.CredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static es.in2.wallet.api.util.MessageUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialServiceImpl implements CredentialService {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<CredentialResponse> getCredential(String processId, String jwt, TokenResponse tokenResponse, CredentialIssuerMetadata credentialIssuerMetadata) {
        // build CredentialRequest
        return buildCredentialRequest(jwt)
                .doOnSuccess(credentialRequest -> log.info("ProcessID: {} - CredentialRequest: {}", processId, credentialRequest))
                // post CredentialRequest
                .flatMap(credentialRequest -> postCredential(tokenResponse, credentialIssuerMetadata, credentialRequest))
                .doOnSuccess(response -> log.info("ProcessID: {} - Credential Post Response: {}", processId, response))
                // deserialize CredentialResponse
                .flatMap(response -> {
                    try {
                        CredentialResponse credentialResponse = objectMapper.readValue(response, CredentialResponse.class);
                        return Mono.just(credentialResponse);
                    } catch (Exception e) {
                        log.error("Error while deserializing CredentialResponse from the issuer", e);
                        return Mono.error(new FailedDeserializingException("Error while deserializing CredentialResponse: " + response));
                    }
                })
                .doOnSuccess(credentialResponse -> log.info("ProcessID: {} - CredentialResponse: {}", processId, credentialResponse));
    }

    private Mono<String> postCredential(TokenResponse tokenResponse, CredentialIssuerMetadata credentialIssuerMetadata,
                                        CredentialRequest credentialRequest) {
        List<Map.Entry<String, String>> headers = List.of(Map.entry(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON),
                Map.entry(HEADER_AUTHORIZATION, BEARER + tokenResponse.accessToken()));
        try {
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
    private Mono<CredentialRequest> buildCredentialRequest(String jwt){
        return Mono.just(CredentialRequest.builder()
                        .format("jwt_vc_json")
                        .proof(CredentialRequest.Proof.builder().proofType("jwt").jwt(jwt).build())
                        .build())
                .doOnNext(requestBody -> log.debug("Credential Request Body: {}", requestBody))
                .onErrorResume(e -> {
                    log.error("Error creating CredentialRequestBodyDTO", e);
                    return Mono.error(new RuntimeException("Error creating CredentialRequestBodyDTO", e));
                });
    }

}
