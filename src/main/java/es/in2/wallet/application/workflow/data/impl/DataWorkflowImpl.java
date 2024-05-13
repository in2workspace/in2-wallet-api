package es.in2.wallet.application.workflow.data.impl;

import es.in2.wallet.application.port.BrokerService;
import es.in2.wallet.application.port.VaultService;
import es.in2.wallet.application.workflow.data.DataWorkflow;
import es.in2.wallet.domain.model.CredentialsBasicInfo;
import es.in2.wallet.domain.service.DataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataWorkflowImpl implements DataWorkflow {

    private final BrokerService brokerService;
    private final DataService dataService;
    private final VaultService vaultService;

    /**
     * Retrieves a list of basic information about the verifiable credentials (VCs) associated with a given user ID.
     *
     * @param processId A unique identifier for the process, used for logging and tracking.
     * @param userId    The unique identifier of the user whose VCs are to be retrieved.
     */
    @Override
    public Mono<List<CredentialsBasicInfo>> getAllCredentialsByUserId(String processId, String userId) {
        return brokerService.getAllCredentialsByUserId(processId, userId)
                .flatMap(dataService::getUserVCsInJson)
                .doOnSuccess(list -> log.info("Retrieved VCs in JSON for userId: {}", userId))
                .onErrorResume(Mono::error);
    }

    /**
     * Deletes a specific verifiable credential (VC) by its ID for a given user.
     * This method first retrieves the requested credential associated with the user. If the credential is found, it then
     * extracts the Decentralized Identifier (DID) from the VC, deletes the secret key associated with the DID
     * in the vault, and finally deletes the VC itself.
     *
     * @param processId    A unique identifier for the process, used for logging and tracking.
     * @param credentialId The unique identifier of the credential to be deleted.
     * @param userId       The unique identifier of the user from whom the VC is to be deleted.
     */

    @Override
    public Mono<Void> deleteCredentialByIdAndUserId(String processId, String credentialId, String userId) {
        return brokerService.getCredentialByIdAndUserId(processId,credentialId,userId)
                .flatMap(dataService::extractDidFromVerifiableCredential)
                .flatMap(vaultService::deleteSecretByKey)
                .then(brokerService.deleteCredentialByIdAndUserId(processId, credentialId, userId))
                .doOnSuccess(list -> log.info("Delete VC with Id: {}", credentialId))
                .onErrorResume(Mono::error);
    }

}
