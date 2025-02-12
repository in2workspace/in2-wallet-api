package es.in2.wallet.application.workflows.issuance;

import reactor.core.publisher.Mono;

public interface CredentialIssuanceCommonWorkflow {
    Mono<Void> identifyAuthMethod(String processId, String authorizationToken, String qrContent);
}

