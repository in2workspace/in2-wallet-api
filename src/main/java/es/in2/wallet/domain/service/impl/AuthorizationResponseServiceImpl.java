package es.in2.wallet.domain.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import es.in2.wallet.domain.exception.FailedDeserializingException;
import es.in2.wallet.domain.model.*;
import es.in2.wallet.domain.service.AuthorizationResponseService;
import es.in2.wallet.infrastructure.core.config.WebClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static es.in2.wallet.domain.util.ApplicationConstants.*;


@Slf4j
@Service
@RequiredArgsConstructor

public class AuthorizationResponseServiceImpl implements AuthorizationResponseService {

    private final ObjectMapper objectMapper;

    private final WebClientConfig webClient;

    @Override
    public Mono<String> buildAndPostAuthorizationResponseWithVerifiablePresentation(String processId, VcSelectorResponse vcSelectorResponse, String verifiablePresentation, String authorizationToken) throws JsonProcessingException {
        return generateDescriptorMapping(verifiablePresentation)
                .flatMap(descriptorMapping ->
                        getPresentationSubmissionAsString(processId, descriptorMapping))
                .flatMap(presentationSubmissionString ->
                        postAuthorizationResponse(processId, vcSelectorResponse, verifiablePresentation,
                                presentationSubmissionString, authorizationToken));
    }

    @Override
    public Mono<Void> sendDomeAuthorizationResponse(String vpToken, VcSelectorResponse vcSelectorResponse) {
        String body = "vp_token=" + vpToken;

        String urlWithState = vcSelectorResponse.redirectUri() + "?state=" + vcSelectorResponse.state();
        return webClient.centralizedWebClient()
                .post()
                .uri(urlWithState)
                .header(CONTENT_TYPE, CONTENT_TYPE_URL_ENCODED_FORM)
                .bodyValue(body)
                .exchangeToMono(response -> {
                    if (response.statusCode().is4xxClientError() || response.statusCode().is5xxServerError()) {
                        return Mono.error(new RuntimeException("There was an error during the DOME attestation exchange, error" + response));
                    } else {
                        log.info("DOME attestation exchange completed");
                        return Mono.empty();
                    }
                });
    }

    private Mono<DescriptorMap> generateDescriptorMapping(String verifiablePresentationString) throws JsonProcessingException {
        // Parse the Verifiable Presentation
        return parseVerifiablePresentationFromString(verifiablePresentationString)
                .flatMap(verifiablePresentation ->
                        // Process each Verifiable Credential in the Verifiable Presentation
                        Flux.fromIterable(Objects.requireNonNull(verifiablePresentation.verifiableCredential()))
                                .index()
                                .flatMap(indexed -> {
                                    String credential = indexed.getT2();
                                    Long index = indexed.getT1();
                                    try {
                                        return parseVerifiableCredentialFromString(credential)
                                                .map(verifiableCredential ->
                                                        new DescriptorMap(JWT_VC, "$.verifiableCredential[" + index + "]", verifiableCredential.id(), null)
                                                );
                                    } catch (JsonProcessingException e) {
                                        return Mono.error(new FailedDeserializingException("Error while deserializing Verifiable Credential: " + e));
                                    }
                                })
                                .collectList()  // Collect DescriptorMappings into a List
                                .flatMap(list -> buildDescriptorMapping(list, verifiablePresentation.id())) // Build the final DescriptorMapping
                );

    }

    private Mono<VerifiablePresentation> parseVerifiablePresentationFromString(String verifiablePresentationString) throws JsonProcessingException {
        try {
            JWT jwt = JWTParser.parse(verifiablePresentationString);
            JWTClaimsSet claimsSet = jwt.getJWTClaimsSet();
            JsonNode rootNode = objectMapper.valueToTree(claimsSet.getClaim("vp"));
            VerifiablePresentation verifiablePresentation = objectMapper.treeToValue(rootNode, VerifiablePresentation.class);
            return Mono.just(verifiablePresentation);
        } catch (ParseException e) {
            return Mono.error(new FailedDeserializingException("Error while deserializing Verifiable Presentation: " + e));
        }
    }

    private Mono<VerifiableCredential> parseVerifiableCredentialFromString(String verifiableCredentialString) throws JsonProcessingException {
        try {
            JWT jwt = JWTParser.parse(verifiableCredentialString);
            JWTClaimsSet claimsSet = jwt.getJWTClaimsSet();
            JsonNode rootNode = objectMapper.valueToTree(claimsSet.getClaim("vc"));
            VerifiableCredential verifiableCredential = objectMapper.treeToValue(rootNode, VerifiableCredential.class);
            return Mono.just(verifiableCredential);
        } catch (ParseException e) {
            return Mono.error(new FailedDeserializingException("Error while deserializing Verifiable Credential: " + e));
        }
    }

