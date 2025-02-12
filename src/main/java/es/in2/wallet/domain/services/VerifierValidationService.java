package es.in2.wallet.domain.services;

import reactor.core.publisher.Mono;

public interface VerifierValidationService {
    Mono<String> verifyIssuerOfTheAuthorizationRequest(String processId, String jwtAuthorizationRequest);
}
