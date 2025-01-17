package es.in2.wallet.infrastructure.services;

import es.in2.wallet.application.dto.CredentialResponse;
import es.in2.wallet.application.dto.CredentialsBasicInfo;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface CredentialRepositoryService {
    Mono<UUID> saveCredential(String processId, UUID userId, CredentialResponse credentialResponse);
    Mono<Void> saveDeferredCredential(String processId, String userId, String credentialId, CredentialResponse credentialResponse);
    Mono<List<CredentialsBasicInfo>> getCredentialsByUserId(String processId, String userId);
}
