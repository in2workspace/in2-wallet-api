package es.in2.wallet.api.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import es.in2.wallet.api.exception.FailedCommunicationException;
import es.in2.wallet.api.exception.FailedDeserializingException;
import es.in2.wallet.api.model.CredentialOffer;
import es.in2.wallet.api.service.CredentialOfferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static es.in2.wallet.api.util.ApplicationUtils.getRequest;
import static es.in2.wallet.api.util.MessageUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialOfferServiceImpl implements CredentialOfferService {

    private final ObjectMapper objectMapper;

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
        List<Map.Entry<String, String>> headers = List.of(Map.entry(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON));
        return getRequest(credentialOfferUri, headers)
                .onErrorResume(e -> Mono.error(new FailedCommunicationException("Error while fetching credentialOffer from the issuer")));
    }

    /**
     * Parses the Credential Offer response and handles backward compatibility.
     * If the response contains credentials as a list of strings, it converts them to a list of Credential objects.
     * This method is marked as deprecated and is intended to be replaced in future versions for improved logic.
     *
     * @param response The response String to be parsed.
     * @return A Mono<CredentialOffer> instance.
     * @deprecated (since = "2.0.0", forRemoval = true) Temporary implementation to handle backward compatibility.
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    private Mono<CredentialOffer> parseCredentialOfferResponse(String response) {
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            // Check if credentials are provided as an array of strings
            if (rootNode.has(CREDENTIALS) && rootNode.get(CREDENTIALS).isArray() &&
                    rootNode.get(CREDENTIALS).get(0).isTextual()) {
                // Custom logic for handling credentials as a list of strings
                List<String> list = new ArrayList<>();
                for (JsonNode node : rootNode.get(CREDENTIALS)) {
                    list.add(node.asText());
                }
                CredentialOffer.Credential credential = CredentialOffer.Credential.builder().types(list).build();
                // Remove old 'credentials' node from the root
                ((ObjectNode) rootNode).remove(CREDENTIALS);
                // Deserialize the modified root node to CredentialOffer
                CredentialOffer credentialOffer = objectMapper.treeToValue(rootNode, CredentialOffer.class);
                return Mono.just(CredentialOffer.builder()
                        .credentialIssuer(credentialOffer.credentialIssuer())
                        .credentials(List.of(credential))
                        .grant(credentialOffer.grant())
                        .build());
            } else {
                // Standard deserialization for Credential Offer
                return Mono.just(objectMapper.readValue(response, CredentialOffer.class));
            }
        } catch (Exception e) {
            return Mono.error(new FailedDeserializingException("Error while deserializing CredentialOffer: " + e));
        }
    }

}
