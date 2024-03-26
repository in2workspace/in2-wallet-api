package es.in2.wallet.application.service;

import es.in2.wallet.domain.model.CredentialsBasicInfo;
import reactor.core.publisher.Mono;

public interface TurnstileAttestationExchangeService {
    Mono<String> createVerifiablePresentationForTurnstile(String processId, String authorizationToken, CredentialsBasicInfo credentialsBasicInfo);
}
