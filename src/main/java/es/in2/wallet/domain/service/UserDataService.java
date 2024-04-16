package es.in2.wallet.domain.service;

import es.in2.wallet.domain.model.CredentialAttribute;
import es.in2.wallet.domain.model.CredentialResponse;
import es.in2.wallet.domain.model.CredentialsBasicInfo;
import reactor.core.publisher.Mono;

import java.util.List;

public interface UserDataService {

    Mono<String> createUserEntity(String id);
    Mono<String> saveVC(String userEntity, List<CredentialResponse> credentials);
    Mono<List<CredentialsBasicInfo>> getUserVCsInJson(String userEntity);

    Mono<List<CredentialAttribute>> getVerifiableCredentialsByFormat(String userEntity, String format);

    Mono<String> getVerifiableCredentialByIdAndFormat(String userEntity, String id, String format);

    Mono<String> extractDidFromVerifiableCredential(String userEntity, String vcId);

    Mono<String> deleteVerifiableCredential(String userEntity, String vcId, String did);

    Mono<List<CredentialsBasicInfo>> getSelectableVCsByVcTypeList(List<String> vcTypeList, String userEntity);

}
