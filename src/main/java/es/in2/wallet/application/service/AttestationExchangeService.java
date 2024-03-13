package es.in2.wallet.application.service;

import es.in2.wallet.domain.model.VcSelectorRequest;
import es.in2.wallet.domain.model.VcSelectorResponse;
import reactor.core.publisher.Mono;

public interface AttestationExchangeService {
    Mono<VcSelectorRequest> getSelectableCredentialsRequiredToBuildThePresentation(String processId, String authorizationToken, String qrContent);
    Mono<Void> buildVerifiablePresentationWithSelectedVCs(String processId, String authorizationToken, VcSelectorResponse vcSelectorResponse);
}
