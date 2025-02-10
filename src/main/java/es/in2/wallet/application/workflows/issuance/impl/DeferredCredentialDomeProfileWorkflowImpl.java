package es.in2.wallet.application.workflows.issuance.impl;

import es.in2.wallet.application.workflows.issuance.DeferredCredentialDomeProfileWorkflow;
import es.in2.wallet.domain.services.CredentialService;
import es.in2.wallet.domain.services.DeferredCredentialMetadataService;
import es.in2.wallet.domain.services.OID4VCICredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeferredCredentialDomeProfileWorkflowImpl implements DeferredCredentialDomeProfileWorkflow {
    private final OID4VCICredentialService OID4VCICredentialService;
    private final DeferredCredentialMetadataService deferredCredentialMetadataService;
    private final CredentialService credentialService;
    @Override
    public Mono<Void> requestDeferredCredential(String processId, String userId, String credentialId) {
        return deferredCredentialMetadataService.getDeferredCredentialMetadataByCredentialId(processId,credentialId)
                .flatMap(deferredCredentialMetadata -> OID4VCICredentialService.getCredentialDomeDeferredCase(deferredCredentialMetadata.getTransactionId().toString(), deferredCredentialMetadata.getAccessToken(), deferredCredentialMetadata.getDeferredEndpoint()))
                .flatMap(credentialResponseWithStatus -> {
                    if (credentialResponseWithStatus.credentialResponse().transactionId() == null || credentialResponseWithStatus.credentialResponse().transactionId().isEmpty()) {
                        return credentialService.saveDeferredCredential(processId, userId, credentialId, credentialResponseWithStatus.credentialResponse())
                                .then(deferredCredentialMetadataService.deleteDeferredCredentialMetadataByCredentialId(processId, credentialId))
                                .doOnSuccess(aVoid -> log.info("Credential deferred case saved successfully for processId: {} and credentialId: {}", processId, credentialId));
                    }
                    else {
                        return deferredCredentialMetadataService.updateDeferredCredentialMetadataTransactionIdByCredentialId(processId, credentialId, credentialResponseWithStatus.credentialResponse().transactionId())
                                .doOnSuccess(aVoid -> log.info("TransactionId updated successfully for processId: {} and credentialId: {}", processId, credentialId));
                    }

                });
    }
}
