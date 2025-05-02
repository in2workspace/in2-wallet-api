package es.in2.wallet.application.workflows.presentation;

import es.in2.wallet.application.dto.CredentialsBasicInfo;
import es.in2.wallet.application.dto.VcSelectorRequest;
import es.in2.wallet.application.dto.VcSelectorResponse;
import reactor.core.publisher.Mono;

import java.util.List;

public interface Oid4vpWorkflow {
    Mono<VcSelectorRequest> processAuthorizationRequest(String processId, String authorizationToken, String qrContent);
    Mono<List<CredentialsBasicInfo>> getSelectableCredentialsRequiredToBuildThePresentation(String processId, String authorizationToken, List<String> scope);
    Mono<Void> buildVerifiablePresentationWithSelectedVCs(String processId, String authorizationToken, VcSelectorResponse vcSelectorResponse);
}
