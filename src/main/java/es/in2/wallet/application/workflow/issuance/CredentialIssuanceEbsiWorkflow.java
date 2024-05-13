package es.in2.wallet.application.workflow.issuance;

import reactor.core.publisher.Mono;

public interface CredentialIssuanceEbsiWorkflow {
    Mono<Void> identifyAuthMethod(String processId, String authorizationToken, String qrContent);
}
