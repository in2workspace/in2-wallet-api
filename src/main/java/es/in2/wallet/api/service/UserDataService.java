package es.in2.wallet.api.service;

import es.in2.wallet.api.model.UserEntity;
import es.in2.wallet.api.model.VCAttribute;
import es.in2.wallet.api.model.VcBasicData;
import reactor.core.publisher.Mono;

import java.util.List;

public interface UserDataService {

    Mono<String> createUserEntity(String id);
    Mono<String> saveVC(String userEntity, String vcJwt);
    Mono<List<VcBasicData>> getUserVCsInJson(String userEntity);

    Mono<List<VCAttribute>> getVerifiableCredentialsByFormat(String userEntity, String format);

    Mono<String> getVerifiableCredentialByIdAndFormat(String userEntity, String id, String format);

    Mono<String> extractDidFromVerifiableCredential(String userEntity, String vcId);

    Mono<String> deleteVerifiableCredential(String userEntity, String vcId, String did);

    Mono<List<VcBasicData>> getSelectableVCsByVcTypeList(List<String> vcTypeList, String userEntity);

    Mono<String> saveDid(String userEntity, String did, String didMethod);

    Mono<List<String>> getDidsByUserEntity(String userEntity);

    Mono<String> deleteSelectedDidFromUserEntity(String did, String userEntity);


}