    private Mono<DescriptorMap> buildDescriptorMapping(List<DescriptorMap> descriptorMappingList, String verifiablePresentationId) {
        // Check if the list is empty
        if (descriptorMappingList == null || descriptorMappingList.isEmpty()) {
            return Mono.empty();
        }
        // If the list has only one element, just return it
        Mono<DescriptorMap> result = Mono.just(descriptorMappingList.get(0));
        // If the list has more than one element, recursively add the DescriptorMappings
        for (int i = 1; i < descriptorMappingList.size(); i++) {
            DescriptorMap tmpCredentialDescriptorMap = descriptorMappingList.get(i);
            result = result.flatMap(credentialDescriptorMap ->
                    addCredentialDescriptorMap(credentialDescriptorMap, tmpCredentialDescriptorMap));
        }
        return result.map(finalMap -> new DescriptorMap(JWT_VP, "$", verifiablePresentationId, finalMap));
    }

    private Mono<String> getPresentationSubmissionAsString(String processId, DescriptorMap descriptorMapping) {
        return Mono.fromCallable(() -> {
                    PresentationSubmission presentationSubmission = new PresentationSubmission(
                            CUSTOMER_PRESENTATION_SUBMISSION,
                            CUSTOMER_PRESENTATION_DEFINITION,
                            Collections.singletonList(descriptorMapping)
                    );
                    return objectMapper.writeValueAsString(presentationSubmission);
                })
                .doOnSuccess(presentationSubmissionString ->
                        log.info("ProcessID: {} - PresentationSubmission: {}", processId, presentationSubmissionString))
                .onErrorResume(e -> {
                    log.error("ProcessID: {} - Error parsing PresentationSubmission to String: {}", processId, e.getMessage());
                    return Mono.error(new RuntimeException("Error parsing PresentationSubmission", e));
                });
    }

    private Mono<DescriptorMap> addCredentialDescriptorMap(DescriptorMap credentialDescriptorMap, DescriptorMap tmpCredentialDescriptorMap) {
        // If the original DescriptorMapping is null, just return the temporary one
        if (credentialDescriptorMap == null) {
            return Mono.just(tmpCredentialDescriptorMap);
        }
        // If the pathNested of the original DescriptorMapping is null, create a new instance with the updated pathNested
        if (credentialDescriptorMap.pathNested() == null) {
            DescriptorMap updatedMap = new DescriptorMap(
                    credentialDescriptorMap.format(),
                    credentialDescriptorMap.path(),
                    credentialDescriptorMap.id(),
                    tmpCredentialDescriptorMap
            );
            return Mono.just(updatedMap);
        } else {
            // If pathNested is not null, recursively update pathNested
            return addCredentialDescriptorMap(credentialDescriptorMap.pathNested(), tmpCredentialDescriptorMap)
                    .map(updatedNestedMap -> new DescriptorMap(
                            credentialDescriptorMap.format(),
                            credentialDescriptorMap.path(),
                            credentialDescriptorMap.id(),
                            updatedNestedMap
                    ));
        }
    }

    private Mono<String> postAuthorizationResponse(String processId, VcSelectorResponse vcSelectorResponse,
                                                   String verifiablePresentation, String presentationSubmissionString, String authorizationToken) {
        // Build URL encoded form data request body
        Map<String, String> formDataMap = Map.of(
                "state", vcSelectorResponse.state(),
                "vp_token", verifiablePresentation,
                "presentation_submission", presentationSubmissionString);
        // Build the request body
        String xWwwFormUrlencodedBody = formDataMap.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        // Post request
        return webClient.centralizedWebClient()
                .post()
                .uri(vcSelectorResponse.redirectUri())
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.AUTHORIZATION, BEARER + authorizationToken)
                .bodyValue(xWwwFormUrlencodedBody)
                .exchangeToMono(response -> {
                    if (response.statusCode().is4xxClientError() || response.statusCode().is5xxServerError()) {
                        return Mono.error(new RuntimeException("There was an error during the attestation exchange, error" + response));
                    } else if (response.statusCode().is3xxRedirection()) {
                        return Mono.just(Objects.requireNonNull(response.headers().asHttpHeaders().getFirst(HttpHeaders.LOCATION)));
                    } else {
                        log.info("ProcessID: {} - Authorization Response: {}", processId, response);
                        return response.bodyToMono(String.class);
                    }
                });
    }

}
