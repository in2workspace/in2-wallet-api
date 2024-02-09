package es.in2.wallet.api.service;

import reactor.core.publisher.Mono;

public interface VerifierValidationService {
    Mono<String> verifyIssuerOfTheAuthorizationRequest(String processId, String jwtAuthorizationRequest);
}