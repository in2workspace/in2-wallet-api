package es.in2.wallet.application.service;

import reactor.core.publisher.Mono;

public interface RequestSignedLEARCredentialService {
    Mono<Void> requestSignedLEARCredentialServiceByCredentialId(String processId, String userId, String credentialId);
}
