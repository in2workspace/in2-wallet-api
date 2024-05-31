package es.in2.wallet.domain.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.domain.exception.FailedDeserializingException;
import es.in2.wallet.domain.exception.FailedSerializingException;
import es.in2.wallet.domain.model.*;
import es.in2.wallet.domain.service.CredentialService;
import es.in2.wallet.infrastructure.core.config.WebClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static es.in2.wallet.domain.util.ApplicationConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialServiceImpl implements CredentialService {

    private final ObjectMapper objectMapper;
    private final WebClientConfig webClient;

    @Override
    public Mono<CredentialResponse> getCredential(String jwt, TokenResponse tokenResponse, CredentialIssuerMetadata credentialIssuerMetadata, String format, List<String> types) {
        String processId = MDC.get(PROCESS_ID);
        // build CredentialRequest
        return buildCredentialRequest(jwt,format,types)
                .doOnSuccess(credentialRequest -> log.info("ProcessID: {} - CredentialRequest: {}", processId, credentialRequest))
                // post CredentialRequest
                .flatMap(credentialRequest -> postCredential(tokenResponse.accessToken(), credentialIssuerMetadata.credentialEndpoint(), credentialRequest))
                .doOnSuccess(response -> log.info("ProcessID: {} - Credential Post Response: {}", processId, response))
                // handle CredentialResponse or deferred response
                .flatMap(response -> handleCredentialResponse(response, credentialIssuerMetadata))
                .doOnSuccess(credentialResponse -> log.info("ProcessID: {} - CredentialResponse: {}", processId, credentialResponse));
    }

    @Override
    public Mono<CredentialResponse> getCredentialDomeDeferredCase(String transactionId, String accessToken, String deferredEndpoint) {
        String processId = MDC.get(PROCESS_ID);
        DeferredCredentialRequest deferredCredentialRequest = DeferredCredentialRequest.builder().transactionId(transactionId).build();
        return postCredential(accessToken,deferredEndpoint,deferredCredentialRequest)
                .flatMap(response -> {
                    try {
                    CredentialResponse credentialResponse = objectMapper.readValue(response, CredentialResponse.class);
                    return Mono.just(credentialResponse);
                    }
                    catch (Exception e) {
                        log.error("Error while processing deferred CredentialResponse", e);
                        return Mono.error(new FailedDeserializingException("Error processing deferred CredentialResponse: " + response));
                    }
                }
                ).doOnSuccess(credentialResponse -> log.info("ProcessID: {} - CredentialResponse: {}", processId, credentialResponse));
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
        // Logic to handle the deferred credential request using acceptanceToken
        return webClient.centralizedWebClient()
                .post()
                .uri(credentialIssuerMetadata.deferredCredentialEndpoint())
                .header(HEADER_AUTHORIZATION, BEARER + acceptanceToken)
                .exchangeToMono(response -> {
                    if (response.statusCode().is4xxClientError() || response.statusCode().is5xxServerError()) {
                        return Mono.error(new RuntimeException("There was an error during the deferred credential request, error" + response));
                    } else {
                        log.info("Deferred credential response retrieve");
                        return response.bodyToMono(String.class);
                    }
                })
                .flatMap(response -> {
                    try {
                        log.debug(response);
                        CredentialResponse credentialResponse = objectMapper.readValue(response, CredentialResponse.class);
                        if (credentialResponse.acceptanceToken() != null && !credentialResponse.acceptanceToken().equals(acceptanceToken)) {
                            // New acceptance token received, call recursively
                            return handleDeferredCredential(credentialResponse.acceptanceToken(), credentialIssuerMetadata);
                        }
                        //Handle deferred issuance for DOME profile
                        else if (credentialResponse.transactionId() != null && credentialResponse.credential() != null){
                            return Mono.just(credentialResponse);
                        }
                        else if (credentialResponse.credential() != null) {
                            // Credential received, return the response
                            return Mono.just(credentialResponse);
                        }
                        else {
                            // No credential and no new token, throw an error
                            return Mono.error(new IllegalStateException("No credential or new acceptance token received"));
                        }
                    } catch (Exception e) {
                        log.error("Error while processing deferred CredentialResponse", e);
                        return Mono.error(new FailedDeserializingException("Error processing deferred CredentialResponse: " + response));
                    }
                });
    }


    private Mono<String> postCredential(String accessToken,
                                        String credentialEndpoint,
                                        Object credentialRequest) {
        try {
            return webClient.centralizedWebClient()
                    .post()
                    .uri(credentialEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, BEARER + accessToken)
                    .bodyValue(objectMapper.writeValueAsString(credentialRequest))
                    .exchangeToMono(response -> {
                        if (response.statusCode().is4xxClientError() || response.statusCode().is5xxServerError()) {
                            return Mono.error(new RuntimeException("There was an error during the credential request, error" + response));
                        } else {
                            log.info("Credential response retrieved: {}", response);
                            return response.bodyToMono(String.class);
                        }
                    });
        } catch (Exception e) {
            log.error("Error while serializing CredentialRequest: {}", e.getMessage());
            return Mono.error(new FailedSerializingException("Error while serializing Credential Request"));
        }
    }

    private Mono<?> buildCredentialRequest(String jwt, String format, List<String> types){
        if (types == null){
            return Mono.just(CredentialRequest.builder()
                    .format(format)
                    .proof(CredentialRequest.Proof.builder().proofType("jwt").jwt(jwt).build())
                    .build())
                    .doOnNext(requestBody -> log.debug("Credential Request Body for DOME Profile: {}", requestBody));
        }
        else if (types.size() > 1){
            return Mono.just(CredentialRequest.builder()
                            .format(format)
                            .types(types)
                            .proof(CredentialRequest.Proof.builder().proofType("jwt").jwt(jwt).build())
                            .build())
                    .doOnNext(requestBody -> log.debug("Credential Request Body: {}", requestBody));
        }
        else {
            return Mono.just(FiwareCredentialRequest.builder()
                            .format(format)
                            .type(types.get(0))
                            .types(types)
                            .build())
                    .doOnNext(requestBody -> log.debug("Credential Request Body: {}", requestBody));
        }
    }

}
