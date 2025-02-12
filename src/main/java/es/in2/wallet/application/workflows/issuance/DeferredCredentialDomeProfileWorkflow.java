package es.in2.wallet.application.workflows.issuance;

import reactor.core.publisher.Mono;

public interface DeferredCredentialDomeProfileWorkflow {
    Mono<Void> requestDeferredCredential(String processId, String userId, String credentialId);
}
