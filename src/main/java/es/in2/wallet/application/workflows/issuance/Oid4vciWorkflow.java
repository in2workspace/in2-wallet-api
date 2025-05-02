package es.in2.wallet.application.workflows.issuance;

import reactor.core.publisher.Mono;

public interface Oid4vciWorkflow {
    Mono<Void> execute(String processId, String authorizationToken, String qrContent);
}

