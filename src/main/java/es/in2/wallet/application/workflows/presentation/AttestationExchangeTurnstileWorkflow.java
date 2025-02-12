package es.in2.wallet.application.workflows.presentation;

import es.in2.wallet.application.dto.CredentialsBasicInfo;
import reactor.core.publisher.Mono;

public interface AttestationExchangeTurnstileWorkflow {
    Mono<String> createVerifiablePresentationForTurnstile(String processId, String authorizationToken, CredentialsBasicInfo credentialsBasicInfo);
}
