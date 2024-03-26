package es.in2.wallet.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

public interface SignerService {
    Mono<String> buildJWTSFromJsonNode(JsonNode document, String did, String documentType);
}
