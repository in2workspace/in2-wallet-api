package es.in2.wallet.application.workflow.presentation;

import es.in2.wallet.domain.model.CredentialsBasicInfo;
import es.in2.wallet.domain.model.VcSelectorRequest;
import es.in2.wallet.domain.model.VcSelectorResponse;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AttestationExchangeCommonWorkflow {
    Mono<VcSelectorRequest> processAuthorizationRequest(String processId, String authorizationToken, String qrContent);
    Mono<List<CredentialsBasicInfo>> getSelectableCredentialsRequiredToBuildThePresentation(String processId, String authorizationToken, List<String> scope);
    Mono<Void> buildVerifiablePresentationWithSelectedVCs(String processId, String authorizationToken, VcSelectorResponse vcSelectorResponse);
}
