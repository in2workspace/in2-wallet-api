package es.in2.wallet.domain.services;

import es.in2.wallet.application.dto.CredentialResponse;
import es.in2.wallet.application.dto.CredentialsBasicInfo;
import es.in2.wallet.application.dto.SingleCredentialResponse;
import reactor.core.publisher.Mono;

import java.util.List;

public interface DataService {

    Mono<List<CredentialsBasicInfo>> getUserVCsInJson(String userEntity);
    Mono<String> getVerifiableCredentialOnRequestedFormat(String credentialEntityJson, String format);
    Mono<String> extractDidFromVerifiableCredential(String credentialJson);
}
