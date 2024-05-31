package es.in2.wallet.application.workflow.data;

import es.in2.wallet.domain.model.CredentialsBasicInfo;
import reactor.core.publisher.Mono;

import java.util.List;

public interface DataWorkflow {
    Mono<List<CredentialsBasicInfo>> getAllCredentialsByUserId(String processId, String userId);
    Mono<Void> deleteCredentialByIdAndUserId(String processId, String credentialId, String userId);
}
