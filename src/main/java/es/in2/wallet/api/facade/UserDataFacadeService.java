package es.in2.wallet.api.facade;

import es.in2.wallet.api.model.CredentialsBasicInfoWithExpirationDate;
import reactor.core.publisher.Mono;

import java.util.List;

public interface UserDataFacadeService {
    public Mono<List<CredentialsBasicInfoWithExpirationDate>> getUserVCs(String processId, String userId);
    Mono<Void> deleteVerifiableCredentialById(String processId,String credentialId, String userId);
}
