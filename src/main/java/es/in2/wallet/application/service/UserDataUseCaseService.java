package es.in2.wallet.application.service;

import es.in2.wallet.domain.model.CredentialsBasicInfo;
import reactor.core.publisher.Mono;

import java.util.List;

public interface UserDataUseCaseService {
    public Mono<List<CredentialsBasicInfo>> getUserVCs(String processId, String userId);
    Mono<Void> deleteVerifiableCredentialById(String processId,String credentialId, String userId);
}
