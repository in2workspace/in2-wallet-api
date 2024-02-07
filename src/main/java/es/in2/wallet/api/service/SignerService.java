package es.in2.wallet.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

public interface SignerService {
    Mono<String> buildJWTSFromJsonNode(JsonNode document, String did, String documentType, String privateKey);
}
