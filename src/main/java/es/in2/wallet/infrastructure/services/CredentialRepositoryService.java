package es.in2.wallet.infrastructure.services;

import es.in2.wallet.application.dto.CredentialResponse;
import es.in2.wallet.application.dto.CredentialsBasicInfo;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface CredentialRepositoryService {
    Mono<UUID> saveCredential(String processId, UUID userId, CredentialResponse credentialResponse, String format);
    Mono<Void> saveDeferredCredential(String processId, String userId, String credentialId, CredentialResponse credentialResponse);
    Mono<List<CredentialsBasicInfo>> getCredentialsByUserId(String processId, String userId);
    Mono<String> extractDidFromCredential(String processId, String credentialId, String userId);
    Mono<Void> deleteCredential(String processId, String credentialId, String userId);
    Mono<List<CredentialsBasicInfo>> getCredentialsByUserIdAndType(String processId, String userId, String requiredType);
    Mono<String> getCredentialDataByIdAndUserId(String processId, String userId, String credentialId);
}
