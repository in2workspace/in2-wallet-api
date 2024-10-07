package es.in2.wallet.domain.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.domain.exception.FailedCommunicationException;
import es.in2.wallet.domain.exception.IssuerNotAuthorizedException;
import es.in2.wallet.domain.exception.JsonReadingException;
import es.in2.wallet.domain.model.IssuerAttribute;
import es.in2.wallet.domain.model.IssuerCredentialsCapabilities;
import es.in2.wallet.domain.model.IssuerResponse;
import es.in2.wallet.domain.service.TrustedIssuerListService;
import es.in2.wallet.infrastructure.core.config.WebClientConfig;
import es.in2.wallet.infrastructure.core.config.properties.TrustedIssuerListProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrustedIssuerListServiceImpl implements TrustedIssuerListService {
    private final ObjectMapper objectMapper;
    private final WebClientConfig webClient;
    private final TrustedIssuerListProperties trustedIssuerListProperties;
    @Override
    public Mono<List<IssuerCredentialsCapabilities>> getTrustedIssuerListData(String id) {
        return webClient.centralizedWebClient()
                .get()
                .uri(trustedIssuerListProperties.uri() + id)
                .retrieve()
                .onStatus(
                        status -> status != null && status.is4xxClientError(),
                        response -> response.createException().flatMap(error -> {
                            // Check if the status is specifically 404
                            if (response.statusCode().value() == 404) {
                                log.error("Issuer with id: {} not found (404).", id);
                                return Mono.error(new IssuerNotAuthorizedException("Issuer with id: " + id + " not found."));
                            } else {
                                log.error("Client error while fetching issuer data. Status code: {}", response.statusCode());
                                return Mono.error(new FailedCommunicationException("Client error while fetching issuer data"));
                            }
                        })
                )
                .onStatus(
                        status -> status != null && status.is5xxServerError(),
                        response -> {
                            log.error("Client error while fetching issuer data. Status code: {}", response.statusCode());
                            return Mono.error(new FailedCommunicationException("Client error while fetching issuer data"));
                        }
                )
                .bodyToMono(String.class)
                .flatMap(responseBody -> {
                    try {
                        // Step 2: Map response to IssuerResponse object
                        IssuerResponse issuerResponse = objectMapper.readValue(responseBody, IssuerResponse.class);

                        // Step 3: Decode and map each attribute's body to IssuerCredentialsCapabilities reactively
                        return Flux.fromIterable(issuerResponse.attributes())
                                .flatMap(this::decodeAndMapIssuerAttributeBody) // Use reactive sub-method
                                .collectList(); // Collect all capabilities into a list
                    } catch (IOException e) {
                        log.error("Error mapping response to IssuerResponse: {}", e.getMessage());
                        return Mono.error(new JsonReadingException("Error mapping response to IssuerResponse"));
                    }
                });
    }

    // Reactive helper method to decode Base64 and map to IssuerCredentialsCapabilities
    private Mono<IssuerCredentialsCapabilities> decodeAndMapIssuerAttributeBody(IssuerAttribute issuerAttribute) {
        return Mono.fromCallable(() -> {
                    // Step 1: Decode the Base64 body
                    String decodedBody = new String(Base64.getDecoder().decode(issuerAttribute.body()), StandardCharsets.UTF_8);

                    // Step 2: Map the decoded string to IssuerCredentialsCapabilities
                    return objectMapper.readValue(decodedBody, IssuerCredentialsCapabilities.class);
                })
                .doOnError(e -> log.error("Failed to decode and map issuer attribute body: {}", e.getMessage()))
                .onErrorMap(e -> new JsonReadingException("Failed to decode and map issuer attribute body"));
    }
}
