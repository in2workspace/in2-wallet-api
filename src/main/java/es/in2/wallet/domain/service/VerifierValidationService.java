package es.in2.wallet.domain.service;

import reactor.core.publisher.Mono;

public interface VerifierValidationService {
    Mono<String> verifyIssuerOfTheAuthorizationRequest(String processId, String jwtAuthorizationRequest);
}
