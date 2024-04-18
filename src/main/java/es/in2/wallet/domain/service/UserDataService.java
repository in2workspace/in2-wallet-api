package es.in2.wallet.domain.service;

import es.in2.wallet.domain.model.CredentialResponse;
import es.in2.wallet.domain.model.CredentialsBasicInfo;
import reactor.core.publisher.Mono;

import java.util.List;

public interface UserDataService {

    Mono<String> createUserEntity(String id);
    Mono<String> saveVC(String userEntity, List<CredentialResponse> credentials);
    Mono<List<CredentialsBasicInfo>> getUserVCsInJson(String userEntity);
    Mono<String> getVerifiableCredentialOnRequestedFormat(String credentialEntityJson, String format);
    Mono<String> extractDidFromVerifiableCredential(String credentialJson);

    Mono<String> saveTransaction(String credentialId, String transactionId, String accessToken, String deferredEndpoint);

}
