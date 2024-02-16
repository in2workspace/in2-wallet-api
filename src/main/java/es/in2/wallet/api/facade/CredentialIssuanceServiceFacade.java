package es.in2.wallet.api.facade;

import reactor.core.publisher.Mono;

public interface CredentialIssuanceServiceFacade {
    Mono<Void> identifyAuthMethod(String processId, String authorizationToken, String qrContent);
}

