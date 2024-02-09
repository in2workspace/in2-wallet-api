package es.in2.wallet.api.facade.impl;

import es.in2.wallet.api.facade.UserDataFacadeService;
import es.in2.wallet.api.model.CredentialsBasicInfo;
import es.in2.wallet.api.service.UserDataService;
import es.in2.wallet.broker.service.BrokerService;
import es.in2.wallet.vault.service.VaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataFacadeServiceImpl implements UserDataFacadeService {

    private final BrokerService brokerService;
    private final UserDataService userDataService;
    private final VaultService vaultService;
    @Override
    public Mono<List<CredentialsBasicInfo>> getUserVCs(String processId, String userId) {
        // Retrieve the UserEntity from the Context Broker
        return brokerService.getEntityById(processId, userId)
                .flatMap(optionalEntity -> optionalEntity
                        .map(userDataService::getUserVCsInJson)
                        .orElseGet(() -> Mono.error(new RuntimeException("There's no credential available.")))
                )
                .doOnSuccess(list -> log.info("Retrieved VCs in JSON for userId: {}", userId))
                .onErrorResume(e -> {
                    log.error("Error in retrieving VCs in JSON for userId: {}", userId, e);
                    return Mono.error(e);
                });
    }

    @Override
    public Mono<Void> deleteVerifiableCredentialById(String processId,String credentialId, String userId) {
        return brokerService.getEntityById(processId,userId)
                        .flatMap(optionalEntity -> optionalEntity
                                .map(entity -> userDataService.extractDidFromVerifiableCredential(entity,credentialId)
                                        .flatMap(did -> vaultService.deleteSecretByKey(did)
                                                .then(userDataService.deleteVerifiableCredential(entity,credentialId,did))
                                                .flatMap(updateEntity-> brokerService.updateEntity(processId, userId ,updateEntity))
                                        ))
                                .orElseGet(() -> Mono.error(new RuntimeException("There's no credential available with the id: " + credentialId)))
                        );
    }
}
