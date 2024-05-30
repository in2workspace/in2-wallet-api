package es.in2.wallet.domain.service;

import es.in2.wallet.domain.model.CredentialResponse;
import es.in2.wallet.domain.model.CredentialsBasicInfo;
import reactor.core.publisher.Mono;

import java.util.List;

public interface DataService {

    Mono<String> createUserEntity(String id);
    Mono<String> saveVC(String processId, String userId, CredentialResponse credential);
    Mono<List<CredentialsBasicInfo>> getUserVCsInJson(String userEntity);
    Mono<String> getVerifiableCredentialOnRequestedFormat(String credentialEntityJson, String format);
    Mono<String> extractDidFromVerifiableCredential(String credentialJson);
    Mono<String> saveDOMEUnsignedCredential(String userId, String credentialJson);

    Mono<String> saveTransaction(String credentialId, String transactionId, String accessToken, String deferredEndpoint);
    Mono<String> updateVCEntityWithSignedFormat(String credentialEntity, CredentialResponse signedCredential);
    Mono<String> updateTransactionWithNewTransactionId(String transactionEntity, String transactionId);
}
