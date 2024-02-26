package es.in2.wallet.api.facade;

import es.in2.wallet.api.model.CredentialsBasicInfo;
import reactor.core.publisher.Mono;

public interface CredentialPresentationForTurnstileServiceFacade {
    Mono<String> createVerifiablePresentationForTurnstile(String processId, String authorizationToken, CredentialsBasicInfo credentialsBasicInfo);
}
