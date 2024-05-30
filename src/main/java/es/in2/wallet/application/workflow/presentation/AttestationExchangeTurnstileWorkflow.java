package es.in2.wallet.application.workflow.presentation;

import es.in2.wallet.domain.model.CredentialsBasicInfo;
import reactor.core.publisher.Mono;

public interface AttestationExchangeTurnstileWorkflow {
    Mono<String> createVerifiablePresentationForTurnstile(String processId, String authorizationToken, CredentialsBasicInfo credentialsBasicInfo);
}
