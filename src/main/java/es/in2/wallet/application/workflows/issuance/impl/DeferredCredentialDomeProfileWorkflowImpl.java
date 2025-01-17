package es.in2.wallet.application.workflows.issuance.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.application.workflows.issuance.DeferredCredentialDomeProfileWorkflow;
import es.in2.wallet.domain.services.CredentialService;
import es.in2.wallet.infrastructure.services.CredentialRepositoryService;
import es.in2.wallet.infrastructure.services.DeferredCredentialMetadataRepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeferredCredentialDomeProfileWorkflowImpl implements DeferredCredentialDomeProfileWorkflow {
    private final CredentialService credentialService;
    private final DeferredCredentialMetadataRepositoryService deferredCredentialMetadataRepositoryService;
    private final CredentialRepositoryService credentialRepositoryService;
    @Override
    public Mono<Void> requestDeferredCredential(String processId, String userId, String credentialId) {
        return deferredCredentialMetadataRepositoryService.getDeferredCredentialMetadataByCredentialId(processId,credentialId)
                .flatMap(deferredCredentialMetadata -> credentialService.getCredentialDomeDeferredCase(deferredCredentialMetadata.getTransactionId().toString(), deferredCredentialMetadata.getAccessToken(), deferredCredentialMetadata.getDeferredEndpoint()))
                .flatMap(credentialResponseWithStatus -> {
                    if (credentialResponseWithStatus.credentialResponse().transactionId() == null || credentialResponseWithStatus.credentialResponse().transactionId().isEmpty()) {
                        return credentialRepositoryService.saveDeferredCredential(processId, userId, credentialId, credentialResponseWithStatus.credentialResponse())
                                .then(deferredCredentialMetadataRepositoryService.deleteDeferredCredentialMetadataByCredentialId(processId, credentialId))
                                .doOnSuccess(aVoid -> log.info("Credential deferred case saved successfully for processId: {} and credentialId: {}", processId, credentialId));
                    }
                    else {
                        return deferredCredentialMetadataRepositoryService.updateDeferredCredentialMetadataTransactionIdByCredentialId(processId, credentialId, credentialResponseWithStatus.credentialResponse().transactionId())
                                .doOnSuccess(aVoid -> log.info("TransactionId updated successfully for processId: {} and credentialId: {}", processId, credentialId));
                    }

                });
    }
}
