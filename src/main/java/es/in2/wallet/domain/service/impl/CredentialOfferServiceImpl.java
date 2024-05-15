package es.in2.wallet.domain.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import es.in2.wallet.domain.exception.FailedCommunicationException;
import es.in2.wallet.domain.exception.FailedDeserializingException;
import es.in2.wallet.domain.model.CredentialOffer;
import es.in2.wallet.domain.service.CredentialOfferService;
import es.in2.wallet.infrastructure.core.config.WebClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static es.in2.wallet.domain.util.ApplicationConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialOfferServiceImpl implements CredentialOfferService {

    private final ObjectMapper objectMapper;
    private final WebClientConfig webClient;
    @Override
    public Mono<CredentialOffer> getCredentialOfferFromCredentialOfferUri(String processId, String credentialOfferUri) {
        return parseCredentialOfferUri(credentialOfferUri)
                .doOnSuccess(credentialOfferUriValue -> log.info("ProcessId: {}, Credential Offer Uri parsed successfully: {}", processId, credentialOfferUriValue))
                .doOnError(e -> log.error("ProcessId: {}, Error while parsing Credential Offer Uri: {}", processId, e.getMessage()))
                .flatMap(this::getCredentialOffer)
                .doOnSuccess(credentialOffer -> log.info("ProcessId: {}, Credential Offer fetched successfully: {}", processId, credentialOffer))
                .doOnError(e -> log.error("ProcessId: {}, Error while fetching Credential Offer: {}", processId, e.getMessage()))
                .flatMap(this::parseCredentialOfferResponse)
                .doOnSuccess(preAuthorizedCredentialOffer -> log.info("ProcessId: {}, Credential Offer parsed successfully: {}", processId, preAuthorizedCredentialOffer))
                .doOnError(e -> log.error("ProcessId: {}, Error while parsing Credential Offer: {}", processId, e.getMessage()));
    }

    private Mono<String> parseCredentialOfferUri(String credentialOfferUri) {
        return Mono.fromCallable(() -> {
            try {
                String[] splitCredentialOfferUri = credentialOfferUri.split("=");
                String credentialOfferUriValue = splitCredentialOfferUri[1];
                return URLDecoder.decode(credentialOfferUriValue, StandardCharsets.UTF_8);
            }catch (Exception e){
                log.debug("Credential offer uri it's already parsed");
                return credentialOfferUri;
            }

        });
    }
    private Mono<String> getCredentialOffer(String credentialOfferUri) {
        log.info("CredentialOfferServiceImpl - getCredentialOffer invoked");
        return webClient.centralizedWebClient()
                .get()
                .uri(credentialOfferUri)
                .header(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON)
                .exchangeToMono(response -> {
                    if (response.statusCode().is4xxClientError() || response.statusCode().is5xxServerError()) {
                        return Mono.error(new RuntimeException("Error while fetching credentialOffer from the issuer, error: " + response));
                    }
                    else {
                        log.info("Credential Offer: {}", response);
                        return response.bodyToMono(String.class);
                    }
                })
                .onErrorResume(e -> Mono.error(new FailedCommunicationException("Error while fetching credentialOffer from the issuer", e)));
    }

    private Mono<CredentialOffer> parseCredentialOfferResponse(String response) {
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            if (rootNode.has(CREDENTIALS)) {
                JsonNode credentialsNode = rootNode.get(CREDENTIALS);
                List<CredentialOffer.Credential> updatedCredentials = new ArrayList<>();

                if (credentialsNode.isArray()) {
                    for (JsonNode credentialNode : credentialsNode) {

                        if (credentialNode.has("type") && !credentialNode.has("types")) {

                            String type = credentialNode.get("type").asText();
                            List<String> types = Collections.singletonList(type);

                            ObjectNode modifiedCredentialNode = credentialNode.deepCopy();
                            modifiedCredentialNode.remove("type");

                            modifiedCredentialNode.set("types", objectMapper.valueToTree(types));

                            CredentialOffer.Credential credential = objectMapper.treeToValue(modifiedCredentialNode, CredentialOffer.Credential.class);
                            updatedCredentials.add(credential);
                        } else {
                            CredentialOffer.Credential credential = objectMapper.treeToValue(credentialNode, CredentialOffer.Credential.class);
                            updatedCredentials.add(credential);
                        }
                    }
                    ((ObjectNode)rootNode).set(CREDENTIALS, objectMapper.valueToTree(updatedCredentials));
                }
            }
            CredentialOffer credentialOffer = objectMapper.treeToValue(rootNode, CredentialOffer.class);
            return Mono.just(credentialOffer);
        } catch (Exception e) {
            return Mono.error(new FailedDeserializingException("Error while deserializing Credential Offer: " + e.getMessage()));
        }
    }

}
