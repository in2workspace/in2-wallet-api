package es.in2.wallet.application.service;

import reactor.core.publisher.Mono;

public interface CredentialIssuanceService {
    Mono<Void> identifyAuthMethod(String processId, String authorizationToken, String qrContent);
}

